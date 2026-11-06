package br.com.magnobraccini.helplixo.view.process;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * {@link FlowLayout} que informa a altura real quando os itens quebram de linha. Mantém a barra
 * do editor em uma linha em telas largas e a deixa quebrar (sem cortar botões) em telas estreitas.
 */
final class WrapLayout extends FlowLayout {
    WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

    @Override public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) container = container.getParent();
            if (targetWidth == 0) targetWidth = container.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            Insets insets = target.getInsets();
            int horizontalInsets = insets.left + insets.right + getHgap() * 2;
            int maxWidth = targetWidth - horizontalInsets;
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            for (Component component : target.getComponents()) {
                if (!component.isVisible()) continue;
                Dimension d = preferred ? component.getPreferredSize() : component.getMinimumSize();
                if (rowWidth + d.width > maxWidth) {
                    addRow(dim, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) rowWidth += getHgap();
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(dim, rowWidth, rowHeight);
            dim.width += horizontalInsets;
            dim.height += insets.top + insets.bottom + getVgap() * 2;
            Container scroll = (Container) SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scroll != null && target.isValid()) dim.width -= getHgap() + 1;
            return dim;
        }
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) dim.height += getVgap();
        dim.height += rowHeight;
    }
}
