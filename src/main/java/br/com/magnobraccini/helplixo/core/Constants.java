package br.com.magnobraccini.helplixo.core;

public final class Constants {
    public static final String APP_NAME = "Helplixo";
    /**
     * O desenvolvedor precisa definir uma senha própria na variável de ambiente
     * HELPLIXO_EDIT_PASSWORD antes de executar a aplicação. Não inserir senhas no código.
     */
    public static final String DEFAULT_EDIT_PASSWORD = System.getenv().getOrDefault("HELPLIXO_EDIT_PASSWORD", "");
    /** O desenvolvedor também define sua senha mestre em HELPLIXO_MASTER_PASSWORD. */
    public static final String MASTER_PASSWORD = System.getenv().getOrDefault("HELPLIXO_MASTER_PASSWORD", "");
    public static final int MINIMUM_WIDTH = 1280;
    public static final int MINIMUM_HEIGHT = 720;

    private Constants() {
    }
}
