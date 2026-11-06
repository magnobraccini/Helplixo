package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

/**
 * Card "Informações" do painel lateral: categoria, responsável, tags e datas reais de
 * {@link ProcessDocument}, em linhas compactas rótulo → valor. Campos vazios não aparecem, para
 * que o card mostre só o que existe; as datas aparecem sempre.
 */
public final class InformationCard extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final int LABEL_WIDTH = 92;
    private final JPanel fields = new JPanel();

    public InformationCard() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Palette.CONTENT_SURFACE_ALT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("Informações");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        add(fields, BorderLayout.CENTER);
    }

    public void render(ProcessDocument process) {
        fields.removeAll();
        addField("Categoria", process.getCategory());
        addField("Responsável", process.getResponsible());
        addField("Tags", process.getTags().isEmpty() ? null : String.join(", ", process.getTags()));
        addField("Criado em", process.getCreatedAt() == null ? null : process.getCreatedAt().format(DATE_TIME));
        addField("Atualizado em", process.getUpdatedAt() == null ? null : process.getUpdatedAt().format(DATE_TIME));
        fields.revalidate();
        fields.repaint();
    }

    public void renderEmpty() {
        fields.removeAll();
        fields.revalidate();
        fields.repaint();
    }

    private void addField(String label, String value) {
        if (value == null || value.isBlank()) return;
        if (fields.getComponentCount() > 0) fields.add(Box.createVerticalStrut(7));
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel labelText = new JLabel(label);
        labelText.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        labelText.setFont(labelText.getFont().deriveFont(Font.PLAIN, 12f));
        labelText.setPreferredSize(new Dimension(LABEL_WIDTH, labelText.getPreferredSize().height));
        labelText.setVerticalAlignment(JLabel.TOP);
        JTextArea valueText = new JTextArea(value);
        valueText.setEditable(false);
        valueText.setFocusable(false);
        valueText.setLineWrap(true);
        valueText.setWrapStyleWord(true);
        valueText.setOpaque(false);
        valueText.setBorder(BorderFactory.createEmptyBorder());
        valueText.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        valueText.setFont(valueText.getFont().deriveFont(Font.PLAIN, 12.5f));
        valueText.setColumns(14);
        row.add(labelText, BorderLayout.WEST);
        row.add(valueText, BorderLayout.CENTER);
        fields.add(row);
    }
}
