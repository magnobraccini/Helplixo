package br.com.magnobraccini.helplixo.repository;

import br.com.magnobraccini.helplixo.model.Tool;

import java.util.List;
import java.util.UUID;

public interface ToolRepository extends BaseRepository<Tool, UUID> {
    List<Tool> search(String query, int limit);

    List<Tool> findFavorites(int limit);
}
