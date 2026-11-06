package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.DefaultListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Editor visual dos passos de um processo. Barra única e clara colada à página (estilo, formatação,
 * listas, recuo, alinhamento, inserir), botões que refletem a formatação onde está o cursor,
 * listas que convertem os parágrafos selecionados, títulos semânticos ({@code <h1>}–{@code <h3>}),
 * código para comandos e atalhos de teclado iguais aos dos editores mais usados.
 */
public final class RichTextEditorPanel extends JPanel {
    private static final int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    private final HTMLEditorKit editorKit = new HTMLEditorKit();
    private final JEditorPane editor = new JEditorPane();
    private final TextUndo history;
    private final JLabel statsLabel = new JLabel(" ");
    private boolean syncing;

    private JComboBox<BlockStyle> styleCombo;
    private JToggleButton boldButton;
    private JToggleButton italicButton;
    private JToggleButton underlineButton;
    private JToggleButton strikeButton;
    private JToggleButton codeButton;
    private JToggleButton bulletButton;
    private JToggleButton numberButton;
    private JButton alignButton;
    private JButton undoButton;
    private JButton redoButton;
    private Color lastTextColor = new Color(198, 40, 40);
    private Color lastHighlight = new Color(255, 241, 118);
    private int currentAlignment = StyleConstants.ALIGN_LEFT;

    /** Estilos de bloco oferecidos no seletor — viram tags HTML reais, não só tamanhos de fonte. */
    enum BlockStyle {
        PARAGRAPH("Texto normal", "p"),
        HEADING_1("Título 1", "h1"),
        HEADING_2("Título 2", "h2"),
        HEADING_3("Título 3", "h3"),
        QUOTE("Citação", "blockquote"),
        CODE("Bloco de código", "pre");

        final String label;
        final String tag;
        BlockStyle(String label, String tag) { this.label = label; this.tag = tag; }
        @Override public String toString() { return label; }
    }

    public RichTextEditorPanel() {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(Palette.CONTENT_BACKGROUND);
        setBorder(BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true));

        editor.setEditorKit(editorKit);
        editor.setEditable(true);
        editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        editor.setFont(UIFonts.body());
        editor.setBackground(Palette.CONTENT_BACKGROUND);
        editor.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        editor.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
        editor.setSelectionColor(new Color(187, 214, 251));
        editor.setBorder(BorderFactory.createEmptyBorder(18, 28, 24, 28));
        HTMLDocument document = (HTMLDocument) editor.getDocument();
        RichTextHtml.applyDocumentStyle(document.getStyleSheet());
        history = new TextUndo(document);
        editor.setText("<html><body><p></p></body></html>");
        history.discardAll();

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Palette.CONTENT_BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setPreferredSize(new Dimension(420, 360));

        add(buildToolbar(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        installShortcuts();
        editor.addCaretListener(event -> SwingUtilities.invokeLater(this::syncToolbar));
        history.onStateChange(this::syncHistory);
        document.addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleStats(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleStats(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
        syncToolbar();
        updateStats();
    }

    // ---------------------------------------------------------------- API pública

    public void setHtml(String html) {
        history.silently(() -> {
            String safe = RichTextHtml.sanitize(html);
            editor.setText("<html><body>" + (safe.isBlank() ? "<p></p>" : safe) + "</body></html>");
            editor.setCaretPosition(0);
        });
        history.discardAll();
        syncToolbar();
        updateStats();
    }

    public String getHtml() { return RichTextHtml.sanitize(editor.getText()); }
    public JEditorPane getEditor() { return editor; }

    boolean canUndo() { return history.canUndo(); }
    boolean canRedo() { return history.canRedo(); }
    void undo() { history.undo(); }
    void redo() { history.redo(); }
    void onHistoryChange(Runnable listener) {
        history.onStateChange(() -> { syncHistory(); listener.run(); });
    }

    // ---------------------------------------------------------------- barra de ferramentas

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 2, 4));
        bar.setBackground(Palette.CONTENT_SURFACE_ALT);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.CONTENT_BORDER),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        undoButton = iconButton(EditorIcons.of(EditorIcons.UNDO), "Desfazer", "Ctrl+Z", history::undo);
        redoButton = iconButton(EditorIcons.of(EditorIcons.REDO), "Refazer", "Ctrl+Y", history::redo);
        bar.add(undoButton);
        bar.add(redoButton);
        bar.add(separator());

        styleCombo = new JComboBox<>(BlockStyle.values());
        styleCombo.setToolTipText("Estilo do parágrafo (Ctrl+Alt+0 a 3)");
        styleCombo.setFocusable(false);
        styleCombo.setPreferredSize(new Dimension(150, 28));
        styleCombo.setBackground(Palette.CONTENT_BACKGROUND);
        styleCombo.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        styleCombo.putClientProperty("FlatLaf.style", "borderColor: #d3dae4; buttonBackground: #ffffff;"
                + " buttonArrowColor: #46505e; buttonHoverArrowColor: #1c2531");
        styleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (index >= 0 && value instanceof BlockStyle style) {
                    Font base = list.getFont();
                    setFont(switch (style) {
                        case HEADING_1 -> base.deriveFont(Font.BOLD, base.getSize2D() + 5);
                        case HEADING_2 -> base.deriveFont(Font.BOLD, base.getSize2D() + 3);
                        case HEADING_3 -> base.deriveFont(Font.BOLD, base.getSize2D() + 1);
                        case QUOTE -> base.deriveFont(Font.ITALIC);
                        case CODE -> new Font(Font.MONOSPACED, Font.PLAIN, base.getSize());
                        default -> base;
                    });
                }
                return this;
            }
        });
        styleCombo.addActionListener(event -> {
            if (!syncing && styleCombo.getSelectedItem() instanceof BlockStyle style) {
                setBlockStyle(style);
                editor.requestFocusInWindow();
            }
        });
        bar.add(styleCombo);
        bar.add(separator());

        boldButton = textToggle("B", Font.BOLD, "Negrito", "Ctrl+B", () -> toggleCharacter(StyleConstants.Bold));
        italicButton = textToggle("I", Font.ITALIC, "Itálico", "Ctrl+I", () -> toggleCharacter(StyleConstants.Italic));
        underlineButton = textToggle("U", Font.PLAIN, "Sublinhado", "Ctrl+U", () -> toggleCharacter(StyleConstants.Underline));
        underlineButton.setText("<html><u>U</u></html>");
        strikeButton = textToggle("S", Font.PLAIN, "Tachado", "Alt+Shift+5", () -> toggleCharacter(StyleConstants.StrikeThrough));
        strikeButton.setText("<html><s>S</s></html>");
        codeButton = iconToggle(EditorIcons.of(EditorIcons.CODE), "Código (comandos, caminhos)", "Ctrl+Shift+C", this::toggleInlineCode);
        bar.add(boldButton);
        bar.add(italicButton);
        bar.add(underlineButton);
        bar.add(strikeButton);
        bar.add(codeButton);

        JButton textColor = iconButton(EditorIcons.withSwatch(EditorIcons.TEXT_COLOR, () -> lastTextColor),
                "Cor do texto", null, null);
        textColor.addActionListener(event -> showColorMenu(textColor, false));
        JButton highlight = iconButton(EditorIcons.withSwatch(EditorIcons.HIGHLIGHT, () -> lastHighlight),
                "Realce", null, null);
        highlight.addActionListener(event -> showColorMenu(highlight, true));
        bar.add(textColor);
        bar.add(highlight);
        bar.add(iconButton(EditorIcons.of(EditorIcons.CLEAR), "Limpar formatação", "Ctrl+Espaço", this::clearFormatting));
        bar.add(separator());

        bulletButton = iconToggle(EditorIcons.of(EditorIcons.BULLETS), "Lista com marcadores", "Ctrl+Shift+8", () -> toggleList("ul"));
        numberButton = iconToggle(EditorIcons.of(EditorIcons.NUMBERS), "Lista numerada", "Ctrl+Shift+7", () -> toggleList("ol"));
        bar.add(bulletButton);
        bar.add(numberButton);
        bar.add(iconButton(EditorIcons.of(EditorIcons.OUTDENT), "Diminuir recuo", "Ctrl+[", () -> indent(-1)));
        bar.add(iconButton(EditorIcons.of(EditorIcons.INDENT), "Aumentar recuo", "Ctrl+]", () -> indent(1)));
        alignButton = iconButton(EditorIcons.of(EditorIcons.align(0)), "Alinhamento", null, null);
        alignButton.addActionListener(event -> showAlignMenu());
        bar.add(alignButton);
        bar.add(separator());

        bar.add(iconButton(EditorIcons.of(EditorIcons.LINK), "Inserir ou editar link", "Ctrl+K", this::editLink));
        bar.add(iconButton(EditorIcons.of(EditorIcons.DIVIDER), "Inserir linha divisória", null, this::insertDivider));
        JButton more = iconButton(EditorIcons.of(EditorIcons.MORE), "Mais opções", null, null);
        more.addActionListener(event -> showMoreMenu(more));
        bar.add(more);
        return bar;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Palette.CONTENT_SURFACE_ALT);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.CONTENT_BORDER),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        statsLabel.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        statsLabel.setFont(statsLabel.getFont().deriveFont(11.5f));
        JLabel hint = new JLabel("Ctrl+Shift+V cola sem formatação");
        hint.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        hint.setFont(hint.getFont().deriveFont(11.5f));
        footer.add(statsLabel, BorderLayout.WEST);
        footer.add(hint, BorderLayout.EAST);
        return footer;
    }

    private static JComponent separator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(9, 22));
        separator.setForeground(Palette.CONTENT_BORDER);
        return separator;
    }

    private JButton iconButton(Icon icon, String name, String shortcut, Runnable operation) {
        JButton button = new JButton(icon);
        styleToolButton(button, name, shortcut);
        if (operation != null) button.addActionListener(event -> { operation.run(); editor.requestFocusInWindow(); });
        return button;
    }

    private JToggleButton iconToggle(Icon icon, String name, String shortcut, Runnable operation) {
        JToggleButton button = new JToggleButton(icon);
        styleToolButton(button, name, shortcut);
        button.addActionListener(event -> { operation.run(); editor.requestFocusInWindow(); syncToolbar(); });
        return button;
    }

    private JToggleButton textToggle(String text, int style, String name, String shortcut, Runnable operation) {
        JToggleButton button = iconToggle(null, name, shortcut, operation);
        button.setText(text);
        button.setFont(button.getFont().deriveFont(style == Font.PLAIN ? Font.PLAIN : style | Font.BOLD, 13.5f));
        if (style == Font.BOLD) button.setFont(button.getFont().deriveFont(Font.BOLD));
        if (style == Font.ITALIC) button.setFont(new Font(Font.SERIF, Font.ITALIC | Font.BOLD, 15));
        return button;
    }

    private void styleToolButton(AbstractButton button, String name, String shortcut) {
        button.setToolTipText(shortcut == null ? name : name + "  (" + shortcut + ")");
        button.getAccessibleContext().setAccessibleName(name);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        button.setMargin(new Insets(4, 6, 4, 6));
        button.setPreferredSize(new Dimension(30, 28));
    }

    // ---------------------------------------------------------------- estado dos botões

    private void syncToolbar() {
        if (styleCombo == null) return;
        syncing = true;
        try {
            AttributeSet chars = caretCharacterAttributes();
            boldButton.setSelected(StyleConstants.isBold(chars));
            italicButton.setSelected(StyleConstants.isItalic(chars));
            underlineButton.setSelected(StyleConstants.isUnderline(chars));
            strikeButton.setSelected(StyleConstants.isStrikeThrough(chars));
            codeButton.setSelected(chars.isDefined(HTML.Tag.CODE));
            Element paragraph = document().getParagraphElement(editor.getSelectionStart());
            String kind = blockKind(paragraph);
            bulletButton.setSelected(kind.equals("ul"));
            numberButton.setSelected(kind.equals("ol"));
            styleCombo.setSelectedItem(switch (kind) {
                case "h1" -> BlockStyle.HEADING_1;
                case "h2" -> BlockStyle.HEADING_2;
                case "h3" -> BlockStyle.HEADING_3;
                case "blockquote" -> BlockStyle.QUOTE;
                case "pre" -> BlockStyle.CODE;
                default -> BlockStyle.PARAGRAPH;
            });
            currentAlignment = alignmentOf(paragraph);
            alignButton.setIcon(EditorIcons.of(EditorIcons.align(alignmentIndex(currentAlignment))));
        } finally {
            syncing = false;
        }
        syncHistory();
    }

    private void syncHistory() {
        if (undoButton == null) return;
        undoButton.setEnabled(history.canUndo());
        redoButton.setEnabled(history.canRedo());
    }

    private final javax.swing.Timer statsTimer = new javax.swing.Timer(400, event -> updateStats());
    { statsTimer.setRepeats(false); }

    private void scheduleStats() { statsTimer.restart(); }

    private void updateStats() {
        try {
            String text = editor.getDocument().getText(0, editor.getDocument().getLength());
            String trimmed = text.replace(' ', ' ').trim();
            int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
            int paragraphs = 0;
            for (String line : text.split("\n")) if (!line.replace(' ', ' ').isBlank()) paragraphs++;
            statsLabel.setText(paragraphs == 0 ? "Documento vazio — comece digitando o primeiro passo"
                    : plural(paragraphs, "parágrafo", "parágrafos") + ", " + plural(words, "palavra", "palavras"));
        } catch (BadLocationException ignored) { }
    }

    private static String plural(int count, String one, String many) { return count + " " + (count == 1 ? one : many); }

    private AttributeSet caretCharacterAttributes() {
        if (editor.getSelectionStart() == editor.getSelectionEnd()) return editorKit.getInputAttributes();
        return document().getCharacterElement(editor.getSelectionStart()).getAttributes();
    }

    // ---------------------------------------------------------------- formatação de caracteres

    private void toggleCharacter(Object key) {
        AttributeSet current = caretCharacterAttributes();
        boolean enable;
        if (key == StyleConstants.Bold) enable = !StyleConstants.isBold(current);
        else if (key == StyleConstants.Italic) enable = !StyleConstants.isItalic(current);
        else if (key == StyleConstants.Underline) enable = !StyleConstants.isUnderline(current);
        else if (key == StyleConstants.StrikeThrough) enable = !StyleConstants.isStrikeThrough(current);
        else if (key == StyleConstants.Subscript) enable = !StyleConstants.isSubscript(current);
        else enable = !StyleConstants.isSuperscript(current);
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        attributes.addAttribute(key, enable);
        applyCharacterAttributes(attributes);
        syncToolbar();
    }

    private void applyCharacterAttributes(AttributeSet attributes) {
        int start = editor.getSelectionStart();
        int length = editor.getSelectionEnd() - start;
        if (length > 0) {
            history.compound(() -> document().setCharacterAttributes(start, length, attributes, false));
        } else {
            editorKit.getInputAttributes().addAttributes(attributes);
        }
    }

    /** Remove atributos de caractere da seleção preservando o restante (links, outras marcas). */
    private void removeCharacterAttributes(Object... keys) {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (end <= start) {
            for (Object key : keys) editorKit.getInputAttributes().removeAttribute(key);
            return;
        }
        history.compound(() -> forEachRun(start, end, (runStart, runEnd, attributes) -> {
            MutableAttributeSet copy = new SimpleAttributeSet(attributes);
            for (Object key : keys) copy.removeAttribute(key);
            document().setCharacterAttributes(runStart, runEnd - runStart, copy, true);
        }));
    }

    @FunctionalInterface
    private interface RunVisitor { void visit(int start, int end, AttributeSet attributes); }

    private void forEachRun(int start, int end, RunVisitor visitor) {
        List<int[]> runs = new ArrayList<>();
        List<AttributeSet> attributes = new ArrayList<>();
        int position = start;
        while (position < end) {
            Element run = document().getCharacterElement(position);
            int runEnd = Math.min(end, run.getEndOffset());
            runs.add(new int[]{position, runEnd});
            attributes.add(run.getAttributes().copyAttributes());
            if (runEnd <= position) break;
            position = runEnd;
        }
        for (int i = 0; i < runs.size(); i++) visitor.visit(runs.get(i)[0], runs.get(i)[1], attributes.get(i));
    }

    private void toggleInlineCode() {
        if (caretCharacterAttributes().isDefined(HTML.Tag.CODE)) {
            removeCharacterAttributes(HTML.Tag.CODE);
        } else {
            SimpleAttributeSet code = new SimpleAttributeSet();
            code.addAttribute(HTML.Tag.CODE, new SimpleAttributeSet());
            applyCharacterAttributes(code);
        }
        syncToolbar();
    }

    private void applyTextColor(Color color) {
        if (color == null) {
            removeCharacterAttributes(StyleConstants.Foreground, CSS.Attribute.COLOR, HTML.Tag.FONT);
            return;
        }
        lastTextColor = color;
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        applyCharacterAttributes(attributes);
    }

    private void applyHighlight(Color color) {
        if (color == null) {
            removeCharacterAttributes(StyleConstants.Background, CSS.Attribute.BACKGROUND_COLOR, HTML.Tag.SPAN);
            return;
        }
        lastHighlight = color;
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setBackground(attributes, color);
        // O HTMLWriter do Swing não grava background-color; um <span style> embutido garante
        // que o realce sobreviva ao salvar e reabrir.
        SimpleAttributeSet span = new SimpleAttributeSet();
        span.addAttribute(HTML.Attribute.STYLE, "background-color: " + hex(color));
        attributes.addAttribute(HTML.Tag.SPAN, span);
        applyCharacterAttributes(attributes);
    }

    private void showColorMenu(JButton anchor, boolean highlight) {
        Color[] colors = highlight
                ? new Color[]{new Color(255, 241, 118), new Color(200, 230, 201), new Color(187, 222, 251),
                new Color(248, 187, 208), new Color(255, 224, 178), new Color(224, 224, 224)}
                : new Color[]{new Color(198, 40, 40), new Color(230, 81, 0), new Color(46, 125, 50),
                new Color(21, 101, 192), new Color(106, 27, 154), new Color(97, 97, 97)};
        String[] names = highlight
                ? new String[]{"Amarelo", "Verde", "Azul", "Rosa", "Laranja", "Cinza"}
                : new String[]{"Vermelho", "Laranja", "Verde", "Azul", "Roxo", "Cinza"};
        JPopupMenu menu = new JPopupMenu();
        JPanel grid = new JPanel(new GridLayout(1, colors.length, 4, 0));
        grid.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        grid.setOpaque(false);
        Consumer<Color> apply = highlight ? this::applyHighlight : this::applyTextColor;
        for (int i = 0; i < colors.length; i++) {
            Color color = colors[i];
            JButton swatch = new JButton(new SwatchIcon(color));
            swatch.setToolTipText(names[i]);
            swatch.setFocusable(false);
            swatch.putClientProperty("JButton.buttonType", "toolBarButton");
            swatch.addActionListener(event -> { menu.setVisible(false); apply.accept(color); editor.requestFocusInWindow(); anchor.repaint(); });
            grid.add(swatch);
        }
        menu.add(grid);
        menu.addSeparator();
        JMenuItem none = new JMenuItem(highlight ? "Sem realce" : "Cor padrão");
        none.addActionListener(event -> { apply.accept(null); editor.requestFocusInWindow(); });
        menu.add(none);
        JMenuItem custom = new JMenuItem("Mais cores…");
        custom.addActionListener(event -> {
            Color chosen = JColorChooser.showDialog(this, highlight ? "Cor de realce" : "Cor do texto",
                    highlight ? lastHighlight : lastTextColor);
            if (chosen != null) { apply.accept(chosen); anchor.repaint(); }
            editor.requestFocusInWindow();
        });
        menu.add(custom);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private record SwatchIcon(Color color) implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, 18, 18, 5, 5);
            g2.setColor(new Color(0, 0, 0, 40));
            g2.drawRoundRect(x, y, 17, 17, 5, 5);
            g2.dispose();
        }
    }

    private void clearFormatting() {
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (end <= start) {
            MutableAttributeSet input = editorKit.getInputAttributes();
            input.removeAttributes(input.copyAttributes());
            syncToolbar();
            return;
        }
        history.compound(() -> forEachRun(start, end, (runStart, runEnd, attributes) -> {
            SimpleAttributeSet clean = new SimpleAttributeSet();
            Object name = attributes.getAttribute(StyleConstants.NameAttribute);
            if (name != null) clean.addAttribute(StyleConstants.NameAttribute, name);
            Object link = attributes.getAttribute(HTML.Tag.A);
            if (link != null) clean.addAttribute(HTML.Tag.A, link);
            document().setCharacterAttributes(runStart, runEnd - runStart, clean, true);
        }));
        syncToolbar();
    }

    // ---------------------------------------------------------------- blocos (estilo, listas)

    private void setBlockStyle(BlockStyle style) {
        convertSelectedBlocks(kind -> style.tag);
    }

    /** Liga/desliga a lista: converte os parágrafos selecionados, troca o tipo ou volta a parágrafos. */
    private void toggleList(String listTag) {
        boolean allInList = selectedParagraphs().stream().allMatch(p -> blockKind(p).equals(listTag));
        convertSelectedBlocks(kind -> allInList ? "p" : listTag);
    }

    private List<Element> selectedParagraphs() {
        HTMLDocument document = document();
        int start = editor.getSelectionStart();
        int end = Math.max(start, editor.getSelectionEnd() - (editor.getSelectionEnd() > start ? 1 : 0));
        List<Element> paragraphs = new ArrayList<>();
        Element paragraph = document.getParagraphElement(start);
        paragraphs.add(paragraph);
        while (paragraph.getEndOffset() <= end && paragraph.getEndOffset() < document.getLength()) {
            Element next = document.getParagraphElement(paragraph.getEndOffset());
            if (next == paragraph) break;
            paragraph = next;
            paragraphs.add(paragraph);
        }
        return paragraphs;
    }

    /**
     * Reescreve os blocos de nível superior tocados pela seleção. Cada parágrafo vira um item com
     * tipo + HTML inline; os selecionados recebem o novo tipo e o conjunto é serializado de novo,
     * agrupando itens de lista, citação e código consecutivos. Preserva negrito, cores e links.
     */
    private void convertSelectedBlocks(java.util.function.UnaryOperator<String> target) {
        HTMLDocument document = document();
        List<Element> selected = selectedParagraphs();
        List<Element> tops = new ArrayList<>();
        for (Element paragraph : selected) {
            Element top = topBlock(paragraph);
            if (!tops.contains(top)) tops.add(top);
        }
        if (tops.isEmpty() || isBody(tops.get(0))) return;
        int firstStart = tops.get(0).getStartOffset();
        int caretOffset = editor.getCaretPosition() - firstStart;

        StringBuilder html = new StringBuilder();
        String openContainer = null;
        for (Element top : tops) {
            for (Element paragraph : paragraphsWithin(top)) {
                boolean artifact = paragraph.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.IMPLIED
                        && isBody(paragraph.getParentElement()) && plainText(paragraph).isBlank();
                if (artifact && tops.size() > 1) continue;
                String kind = blockKind(paragraph);
                String newKind = selected.contains(paragraph) ? target.apply(kind) : kind;
                String container = switch (newKind) { case "ul", "ol", "blockquote", "pre" -> newKind; default -> null; };
                if (openContainer != null && !openContainer.equals(container)) {
                    html.append("</").append(openContainer).append('>');
                    openContainer = null;
                }
                if (container != null && openContainer == null) {
                    html.append('<').append(container).append('>');
                    openContainer = container;
                }
                String inline = newKind.equals("pre") ? escape(plainText(paragraph)) : inlineHtml(paragraph);
                if (inline.isBlank()) inline = newKind.equals("pre") ? " " : "&nbsp;";
                switch (newKind) {
                    case "ul", "ol" -> html.append("<li>").append(inline).append("</li>");
                    case "blockquote" -> html.append("<p>").append(inline).append("</p>");
                    case "pre" -> html.append(inline).append('\n');
                    case "h1", "h2", "h3" -> html.append('<').append(newKind).append('>').append(inline)
                            .append("</").append(newKind).append('>');
                    default -> html.append("<p").append(alignAttribute(paragraph)).append('>').append(inline).append("</p>");
                }
            }
        }
        if (openContainer != null) html.append("</").append(openContainer).append('>');
        String markup = html.toString().replace("\n</pre>", "</pre>");

        history.compound(() -> {
            try {
                for (int i = tops.size() - 1; i >= 1; i--) document.removeElement(tops.get(i));
                document.setOuterHTML(tops.get(0), markup);
            } catch (BadLocationException | IOException ignored) { }
        });
        int caret = Math.max(0, Math.min(document.getLength(), firstStart + Math.max(0, caretOffset)));
        editor.setCaretPosition(caret);
        syncToolbar();
    }

    private List<Element> paragraphsWithin(Element top) {
        List<Element> paragraphs = new ArrayList<>();
        int position = top.getStartOffset();
        while (position < top.getEndOffset()) {
            Element paragraph = document().getParagraphElement(position);
            paragraphs.add(paragraph);
            if (paragraph.getEndOffset() <= position) break;
            position = paragraph.getEndOffset();
        }
        return paragraphs;
    }

    private Element topBlock(Element element) {
        Element current = element;
        while (current.getParentElement() != null && !isBody(current.getParentElement())) {
            current = current.getParentElement();
        }
        return current;
    }

    private static boolean isBody(Element element) {
        return element.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.BODY
                || element.getParentElement() == null;
    }

    /** Tipo do bloco onde está o parágrafo: p, h1–h3, ul, ol, blockquote ou pre. */
    private static String blockKind(Element paragraph) {
        for (Element e = paragraph; e != null; e = e.getParentElement()) {
            Object name = e.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (name == HTML.Tag.H1) return "h1";
            if (name == HTML.Tag.H2) return "h2";
            if (name == HTML.Tag.H3 || name == HTML.Tag.H4 || name == HTML.Tag.H5 || name == HTML.Tag.H6) return "h3";
            if (name == HTML.Tag.UL) return "ul";
            if (name == HTML.Tag.OL) return "ol";
            if (name == HTML.Tag.BLOCKQUOTE) return "blockquote";
            if (name == HTML.Tag.PRE) return "pre";
            if (name == HTML.Tag.BODY) break;
        }
        return "p";
    }

    private String inlineHtml(Element paragraph) {
        int start = paragraph.getStartOffset();
        int length = Math.max(0, Math.min(paragraph.getEndOffset(), document().getLength()) - start);
        StringWriter writer = new StringWriter();
        try {
            new HTMLWriter(writer, document(), start, length).write();
        } catch (IOException | BadLocationException exception) {
            return escape(plainText(paragraph));
        }
        String safe = RichTextHtml.sanitize(writer.toString());
        return safe.replaceAll("(?i)</?(p|div|h[1-6]|li|ul|ol|blockquote|pre)(\\s[^>]*)?>", "").trim();
    }

    private String plainText(Element paragraph) {
        try {
            int end = Math.min(paragraph.getEndOffset(), document().getLength());
            return document().getText(paragraph.getStartOffset(), end - paragraph.getStartOffset())
                    .replace("\n", "").replace(' ', ' ');
        } catch (BadLocationException exception) {
            return "";
        }
    }

    private String alignAttribute(Element paragraph) {
        return switch (alignmentOf(paragraph)) {
            case StyleConstants.ALIGN_CENTER -> " align=\"center\"";
            case StyleConstants.ALIGN_RIGHT -> " align=\"right\"";
            case StyleConstants.ALIGN_JUSTIFIED -> " align=\"justify\"";
            default -> "";
        };
    }

    private static int alignmentOf(Element paragraph) {
        AttributeSet attributes = paragraph.getAttributes();
        Object css = attributes.getAttribute(CSS.Attribute.TEXT_ALIGN);
        Object html = attributes.getAttribute(HTML.Attribute.ALIGN);
        String value = String.valueOf(css != null ? css : html != null ? html : "left").toLowerCase();
        return switch (value) {
            case "center" -> StyleConstants.ALIGN_CENTER;
            case "right" -> StyleConstants.ALIGN_RIGHT;
            case "justify" -> StyleConstants.ALIGN_JUSTIFIED;
            default -> StyleConstants.ALIGN_LEFT;
        };
    }

    private static int alignmentIndex(int alignment) {
        return switch (alignment) {
            case StyleConstants.ALIGN_CENTER -> 1;
            case StyleConstants.ALIGN_RIGHT -> 2;
            case StyleConstants.ALIGN_JUSTIFIED -> 3;
            default -> 0;
        };
    }

    private void showAlignMenu() {
        JPopupMenu menu = new JPopupMenu();
        String[] labels = {"Alinhar à esquerda", "Centralizar", "Alinhar à direita", "Justificar"};
        String[] keys = {"Ctrl+Shift+L", "Ctrl+Shift+E", "Ctrl+Shift+R", "Ctrl+Shift+J"};
        int[] values = {StyleConstants.ALIGN_LEFT, StyleConstants.ALIGN_CENTER, StyleConstants.ALIGN_RIGHT, StyleConstants.ALIGN_JUSTIFIED};
        for (int i = 0; i < labels.length; i++) {
            int value = values[i];
            JMenuItem item = new JMenuItem(labels[i], EditorIcons.of(EditorIcons.align(i)));
            item.setAccelerator(KeyStroke.getKeyStroke(keys[i].replace("Ctrl+Shift+", "control shift ")));
            item.setFont(item.getFont().deriveFont(value == currentAlignment ? Font.BOLD : Font.PLAIN));
            item.addActionListener(event -> { align(value); editor.requestFocusInWindow(); });
            menu.add(item);
        }
        menu.show(alignButton, 0, alignButton.getHeight());
    }

    private void align(int alignment) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, alignment);
        history.compound(() -> {
            for (Element paragraph : selectedParagraphs()) {
                document().setParagraphAttributes(paragraph.getStartOffset(),
                        Math.max(1, paragraph.getEndOffset() - paragraph.getStartOffset()), attributes, false);
            }
        });
        syncToolbar();
    }

    private void indent(int direction) {
        history.compound(() -> {
            for (Element paragraph : selectedParagraphs()) {
                int existing = leftIndentOf(paragraph);
                SimpleAttributeSet update = new SimpleAttributeSet();
                document().getStyleSheet().addCSSAttribute(update, CSS.Attribute.MARGIN_LEFT,
                        Math.max(0, Math.min(240, existing + direction * 24)) + "px");
                document().setParagraphAttributes(paragraph.getStartOffset(),
                        Math.max(1, paragraph.getEndOffset() - paragraph.getStartOffset()), update, false);
            }
        });
    }

    private static int leftIndentOf(Element paragraph) {
        Object value = paragraph.getAttributes().getAttribute(CSS.Attribute.MARGIN_LEFT);
        if (value == null) return 0;
        java.util.regex.Matcher number = java.util.regex.Pattern.compile("[0-9]+").matcher(value.toString());
        return number.find() ? Integer.parseInt(number.group()) : 0;
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    // ---------------------------------------------------------------- inserir

    private void insertDivider() {
        for (Action action : editorKit.getActions()) {
            if ("InsertHR".equals(action.getValue(Action.NAME))) {
                Action hr = action;
                history.compound(() -> hr.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, "InsertHR")));
                return;
            }
        }
    }

    private void editLink() {
        AttributeSet current = document().getCharacterElement(Math.max(0, editor.getSelectionStart())).getAttributes();
        Object anchor = current.getAttribute(HTML.Tag.A);
        String existing = anchor instanceof AttributeSet set && set.getAttribute(HTML.Attribute.HREF) != null
                ? set.getAttribute(HTML.Attribute.HREF).toString() : "";
        int start = editor.getSelectionStart();
        int end = editor.getSelectionEnd();
        if (!existing.isEmpty() && start == end) {
            Element run = document().getCharacterElement(start);
            start = run.getStartOffset();
            end = run.getEndOffset();
        }
        String selectedText = textBetween(start, end);

        JTextField textField = new JTextField(selectedText, 32);
        JTextField urlField = new JTextField(existing, 32);
        urlField.putClientProperty("JTextField.placeholderText", "https://…");
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Texto exibido"));
        form.add(textField);
        form.add(new JLabel("Endereço"));
        form.add(urlField);
        SwingUtilities.invokeLater(urlField::requestFocusInWindow);
        Object[] options = existing.isEmpty() ? new Object[]{"Inserir link", "Cancelar"}
                : new Object[]{"Salvar link", "Remover link", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(this, form, existing.isEmpty() ? "Inserir link" : "Editar link",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice < 0 || options[choice].equals("Cancelar")) { editor.requestFocusInWindow(); return; }
        int finalStart = start;
        int finalEnd = end;
        if (options[choice].equals("Remover link")) {
            editor.select(finalStart, finalEnd);
            removeCharacterAttributes(HTML.Tag.A);
            editor.requestFocusInWindow();
            return;
        }
        String url = RichTextHtml.normalizeUrl(urlField.getText());
        if (url == null) {
            JOptionPane.showMessageDialog(this, "Informe um endereço web, por exemplo https://intranet.empresa.com.br.",
                    "Endereço inválido", JOptionPane.WARNING_MESSAGE);
            editor.requestFocusInWindow();
            return;
        }
        String label = textField.getText().isBlank() ? url : textField.getText().trim();
        history.compound(() -> {
            try {
                int position = finalStart;
                if (!label.equals(selectedText)) {
                    AttributeSet base = document().getCharacterElement(Math.max(0, finalStart)).getAttributes();
                    if (finalEnd > finalStart) document().remove(finalStart, finalEnd - finalStart);
                    MutableAttributeSet plain = new SimpleAttributeSet(base);
                    plain.removeAttribute(HTML.Tag.A);
                    document().insertString(position, label, plain);
                }
                SimpleAttributeSet href = new SimpleAttributeSet();
                href.addAttribute(HTML.Attribute.HREF, url);
                SimpleAttributeSet link = new SimpleAttributeSet();
                link.addAttribute(HTML.Tag.A, href);
                document().setCharacterAttributes(position, label.length(), link, false);
                editor.setCaretPosition(position + label.length());
            } catch (BadLocationException ignored) { }
        });
        editor.requestFocusInWindow();
    }

    private String textBetween(int start, int end) {
        try { return end > start ? document().getText(start, end - start) : ""; }
        catch (BadLocationException exception) { return ""; }
    }

    private void pastePlainText() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String text) {
                history.compound(() -> editor.replaceSelection(text.replace("\r\n", "\n").replace('\t', ' ')));
            }
        } catch (Exception ignored) { }
    }

    private void showMoreMenu(JButton anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenu font = new JMenu("Fonte");
        for (String family : new String[]{"Padrão do sistema", "Arial", "Calibri", "Segoe UI", "Consolas"}) {
            JMenuItem item = new JMenuItem(family);
            item.addActionListener(event -> {
                if (family.startsWith("Padrão")) removeCharacterAttributes(StyleConstants.FontFamily, CSS.Attribute.FONT_FAMILY);
                else { SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setFontFamily(a, family); applyCharacterAttributes(a); }
                editor.requestFocusInWindow();
            });
            font.add(item);
        }
        menu.add(font);
        JMenu size = new JMenu("Tamanho");
        for (int value : new int[]{11, 12, 14, 16, 18, 20, 24, 28}) {
            JMenuItem item = new JMenuItem(value + (value == 14 ? "  (padrão)" : ""));
            item.addActionListener(event -> {
                if (value == 14) removeCharacterAttributes(StyleConstants.FontSize, CSS.Attribute.FONT_SIZE);
                else { SimpleAttributeSet a = new SimpleAttributeSet(); StyleConstants.setFontSize(a, value); applyCharacterAttributes(a); }
                editor.requestFocusInWindow();
            });
            size.add(item);
        }
        menu.add(size);
        menu.addSeparator();
        menu.add(menuItem("Subscrito", null, () -> toggleCharacter(StyleConstants.Subscript)));
        menu.add(menuItem("Sobrescrito", null, () -> toggleCharacter(StyleConstants.Superscript)));
        menu.addSeparator();
        menu.add(menuItem("Colar sem formatação", KeyStroke.getKeyStroke(KeyEvent.VK_V, SHORTCUT | InputEvent.SHIFT_DOWN_MASK), this::pastePlainText));
        menu.show(anchor, 0, anchor.getHeight());
    }

    private JMenuItem menuItem(String label, KeyStroke accelerator, Runnable operation) {
        JMenuItem item = new JMenuItem(label);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(event -> { operation.run(); editor.requestFocusInWindow(); });
        return item;
    }

    // ---------------------------------------------------------------- atalhos

    private void installShortcuts() {
        bind(KeyEvent.VK_Z, SHORTCUT, "rich-undo", history::undo);
        bind(KeyEvent.VK_Y, SHORTCUT, "rich-redo", history::redo);
        bind(KeyEvent.VK_Z, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-redo", history::redo);
        bind(KeyEvent.VK_B, SHORTCUT, "rich-bold", () -> toggleCharacter(StyleConstants.Bold));
        bind(KeyEvent.VK_I, SHORTCUT, "rich-italic", () -> toggleCharacter(StyleConstants.Italic));
        bind(KeyEvent.VK_U, SHORTCUT, "rich-underline", () -> toggleCharacter(StyleConstants.Underline));
        bind(KeyEvent.VK_5, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, "rich-strike", () -> toggleCharacter(StyleConstants.StrikeThrough));
        bind(KeyEvent.VK_C, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-code", this::toggleInlineCode);
        bind(KeyEvent.VK_SPACE, SHORTCUT, "rich-clear", this::clearFormatting);
        bind(KeyEvent.VK_K, SHORTCUT, "rich-link", this::editLink);
        bind(KeyEvent.VK_8, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-bullets", () -> toggleList("ul"));
        bind(KeyEvent.VK_7, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-numbers", () -> toggleList("ol"));
        bind(KeyEvent.VK_CLOSE_BRACKET, SHORTCUT, "rich-indent", () -> indent(1));
        bind(KeyEvent.VK_OPEN_BRACKET, SHORTCUT, "rich-outdent", () -> indent(-1));
        bind(KeyEvent.VK_L, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-align-left", () -> align(StyleConstants.ALIGN_LEFT));
        bind(KeyEvent.VK_E, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-align-center", () -> align(StyleConstants.ALIGN_CENTER));
        bind(KeyEvent.VK_R, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-align-right", () -> align(StyleConstants.ALIGN_RIGHT));
        bind(KeyEvent.VK_J, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-align-justify", () -> align(StyleConstants.ALIGN_JUSTIFIED));
        bind(KeyEvent.VK_V, SHORTCUT | InputEvent.SHIFT_DOWN_MASK, "rich-paste-plain", this::pastePlainText);
        BlockStyle[] styles = {BlockStyle.PARAGRAPH, BlockStyle.HEADING_1, BlockStyle.HEADING_2, BlockStyle.HEADING_3};
        for (int i = 0; i < styles.length; i++) {
            BlockStyle style = styles[i];
            bind(KeyEvent.VK_0 + i, SHORTCUT | InputEvent.ALT_DOWN_MASK, "rich-style-" + i, () -> setBlockStyle(style));
        }
    }

    private void bind(int key, int modifiers, String name, Runnable operation) {
        editor.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, modifiers), name);
        editor.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { operation.run(); }
        });
    }

    private HTMLDocument document() { return (HTMLDocument) editor.getDocument(); }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
