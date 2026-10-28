package br.com.magnobraccini.helplixo.view.dialogs;

import br.com.magnobraccini.helplixo.model.PhoneContact;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;

/** Diálogo de criação/edição de um Ramal. */
public final class PhoneContactDialog {

    public record Result(String name, String sector, String floor, String phone, String extension, String email) {
    }

    public Result request(Component parent, PhoneContact existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.getName() : "", 24);
        JTextField sectorField = new JTextField(editing ? existing.getSector() : "", 24);
        JTextField floorField = new JTextField(editing ? existing.getFloor() : "", 24);
        JTextField phoneField = new JTextField(editing ? existing.getPhone() : "", 24);
        JTextField extensionField = new JTextField(editing ? existing.getExtension() : "", 24);
        JTextField emailField = new JTextField(editing ? existing.getEmail() : "", 24);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Setor:"));
        panel.add(sectorField);
        panel.add(new JLabel("Andar:"));
        panel.add(floorField);
        panel.add(new JLabel("Telefone:"));
        panel.add(phoneField);
        panel.add(new JLabel("Ramal:"));
        panel.add(extensionField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        int option = JOptionPane.showConfirmDialog(parent, panel,
                editing ? "Editar ramal" : "Novo ramal", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            JOptionPane.showMessageDialog(parent, "O nome é obrigatório.", "Helplixo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Result(name.trim(), sectorField.getText(), floorField.getText(), phoneField.getText(),
                extensionField.getText(), emailField.getText());
    }
}
