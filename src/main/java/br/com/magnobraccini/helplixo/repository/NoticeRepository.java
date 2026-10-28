package br.com.magnobraccini.helplixo.repository;

import br.com.magnobraccini.helplixo.model.Notice;

import java.util.List;
import java.util.UUID;

public interface NoticeRepository extends BaseRepository<Notice, UUID> {
    List<Notice> search(String query, int limit);

    List<Notice> findFavorites(int limit);
}
