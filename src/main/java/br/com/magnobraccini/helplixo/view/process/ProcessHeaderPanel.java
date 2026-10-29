package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Cabeçalho reaproveitado por {@link ProcessViewerPanel} e {@link ProcessEditorPanel}: campo de
 * título com moldura (rótulo "Título" acima, igual à referência visual) e o alternador
 * "Visualização / Edição" na mesma linha, à direita. Não contém lógica própria de troca de modo
 * — apenas delega para os callbacks já ligados por {@link ProcessPanel} aos botões reais de
 * Editar/Cancelar da toolbar, para não duplicar comportamento. A estrela de favorito não mora
 * mais aqui — subiu para a barra de breadcrumb, junto do caminho, como na referência.
 */
public final class ProcessHeaderPanel extends JPanel {
    private final JLabel titleCaption = new JLabel("Título");
    private final JTextField titleField = new JTextField();
    private final JButton viewModeButton = new JButton("👁 Visualização");
    private final JButton editModeButton = new JButton("✎ Edição");
    private final JPanel modeToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel titleBlock = new JPanel(new BorderLayout(0, 4));

    private Runnable switchToViewListener = () -> { };
    private Runnable switchToEditListener = () -> { };

    public ProcessHeaderPanel() {
        setLayout(new BorderLayout(16, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        titleCaption.setFont(titleCaption.getFont().deriveFont(Font.PLAIN, 12f));
        titleCaption.setForeground(Palette.CONTENT_TEXT_SECONDARY);

        titleField.setFont(titleField.getFont().deriveFont(Font.BOLD, 16f));
        titleField.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        titleField.setBackground(Palette.CONTENT_BACKGROUND);
        titleField.setCaretColor(Palette.CONTENT_TEXT_PRIMARY);
        titleField.setEditable(false);
        titleField.putClientProperty("JTextField.arc", 8);
        titleField.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        titleBlock.setOpaque(false);
        titleBlock.add(titleCaption, BorderLayout.NORTH);
        titleBlock.add(titleField, BorderLayout.CENTER);
        titleBlock.setPreferredSize(new java.awt.Dimension(360, 56));

        viewModeButton.setFocusable(false);
        editModeButton.setFocusable(false);
        viewModeButton.putClientProperty("JButton.buttonType", "tab");
        editModeButton.putClientProperty("JButton.buttonType", "tab");
        viewModeButton.addActionListener(event -> switchToViewListener.run());
        editModeButton.addActionListener(event -> switchToEditListener.run());
        modeToggle.setOpaque(false);
        modeToggle.add(modeLabel());
        modeToggle.add(viewModeButton);
        modeToggle.add(editModeButton);

        add(titleBlock, BorderLayout.WEST);
        add(modeToggle, BorderLayout.EAST);
    }

    private JLabel modeLabel() {
        JLabel label = new JLabel("Modo:  ");
        label.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        return label;
    }

    public void setTitle(String title) {
        titleField.setText(title == null ? "" : title);
    }

    public String getTitleText() {
        return titleField.getText();
    }

    /** Componente real usado pelo editor para digitar o título (undo/redo e rastreio de alteração se ligam nele). */
    public JTextField getTitleField() {
        return titleField;
    }

    /** No visualizador o campo fica com aparência de somente leitura; no editor, editável. */
    public void setTitleEditable(boolean editable) {
        titleField.setEditable(editable);
        titleField.setBackground(editable ? Palette.CONTENT_BACKGROUND : Palette.CONTENT_SURFACE_ALT);
    }

    /** Controla se o alternador Visualização/Edição aparece — pastas não têm modo de edição. */
    public void setModeToggleVisible(boolean visible) {
        modeToggle.setVisible(visible);
    }

    /** Marca qual dos dois modos está ativo (o botão ativo fica desabilitado, como uma aba selecionada). */
    public void setEditingMode(boolean editing) {
        viewModeButton.setEnabled(editing);
        editModeButton.setEnabled(!editing);
        viewModeButton.putClientProperty("JButton.buttonType", editing ? "tab" : "default");
        editModeButton.putClientProperty("JButton.buttonType", editing ? "default" : "tab");
    }

    /** Habilita/desabilita a troca para o modo de edição (ex.: sem permissão de edição). */
    public void setEditAvailable(boolean available) {
        editModeButton.setEnabled(available);
    }

    public void onSwitchToView(Runnable listener) {
        switchToViewListener = listener == null ? () -> { } : listener;
    }

    public void onSwitchToEdit(Runnable listener) {
        switchToEditListener = listener == null ? () -> { } : listener;
    }
}
