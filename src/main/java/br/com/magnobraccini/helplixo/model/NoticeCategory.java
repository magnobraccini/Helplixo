package br.com.magnobraccini.helplixo.model;

public enum NoticeCategory {
    URGENTE("Urgente"),
    REDE("Rede"),
    INFRAESTRUTURA("Infraestrutura"),
    SISTEMA("Sistema"),
    MANUTENCAO("Manutenção");

    private final String displayName;

    NoticeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
