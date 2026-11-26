package helplixo.web.processo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Um processo, lido de processes/{id}.json do Helplixo atual.
 * O conteúdo é uma lista de blocos (parágrafo, título, listas, código, link, imagem, informação, alerta, nota).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Processo(
        UUID id,
        @JsonAlias("folderId") UUID pasta,
        @JsonAlias("title") String titulo,
        @JsonAlias("description") String descricao,
        @JsonAlias("objective") String objetivo,
        @JsonAlias("prerequisites") String preRequisitos,
        @JsonAlias("responsible") String responsavel,
        @JsonAlias("category") String categoria,
        List<String> tags,
        @JsonAlias("steps") List<Passo> passos,
        @JsonAlias("content") List<Bloco> conteudo,
        @JsonAlias("observations") String observacoes,
        List<String> links,
        @JsonAlias("createdAt") LocalDateTime criadoEm,
        @JsonAlias("updatedAt") LocalDateTime atualizadoEm,
        @JsonAlias("archived") boolean arquivado,
        @JsonAlias("deleted") boolean apagado) {

    public Processo {
        tags = tags == null ? List.of() : List.copyOf(tags);
        passos = passos == null ? List.of() : List.copyOf(passos);
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
        links = links == null ? List.of() : List.copyOf(links);
    }

    boolean aparece() {
        return !apagado && !arquivado;
    }

    /** Tudo o que pode ser procurado, num texto só, em minúsculas. */
    String textoParaBusca() {
        StringBuilder t = new StringBuilder();
        for (String s : new String[] {titulo, descricao, objetivo, categoria, responsavel, observacoes}) {
            if (s != null) t.append(s).append(' ');
        }
        tags.forEach(s -> t.append(s).append(' '));
        conteudo.forEach(b -> { if (b.texto() != null) t.append(b.texto()).append(' '); });
        passos.forEach(p -> t.append(p.titulo()).append(' ').append(p.descricao()).append(' '));
        return t.toString().toLowerCase();
    }

    /** Bloco de conteúdo. Listas guardam um item por linha em "texto". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bloco(UUID id, @JsonAlias("type") TipoBloco tipo, @JsonAlias("text") String texto, String url,
                        @JsonAlias("bold") boolean negrito, @JsonAlias("italic") boolean italico, @JsonAlias("underline") boolean sublinhado) {}

    /** Passo numerado (formato antigo do Helplixo). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Passo(UUID id, @JsonAlias("number") int numero, @JsonAlias("title") String titulo,
                        @JsonAlias("description") String descricao, @JsonAlias("expectedResult") String resultadoEsperado,
                        List<String> checklist) {}

    /** Os mesmos tipos do Helplixo atual. */
    public enum TipoBloco { PARAGRAPH, HEADING, BULLET_LIST, NUMBERED_LIST, CODE, LINK, IMAGE, INFO, WARNING, NOTE }
}
