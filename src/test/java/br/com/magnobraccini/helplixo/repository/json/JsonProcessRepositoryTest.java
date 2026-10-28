package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonProcessRepositoryTest {
    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreOneJsonFilePerProcess() {
        JsonProcessRepository repository = new JsonProcessRepository(tempDirectory, JsonMapperFactory.create());
        UUID folderId = UUID.randomUUID();
        ProcessDocument process = ProcessDocument.create(folderId, "Instalar impressora", "Procedimento de teste");
        repository.save(process);

        assertTrue(repository.findById(process.getId()).isPresent());
        assertEquals(1, repository.findByFolderId(folderId).size());
        assertTrue(tempDirectory.resolve("processes").resolve(process.getId() + ".json").toFile().exists());
    }
}
