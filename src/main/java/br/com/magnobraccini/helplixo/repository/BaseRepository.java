package br.com.magnobraccini.helplixo.repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistência utilizado pelos repositories cujo
 * conjunto de operações se resume ao CRUD básico (listar, buscar, salvar,
 * excluir e contar). Repositories com necessidades adicionais podem manter
 * sua própria interface especializada sem estender esta.
 *
 * @param <T>  tipo da entidade
 * @param <ID> tipo do identificador da entidade
 */
public interface BaseRepository<T, ID> {
    List<T> findAll();

    Optional<T> findById(ID id);

    T save(T entity);

    void delete(ID id);

    long count();
}
