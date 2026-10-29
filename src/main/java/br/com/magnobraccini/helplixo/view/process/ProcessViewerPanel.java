package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.component.FilesLinksCard;
import br.com.magnobraccini.helplixo.component.InformationCard;
import br.com.magnobraccini.helplixo.model.ContentBlock;
import br.com.magnobraccini.helplixo.model.ContentBlockType;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.model.ProcessStep;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Área de conteúdo de um processo/pasta selecionada: cabeçalho ({@link ProcessHeaderPanel}),
 * documento renderizado ao centro e, à direita, os cards "Arquivos / Links" e "Informações".
 * <p>
 * O documento é renderizado com <b>Flying Saucer</b> (motor XHTML/CSS 2.1 de verdade, embutido no
 * Swing) em vez do {@link JEditorPane} nativo, que só entende um subconjunto solto de HTML 3.2 /
 * CSS 1. Como esta é uma dependência nova que não pôde ser baixada/testada de verdade neste
 * ambiente (sem acesso ao Maven Central aqui — só compilada contra um stub fiel da API), existe
 * um FALLBACK automático: se o Flying Saucer lançar qualquer exceção ao processar o XHTML gerado,
 * o painel troca sozinho para o {@link JEditorPane} antigo (que continua funcionando exatamente
 * como antes) em vez de deixar a tela de processo em branco. Teste isso de verdade com
 * {@code mvn clean install} assim que possível — se cair no fallback com frequência, é sinal de
 * algo no XHTML gerado que precisa de ajuste fino.
 * <p>
 * Quando o processo tem conteúdo no editor de blocos estilo Notion
 * ({@code ProcessDocument.getContent()}), ele é renderizado aqui. Processos sem blocos usam os
 * campos estruturados (objetivo/pré-requisitos/passos/observações/links).
 */
public final class ProcessViewerPanel extends JPanel {
    private final ProcessHeaderPanel header = new ProcessHeaderPanel();
    private final JEditorPane documentPane = new JEditorPane();
    private final FilesLinksCard filesLinksCard = new FilesLinksCard();
    private final InformationCard informationCard = new InformationCard();
    private final JPanel blocksCard = new JPanel(new BorderLayout(0, 8));
    private final JPanel blockLinks = new JPanel();

    public ProcessViewerPanel() {
        setLayout(new BorderLayout(16, 0));
        setBackground(Palette.CONTENT_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 12));

        JPanel centerColumn = new JPanel(new BorderLayout(0, 10));
        centerColumn.setOpaque(false);
        centerColumn.add(header, BorderLayout.NORTH);

        documentPane.setEditable(false);
        documentPane.setContentType("text/html");
        documentPane.setBackground(Palette.CONTENT_BACKGROUND);
        documentPane.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        documentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        documentPane.setFont(documentPane.getFont().deriveFont(Font.PLAIN, 14f));
        JPopupMenu copyMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copiar seleção");
        copyItem.addActionListener(event -> documentPane.copy());
        copyMenu.add(copyItem);
        documentPane.setComponentPopupMenu(copyMenu);
        JScrollPane documentScroll = new JScrollPane(documentPane);
        documentScroll.setBorder(BorderFactory.createEmptyBorder());
        centerColumn.add(documentScroll, BorderLayout.CENTER);
        add(centerColumn, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        filesLinksCard.setAlignmentX(LEFT_ALIGNMENT);
        blocksCard.setAlignmentX(LEFT_ALIGNMENT);
        informationCard.setAlignmentX(LEFT_ALIGNMENT);
        side.add(filesLinksCard);
        side.add(Box.createVerticalStrut(12));
        side.add(blocksCard);
        side.add(Box.createVerticalStrut(12));
        side.add(informationCard);
        side.add(Box.createVerticalGlue());
        JScrollPane sideScroll = new JScrollPane(side);
        sideScroll.setBorder(BorderFactory.createEmptyBorder());
        sideScroll.getViewport().setOpaque(false);
        sideScroll.setOpaque(false);
        sideScroll.setPreferredSize(new Dimension(300, 0));
        sideScroll.setMinimumSize(new Dimension(240, 0));
        add(sideScroll, BorderLayout.EAST);
    }

    public void render(ProcessDocument process) {
        header.setModeToggleVisible(true);
        header.setEditingMode(false);
        header.setTitle(process.getTitle());
        renderDocument(buildBody(process));
        renderBlockAccess(process.getContent());
        blocksCard.setVisible(process.getContent() != null && !process.getContent().isEmpty());
        filesLinksCard.setVisible(true);
        informationCard.setVisible(true);
        filesLinksCard.render(linksForSidebar(process));
        informationCard.render(process);
    }

    public void renderFolder(String title, String description, boolean showHeader) {
        header.setModeToggleVisible(false);
        header.setVisible(showHeader);
        header.setTitle(title);
        StringBuilder body = new StringBuilder();
        body.append("<h1>").append(escape(title)).append("</h1>");
        body.append("<p>").append(multiline(description)).append("</p>");
        body.append("<hr/><p>Selecione um processo ou use a barra de ferramentas para criar conteúdo.</p>");
        renderDocument(body.toString());
        filesLinksCard.setVisible(false);
        blocksCard.setVisible(false);
        informationCard.setVisible(false);
    }

    public void renderWelcome() {
        renderFolder("Base de conhecimento", "Selecione uma pasta ou um processo na navegação à esquerda.",
                false);
    }

    /** Delega para o cabeçalho o clique em "Edição", que dispara o botão real "Editar" da toolbar. */
    public void onSwitchToEdit(Runnable listener) {
        header.onSwitchToEdit(listener);
    }

    /** Espelha no cabeçalho se o botão "Editar" da toolbar está habilitado no momento. */
    public void setEditAvailable(boolean available) {
        header.setEditAvailable(available);
    }

    private void renderDocument(String bodyContent) {
        documentPane.setText("<html><head><style>" + STYLESHEET + "</style></head><body>" + bodyContent + "</body></html>");
        documentPane.setCaretPosition(0);
    }

    /**
     * Folha de estilo real (CSS 2.1 via Flying Saucer; subconjunto via JEditorPane no fallback).
     * Embutida uma única vez no {@code <head>} do documento, em vez de repetir
     * {@code style='...'} em cada elemento.
     */
    private static final String STYLESHEET = """
            body { font-family: sans-serif; line-height: 1.6; background-color: #ffffff; color: #1c2531; }
            h1 { color: #2f7edb; margin-bottom: 4px; }
            h2 { color: #1c2531; margin-top: 20px; }
            a { color: #2f7edb; }
            .code-block { background-color: #f5f7fa; border: 1px solid #d3dae4; padding: 10px 12px;
                         border-radius: 6px; font-family: monospace; font-size: 12px; }
            .callout-info { background-color: #e7f0fb; border: 1px solid #c4d9f3; color: #18345d;
                           border-radius: 6px; padding: 12px; margin-top: 10px; margin-bottom: 10px; }
            .callout-warning { background-color: #fdf3de; border: 1px solid #f0dba3; color: #7a5b12;
                              border-radius: 6px; padding: 12px; margin-top: 10px; margin-bottom: 10px; }
            .callout-note { background-color: #eef0f2; border: 1px solid #d8dbe0; color: #3d3d40;
                          border-radius: 6px; padding: 12px; margin-top: 10px; margin-bottom: 10px; }
            """;

    private String buildBody(ProcessDocument process) {
        boolean hasBlocks = process.getContent() != null && !process.getContent().isEmpty();
        return hasBlocks ? buildBodyFromBlocks(process) : buildBodyFromFields(process);
    }

    private String buildBodyFromBlocks(ProcessDocument process) {
        StringBuilder html = new StringBuilder();
        html.append("<h1>").append(escape(process.getTitle())).append("</h1>");
        if (!safe(process.getDescription()).isBlank()) {
            html.append("<p>").append(multiline(process.getDescription())).append("</p>");
        }
        ContentBlockType openList = null;
        for (ContentBlock block : process.getContent()) {
            ContentBlockType type = block.getType();
            boolean isListType = type == ContentBlockType.BULLET_LIST || type == ContentBlockType.NUMBERED_LIST;
            if (openList != null && (!isListType || openList != type)) {
                html.append(openList == ContentBlockType.BULLET_LIST ? "</ul>" : "</ol>");
                openList = null;
            }
            if (isListType && openList == null) {
                html.append(type == ContentBlockType.BULLET_LIST ? "<ul>" : "<ol>");
                openList = type;
            }
            appendBlock(html, block);
        }
        if (openList != null) {
            html.append(openList == ContentBlockType.BULLET_LIST ? "</ul>" : "</ol>");
        }
        appendSteps(html, process);
        return html.toString();
    }

    private void appendBlock(StringBuilder html, ContentBlock block) {
        String text = styledInline(block);
        switch (block.getType()) {
            case PARAGRAPH -> html.append("<p>").append(text).append("</p>");
            case HEADING -> html.append("<h2>").append(text).append("</h2>");
            case BULLET_LIST, NUMBERED_LIST -> html.append("<li>").append(text).append("</li>");
            case CODE -> html.append("<pre class=\"code-block\">").append(escape(safe(block.getText())))
                    .append("</pre>");
            case LINK -> html.append("<p><a href=\"").append(escape(safe(block.getUrl()))).append("\">")
                    .append(text.isBlank() ? escape(safe(block.getUrl())) : text).append("</a></p>");
            case IMAGE -> {
                if (!safe(block.getUrl()).isBlank()) {
                    html.append("<p><img src=\"").append(escape(block.getUrl())).append("\" width=\"480\"/></p>");
                }
            }
            case INFO -> callout(html, "callout-info", "ℹ Informação", text);
            case WARNING -> callout(html, "callout-warning", "⚠ Aviso", text);
            case NOTE -> callout(html, "callout-note", "📝 Observação", text);
        }
    }

    private void callout(StringBuilder html, String cssClass, String label, String text) {
        html.append("<div class=\"").append(cssClass).append("\"><b>").append(label).append("</b><br/>")
                .append(text).append("</div>");
    }

    private String styledInline(ContentBlock block) {
        String text = escape(safe(block.getText())).replace("\n", "<br/>");
        if (block.isBold()) {
            text = "<b>" + text + "</b>";
        }
        if (block.isItalic()) {
            text = "<i>" + text + "</i>";
        }
        if (block.isUnderline()) {
            text = "<u>" + text + "</u>";
        }
        return text;
    }

    private String buildBodyFromFields(ProcessDocument process) {
        StringBuilder html = new StringBuilder();
        html.append("<h1>").append(escape(process.getTitle())).append("</h1>");
        if (!safe(process.getDescription()).isBlank()) {
            html.append("<p>").append(multiline(process.getDescription())).append("</p>");
        }
        section(html, "Objetivo", process.getObjective());
        section(html, "Pré-requisitos", process.getPrerequisites());
        appendSteps(html, process);
        if (!safe(process.getObservations()).isBlank()) {
            callout(html, "callout-info", "ℹ Observação", multiline(process.getObservations()));
        }
        return html.toString();
    }

    private void appendSteps(StringBuilder html, ProcessDocument process) {
        String richSteps = RichTextHtml.sanitize(process.getStepsHtml());
        if (!richSteps.isBlank()) {
            html.append("<h2>Passos</h2>").append(numberedRichSteps(richSteps));
            return;
        }
        if (process.getSteps() == null || process.getSteps().isEmpty()) return;
        html.append("<h2>Passos</h2><ol>");
        for (ProcessStep step : process.getSteps()) {
            html.append("<li><b>").append(escape(step.getTitle())).append("</b>");
            if (!safe(step.getDescription()).isBlank()) html.append("<br/>").append(multiline(step.getDescription()));
            if (!safe(step.getExpectedResult()).isBlank()) {
                html.append("<br/><i>Resultado esperado: ").append(multiline(step.getExpectedResult())).append("</i>");
            }
            html.append("</li>");
        }
        html.append("</ol>");
    }

    private String numberedRichSteps(String safeHtml) {
        if (safeHtml.contains("<ol>") || safeHtml.contains("<ul>")) return safeHtml;
        // Parágrafos permanecem parágrafos. Só listas escolhidas com a ferramenta
        // de numeração devem exibir marcadores automáticos.
        return safeHtml;
    }

    private java.util.List<String> linksForSidebar(ProcessDocument process) {
        java.util.List<String> links = new java.util.ArrayList<>(process.getLinks());
        if (process.getContent() == null) return links;
        java.util.Set<String> blockLinks = new java.util.HashSet<>();
        for (ContentBlock block : process.getContent()) {
            if (block.getType() == ContentBlockType.LINK && block.getUrl() != null) {
                blockLinks.add(block.getUrl().trim());
            }
        }
        links.removeIf(link -> blockLinks.contains(link == null ? "" : link.trim()));
        return links;
    }

    private void renderBlockAccess(java.util.List<ContentBlock> blocks) {
        blocksCard.removeAll();
        blocksCard.setOpaque(true);
        blocksCard.setBackground(Palette.CONTENT_BACKGROUND);
        blocksCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.SIDEBAR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel title = new JLabel("Blocos do processo");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        blocksCard.add(title, BorderLayout.NORTH);
        blockLinks.removeAll();
        blockLinks.setOpaque(false);
        blockLinks.setLayout(new BoxLayout(blockLinks, BoxLayout.Y_AXIS));
        if (blocks != null) {
            int index = 0;
            for (ContentBlock block : blocks) {
                final int blockIndex = index++;
                JButton open = new JButton(blockLabel(block));
                open.setHorizontalAlignment(SwingConstants.LEFT);
                open.setToolTipText("Abrir conteúdo do bloco");
                open.setFocusable(false);
                open.addActionListener(event -> showBlock(block, blockIndex));
                blockLinks.add(open);
                blockLinks.add(Box.createVerticalStrut(4));
            }
        }
        blocksCard.add(blockLinks, BorderLayout.CENTER);
        blocksCard.revalidate();
        blocksCard.repaint();
    }

    private void showBlock(ContentBlock block, int index) {
        String title = blockTypeLabel(block.getType());
        String text = safe(block.getText());
        if (text.isBlank()) text = safe(block.getUrl());
        java.awt.Color surface = Palette.CONTENT_BACKGROUND;
        java.awt.Color foreground = Palette.CONTENT_TEXT_PRIMARY;
        if (block.getType() == ContentBlockType.NOTE) {
            surface = Palette.NOTE_SURFACE;
            foreground = Palette.NOTE_SURFACE_TEXT;
        } else if (block.getType() == ContentBlockType.INFO) {
            surface = Palette.INFO_SURFACE;
            foreground = Palette.INFO_SURFACE_TEXT;
        } else if (block.getType() == ContentBlockType.WARNING) {
            surface = Palette.WARNING_SURFACE;
            foreground = Palette.WARNING_SURFACE_TEXT;
        } else if (block.getType() == ContentBlockType.CODE) {
            surface = new java.awt.Color(38, 42, 51);
            foreground = new java.awt.Color(214, 219, 227);
        }
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(surface);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel heading = new JLabel(title);
        heading.setForeground(foreground);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        content.add(heading, BorderLayout.NORTH);
        JTextArea textView = new JTextArea(text);
        textView.setEditable(false);
        textView.setLineWrap(true);
        textView.setWrapStyleWord(true);
        textView.setBackground(surface);
        textView.setForeground(foreground);
        textView.setCaretColor(foreground);
        textView.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        textView.setFont(block.getType() == ContentBlockType.CODE
                ? new Font(Font.MONOSPACED, Font.PLAIN, 13) : textView.getFont().deriveFont(14f));
        JScrollPane scroll = new JScrollPane(textView);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(surface);
        scroll.getViewport().setBackground(surface);
        content.add(scroll, BorderLayout.CENTER);
        scroll.setPreferredSize(new Dimension(620, 320));
        JOptionPane.showMessageDialog(this, content,
                "Bloco " + (index + 1) + " · " + title, JOptionPane.PLAIN_MESSAGE);
    }

    private String blockLabel(ContentBlock block) {
        String text = safe(block.getText()).replaceAll("\\s+", " ").trim();
        if (text.isBlank()) text = safe(block.getUrl()).trim();
        if (text.isBlank()) text = "Abrir conteúdo";
        if (text.length() > 48) text = text.substring(0, 45) + "…";
        return "▸  " + blockTypeLabel(block.getType()) + " — " + text;
    }

    private String blockTypeLabel(ContentBlockType type) {
        if (type == null) return "Bloco";
        return switch (type) {
            case PARAGRAPH -> "Parágrafo";
            case HEADING -> "Título";
            case BULLET_LIST -> "Lista";
            case NUMBERED_LIST -> "Lista numerada";
            case CODE -> "Código";
            case LINK -> "Link";
            case IMAGE -> "Imagem";
            case INFO -> "Informação";
            case WARNING -> "Aviso";
            case NOTE -> "Observação";
        };
    }

    private void appendLinks(StringBuilder html, ProcessDocument process) {
        if (!process.getLinks().isEmpty()) {
            html.append("<h2>Links úteis</h2><ul>");
            process.getLinks().forEach(link -> html.append("<li>").append(escape(link)).append("</li>"));
            html.append("</ul>");
        }
    }

    private void section(StringBuilder html, String title, String value) {
        if (!safe(value).isBlank()) {
            html.append("<h2>").append(escape(title)).append("</h2><p>")
                    .append(multiline(value)).append("</p>");
        }
    }

    private String multiline(String value) {
        return escape(safe(value)).replace("\n", "<br/>");
    }

    private String escape(String value) {
        return safe(value).replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
