package br.com.magnobraccini.helplixo.view.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.GridLayout;

/** Diálogo de configuração do intervalo entre eventos F6 simulados. */
public final class AutoClickerDialog {

    public record Result(int intervalMillis) {
    }

    public Result request(Component parent, int currentIntervalMillis) {
        JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(currentIntervalMillis, 20, 60000, 10));

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Intervalo entre F6 (ms):"));
        panel.add(intervalSpinner);
        JLabel hint = new JLabel("F6 é enviado à janela que estiver com foco.");
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        hint.setEnabled(false);
        panel.add(hint);

        int option = JOptionPane.showConfirmDialog(parent, panel, "Configurar Autoclicker",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        return new Result((Integer) intervalSpinner.getValue());
    }
}
