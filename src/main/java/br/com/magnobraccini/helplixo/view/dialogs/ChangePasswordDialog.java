package br.com.magnobraccini.helplixo.view.dialogs;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridLayout;

/**
 * Diálogo de troca da senha de edição: exige a senha atual e uma nova senha
 * (com confirmação), sem expor a senha em texto simples na tela.
 */
public final class ChangePasswordDialog {

    public record Result(char[] masterPassword, String newPassword) {
    }

    public Result request(Component parent) {
        JPasswordField masterField = new JPasswordField(20);
        JPasswordField newField = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Senha mestre:"));
        panel.add(masterField);
        panel.add(new JLabel("Nova senha:"));
        panel.add(newField);
        panel.add(new JLabel("Confirmar nova senha:"));
        panel.add(confirmField);

        int option = JOptionPane.showConfirmDialog(parent, panel, "Alterar senha de edição",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        char[] master = masterField.getPassword();
        char[] newPassword = newField.getPassword();
        char[] confirmPassword = confirmField.getPassword();

        if (newPassword.length == 0) {
            JOptionPane.showMessageDialog(parent, "A nova senha não pode ser vazia.",
                    "Helplixo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (!java.util.Arrays.equals(newPassword, confirmPassword)) {
            JOptionPane.showMessageDialog(parent, "A confirmação não coincide com a nova senha.",
                    "Helplixo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Result(master, new String(newPassword));
    }
}
