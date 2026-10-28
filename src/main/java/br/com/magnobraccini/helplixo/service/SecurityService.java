package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.config.Configuration;
import br.com.magnobraccini.helplixo.core.Constants;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class SecurityService {
    private final Configuration configuration;

    public SecurityService(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Por segurança, entrar em modo de edição sempre exige senha — não é mais configurável.
     * Independentemente do que um {@code configuration.json} antigo tenha salvo em
     * {@code requireEditPassword}, esse valor é ignorado aqui de propósito.
     */
    public boolean requiresPassword() {
        return true;
    }

    public boolean validateEditPassword(char[] password) {
        if (!requiresPassword()) {
            return true;
        }
        String configuredPassword = configuration.getEditPassword();
        if (configuredPassword == null || configuredPassword.isBlank()) {
            return false;
        }
        byte[] expected = configuredPassword.getBytes(StandardCharsets.UTF_8);
        byte[] provided = new String(password).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    public boolean validateMasterPassword(char[] password) {
        if (Constants.MASTER_PASSWORD.isBlank()) {
            return false;
        }
        byte[] expected = Constants.MASTER_PASSWORD.getBytes(StandardCharsets.UTF_8);
        byte[] provided = new String(password).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    /** Troca a senha de edição. Exige a senha mestre para autorizar a troca. */
    public boolean changePassword(char[] masterPassword, String newPassword) {
        if (!validateMasterPassword(masterPassword)) {
            return false;
        }
        configuration.setEditPassword(newPassword);
        return true;
    }
}
