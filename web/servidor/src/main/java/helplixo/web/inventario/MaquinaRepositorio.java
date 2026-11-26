package helplixo.web.inventario;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

/** Guarda as máquinas. Por enquanto, os dados de teste em memória; depois, o banco de dados. */
@Repository
public class MaquinaRepositorio {

    private final List<Maquina> maquinas = new CopyOnWriteArrayList<>();

    public MaquinaRepositorio(ObjectMapper json, @Value("${helplixo.dados.maquinas}") Resource arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            maquinas.addAll(json.readValue(entrada, new TypeReference<List<Maquina>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui ler os dados de teste das máquinas: " + arquivo, e);
        }
    }

    public List<Maquina> todas() {
        return maquinas.stream().sorted(Comparator.comparing(Maquina::nSemin)).toList();
    }

    /** Busca pelo Nº Semin sem diferenciar maiúsculas (ic21 = IC21). */
    public Optional<Maquina> porNSemin(String nSemin) {
        return maquinas.stream().filter(m -> m.nSemin().equalsIgnoreCase(nSemin.trim())).findFirst();
    }

    public Optional<Maquina> porPatrimonio(String patrimonio) {
        return maquinas.stream().filter(m -> m.patrimonio() != null && m.patrimonio().equalsIgnoreCase(patrimonio.trim())).findFirst();
    }

    /** Guarda a máquina: troca a que tem o mesmo Nº Semin, ou acrescenta se for nova. */
    public synchronized void salvar(Maquina maquina) {
        boolean existia = maquinas.stream().anyMatch(m -> m.nSemin().equals(maquina.nSemin()));
        if (existia) maquinas.replaceAll(m -> m.nSemin().equals(maquina.nSemin()) ? maquina : m);
        else maquinas.add(maquina);
    }

    // movimentações e alterações de cadastro (no sistema real, tabelas do banco)
    private final List<Movimentacao> movimentacoes = new CopyOnWriteArrayList<>();
    private final List<RegistroCadastro> registros = new CopyOnWriteArrayList<>();

    public void registrar(Movimentacao m) {
        movimentacoes.add(m);
    }

    public void registrar(RegistroCadastro r) {
        registros.add(r);
    }

    public List<Movimentacao> movimentacoesDe(String nSemin) {
        return movimentacoes.stream().filter(m -> m.nSemin().equalsIgnoreCase(nSemin)).toList();
    }

    public List<RegistroCadastro> registrosDe(String nSemin) {
        return registros.stream().filter(r -> r.nSemin().equalsIgnoreCase(nSemin)).toList();
    }
}
