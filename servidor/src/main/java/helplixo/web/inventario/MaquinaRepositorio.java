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
}
