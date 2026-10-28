package br.com.magnobraccini.helplixo.view.dialogs;

import br.com.magnobraccini.helplixo.model.Tool;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/** Diálogo de criação/edição de uma Ferramenta. */
public final class ToolDialog {

    public record Result(String name, String description, String category, String executablePath, String command,
                         String icon) {
    }

    public Result request(Component parent, Tool existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.getName() : "", 24);
        JTextField descriptionField = new JTextField(editing ? existing.getDescription() : "", 24);
        JTextField categoryField = new JTextField(editing ? existing.getCategory() : "", 24);
        JTextField executableField = new JTextField(editing ? existing.getExecutablePath() : "", 24);
        JTextField commandField = new JTextField(editing ? existing.getCommand() : "", 24);
        JTextField iconField = new JTextField(editing ? existing.getIcon() : "🛠", 6);

        JPanel executableRow = buildExecutableRow(parent, executableField);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Nome:"));
        panel.add(nameField);
        panel.add(new JLabel("Descrição:"));
        panel.add(descriptionField);
        panel.add(new JLabel("Categoria:"));
        panel.add(categoryField);
        panel.add(new JLabel("Arquivo anexado (batch/executável, opcional):"));
        panel.add(executableRow);
        panel.add(new JLabel("Comando (ex.: cmd, powershell, mstsc, control) — se preenchido, tem prioridade"
                + " sobre o arquivo anexado:"));
        panel.add(commandField);
        panel.add(new JLabel("Ícone (emoji):"));
        panel.add(iconField);

        int option = JOptionPane.showConfirmDialog(parent, panel,
                editing ? "Editar ferramenta" : "Nova ferramenta", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            JOptionPane.showMessageDialog(parent, "O nome é obrigatório.", "Helplixo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new Result(name.trim(), descriptionField.getText(), categoryField.getText(),
                executableField.getText(), commandField.getText(), iconField.getText());
    }

    /**
     * Monta a linha do campo "Executável": o campo em si não é digitável — clicar nele (ou no
     * botão "Anexar arquivo...") abre um seletor de arquivos para anexar um .bat/.cmd/.exe.
     * A ideia é que o caminho escolhido ali seja exatamente o que {@code ToolService.execute}
     * manda o Windows abrir/rodar quando a ferramenta é executada.
     */
    private JPanel buildExecutableRow(Component parent, JTextField executableField) {
        executableField.setEditable(false);
        executableField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        executableField.putClientProperty("JTextField.placeholderText",
                "Clique aqui (ou em \"Anexar arquivo...\") para escolher um .bat, .cmd ou .exe");
        executableField.setToolTipText("Clique para escolher o arquivo que será executado por esta ferramenta.");

        JButton attachButton = new JButton("📎 Anexar arquivo...");
        JButton clearButton = new JButton("✕");
        clearButton.setMargin(new Insets(2, 6, 2, 6));
        clearButton.setToolTipText("Remover arquivo anexado");

        Runnable browse = () -> chooseFile(parent, executableField);
        attachButton.addActionListener(event -> browse.run());
        clearButton.addActionListener(event -> executableField.setText(""));
        executableField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                browse.run();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(attachButton);
        buttons.add(clearButton);

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(executableField, BorderLayout.CENTER);
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private void chooseFile(Component parent, JTextField executableField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Anexar arquivo à ferramenta");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        String current = executableField.getText();
        if (current != null && !current.isBlank()) {
            File startDir = new File(current).getParentFile();
            if (startDir != null && startDir.isDirectory()) {
                chooser.setCurrentDirectory(startDir);
            }
        }

        FileNameExtensionFilter batchFilter =
                new FileNameExtensionFilter("Arquivos batch (*.bat, *.cmd)", "bat", "cmd");
        chooser.addChoosableFileFilter(batchFilter);
        chooser.setFileFilter(batchFilter);

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            executableField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}
