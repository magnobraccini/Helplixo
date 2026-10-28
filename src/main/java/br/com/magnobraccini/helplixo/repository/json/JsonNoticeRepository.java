package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.repository.NoticeRepository;
import br.com.magnobraccini.helplixo.util.TextNormalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class JsonNoticeRepository extends AbstractJsonListRepository<Notice, UUID> implements NoticeRepository {
    private static final TypeReference<List<Notice>> NOTICE_LIST = new TypeReference<>() { };

    public JsonNoticeRepository(Path dataDirectory, ObjectMapper objectMapper) {
        super(dataDirectory, "notices.json", objectMapper, NOTICE_LIST);
    }

    @Override
    protected UUID idOf(Notice entity) {
        return entity.getId();
    }

    @Override
    protected boolean isDeleted(Notice entity) {
        return entity.isDeleted();
    }

    @Override
    protected void markDeleted(Notice entity) {
        entity.setDeleted(true);
    }

    @Override
    protected List<Notice> postProcess(List<Notice> visible) {
        return visible.stream()
                .sorted(Comparator.comparing(Notice::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public List<Notice> search(String query, int limit) {
        String normalized = TextNormalizer.normalize(query);
        return findAll().stream()
                .filter(notice -> matches(notice, normalized))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Notice> findFavorites(int limit) {
        return findAll().stream().filter(Notice::isFavorite).limit(limit).toList();
    }

    private boolean matches(Notice notice, String normalizedQuery) {
        return TextNormalizer.normalize(notice.getTitle()).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(notice.getMessage())).contains(normalizedQuery)
                || TextNormalizer.normalize(notice.getCategory().getDisplayName()).contains(normalizedQuery);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
