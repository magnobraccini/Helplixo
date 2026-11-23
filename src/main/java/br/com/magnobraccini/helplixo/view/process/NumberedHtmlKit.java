package br.com.magnobraccini.helplixo.view.process;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ListView;
import javax.swing.text.html.ParagraphView;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kit HTML do editor e da leitura com numeração no estilo do Word.
 * <ul>
 *   <li><b>Listas numeradas em vários níveis:</b> 1. → a. → i. conforme o nível. Os números são
 *   desenhados por este kit (o CSS deixa {@code ol} sem marcador), o que permite respeitar
 *   {@code <ol start="N">} e {@code <ol class="continua">} (continua a numeração da lista anterior
 *   do mesmo nível).</li>
 *   <li><b>Títulos numerados:</b> {@code <h1 class="num">}, {@code h2}, {@code h3} recebem 1. / 1.1 /
 *   1.1.1 calculados na hora de desenhar — o número não fica no texto, então nunca fica desatualizado.</li>
 * </ul>
 */
public final class NumberedHtmlKit extends HTMLEditorKit {
    static final String HEADING_CLASS = "num";
    static final String CONTINUE_CLASS = "continua";
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private final transient ViewFactory factory = new HTMLFactory() {
        @Override public View create(Element element) {
            Object name = element.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (name == HTML.Tag.OL) return new OrderedListView(element);
            if (name == HTML.Tag.H1 || name == HTML.Tag.H2 || name == HTML.Tag.H3) return new HeadingView(element);
            return super.create(element);
        }
    };

    @Override public ViewFactory getViewFactory() { return factory; }

    // ---------------------------------------------------------------- numeração (sem Swing de tela)

    /** Rótulo do item de lista numerada: "3.", "b.", "iv." conforme o nível. */
    static String listLabel(Element item) {
        Element list = item.getParentElement();
        if (list == null || nameOf(list) != HTML.Tag.OL) return "";
        int position = 0;
        for (int i = 0; i < list.getElementCount(); i++) {
            Element child = list.getElement(i);
            if (nameOf(child) != HTML.Tag.LI) continue;
            if (child == item) break;
            position++;
        }
        return format(startOf(list) + position, listDepth(list)) + ".";
    }

    /** Primeiro número da lista: atributo start, continuação da lista anterior ou 1. */
    static int startOf(Element list) {
        if (hasClass(list, CONTINUE_CLASS)) {
            Element previous = previousOrderedList(list);
            if (previous != null) return startOf(previous) + itemCount(previous);
        }
        Object start = list.getAttributes().getAttribute(HTML.Attribute.START);
        if (start != null) {
            try { return Math.max(0, Integer.parseInt(start.toString().trim())); } catch (NumberFormatException ignored) { }
        }
        return 1;
    }

    /** Lista numerada anterior do mesmo nível, ou {@code null}. */
    static Element previousOrderedList(Element list) {
        int depth = listDepth(list);
        List<Element> all = new ArrayList<>();
        collect(list.getDocument().getDefaultRootElement(), HTML.Tag.OL, all);
        Element previous = null;
        for (Element candidate : all) {
            if (candidate == list) return previous;
            if (listDepth(candidate) == depth) previous = candidate;
        }
        return null;
    }

    static int itemCount(Element list) {
        int count = 0;
        for (int i = 0; i < list.getElementCount(); i++) if (nameOf(list.getElement(i)) == HTML.Tag.LI) count++;
        return count;
    }

    /** Quantas listas (ol/ul) envolvem esta — 0 para a lista de primeiro nível. */
    static int listDepth(Element list) {
        int depth = 0;
        for (Element e = list.getParentElement(); e != null; e = e.getParentElement()) {
            Object name = nameOf(e);
            if (name == HTML.Tag.OL || name == HTML.Tag.UL) depth++;
        }
        return depth;
    }

    /** Rótulo do título numerado: "2." para Título 1, "2.1" para Título 2, "2.1.3" para Título 3. */
    static String headingLabel(Element heading) {
        int[] counters = new int[3];
        List<Element> headings = new ArrayList<>();
        collectHeadings(heading.getDocument().getDefaultRootElement(), headings);
        for (Element candidate : headings) {
            if (!hasClass(candidate, HEADING_CLASS)) continue;
            int level = headingLevel(candidate);
            counters[level - 1]++;
            for (int i = level; i < counters.length; i++) counters[i] = 0;
            if (candidate == heading) {
                StringBuilder label = new StringBuilder();
                for (int i = 0; i < level; i++) {
                    if (i > 0) label.append('.');
                    label.append(counters[i]);
                }
                return level == 1 ? label + "." : label.toString();
            }
        }
        return "";
    }

    static int headingLevel(Element element) {
        Object name = nameOf(element);
        if (name == HTML.Tag.H1) return 1;
        if (name == HTML.Tag.H2) return 2;
        if (name == HTML.Tag.H3) return 3;
        return 0;
    }

    static boolean hasClass(Element element, String cssClass) {
        Object value = element.getAttributes().getAttribute(HTML.Attribute.CLASS);
        if (value == null) return false;
        for (String part : value.toString().trim().split("\\s+")) if (part.equalsIgnoreCase(cssClass)) return true;
        return false;
    }

    static Object nameOf(Element element) {
        return element.getAttributes().getAttribute(StyleConstants.NameAttribute);
    }

    private static void collect(Element element, HTML.Tag tag, List<Element> found) {
        if (nameOf(element) == tag) found.add(element);
        for (int i = 0; i < element.getElementCount(); i++) collect(element.getElement(i), tag, found);
    }

    private static void collectHeadings(Element element, List<Element> found) {
        if (headingLevel(element) > 0) { found.add(element); return; }
        for (int i = 0; i < element.getElementCount(); i++) collectHeadings(element.getElement(i), found);
    }

    /** 1 → "1" no nível 0, "a" no nível 1, "i" no nível 2; depois repete. */
    static String format(int number, int depth) {
        return switch (depth % 3) {
            case 1 -> alpha(number);
            case 2 -> roman(number);
            default -> Integer.toString(number);
        };
    }

    private static String alpha(int number) {
        if (number <= 0) return Integer.toString(number);
        StringBuilder text = new StringBuilder();
        int n = number;
        while (n > 0) {
            n--;
            text.insert(0, (char) ('a' + n % 26));
            n /= 26;
        }
        return text.toString();
    }

    private static String roman(int number) {
        if (number <= 0 || number >= 4000) return Integer.toString(number);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"};
        StringBuilder text = new StringBuilder();
        int n = number;
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) { text.append(symbols[i]); n -= values[i]; }
        }
        return text.toString();
    }

    // ---------------------------------------------------------------- desenho

    private static void prepare(Graphics g, Font font, Color color) {
        if (g instanceof Graphics2D g2) {
            Object hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (hints instanceof Map<?, ?> map) g2.addRenderingHints(map);
            else g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        g.setFont(font);
        g.setColor(color);
    }

    /** Lista numerada: desenha o número de cada item à esquerda do texto. */
    static final class OrderedListView extends ListView {
        OrderedListView(Element element) { super(element); }

        @Override protected void paintChild(Graphics g, Rectangle alloc, int index) {
            super.paintChild(g, alloc, index);
            View child = getView(index);
            Element item = child.getElement();
            if (nameOf(item) != HTML.Tag.LI) return;
            String label = listLabel(item);
            if (label.isEmpty()) return;
            AttributeSet attributes = child.getAttributes();
            try {
                Shape first = child.modelToView(item.getStartOffset(), alloc, Position.Bias.Forward);
                Rectangle line = first.getBounds();
                Graphics copy = g.create();
                try {
                    prepare(copy, getStyleSheet().getFont(attributes), getStyleSheet().getForeground(attributes));
                    FontMetrics metrics = copy.getFontMetrics();
                    int x = line.x - 7 - metrics.stringWidth(label);
                    int y = line.y + line.height - metrics.getDescent();
                    copy.drawString(label, x, y);
                } finally {
                    copy.dispose();
                }
            } catch (BadLocationException ignored) {
                // Item vazio no meio da edição: o número volta no próximo desenho.
            }
        }
    }

    /** Título com numeração automática: reserva espaço à esquerda e desenha o número ali. */
    static final class HeadingView extends ParagraphView {
        private static final int GAP = 10;
        private int indent;

        HeadingView(Element element) { super(element); }

        @Override protected void setPropertiesFromAttributes() {
            super.setPropertiesFromAttributes();
            indent = 0;
            if (!hasClass(getElement(), HEADING_CLASS)) return;
            String template = switch (headingLevel(getElement())) {
                case 1 -> "00.";
                case 2 -> "00.00";
                default -> "00.00.00";
            };
            Font font = getStyleSheet().getFont(getAttributes());
            indent = (int) Math.ceil(font.getStringBounds(template, FRC).getWidth()) + GAP;
            setInsets(getTopInset(), (short) (getLeftInset() + indent), getBottomInset(), getRightInset());
        }

        @Override public void paint(Graphics g, Shape allocation) {
            super.paint(g, allocation);
            if (indent == 0) return;
            String label = headingLabel(getElement());
            if (label.isEmpty()) return;
            try {
                Rectangle line = modelToView(getStartOffset(), allocation, Position.Bias.Forward).getBounds();
                Graphics copy = g.create();
                try {
                    prepare(copy, getStyleSheet().getFont(getAttributes()), getStyleSheet().getForeground(getAttributes()));
                    FontMetrics metrics = copy.getFontMetrics();
                    copy.drawString(label, line.x - indent, line.y + line.height - metrics.getDescent());
                } finally {
                    copy.dispose();
                }
            } catch (BadLocationException ignored) {
                // Sem posição válida ainda; o próximo desenho resolve.
            }
        }
    }
}
