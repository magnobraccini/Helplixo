package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/**
 * Card "Informações" do painel lateral direito: metadados reais de {@link ProcessDocument}
 * (categoria, responsável, tags e datas de criação/atualização).
 * Nenhum campo aqui é inventado — todos já existem no model; apenas a apresentação é nova.
 */
public final class InformationCard extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final JPanel fields = new JPanel(new GridLayout(0, 1, 0, 10));

    public InformationCard() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Palette.CONTENT_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.SIDEBAR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("Informações");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        fields.setOpaque(false);
        add(fields, BorderLayout.CENTER);
    }

    public void render(ProcessDocument process) {
        fields.removeAll();
        fields.add(field("Categoria", safe(process.getCategory(), "—")));
        fields.add(field("Responsável", safe(process.getResponsible(), "—")));
        fields.add(field("Tags", process.getTags().isEmpty() ? "—" : String.join(", ", process.getTags())));
        fields.add(field("Criado em", process.getCreatedAt() == null ? "—" : process.getCreatedAt().format(DATE_TIME)));
        fields.add(field("Atualizado em", process.getUpdatedAt() == null ? "—" : process.getUpdatedAt().format(DATE_TIME)));
        fields.revalidate();
        fields.repaint();
    }

    public void renderEmpty() {
        fields.removeAll();
        fields.revalidate();
        fields.repaint();
    }

    private JPanel field(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setOpaque(false);
        JLabel labelText = new JLabel(label);
        labelText.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        labelText.setFont(labelText.getFont().deriveFont(Font.BOLD, 11f));
        JTextArea valueText = new JTextArea(value);
        valueText.setEditable(false);
        valueText.setLineWrap(true);
        valueText.setWrapStyleWord(true);
        valueText.setOpaque(false);
        valueText.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        valueText.setFont(valueText.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(labelText, BorderLayout.NORTH);
        panel.add(valueText, BorderLayout.CENTER);
        return panel;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
