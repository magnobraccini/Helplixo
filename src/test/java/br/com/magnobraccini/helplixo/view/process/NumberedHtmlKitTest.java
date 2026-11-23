package br.com.magnobraccini.helplixo.view.process;

import org.junit.jupiter.api.Test;

import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberedHtmlKitTest {
    private static HTMLDocument parse(String body) throws Exception {
        NumberedHtmlKit kit = new NumberedHtmlKit();
        HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
        kit.read(new StringReader("<html><body>" + body + "</body></html>"), document, 0);
        return document;
    }

    private static List<Element> all(Element root, HTML.Tag tag) {
        List<Element> found = new ArrayList<>();
        collect(root, tag, found);
        return found;
    }

    private static void collect(Element element, HTML.Tag tag, List<Element> found) {
        if (NumberedHtmlKit.nameOf(element) == tag) found.add(element);
        for (int i = 0; i < element.getElementCount(); i++) collect(element.getElement(i), tag, found);
    }

    @Test
    void numbersNestedListsLikeWord() throws Exception {
        HTMLDocument document = parse("<ol><li>Um<ol><li>Sub<ol><li>Fundo</li><li>Fundo 2</li></ol></li></ol></li><li>Dois</li></ol>");
        List<String> labels = all(document.getDefaultRootElement(), HTML.Tag.LI).stream()
                .map(NumberedHtmlKit::listLabel).toList();
        assertEquals(List.of("1.", "a.", "i.", "ii.", "2."), labels);
    }

    @Test
    void respectsStartAndContinuation() throws Exception {
        HTMLDocument document = parse("<ol><li>A</li><li>B</li></ol><p>pausa</p>"
                + "<ol class=\"continua\"><li>C</li></ol><ol start=\"7\"><li>D</li></ol>");
        List<String> labels = all(document.getDefaultRootElement(), HTML.Tag.LI).stream()
                .map(NumberedHtmlKit::listLabel).toList();
        assertEquals(List.of("1.", "2.", "3.", "7."), labels);
    }

    @Test
    void numbersOnlyMarkedHeadings() throws Exception {
        HTMLDocument document = parse("<h1 class=\"num\">A</h1><h2 class=\"num\">B</h2><h2>sem número</h2>"
                + "<h3 class=\"num\">C</h3><h1 class=\"num\">D</h1><h2 class=\"num\">E</h2>");
        Element root = document.getDefaultRootElement();
        List<String> labels = new ArrayList<>();
        for (HTML.Tag tag : new HTML.Tag[]{HTML.Tag.H1, HTML.Tag.H2, HTML.Tag.H3}) {
            for (Element heading : all(root, tag)) labels.add(NumberedHtmlKit.headingLabel(heading));
        }
        assertEquals(List.of("1.", "2.", "1.1", "", "2.1", "1.1.1"), labels);
    }

    @Test
    void formatsLettersAndRomanNumerals() {
        assertEquals("aa", NumberedHtmlKit.format(27, 1));
        assertEquals("xiv", NumberedHtmlKit.format(14, 2));
        assertEquals("4", NumberedHtmlKit.format(4, 3));
    }

    @Test
    void serializesLevelsAsNestedLists() {
        List<RichTextEditorPanel.BlockItem> items = new ArrayList<>();
        items.add(new RichTextEditorPanel.BlockItem("ol", 0, "Um", ""));
        items.add(new RichTextEditorPanel.BlockItem("ol", 1, "Sub", ""));
        items.add(new RichTextEditorPanel.BlockItem("ol", 3, "Pula níveis", ""));
        items.add(new RichTextEditorPanel.BlockItem("ol", 0, "Dois", ""));
        items.add(new RichTextEditorPanel.BlockItem("p", 0, "Fim", ""));
        RichTextEditorPanel.normalizeLevels(items);
        assertEquals("<ol><li>Um<ol><li>Sub<ol><li>Pula níveis</li></ol></li></ol></li><li>Dois</li></ol><p>Fim</p>",
                RichTextEditorPanel.serialize(items));
    }

    @Test
    void restartSplitsTheListAndKeepsStart() {
        List<RichTextEditorPanel.BlockItem> items = new ArrayList<>();
        items.add(new RichTextEditorPanel.BlockItem("ol", 0, "A", ""));
        RichTextEditorPanel.BlockItem restarted = new RichTextEditorPanel.BlockItem("ol", 0, "B", "");
        restarted.newList = true;
        restarted.start = "5";
        items.add(restarted);
        assertEquals("<ol><li>A</li></ol><ol start=\"5\"><li>B</li></ol>", RichTextEditorPanel.serialize(items));
    }

    @Test
    void sanitizeKeepsNumberingAttributes() {
        String safe = RichTextHtml.sanitize("<h2 class=\"num\">T</h2><ol start=\"3\" class=\"continua\"><li>x</li></ol>"
                + "<ol class=\"perigo\" start=\"abc\"><li>y</li></ol>");
        assertTrue(safe.contains("<h2 class=\"num\">"), safe);
        assertTrue(safe.contains("<ol start=\"3\" class=\"continua\">"), safe);
        assertTrue(safe.contains("<ol><li>y</li></ol>"), safe);
    }
}
