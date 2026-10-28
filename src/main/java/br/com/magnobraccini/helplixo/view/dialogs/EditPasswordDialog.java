package br.com.magnobraccini.helplixo.view.dialogs;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import java.awt.Component;

public final class EditPasswordDialog {
    public char[] request(Component parent) {
        JPasswordField passwordField = new JPasswordField(20);
        int option = JOptionPane.showConfirmDialog(parent, passwordField,
                "Senha do modo de edição", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return option == JOptionPane.OK_OPTION ? passwordField.getPassword() : null;
    }
}
