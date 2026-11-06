package br.com.magnobraccini.helplixo.view.process;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.function.Supplier;

/**
 * Ícones vetoriais 16×16 da barra do editor, desenhados em Java2D. Ficam nítidos em qualquer
 * escala do Windows e herdam a cor do botão (cinza quando desabilitado), sem depender de glifos
 * Unicode que mudam de fonte para fonte.
 */
final class EditorIcons {
    static final int SIZE = 16;

    private EditorIcons() { }

    @FunctionalInterface
    interface Painter { void paint(Graphics2D g); }

    static Icon of(Painter painter) { return new StrokeIcon(painter, null); }

    /** Ícone com uma faixa colorida embaixo (cor do texto / realce), que acompanha a última cor usada. */
    static Icon withSwatch(Painter painter, Supplier<Color> swatch) { return new StrokeIcon(painter, swatch); }

    private record StrokeIcon(Painter painter, Supplier<Color> swatch) implements Icon {
        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }

        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            Color base = c == null ? Color.DARK_GRAY : c.getForeground();
            if (c != null && !c.isEnabled()) base = new Color(base.getRed(), base.getGreen(), base.getBlue(), 90);
            g.setColor(base);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            painter.paint(g);
            if (swatch != null) {
                Color color = swatch.get();
                g.setColor(color == null ? new Color(0, 0, 0, 0) : color);
                g.fillRoundRect(1, 13, 14, 3, 2, 2);
                if (color == null) {
                    g.setColor(base);
                    g.setStroke(new BasicStroke(1f));
                    g.drawRoundRect(1, 13, 13, 2, 2, 2);
                }
            }
            g.dispose();
        }
    }

    static final Painter UNDO = g -> {
        Path2D p = new Path2D.Float();
        p.moveTo(4, 8); p.curveTo(5, 4.5, 11, 4, 13, 8); p.curveTo(14, 10.5, 12.5, 12.5, 10, 13);
        g.draw(p);
        g.draw(line(4, 8, 4, 4.5f)); g.draw(line(4, 8, 7.5f, 8));
    };

    static final Painter REDO = g -> {
        Path2D p = new Path2D.Float();
        p.moveTo(12, 8); p.curveTo(11, 4.5, 5, 4, 3, 8); p.curveTo(2, 10.5, 3.5, 12.5, 6, 13);
        g.draw(p);
        g.draw(line(12, 8, 12, 4.5f)); g.draw(line(12, 8, 8.5f, 8));
    };

    static final Painter BULLETS = g -> {
        for (int i = 0; i < 3; i++) {
            float y = 3.5f + i * 4.5f;
            g.fill(new java.awt.geom.Ellipse2D.Float(1.6f, y - 1.4f, 2.8f, 2.8f));
            g.draw(line(7, y, 14.5f, y));
        }
    };

    static final Painter NUMBERS = g -> {
        java.awt.Font font = g.getFont().deriveFont(java.awt.Font.BOLD, 6.5f);
        g.setFont(font);
        for (int i = 0; i < 3; i++) {
            float y = 3.5f + i * 4.5f;
            g.drawString(String.valueOf(i + 1), 1.2f, y + 2.4f);
            g.draw(line(7, y, 14.5f, y));
        }
    };

    static final Painter OUTDENT = g -> {
        g.draw(line(2, 2.5f, 14, 2.5f)); g.draw(line(8, 6.5f, 14, 6.5f));
        g.draw(line(8, 10.5f, 14, 10.5f)); g.draw(line(2, 14, 14, 14));
        Path2D p = new Path2D.Float(); p.moveTo(5, 6); p.lineTo(2.2, 8.5); p.lineTo(5, 11); g.draw(p);
    };

    static final Painter INDENT = g -> {
        g.draw(line(2, 2.5f, 14, 2.5f)); g.draw(line(8, 6.5f, 14, 6.5f));
        g.draw(line(8, 10.5f, 14, 10.5f)); g.draw(line(2, 14, 14, 14));
        Path2D p = new Path2D.Float(); p.moveTo(2.2, 6); p.lineTo(5, 8.5); p.lineTo(2.2, 11); g.draw(p);
    };

    static Painter align(int kind) {
        return g -> {
            float[] widths = {12, 8, 12, 6};
            for (int i = 0; i < 4; i++) {
                float y = 2.5f + i * 3.8f;
                float w = kind == 3 ? 12 : widths[i];
                float x = switch (kind) { case 1 -> 2 + (12 - w) / 2; case 2 -> 14 - w; default -> 2; };
                g.draw(line(x, y, x + w, y));
            }
        };
    }

    static final Painter LINK = g -> {
        g.draw(new java.awt.geom.RoundRectangle2D.Float(1.5f, 6, 7, 4.5f, 4.5f, 4.5f));
        g.draw(new java.awt.geom.RoundRectangle2D.Float(7.5f, 5.5f, 7, 4.5f, 4.5f, 4.5f));
    };

    static final Painter DIVIDER = g -> {
        g.draw(line(1.5f, 8, 14.5f, 8));
        g.setStroke(new BasicStroke(1f));
        g.draw(line(4, 3.5f, 12, 3.5f)); g.draw(line(4, 12.5f, 12, 12.5f));
    };

    static final Painter CODE = g -> {
        Path2D l = new Path2D.Float(); l.moveTo(5.5, 4); l.lineTo(1.8, 8); l.lineTo(5.5, 12); g.draw(l);
        Path2D r = new Path2D.Float(); r.moveTo(10.5, 4); r.lineTo(14.2, 8); r.lineTo(10.5, 12); g.draw(r);
        g.draw(line(9, 3, 7, 13));
    };

    static final Painter CLEAR = g -> {
        g.draw(line(3, 3, 11, 3)); g.draw(line(7, 3, 5, 13));
        g.draw(line(10, 9.5f, 14, 13.5f)); g.draw(line(14, 9.5f, 10, 13.5f));
    };

    static final Painter TEXT_COLOR = g -> {
        Path2D p = new Path2D.Float(); p.moveTo(3.5, 11); p.lineTo(8, 1.8); p.lineTo(12.5, 11); g.draw(p);
        g.draw(line(5.3f, 7.4f, 10.7f, 7.4f));
    };

    static final Painter HIGHLIGHT = g -> {
        Path2D p = new Path2D.Float();
        p.moveTo(5, 10.5); p.lineTo(10.5, 2.5); p.lineTo(13.5, 4.8); p.lineTo(8, 12); p.closePath();
        g.draw(p);
        g.draw(line(5, 10.5f, 3.5f, 12)); g.draw(line(3.5f, 12, 7, 12));
    };

    static final Painter MORE = g -> {
        for (int i = 0; i < 3; i++) g.fill(new java.awt.geom.Ellipse2D.Float(2.2f + i * 4.6f, 6.8f, 2.4f, 2.4f));
    };

    static final Painter CHEVRON = g -> {
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Float(); p.moveTo(5, 7); p.lineTo(8, 10); p.lineTo(11, 7); g.draw(p);
    };

    private static java.awt.geom.Line2D line(float x1, float y1, float x2, float y2) {
        return new java.awt.geom.Line2D.Float(x1, y1, x2, y2);
    }
}
