package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.config.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SecurityServiceTest {
    @Test
    void shouldRejectMissingOrIncorrectPassword() {
        SecurityService service = new SecurityService(new Configuration());
        assertFalse(service.validateEditPassword(new char[0]));
        assertFalse(service.validateEditPassword("errada".toCharArray()));
    }
}
