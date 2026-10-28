package br.com.magnobraccini.helplixo.config;

import br.com.magnobraccini.helplixo.core.Constants;
import br.com.magnobraccini.helplixo.theme.ThemeType;

public class Configuration {
    private ThemeType theme = ThemeType.DARK;
    private String language = "pt-BR";
    /**
     * Mantido só para {@code configuration.json} antigos continuarem carregando sem erro
     * (Jackson falha em propriedade desconhecida). Não é mais lido em lugar nenhum: por
     * segurança, {@link br.com.magnobraccini.helplixo.service.SecurityService#requiresPassword()}
     * agora sempre exige senha, sem depender deste valor.
     */
    private boolean requireEditPassword = true;
    private String editPassword = Constants.DEFAULT_EDIT_PASSWORD;
    private int windowWidth = 1440;
    private int windowHeight = 900;
    private boolean maximized = true;
    private boolean autoSaveEnabled = true;
    private int autoSaveIntervalSeconds = 30;
    private boolean backupAutoEnabled = true;
    private int backupIntervalMinutes = 60;
    private int backupRetentionCount = 10;
    /**
     * Marca se os dados de exemplo (pastas/processos/ferramentas/avisos/ramais de demonstração)
     * já foram inseridos alguma vez. Existe para {@code DemoDataSeeder} não confundir "usuário
     * esvaziou os processos/ferramentas de propósito" com "instalação nova" — antes disso, a
     * checagem era só "tem alguma pasta cadastrada?", e zerar os dados fazia tudo voltar sozinho
     * no próximo início do programa (inclusive duplicando ramais de exemplo).
     */
    private boolean demoDataSeeded = false;

    public ThemeType getTheme() {
        return theme;
    }

    public void setTheme(ThemeType theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isRequireEditPassword() {
        return requireEditPassword;
    }

    public void setRequireEditPassword(boolean requireEditPassword) {
        this.requireEditPassword = requireEditPassword;
    }

    public String getEditPassword() {
        return editPassword;
    }

    public void setEditPassword(String editPassword) {
        this.editPassword = editPassword;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public void setMaximized(boolean maximized) {
        this.maximized = maximized;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void setAutoSaveEnabled(boolean autoSaveEnabled) {
        this.autoSaveEnabled = autoSaveEnabled;
    }

    public int getAutoSaveIntervalSeconds() {
        return autoSaveIntervalSeconds;
    }

    public void setAutoSaveIntervalSeconds(int autoSaveIntervalSeconds) {
        this.autoSaveIntervalSeconds = autoSaveIntervalSeconds;
    }

    public boolean isBackupAutoEnabled() {
        return backupAutoEnabled;
    }

    public void setBackupAutoEnabled(boolean backupAutoEnabled) {
        this.backupAutoEnabled = backupAutoEnabled;
    }

    public int getBackupIntervalMinutes() {
        return backupIntervalMinutes;
    }

    public void setBackupIntervalMinutes(int backupIntervalMinutes) {
        this.backupIntervalMinutes = backupIntervalMinutes;
    }

    public int getBackupRetentionCount() {
        return backupRetentionCount;
    }

    public void setBackupRetentionCount(int backupRetentionCount) {
        this.backupRetentionCount = backupRetentionCount;
    }

    public boolean isDemoDataSeeded() {
        return demoDataSeeded;
    }

    public void setDemoDataSeeded(boolean demoDataSeeded) {
        this.demoDataSeeded = demoDataSeeded;
    }
}
