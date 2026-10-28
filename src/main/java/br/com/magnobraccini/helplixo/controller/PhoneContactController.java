package br.com.magnobraccini.helplixo.controller;

import br.com.magnobraccini.helplixo.model.PhoneContact;
import br.com.magnobraccini.helplixo.service.PhoneContactService;
import br.com.magnobraccini.helplixo.service.WorkspaceWatcherService;
import br.com.magnobraccini.helplixo.service.UserPreferencesService;
import br.com.magnobraccini.helplixo.view.dialogs.PhoneContactDialog;
import br.com.magnobraccini.helplixo.view.phones.PhoneContactPanel;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.UUID;

public final class PhoneContactController {
    private final PhoneContactService service;
    private final PhoneContactPanel view;
    private final Component dialogOwner;
    private final WorkspaceWatcherService workspaceWatcherService;
    private final UserPreferencesService preferences;
    private String currentQuery = "";
    private String currentSector;
    private String currentFloor;

    public PhoneContactController(PhoneContactService service, PhoneContactPanel view, Component dialogOwner,
                                  WorkspaceWatcherService workspaceWatcherService) {
        this(service, view, dialogOwner, workspaceWatcherService, null);
    }

    public PhoneContactController(PhoneContactService service, PhoneContactPanel view, Component dialogOwner,
                                  WorkspaceWatcherService workspaceWatcherService, UserPreferencesService preferences) {
        this.service = service;
        this.view = view;
        this.dialogOwner = dialogOwner;
        this.workspaceWatcherService = workspaceWatcherService;
        this.preferences = preferences;
        bind();
    }

    public PhoneContactController(PhoneContactService service, PhoneContactPanel view, Component dialogOwner) {
        this(service, view, dialogOwner, null);
    }

    private void bind() {
        view.onSearch(this::search);
        view.onSectorFilter(this::filterBySector);
        view.onFloorFilter(this::filterByFloor);
        view.onNew(this::createContact);
        view.onEdit(this::editContact);
        view.onDelete(this::deleteContact);
        view.onFavorite(this::toggleFavorite);
    }

    public void refresh() {
        var contacts = service.search(currentQuery, currentSector, currentFloor);
        if (preferences != null) contacts.forEach(c -> c.setFavorite(preferences.isFavorite("phone", c.getId())));
        view.render(contacts);
        view.updateFilterOptions(service.distinctSectors(), service.distinctFloors());
        if (workspaceWatcherService != null) {
            workspaceWatcherService.acknowledge();
        }
    }

    private void search(String query) {
        currentQuery = query == null ? "" : query;
        refresh();
    }

    private void filterBySector(String sector) {
        currentSector = sector;
        refresh();
    }

    private void filterByFloor(String floor) {
        currentFloor = floor;
        refresh();
    }

    private void createContact() {
        PhoneContactDialog.Result result = new PhoneContactDialog().request(dialogOwner, null);
        if (result == null) {
            return;
        }
        try {
            PhoneContact created = service.create(result.name(), result.sector(), result.floor(), result.phone(),
                    result.extension(), result.email());
            refresh();
            view.selectContact(created.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void editContact(PhoneContact contact) {
        if (contact == null) {
            return;
        }
        PhoneContactDialog.Result result = new PhoneContactDialog().request(dialogOwner, contact);
        if (result == null) {
            return;
        }
        try {
            PhoneContact updated = service.update(contact.getId(), result.name(), result.sector(), result.floor(),
                    result.phone(), result.extension(), result.email());
            refresh();
            view.selectContact(updated.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void deleteContact(PhoneContact contact) {
        if (contact == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(dialogOwner,
                "Excluir o ramal \"" + contact.getName() + "\"?", "Excluir ramal",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.delete(contact.getId());
            refresh();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void toggleFavorite(PhoneContact contact) {
        if (contact == null) {
            return;
        }
        try {
            PhoneContact updated = contact;
            if (preferences != null) updated.setFavorite(preferences.toggleFavorite("phone", contact.getId()));
            else updated = service.toggleFavorite(contact.getId());
            refresh();
            view.selectContact(updated.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    public void selectContact(UUID id) {
        view.selectContact(id);
    }

    private void showError(RuntimeException exception) {
        JOptionPane.showMessageDialog(dialogOwner, exception.getMessage(), "Helplixo", JOptionPane.ERROR_MESSAGE);
    }
}
