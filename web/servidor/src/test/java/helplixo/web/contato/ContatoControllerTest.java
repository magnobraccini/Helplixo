package helplixo.web.contato;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import helplixo.web.edicao.EdicaoController;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
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

/** Senha inventada só para o teste; cada teste começa com os dados limpos. */
@SpringBootTest(properties = "helplixo.edicao.senha=senha-do-teste-unitario")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContatoControllerTest {

    /** "Hoje" nos testes: 26/11/2026. */
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

    @Autowired
    ObjectMapper json;

    private String chave() throws Exception {
        String corpo = mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"senha-do-teste-unitario\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("chave").asText();
    }

    @Test
    void internosFiltradosPorSecretariaEOrdenadosPorRamal() throws Exception {
        mvc.perform(get("/api/contatos/internos").param("secretaria", "SMED").param("ordem", "ramal"))
                .andExpect(jsonPath("$.total", is(5)))
                .andExpect(jsonPath("$.deTotal", is(12)))
                .andExpect(jsonPath("$.contatos[0].ramal", is("2300")))
                .andExpect(jsonPath("$.opcoes.secretarias", contains("GABINETE", "SEAD", "SEFAZ", "SEMOB", "SMED")));
    }

    @Test
    void buscaSemAcento() throws Exception {
        mvc.perform(get("/api/contatos/internos").param("q", "lucia"))
                .andExpect(jsonPath("$.contatos[*].nome", contains("Lúcia Ramos")));
    }

    @Test
    void externosComSituacaoDoContratoEOrdemDeVencimento() throws Exception {
        mvc.perform(get("/api/contatos/externos"))
                .andExpect(jsonPath("$.contatos[0].contato.empresa", is("SoftGov Sistemas")))
                .andExpect(jsonPath("$.contatos[0].situacao", is("vencido")))
                .andExpect(jsonPath("$.contatos[0].texto", is("Venceu em 10/11/2026")))
                .andExpect(jsonPath("$.contatos[1].texto", is("Vence em 19 dias")));
        mvc.perform(get("/api/contatos/externos").param("categoria", "Internet e telefonia"))
                .andExpect(jsonPath("$.contatos", hasSize(2)));
        mvc.perform(get("/api/contatos/externos").param("situacao", "vencido"))
                .andExpect(jsonPath("$.contatos[*].contato.empresa", contains("SoftGov Sistemas")));
    }

    @Test
    void cadastrarRamalPedeModoEdicaoEAceitaRamalCompartilhado() throws Exception {
        Map<String, String> novo = Map.of("nome", "Recepção do Anexo", "setor", "Diretoria Obras - SEMOB", "andar", "Mezanino", "ramal", "2410");
        mvc.perform(post("/api/contatos/internos").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(novo)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/contatos/internos").header(EdicaoController.CABECALHO, chave())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(novo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compartilhadoCom", contains("Andreia Lopes")));
        mvc.perform(get("/api/contatos/internos"))
                .andExpect(jsonPath("$.total", is(13)))
                .andExpect(jsonPath("$.opcoes.andares", hasItem("Mezanino")));
    }

    @Test
    void ramalInvalidoERecusado() throws Exception {
        mvc.perform(post("/api/contatos/internos").header(EdicaoController.CABECALHO, chave())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\",\"setor\":\"Informática - SMED\",\"ramal\":\"23\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem("O ramal tem 4 números (ex.: 2315).")));
    }

    @Test
    void cadastrarExternoCriaACategoriaNoFiltro() throws Exception {
        Map<String, String> novo = Map.of("categoria", "Elétrica", "empresa", "Energia Total", "servico", "Manutenção de nobreaks",
                "telefone", "(11) 3999-0000", "vence", "2027-08-01");
        mvc.perform(post("/api/contatos/externos").header(EdicaoController.CABECALHO, chave())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(novo)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/contatos/externos")).andExpect(jsonPath("$.categorias", hasItem("Elétrica")));
    }

    @Test
    void externoSemVencimentoNemContatoERecusado() throws Exception {
        mvc.perform(post("/api/contatos/externos").header(EdicaoController.CABECALHO, chave())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"categoria\":\"X\",\"empresa\":\"Y\",\"servico\":\"Z\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem("Informe telefone ou e-mail.")))
                .andExpect(jsonPath("$.erros", hasItem("Informe o vencimento do contrato.")));
    }
}
