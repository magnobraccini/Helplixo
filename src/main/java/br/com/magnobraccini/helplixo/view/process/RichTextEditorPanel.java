package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;

/** Ribbon-like WYSIWYG editor for procedure text, backed by Swing's HTML document model. */
public final class RichTextEditorPanel extends JPanel {
    private final HTMLEditorKit editorKit = new HTMLEditorKit();
    private final JEditorPane editor = new JEditorPane();
    private final UndoManager undoManager = new UndoManager();
    private boolean loading;

    public RichTextEditorPanel() {
        super(new BorderLayout(0, 5));
        setOpaque(true);
        setBackground(Palette.SURFACE_DARK_ALT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER_DARK, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        editor.setEditorKit(editorKit);
        editor.setEditable(true);
        editor.setFont(editor.getFont().deriveFont(Font.PLAIN, 14f));
        editor.setBackground(Palette.CONTENT_BACKGROUND);
        editor.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        editor.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
        editor.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        editor.setText("<html><body><p></p></body></html>");
        editor.getDocument().addUndoableEditListener(event -> {
            if (!loading) undoManager.addEdit(event.getEdit());
        });

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true));
        scroll.setBackground(Palette.CONTENT_SURFACE_ALT);
        scroll.setPreferredSize(new Dimension(360, 300));
        scroll.getViewport().setBackground(Color.WHITE);
        add(buildRibbon(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        installEditorShortcuts();
    }

    public void setHtml(String html) {
        loading = true;
        try {
            String safe = RichTextHtml.sanitize(html);
            editor.setText("<html><body>" + (safe.isBlank() ? "<p></p>" : safe) + "</body></html>");
            editor.setCaretPosition(0);
            undoManager.discardAllEdits();
        } finally {
            loading = false;
        }
    }

    public String getHtml() { return RichTextHtml.sanitize(editor.getText()); }
    public JEditorPane getEditor() { return editor; }

    private JPanel buildRibbon() {
        JPanel ribbon = new JPanel(new BorderLayout(0, 3));
        ribbon.setOpaque(true);
        ribbon.setBackground(Palette.SURFACE_DARK_ALT);
        JToolBar textTools = toolBar();
        group(textTools, "Histórico",
                button("↶", "Desfazer (Ctrl+Z)", this::undo),
                button("↷", "Refazer (Ctrl+Y)", this::redo));

        JComboBox<String> family = new JComboBox<>(new String[]{"Arial", "Calibri", "Liberation Sans", "Serif", "Monospaced"});
        family.setSelectedItem("Arial");
        family.setToolTipText("Família da fonte");
        family.setPreferredSize(new Dimension(142, 28));
        styleCombo(family);
        family.addActionListener(event -> {
            if (!loading && family.getSelectedItem() != null) applyCharacterAttribute(StyleConstants.FontFamily, family.getSelectedItem().toString());
        });
        JComboBox<Integer> size = new JComboBox<>(new Integer[]{8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72});
        size.setSelectedItem(14);
        size.setEditable(true);
        size.setToolTipText("Tamanho da fonte");
        size.setPreferredSize(new Dimension(68, 28));
        styleCombo(size);
        size.addActionListener(event -> {
            if (!loading && size.getSelectedItem() != null) {
                try { applyCharacterAttribute(StyleConstants.FontSize, Math.max(6, Math.min(96, Integer.parseInt(size.getSelectedItem().toString())))); }
                catch (NumberFormatException ignored) { }
            }
        });

        group(textTools, "Fonte",
                component(family), component(size),
                button("B", "Negrito (Ctrl+B)", () -> toggleCharacterAttribute(StyleConstants.Bold)),
                button("I", "Itálico (Ctrl+I)", () -> toggleCharacterAttribute(StyleConstants.Italic)),
                button("U̲", "Sublinhado (Ctrl+U)", () -> toggleCharacterAttribute(StyleConstants.Underline)),
                button("S̶", "Tachado", () -> toggleCharacterAttribute(StyleConstants.StrikeThrough)),
                button("x₂", "Subscrito", () -> toggleCharacterAttribute(StyleConstants.Subscript)),
                button("x²", "Sobrescrito", () -> toggleCharacterAttribute(StyleConstants.Superscript)),
                button("Cor", "Cor do texto", this::chooseTextColor),
                button("Marca", "Cor de realce", this::chooseHighlightColor),
                button("Limpar", "Limpar formatação", this::clearFormatting));

        JComboBox<String> style = new JComboBox<>(new String[]{"Corpo", "Título 1", "Título 2", "Título 3", "Citação"});
        style.setToolTipText("Estilo do parágrafo");
        style.setPreferredSize(new Dimension(106, 28));
        styleCombo(style);
        style.addActionListener(event -> { if (!loading && style.getSelectedItem() != null) applyParagraphStyle(style.getSelectedItem().toString()); });
        group(textTools, "Estilos", component(style));

        JToolBar paragraphTools = toolBar();
        group(paragraphTools, "Alinhamento",
                button("☰", "Alinhar à esquerda", () -> align(StyleConstants.ALIGN_LEFT)),
                button("≡", "Centralizar", () -> align(StyleConstants.ALIGN_CENTER)),
                button("☷", "Alinhar à direita", () -> align(StyleConstants.ALIGN_RIGHT)),
                button("▤", "Justificar", () -> align(StyleConstants.ALIGN_JUSTIFIED)));
        group(paragraphTools, "Listas",
                button("• Lista", "Iniciar lista com marcadores", () -> insertList(false)),
                button("1. Lista", "Iniciar lista numerada", () -> insertList(true)));
        group(paragraphTools, "Recuo",
                button("⇤", "Diminuir recuo", () -> indent(-1)),
                button("⇥", "Aumentar recuo", () -> indent(1)));
        group(paragraphTools, "Inserir",
                button("Linha", "Inserir divisor", this::insertDivider),
                button("Link", "Inserir hiperlink", this::insertLink));

        JPanel rows = new JPanel(new BorderLayout(0, 2));
        rows.setOpaque(false);
        rows.add(textTools, BorderLayout.NORTH);
        rows.add(paragraphTools, BorderLayout.SOUTH);
        ribbon.add(rows, BorderLayout.CENTER);
        ribbon.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        return ribbon;
    }

    private JToolBar toolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setOpaque(true);
        toolbar.setBackground(Palette.SURFACE_DARK_ALT);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return toolbar;
    }

    private void group(JToolBar toolbar, String title, java.awt.Component... controls) {
        JPanel group = new JPanel(new BorderLayout(2, 1));
        group.setOpaque(false);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        row.setOpaque(false);
        for (java.awt.Component control : controls) row.add(control);
        JLabel label = new JLabel(title);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 10f));
        label.setForeground(Palette.TEXT_ON_DARK_MUTED);
        group.add(row, BorderLayout.CENTER);
        group.add(label, BorderLayout.SOUTH);
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Palette.BORDER_DARK),
                BorderFactory.createEmptyBorder(1, 5, 2, 6)));
        toolbar.add(group);
    }

    private JButton button(String text, String tooltip, Runnable operation) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(3, 7, 3, 7));
        button.setForeground(Palette.TEXT_ON_DARK);
        button.setBackground(Palette.SURFACE_DARK);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER_DARK, 1, true),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        if (text.equals("B")) button.setFont(button.getFont().deriveFont(Font.BOLD));
        if (text.equals("I")) button.setFont(button.getFont().deriveFont(Font.ITALIC));
        button.addActionListener(event -> { operation.run(); editor.requestFocusInWindow(); });
        return button;
    }

    private java.awt.Component component(java.awt.Component component) { return component; }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(Palette.SURFACE_DARK);
        combo.setForeground(Palette.TEXT_ON_DARK);
        combo.setBorder(BorderFactory.createLineBorder(Palette.BORDER_DARK, 1, true));
    }

    private void toggleCharacterAttribute(Object key) {
        int position = Math.min(editor.getCaretPosition(), editor.getDocument().getLength() - 1);
        AttributeSet current = editor.getDocument().getDefaultRootElement().getElement(0).getAttributes();
        if (editor.getDocument() instanceof HTMLDocument document && position >= 0) {
            current = document.getCharacterElement(position).getAttributes();
        }
        boolean enabled;
        if (key == StyleConstants.Bold) enabled = !StyleConstants.isBold(current);
        else if (key == StyleConstants.Italic) enabled = !StyleConstants.isItalic(current);
        else if (key == StyleConstants.Underline) enabled = !StyleConstants.isUnderline(current);
        else if (key == StyleConstants.StrikeThrough) enabled = !StyleConstants.isStrikeThrough(current);
        else if (key == StyleConstants.Subscript) enabled = !StyleConstants.isSubscript(current);
        else enabled = !StyleConstants.isSuperscript(current);
        applyCharacterAttribute(key, enabled);
    }

    private void applyCharacterAttribute(Object key, Object value) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        attributes.addAttribute(key, value);
        int start = editor.getSelectionStart();
        int length = editor.getSelectionEnd() - start;
        editorKit.getInputAttributes().addAttributes(attributes);
        try {
            if (length > 0) ((HTMLDocument) editor.getDocument()).setCharacterAttributes(start, length, attributes, false);
        } catch (RuntimeException ignored) { }
    }

    private void chooseTextColor() {
        Color color = JColorChooser.showDialog(this, "Cor do texto", editor.getForeground());
        if (color != null) applyCharacterAttribute(StyleConstants.Foreground, color);
    }

    private void chooseHighlightColor() {
        Color color = JColorChooser.showDialog(this, "Cor de realce", new Color(255, 245, 157));
        if (color != null) applyCharacterAttribute(StyleConstants.Background, color);
    }

    private void align(int alignment) {
        HTMLDocument document = (HTMLDocument) editor.getDocument();
        int start = editor.getSelectionStart();
        int length = Math.max(1, editor.getSelectionEnd() - start);
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, alignment);
        document.setParagraphAttributes(start, length, attributes, false);
    }

    private void applyParagraphStyle(String name) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        switch (name) {
            case "Título 1" -> { StyleConstants.setFontSize(attributes, 24); StyleConstants.setBold(attributes, true); }
            case "Título 2" -> { StyleConstants.setFontSize(attributes, 20); StyleConstants.setBold(attributes, true); }
            case "Título 3" -> { StyleConstants.setFontSize(attributes, 16); StyleConstants.setBold(attributes, true); }
            case "Citação" -> { StyleConstants.setItalic(attributes, true); StyleConstants.setLeftIndent(attributes, 24f); }
            default -> { StyleConstants.setFontSize(attributes, 14); StyleConstants.setBold(attributes, false); StyleConstants.setItalic(attributes, false); }
        }
        int start = editor.getSelectionStart();
        int length = Math.max(1, editor.getSelectionEnd() - start);
        ((HTMLDocument) editor.getDocument()).setParagraphAttributes(start, length, attributes, false);
    }

    private void indent(int direction) {
        HTMLDocument document = (HTMLDocument) editor.getDocument();
        int position = editor.getCaretPosition();
        javax.swing.text.Element paragraph = document.getParagraphElement(position);
        AttributeSet current = paragraph.getAttributes();
        float existing = StyleConstants.getLeftIndent(current);
        SimpleAttributeSet update = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(update, Math.max(0f, existing + direction * 18f));
        document.setParagraphAttributes(paragraph.getStartOffset(),
                Math.max(1, paragraph.getEndOffset() - paragraph.getStartOffset()), update, false);
    }

    private void insertList(boolean numbered) {
        String tag = numbered ? "ol" : "ul";
        String command = "insert-" + tag + "-list";
        Action action = new HTMLEditorKit.InsertHTMLTextAction(command,
                "<" + tag + "><li>&nbsp;</li></" + tag + ">", HTML.Tag.BODY,
                numbered ? HTML.Tag.OL : HTML.Tag.UL);
        action.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, command));
        editor.requestFocusInWindow();
    }

    private void insertDivider() {
        try {
            editorKit.insertHTML((HTMLDocument) editor.getDocument(), editor.getCaretPosition(), "<hr>", 0, 0, HTML.Tag.BODY);
        } catch (IOException | javax.swing.text.BadLocationException ignored) { }
    }

    private void insertLink() {
        String url = javax.swing.JOptionPane.showInputDialog(this, "Endereço do link:", "Inserir hiperlink",
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (url == null || url.isBlank()) return;
        String normalized = url.trim();
        if (!normalized.matches("(?i)^https?://[^\\s<>\"']+$")) {
            javax.swing.JOptionPane.showMessageDialog(this, "Informe um endereço iniciado por http:// ou https://.",
                    "Link inválido", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selected = editor.getSelectedText();
        String label = selected == null || selected.isBlank() ? normalized : selected;
        String markup = "<a href=\"" + escape(normalized) + "\">" + escape(label) + "</a>";
        try {
            int start = editor.getSelectionStart();
            int end = editor.getSelectionEnd();
            if (end > start) editor.getDocument().remove(start, end - start);
            editorKit.insertHTML((HTMLDocument) editor.getDocument(), start, markup, 0, 0, HTML.Tag.BODY);
        } catch (IOException | javax.swing.text.BadLocationException ignored) { }
        editor.requestFocusInWindow();
    }

    private void clearFormatting() {
        int start = editor.getSelectionStart();
        int length = editor.getSelectionEnd() - start;
        if (length <= 0) return;
        SimpleAttributeSet defaults = new SimpleAttributeSet();
        StyleConstants.setBold(defaults, false);
        StyleConstants.setItalic(defaults, false);
        StyleConstants.setUnderline(defaults, false);
        StyleConstants.setStrikeThrough(defaults, false);
        StyleConstants.setSubscript(defaults, false);
        StyleConstants.setSuperscript(defaults, false);
        StyleConstants.setFontFamily(defaults, "Arial");
        StyleConstants.setFontSize(defaults, 14);
        StyleConstants.setForeground(defaults, Palette.CONTENT_TEXT_PRIMARY);
        ((HTMLDocument) editor.getDocument()).setCharacterAttributes(start, length, defaults, true);
    }

    private void installEditorShortcuts() {
        editor.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control Z"), "rich-undo");
        editor.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control Y"), "rich-redo");
        editor.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control shift Z"), "rich-redo");
        editor.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control B"), "rich-bold");
        editor.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control I"), "rich-italic");
        editor.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("control U"), "rich-underline");
        editor.getActionMap().put("rich-undo", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { undo(); }
        });
        editor.getActionMap().put("rich-redo", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { redo(); }
        });
        editor.getActionMap().put("rich-bold", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { toggleCharacterAttribute(StyleConstants.Bold); }
        });
        editor.getActionMap().put("rich-italic", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { toggleCharacterAttribute(StyleConstants.Italic); }
        });
        editor.getActionMap().put("rich-underline", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { toggleCharacterAttribute(StyleConstants.Underline); }
        });
    }

    private void undo() {
        try { if (undoManager.canUndo()) undoManager.undo(); }
        catch (CannotUndoException ignored) { }
    }

    private void redo() {
        try { if (undoManager.canRedo()) undoManager.redo(); }
        catch (CannotRedoException ignored) { }
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
