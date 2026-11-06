package br.com.magnobraccini.helplixo.view.process;

import javax.swing.UIManager;
import java.awt.Font;

/** Fonte de interface usada nos documentos (Segoe UI no Windows), para editor e leitura serem idênticos. */
final class UIFonts {
    private UIFonts() { }

    static String family() {
        Font font = UIManager.getFont("defaultFont");
        if (font == null) font = UIManager.getFont("Label.font");
        return font == null ? Font.SANS_SERIF : font.getFamily();
    }

    static Font body() { return new Font(family(), Font.PLAIN, 14); }
}
