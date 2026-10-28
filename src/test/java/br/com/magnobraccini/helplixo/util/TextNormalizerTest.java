package br.com.magnobraccini.helplixo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextNormalizerTest {
    @Test
    void shouldIgnoreCaseAndAccents() {
        assertEquals("instalacao", TextNormalizer.normalize("INSTALAÇÃO"));
    }
}
