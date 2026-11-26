package helplixo.web.contato;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

/** Contatos internos (ramais) e externos (fornecedores e contratos). Em memória por enquanto. */
@Repository
public class ContatoRepositorio {

    /** Pessoa ou sala com ramal. O ramal pode ser compartilhado por várias pessoas. */
    public record Interno(String nome, String setor, String andar, String ramal, String email) {
        /** "Tributação - SEFAZ" -> "SEFAZ". */
        public String secretaria() {
            int i = setor == null ? -1 : setor.lastIndexOf(" - ");
            return i < 0 ? setor : setor.substring(i + 3);
        }
    }

    /** Fornecedor ou prestador, com o contrato e o vencimento. */
    public record Externo(String categoria, String empresa, String servico, String contato, String telefone, String email,
                          String contrato, LocalDate vence) {}

    private record Arquivo(List<Interno> internos, List<Externo> externos) {}

    private final List<Interno> internos = new CopyOnWriteArrayList<>();
    private final List<Externo> externos = new CopyOnWriteArrayList<>();

    public ContatoRepositorio(ObjectMapper json, @Value("${helplixo.dados.contatos}") Resource arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            Arquivo a = json.readValue(entrada, Arquivo.class);
            if (a.internos() != null) internos.addAll(a.internos());
            if (a.externos() != null) externos.addAll(a.externos());
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui ler os dados de teste dos contatos: " + arquivo, e);
        }
    }

    public List<Interno> internos() {
        return List.copyOf(internos);
    }

    public List<Externo> externos() {
        return List.copyOf(externos);
    }

    public void adicionar(Interno c) {
        internos.add(c);
    }

    public void adicionar(Externo c) {
        externos.add(c);
    }
}
