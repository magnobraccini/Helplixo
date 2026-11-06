package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Cabeçalho comum de {@link ProcessViewerPanel} e {@link ProcessEditorPanel}: o título como
 * manchete do documento e, à direita, o seletor Visualizar / Editar. O título tem a mesma fonte
 * e posição nos dois modos — ao editar ele apenas ganha um sublinhado de campo —, então alternar
 * entre ler e editar não faz a tela "pular". A troca de modo delega para os botões reais de
 * Editar/Cancelar da barra (ligados por {@link ProcessPanel}), sem duplicar comportamento.
 */
public final class ProcessHeaderPanel extends JPanel {
    private static final Border READ_BORDER = BorderFactory.createEmptyBorder(4, 0, 6, 0);
    private static final Border EDIT_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.CONTENT_BORDER),
            BorderFactory.createEmptyBorder(4, 0, 5, 0));
    private static final Border FOCUS_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, Palette.ACCENT),
            BorderFactory.createEmptyBorder(4, 0, 4, 0));

    private final JTextField titleField = new JTextField();
    private final JToggleButton viewModeButton = segment("Visualizar");
    private final JToggleButton editModeButton = segment("Editar");
    private final JPanel modeToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    private boolean editingMode;
    private Runnable switchToViewListener = () -> { };
    private Runnable switchToEditListener = () -> { };

    public ProcessHeaderPanel() {
        setLayout(new BorderLayout(24, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        titleField.setFont(titleField.getFont().deriveFont(Font.BOLD, 22f));
        titleField.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        titleField.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
        titleField.setSelectionColor(new Color(187, 214, 251));
        titleField.setSelectedTextColor(Palette.CONTENT_TEXT_PRIMARY);
        titleField.putClientProperty("JTextField.placeholderText", "Dê um título ao processo");
        titleField.getAccessibleContext().setAccessibleName("Título do processo");
        titleField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { if (titleField.isEditable()) titleField.setBorder(FOCUS_BORDER); }
            @Override public void focusLost(FocusEvent event) { titleField.setBorder(titleField.isEditable() ? EDIT_BORDER : READ_BORDER); }
        });
        setTitleEditable(false);

        ButtonGroup group = new ButtonGroup();
        group.add(viewModeButton);
        group.add(editModeButton);
        // Cada cabeçalho pertence a um único modo (o do leitor ou o do editor). Depois do clique,
        // ele volta a mostrar o próprio modo: se a troca for cancelada (ex.: "Descartar
        // alterações?" → Não), o seletor não fica mentindo sobre o estado da tela.
        viewModeButton.addActionListener(event -> { switchToViewListener.run(); restoreMode(); });
        editModeButton.addActionListener(event -> { switchToEditListener.run(); restoreMode(); });
        modeToggle.setOpaque(true);
        modeToggle.setBackground(Palette.CONTENT_SURFACE_ALT);
        modeToggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        modeToggle.add(viewModeButton);
        modeToggle.add(editModeButton);

        JPanel modeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        modeWrapper.setOpaque(false);
        modeWrapper.add(modeToggle);

        add(titleField, BorderLayout.CENTER);
        add(modeWrapper, BorderLayout.EAST);
        setEditingMode(false);
    }

    private static JToggleButton segment(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12.5f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addChangeListener(event -> paintSegment(button));
        paintSegment(button);
        return button;
    }

    private static void paintSegment(JToggleButton button) {
        boolean selected = button.isSelected();
        button.setBackground(selected ? Palette.CONTENT_BACKGROUND : Palette.CONTENT_SURFACE_ALT);
        button.setForeground(!button.isEnabled() && !selected ? new Color(160, 168, 180)
                : selected ? Palette.CONTENT_TEXT_PRIMARY : Palette.CONTENT_TEXT_SECONDARY);
        button.setFont(button.getFont().deriveFont(selected ? Font.BOLD : Font.PLAIN));
        button.setBorder(selected
                ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 13, 4, 13))
                : BorderFactory.createEmptyBorder(5, 14, 5, 14));
    }

    public void setTitle(String title) {
        titleField.setText(title == null ? "" : title);
        titleField.setCaretPosition(0);
    }

    public String getTitleText() {
        return titleField.getText();
    }

    /** Componente real usado pelo editor para digitar o título (undo/redo e rastreio de alteração se ligam nele). */
    public JTextField getTitleField() {
        return titleField;
    }

    /** No visualizador o título é uma manchete; no editor, o mesmo texto vira um campo sublinhado. */
    public void setTitleEditable(boolean editable) {
        titleField.setEditable(editable);
        titleField.setFocusable(editable);
        titleField.setOpaque(false);
        titleField.setBackground(Palette.CONTENT_BACKGROUND);
        titleField.setBorder(editable ? EDIT_BORDER : READ_BORDER);
        titleField.setCursor(Cursor.getPredefinedCursor(editable ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Controla se o seletor Visualizar/Editar aparece — pastas não têm modo de edição. */
    public void setModeToggleVisible(boolean visible) {
        modeToggle.setVisible(visible);
    }

    private void restoreMode() {
        javax.swing.SwingUtilities.invokeLater(() -> setEditingMode(editingMode));
    }

    public void setEditingMode(boolean editing) {
        editingMode = editing;
        (editing ? editModeButton : viewModeButton).setSelected(true);
        paintSegment(viewModeButton);
        paintSegment(editModeButton);
    }

    /** Habilita/desabilita a troca para o modo de edição e explica o motivo quando indisponível. */
    public void setEditAvailable(boolean available) {
        editModeButton.setEnabled(available || editModeButton.isSelected());
        editModeButton.setToolTipText(available ? "Editar este processo (Ctrl+E)"
                : "Para editar, clique em “Ativar edição” no topo da janela");
        viewModeButton.setToolTipText("Voltar para a leitura");
        paintSegment(editModeButton);
    }

    public void onSwitchToView(Runnable listener) {
        switchToViewListener = listener == null ? () -> { } : listener;
    }

    public void onSwitchToEdit(Runnable listener) {
        switchToEditListener = listener == null ? () -> { } : listener;
    }
}
