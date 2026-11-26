package helplixo.web.processo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Uma pasta da árvore, lida do folders.json do Helplixo atual (os nomes em inglês do arquivo viram aliases). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Pasta(
        UUID id,
        @JsonAlias("parentId") UUID pastaPai,
        @JsonAlias("name") String nome,
        @JsonAlias("description") String descricao,
        @JsonAlias("visible") Boolean visivel,
        @JsonAlias("deleted") boolean apagada,
        @JsonAlias("order") int ordem) {

    boolean aparece() {
        return !apagada && (visivel == null || visivel);
    }
}
