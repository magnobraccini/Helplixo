package helplixo.web.calendario;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import helplixo.web.Cargos;
import helplixo.web.edicao.EdicaoController;
import helplixo.web.inventario.InventarioController;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Calendário. "Hoje" = quinta, 26/11/2026, 10h. Senha inventada só para o teste. */
@SpringBootTest(properties = "helplixo.edicao.senha=senha-do-teste-unitario")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CalendarioControllerTest {

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

    private ResultActions dia(String de, String ate, String tipos) throws Exception {
        MockHttpServletRequestBuilder r = get("/api/calendario").param("de", de).param("ate", ate);
        if (tipos != null) r.param("tipos", tipos);
        return mvc.perform(r);
    }

    private MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder r, String nome, String cargo) {
        return r.header(InventarioController.CABECALHO_USUARIO, nome).header(Cargos.CABECALHO, cargo);
    }

    private ResultActions marcar(MockHttpServletRequestBuilder r, Map<String, Object> corpo) throws Exception {
        return mvc.perform(r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }

    private String chave() throws Exception {
        String corpo = mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"senha-do-teste-unitario\"}"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("chave").asText();
    }

    private static Map<String, Object> aviso(String titulo) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", titulo);
        m.put("tipo", "Aviso");
        m.put("inicio", "2026-11-26");
        return m;
    }

    // ---------- consultar ----------

    @Test
    void hojeDiaInteiroPrimeiroDepoisPorHora() throws Exception {
        dia("2026-11-26", "2026-11-26", null)
                .andExpect(jsonPath("$[*].titulo", contains("Sistema de protocolo fora do ar", "Aniversário da Lúcia", "Troca do switch do 3º andar")))
                .andExpect(jsonPath("$[1].repeteTexto", is("Todo ano, em 26/11")))
                .andExpect(jsonPath("$[2].diaInteiro", is(false)));
    }

    @Test
    void repeticaoSemanalEPeriodo() throws Exception {
        dia("2026-11-30", "2026-12-06", "Reunião")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dia", is("2026-11-30")))
                .andExpect(jsonPath("$[0].repeteTexto", is("Toda semana (segunda-feira)")));
        dia("2026-12-10", "2026-12-10", "Ausência").andExpect(jsonPath("$[*].titulo", contains("Férias do Rafael")));
    }

    @Test
    void automaticasFeriadosContratosEGarantias() throws Exception {
        dia("2026-11-01", "2026-12-31", "Feriado,Contrato,Garantia")
                .andExpect(jsonPath("$[*].titulo", hasItem("Dia da Consciência Negra")))
                .andExpect(jsonPath("$[*].titulo", hasItem("Vence contrato SoftGov Sistemas")))
                .andExpect(jsonPath("$[*].titulo", hasItem("Fim da garantia SC135")))
                .andExpect(jsonPath("$[*].titulo", not(hasItem("Fim da garantia IC21")))) // venceu há mais de 30 dias
                .andExpect(jsonPath("$[0].automatica", is(true)));
    }

    @Test
    void consultaMuitoLongaERecusada() throws Exception {
        dia("2026-01-01", "2027-12-31", null).andExpect(status().isUnprocessableEntity());
    }

    // ---------- marcar ----------

    @Test
    void avisoQualquerCargoMarcaSemModoEdicao() throws Exception {
        marcar(como(post("/api/calendario"), "Lúcia", "Triagem"), aviso("Impressora do 2º andar em manutenção"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marcacao.autor", is("Lúcia")))
                .andExpect(jsonPath("$.marcacao.cargo", is("Triagem")));
        marcar(post("/api/calendario"), aviso("Sem autor"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem("Informe quem está marcando o aviso (nome e cargo).")));
    }

    @Test
    void outrasMarcacoesPedemModoEdicao() throws Exception {
        Map<String, Object> m = aviso("Atualizar o servidor de arquivos");
        m.put("tipo", "Manutenção");
        marcar(como(post("/api/calendario"), "Magno", "Técnico"), m).andExpect(status().isForbidden());
        marcar(como(post("/api/calendario"), "Magno", "Técnico").header(EdicaoController.CABECALHO, chave()), m)
                .andExpect(status().isCreated());
    }

    @Test
    void regrasDeHorarioERepeticao() throws Exception {
        Map<String, Object> m = aviso("Teste");
        m.put("horaInicio", "10:00");
        m.put("horaFim", "09:00");
        m.put("repete", "semanal");
        m.put("fim", "2026-11-28");
        m.put("repeteAte", "2026-11-01");
        marcar(como(post("/api/calendario"), "Lúcia", "Triagem"), m)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem("A hora de fim precisa ser depois da hora de início.")))
                .andExpect(jsonPath("$.erros", hasItem("Repetição vale só para marcação de um dia.")))
                .andExpect(jsonPath("$.erros", hasItem("A repetição precisa terminar depois do primeiro dia.")));
    }

    @Test
    void foraDoExpedienteSoAvisaERepeticaoTermina() throws Exception {
        Map<String, Object> m = aviso("Plantão de fim de mês");
        m.put("horaInicio", "15:00");
        m.put("repete", "semanal");
        m.put("repeteAte", "2026-12-03");
        marcar(como(post("/api/calendario"), "Lúcia", "Triagem"), m)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avisos[0]", is("Fora do expediente (8h às 14h). É só um aviso.")));
        dia("2026-11-26", "2026-12-17", "Aviso")
                .andExpect(jsonPath("$[?(@.titulo == 'Plantão de fim de mês')].dia", contains("2026-11-26", "2026-12-03")));
    }

    // ---------- remover ----------

    @Test
    void removerSegueAHierarquia() throws Exception {
        mvc.perform(delete("/api/calendario/av1").header(Cargos.CABECALHO, "Triagem"))
                .andExpect(status().isUnprocessableEntity()); // aviso do chefe
        mvc.perform(delete("/api/calendario/av3").header(Cargos.CABECALHO, "Chefe de TI"))
                .andExpect(status().isNoContent()); // chefe remove o da triagem
        dia("2026-11-18", "2026-11-18", "Aviso").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void automaticaNaoSeRemove() throws Exception {
        mvc.perform(delete("/api/calendario/feriado-11-20").header(Cargos.CABECALHO, "Chefe de TI"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- lembretes ----------

    @Test
    void lembretesQueJaDevemAparecer() throws Exception {
        mvc.perform(get("/api/calendario/lembretes"))
                .andExpect(jsonPath("$[*].texto", contains(
                        "Lembrete: Sistema de protocolo fora do ar · hoje.",
                        "Lembrete: Troca do switch do 3º andar · hoje às 12:00.")))
                .andExpect(jsonPath("$[1].faltaMinutos", is(120)));
    }
}
