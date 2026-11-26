package helplixo.web.chamado;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** O formulário único da aba Resultado: status Aberto / Em andamento / Fechado. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // cada teste começa com os dados de teste limpos
class ChamadoResultadoTest {

    /** "Hoje" nos testes: quinta, 26/11/2026, 10h. */
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

    private ResultActions enviar(String numero, String status, String anotacao, String quando, String tecnico) throws Exception {
        String corpo = "{\"status\":" + texto(status) + ",\"anotacao\":" + texto(anotacao) + ",\"quando\":" + texto(quando) + ",\"tecnico\":" + texto(tecnico) + "}";
        return mvc.perform(post("/api/chamados/" + numero + "/resultado").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String texto(String v) {
        return v == null ? "null" : "\"" + v + "\"";
    }

    @Test
    void mudarSoOStatusSemAnotacaoRegistraOStatus() throws Exception {
        enviar("177457", "Em andamento", "", "2026-11-26T09:50", "Diego")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chamado.status", is("Em andamento")))
                .andExpect(jsonPath("$.chamado.andamento[-1].texto", is("Status: em andamento.")))
                .andExpect(jsonPath("$.avisos", hasSize(0)));
    }

    @Test
    void mesmoStatusSemAnotacaoNaoTemOQueSalvar() throws Exception {
        enviar("177457", "Aberto", "  ", "2026-11-26T09:50", "Diego")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(containsString("Escreva uma anotação"))));
    }

    @Test
    void paraFecharAAnotacaoEObrigatoria() throws Exception {
        enviar("177456", "Fechado", "ok", "2026-11-26T09:50", "Diego")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(startsWith("Para fechar"))));
    }

    @Test
    void fecharGuardaOResultado() throws Exception {
        enviar("177456", "Fechado", "Fusor trocado pela assistência; impressão normal.", "2026-11-26T09:55", "Diego")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chamado.status", is("Fechado")))
                .andExpect(jsonPath("$.chamado.fechamento.tecnico", is("Diego")))
                .andExpect(jsonPath("$.chamado.fechamento.descricao", containsString("Fusor trocado")));
    }

    @Test
    void reabrirPeloStatusGuardaOFechamentoAnteriorNoAndamento() throws Exception {
        enviar("177456", "Fechado", "Fusor trocado pela assistência; impressão normal.", "2026-11-26T09:55", "Diego").andExpect(status().isOk());
        enviar("177456", "Em andamento", "", "2026-11-26T09:58", "Magno")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chamado.status", is("Em andamento")))
                .andExpect(jsonPath("$.chamado.fechamento", nullValue()))
                .andExpect(jsonPath("$.chamado.andamento[0].texto", startsWith("Fechado antes por Diego: Fusor trocado")))
                .andExpect(jsonPath("$.chamado.andamento[1].texto", is("Reaberto")));
    }

    @Test
    void horarioNoFuturoEAntesDaAberturaSaoRecusados() throws Exception {
        enviar("177457", "Em andamento", "teste", "2026-11-26T11:00", "Diego")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(containsString("futuro"))));
        enviar("177457", "Em andamento", "teste", "2026-11-25T08:00", "Diego")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(containsString("depois da abertura do chamado (25/11/2026 às 09:12)"))));
    }

    @Test
    void foraDoExpedienteSoAvisa() throws Exception {
        enviar("177448", "Em andamento", "Operadora visitou o rack.", "2026-11-25T15:00", "Diego")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos[0]", startsWith("Fora do expediente")));
    }

    @Test
    void semTecnicoEStatusInvalido() throws Exception {
        enviar("177457", "Pendente", "teste", "2026-11-26T09:50", "")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(startsWith("Status inválido"))))
                .andExpect(jsonPath("$.erros", hasItem(is("Escolha o técnico."))));
    }

    @Test
    void chamadoQueNaoExisteDa404() throws Exception {
        enviar("999999", "Aberto", "x", "2026-11-26T09:50", "Diego").andExpect(status().isNotFound());
    }

    @Test
    void dataEmFormatoErradoExplicaOFormato() throws Exception {
        enviar("177457", "Aberto", "x", "26/11/2026 09:50", "Diego")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0]", containsString("2026-11-26T10:30")));
    }
}
