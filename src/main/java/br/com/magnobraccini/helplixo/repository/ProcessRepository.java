package br.com.magnobraccini.helplixo.repository;

import br.com.magnobraccini.helplixo.model.ProcessDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository {
    Optional<ProcessDocument> findById(UUID id);
    List<ProcessDocument> findByFolderId(UUID folderId);
    List<ProcessDocument> findRecent(int limit);
    List<ProcessDocument> findFavorites(int limit);
    List<ProcessDocument> search(String query, int limit);
    ProcessDocument save(ProcessDocument process);
    void delete(UUID id);
    long count();
    long countFavorites();
}
