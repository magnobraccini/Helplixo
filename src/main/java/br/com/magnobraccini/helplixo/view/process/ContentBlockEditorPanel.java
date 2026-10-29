package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.model.ContentBlock;
import br.com.magnobraccini.helplixo.model.ContentBlockType;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Editor de conteúdo em blocos, no espírito do Notion: cada bloco tem um
 * tipo (parágrafo, título, lista, código, link, imagem, caixas de
 * informação/aviso/observação) e pode ser adicionado, reordenado, removido
 * e formatado (negrito/itálico/sublinhado). Tudo é serializado como uma
 * lista de {@link ContentBlock}, salva junto do restante do processo em JSON.
 */
public final class ContentBlockEditorPanel extends JPanel {
    private final JPanel blocksContainer = new JPanel();
    private final List<BlockRow> rows = new ArrayList<>();
    private Runnable changeListener = () -> { };
    private boolean filling;

    public ContentBlockEditorPanel() {
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        add(buildToolbar(), BorderLayout.NORTH);

        blocksContainer.setLayout(new BoxLayout(blocksContainer, BoxLayout.Y_AXIS));
        blocksContainer.setOpaque(true);
        blocksContainer.setBackground(Palette.CONTENT_BACKGROUND);
        JScrollPane scrollPane = new JScrollPane(blocksContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true));
        scrollPane.setPreferredSize(new Dimension(200, 190));
        scrollPane.setBackground(Palette.CONTENT_BACKGROUND);
        scrollPane.getViewport().setBackground(Palette.CONTENT_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolbar.setOpaque(true);
        toolbar.setBackground(Palette.CONTENT_SURFACE_ALT);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.CONTENT_BORDER),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        toolbar.add(addBlockButton("¶ Parágrafo", ContentBlockType.PARAGRAPH));
        toolbar.add(addBlockButton("H Título", ContentBlockType.HEADING));
        toolbar.add(addBlockButton("• Lista", ContentBlockType.BULLET_LIST));
        toolbar.add(addBlockButton("1. Numeração", ContentBlockType.NUMBERED_LIST));
        toolbar.add(addBlockButton("</> Código", ContentBlockType.CODE));
        toolbar.add(addBlockButton("🔗 Link", ContentBlockType.LINK));
        toolbar.add(addBlockButton("🖼 Imagem", ContentBlockType.IMAGE));
        toolbar.add(addBlockButton("ℹ Informação", ContentBlockType.INFO));
        toolbar.add(addBlockButton("⚠ Aviso", ContentBlockType.WARNING));
        toolbar.add(addBlockButton("📝 Observação", ContentBlockType.NOTE));
        return toolbar;
    }

    private JButton addBlockButton(String label, ContentBlockType type) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFont(button.getFont().deriveFont(12f));
        button.setOpaque(true);
        button.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        button.setBackground(Palette.CONTENT_BACKGROUND);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        button.setMargin(new Insets(2, 4, 2, 4));
        button.addActionListener(event -> addBlock(ContentBlock.create(type)));
        return button;
    }

    public void setBlocks(List<ContentBlock> blocks) {
        filling = true;
        blocksContainer.removeAll();
        rows.clear();
        for (ContentBlock block : blocks) {
            addRow(block);
        }
        blocksContainer.revalidate();
        blocksContainer.repaint();
        filling = false;
    }

    public List<ContentBlock> getBlocks() {
        List<ContentBlock> blocks = new ArrayList<>();
        for (BlockRow row : rows) {
            blocks.add(row.toBlock());
        }
        return blocks;
    }

    public void onChange(Runnable listener) {
        this.changeListener = listener == null ? () -> { } : listener;
    }

    private void addBlock(ContentBlock block) {
        addRow(block);
        blocksContainer.revalidate();
        blocksContainer.repaint();
        notifyChange();
    }

    private void addRow(ContentBlock block) {
        BlockRow row = new BlockRow(block, this::notifyChange, this::moveUp, this::moveDown, this::removeRow);
        rows.add(row);
        blocksContainer.add(row.component());
    }

    private void moveUp(BlockRow row) {
        int index = rows.indexOf(row);
        if (index <= 0) {
            return;
        }
        rows.remove(index);
        rows.add(index - 1, row);
        rebuildContainer();
        notifyChange();
    }

    private void moveDown(BlockRow row) {
        int index = rows.indexOf(row);
        if (index < 0 || index >= rows.size() - 1) {
            return;
        }
        rows.remove(index);
        rows.add(index + 1, row);
        rebuildContainer();
        notifyChange();
    }

    private void removeRow(BlockRow row) {
        rows.remove(row);
        rebuildContainer();
        notifyChange();
    }

    private void rebuildContainer() {
        blocksContainer.removeAll();
        for (BlockRow row : rows) {
            blocksContainer.add(row.component());
        }
        blocksContainer.revalidate();
        blocksContainer.repaint();
    }

    private void notifyChange() {
        if (!filling) {
            changeListener.run();
        }
    }

    /** Uma linha do editor, representando um único bloco de conteúdo. */
    private static final class BlockRow {
        private final ContentBlockType type;
        private final JPanel component = new JPanel(new BorderLayout(8, 4));
        private JTextArea textArea;
        private JTextField urlField;
        private JToggleButton boldButton;
        private JToggleButton italicButton;
        private JToggleButton underlineButton;

        BlockRow(ContentBlock block, Runnable onChange, Consumer<BlockRow> onMoveUp,
                 Consumer<BlockRow> onMoveDown, Consumer<BlockRow> onRemove) {
            this.type = block.getType();
            build(block, onChange, onMoveUp, onMoveDown, onRemove);
        }

        JPanel component() {
            return component;
        }

        private void build(ContentBlock block, Runnable onChange, Consumer<BlockRow> onMoveUp,
                            Consumer<BlockRow> onMoveDown, Consumer<BlockRow> onRemove) {
            component.setBorder(new CompoundBorder(
                    BorderFactory.createEmptyBorder(4, 0, 4, 0),
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.DIVIDER)));
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

            component.add(buildLabel(), BorderLayout.WEST);
            component.add(buildContentEditor(block, onChange), BorderLayout.CENTER);
            component.add(buildSideControls(block, onChange, onMoveUp, onMoveDown, onRemove), BorderLayout.EAST);
            applyStyle(block);
        }

        private JLabel buildLabel() {
            JLabel label = new JLabel(iconFor(type));
            label.setPreferredSize(new Dimension(28, 24));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setToolTipText(nameFor(type));
            return label;
        }

        private Component buildContentEditor(ContentBlock block, Runnable onChange) {
            if (type == ContentBlockType.LINK) {
                JPanel panel = new JPanel(new BorderLayout(4, 2));
                panel.setOpaque(false);
                textArea = new JTextArea(safe(block.getText()), 1, 20);
                textArea.setLineWrap(true);
                urlField = new JTextField(safe(block.getUrl()));
                urlField.putClientProperty("JTextField.placeholderText", "https://...");
                JPanel labelRow = new JPanel(new BorderLayout(4, 0));
                labelRow.setOpaque(false);
                labelRow.add(new JLabel("Texto:"), BorderLayout.WEST);
                labelRow.add(textArea, BorderLayout.CENTER);
                JPanel urlRow = new JPanel(new BorderLayout(4, 0));
                urlRow.setOpaque(false);
                urlRow.add(new JLabel("URL:"), BorderLayout.WEST);
                urlRow.add(urlField, BorderLayout.CENTER);
                panel.add(labelRow, BorderLayout.NORTH);
                panel.add(urlRow, BorderLayout.SOUTH);
                wireChange(textArea, onChange);
                wireChange(urlField, onChange);
                return panel;
            }
            if (type == ContentBlockType.IMAGE) {
                JPanel panel = new JPanel(new BorderLayout(4, 0));
                panel.setOpaque(false);
                urlField = new JTextField(safe(block.getUrl()));
                urlField.putClientProperty("JTextField.placeholderText", "Caminho ou URL da imagem...");
                JButton browseButton = new JButton("Selecionar...");
                browseButton.setFocusable(false);
                browseButton.addActionListener(event -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Selecionar imagem");
                    if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                        urlField.setText(chooser.getSelectedFile().getAbsolutePath());
                        onChange.run();
                    }
                });
                panel.add(urlField, BorderLayout.CENTER);
                panel.add(browseButton, BorderLayout.EAST);
                wireChange(urlField, onChange);
                return panel;
            }
            textArea = new JTextArea(safe(block.getText()), rowsFor(type), 20);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            applyStyle(block);
            styleByType(type);
            wireChange(textArea, onChange);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            return scrollPane;
        }

        private Component buildSideControls(ContentBlock currentBlock, Runnable onChange, Consumer<BlockRow> onMoveUp,
                                             Consumer<BlockRow> onMoveDown, Consumer<BlockRow> onRemove) {
            JPanel side = new JPanel();
            side.setOpaque(false);
            side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

            if (supportsInlineStyle(type)) {
                JPanel styleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
                styleRow.setOpaque(false);
                boldButton = styleToggle("B", onChange, currentBlock.isBold());
                italicButton = styleToggle("I", onChange, currentBlock.isItalic());
                underlineButton = styleToggle("S", onChange, currentBlock.isUnderline());
                styleRow.add(boldButton);
                styleRow.add(italicButton);
                styleRow.add(underlineButton);
                side.add(styleRow);
            }

            JPanel moveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            moveRow.setOpaque(false);
            JButton up = smallButton("▲");
            JButton down = smallButton("▼");
            JButton remove = smallButton("✕");
            up.addActionListener(event -> onMoveUp.accept(this));
            down.addActionListener(event -> onMoveDown.accept(this));
            remove.addActionListener(event -> onRemove.accept(this));
            moveRow.add(up);
            moveRow.add(down);
            moveRow.add(remove);
            side.add(moveRow);
            return side;
        }

        private JToggleButton styleToggle(String label, Runnable onChange, boolean initiallySelected) {
            JToggleButton button = new JToggleButton(label, initiallySelected);
            button.setFocusable(false);
            button.setMargin(new Insets(0, 4, 0, 4));
            button.addActionListener(event -> {
                applyStyle(toBlock());
                onChange.run();
            });
            return button;
        }

        private JButton smallButton(String label) {
            JButton button = new JButton(label);
            button.setFocusable(false);
            button.setMargin(new Insets(0, 4, 0, 4));
            return button;
        }

        private void wireChange(JTextArea area, Runnable onChange) {
            area.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
                @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
                @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
            });
        }

        private void wireChange(JTextField field, Runnable onChange) {
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
                @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
                @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
            });
        }

        private void applyStyle(ContentBlock block) {
            if (textArea == null) {
                return;
            }
            boolean bold = boldButton != null ? boldButton.isSelected() : block.isBold();
            boolean italic = italicButton != null ? italicButton.isSelected() : block.isItalic();
            boolean underline = underlineButton != null ? underlineButton.isSelected() : block.isUnderline();

            Map<TextAttribute, Object> attributes = new HashMap<>(textArea.getFont().getAttributes());
            attributes.put(TextAttribute.WEIGHT, bold ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
            attributes.put(TextAttribute.POSTURE, italic ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
            attributes.put(TextAttribute.UNDERLINE, underline ? TextAttribute.UNDERLINE_ON : -1);
            textArea.setFont(textArea.getFont().deriveFont(attributes));

            if (boldButton != null) {
                boldButton.setSelected(bold);
            }
            if (italicButton != null) {
                italicButton.setSelected(italic);
            }
            if (underlineButton != null) {
                underlineButton.setSelected(underline);
            }
        }

        private void styleByType(ContentBlockType type) {
            switch (type) {
                case HEADING -> textArea.setFont(textArea.getFont().deriveFont(Font.BOLD, 18f));
                case CODE -> {
                    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
                    textArea.setBackground(new Color(38, 42, 51));
                    textArea.setForeground(new Color(214, 219, 227));
                }
                case INFO -> {
                    textArea.setBackground(Palette.INFO_SURFACE);
                    textArea.setForeground(Palette.INFO_SURFACE_TEXT);
                }
                case WARNING -> {
                    textArea.setBackground(Palette.WARNING_SURFACE);
                    textArea.setForeground(Palette.WARNING_SURFACE_TEXT);
                }
                case NOTE -> {
                    textArea.setBackground(Palette.NOTE_SURFACE);
                    textArea.setForeground(Palette.NOTE_SURFACE_TEXT);
                }
                default -> { }
            }
        }

        ContentBlock toBlock() {
            ContentBlock block = new ContentBlock();
            block.setId(UUID.randomUUID());
            block.setType(type);
            block.setText(textArea != null ? textArea.getText() : "");
            block.setUrl(urlField != null ? urlField.getText() : "");
            block.setBold(boldButton != null && boldButton.isSelected());
            block.setItalic(italicButton != null && italicButton.isSelected());
            block.setUnderline(underlineButton != null && underlineButton.isSelected());
            return block;
        }

        private boolean supportsInlineStyle(ContentBlockType type) {
            return type == ContentBlockType.PARAGRAPH || type == ContentBlockType.HEADING
                    || type == ContentBlockType.BULLET_LIST || type == ContentBlockType.NUMBERED_LIST
                    || type == ContentBlockType.INFO || type == ContentBlockType.WARNING
                    || type == ContentBlockType.NOTE;
        }

        private int rowsFor(ContentBlockType type) {
            return switch (type) {
                case HEADING -> 1;
                case CODE -> 4;
                default -> 2;
            };
        }

        private String iconFor(ContentBlockType type) {
            return switch (type) {
                case PARAGRAPH -> "¶";
                case HEADING -> "H";
                case BULLET_LIST -> "•";
                case NUMBERED_LIST -> "1.";
                case CODE -> "</>";
                case LINK -> "🔗";
                case IMAGE -> "🖼";
                case INFO -> "ℹ";
                case WARNING -> "⚠";
                case NOTE -> "📝";
            };
        }

        private String nameFor(ContentBlockType type) {
            return switch (type) {
                case PARAGRAPH -> "Parágrafo";
                case HEADING -> "Título";
                case BULLET_LIST -> "Lista";
                case NUMBERED_LIST -> "Numeração";
                case CODE -> "Código";
                case LINK -> "Link";
                case IMAGE -> "Imagem";
                case INFO -> "Caixa de Informação";
                case WARNING -> "Caixa de Aviso";
                case NOTE -> "Caixa de Observação";
            };
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
