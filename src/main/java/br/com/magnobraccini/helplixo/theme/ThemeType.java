package br.com.magnobraccini.helplixo.theme;

public enum ThemeType {
    CORPORATE_LIGHT("Corporativo Claro"),
    DARK("Escuro"),
    LIGHT("Claro");

    private final String displayName;

    ThemeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
