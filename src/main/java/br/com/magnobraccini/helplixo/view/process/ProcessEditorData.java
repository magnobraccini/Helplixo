package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.model.ContentBlock;

import java.util.List;

public record ProcessEditorData(
        String title,
        String description,
        String objective,
        String prerequisites,
        String responsible,
        String tags,
        String observations,
        String links,
        String steps,
        List<ContentBlock> content) {
}
