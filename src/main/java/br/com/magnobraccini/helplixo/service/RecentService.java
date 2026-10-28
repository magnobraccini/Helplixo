package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;
import br.com.magnobraccini.helplixo.repository.RecentRepository;

import java.util.List;
import java.util.UUID;

public final class RecentService {
    private final RecentRepository recentRepository;
    private final ProcessRepository processRepository;

    public RecentService(RecentRepository recentRepository, ProcessRepository processRepository) {
        this.recentRepository = recentRepository;
        this.processRepository = processRepository;
    }

    public void registerOpened(UUID processId) {
        if (processId != null) {
            recentRepository.register(processId);
        }
    }

    public List<ProcessDocument> findRecentProcesses(int limit) {
        return recentRepository.findRecent(limit).stream()
                .map(item -> processRepository.findById(item.getProcessId()).orElse(null))
                .filter(process -> process != null && !process.isDeleted())
                .toList();
    }

    public void clear() {
        recentRepository.clear();
    }
}
