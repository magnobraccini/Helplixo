package br.com.magnobraccini.helplixo.controller;

import br.com.magnobraccini.helplixo.model.Tool;
import br.com.magnobraccini.helplixo.service.ToolService;
import br.com.magnobraccini.helplixo.service.WorkspaceWatcherService;
import br.com.magnobraccini.helplixo.service.UserPreferencesService;
import br.com.magnobraccini.helplixo.util.ClipboardFiles;
import br.com.magnobraccini.helplixo.view.dialogs.ToolDialog;
import br.com.magnobraccini.helplixo.view.tools.ToolPanel;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;
import java.util.UUID;

public final class ToolController {
    private final ToolService service;
    private final ToolPanel view;
    private final Component dialogOwner;
    private final WorkspaceWatcherService workspaceWatcherService;
    private final UserPreferencesService preferences;
    private String currentQuery = "";

    public ToolController(ToolService service, ToolPanel view, Component dialogOwner,
                          WorkspaceWatcherService workspaceWatcherService) {
        this(service, view, dialogOwner, workspaceWatcherService, null);
    }

    public ToolController(ToolService service, ToolPanel view, Component dialogOwner,
                          WorkspaceWatcherService workspaceWatcherService, UserPreferencesService preferences) {
        this.service = service;
        this.view = view;
        this.dialogOwner = dialogOwner;
        this.workspaceWatcherService = workspaceWatcherService;
        this.preferences = preferences;
        bind();
    }

    public ToolController(ToolService service, ToolPanel view, Component dialogOwner) {
        this(service, view, dialogOwner, null);
    }

    private void bind() {
        view.onSearch(this::search);
        view.onNew(this::createTool);
        view.onEdit(this::editTool);
        view.onDelete(this::deleteTool);
        view.onFavorite(this::toggleFavorite);
        view.onExecute(this::executeTool);
        view.onCopyFile(this::copyToolFile);
    }

    public void refresh() {
        var tools = service.search(currentQuery);
        if (preferences != null) tools.forEach(t -> t.setFavorite(preferences.isFavorite("tool", t.getId())));
        view.render(tools);
        if (workspaceWatcherService != null) {
            workspaceWatcherService.acknowledge();
        }
    }

    private void search(String query) {
        currentQuery = query == null ? "" : query;
        refresh();
    }

    private void createTool() {
        ToolDialog.Result result = new ToolDialog().request(dialogOwner, null);
        if (result == null) {
            return;
        }
        try {
            Tool created = service.create(result.name(), result.description(), result.category(),
                    result.executablePath(), result.command(), result.icon());
            refresh();
            view.selectTool(created.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void editTool(Tool tool) {
        if (tool == null) {
            return;
        }
        ToolDialog.Result result = new ToolDialog().request(dialogOwner, tool);
        if (result == null) {
            return;
        }
        try {
            Tool updated = service.update(tool.getId(), result.name(), result.description(), result.category(),
                    result.executablePath(), result.command(), result.icon());
            refresh();
            view.selectTool(updated.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void deleteTool(Tool tool) {
        if (tool == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(dialogOwner,
                "Excluir a ferramenta \"" + tool.getName() + "\"?", "Excluir ferramenta",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.delete(tool.getId());
            refresh();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void toggleFavorite(Tool tool) {
        if (tool == null) {
            return;
        }
        try {
            Tool updated = tool;
            if (preferences != null) updated.setFavorite(preferences.toggleFavorite("tool", tool.getId()));
            else updated = service.toggleFavorite(tool.getId());
            refresh();
            view.selectTool(updated.getId());
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void executeTool(Tool tool) {
        if (tool == null) {
            return;
        }
        try {
            service.execute(tool);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    /**
     * Copia o arquivo anexado à ferramenta (o mesmo caminho que "Executar" roda) para a área de
     * transferência do Windows, como se fosse Ctrl+C no Explorer — permite colar (Ctrl+V) o
     * arquivo em outra pasta, ou dentro de uma sessão de área de trabalho remota.
     */
    private void copyToolFile(Tool tool) {
        if (tool == null) {
            return;
        }
        String path = tool.getExecutablePath();
        if (path == null || path.isBlank()) {
            JOptionPane.showMessageDialog(dialogOwner,
                    "Esta ferramenta não tem nenhum arquivo anexado.\nEdite a ferramenta e anexe um arquivo primeiro.",
                    "Copiar arquivo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = new File(path);
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(dialogOwner,
                    "O arquivo anexado não foi encontrado neste computador:\n" + path,
                    "Copiar arquivo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            ClipboardFiles.copyFile(file);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    public void selectTool(UUID id) {
        view.selectTool(id);
    }

    private void showError(RuntimeException exception) {
        JOptionPane.showMessageDialog(dialogOwner, exception.getMessage(), "Helplixo", JOptionPane.ERROR_MESSAGE);
    }
}
