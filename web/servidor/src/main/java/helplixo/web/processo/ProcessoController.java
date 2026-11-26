package helplixo.web.processo;

import helplixo.web.RegraVioladaException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de processos (só leitura):
 * GET /api/processos/arvore      a árvore de pastas com os processos
 * GET /api/processos/{id}        um processo inteiro, com o caminho de pastas
 * GET /api/processos?q=sigem     busca por título, descrição, tags e conteúdo (ignora acentos)
 */
@RestController
@RequestMapping("/api/processos")
public class ProcessoController {

    private final ProcessoRepositorio repositorio;

    public ProcessoController(ProcessoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public record ItemProcesso(UUID id, String titulo, LocalDateTime atualizadoEm) {}

    /** Uma pasta da árvore. total = quantos processos há nela e nas subpastas. */
    public record NoPasta(UUID id, String nome, List<NoPasta> pastas, List<ItemProcesso> processos, int total) {}

    /** semPasta = processos cuja pasta não existe mais (para nada sumir sem aviso). */
    public record Arvore(List<NoPasta> pastas, List<ItemProcesso> semPasta) {}

    public record DetalheProcesso(Processo processo, List<String> caminho) {}

    public record ItemBusca(UUID id, String titulo, String caminho) {}

    @GetMapping("/arvore")
    public Arvore arvore() {
        List<Pasta> pastas = repositorio.pastas();
        List<Processo> processos = repositorio.processos();
        List<UUID> idsPastas = pastas.stream().map(Pasta::id).toList();
        List<ItemProcesso> semPasta = processos.stream()
                .filter(p -> p.pasta() == null || !idsPastas.contains(p.pasta()))
                .map(ProcessoController::item).sorted(Comparator.comparing(ItemProcesso::titulo)).toList();
        return new Arvore(filhas(null, pastas, processos), semPasta);
    }

    private static List<NoPasta> filhas(UUID pai, List<Pasta> pastas, List<Processo> processos) {
        return pastas.stream()
                .filter(p -> Objects.equals(p.pastaPai(), pai))
                .sorted(Comparator.comparingInt(Pasta::ordem).thenComparing(Pasta::nome))
                .map(p -> {
                    List<NoPasta> sub = filhas(p.id(), pastas, processos);
                    List<ItemProcesso> aqui = processos.stream().filter(x -> p.id().equals(x.pasta()))
                            .map(ProcessoController::item).sorted(Comparator.comparing(ItemProcesso::titulo)).toList();
                    int total = aqui.size() + sub.stream().mapToInt(NoPasta::total).sum();
                    return new NoPasta(p.id(), p.nome(), sub, aqui, total);
                })
                .toList();
    }

    private static ItemProcesso item(Processo p) {
        return new ItemProcesso(p.id(), p.titulo(), p.atualizadoEm());
    }

    @GetMapping("/{id}")
    public DetalheProcesso detalhe(@PathVariable UUID id) {
        Processo p = repositorio.porId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo " + id + " não encontrado."));
        return new DetalheProcesso(p, caminho(p.pasta()));
    }

    /** Nomes das pastas, da raiz até a pasta do processo. */
    private List<String> caminho(UUID pasta) {
        List<String> nomes = new ArrayList<>();
        UUID atual = pasta;
        for (int guarda = 0; atual != null && guarda < 50; guarda++) { // guarda: evita laço se o arquivo tiver um ciclo
            Pasta p = repositorio.pasta(atual).orElse(null);
            if (p == null) break;
            nomes.add(p.nome());
            atual = p.pastaPai();
        }
        Collections.reverse(nomes);
        return nomes;
    }

    @GetMapping
    public List<ItemBusca> buscar(@RequestParam String q) {
        String termo = semAcento(q.trim());
        if (termo.length() < 2) {
            throw new RegraVioladaException(List.of("Digite pelo menos 2 letras para procurar."));
        }
        return repositorio.processos().stream()
                .filter(p -> semAcento(p.textoParaBusca()).contains(termo))
                .sorted(Comparator.comparing(Processo::titulo))
                .map(p -> new ItemBusca(p.id(), p.titulo(), String.join(" › ", caminho(p.pasta()))))
                .toList();
    }

    static String semAcento(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
