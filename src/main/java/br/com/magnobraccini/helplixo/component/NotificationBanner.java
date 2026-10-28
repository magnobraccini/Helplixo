package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Faixa de notificação não bloqueante, exibida entre o header e o conteúdo
 * principal. Usada para avisos como "dados atualizados por outro usuário",
 * sem interromper o que a pessoa está fazendo.
 */
public final class NotificationBanner extends JPanel {
    private static final Color BACKGROUND = new Color(255, 205, 92);
    private static final Color TEXT = new Color(46, 34, 6);

    private final JLabel messageLabel = new JLabel();
    private final JButton actionButton = new JButton();

    public NotificationBanner() {
        setLayout(new BorderLayout(10, 0));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.WARNING),
                BorderFactory.createEmptyBorder(7, 16, 7, 12)));

        messageLabel.setForeground(TEXT);
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 13f));
        add(messageLabel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actionButton.putClientProperty("JButton.buttonType", "roundRect");
        actionButton.setFocusable(false);

        JButton dismissButton = new JButton("✕");
        dismissButton.setFocusable(false);
        dismissButton.setBorderPainted(false);
        dismissButton.setContentAreaFilled(false);
        dismissButton.setToolTipText("Dispensar aviso");
        dismissButton.addActionListener(event -> setVisible(false));

        actions.add(actionButton);
        actions.add(dismissButton);
        add(actions, BorderLayout.EAST);

        setVisible(false);
    }

    /** Exibe o aviso com uma ação principal (ex.: "Atualizar agora"). */
    public void show(String message, String actionText, Runnable onAction) {
        messageLabel.setText(message);
        actionButton.setText(actionText);
        for (var listener : actionButton.getActionListeners()) {
            actionButton.removeActionListener(listener);
        }
        actionButton.addActionListener(event -> {
            setVisible(false);
            onAction.run();
        });
        setVisible(true);
    }

    public void dismiss() {
        setVisible(false);
    }
}
