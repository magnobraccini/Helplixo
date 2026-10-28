package br.com.magnobraccini.helplixo.model;

import java.util.UUID;

public class ContentBlock {
    private UUID id;
    private ContentBlockType type;
    private String text;
    /** Usado por blocos do tipo LINK (destino do link) e IMAGE (caminho/URL da imagem). */
    private String url;
    private boolean bold;
    private boolean italic;
    private boolean underline;

    public ContentBlock() {
    }

    public static ContentBlock create(ContentBlockType type) {
        ContentBlock block = new ContentBlock();
        block.id = UUID.randomUUID();
        block.type = type;
        block.text = "";
        block.url = "";
        block.bold = type == ContentBlockType.HEADING;
        return block;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ContentBlockType getType() { return type; }
    public void setType(ContentBlockType type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }
    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }
    public boolean isUnderline() { return underline; }
    public void setUnderline(boolean underline) { this.underline = underline; }
}
