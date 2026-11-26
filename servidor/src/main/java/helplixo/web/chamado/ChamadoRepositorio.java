package helplixo.web.chamado;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

/**
 * Guarda os chamados. Por enquanto lê os dados de teste em memória;
 * no sistema real, esta classe passa a conversar com o banco de dados
 * e o resto do código não precisa mudar.
 */
@Repository
public class ChamadoRepositorio {

    private final List<Chamado> chamados = new CopyOnWriteArrayList<>();

    public ChamadoRepositorio(ObjectMapper json, @Value("${helplixo.dados.chamados}") Resource arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            chamados.addAll(json.readValue(entrada, new TypeReference<List<Chamado>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui ler os dados de teste dos chamados: " + arquivo, e);
        }
    }

    /**
     * Chamados abertos dentro do período (de, ate), do mais novo para o mais antigo.
     * O filtro acontece aqui no servidor: a tela só recebe o período pedido.
     */
    public List<Chamado> buscar(LocalDateTime de, LocalDateTime ate, StatusChamado status) {
        return chamados.stream()
                .filter(c -> !c.abertoEm().isBefore(de) && c.abertoEm().isBefore(ate))
                .filter(c -> status == null || c.status() == status)
                .sorted(Comparator.comparing(Chamado::abertoEm).reversed())
                .toList();
    }

    public Optional<Chamado> porNumero(String numero) {
        return chamados.stream().filter(c -> c.numero().equals(numero)).findFirst();
    }
}
