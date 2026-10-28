package br.com.magnobraccini.helplixo.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

public final class ThemeManager {
    public void install(ThemeType theme) {
        try {
            switch (theme) {
                case DARK -> FlatDarkLaf.setup();
                case LIGHT -> FlatLightLaf.setup();
                case CORPORATE_LIGHT -> FlatIntelliJLaf.setup();
            }
            applyCorporateDefaults(theme);
        } catch (RuntimeException exception) {
            FlatLightLaf.setup();
            applyCorporateDefaults(ThemeType.LIGHT);
        }
    }

    private void applyCorporateDefaults(ThemeType theme) {
        UIManager.put("Component.arc", 7);
        UIManager.put("Button.arc", 7);
        UIManager.put("TextComponent.arc", 7);
        UIManager.put("ScrollBar.thumbArc", 7);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Tree.rowHeight", 30);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("TitlePane.unifiedBackground", false);
        UIManager.put("Button.default.boldText", true);
        UIManager.put("ToolTip.hideAccelerator", false);
        if (theme == ThemeType.DARK) {
            UIManager.put("Panel.background", new ColorUIResource(23, 31, 42));
            UIManager.put("ToolBar.background", new ColorUIResource(25, 35, 48));
            UIManager.put("Tree.background", new ColorUIResource(22, 31, 42));
            UIManager.put("TextField.background", new ColorUIResource(30, 42, 57));
            UIManager.put("TextArea.background", new ColorUIResource(30, 42, 57));
            UIManager.put("Button.default.background", new ColorUIResource(24, 103, 214));
            UIManager.put("Button.default.foreground", new ColorUIResource(255, 255, 255));
        }
    }
}
