package helplixo.web.processo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

/**
 * Lê pastas e processos no formato do Helplixo atual (folders.json + processes/*.json).
 * SÓ LEITURA: esta classe nunca grava, move ou apaga nada na pasta de dados.
 * A raiz vem de helplixo.dados.processos (classpath com a amostra, ou file: com uma cópia de teste).
 */
@Repository
public class ProcessoRepositorio {

    private final List<Pasta> pastas;
    private final List<Processo> processos;

    public ProcessoRepositorio(ObjectMapper json, @Value("${helplixo.dados.processos}") String raiz) {
        String base = raiz.endsWith("/") ? raiz.substring(0, raiz.length() - 1) : raiz;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource arquivoPastas = resolver.getResource(base + "/folders.json");
            this.pastas = arquivoPastas.exists() ? ler(json, arquivoPastas, new TypeReference<List<Pasta>>() {}) : List.of();
            List<Processo> lidos = new ArrayList<>();
            for (Resource r : resolver.getResources(base + "/processes/*.json")) {
                lidos.add(ler(json, r, new TypeReference<Processo>() {}));
            }
            this.processos = List.copyOf(lidos);
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui ler os processos em " + base, e);
        }
    }

    private static <T> T ler(ObjectMapper json, Resource r, TypeReference<T> tipo) throws IOException {
        try (InputStream entrada = r.getInputStream()) {
            return json.readValue(entrada, tipo);
        }
    }

    /** Pastas que aparecem (não apagadas e visíveis). */
    public List<Pasta> pastas() {
        return pastas.stream().filter(Pasta::aparece).toList();
    }

    /** Processos que aparecem (não apagados nem arquivados). */
    public List<Processo> processos() {
        return processos.stream().filter(Processo::aparece).toList();
    }

    public Optional<Processo> porId(UUID id) {
        return processos().stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public Optional<Pasta> pasta(UUID id) {
        return id == null ? Optional.empty() : pastas.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
