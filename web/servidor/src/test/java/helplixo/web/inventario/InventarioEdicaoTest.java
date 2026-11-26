package helplixo.web.inventario;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import helplixo.web.edicao.EdicaoController;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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

/** Edição do inventário. Senha inventada só para o teste; cada teste começa com os dados limpos. */
@SpringBootTest(properties = "helplixo.edicao.senha=senha-do-teste-unitario")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventarioEdicaoTest {

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

    @Autowired
    ObjectMapper json;

    String chave;

    @BeforeEach
    void entrarNoModoEdicao() throws Exception {
        String corpo = mvc.perform(post("/api/edicao/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"senha-do-teste-unitario\"}"))
                .andReturn().getResponse().getContentAsString();
        chave = json.readTree(corpo).get("chave").asText();
    }

    private MockHttpServletRequestBuilder comEdicao(MockHttpServletRequestBuilder r) {
        return r.header(EdicaoController.CABECALHO, chave).header(InventarioController.CABECALHO_USUARIO, "Magno");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder r, Object corpo) throws Exception {
        return mvc.perform(r.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }

    private static Map<String, Object> novaMaquina() {
        Map<String, Object> m = new HashMap<>();
        m.put("nSemin", "ic52");
        m.put("patrimonio", "pms-01400");
        m.put("tipo", "Computador");
        m.put("modelo", "Dell OptiPlex 7010");
        m.put("memoria", "16 GB DDR4");
        m.put("ip", "10.0.13.52");
        m.put("local", "Informática - SMED");
        m.put("usuario", "Diego");
        m.put("compra", Map.of("fornecedor", "Dell", "data", "2026-10-01", "garantiaAte", "2029-10-01"));
        return m;
    }

    /** Os dados de hoje da IC21, para editar só o que o teste muda. */
    private Map<String, Object> dadosDaIC21() {
        Map<String, Object> m = new HashMap<>();
        m.put("tipo", "Computador");
        m.put("modelo", "Dell OptiPlex 7090");
        m.put("serie", "7HXK2Q3");
        m.put("processador", "Intel Core i5-10505 3.2 GHz");
        m.put("memoria", "16 GB DDR4");
        m.put("armazenamento", "SSD 512 GB");
        m.put("sistema", "Windows 11 Pro 64 bits");
        m.put("ip", "10.0.13.21");
        m.put("mac", "B0-7B-25-1A-4C-21");
        m.put("local", "Informática - SMED");
        m.put("usuario", "Magno");
        m.put("observacao", "Máquina de exemplo (a do Magno).");
        m.put("compra", Map.of("fornecedor", "Dell Computadores do Brasil", "notaFiscal", "000.184.552", "data", "2023-03-14",
                "valor", "R$ 5.480,00", "processo", "Pregão 012/2023", "garantiaAte", "2026-03-14"));
        return m;
    }

    // ---------- adicionar ----------

    @Test
    void semModoEdicaoNaoAdiciona() throws Exception {
        enviar(post("/api/inventario"), novaMaquina())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erros[0]", containsString("modo edição")));
    }

    @Test
    void adicionarMaquinaComOModoEdicao() throws Exception {
        enviar(comEdicao(post("/api/inventario")), novaMaquina())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nSemin", is("IC52")))
                .andExpect(jsonPath("$.patrimonio", is("PMS-01400")))
                .andExpect(jsonPath("$.situacao", is("Em uso")));
        mvc.perform(get("/api/inventario/IC52"))
                .andExpect(jsonPath("$.historico[0].tipo", is("Cadastro")))
                .andExpect(jsonPath("$.historico[0].texto", is("Máquina cadastrada.")))
                .andExpect(jsonPath("$.historico[0].quem", is("Magno")));
    }

    @Test
    void adicionarConfereIdentidadeIpEDatas() throws Exception {
        Map<String, Object> m = novaMaquina();
        m.put("nSemin", "IC21");
        m.put("patrimonio", "PMS-00877");
        m.put("ip", "10.0.300.1");
        m.put("compra", Map.of("data", "2026-12-01", "garantiaAte", "2026-11-30"));
        enviar(comEdicao(post("/api/inventario")), m)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem("Já existe uma máquina com o Nº Semin IC21.")))
                .andExpect(jsonPath("$.erros", hasItem("O patrimônio PMS-00877 já está cadastrado (CC1261).")))
                .andExpect(jsonPath("$.erros", hasItem("Cada parte do IP vai de 0 a 255.")))
                .andExpect(jsonPath("$.erros", hasItem("A data da compra não pode estar no futuro.")))
                .andExpect(jsonPath("$.erros", hasItem("A garantia não pode terminar antes da data da compra.")));
        m.put("nSemin", "ABC1");
        enviar(comEdicao(post("/api/inventario")), m)
                .andExpect(jsonPath("$.erros", hasItem(startsWith("O Nº Semin deve ter 2 letras"))));
    }

    // ---------- editar ----------

    @Test
    void editarRegistraOQueMudouNoHistorico() throws Exception {
        Map<String, Object> d = dadosDaIC21();
        d.put("memoria", "32 GB DDR4");
        d.put("local", "Depósito da TI");
        enviar(comEdicao(put("/api/inventario/IC21")), d)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mudancas", hasSize(2)))
                .andExpect(jsonPath("$.mudancas", hasItem("RAM: 16 GB DDR4 → 32 GB DDR4")))
                .andExpect(jsonPath("$.maquina.situacao", is("Reserva")))
                .andExpect(jsonPath("$.maquina.patrimonio", is("PMS-01350")));
        mvc.perform(get("/api/inventario/IC21"))
                .andExpect(jsonPath("$.historico[0].tipo", is("Cadastro")))
                .andExpect(jsonPath("$.historico[0].texto", is("RAM: 16 GB DDR4 → 32 GB DDR4 · Local: Informática - SMED → Depósito da TI")));
    }

    @Test
    void nSeminEPatrimonioNaoMudam() throws Exception {
        Map<String, Object> d = dadosDaIC21();
        d.put("nSemin", "IC99");
        d.put("patrimonio", "PMS-99999");
        enviar(comEdicao(put("/api/inventario/IC21")), d)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(startsWith("O Nº Semin não muda"))))
                .andExpect(jsonPath("$.erros", hasItem(startsWith("O patrimônio não muda"))));
    }

    @Test
    void editarSemMudarNadaNaoGravaHistorico() throws Exception {
        enviar(comEdicao(put("/api/inventario/IC21")), dadosDaIC21())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mudancas", hasSize(0)));
        mvc.perform(get("/api/inventario/IC21")).andExpect(jsonPath("$.historico[0].tipo", is("Conserto")));
    }

    // ---------- movimentação ----------

    @Test
    void movimentarTrocaOLocalEAparaceNoHistorico() throws Exception {
        enviar(post("/api/inventario/EC775/movimentacoes"),
                Map.of("destino", "Tributação - SEFAZ", "motivo", "Substitui a CC1215", "quando", "2026-11-26T09:30", "tecnico", "Diego"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.local", is("Tributação - SEFAZ")))
                .andExpect(jsonPath("$.situacao", is("Em uso")));
        mvc.perform(get("/api/inventario/EC775"))
                .andExpect(jsonPath("$.historico[0].tipo", is("Movimentação")))
                .andExpect(jsonPath("$.historico[0].texto", is("Depósito da TI → Tributação - SEFAZ: Substitui a CC1215")));
    }

    @Test
    void movimentacaoConfereDestinoEChamado() throws Exception {
        enviar(post("/api/inventario/EC775/movimentacoes"),
                Map.of("destino", "Depósito da TI", "motivo", "x", "quando", "2026-11-26T09:30", "tecnico", "Diego", "chamado", "999999"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem("O destino é o mesmo lugar de onde a máquina saiu.")))
                .andExpect(jsonPath("$.erros", hasItem("O chamado 999999 não existe.")));
    }

    // ---------- baixa ----------

    @Test
    void baixaPedeChamadosFechadosEExplicacaoNoOutro() throws Exception {
        enviar(comEdicao(post("/api/inventario/IM04/baixa")), Map.of("motivo", "Descarte"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros", hasItem(containsString("177456"))));
        enviar(comEdicao(post("/api/inventario/NB02/baixa")), Map.of("motivo", "Outro"))
                .andExpect(jsonPath("$.erros", hasItem("Explique o motivo da baixa.")));
    }

    @Test
    void baixadaContinuaNaBuscaNaoMovimentaEPodeDesfazer() throws Exception {
        enviar(comEdicao(post("/api/inventario/NB02/baixa")), Map.of("motivo", "Descarte", "observacao", "Bateria sem conserto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao", is("Baixada")))
                .andExpect(jsonPath("$.baixa.quem", is("Magno")));
        mvc.perform(get("/api/inventario").param("q", "nb02")).andExpect(jsonPath("$.maquinas[0].situacao", is("Baixada")));
        enviar(post("/api/inventario/NB02/movimentacoes"),
                Map.of("destino", "Gabinete - GABINETE", "motivo", "teste", "quando", "2026-11-26T09:30", "tecnico", "Diego"))
                .andExpect(jsonPath("$.erros", hasItem(containsString("foi baixada"))));
        mvc.perform(comEdicao(delete("/api/inventario/NB02/baixa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao", is("Reserva")));
        mvc.perform(get("/api/inventario/NB02"))
                .andExpect(jsonPath("$.historico[*].tipo", hasItem("Baixa")))
                .andExpect(jsonPath("$.historico[0].texto", is("Baixa desfeita.")));
    }

    @Test
    void desfazerBaixaSemModoEdicaoERecusado() throws Exception {
        mvc.perform(delete("/api/inventario/NB02/baixa")).andExpect(status().isForbidden());
    }
}
