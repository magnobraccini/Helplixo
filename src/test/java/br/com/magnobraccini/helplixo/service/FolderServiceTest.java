package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.event.EventBus;
import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.repository.FolderRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FolderServiceTest {
    @Test
    void shouldCreateValidatedFolder() {
        InMemoryFolderRepository repository = new InMemoryFolderRepository();
        FolderService service = new FolderService(repository, new EventBus());
        Folder folder = service.create("Windows", "", null);
        assertEquals("Windows", folder.getName());
        assertEquals(1, service.count());
    }

    @Test
    void shouldRejectBlankName() {
        FolderService service = new FolderService(new InMemoryFolderRepository(), new EventBus());
        assertThrows(IllegalArgumentException.class, () -> service.create(" ", "", null));
    }

    private static final class InMemoryFolderRepository implements FolderRepository {
        private final List<Folder> folders = new ArrayList<>();
        public List<Folder> findAll() { return List.copyOf(folders); }
        public Optional<Folder> findById(UUID id) { return folders.stream().filter(f -> f.getId().equals(id)).findFirst(); }
        public Folder save(Folder folder) { folders.add(folder); return folder; }
        public void delete(UUID id) { folders.removeIf(folder -> folder.getId().equals(id)); }
        public long count() { return folders.size(); }
    }
}
