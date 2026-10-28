package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.RecentItem;
import br.com.magnobraccini.helplixo.repository.RecentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class JsonRecentRepository implements RecentRepository {
    private static final TypeReference<List<RecentItem>> RECENT_LIST = new TypeReference<>() { };
    private static final int MAX_ENTRIES = 100;
    private final Path file;
    private final ObjectMapper objectMapper;

    public JsonRecentRepository(Path dataDirectory, ObjectMapper objectMapper) {
        this.file = dataDirectory.resolve("history.json");
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized List<RecentItem> findRecent(int limit) {
        return read().stream()
                .sorted(Comparator.comparing(RecentItem::getOpenedAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized void register(UUID processId) {
        List<RecentItem> items = read();
        items.removeIf(item -> processId.equals(item.getProcessId()));
        items.add(RecentItem.create(processId));
        List<RecentItem> trimmed = items.stream()
                .sorted(Comparator.comparing(RecentItem::getOpenedAt).reversed())
                .limit(MAX_ENTRIES)
                .toList();
        write(trimmed);
    }

    @Override
    public synchronized void clear() {
        write(List.of());
    }

    private List<RecentItem> read() {
        try {
            if (Files.notExists(file)) {
                return new ArrayList<>();
            }
            return new ArrayList<>(objectMapper.readValue(file.toFile(), RECENT_LIST));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o histórico.", exception);
        }
    }

    private void write(List<RecentItem> items) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), items);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível salvar o histórico.", exception);
        }
    }
}
