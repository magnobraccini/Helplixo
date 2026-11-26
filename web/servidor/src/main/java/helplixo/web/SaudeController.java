package helplixo.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/saude: responde se o servidor está no ar. */
@RestController
public class SaudeController {

    private final Clock relogio;

    public SaudeController(Clock relogio) {
        this.relogio = relogio;
    }

    @GetMapping("/api/saude")
    public Map<String, Object> saude() {
        return Map.of("status", "ok", "sistema", "Helplixo Web", "agora", LocalDateTime.now(relogio).toString());
    }

    /** Relógio único do sistema: nos testes ele pode ser fixado numa data. */
    @Configuration
    static class Relogio {
        @Bean
        Clock clock() {
            return Clock.systemDefaultZone();
        }
    }
}
