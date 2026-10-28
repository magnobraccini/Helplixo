package br.com.magnobraccini.helplixo.component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

public final class DashboardCard extends JPanel {
    private final JLabel valueLabel = new JLabel("0");

    public DashboardCard(String title) {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getForeground(), 1, true),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 13f));
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 28f));
        add(titleLabel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(long value) {
        valueLabel.setText(Long.toString(value));
    }
}
