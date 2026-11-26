package helplixo.web.inventario;

import helplixo.web.RegraVioladaException;
import helplixo.web.chamado.ChamadoRepositorio;
import helplixo.web.chamado.StatusChamado;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regras de edição do inventário (as mesmas da maquete e do documento de regras):
 * - Nº Semin e patrimônio são a identidade da máquina e nunca mudam.
 * - Toda edição vai para o histórico com o que mudou (antes → depois) e quem fez.
 * - Máquina baixada continua nas buscas, mas não recebe movimentação; a baixa pode ser desfeita.
 */
@Service
public class InventarioServico {

    static final Pattern N_SEMIN = Pattern.compile("^[A-Z]{2}\\d{1,4}$");
    static final Pattern IP = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    static final List<String> TIPOS = List.of("Computador", "Notebook", "Impressora", "Monitor", "Telefone IP", "Nobreak", "Outro");
    static final List<String> MOTIVOS_BAIXA = List.of("Descarte", "Doação", "Leilão", "Furto ou perda", "Outro");
    static final int TOLERANCIA_FUTURO_MIN = 5;

    private final MaquinaRepositorio maquinas;
    private final ChamadoRepositorio chamados;
    private final Clock relogio;

    public InventarioServico(MaquinaRepositorio maquinas, ChamadoRepositorio chamados, Clock relogio) {
        this.maquinas = maquinas;
        this.chamados = chamados;
        this.relogio = relogio;
    }

    /** Os campos que a tela manda ao adicionar ou editar uma máquina. */
    public record DadosMaquina(String nSemin, String patrimonio, String tipo, String modelo, String serie, String processador,
                               String memoria, String armazenamento, String sistema, String ip, String mac, String local,
                               String usuario, String observacao, Maquina.Compra compra) {}

    public record PedidoMovimentacao(String destino, String motivo, LocalDateTime quando, String tecnico, String chamado) {}

    public record PedidoBaixa(String motivo, String observacao) {}

    /** A máquina como ficou e a lista do que mudou (vazia = nada mudou). */
    public record ResultadoEdicao(Maquina maquina, List<String> mudancas) {}

    /** A situação acompanha o lugar: assistência = em manutenção, depósito = reserva, o resto = em uso. */
    static String situacaoDoLocal(String local) {
        if ("Assistência técnica".equalsIgnoreCase(local)) return "Em manutenção";
        if ("Depósito da TI".equalsIgnoreCase(local)) return "Reserva";
        return "Em uso";
    }

    // ---------- adicionar ----------

    public Maquina adicionar(DadosMaquina d, String quem) {
        List<String> erros = new ArrayList<>();
        String nSemin = maiusculo(d.nSemin());
        if (!N_SEMIN.matcher(nSemin).matches()) {
            erros.add("O Nº Semin deve ter 2 letras e até 4 números (ex.: IC21, CC1261).");
        } else if (maquinas.porNSemin(nSemin).isPresent()) {
            erros.add("Já existe uma máquina com o Nº Semin " + nSemin + ".");
        }
        String patrimonio = maiusculo(d.patrimonio());
        if (patrimonio.isEmpty()) {
            erros.add("Informe o número de patrimônio.");
        } else {
            maquinas.porPatrimonio(patrimonio).ifPresent(m -> erros.add("O patrimônio " + patrimonio + " já está cadastrado (" + m.nSemin() + ")."));
        }
        conferirCampos(d, erros);
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);

        Maquina nova = new Maquina(nSemin, patrimonio, d.tipo(), d.modelo().trim(), limpo(d.serie()), limpo(d.processador()),
                limpo(d.memoria()), limpo(d.armazenamento()), limpo(d.sistema()), limpo(d.ip()), limpo(d.mac()), d.local().trim(),
                limpo(d.usuario()), situacaoDoLocal(d.local()), limpo(d.observacao()), d.compra(), List.of(), List.of(), List.of(), null);
        maquinas.salvar(nova);
        maquinas.registrar(new RegistroCadastro(nSemin, LocalDateTime.now(relogio), "Cadastro", "Máquina cadastrada.", quem));
        return nova;
    }

    // ---------- editar ----------

    public ResultadoEdicao editar(String nSemin, DadosMaquina d, String quem) {
        Maquina atual = existente(nSemin);
        List<String> erros = new ArrayList<>();
        if (d.nSemin() != null && !d.nSemin().isBlank() && !maiusculo(d.nSemin()).equals(atual.nSemin())) {
            erros.add("O Nº Semin não muda: chamados e movimentações apontam para ele.");
        }
        if (d.patrimonio() != null && !d.patrimonio().isBlank() && !maiusculo(d.patrimonio()).equalsIgnoreCase(atual.patrimonio())) {
            erros.add("O patrimônio não muda: ele é a identidade da máquina, junto com o Nº Semin.");
        }
        conferirCampos(d, erros);
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);

        String local = d.local().trim();
        boolean mudouLocal = !local.equals(atual.local());
        String situacao = atual.baixada() ? atual.situacao() : (mudouLocal ? situacaoDoLocal(local) : atual.situacao());
        Maquina nova = new Maquina(atual.nSemin(), atual.patrimonio(), d.tipo(), d.modelo().trim(), limpo(d.serie()), limpo(d.processador()),
                limpo(d.memoria()), limpo(d.armazenamento()), limpo(d.sistema()), limpo(d.ip()), limpo(d.mac()), local,
                limpo(d.usuario()), situacao, limpo(d.observacao()), d.compra(), atual.softwares(), atual.sistemasPmsl(), atual.consertos(), atual.baixa());

        List<String> mudancas = diferencas(atual, nova);
        if (mudancas.isEmpty()) return new ResultadoEdicao(atual, mudancas);
        maquinas.salvar(nova);
        maquinas.registrar(new RegistroCadastro(atual.nSemin(), LocalDateTime.now(relogio), "Cadastro", String.join(" · ", mudancas), quem));
        return new ResultadoEdicao(nova, mudancas);
    }

    private static List<String> diferencas(Maquina a, Maquina b) {
        List<String> d = new ArrayList<>();
        campo(d, "Tipo", a.tipo(), b.tipo());
        campo(d, "Modelo", a.modelo(), b.modelo());
        campo(d, "Nº de série", a.serie(), b.serie());
        campo(d, "Processador", a.processador(), b.processador());
        campo(d, "RAM", a.memoria(), b.memoria());
        campo(d, "Armazenamento", a.armazenamento(), b.armazenamento());
        campo(d, "Sistema", a.sistema(), b.sistema());
        campo(d, "IP", a.ip(), b.ip());
        campo(d, "MAC", a.mac(), b.mac());
        campo(d, "Local", a.local(), b.local());
        campo(d, "Usuário", a.usuario(), b.usuario());
        campo(d, "Observação", a.observacao(), b.observacao());
        Maquina.Compra ca = a.compra(), cb = b.compra();
        campo(d, "Fornecedor", ca == null ? null : ca.fornecedor(), cb == null ? null : cb.fornecedor());
        campo(d, "Nota fiscal", ca == null ? null : ca.notaFiscal(), cb == null ? null : cb.notaFiscal());
        campo(d, "Data da compra", ca == null ? null : ca.data(), cb == null ? null : cb.data());
        campo(d, "Valor", ca == null ? null : ca.valor(), cb == null ? null : cb.valor());
        campo(d, "Processo de compra", ca == null ? null : ca.processo(), cb == null ? null : cb.processo());
        campo(d, "Garantia", ca == null ? null : ca.garantiaAte(), cb == null ? null : cb.garantiaAte());
        return d;
    }

    private static void campo(List<String> d, String nome, Object antes, Object depois) {
        String a = antes == null || antes.toString().isBlank() ? "—" : antes.toString();
        String b = depois == null || depois.toString().isBlank() ? "—" : depois.toString();
        if (!a.equals(b)) d.add(nome + ": " + a + " → " + b);
    }

    /** Regras comuns a adicionar e editar. */
    private void conferirCampos(DadosMaquina d, List<String> erros) {
        if (d.tipo() == null || !TIPOS.contains(d.tipo())) erros.add("Escolha o tipo: " + String.join(", ", TIPOS) + ".");
        if (d.modelo() == null || d.modelo().isBlank()) erros.add("Informe a marca e o modelo.");
        if (d.local() == null || d.local().isBlank()) erros.add("Escolha onde a máquina está.");
        if (d.ip() != null && !d.ip().isBlank()) {
            var m = IP.matcher(d.ip().trim());
            if (!m.matches()) erros.add("O IP deve ter o formato 10.0.12.21.");
            else for (int i = 1; i <= 4; i++) {
                if (Integer.parseInt(m.group(i)) > 255) { erros.add("Cada parte do IP vai de 0 a 255."); break; }
            }
        }
        Maquina.Compra c = d.compra();
        if (c != null) {
            if (c.data() != null && c.data().isAfter(LocalDate.now(relogio))) erros.add("A data da compra não pode estar no futuro.");
            if (c.data() != null && c.garantiaAte() != null && c.garantiaAte().isBefore(c.data())) erros.add("A garantia não pode terminar antes da data da compra.");
        }
    }

    // ---------- movimentação ----------

    public Maquina movimentar(String nSemin, PedidoMovimentacao p) {
        Maquina atual = existente(nSemin);
        List<String> erros = new ArrayList<>();
        if (atual.baixada()) erros.add("A máquina " + atual.nSemin() + " foi baixada e não pode ser movimentada.");
        if (p.destino() == null || p.destino().isBlank()) erros.add("Escolha para onde ela foi.");
        else if (p.destino().trim().equalsIgnoreCase(atual.local())) erros.add("O destino é o mesmo lugar de onde a máquina saiu.");
        if (p.motivo() == null || p.motivo().isBlank()) erros.add("Informe o motivo da movimentação.");
        if (p.tecnico() == null || p.tecnico().isBlank()) erros.add("Escolha o técnico.");
        if (p.quando() == null) erros.add("Informe a data e a hora.");
        else if (p.quando().isAfter(LocalDateTime.now(relogio).plusMinutes(TOLERANCIA_FUTURO_MIN))) erros.add("A data e a hora não podem estar no futuro.");
        if (p.chamado() != null && !p.chamado().isBlank() && chamados.porNumero(p.chamado().trim()).isEmpty()) {
            erros.add("O chamado " + p.chamado().trim() + " não existe.");
        }
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);

        String destino = p.destino().trim();
        Maquina nova = atual.noLocal(destino, situacaoDoLocal(destino));
        maquinas.salvar(nova);
        maquinas.registrar(new Movimentacao(atual.nSemin(), atual.local(), destino, p.motivo().trim(), p.quando(), p.tecnico().trim(),
                p.chamado() == null ? "" : p.chamado().trim()));
        return nova;
    }

    // ---------- baixa ----------

    public Maquina darBaixa(String nSemin, PedidoBaixa p, String quem) {
        Maquina atual = existente(nSemin);
        List<String> erros = new ArrayList<>();
        if (atual.baixada()) erros.add("A máquina " + atual.nSemin() + " já está baixada.");
        if (p.motivo() == null || !MOTIVOS_BAIXA.contains(p.motivo())) erros.add("Escolha o motivo: " + String.join(", ", MOTIVOS_BAIXA) + ".");
        String obs = p.observacao() == null ? "" : p.observacao().trim();
        if ("Outro".equals(p.motivo()) && obs.isEmpty()) erros.add("Explique o motivo da baixa.");
        List<String> abertos = chamados.daMaquina(atual.nSemin()).stream()
                .filter(c -> c.status() != StatusChamado.FECHADO).map(c -> c.numero()).toList();
        if (!abertos.isEmpty()) erros.add("Feche antes o(s) chamado(s) aberto(s) desta máquina: " + String.join(", ", abertos) + ".");
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);

        Maquina nova = atual.comBaixa(new Maquina.Baixa(p.motivo(), obs, LocalDate.now(relogio), quem), "Baixada");
        maquinas.salvar(nova);
        maquinas.registrar(new RegistroCadastro(atual.nSemin(), LocalDateTime.now(relogio), "Baixa", p.motivo() + (obs.isEmpty() ? "" : ": " + obs), quem));
        return nova;
    }

    public Maquina desfazerBaixa(String nSemin, String quem) {
        Maquina atual = existente(nSemin);
        if (!atual.baixada()) throw new RegraVioladaException(List.of("A máquina " + atual.nSemin() + " não está baixada."));
        Maquina nova = atual.comBaixa(null, situacaoDoLocal(atual.local()));
        maquinas.salvar(nova);
        maquinas.registrar(new RegistroCadastro(atual.nSemin(), LocalDateTime.now(relogio), "Cadastro", "Baixa desfeita.", quem));
        return nova;
    }

    // ---------- ajudantes ----------

    private Maquina existente(String nSemin) {
        return maquinas.porNSemin(nSemin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina " + nSemin + " não está no inventário."));
    }

    private static String maiusculo(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private static String limpo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
