package br.com.magnobraccini.helplixo.controller;

import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.model.NoticeCategory;
import br.com.magnobraccini.helplixo.service.NoticeService;
import br.com.magnobraccini.helplixo.service.WorkspaceWatcherService;
import br.com.magnobraccini.helplixo.view.dialogs.NoticeDialog;
import br.com.magnobraccini.helplixo.view.notices.NoticePanel;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.UUID;

public final class NoticeController {
    private final NoticeService service;
    private final NoticePanel view;
    private final Component dialogOwner;
    private final WorkspaceWatcherService workspaceWatcherService;
    private String currentQuery = "";
    private NoticeCategory currentCategory;

    public NoticeController(NoticeService service, NoticePanel view, Component dialogOwner,
                            WorkspaceWatcherService workspaceWatcherService) {
        this.service = service;
        this.view = view;
        this.dialogOwner = dialogOwner;
        this.workspaceWatcherService = workspaceWatcherService;
        bind();
    }

    public NoticeController(NoticeService service, NoticePanel view, Component dialogOwner) {
        this(service, view, dialogOwner, null);
    }

    private void bind() {
        view.onSearch(this::search);
        view.onCategoryFilter(this::filterByCategory);
        view.onNew(this::createNotice);
        view.onEdit(this::editNotice);
        view.onDelete(this::deleteNotice);
        view.onFavorite(this::toggleFavorite);
    }

    public void refresh() {
        view.render(service.search(currentQuery, currentCategory));
        if (workspaceWatcherService != null) {
            workspaceWatcherService.acknowledge();
        }
    }

    private void search(String query) {
        currentQuery = query == null ? "" : query;
        refresh();
    }

    private void filterByCategory(NoticeCategory category) {
        currentCategory = category;
        refresh();
    }

    private void createNotice() {
        NoticeDialog.Result result = new NoticeDialog().request(dialogOwner, null);
        if (result == null) {
            return;
        }
        try {
            Notice created = service.create(result.title(), result.message(), result.category(), result.createdBy());
            refresh();
            view.selectNotice(created.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void editNotice(Notice notice) {
        if (notice == null) {
            return;
        }
        NoticeDialog.Result result = new NoticeDialog().request(dialogOwner, notice);
        if (result == null) {
            return;
        }
        try {
            Notice updated = service.update(notice.getId(), result.title(), result.message(), result.category(), result.createdBy());
            refresh();
            view.selectNotice(updated.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void deleteNotice(Notice notice) {
        if (notice == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(dialogOwner,
                "Excluir o aviso \"" + notice.getTitle() + "\"?", "Excluir aviso",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.delete(notice.getId());
            refresh();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void toggleFavorite(Notice notice) {
        if (notice == null) {
            return;
        }
        try {
            Notice updated = service.toggleFavorite(notice.getId());
            refresh();
            view.selectNotice(updated.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    public void selectNotice(UUID id) {
        view.selectNotice(id);
    }

    private void showError(RuntimeException exception) {
        JOptionPane.showMessageDialog(dialogOwner, exception.getMessage(), "Helplixo", JOptionPane.ERROR_MESSAGE);
    }
}
