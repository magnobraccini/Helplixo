package helplixo.web.inventario;

import helplixo.web.RegraVioladaException;
import helplixo.web.chamado.ChamadoRepositorio;
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

    private final MaquinaRepositorio maquinas;
    private final ChamadoRepositorio chamados;
    private final Clock relogio;

    public InventarioController(MaquinaRepositorio maquinas, ChamadoRepositorio chamados, Clock relogio) {
        this.maquinas = maquinas;
        this.chamados = chamados;
        this.relogio = relogio;
    }

    /** Situação do pacote Office: sim, sim (sem licença), não, não informado ou não se aplica. */
    public record Office(String situacao, String detalhe) {}

    /** situacao: vigente, vence (em até 60 dias), vencida ou sem informação. */
    public record Garantia(String situacao, String texto) {}

    public record ItemMaquina(String nSemin, String patrimonio, String tipo, String modelo, String local, String usuario, String situacao, String office) {}

    public record Busca(List<ItemMaquina> maquinas, int total, boolean temMais) {}

    /** Uma linha do histórico: Conserto ou Chamado, do mais novo para o mais antigo. */
    public record ItemHistorico(LocalDate data, String tipo, String texto, String referencia) {}

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
        m.consertos().forEach(c -> historico.add(new ItemHistorico(c.data(), "Conserto", c.defeito() + " → " + c.solucao() + " (" + c.onde() + ")", null)));
        chamados.daMaquina(m.nSemin()).forEach(c -> historico.add(new ItemHistorico(c.abertoEm().toLocalDate(), "Chamado",
                c.problema() + " (" + c.status().rotulo().toLowerCase() + ")", c.numero())));
        historico.sort(Comparator.comparing(ItemHistorico::data).reversed());
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
