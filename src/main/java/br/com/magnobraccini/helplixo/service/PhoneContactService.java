package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.model.PhoneContact;
import br.com.magnobraccini.helplixo.repository.PhoneContactRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PhoneContactService {
    private final PhoneContactRepository repository;
    private final LogService logService;

    public PhoneContactService(PhoneContactRepository repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    public PhoneContactService(PhoneContactRepository repository) {
        this(repository, LogService.disabled());
    }

    public List<PhoneContact> findAll() {
        return repository.findAll();
    }

    public List<PhoneContact> search(String query) {
        return (query == null || query.isBlank()) ? findAll() : repository.search(query, 200);
    }

    /** Busca textual combinada com filtros exatos de setor e andar. A lista final permanece em ordem alfabética por nome. */
    public List<PhoneContact> search(String query, String sector, String floor) {
        return search(query).stream()
                .filter(contact -> matchesFilter(sector, contact.getSector()))
                .filter(contact -> matchesFilter(floor, contact.getFloor()))
                .toList();
    }

    /** Setores distintos já cadastrados, em ordem alfabética, para popular o filtro. */
    public List<String> distinctSectors() {
        return findAll().stream()
                .map(PhoneContact::getSector)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** Andares distintos já cadastrados, em ordem alfabética, para popular o filtro. */
    public List<String> distinctFloors() {
        return findAll().stream()
                .map(PhoneContact::getFloor)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean matchesFilter(String filterValue, String contactValue) {
        if (filterValue == null || filterValue.isBlank()) {
            return true;
        }
        return filterValue.equalsIgnoreCase(contactValue == null ? "" : contactValue.trim());
    }

    public List<PhoneContact> findFavorites(int limit) {
        return repository.findFavorites(limit);
    }

    public long count() {
        return repository.count();
    }

    public long countFavorites() {
        return repository.findFavorites(Integer.MAX_VALUE).size();
    }

    public PhoneContact create(String name, String sector, String floor, String phone, String extension, String email) {
        validateName(name);
        PhoneContact contact = PhoneContact.create(name.trim(), safe(sector), safe(floor), safe(phone),
                safe(extension), safe(email));
        repository.save(contact);
        logService.logCreate("Ramal criado: " + contact.getName());
        return contact;
    }

    public PhoneContact update(UUID id, String name, String sector, String floor, String phone, String extension,
                               String email) {
        validateName(name);
        PhoneContact contact = requireContact(id);
        contact.setName(name.trim());
        contact.setSector(safe(sector));
        contact.setFloor(safe(floor));
        contact.setPhone(safe(phone));
        contact.setExtension(safe(extension));
        contact.setEmail(safe(email));
        contact.setUpdatedAt(LocalDateTime.now());
        PhoneContact saved = repository.save(contact);
        logService.logEdit("Ramal editado: " + saved.getName());
        return saved;
    }

    public PhoneContact toggleFavorite(UUID id) {
        PhoneContact contact = requireContact(id);
        contact.setFavorite(!contact.isFavorite());
        contact.setUpdatedAt(LocalDateTime.now());
        return repository.save(contact);
    }

    public void delete(UUID id) {
        PhoneContact contact = requireContact(id);
        repository.delete(id);
        logService.logDelete("Ramal excluído: " + contact.getName());
    }

    private PhoneContact requireContact(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ramal não encontrado."));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
