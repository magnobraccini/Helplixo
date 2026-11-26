package helplixo.web.contato;

import helplixo.web.RegraVioladaException;
import helplixo.web.contato.ContatoRepositorio.Externo;
import helplixo.web.contato.ContatoRepositorio.Interno;
import helplixo.web.edicao.EdicaoController;
import helplixo.web.edicao.EdicaoServico;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contatos:
 * GET  /api/contatos/internos?secretaria=SMED&andar=3º&ordem=nome|setor|ramal&q=
 * GET  /api/contatos/externos?categoria=Sistemas&situacao=emdia|vence|vencido&q=
 * POST /api/contatos/internos  e  POST /api/contatos/externos   (modo edição)
 * As opções dos filtros saem dos próprios contatos: um andar ou categoria novos viram opção sozinhos.
 */
@RestController
@RequestMapping("/api/contatos")
public class ContatoController {

    static final Pattern RAMAL = Pattern.compile("^\\d{4}$");
    static final Pattern EMAIL = Pattern.compile("^\\S+@\\S+\\.\\S+$");
    static final int DIAS_AVISO_CONTRATO = 60;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContatoRepositorio repositorio;
    private final EdicaoServico edicao;
    private final Clock relogio;

    public ContatoController(ContatoRepositorio repositorio, EdicaoServico edicao, Clock relogio) {
        this.repositorio = repositorio;
        this.edicao = edicao;
        this.relogio = relogio;
    }

    public record OpcoesInternos(List<String> secretarias, List<String> andares) {}

    public record ListaInternos(List<Interno> contatos, int total, int deTotal, OpcoesInternos opcoes) {}

    /** situacao: emdia, vence (em até 60 dias) ou vencido; texto pronto para a tela. */
    public record ExternoComSituacao(Externo contato, String situacao, String texto, long dias) {}

    public record ListaExternos(List<ExternoComSituacao> contatos, int total, int deTotal, List<String> categorias) {}

    /** Resposta do cadastro de ramal: o contato e, se for o caso, com quem o ramal é dividido. */
    public record InternoSalvo(Interno contato, List<String> compartilhadoCom) {}

    @GetMapping("/internos")
    public ListaInternos internos(@RequestParam(required = false) String secretaria, @RequestParam(required = false) String andar,
                                  @RequestParam(defaultValue = "nome") String ordem, @RequestParam(required = false) String q) {
        List<Interno> todos = repositorio.internos();
        String termo = q == null ? "" : semAcento(q.trim());
        Comparator<Interno> porNome = Comparator.comparing(Interno::nome, String.CASE_INSENSITIVE_ORDER);
        Comparator<Interno> comparador = switch (ordem) {
            case "ramal" -> Comparator.comparing(Interno::ramal);
            case "setor" -> Comparator.comparing(Interno::setor, String.CASE_INSENSITIVE_ORDER).thenComparing(porNome);
            default -> porNome;
        };
        List<Interno> lista = todos.stream()
                .filter(c -> vazio(secretaria) || secretaria.equalsIgnoreCase(c.secretaria()))
                .filter(c -> vazio(andar) || andar.equalsIgnoreCase(c.andar()))
                .filter(c -> termo.isEmpty() || semAcento(c.nome() + " " + c.setor() + " " + c.ramal()).contains(termo))
                .sorted(comparador).toList();
        OpcoesInternos opcoes = new OpcoesInternos(
                todos.stream().map(Interno::secretaria).distinct().sorted().toList(),
                todos.stream().map(Interno::andar).filter(a -> a != null && !a.isBlank()).distinct().sorted().toList());
        return new ListaInternos(lista, lista.size(), todos.size(), opcoes);
    }

    @GetMapping("/externos")
    public ListaExternos externos(@RequestParam(required = false) String categoria, @RequestParam(required = false) String situacao,
                                  @RequestParam(required = false) String q) {
        List<Externo> todos = repositorio.externos();
        String termo = q == null ? "" : semAcento(q.trim());
        List<ExternoComSituacao> lista = todos.stream()
                .filter(c -> vazio(categoria) || categoria.equalsIgnoreCase(c.categoria()))
                .filter(c -> termo.isEmpty() || semAcento(c.empresa() + " " + c.servico() + " " + c.contato() + " " + c.telefone()).contains(termo))
                .map(this::comSituacao)
                .filter(c -> vazio(situacao) || situacao.equalsIgnoreCase(c.situacao()))
                .sorted(Comparator.comparingLong(ExternoComSituacao::dias)) // vencimento mais próximo primeiro
                .toList();
        List<String> categorias = todos.stream().map(Externo::categoria).distinct().sorted().toList();
        return new ListaExternos(lista, lista.size(), todos.size(), categorias);
    }

    private ExternoComSituacao comSituacao(Externo c) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(relogio), c.vence());
        if (dias < 0) return new ExternoComSituacao(c, "vencido", "Venceu em " + DIA.format(c.vence()), dias);
        if (dias <= DIAS_AVISO_CONTRATO) return new ExternoComSituacao(c, "vence", "Vence em " + dias + (dias == 1 ? " dia" : " dias"), dias);
        return new ExternoComSituacao(c, "emdia", "Em dia até " + DIA.format(c.vence()), dias);
    }

    @PostMapping("/internos")
    @ResponseStatus(HttpStatus.CREATED)
    public InternoSalvo adicionarInterno(@RequestBody Interno c, @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave) {
        edicao.exigir(chave);
        List<String> erros = new ArrayList<>();
        if (vazio(c.nome())) erros.add("Informe o nome.");
        if (vazio(c.setor())) erros.add("Escolha o setor.");
        if (c.ramal() == null || !RAMAL.matcher(c.ramal().trim()).matches()) erros.add("O ramal tem 4 números (ex.: 2315).");
        if (!vazio(c.email()) && !EMAIL.matcher(c.email().trim()).matches()) erros.add("E-mail inválido.");
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);
        // ramais costumam ser compartilhados: não é erro, só avisa com quem
        List<String> divide = repositorio.internos().stream().filter(o -> o.ramal().equals(c.ramal().trim())).map(Interno::nome).toList();
        Interno novo = new Interno(c.nome().trim(), c.setor().trim(), vazio(c.andar()) ? "—" : c.andar().trim(), c.ramal().trim(),
                vazio(c.email()) ? null : c.email().trim());
        repositorio.adicionar(novo);
        return new InternoSalvo(novo, divide);
    }

    @PostMapping("/externos")
    @ResponseStatus(HttpStatus.CREATED)
    public Externo adicionarExterno(@RequestBody Externo c, @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave) {
        edicao.exigir(chave);
        List<String> erros = new ArrayList<>();
        if (vazio(c.empresa())) erros.add("Informe a empresa.");
        if (vazio(c.servico())) erros.add("Informe o serviço.");
        if (vazio(c.categoria())) erros.add("Escolha ou digite a categoria.");
        if (vazio(c.telefone()) && vazio(c.email())) erros.add("Informe telefone ou e-mail.");
        if (!vazio(c.email()) && !EMAIL.matcher(c.email().trim()).matches()) erros.add("E-mail inválido.");
        if (c.vence() == null) erros.add("Informe o vencimento do contrato.");
        if (!erros.isEmpty()) throw new RegraVioladaException(erros);
        Externo novo = new Externo(c.categoria().trim(), c.empresa().trim(), c.servico().trim(), limpo(c.contato()), limpo(c.telefone()),
                limpo(c.email()), limpo(c.contrato()), c.vence());
        repositorio.adicionar(novo);
        return novo;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpo(String s) {
        return vazio(s) ? null : s.trim();
    }

    static String semAcento(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
