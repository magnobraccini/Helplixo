package br.com.magnobraccini.helplixo.repository;

import br.com.magnobraccini.helplixo.model.RecentItem;

import java.util.List;
import java.util.UUID;

public interface RecentRepository {
    List<RecentItem> findRecent(int limit);
    void register(UUID processId);
    void clear();
}
