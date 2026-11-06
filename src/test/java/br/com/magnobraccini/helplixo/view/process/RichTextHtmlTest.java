package br.com.magnobraccini.helplixo.view.process;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextHtmlTest {
    @Test
    void keepsFormattingProducedByTheEditor() {
        String html = "<h2 align=\"center\">Passo</h2><p>Use <code>gpupdate /force</code>, "
                + "<span style=\"background-color:#fff176;\">realce</span>, H<sub>2</sub>O e x<sup>2</sup>.</p>"
                + "<ol><li>Um</li></ol><blockquote><p>Nota</p></blockquote>";
        String safe = RichTextHtml.sanitize(html);
        assertTrue(safe.contains("<h2 align=\"center\">"), safe);
        assertTrue(safe.contains("<code>gpupdate /force</code>"), safe);
        assertTrue(safe.contains("<span style=\"background-color:#fff176;\">"), safe);
        assertTrue(safe.contains("<sub>2</sub>") && safe.contains("<sup>2</sup>"), safe);
        assertTrue(safe.contains("<ol><li>Um</li></ol>"), safe);
    }

    @Test
    void dropsScriptsAndUnsafeLinks() {
        String safe = RichTextHtml.sanitize("<p onclick=\"x()\">Oi<script>alert(1)</script>"
                + "<a href=\"javascript:alert(1)\">link</a></p>");
        assertEquals("<p>Oi<a>link</a></p>", safe);
    }

    @Test
    void completesWebAddressesAndRejectsOthers() {
        assertEquals("https://intranet.empresa.com.br", RichTextHtml.normalizeUrl(" intranet.empresa.com.br "));
        assertEquals("http://10.0.0.5/painel", RichTextHtml.normalizeUrl("http://10.0.0.5/painel"));
        assertNull(RichTextHtml.normalizeUrl("javascript:alert(1)"));
        assertNull(RichTextHtml.normalizeUrl("apenas texto"));
        assertNull(RichTextHtml.normalizeUrl(""));
    }
}
