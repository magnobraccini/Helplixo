package br.com.magnobraccini.helplixo.view.dialogs;

import br.com.magnobraccini.helplixo.model.Notice;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/**
 * Caixa de detalhe de um aviso: quem publicou, data, hora, categoria e o texto. Usada tanto pelo
 * Dashboard quanto pela tela de Avisos, para não duplicar a mesma lógica em dois lugares.
 */
public final class NoticeDetailDialog {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private NoticeDetailDialog() {
    }

    /**
     * Mostra o detalhe do aviso. Se {@code onOpenInNotices} não for {@code null}, aparece um
     * botão "Abrir em Avisos" que aciona esse callback (usado a partir do Dashboard); se for
     * {@code null}, o botão não aparece (usado a partir da própria tela de Avisos).
     */
    public static void show(Component parent, Notice notice, Runnable onOpenInNotices) {
        JPanel fields = new JPanel(new GridLayout(0, 1, 2, 6));
        fields.add(line("Publicado por", safe(notice.getCreatedBy())));
        fields.add(line("Data", notice.getCreatedAt() == null ? "—" : notice.getCreatedAt().format(DATE)));
        fields.add(line("Hora", notice.getCreatedAt() == null ? "—" : notice.getCreatedAt().format(TIME)));
        fields.add(line("Categoria", notice.getCategory() == null ? "—" : notice.getCategory().getDisplayName()));

        JTextArea messageArea = new JTextArea(safe(notice.getMessage()), 5, 30);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JLabel messageLabel = new JLabel("Aviso");
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
        JPanel messagePanel = new JPanel(new BorderLayout(0, 4));
        messagePanel.add(messageLabel, BorderLayout.NORTH);
        messagePanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        messageArea.setBackground(fields.getBackground());

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(fields, BorderLayout.NORTH);
        content.add(messagePanel, BorderLayout.CENTER);

        if (onOpenInNotices == null) {
            JOptionPane.showMessageDialog(parent, content, notice.getTitle(), JOptionPane.PLAIN_MESSAGE);
            return;
        }
        Object[] options = {"Abrir em Avisos", "Fechar"};
        int choice = JOptionPane.showOptionDialog(parent, content, notice.getTitle(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (choice == 0) {
            onOpenInNotices.run();
        }
    }

    private static JLabel line(String label, String value) {
        return new JLabel("<html><b>" + label + ":</b> " + escape(value) + "</html>");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
