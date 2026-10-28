package br.com.magnobraccini.helplixo.view.dialogs;

import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.model.NoticeCategory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * Diálogo de criação/edição de um Aviso. Reconstruído com {@code GridBagLayout} — a versão
 * anterior usava {@code GridLayout}, que força todas as linhas a terem a MESMA altura; como a
 * linha da mensagem precisa ser alta, os campos de uma linha só (Título, Categoria, Publicado
 * por) ficavam esticados em caixas enormes e vazias, sem parecer com o resto do app.
 */
public final class NoticeDialog {

    public record Result(String title, String message, NoticeCategory category, String createdBy) {
    }

    public Result request(Component parent, Notice existing) {
        boolean editing = existing != null;
        JTextField titleField = new JTextField(editing ? existing.getTitle() : "");
        titleField.setPreferredSize(new java.awt.Dimension(320, 30));
        JComboBox<NoticeCategory> categoryCombo = new JComboBox<>(NoticeCategory.values());
        categoryCombo.setSelectedItem(editing ? existing.getCategory() : NoticeCategory.SISTEMA);
        JTextField createdByField = new JTextField(editing ? existing.getCreatedBy() : "");
        createdByField.setPreferredSize(new java.awt.Dimension(320, 30));
        JTextArea messageArea = new JTextArea(editing ? existing.getMessage() : "");
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setRows(5);
        messageArea.setColumns(30);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        c.gridx = 0;
        int y = 0;

        c.gridy = y++; c.weighty = 0;
        form.add(new JLabel("Título"), c);
        c.gridy = y++;
        form.add(titleField, c);

        c.gridy = y++;
        form.add(new JLabel("Categoria"), c);
        c.gridy = y++;
        form.add(categoryCombo, c);

        c.gridy = y++;
        form.add(new JLabel("Publicado por"), c);
        c.gridy = y++;
        form.add(createdByField, c);

        c.gridy = y++;
        form.add(new JLabel("Mensagem"), c);
        c.gridy = y++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(new JScrollPane(messageArea), c);

        form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        Window owner = parent instanceof Window window ? window
                : javax.swing.SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, editing ? "Editar aviso" : "Novo aviso", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(0, 12));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        dialog.add(form, BorderLayout.CENTER);

        boolean[] confirmed = {false};
        JButton okButton = new JButton("Salvar");
        okButton.putClientProperty("JButton.buttonType", "default");
        JButton cancelButton = new JButton("Cancelar");
        okButton.addActionListener(event -> { confirmed[0] = true; dialog.dispose(); });
        cancelButton.addActionListener(event -> dialog.dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancelButton);
        buttons.add(okButton);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(okButton);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        if (!confirmed[0]) {
            return null;
        }
        String title = titleField.getText();
        if (title == null || title.isBlank()) {
            JOptionPane.showMessageDialog(parent, "O título é obrigatório.", "Helplixo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Result(title.trim(), messageArea.getText(), (NoticeCategory) categoryCombo.getSelectedItem(),
                createdByField.getText());
    }
}
