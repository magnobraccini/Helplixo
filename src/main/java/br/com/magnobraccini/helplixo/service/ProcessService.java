package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.event.EventBus;
import br.com.magnobraccini.helplixo.event.ProcessCreatedEvent;
import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ProcessService {
    private final ProcessRepository repository;
    private final EventBus eventBus;
    private final LogService logService;

    public ProcessService(ProcessRepository repository, EventBus eventBus, LogService logService) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.logService = logService;
    }

    public ProcessService(ProcessRepository repository, EventBus eventBus) {
        this(repository, eventBus, LogService.disabled());
    }

    public Optional<ProcessDocument> findById(UUID id) {
        return repository.findById(id);
    }

    public List<ProcessDocument> findByFolderId(UUID folderId) {
        return repository.findByFolderId(folderId);
    }

    public List<ProcessDocument> findRecent(int limit) {
        return repository.findRecent(limit);
    }

    public List<ProcessDocument> findFavorites(int limit) {
        return repository.findFavorites(limit);
    }

    public ProcessDocument create(UUID folderId, String title, String description) {
        if (folderId == null) {
            throw new IllegalArgumentException("Selecione uma pasta para o processo.");
        }
        validateTitle(title);
        ProcessDocument process = ProcessDocument.create(folderId, title.trim(), safe(description));
        repository.save(process);
        eventBus.publish(new ProcessCreatedEvent(process.getId()));
        logService.logCreate("Processo criado: " + process.getTitle() + " (id=" + process.getId() + ")");
        return process;
    }

    public ProcessDocument save(ProcessDocument process) {
        validateTitle(process.getTitle());
        process.setUpdatedAt(LocalDateTime.now());
        ProcessDocument saved = repository.save(process);
        logService.logEdit("Processo salvo: " + saved.getTitle() + " (id=" + saved.getId() + ")");
        return saved;
    }

    /** Move o processo sem recriar seu conteúdo nem alterar seu identificador. */
    public ProcessDocument moveToFolder(UUID processId, UUID folderId) {
        if (folderId == null) {
            throw new IllegalArgumentException("Selecione uma seção de destino.");
        }
        ProcessDocument process = requireProcess(processId);
        process.setFolderId(folderId);
        return save(process);
    }

    public ProcessDocument toggleFavorite(UUID id) {
        ProcessDocument process = requireProcess(id);
        process.setFavorite(!process.isFavorite());
        return save(process);
    }

    public void delete(UUID id) {
        ProcessDocument process = requireProcess(id);
        repository.delete(id);
        logService.logDelete("Processo excluído: " + process.getTitle() + " (id=" + process.getId() + ")");
    }

    public long count() {
        return repository.count();
    }

    public long countFavorites() {
        return repository.countFavorites();
    }

    private ProcessDocument requireProcess(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado."));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título do processo é obrigatório.");
        }
        if (title.trim().length() > 160) {
            throw new IllegalArgumentException("O título deve ter no máximo 160 caracteres.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
