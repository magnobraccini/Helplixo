package br.com.magnobraccini.helplixo.repository;

import br.com.magnobraccini.helplixo.model.PhoneContact;

import java.util.List;
import java.util.UUID;

public interface PhoneContactRepository extends BaseRepository<PhoneContact, UUID> {
    List<PhoneContact> search(String query, int limit);

    List<PhoneContact> findFavorites(int limit);
}
