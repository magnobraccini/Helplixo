package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.Tool;
import br.com.magnobraccini.helplixo.repository.ToolRepository;
import br.com.magnobraccini.helplixo.util.TextNormalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class JsonToolRepository extends AbstractJsonListRepository<Tool, UUID> implements ToolRepository {
    private static final TypeReference<List<Tool>> TOOL_LIST = new TypeReference<>() { };

    public JsonToolRepository(Path dataDirectory, ObjectMapper objectMapper) {
        super(dataDirectory, "tools.json", objectMapper, TOOL_LIST);
    }

    @Override
    protected UUID idOf(Tool entity) {
        return entity.getId();
    }

    @Override
    protected boolean isDeleted(Tool entity) {
        return entity.isDeleted();
    }

    @Override
    protected void markDeleted(Tool entity) {
        entity.setDeleted(true);
    }

    @Override
    protected List<Tool> postProcess(List<Tool> visible) {
        return visible.stream()
                .sorted(Comparator.comparing(Tool::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<Tool> search(String query, int limit) {
        String normalized = TextNormalizer.normalize(query);
        return findAll().stream()
                .filter(tool -> matches(tool, normalized))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Tool> findFavorites(int limit) {
        return findAll().stream().filter(Tool::isFavorite).limit(limit).toList();
    }

    private boolean matches(Tool tool, String normalizedQuery) {
        return TextNormalizer.normalize(tool.getName()).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(tool.getDescription())).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(tool.getCategory())).contains(normalizedQuery);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
