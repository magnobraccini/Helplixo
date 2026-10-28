package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;
import br.com.magnobraccini.helplixo.util.TextNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class JsonProcessRepository implements ProcessRepository {
    private final Path processesDirectory;
    private final ObjectMapper objectMapper;

    public JsonProcessRepository(Path dataDirectory, ObjectMapper objectMapper) {
        this.processesDirectory = dataDirectory.resolve("processes");
        this.objectMapper = objectMapper;
        ensureDirectory();
    }

    @Override
    public Optional<ProcessDocument> findById(UUID id) {
        Path file = processesDirectory.resolve(id + ".json");
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        return Optional.of(read(file));
    }

    @Override
    public List<ProcessDocument> findByFolderId(UUID folderId) {
        return streamProcesses()
                .filter(process -> folderId.equals(process.getFolderId()))
                .filter(process -> !process.isDeleted() && !process.isArchived())
                .sorted(Comparator.comparing(ProcessDocument::getTitle))
                .toList();
    }

    @Override
    public List<ProcessDocument> findRecent(int limit) {
        return streamProcesses()
                .filter(process -> !process.isDeleted())
                .sorted(Comparator.comparing(ProcessDocument::getUpdatedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<ProcessDocument> findFavorites(int limit) {
        return streamProcesses()
                .filter(process -> !process.isDeleted() && !process.isArchived() && process.isFavorite())
                .sorted(Comparator.comparing(ProcessDocument::getTitle))
                .limit(limit)
                .toList();
    }

    @Override
    public List<ProcessDocument> search(String query, int limit) {
        String normalizedQuery = TextNormalizer.normalize(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        return streamProcesses()
                .filter(process -> !process.isDeleted())
                .filter(process -> searchableText(process).contains(normalizedQuery))
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized ProcessDocument save(ProcessDocument process) {
        try {
            ensureDirectory();
            Path target = processesDirectory.resolve(process.getId() + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), process);
            return process;
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível salvar o processo.", exception);
        }
    }

    @Override
    public void delete(UUID id) {
        findById(id).ifPresent(process -> {
            process.setDeleted(true);
            save(process);
        });
    }

    @Override
    public long count() {
        return streamProcesses().filter(process -> !process.isDeleted()).count();
    }

    @Override
    public long countFavorites() {
        return streamProcesses().filter(process -> !process.isDeleted() && process.isFavorite()).count();
    }

    private Stream<ProcessDocument> streamProcesses() {
        try {
            ensureDirectory();
            try (Stream<Path> paths = Files.list(processesDirectory)) {
                return paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(this::read)
                        .toList()
                        .stream();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível listar os processos.", exception);
        }
    }

    private ProcessDocument read(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), ProcessDocument.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Arquivo de processo inválido: " + path.getFileName(), exception);
        }
    }

    private String searchableText(ProcessDocument process) {
        return TextNormalizer.normalize(String.join(" ",
                safe(process.getTitle()),
                safe(process.getDescription()),
                safe(process.getObjective()),
                safe(process.getResponsible()),
                safe(process.getObservations()),
                String.join(" ", process.getTags())));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(processesDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível criar a pasta de processos.", exception);
        }
    }
}
