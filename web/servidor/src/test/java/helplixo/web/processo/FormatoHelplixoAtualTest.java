package helplixo.web.processo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lê arquivos exatamente como o Helplixo atual (Swing) grava, com campos extras
 * que o servidor novo não usa (icon, favorite, customIcon...). Nada pode quebrar.
 * Usa uma pasta temporária: nunca a pasta de dados de verdade.
 */
class FormatoHelplixoAtualTest {

    @TempDir
    Path pasta;

    @Test
    void lePastaEProcessoGravadosPeloProgramaAtual() throws Exception {
        Files.writeString(pasta.resolve("folders.json"), """
                [ {
                  "id" : "2af1866f-8b53-4b05-84a6-0342f3540469",
                  "parentId" : null,
                  "name" : "aaa",
                  "description" : "",
                  "icon" : "folder",
                  "createdAt" : "2026-11-19T13:49:48.1702408",
                  "updatedAt" : "2026-11-19T13:49:48.1702408",
                  "visible" : true,
                  "deleted" : false,
                  "order" : 0
                } ]
                """, StandardCharsets.UTF_8);
        Files.createDirectories(pasta.resolve("processes"));
        Files.writeString(pasta.resolve("processes/a432774e-018d-4d60-891c-4e838825c65a.json"), """
                {
                  "id" : "a432774e-018d-4d60-891c-4e838825c65a",
                  "folderId" : "2af1866f-8b53-4b05-84a6-0342f3540469",
                  "title" : "aaaaaa",
                  "description" : "",
                  "objective" : "",
                  "prerequisites" : "",
                  "responsible" : "",
                  "createdAt" : "2026-11-19T13:49:51.8342679",
                  "updatedAt" : "2026-11-23T10:57:58.334679",
                  "category" : "",
                  "tags" : [ ],
                  "steps" : [ ],
                  "stepsHtml" : "",
                  "content" : [ ],
                  "observations" : "",
                  "links" : [ ],
                  "favorite" : true,
                  "archived" : false,
                  "deleted" : false,
                  "customIcon" : "document"
                }
                """, StandardCharsets.UTF_8);

        ProcessoRepositorio repo = new ProcessoRepositorio(new ObjectMapper().findAndRegisterModules(), pasta.toUri().toString());

        assertThat(repo.pastas()).extracting(Pasta::nome).containsExactly("aaa");
        assertThat(repo.processos()).singleElement().satisfies(p -> {
            assertThat(p.titulo()).isEqualTo("aaaaaa");
            assertThat(p.pasta()).isEqualTo(repo.pastas().get(0).id());
            assertThat(p.atualizadoEm()).hasYear(2026).hasMonthValue(11).hasDayOfMonth(23);
        });
    }

    @Test
    void pastaSemArquivosNaoQuebra() {
        ProcessoRepositorio repo = new ProcessoRepositorio(new ObjectMapper().findAndRegisterModules(), pasta.toUri().toString());
        assertThat(repo.pastas()).isEmpty();
        assertThat(repo.processos()).isEmpty();
    }
}
