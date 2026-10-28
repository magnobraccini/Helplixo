package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Card moderno de cantos arredondados com sombra suave, usado nas caixas do Dashboard
 * (Recentes / Avisos / Favoritos). Tem um cabeçalho fixo (emblema colorido + título + contador
 * opcional) e um corpo livre ({@link #getBody()}) para qualquer conteúdo, mantendo a mesma
 * aparência em todos os cards.
 */
public final class Card extends JPanel {
    private static final int ARC = 18;

    private final JPanel body = new JPanel(new BorderLayout());
    private final JLabel countLabel = new JLabel();

    public Card(String glyph, Color badgeColor, String title) {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleGroup.setOpaque(false);
        titleGroup.add(new IconBadge(glyph, badgeColor, 32));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Palette.TEXT_ON_DARK);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15.5f));
        titleGroup.add(titleLabel);

        countLabel.setForeground(Palette.TEXT_ON_DARK_MUTED);
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 12f));

        header.add(titleGroup, BorderLayout.WEST);
        header.add(countLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
    }

    public JPanel getBody() {
        return body;
    }

    /** Mostra a contagem de itens no canto superior direito do cabeçalho (vazio se {@code count <= 0}). */
    public void setCount(int count) {
        countLabel.setText(count <= 0 ? "" : count + (count == 1 ? " item" : " itens"));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        g2.setColor(Palette.DASHBOARD_SHADOW);
        g2.fillRoundRect(2, 4, width - 4, height - 6, ARC, ARC);

        g2.setColor(Palette.DASHBOARD_SURFACE);
        g2.fillRoundRect(0, 0, width - 3, height - 7, ARC, ARC);

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Palette.DASHBOARD_BORDER);
        g2.drawRoundRect(0, 0, width - 4, height - 8, ARC, ARC);

        g2.dispose();
    }
}
