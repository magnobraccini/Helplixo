package helplixo.web.processo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProcessoControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void arvoreNaOrdemDoHelplixoSemPastaApagadaNemProcessoArquivado() throws Exception {
        mvc.perform(get("/api/processos/arvore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pastas[*].nome", contains("Rede", "Impressoras", "Sistemas", "Contas de usuário")))
                .andExpect(jsonPath("$.pastas[0].pastas[*].nome", contains("Pastas compartilhadas", "Wi-Fi")))
                .andExpect(jsonPath("$.pastas[0].total", is(1)))
                .andExpect(jsonPath("$.pastas[1].total", is(2)))
                .andExpect(jsonPath("$.pastas[1].pastas[0].processos[0].titulo", is("Configurar impressora de rede")))
                .andExpect(jsonPath("$.pastas[3].total", is(1))) // o processo arquivado não conta
                .andExpect(jsonPath("$.semPasta", hasSize(0)));
    }

    @Test
    void detalheTrazConteudoECaminhoDePastas() throws Exception {
        mvc.perform(get("/api/processos/20000000-0000-0000-0000-000000000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caminho", contains("Sistemas", "Sistema da Folha")))
                .andExpect(jsonPath("$.processo.titulo", is("Configurar SIGEM para abrir PDF e XLS")))
                .andExpect(jsonPath("$.processo.conteudo", hasSize(4)))
                .andExpect(jsonPath("$.processo.conteudo[0].tipo", is("HEADING")));
    }

    @Test
    void detalheComPassosNoFormatoAntigo() throws Exception {
        mvc.perform(get("/api/processos/20000000-0000-0000-0000-000000000004"))
                .andExpect(jsonPath("$.processo.passos", hasSize(2)))
                .andExpect(jsonPath("$.processo.passos[1].checklist[0]", is("Marcar Reconectar ao entrar")))
                .andExpect(jsonPath("$.caminho", contains("Rede", "Pastas compartilhadas")));
    }

    @Test
    void buscaIgnoraAcentoEProcuraNoConteudo() throws Exception {
        mvc.perform(get("/api/processos").param("q", "impressao"))
                .andExpect(jsonPath("$[*].titulo", contains("Limpar fila de impressão")));
        mvc.perform(get("/api/processos").param("q", "spooler"))
                .andExpect(jsonPath("$[0].caminho", is("Impressoras")));
    }

    @Test
    void buscaNaoAchaProcessoArquivado() throws Exception {
        mvc.perform(get("/api/processos").param("q", "windows xp")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void buscaComUmaLetraPedeMais() throws Exception {
        mvc.perform(get("/api/processos").param("q", "a"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros[0]", containsString("pelo menos 2 letras")));
    }

    @Test
    void processoQueNaoExisteDa404() throws Exception {
        mvc.perform(get("/api/processos/99999999-0000-0000-0000-000000000000")).andExpect(status().isNotFound());
    }
}
