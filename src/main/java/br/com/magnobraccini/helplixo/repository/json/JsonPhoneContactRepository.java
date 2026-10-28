package br.com.magnobraccini.helplixo.repository.json;

import br.com.magnobraccini.helplixo.model.PhoneContact;
import br.com.magnobraccini.helplixo.repository.PhoneContactRepository;
import br.com.magnobraccini.helplixo.util.TextNormalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class JsonPhoneContactRepository extends AbstractJsonListRepository<PhoneContact, UUID>
        implements PhoneContactRepository {
    private static final TypeReference<List<PhoneContact>> CONTACT_LIST = new TypeReference<>() { };

    public JsonPhoneContactRepository(Path dataDirectory, ObjectMapper objectMapper) {
        super(dataDirectory, "phones.json", objectMapper, CONTACT_LIST);
    }

    @Override
    protected UUID idOf(PhoneContact entity) {
        return entity.getId();
    }

    @Override
    protected boolean isDeleted(PhoneContact entity) {
        return entity.isDeleted();
    }

    @Override
    protected void markDeleted(PhoneContact entity) {
        entity.setDeleted(true);
    }

    @Override
    protected List<PhoneContact> postProcess(List<PhoneContact> visible) {
        return visible.stream()
                .sorted(Comparator.comparing(PhoneContact::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<PhoneContact> search(String query, int limit) {
        String normalized = TextNormalizer.normalize(query);
        return findAll().stream()
                .filter(contact -> matches(contact, normalized))
                .limit(limit)
                .toList();
    }

    @Override
    public List<PhoneContact> findFavorites(int limit) {
        return findAll().stream().filter(PhoneContact::isFavorite).limit(limit).toList();
    }

    private boolean matches(PhoneContact contact, String normalizedQuery) {
        return TextNormalizer.normalize(contact.getName()).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(contact.getSector())).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(contact.getFloor())).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(contact.getPhone())).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(contact.getExtension())).contains(normalizedQuery)
                || TextNormalizer.normalize(nullToEmpty(contact.getEmail())).contains(normalizedQuery);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
