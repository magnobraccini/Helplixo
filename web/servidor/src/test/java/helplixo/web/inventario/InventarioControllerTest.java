package helplixo.web.inventario;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
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
class InventarioControllerTest {

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

    @Test
    void resumoContaAsMaquinasPorTipo() throws Exception {
        mvc.perform(get("/api/inventario/resumo"))
                .andExpect(jsonPath("$.total", is(14)))
                .andExpect(jsonPath("$.porTipo.Computador", is(8)))
                .andExpect(jsonPath("$.porTipo.Monitor", is(2)));
    }

    @Test
    void buscaPeloNSeminSemDiferenciarMaiusculas() throws Exception {
        mvc.perform(get("/api/inventario").param("q", "ic21"))
                .andExpect(jsonPath("$.maquinas[0].nSemin", is("IC21")))
                .andExpect(jsonPath("$.maquinas[0].office", is("sim")));
    }

    @Test
    void buscaPorSoftwareEPorUsuarioSemAcento() throws Exception {
        mvc.perform(get("/api/inventario").param("q", "windows"))
                .andExpect(jsonPath("$.total", is(9)))
                .andExpect(jsonPath("$.temMais", is(false)));
        mvc.perform(get("/api/inventario").param("q", "fabio"))
                .andExpect(jsonPath("$.maquinas[*].nSemin", contains("EC1290")));
    }

    @Test
    void buscaComUmaLetraPedeMais() throws Exception {
        mvc.perform(get("/api/inventario").param("q", "a"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros[0]", containsString("pelo menos 2 letras")));
    }

    @Test
    void officeNosCincoCasos() throws Exception {
        mvc.perform(get("/api/inventario/IC21")).andExpect(jsonPath("$.office.situacao", is("sim")));
        mvc.perform(get("/api/inventario/GC167")).andExpect(jsonPath("$.office.situacao", is("sim, sem licença")));
        mvc.perform(get("/api/inventario/CC1261"))
                .andExpect(jsonPath("$.office.situacao", is("não")))
                .andExpect(jsonPath("$.office.detalhe", containsString("LibreOffice")));
        mvc.perform(get("/api/inventario/IM04")).andExpect(jsonPath("$.office.situacao", is("não se aplica")));
        mvc.perform(get("/api/inventario/EC775")).andExpect(jsonPath("$.office.situacao", is("não")));
    }

    @Test
    void garantiaVencidaVencendoESemInformacao() throws Exception {
        mvc.perform(get("/api/inventario/IC21")).andExpect(jsonPath("$.garantia.texto", is("Vencida em 14/03/2026")));
        mvc.perform(get("/api/inventario/SC135"))
                .andExpect(jsonPath("$.garantia.situacao", is("vence")))
                .andExpect(jsonPath("$.garantia.texto", is("Vence em 24 dias (20/12/2026)")));
        mvc.perform(get("/api/inventario/NB02")).andExpect(jsonPath("$.garantia.situacao", is("sem informação")));
    }

    @Test
    void historicoJuntaChamadosEConsertosDoMaisNovoParaOMaisAntigo() throws Exception {
        mvc.perform(get("/api/inventario/im04"))
                .andExpect(jsonPath("$.maquina.nSemin", is("IM04")))
                .andExpect(jsonPath("$.historico", hasSize(3)))
                .andExpect(jsonPath("$.historico[*].referencia", contains("177456", "177449", null)))
                .andExpect(jsonPath("$.historico[2].tipo", is("Conserto")));
    }

    @Test
    void maquinaQueNaoExisteDa404() throws Exception {
        mvc.perform(get("/api/inventario/ZZ999")).andExpect(status().isNotFound());
    }
}
