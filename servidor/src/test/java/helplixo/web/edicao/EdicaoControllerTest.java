package helplixo.web.edicao;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** O caminho completo pela API. Senha inventada só para o teste (a de verdade vem da variável de ambiente). */
@SpringBootTest(properties = "helplixo.edicao.senha=senha-do-teste-unitario")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EdicaoControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String entrar(String senha) throws Exception {
        String corpo = mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("chave").asText();
    }

    @Test
    void entrarConferirESair() throws Exception {
        mvc.perform(get("/api/edicao")).andExpect(jsonPath("$.configurado", is(true))).andExpect(jsonPath("$.ligado", is(false)));
        String chave = entrar("senha-do-teste-unitario");
        mvc.perform(get("/api/edicao").header(EdicaoController.CABECALHO, chave))
                .andExpect(jsonPath("$.ligado", is(true)))
                .andExpect(jsonPath("$.validaAte", notNullValue()));
        mvc.perform(post("/api/edicao/sair").header(EdicaoController.CABECALHO, chave)).andExpect(status().isNoContent());
        mvc.perform(get("/api/edicao").header(EdicaoController.CABECALHO, chave)).andExpect(jsonPath("$.ligado", is(false)));
    }

    @Test
    void senhaErradaDa401ComAMensagemParaATela() throws Exception {
        mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erros[0]", startsWith("Senha incorreta")));
    }

    @Test
    void cincoErrosDao429() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"errada\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"errada\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
