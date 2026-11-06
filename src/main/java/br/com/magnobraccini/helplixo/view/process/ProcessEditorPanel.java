package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tela de edição de processo, com a mesma geometria da leitura: título e resumo no topo, o
 * documento ao centro e uma coluna de detalhes de 300px à direita. Os passos — o conteúdo que
 * importa — ficam sempre abertos e ocupam toda a altura disponível; blocos e notas/links ficam
 * em abas ao lado, em vez de seções recolhidas que precisavam ser reabertas a cada edição.
 * {@link #readData()} mantém o contrato com o controlador.
 */
public final class ProcessEditorPanel extends JPanel {
    private static final int SIDE_WIDTH = 300;

    private final ProcessHeaderPanel header = new ProcessHeaderPanel();
    private final JTextArea descriptionArea = area(2);
    private final JTextArea objectiveArea = area(3);
    private final JTextArea prerequisitesArea = area(3);
    private final JTextField responsibleField = new JTextField();
    private final JTextField categoryField = new JTextField();
    private final JTextField tagsField = new JTextField();
    private final RichTextEditorPanel stepsEditor = new RichTextEditorPanel();
    private final ContentBlockEditorPanel contentEditor = new ContentBlockEditorPanel();
    private final JTextArea observationsArea = area(5);
    private final JTextArea linksArea = area(5);
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<JTextComponent, TextUndo> histories = new LinkedHashMap<>();
    private JTextComponent lastFocused;
    private Runnable changeListener = () -> { };
    private Runnable historyListener = () -> { };
    private boolean filling;

    public ProcessEditorPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Palette.CONTENT_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(14, 24, 16, 20));

        add(buildTop(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        body.add(buildTabs(), BorderLayout.CENTER);
        body.add(buildDetailsColumn(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);

        installChangeTracking();
        installUndoSupport();
    }

    // ---------------------------------------------------------------- estrutura

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        descriptionArea.putClientProperty("JTextField.placeholderText",
                "Resumo: o que este processo resolve e quando usar");
        descriptionArea.getAccessibleContext().setAccessibleName("Resumo do processo");
        descriptionArea.setFont(descriptionArea.getFont().deriveFont(14f));
        descriptionArea.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        styleInput(descriptionArea);
        descriptionArea.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        descriptionArea.setMargin(new java.awt.Insets(2, 0, 2, 0));
        JScrollPane description = scroll(descriptionArea, 42);
        javax.swing.border.Border idle = BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.CONTENT_BACKGROUND);
        javax.swing.border.Border focused = BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.CONTENT_BORDER);
        description.setBorder(idle);
        descriptionArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent event) { description.setBorder(focused); }
            @Override public void focusLost(java.awt.event.FocusEvent event) { description.setBorder(idle); }
        });
        JPanel descriptionRow = new JPanel(new BorderLayout());
        descriptionRow.setOpaque(false);
        descriptionRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, SIDE_WIDTH + 18));
        descriptionRow.add(description, BorderLayout.CENTER);
        top.add(descriptionRow, BorderLayout.CENTER);
        return top;
    }

    private JComponent buildTabs() {
        tabs.putClientProperty("JTabbedPane.tabType", "underlined");
        tabs.putClientProperty("JTabbedPane.tabHeight", 34);
        tabs.putClientProperty("JTabbedPane.showTabSeparators", false);
        tabs.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
        tabs.setFocusable(false);
        tabs.setBackground(Palette.CONTENT_BACKGROUND);
        tabs.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        tabs.setFont(tabs.getFont().deriveFont(13f));

        JPanel steps = new JPanel(new BorderLayout());
        steps.setOpaque(false);
        steps.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        steps.add(stepsEditor, BorderLayout.CENTER);
        tabs.addTab("Passos", steps);

        JPanel blocks = new JPanel(new BorderLayout(0, 8));
        blocks.setOpaque(false);
        blocks.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        blocks.add(hint("Caixas de informação, aviso, código, imagens e links que aparecem antes dos passos."),
                BorderLayout.NORTH);
        blocks.add(contentEditor, BorderLayout.CENTER);
        tabs.addTab("Blocos", blocks);

        JPanel notes = new JPanel();
        notes.setOpaque(false);
        notes.setLayout(new BoxLayout(notes, BoxLayout.Y_AXIS));
        notes.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        notes.add(field("Observações", "Aparecem em destaque ao final dos passos.", scroll(observationsArea, 120)));
        notes.add(Box.createVerticalStrut(14));
        notes.add(linksField());
        notes.add(Box.createVerticalGlue());
        tabs.addTab("Notas e links", notes);
        return tabs;
    }

    private JComponent buildDetailsColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(Palette.CONTENT_SURFACE_ALT);
        column.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        JLabel title = new JLabel("Detalhes");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13.5f));
        title.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(12));

        categoryField.putClientProperty("JTextField.placeholderText", "Ex.: Windows");
        responsibleField.putClientProperty("JTextField.placeholderText", "Equipe ou pessoa");
        tagsField.putClientProperty("JTextField.placeholderText", "admin, usuário local");
        column.add(field("Categoria", null, categoryField));
        column.add(Box.createVerticalStrut(12));
        column.add(field("Responsável", null, responsibleField));
        column.add(Box.createVerticalStrut(12));
        column.add(field("Tags", "Separe por vírgula. Ajudam a busca.", tagsField));
        column.add(Box.createVerticalStrut(16));
        column.add(field("Objetivo", null, scroll(objectiveArea, 76)));
        column.add(Box.createVerticalStrut(12));
        column.add(field("Pré-requisitos", "Acessos, ferramentas ou permissões necessárias.", scroll(prerequisitesArea, 76)));
        column.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true));
        scroll.getViewport().setBackground(Palette.CONTENT_SURFACE_ALT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(SIDE_WIDTH, 0));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(44, 0, 0, 0));
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel field(String label, String help, Component input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        JLabel caption = new JLabel(label);
        caption.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 12f));
        caption.setLabelFor(input instanceof JScrollPane s ? s.getViewport().getView() : input);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        if (help != null) panel.add(hint(help), BorderLayout.SOUTH);
        styleInput(input);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private static JLabel hint(String text) {
        JLabel hint = new JLabel(text);
        hint.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11.5f));
        return hint;
    }

    private void styleInput(Component input) {
        Component target = input instanceof JScrollPane s ? s.getViewport().getView() : input;
        // Bordas claras (o tema escuro da janela as deixaria pretas sobre o fundo branco).
        if (input instanceof JScrollPane || input instanceof JTextField) {
            ((JComponent) input).putClientProperty("FlatLaf.style", "borderColor: #d3dae4; focusedBorderColor: #1867d6");
        }
        if (target instanceof JTextComponent text) {
            text.setBackground(Palette.CONTENT_BACKGROUND);
            text.setForeground(Palette.CONTENT_TEXT_PRIMARY);
            text.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
            text.setSelectionColor(new java.awt.Color(187, 214, 251));
            text.setSelectedTextColor(Palette.CONTENT_TEXT_PRIMARY);
        }
    }

    /** Links/arquivos: um por linha, com botão para anexar um arquivo local pelo seletor do sistema. */
    private JPanel linksField() {
        JButton attachButton = new JButton("Anexar arquivo…");
        attachButton.setFocusable(false);
        attachButton.addActionListener(event -> attachFile());

        JLabel caption = new JLabel("Links e arquivos");
        caption.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 12f));
        JPanel linksHeader = new JPanel(new BorderLayout());
        linksHeader.setOpaque(false);
        linksHeader.add(caption, BorderLayout.WEST);
        linksHeader.add(attachButton, BorderLayout.EAST);

        linksArea.putClientProperty("JTextField.placeholderText", "https://intranet…  ou  \\\\servidor\\pasta\\arquivo.pdf");
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(linksHeader, BorderLayout.NORTH);
        JScrollPane scroll = scroll(linksArea, 120);
        styleInput(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(hint("Um por linha. Aparecem no painel “Arquivos e links” com botões para abrir e copiar."), BorderLayout.SOUTH);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private void attachFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Anexar arquivo ao processo");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = chooser.getSelectedFile().getAbsolutePath();
        String current = linksArea.getText();
        if (current.isBlank()) {
            linksArea.setText(path);
        } else {
            linksArea.setText(current.endsWith("\n") ? current + path : current + "\n" + path);
        }
        linksArea.setCaretPosition(linksArea.getDocument().getLength());
    }

    // ---------------------------------------------------------------- dados

    public void fill(ProcessDocument process) {
        filling = true;
        header.setModeToggleVisible(true);
        header.setEditingMode(true);
        header.setTitleEditable(true);
        silently(() -> {
            header.setTitle(safe(process.getTitle()));
            descriptionArea.setText(safe(process.getDescription()));
            objectiveArea.setText(safe(process.getObjective()));
            prerequisitesArea.setText(safe(process.getPrerequisites()));
            responsibleField.setText(safe(process.getResponsible()));
            categoryField.setText(safe(process.getCategory()));
            tagsField.setText(String.join(", ", process.getTags()));
            observationsArea.setText(safe(process.getObservations()));
            linksArea.setText(String.join("\n", process.getLinks()));
        });
        String stepsHtml = safe(process.getStepsHtml());
        stepsEditor.setHtml(stepsHtml.isBlank() ? RichTextHtml.fromLegacySteps(process.getSteps()) : stepsHtml);
        contentEditor.setBlocks(process.getContent());
        histories.values().forEach(TextUndo::discardAll);
        updateTabTitles();
        tabs.setSelectedIndex(0);
        // Sem título, começa pelo título; senão, direto nos passos — é o que quase sempre se edita.
        JComponent focusTarget = safe(process.getTitle()).isBlank() ? header.getTitleField() : stepsEditor.getEditor();
        lastFocused = (JTextComponent) focusTarget;
        javax.swing.SwingUtilities.invokeLater(focusTarget::requestFocusInWindow);
        filling = false;
        historyListener.run();
    }

    private void silently(Runnable operation) {
        Runnable chain = operation;
        for (TextUndo history : histories.values()) {
            Runnable inner = chain;
            chain = () -> history.silently(inner);
        }
        chain.run();
    }

    private void updateTabTitles() {
        int blocks = contentEditor.getBlocks().size();
        tabs.setTitleAt(1, blocks == 0 ? "Blocos" : "Blocos (" + blocks + ")");
        long links = linksArea.getText().lines().filter(line -> !line.isBlank()).count();
        boolean hasNotes = !observationsArea.getText().isBlank();
        tabs.setTitleAt(2, links == 0 && !hasNotes ? "Notas e links"
                : "Notas e links (" + (links + (hasNotes ? 1 : 0)) + ")");
    }

    public ProcessEditorData readData() {
        return new ProcessEditorData(
                header.getTitleText(),
                descriptionArea.getText(),
                objectiveArea.getText(),
                prerequisitesArea.getText(),
                responsibleField.getText(),
                categoryField.getText(),
                tagsField.getText(),
                observationsArea.getText(),
                linksArea.getText(),
                stepsEditor.getHtml(),
                contentEditor.getBlocks());
    }

    public void onChange(Runnable listener) {
        this.changeListener = listener == null ? () -> { } : listener;
    }

    /** Avisado quando muda a disponibilidade de Desfazer/Refazer (para a barra do processo). */
    public void onHistoryChange(Runnable listener) {
        this.historyListener = listener == null ? () -> { } : listener;
    }

    /** Delega para o cabeçalho o clique em "Visualizar", que dispara o "Cancelar" real da toolbar. */
    public void onSwitchToView(Runnable listener) {
        header.onSwitchToView(listener);
    }

    // ---------------------------------------------------------------- alterações e histórico

    private void installChangeTracking() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { changed(); }
            @Override public void removeUpdate(DocumentEvent event) { changed(); }
            @Override public void changedUpdate(DocumentEvent event) { changed(); }
        };
        for (JTextComponent component : plainTextComponents()) component.getDocument().addDocumentListener(listener);
        stepsEditor.getEditor().getDocument().addDocumentListener(listener);
        contentEditor.onChange(() -> { changed(); updateTabTitles(); });
        DocumentListener tabCounter = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { if (!filling) updateTabTitles(); }
            @Override public void removeUpdate(DocumentEvent event) { if (!filling) updateTabTitles(); }
            @Override public void changedUpdate(DocumentEvent event) { }
        };
        linksArea.getDocument().addDocumentListener(tabCounter);
        observationsArea.getDocument().addDocumentListener(tabCounter);
    }

    private JTextComponent[] plainTextComponents() {
        return new JTextComponent[]{header.getTitleField(), descriptionArea, objectiveArea, prerequisitesArea,
                responsibleField, categoryField, tagsField, observationsArea, linksArea};
    }

    /**
     * Cada campo tem o próprio histórico; Desfazer/Refazer (barra ou Ctrl+Z) age no último campo
     * em que a pessoa digitou. Antes, todos os campos dividiam um histórico e o editor de passos
     * ainda tinha outro em paralelo, o que fazia o Desfazer da barra e o Ctrl+Z discordarem.
     */
    private void installUndoSupport() {
        for (JTextComponent component : plainTextComponents()) {
            TextUndo history = new TextUndo(component.getDocument());
            history.onStateChange(() -> historyListener.run());
            histories.put(component, history);
        }
        stepsEditor.onHistoryChange(() -> historyListener.run());
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", event -> {
            if (event.getNewValue() instanceof JTextComponent text
                    && (histories.containsKey(text) || text == stepsEditor.getEditor())) {
                lastFocused = text;
                historyListener.run();
            }
        });

        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask), "helplixo-undo");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, shortcutMask), "helplixo-redo");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, shortcutMask | InputEvent.SHIFT_DOWN_MASK), "helplixo-redo");
        getActionMap().put("helplixo-undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { undo(); }
        });
        getActionMap().put("helplixo-redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { redo(); }
        });
    }

    /** Desfaz a última edição do campo em uso (ou do último campo editado). */
    public void undo() {
        if (lastFocused == stepsEditor.getEditor()) stepsEditor.undo();
        else if (histories.containsKey(lastFocused)) histories.get(lastFocused).undo();
        changed();
    }

    /** Refaz a última edição desfeita do campo em uso. */
    public void redo() {
        if (lastFocused == stepsEditor.getEditor()) stepsEditor.redo();
        else if (histories.containsKey(lastFocused)) histories.get(lastFocused).redo();
        changed();
    }

    public boolean canUndo() {
        if (lastFocused == stepsEditor.getEditor()) return stepsEditor.canUndo();
        TextUndo history = histories.get(lastFocused);
        return history != null && history.canUndo();
    }

    public boolean canRedo() {
        if (lastFocused == stepsEditor.getEditor()) return stepsEditor.canRedo();
        TextUndo history = histories.get(lastFocused);
        return history != null && history.canRedo();
    }

    private void changed() {
        if (!filling) {
            changeListener.run();
        }
    }

    private static JTextArea area(int rows) {
        JTextArea area = new JTextArea(rows, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new java.awt.Insets(4, 6, 4, 6));
        return area;
    }

    private static JScrollPane scroll(JTextArea area, int height) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(160, height));
        scrollPane.setMinimumSize(new Dimension(80, height));
        scrollPane.getViewport().setBackground(Palette.CONTENT_BACKGROUND);
        scrollPane.setBackground(Palette.CONTENT_BACKGROUND);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        return scrollPane;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
