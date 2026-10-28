package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.event.EventBus;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessServiceTest {
    @Test
    void shouldPersistEditionAndFavorite() {
        InMemoryProcessRepository repository = new InMemoryProcessRepository();
        ProcessService service = new ProcessService(repository, new EventBus());
        ProcessDocument process = service.create(UUID.randomUUID(), "Instalar impressora", "");

        process.setObjective("Instalar e validar");
        ProcessDocument saved = service.save(process);
        ProcessDocument favorite = service.toggleFavorite(saved.getId());

        assertEquals("Instalar e validar", favorite.getObjective());
        assertTrue(favorite.isFavorite());
        assertEquals(1, service.findFavorites(10).size());
    }

    private static final class InMemoryProcessRepository implements ProcessRepository {
        private final List<ProcessDocument> processes = new ArrayList<>();

        public Optional<ProcessDocument> findById(UUID id) {
            return processes.stream().filter(process -> process.getId().equals(id)).findFirst();
        }

        public List<ProcessDocument> findByFolderId(UUID folderId) {
            return processes.stream().filter(process -> folderId.equals(process.getFolderId())).toList();
        }

        public List<ProcessDocument> findRecent(int limit) {
            return processes.stream().limit(limit).toList();
        }

        public List<ProcessDocument> findFavorites(int limit) {
            return processes.stream().filter(ProcessDocument::isFavorite).limit(limit).toList();
        }

        public List<ProcessDocument> search(String query, int limit) {
            return processes.stream().filter(process -> process.getTitle().contains(query)).limit(limit).toList();
        }

        public ProcessDocument save(ProcessDocument process) {
            processes.removeIf(current -> current.getId().equals(process.getId()));
            processes.add(process);
            return process;
        }

        public void delete(UUID id) {
            processes.removeIf(process -> process.getId().equals(id));
        }

        public long count() {
            return processes.size();
        }

        public long countFavorites() {
            return processes.stream().filter(ProcessDocument::isFavorite).count();
        }
    }
}
