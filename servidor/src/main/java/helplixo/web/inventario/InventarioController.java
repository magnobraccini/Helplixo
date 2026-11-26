package helplixo.web.inventario;

import helplixo.web.RegraVioladaException;
import helplixo.web.chamado.ChamadoRepositorio;
import helplixo.web.edicao.EdicaoController;
import helplixo.web.edicao.EdicaoServico;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API do inventário (leitura):
 * GET /api/inventario/resumo        quantas máquinas há, por tipo
 * GET /api/inventario?q=windows     busca (pelo menos 2 letras): Nº Semin, patrimônio, usuário, setor, modelo ou software
 * GET /api/inventario/{nSemin}      a ficha: dados, compra, softwares, Office, garantia e histórico
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    static final int MAXIMO_RESULTADOS = 20;
    private static final Pattern OFFICE = Pattern.compile("office|microsoft 365", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Quem fez a alteração. Até existir o login, a tela manda o nome neste cabeçalho. */
    public static final String CABECALHO_USUARIO = "X-Usuario";

    private final MaquinaRepositorio maquinas;
    private final ChamadoRepositorio chamados;
    private final InventarioServico servico;
    private final EdicaoServico edicao;
    private final Clock relogio;

    public InventarioController(MaquinaRepositorio maquinas, ChamadoRepositorio chamados, InventarioServico servico,
                                EdicaoServico edicao, Clock relogio) {
        this.maquinas = maquinas;
        this.chamados = chamados;
        this.servico = servico;
        this.edicao = edicao;
        this.relogio = relogio;
    }

    // ---------- edição (modo edição: cabeçalho X-Chave-Edicao) ----------

    /** Adicionar máquina. Resposta 201 com a máquina criada. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Maquina adicionar(@RequestBody InventarioServico.DadosMaquina dados,
                             @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave,
                             @RequestHeader(value = CABECALHO_USUARIO, required = false) String usuario) {
        edicao.exigir(chave);
        return servico.adicionar(dados, quem(usuario));
    }

    /** Editar máquina. Nº Semin e patrimônio não mudam. */
    @PutMapping("/{nSemin}")
    public InventarioServico.ResultadoEdicao editar(@PathVariable String nSemin, @RequestBody InventarioServico.DadosMaquina dados,
                                                    @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave,
                                                    @RequestHeader(value = CABECALHO_USUARIO, required = false) String usuario) {
        edicao.exigir(chave);
        return servico.editar(nSemin, dados, quem(usuario));
    }

    /** Movimentação: a máquina vai para outro lugar. Não precisa do modo edição (é trabalho do técnico). */
    @PostMapping("/{nSemin}/movimentacoes")
    public Maquina movimentar(@PathVariable String nSemin, @RequestBody InventarioServico.PedidoMovimentacao pedido) {
        return servico.movimentar(nSemin, pedido);
    }

    @PostMapping("/{nSemin}/baixa")
    public Maquina darBaixa(@PathVariable String nSemin, @RequestBody InventarioServico.PedidoBaixa pedido,
                            @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave,
                            @RequestHeader(value = CABECALHO_USUARIO, required = false) String usuario) {
        edicao.exigir(chave);
        return servico.darBaixa(nSemin, pedido, quem(usuario));
    }

    @DeleteMapping("/{nSemin}/baixa")
    public Maquina desfazerBaixa(@PathVariable String nSemin,
                                 @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave,
                                 @RequestHeader(value = CABECALHO_USUARIO, required = false) String usuario) {
        edicao.exigir(chave);
        return servico.desfazerBaixa(nSemin, quem(usuario));
    }

    private static String quem(String usuario) {
        return usuario == null || usuario.isBlank() ? "não identificado" : usuario.trim();
    }

    /** Situação do pacote Office: sim, sim (sem licença), não, não informado ou não se aplica. */
    public record Office(String situacao, String detalhe) {}

    /** situacao: vigente, vence (em até 60 dias), vencida ou sem informação. */
    public record Garantia(String situacao, String texto) {}

    public record ItemMaquina(String nSemin, String patrimonio, String tipo, String modelo, String local, String usuario, String situacao, String office) {}

    public record Busca(List<ItemMaquina> maquinas, int total, boolean temMais) {}

    /** Uma linha do histórico (Conserto, Chamado, Movimentação, Cadastro ou Baixa), do mais novo para o mais antigo. */
    public record ItemHistorico(LocalDateTime quando, String tipo, String texto, String referencia, String quem) {}

    public record Ficha(Maquina maquina, Office office, Garantia garantia, List<ItemHistorico> historico) {}

    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        maquinas.todas().forEach(m -> porTipo.merge(m.tipo(), 1L, Long::sum));
        return Map.of("total", maquinas.todas().size(), "porTipo", porTipo);
    }

    @GetMapping
    public Busca buscar(@RequestParam String q) {
        String termo = semAcento(q.trim());
        if (termo.length() < 2) {
            throw new RegraVioladaException(List.of("Digite pelo menos 2 letras para procurar."));
        }
        List<ItemMaquina> achadas = maquinas.todas().stream()
                .filter(m -> semAcento(textoParaBusca(m)).contains(termo))
                .map(m -> new ItemMaquina(m.nSemin(), m.patrimonio(), m.tipo(), m.modelo(), m.local(), m.usuario(), m.situacao(), office(m).situacao()))
                .toList();
        // a máquina com o Nº Semin exato vem primeiro
        List<ItemMaquina> ordenadas = new ArrayList<>(achadas);
        ordenadas.sort(Comparator.comparing((ItemMaquina i) -> !i.nSemin().equalsIgnoreCase(q.trim())));
        return new Busca(ordenadas.stream().limit(MAXIMO_RESULTADOS).toList(), ordenadas.size(), ordenadas.size() > MAXIMO_RESULTADOS);
    }

    @GetMapping("/{nSemin}")
    public Ficha ficha(@PathVariable String nSemin) {
        Maquina m = maquinas.porNSemin(nSemin)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina " + nSemin + " não está no inventário."));
        List<ItemHistorico> historico = new ArrayList<>();
        m.consertos().forEach(c -> historico.add(new ItemHistorico(c.data().atStartOfDay(), "Conserto",
                c.defeito() + " → " + c.solucao() + " (" + c.onde() + ")", null, null)));
        chamados.daMaquina(m.nSemin()).forEach(c -> historico.add(new ItemHistorico(c.abertoEm(), "Chamado",
                c.problema() + " (" + c.status().rotulo().toLowerCase() + ")", c.numero(), c.tecnico())));
        maquinas.movimentacoesDe(m.nSemin()).forEach(v -> historico.add(new ItemHistorico(v.quando(), "Movimentação",
                v.de() + " → " + v.para() + ": " + v.motivo(), v.chamado().isEmpty() ? null : v.chamado(), v.tecnico())));
        maquinas.registrosDe(m.nSemin()).forEach(r -> historico.add(new ItemHistorico(r.quando(), r.tipo(), r.texto(), null, r.quem())));
        // do mais novo para o mais antigo; no mesmo instante, o último registrado vem primeiro
        java.util.Collections.reverse(historico);
        historico.sort(Comparator.comparing(ItemHistorico::quando).reversed());
        return new Ficha(m, office(m), garantia(m), historico);
    }

    static Office office(Maquina m) {
        if (!m.ehComputador()) return new Office("não se aplica", "Não se aplica a este tipo de equipamento.");
        if (m.softwares().isEmpty()) return new Office("não informado", "Nenhum software cadastrado ainda.");
        for (Maquina.Software s : m.softwares()) {
            if (OFFICE.matcher(s.nome()).find() && !s.nome().toLowerCase().contains("libre")) {
                boolean semLicenca = s.licenca() == null || s.licenca().isBlank();
                return semLicenca ? new Office("sim, sem licença", s.nome() + ": instalado sem licença registrada.")
                        : new Office("sim", s.nome() + " (licença " + s.tipo() + ").");
            }
        }
        return m.softwares().stream().filter(s -> s.nome().toLowerCase().contains("libreoffice")).findFirst()
                .map(l -> new Office("não", "Não tem. Usa " + l.nome() + "."))
                .orElse(new Office("não", "Não tem pacote Office instalado."));
    }

    private Garantia garantia(Maquina m) {
        LocalDate ate = m.compra() == null ? null : m.compra().garantiaAte();
        if (ate == null) return new Garantia("sem informação", "Não informada");
        long dias = ChronoUnit.DAYS.between(LocalDate.now(relogio), ate);
        if (dias < 0) return new Garantia("vencida", "Vencida em " + DIA.format(ate));
        if (dias <= 60) return new Garantia("vence", "Vence em " + dias + (dias == 1 ? " dia" : " dias") + " (" + DIA.format(ate) + ")");
        return new Garantia("vigente", "Vigente até " + DIA.format(ate));
    }

    private static String textoParaBusca(Maquina m) {
        StringBuilder t = new StringBuilder();
        for (String s : new String[] {m.nSemin(), m.patrimonio(), m.tipo(), m.modelo(), m.local(), m.usuario(), m.sistema(), m.ip()}) {
            if (s != null) t.append(s).append(' ');
        }
        m.softwares().forEach(s -> t.append(s.nome()).append(' '));
        m.sistemasPmsl().forEach(s -> t.append(s.nome()).append(' '));
        return t.toString();
    }

    static String semAcento(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
