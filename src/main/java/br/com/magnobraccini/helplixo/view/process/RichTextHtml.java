package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.model.ProcessStep;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/** Sanitizes the editor's HTML down to formatting tags supported by the process viewer. */
final class RichTextHtml {
    private static final Set<String> ALLOWED = Set.of("p", "div", "b", "strong", "i", "em", "u", "s", "strike",
            "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "code",
            "font", "span", "a", "br", "hr");

    private RichTextHtml() { }

    static String sanitize(String html) {
        if (html == null || html.isBlank()) return "";
        StringWriter output = new StringWriter();
        try {
            new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
                private int suppressed;

                @Override public void handleStartTag(HTML.Tag tag, javax.swing.text.MutableAttributeSet attrs, int pos) {
                    String name = tag.toString().toLowerCase();
                    if (name.equals("head") || name.equals("style") || name.equals("script")) {
                        suppressed++;
                    } else if (suppressed == 0 && ALLOWED.contains(name)) {
                        output.append('<').append(name);
                        if (name.equals("font")) {
                            Object size = attrs.getAttribute(HTML.Attribute.SIZE);
                            if (size != null && size.toString().matches("[1-7]")) {
                                output.append(" size=\"").append(size.toString()).append('\"');
                            }
                            appendSafeAttribute(output, "face", attrs.getAttribute(HTML.Attribute.FACE), "[A-Za-z0-9 _,-]{1,64}");
                            appendSafeAttribute(output, "color", attrs.getAttribute(HTML.Attribute.COLOR), "#[0-9a-fA-F]{3,8}");
                        }
                        if (name.equals("a")) appendHref(output, attrs.getAttribute(HTML.Attribute.HREF));
                        if (name.equals("p") || name.equals("div")) appendAlignment(output, attrs.getAttribute(HTML.Attribute.ALIGN));
                        appendSafeStyle(output, attrs.getAttribute(HTML.Attribute.STYLE));
                        output.append('>');
                    }
                }

                @Override public void handleEndTag(HTML.Tag tag, int pos) {
                    String name = tag.toString().toLowerCase();
                    if (name.equals("head") || name.equals("style") || name.equals("script")) {
                        suppressed = Math.max(0, suppressed - 1);
                    } else if (suppressed == 0 && ALLOWED.contains(name) && !name.equals("br") && !name.equals("hr")) {
                        output.append("</").append(name).append('>');
                    }
                }

                @Override public void handleSimpleTag(HTML.Tag tag, javax.swing.text.MutableAttributeSet attrs, int pos) {
                    String name = tag.toString().toLowerCase();
                    if (suppressed == 0 && name.equals("br")) output.append("<br>");
                    else if (suppressed == 0 && name.equals("hr")) output.append("<hr>");
                }

                @Override public void handleText(char[] data, int pos) {
                    if (suppressed == 0) output.append(escape(new String(data)));
                }
            }, true);
        } catch (IOException ignored) {
            return "";
        }
        String sanitized = output.toString().trim();
        String textOnly = sanitized.replaceAll("(?s)<[^>]*>", "")
                .replace("&nbsp;", "").replace("&#160;", "").trim();
        return textOnly.isEmpty() && !sanitized.contains("<hr>") ? "" : sanitized;
    }

    static String fromLegacySteps(List<ProcessStep> steps) {
        if (steps == null || steps.isEmpty()) return "";
        StringBuilder html = new StringBuilder();
        for (ProcessStep step : steps) {
            html.append("<p><b>").append(escape(safe(step.getTitle()))).append("</b>");
            if (!safe(step.getDescription()).isBlank()) {
                html.append("<br>").append(escape(step.getDescription()).replace("\n", "<br>"));
            }
            html.append("</p>");
        }
        return html.toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void appendHref(StringWriter output, Object raw) {
        if (raw == null) return;
        String href = raw.toString().trim();
        if (href.matches("(?i)^https?://[^\\s<>\"']+$")) {
            output.append(" href=\"").append(escape(href)).append('\"');
        }
    }

    private static void appendAlignment(StringWriter output, Object raw) {
        if (raw == null) return;
        String alignment = raw.toString().toLowerCase();
        if (Set.of("left", "right", "center", "justify").contains(alignment)) {
            output.append(" align=\"").append(alignment).append('\"');
        }
    }

    private static void appendSafeAttribute(StringWriter output, String name, Object raw, String pattern) {
        if (raw == null) return;
        String value = raw.toString().trim();
        if (value.matches(pattern)) output.append(' ').append(name).append("=\"").append(escape(value)).append('\"');
    }

    /** Retain a tiny, value-validated inline CSS subset emitted by the visual formatting controls. */
    private static void appendSafeStyle(StringWriter output, Object raw) {
        if (raw == null) return;
        StringBuilder safe = new StringBuilder();
        for (String declaration : raw.toString().split(";")) {
            String[] pair = declaration.split(":", 2);
            if (pair.length != 2) continue;
            String property = pair[0].trim().toLowerCase();
            String value = pair[1].trim().toLowerCase();
            boolean allowed = switch (property) {
                case "font-size" -> value.matches("[0-9]{1,2}(px|pt)");
                case "font-weight" -> value.matches("normal|bold|[1-9]00");
                case "font-style" -> Set.of("normal", "italic").contains(value);
                case "text-align" -> Set.of("left", "right", "center", "justify").contains(value);
                case "color", "background-color" -> value.matches("#[0-9a-f]{3,8}");
                case "text-decoration" -> Set.of("none", "underline", "line-through").contains(value);
                case "margin-left" -> value.matches("[0-9]{1,3}(px|pt)");
                default -> false;
            };
            if (allowed) safe.append(property).append(':').append(value).append(';');
        }
        if (!safe.isEmpty()) output.append(" style=\"").append(safe).append('\"');
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
