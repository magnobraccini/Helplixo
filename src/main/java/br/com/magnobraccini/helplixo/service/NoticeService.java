package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.model.NoticeCategory;
import br.com.magnobraccini.helplixo.repository.NoticeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class NoticeService {
    private final NoticeRepository repository;
    private final LogService logService;

    public NoticeService(NoticeRepository repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    public NoticeService(NoticeRepository repository) {
        this(repository, LogService.disabled());
    }

    public List<Notice> findAll() {
        return repository.findAll();
    }

    public List<Notice> search(String query) {
        return (query == null || query.isBlank()) ? findAll() : repository.search(query, 200);
    }

    public List<Notice> search(String query, NoticeCategory category) {
        List<Notice> base = search(query);
        if (category == null) {
            return base;
        }
        return base.stream().filter(notice -> notice.getCategory() == category).toList();
    }

    public List<Notice> findFavorites(int limit) {
        return repository.findFavorites(limit);
    }

    public long count() {
        return repository.count();
    }

    public long countFavorites() {
        return repository.findFavorites(Integer.MAX_VALUE).size();
    }

    public Notice create(String title, String message, NoticeCategory category, String createdBy) {
        validateTitle(title);
        Notice notice = Notice.create(title.trim(), safe(message), category == null ? NoticeCategory.SISTEMA : category,
                safe(createdBy));
        repository.save(notice);
        logService.logCreate("Aviso criado: " + notice.getTitle() + " (" + notice.getCategory().getDisplayName() + ")");
        return notice;
    }

    public Notice update(UUID id, String title, String message, NoticeCategory category, String createdBy) {
        validateTitle(title);
        Notice notice = requireNotice(id);
        notice.setTitle(title.trim());
        notice.setMessage(safe(message));
        notice.setCategory(category == null ? notice.getCategory() : category);
        notice.setCreatedBy(safe(createdBy));
        notice.setUpdatedAt(LocalDateTime.now());
        Notice saved = repository.save(notice);
        logService.logEdit("Aviso editado: " + saved.getTitle());
        return saved;
    }

    public Notice toggleFavorite(UUID id) {
        Notice notice = requireNotice(id);
        notice.setFavorite(!notice.isFavorite());
        notice.setUpdatedAt(LocalDateTime.now());
        return repository.save(notice);
    }

    public void delete(UUID id) {
        Notice notice = requireNotice(id);
        repository.delete(id);
        logService.logDelete("Aviso excluído: " + notice.getTitle());
    }

    private Notice requireNotice(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Aviso não encontrado."));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título é obrigatório.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
