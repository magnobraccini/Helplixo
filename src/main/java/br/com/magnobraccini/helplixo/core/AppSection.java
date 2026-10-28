package br.com.magnobraccini.helplixo.core;

public enum AppSection {
    DASHBOARD("Dashboard"),
    PROCESSES("Processos"),
    PHONES("Ramais"),
    NOTICES("Avisos"),
    TOOLS("Ferramentas"),
    SETTINGS("Configurações");

    private final String displayName;

    AppSection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
