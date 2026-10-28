package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.repository.BaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base reutilizável para repositories cujas entidades são persistidas em um
 * único arquivo JSON contendo uma lista (ex.: pastas, ramais, avisos,
 * ferramentas). Concentra a leitura/escrita do arquivo e as operações CRUD
 * padrão, eliminando a duplicação que existia entre os repositories JSON.
 * <p>
 * Subclasses definem apenas como extrair o identificador da entidade e,
 * opcionalmente, como tratar exclusão lógica (soft delete) e ordenação.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo do identificador da entidade
 */
public abstract class AbstractJsonListRepository<T, ID> implements BaseRepository<T, ID> {
    private final Path file;
    private final ObjectMapper objectMapper;
    private final TypeReference<List<T>> typeReference;

    protected AbstractJsonListRepository(Path dataDirectory, String fileName, ObjectMapper objectMapper,
                                          TypeReference<List<T>> typeReference) {
        this.file = dataDirectory.resolve(fileName);
        this.objectMapper = objectMapper;
        this.typeReference = typeReference;
    }

    /** Extrai o identificador único da entidade. */
    protected abstract ID idOf(T entity);

    /** Indica se a entidade deve ser tratada como excluída (soft delete). Padrão: nunca. */
    protected boolean isDeleted(T entity) {
        return false;
    }

    /** Marca a entidade como excluída. Subclasses com soft delete devem sobrescrever. */
    protected void markDeleted(T entity) {
        // sem efeito por padrão; subclasses com exclusão lógica sobrescrevem
    }

    /** Permite à subclasse filtrar/ordenar o resultado padrão de {@link #findAll()}. */
    protected List<T> postProcess(List<T> visible) {
        return visible;
    }

    @Override
    public synchronized List<T> findAll() {
        List<T> visible = readAll().stream().filter(entity -> !isDeleted(entity)).toList();
        return postProcess(visible);
    }

    @Override
    public synchronized Optional<T> findById(ID id) {
        return readAll().stream().filter(entity -> idOf(entity).equals(id)).findFirst();
    }

    @Override
    public synchronized T save(T entity) {
        List<T> all = readAll();
        all.removeIf(current -> idOf(current).equals(idOf(entity)));
        all.add(entity);
        write(all);
        return entity;
    }

    @Override
    public synchronized void delete(ID id) {
        List<T> all = readAll();
        all.stream().filter(entity -> idOf(entity).equals(id)).findFirst().ifPresent(this::markDeleted);
        write(all);
    }

    @Override
    public long count() {
        return findAll().size();
    }

    protected synchronized List<T> readAll() {
        try {
            if (Files.notExists(file)) {
                return new ArrayList<>();
            }
            return new ArrayList<>(objectMapper.readValue(file.toFile(), typeReference));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler " + file.getFileName() + ".", exception);
        }
    }

    protected synchronized void write(List<T> items) {
        try {
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), items);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível salvar " + file.getFileName() + ".", exception);
        }
    }
}
