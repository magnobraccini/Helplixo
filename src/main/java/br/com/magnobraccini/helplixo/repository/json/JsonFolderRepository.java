package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.repository.FolderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class JsonFolderRepository extends AbstractJsonListRepository<Folder, UUID> implements FolderRepository {
    private static final TypeReference<List<Folder>> FOLDER_LIST = new TypeReference<>() { };

    public JsonFolderRepository(Path dataDirectory, ObjectMapper objectMapper) {
        super(dataDirectory, "folders.json", objectMapper, FOLDER_LIST);
    }

    @Override
    protected UUID idOf(Folder entity) {
        return entity.getId();
    }

    @Override
    protected boolean isDeleted(Folder entity) {
        return entity.isDeleted() || !entity.isVisible();
    }

    @Override
    protected void markDeleted(Folder entity) {
        entity.setDeleted(true);
    }

    @Override
    protected List<Folder> postProcess(List<Folder> visible) {
        return visible.stream()
                .sorted(Comparator.comparingInt(Folder::getOrder).thenComparing(Folder::getName))
                .toList();
    }
}
