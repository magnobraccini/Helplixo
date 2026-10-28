package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.event.EventBus;
import br.com.magnobraccini.helplixo.event.FolderCreatedEvent;
import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.repository.FolderRepository;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FolderService {
    private final FolderRepository repository;
    private final ProcessRepository processRepository;
    private final EventBus eventBus;
    private final LogService logService;

    public FolderService(FolderRepository repository, ProcessRepository processRepository, EventBus eventBus,
                          LogService logService) {
        this.repository = repository;
        this.processRepository = processRepository;
        this.eventBus = eventBus;
        this.logService = logService;
    }

    public FolderService(FolderRepository repository, ProcessRepository processRepository, EventBus eventBus) {
        this(repository, processRepository, eventBus, LogService.disabled());
    }

    public FolderService(FolderRepository repository, EventBus eventBus) {
        this(repository, null, eventBus, LogService.disabled());
    }

    public List<Folder> findAll() {
        return repository.findAll();
    }

    public Optional<Folder> findById(UUID id) {
        return repository.findById(id);
    }

    public List<Folder> findFavorites() {
        return repository.findAll().stream()
                .filter(Folder::isFavorite)
                .toList();
    }

    public Folder create(String name, String description, UUID parentId) {
        validateName(name);
        Folder folder = Folder.create(name.trim(), safe(description), parentId);
        repository.save(folder);
        eventBus.publish(new FolderCreatedEvent(folder.getId()));
        logService.logCreate("Pasta criada: " + folder.getName() + " (id=" + folder.getId() + ")");
        return folder;
    }

    public Folder rename(UUID id, String newName) {
        validateName(newName);
        Folder folder = requireFolder(id);
        String oldName = folder.getName();
        folder.setName(newName.trim());
        folder.setUpdatedAt(LocalDateTime.now());
        Folder saved = repository.save(folder);
        logService.logEdit("Pasta renomeada: '" + oldName + "' -> '" + saved.getName() + "'");
        return saved;
    }

    /** Move a seção inteira; processos e subseções permanecem ligados à mesma árvore. */
    public Folder move(UUID id, UUID newParentId) {
        Folder folder = requireFolder(id);
        List<Folder> all = repository.findAll();
        if (newParentId != null && all.stream().noneMatch(candidate -> newParentId.equals(candidate.getId()))) {
            throw new IllegalArgumentException("A seção de destino não existe.");
        }
        if (id.equals(newParentId) || isDescendant(newParentId, id, all)) {
            throw new IllegalArgumentException("Uma seção não pode ser movida para dentro dela mesma ou de uma subseção.");
        }
        folder.setParentId(newParentId);
        folder.setUpdatedAt(LocalDateTime.now());
        Folder saved = repository.save(folder);
        logService.logEdit("Seção movida: " + saved.getName() + " (id=" + saved.getId() + ")");
        return saved;
    }

    public Folder toggleFavorite(UUID id) {
        Folder folder = requireFolder(id);
        folder.setFavorite(!folder.isFavorite());
        folder.setUpdatedAt(LocalDateTime.now());
        return repository.save(folder);
    }

    public void delete(UUID id) {
        Folder folder = requireFolder(id);
        boolean hasChildren = repository.findAll().stream()
                .anyMatch(candidate -> id.equals(candidate.getParentId()));
        boolean hasProcesses = processRepository != null && !processRepository.findByFolderId(id).isEmpty();
        if (hasChildren || hasProcesses) {
            throw new IllegalArgumentException("A pasta precisa estar vazia antes de ser excluída.");
        }
        repository.delete(folder.getId());
        logService.logDelete("Pasta excluída: " + folder.getName() + " (id=" + folder.getId() + ")");
    }

    public long count() {
        return repository.count();
    }

    public long countFavorites() {
        return findFavorites().size();
    }

    private Folder requireFolder(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pasta não encontrada."));
    }

    private boolean isDescendant(UUID candidateId, UUID ancestorId, List<Folder> all) {
        if (candidateId == null) {
            return false;
        }
        java.util.Map<UUID, Folder> byId = new java.util.HashMap<>();
        all.forEach(folder -> byId.put(folder.getId(), folder));
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        UUID currentId = candidateId;
        while (currentId != null && visited.add(currentId)) {
            if (ancestorId.equals(currentId)) {
                return true;
            }
            Folder current = byId.get(currentId);
            currentId = current == null ? null : current.getParentId();
        }
        return false;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da pasta é obrigatório.");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("O nome da pasta deve ter no máximo 100 caracteres.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
