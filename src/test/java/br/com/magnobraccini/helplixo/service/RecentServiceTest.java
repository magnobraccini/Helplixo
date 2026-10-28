package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.model.RecentItem;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;
import br.com.magnobraccini.helplixo.repository.RecentRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentServiceTest {
    @Test
    void shouldKeepMostRecentlyOpenedProcessFirst() {
        InMemoryProcessRepository processRepository = new InMemoryProcessRepository();
        ProcessDocument first = processRepository.add("Primeiro");
        ProcessDocument second = processRepository.add("Segundo");
        RecentService service = new RecentService(new InMemoryRecentRepository(), processRepository);

        service.registerOpened(first.getId());
        service.registerOpened(second.getId());
        service.registerOpened(first.getId());

        List<ProcessDocument> recent = service.findRecentProcesses(10);
        assertEquals(List.of("Primeiro", "Segundo"), recent.stream().map(ProcessDocument::getTitle).toList());
    }

    private static final class InMemoryRecentRepository implements RecentRepository {
        private final LinkedList<RecentItem> items = new LinkedList<>();

        public List<RecentItem> findRecent(int limit) {
            return items.stream().limit(limit).toList();
        }

        public void register(UUID processId) {
            items.removeIf(item -> processId.equals(item.getProcessId()));
            items.addFirst(RecentItem.create(processId));
        }

        public void clear() {
            items.clear();
        }
    }

    private static final class InMemoryProcessRepository implements ProcessRepository {
        private final List<ProcessDocument> processes = new ArrayList<>();

        ProcessDocument add(String title) {
            ProcessDocument process = ProcessDocument.create(UUID.randomUUID(), title, "");
            processes.add(process);
            return process;
        }

        public Optional<ProcessDocument> findById(UUID id) {
            return processes.stream().filter(process -> process.getId().equals(id)).findFirst();
        }

        public List<ProcessDocument> findByFolderId(UUID folderId) { return List.of(); }
        public List<ProcessDocument> findRecent(int limit) { return processes.stream().limit(limit).toList(); }
        public List<ProcessDocument> findFavorites(int limit) { return List.of(); }
        public List<ProcessDocument> search(String query, int limit) { return List.of(); }
        public ProcessDocument save(ProcessDocument process) { return process; }
        public void delete(UUID id) { }
        public long count() { return processes.size(); }
        public long countFavorites() { return 0; }
    }
}
