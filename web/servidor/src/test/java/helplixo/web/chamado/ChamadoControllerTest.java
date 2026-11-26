package helplixo.web.chamado;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChamadoControllerTest {

    /** "Hoje" nos testes: 26/11/2026, 10h. */
    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioDeTeste() {
            ZoneId zona = ZoneId.systemDefault();
            return Clock.fixed(LocalDateTime.of(2026, 11, 26, 10, 0).atZone(zona).toInstant(), zona);
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void semDatasTrazOsUltimos30DiasDoMaisNovoParaOMaisAntigo() throws Exception {
        mvc.perform(get("/api/chamados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9))) // 177350 (16/10) e 177301 (11/09) ficam de fora
                .andExpect(jsonPath("$[0].numero", is("177457")))
                .andExpect(jsonPath("$[0].status", is("Aberto")))
                .andExpect(jsonPath("$[0].nSemin", is("CC1261")));
    }

    @Test
    void periodoMaiorTrazOsAntigos() throws Exception {
        mvc.perform(get("/api/chamados").param("de", "2026-08-01").param("ate", "2026-11-26"))
                .andExpect(jsonPath("$", hasSize(11)));
    }

    @Test
    void filtraPorStatusNoServidor() throws Exception {
        mvc.perform(get("/api/chamados").param("status", "Em andamento"))
                .andExpect(jsonPath("$[*].numero", contains("177452", "177448")))
                .andExpect(jsonPath("$[*].status", everyItem(is("Em andamento"))));
    }

    @Test
    void statusInvalidoExplicaOQueUsar() throws Exception {
        mvc.perform(get("/api/chamados").param("status", "Pendente"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodoInvertidoERecusado() throws Exception {
        mvc.perform(get("/api/chamados").param("de", "2026-11-30").param("ate", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detalheDeUmChamadoSemMaquinaENaoComputavel() throws Exception {
        mvc.perform(get("/api/chamados/177440"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nSemin", is("")))
                .andExpect(jsonPath("$.naoComputavel", is(true)))
                .andExpect(jsonPath("$.descricao", containsString("Júlia Campos")));
    }

    @Test
    void chamadoQueNaoExisteDa404() throws Exception {
        mvc.perform(get("/api/chamados/999999")).andExpect(status().isNotFound());
    }

    @Test
    void saudeRespondeOk() throws Exception {
        mvc.perform(get("/api/saude")).andExpect(jsonPath("$.status", is("ok")));
    }
}
