package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.config.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityServiceTest {
    @Test
    void shouldRejectMissingOrIncorrectPassword() {
        SecurityService service = new SecurityService(new Configuration());
        assertFalse(service.validateEditPassword(new char[0]));
        assertFalse(service.validateEditPassword("errada".toCharArray()));
    }

    @Test
    void tellsWhetherAnEditPasswordIsConfigured() {
        Configuration configuration = new Configuration();
        configuration.setEditPassword("");
        SecurityService service = new SecurityService(configuration);
        assertFalse(service.hasEditPassword());

        configuration.setEditPassword("   ");
        assertFalse(service.hasEditPassword());

        configuration.setEditPassword("segredo");
        assertTrue(service.hasEditPassword());
        assertTrue(service.validateEditPassword("segredo".toCharArray()));
        assertFalse(service.validateEditPassword("Segredo".toCharArray()));
    }
}
