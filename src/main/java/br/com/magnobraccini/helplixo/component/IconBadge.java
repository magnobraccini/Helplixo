package br.com.magnobraccini.helplixo.component;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Emblema quadrado de cantos arredondados com um glifo/emoji centralizado, usado para dar
 * identidade visual (cor + ícone) a linhas de lista e cabeçalhos de card no Dashboard. Puramente
 * decorativo — não guarda nenhum dado, só apresenta o que já vem do model.
 */
public final class IconBadge extends JComponent {
    private static final int ARC = 10;

    private String glyph;
    private Color background;
    private final int size;

    public IconBadge(String glyph, Color background) {
        this(glyph, background, 30);
    }

    public IconBadge(String glyph, Color background, int size) {
        this.glyph = glyph == null ? "" : glyph;
        this.background = background;
        this.size = size;
        setOpaque(false);
        setPreferredSize(new Dimension(size, size));
    }

    public void setGlyph(String glyph) {
        this.glyph = glyph == null ? "" : glyph;
        repaint();
    }

    public void setBadgeColor(Color background) {
        this.background = background;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, size - 1, size - 1, ARC, ARC);
        if (!glyph.isBlank()) {
            g2.setFont(getFont().deriveFont((float) Math.round(size * 0.5)));
            g2.setColor(Color.WHITE);
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(glyph);
            float x = (size - textWidth) / 2f;
            float y = (size + metrics.getAscent() - metrics.getDescent()) / 2f;
            g2.drawString(glyph, x, y);
        }
        g2.dispose();
    }
}
