package helplixo.web.calendario;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import helplixo.web.Cargos;
import helplixo.web.RegraVioladaException;
import helplixo.web.contato.ContatoRepositorio;
import helplixo.web.edicao.EdicaoServico;
import helplixo.web.inventario.MaquinaRepositorio;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regras do calendário (as do documento de regras):
 * - Aviso: qualquer cargo marca. As outras marcações pedem o modo edição.
 * - Feriados, vencimentos de contrato (Contatos) e fins de garantia (Inventário) entram sozinhos e não se removem.
 * - Remover: o próprio cargo do autor, ou um cargo de nível maior no mesmo setor.
 * - Fora do expediente (8h às 14h) é só um aviso.
 */
@Service
public class CalendarioServico {

    static final List<String> TIPOS = List.of("Aviso", "Manutenção", "Reunião", "Ausência", "Aniversário", "Feriado", "Outro");
    static final Set<String> REPETICOES = Set.of("", "semanal", "mensal", "anual");
    static final Set<String> LEMBRETES = Set.of("", "dia", "30", "60", "1440");
    static final int MAXIMO_DIAS_CONSULTA = 400;
    static final LocalTime HORA_LEMBRETE_DO_DIA = LocalTime.of(7, 30);
    private static final String[][] FERIADOS = {
            {"01-01", "Confraternização Universal"}, {"04-21", "Tiradentes"}, {"05-01", "Dia do Trabalho"}, {"09-07", "Independência do Brasil"},
            {"10-12", "Nossa Senhora Aparecida"}, {"11-02", "Finados"}, {"11-15", "Proclamação da República"},
            {"11-20", "Dia da Consciência Negra"}, {"12-25", "Natal"}};

    private final List<Marcacao> marcacoes = new CopyOnWriteArrayList<>();
    private final AtomicInteger sequencia = new AtomicInteger();
    private final ContatoRepositorio contatos;
    private final MaquinaRepositorio maquinas;
    private final EdicaoServico edicao;
    private final Clock relogio;

    public CalendarioServico(ObjectMapper json, @Value("${helplixo.dados.marcacoes}") Resource arquivo, ContatoRepositorio contatos,
                             MaquinaRepositorio maquinas, EdicaoServico edicao, Clock relogio) {
        this.contatos = contatos;
        this.maquinas = maquinas;
        this.edicao = edicao;
        this.relogio = relogio;
        try (InputStream entrada = arquivo.getInputStream()) {
            marcacoes.addAll(json.readValue(entrada, new TypeReference<List<Marcacao>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui ler os dados de teste do calendário: " + arquivo, e);
        }
    }

    /** Uma marcação num dia (as repetições viram uma ocorrência por dia). */
    public record Ocorrencia(String id, LocalDate dia, String titulo, String tipo, LocalTime horaInicio, LocalTime horaFim,
                             boolean diaInteiro, String descricao, String repeteTexto, String autor, String cargo,
                             boolean urgente, boolean automatica) {}

    public record PedidoMarcacao(String titulo, String tipo, LocalDate inicio, LocalDate fim, LocalTime horaInicio, LocalTime horaFim,
                                 String repete, LocalDate repeteAte, String lembrete, String descricao, boolean urgente) {}

    public record Criada(Marcacao marcacao, List<String> avisos) {}

    public record Lembrete(String id, String texto, LocalDateTime inicio, long faltaMinutos) {}

    // ---------- consultar ----------

    public List<Ocorrencia> ocorrencias(LocalDate de, LocalDate ate, Set<String> tipos, String busca) {
        if (ate.isBefore(de)) throw new RegraVioladaException(List.of("A data final precisa ser depois da inicial."));
        if (ChronoUnit.DAYS.between(de, ate) > MAXIMO_DIAS_CONSULTA) {
            throw new RegraVioladaException(List.of("Consulte no máximo " + MAXIMO_DIAS_CONSULTA + " dias de uma vez."));
        }
        String termo = busca == null ? "" : busca.trim().toLowerCase();
        List<Marcacao> todas = todas();
        List<Ocorrencia> lista = new ArrayList<>();
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
            for (Marcacao m : todas) {
                if ((tipos == null || tipos.isEmpty() || tipos.contains(m.tipo()))
                        && (termo.isEmpty() || (m.titulo() + " " + (m.descricao() == null ? "" : m.descricao())).toLowerCase().contains(termo))
                        && m.caiEm(dia)) {
                    lista.add(new Ocorrencia(m.id(), dia, m.titulo(), m.tipo(), m.horaInicio(), m.horaFim(), m.horaInicio() == null,
                            m.descricao(), m.repeteTexto(), m.autor(), m.cargo(), m.urgente(), m.automatica()));
                }
            }
        }
        lista.sort(Comparator.comparing(Ocorrencia::dia).thenComparing(o -> o.horaInicio() == null ? LocalTime.MIN : o.horaInicio()));
        return lista;
    }

    /** As marcações lançadas + as automáticas (feriados, contratos e garantias). */
    List<Marcacao> todas() {
        List<Marcacao> todas = new ArrayList<>(marcacoes);
        for (String[] f : FERIADOS) {
            todas.add(new Marcacao("feriado-" + f[0], f[1], "Feriado", LocalDate.parse("2000-" + f[0]), null, null, null,
                    "anual", null, "", null, null, null, false, true));
        }
        contatos.externos().forEach(c -> todas.add(new Marcacao("contrato-" + c.empresa(), "Vence contrato " + c.empresa(), "Contrato",
                c.vence(), null, null, null, "", null, "", "Contrato " + (c.contrato() == null ? "" : c.contrato()), null, null, false, true)));
        LocalDate limite = LocalDate.now(relogio).minusDays(30);
        maquinas.todas().forEach(m -> {
            LocalDate g = m.compra() == null ? null : m.compra().garantiaAte();
            if (g != null && !g.isBefore(limite)) {
                todas.add(new Marcacao("garantia-" + m.nSemin(), "Fim da garantia " + m.nSemin(), "Garantia", g, null, null, null,
                        "", null, "", m.modelo(), null, null, false, true));
            }
        });
        return todas;
    }

    // ---------- marcar ----------

    public Criada criar(PedidoMarcacao p, String chave, String usuario, String cargo) {
        boolean aviso = "Aviso".equals(p.tipo());
        if (!aviso) edicao.exigir(chave); // as outras marcações pedem o modo edição
        List<String> erros = new ArrayList<>();
        if (aviso && (usuario == null || usuario.isBlank() || !Cargos.existe(cargo))) {
            erros.add("Informe quem está marcando o aviso (nome e cargo).");
        }
        if (p.titulo() == null || p.titulo().isBlank()) erros.add("Escreva o que é a marcação.");
        if (p.tipo() == null || !TIPOS.contains(p.tipo())) erros.add("Escolha o tipo: " + String.join(", ", TIPOS) + ".");
        if (p.inicio() == null) erros.add("Escolha o dia.");
        if (p.inicio() != null && p.fim() != null && p.fim().isBefore(p.inicio())) erros.add("O fim não pode ser antes do início.");
        if (p.horaInicio() == null && p.horaFim() != null) erros.add("Informe a hora de início ou deixe como dia inteiro.");
        if (p.horaInicio() != null && p.horaFim() != null && !p.horaFim().isAfter(p.horaInicio())) {
            erros.add("A hora de fim precisa ser depois da hora de início.");
        }
        String repete = p.repete() == null ? "" : p.repete();
        if (!REPETICOES.contains(repete)) erros.add("Repetição inválida: use semanal, mensal, anual ou nenhuma.");
        if (!repete.isEmpty() && p.fim() != null && !p.fim().equals(p.inicio())) erros.add("Repetição vale só para marcação de um dia.");
        if (!repete.isEmpty() && p.repeteAte() != null && p.inicio() != null && p.repeteAte().isBefore(p.inicio())) {
            erros.add("A repetição precisa terminar depois do primeiro dia.");
        }
        String lembrete = p.lembrete() == null ? "" : p.lembrete();
        if (!LEMBRETES.contains(lembrete)) erros.add("Lembrete inválido.");
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);

        Marcacao m = new Marcacao("ev-" + sequencia.incrementAndGet(), p.titulo().trim(), p.tipo(), p.inicio(),
                p.fim() != null && !p.fim().equals(p.inicio()) ? p.fim() : null, p.horaInicio(), p.horaFim(),
                repete, repete.isEmpty() ? null : p.repeteAte(), lembrete, p.descricao() == null ? "" : p.descricao().trim(),
                usuario == null || usuario.isBlank() ? "não identificado" : usuario.trim(), Cargos.existe(cargo) ? cargo : null,
                p.urgente(), false);
        marcacoes.add(m);
        List<String> avisos = new ArrayList<>();
        LocalTime h = p.horaInicio();
        if (h != null && (h.isBefore(LocalTime.of(8, 0)) || !h.isBefore(LocalTime.of(14, 0)))) {
            avisos.add("Fora do expediente (8h às 14h). É só um aviso.");
        }
        return new Criada(m, avisos);
    }

    // ---------- remover ----------

    public void remover(String id, String cargo) {
        Marcacao m = todas().stream().filter(x -> x.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marcação " + id + " não encontrada."));
        if (m.automatica()) throw new RegraVioladaException(List.of("Feriados, contratos e garantias entram sozinhos e não podem ser removidos."));
        if (m.cargo() == null || !Cargos.podeSobre(cargo, m.cargo())) {
            throw new RegraVioladaException(List.of("Só o autor ou um cargo acima dele no mesmo setor pode remover esta marcação."));
        }
        marcacoes.removeIf(x -> x.id().equals(id));
    }

    // ---------- lembretes ----------

    /** Lembretes de hoje e amanhã cuja hora já chegou e cuja marcação ainda não terminou. */
    public List<Lembrete> lembretes() {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();
        List<Lembrete> lista = new ArrayList<>();
        for (LocalDate dia : List.of(hoje, hoje.plusDays(1))) {
            for (Marcacao m : todas()) {
                if (m.lembrete().isEmpty() || !m.caiEm(dia)) continue;
                LocalDateTime inicio = dia.atTime(m.horaInicio() == null ? LocalTime.MIN : m.horaInicio());
                LocalDateTime fim = m.horaFim() != null ? dia.atTime(m.horaFim())
                        : (m.horaInicio() != null ? inicio.plusHours(1) : dia.plusDays(1).atStartOfDay());
                LocalDateTime momento = "dia".equals(m.lembrete()) ? dia.atTime(HORA_LEMBRETE_DO_DIA)
                        : inicio.minusMinutes(Long.parseLong(m.lembrete()));
                if (agora.isBefore(momento) || !agora.isBefore(fim)) continue;
                String quando = (dia.equals(hoje) ? "hoje" : "amanhã") + (m.horaInicio() == null ? "" : " às " + m.horaInicio());
                lista.add(new Lembrete(m.id(), "Lembrete: " + m.titulo() + " · " + quando + ".", inicio,
                        Math.max(0, Duration.between(agora, inicio).toMinutes())));
            }
        }
        lista.sort(Comparator.comparing(Lembrete::inicio));
        return lista;
    }
}
