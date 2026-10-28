package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.model.ProcessStep;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.stream.Collectors;

/**
 * Formulário de edição de processo. Reorganizado em seções com cabeçalho (Conteúdo, Detalhes,
 * Passos, Blocos, Observações e Links) para deixar de parecer uma pilha de {@code JTextArea}s —
 * mas cada campo continua exatamente o mesmo componente Swing de antes, então {@link #fill} e
 * {@link #readData()} não mudaram de contrato.
 */
public final class ProcessEditorPanel extends JPanel {
    private final ProcessHeaderPanel header = new ProcessHeaderPanel();
    private final JTextArea descriptionArea = area(3);
    private final JTextArea objectiveArea = area(3);
    private final JTextArea prerequisitesArea = area(3);
    private final JTextField responsibleField = new JTextField();
    private final JTextField tagsField = new JTextField();
    private final JTextArea stepsArea = area(8);
    private final ContentBlockEditorPanel contentEditor = new ContentBlockEditorPanel();
    private final JTextArea observationsArea = area(4);
    private final JTextArea linksArea = area(4);
    private final UndoManager undoManager = new UndoManager();
    private Runnable changeListener = () -> { };
    private boolean filling;

    public ProcessEditorPanel() {
        setLayout(new BorderLayout());
        setBackground(Palette.CONTENT_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        add(header, BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(Palette.CONTENT_BACKGROUND);

        stack.add(section("Conteúdo",
                labeled("Descrição", scroll(descriptionArea))));
        stack.add(verticalGap());
        stack.add(section("Detalhes",
                labeled("Objetivo", scroll(objectiveArea)),
                labeled("Pré-requisitos", scroll(prerequisitesArea)),
                labeled("Responsável", responsibleField),
                labeled("Tags (separadas por vírgula)", tagsField)));
        stack.add(verticalGap());
        stack.add(section("Passos",
                labeled("Uma linha por passo: Título :: Descrição", scroll(stepsArea))));
        stack.add(verticalGap());
        stack.add(section("Blocos (estilo Notion)", contentEditor));
        stack.add(verticalGap());
        stack.add(section("Observações e links",
                labeled("Observações", scroll(observationsArea)),
                linksField()));

        JScrollPane formScroll = new JScrollPane(stack);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(Palette.CONTENT_BACKGROUND);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(formScroll, BorderLayout.CENTER);

        applyLightSurface(this);
        installChangeTracking();
        installUndoSupport();
    }

    public void fill(ProcessDocument process) {
        filling = true;
        header.setModeToggleVisible(true);
        header.setEditingMode(true);
        header.setTitleEditable(true);
        header.setTitle(safe(process.getTitle()));
        descriptionArea.setText(safe(process.getDescription()));
        objectiveArea.setText(safe(process.getObjective()));
        prerequisitesArea.setText(safe(process.getPrerequisites()));
        responsibleField.setText(safe(process.getResponsible()));
        tagsField.setText(String.join(", ", process.getTags()));
        observationsArea.setText(safe(process.getObservations()));
        linksArea.setText(String.join("\n", process.getLinks()));
        stepsArea.setText(process.getSteps().stream()
                .map(this::formatStep)
                .collect(Collectors.joining("\n")));
        contentEditor.setBlocks(process.getContent());
        undoManager.discardAllEdits();
        header.getTitleField().requestFocusInWindow();
        filling = false;
    }

    public ProcessEditorData readData() {
        return new ProcessEditorData(
                header.getTitleText(),
                descriptionArea.getText(),
                objectiveArea.getText(),
                prerequisitesArea.getText(),
                responsibleField.getText(),
                tagsField.getText(),
                observationsArea.getText(),
                linksArea.getText(),
                stepsArea.getText(),
                contentEditor.getBlocks());
    }

    public void onChange(Runnable listener) {
        this.changeListener = listener == null ? () -> { } : listener;
    }


    /** Delega para o cabeçalho o clique em "Visualização", que dispara o "Cancelar" real da toolbar. */
    public void onSwitchToView(Runnable listener) {
        header.onSwitchToView(listener);
    }

    private JPanel section(String title, Component... rows) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Palette.CONTENT_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 14, 14)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        card.add(titleLabel);

        for (Component row : rows) {
            if (row instanceof JComponent jComponent) {
                jComponent.setAlignmentX(LEFT_ALIGNMENT);
            }
            card.add(row);
            card.add(Box.createVerticalStrut(8));
        }
        return card;
    }

    private JPanel labeled(String label, Component field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Palette.CONTENT_BACKGROUND);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        fieldLabel.setFont(fieldLabel.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(fieldLabel, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Igual a {@link #labeled} para os "Links", mas com um botão "📎 Anexar arquivo..." ao lado
     * do rótulo: abre um seletor de arquivos e acrescenta o caminho escolhido como uma nova
     * linha em {@link #linksArea} — mesma lista que vira o card "Arquivos / Links" na
     * visualização do processo, só que agora também aceita arquivos locais, não só URLs.
     */
    private JPanel linksField() {
        JLabel fieldLabel = new JLabel("Links / arquivos — um por linha");
        fieldLabel.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        fieldLabel.setFont(fieldLabel.getFont().deriveFont(Font.PLAIN, 12f));

        JButton attachButton = new JButton("📎 Anexar arquivo...");
        attachButton.setFocusable(false);
        attachButton.putClientProperty("JButton.buttonType", "toolBarButton");
        attachButton.addActionListener(event -> attachFile());

        JPanel linksHeader = new JPanel(new BorderLayout());
        linksHeader.setBackground(Palette.CONTENT_BACKGROUND);
        linksHeader.add(fieldLabel, BorderLayout.WEST);
        linksHeader.add(attachButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Palette.CONTENT_BACKGROUND);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(linksHeader, BorderLayout.NORTH);
        panel.add(scroll(linksArea), BorderLayout.CENTER);
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

    private static Component verticalGap() {
        return Box.createVerticalStrut(12);
    }

    private void applyLightSurface(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField field) {
                field.setBackground(Palette.CONTENT_BACKGROUND);
                field.setForeground(Palette.CONTENT_TEXT_PRIMARY);
                field.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
            } else if (component instanceof JTextArea area) {
                area.setBackground(Palette.CONTENT_BACKGROUND);
                area.setForeground(Palette.CONTENT_TEXT_PRIMARY);
                area.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
            } else if (component instanceof JLabel label) {
                label.setForeground(Palette.CONTENT_TEXT_PRIMARY);
            } else if (component instanceof JPanel panel) {
                panel.setBackground(Palette.CONTENT_BACKGROUND);
            }
            if (component instanceof Container child) {
                applyLightSurface(child);
            }
        }
    }

    private void installChangeTracking() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { changed(); }
            @Override public void removeUpdate(DocumentEvent event) { changed(); }
            @Override public void changedUpdate(DocumentEvent event) { changed(); }
        };
        header.getTitleField().getDocument().addDocumentListener(listener);
        descriptionArea.getDocument().addDocumentListener(listener);
        objectiveArea.getDocument().addDocumentListener(listener);
        prerequisitesArea.getDocument().addDocumentListener(listener);
        responsibleField.getDocument().addDocumentListener(listener);
        tagsField.getDocument().addDocumentListener(listener);
        stepsArea.getDocument().addDocumentListener(listener);
        observationsArea.getDocument().addDocumentListener(listener);
        linksArea.getDocument().addDocumentListener(listener);
        contentEditor.onChange(this::changed);
    }

    private void installUndoSupport() {
        JTextComponent[] textComponents = {
                header.getTitleField(), descriptionArea, objectiveArea, prerequisitesArea,
                responsibleField, tagsField, stepsArea, observationsArea, linksArea
        };
        for (JTextComponent component : textComponents) {
            component.getDocument().addUndoableEditListener(event -> undoManager.addEdit(event.getEdit()));
        }

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

    /** Desfaz a última edição de texto no editor, se houver. */
    public void undo() {
        try {
            if (undoManager.canUndo()) {
                undoManager.undo();
                changed();
            }
        } catch (CannotUndoException ignored) {
            // nada a desfazer
        }
    }

    /** Refaz a última edição de texto desfeita, se houver. */
    public void redo() {
        try {
            if (undoManager.canRedo()) {
                undoManager.redo();
                changed();
            }
        } catch (CannotRedoException ignored) {
            // nada a refazer
        }
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }

    private void changed() {
        if (!filling) {
            changeListener.run();
        }
    }

    private String formatStep(ProcessStep step) {
        String title = safe(step.getTitle()).replace("\n", " ");
        String description = safe(step.getDescription()).replace("\n", " ");
        return description.isBlank() ? title : title + " :: " + description;
    }

    private static JTextArea area(int rows) {
        JTextArea area = new JTextArea(rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static JScrollPane scroll(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new java.awt.Dimension(200, Math.max(80, area.getRows() * 22)));
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        return scrollPane;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
