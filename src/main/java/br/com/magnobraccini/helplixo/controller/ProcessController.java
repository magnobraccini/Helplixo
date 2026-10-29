package br.com.magnobraccini.helplixo.controller;

import br.com.magnobraccini.helplixo.core.ApplicationState;
import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.service.FolderService;
import br.com.magnobraccini.helplixo.service.ProcessService;
import br.com.magnobraccini.helplixo.service.RecentService;
import br.com.magnobraccini.helplixo.service.UserPreferencesService;
import br.com.magnobraccini.helplixo.service.WorkspaceWatcherService;
import br.com.magnobraccini.helplixo.view.dialogs.NewFolderDialog;
import br.com.magnobraccini.helplixo.view.dialogs.MoveToFolderDialog;
import br.com.magnobraccini.helplixo.view.main.MainFrame;
import br.com.magnobraccini.helplixo.view.process.ProcessEditorData;
import br.com.magnobraccini.helplixo.view.process.ProcessPanel;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

public final class ProcessController {
    private final FolderService folderService;
    private final ProcessService processService;
    private final RecentService recentService;
    private final UserPreferencesService preferences;
    private final ApplicationState state;
    private final ProcessPanel view;
    private final MainFrame frame;
    private final LogService logService;
    private final WorkspaceWatcherService workspaceWatcherService;
    private UUID pendingNewProcessId;
    private LocalDateTime editingBaselineUpdatedAt;

    public ProcessController(FolderService folderService, ProcessService processService,
                             RecentService recentService, ApplicationState state,
                             ProcessPanel view, MainFrame frame, LogService logService,
                             WorkspaceWatcherService workspaceWatcherService) {
        this(folderService, processService, recentService, state, view, frame, logService, workspaceWatcherService, null);
    }

    public ProcessController(FolderService folderService, ProcessService processService,
                             RecentService recentService, ApplicationState state,
                             ProcessPanel view, MainFrame frame, LogService logService,
                             WorkspaceWatcherService workspaceWatcherService, UserPreferencesService preferences) {
        this.folderService = folderService;
        this.processService = processService;
        this.recentService = recentService;
        this.preferences = preferences;
        this.state = state;
        this.view = view;
        this.frame = frame;
        this.logService = logService;
        this.workspaceWatcherService = workspaceWatcherService;
        bind();
    }

    public ProcessController(FolderService folderService, ProcessService processService,
                             RecentService recentService, ApplicationState state,
                             ProcessPanel view, MainFrame frame, LogService logService) {
        this(folderService, processService, recentService, state, view, frame, logService, null);
    }

    public ProcessController(FolderService folderService, ProcessService processService,
                             RecentService recentService, ApplicationState state,
                             ProcessPanel view, MainFrame frame) {
        this(folderService, processService, recentService, state, view, frame, LogService.disabled(), null);
    }

    public void refreshTree() {
        List<Folder> folders = folderService.findAll();
        Map<UUID, List<ProcessDocument>> processes = new HashMap<>();
        folders.forEach(folder -> processes.put(folder.getId(), processService.findByFolderId(folder.getId())));
        if (preferences != null) {
            folders.forEach(f -> f.setFavorite(preferences.isFavorite("folder", f.getId())));
            processes.values().stream().flatMap(List::stream)
                    .forEach(p -> p.setFavorite(preferences.isFavorite("process", p.getId())));
        }
        view.renderTree(folders, processes, folders.stream().filter(Folder::isFavorite).toList(),
                processes.values().stream().flatMap(List::stream).filter(ProcessDocument::isFavorite).limit(50).toList(), recentService.findRecentProcesses(20));
        view.setEditMode(state.isEditMode());
        frame.getStatusBar().setMessage("Base atualizada");
        if (workspaceWatcherService != null) {
            workspaceWatcherService.acknowledge();
        }
    }

    public void showProcess(ProcessDocument process) {
        ProcessDocument current = processService.findById(process.getId()).orElse(process);
        if (preferences != null) current.setFavorite(preferences.isFavorite("process", current.getId()));
        state.setSelectedFolderId(current.getFolderId());
        state.setSelectedProcessId(current.getId());
        recentService.registerOpened(current.getId());
        view.renderProcess(current, breadcrumbFor(current));
        frame.getStatusBar().setMessage("Processo aberto: " + current.getTitle());
    }

    public void showFolder(Folder folder) {
        Folder current = folderService.findById(folder.getId()).orElse(folder);
        if (preferences != null) current.setFavorite(preferences.isFavorite("folder", current.getId()));
        state.setSelectedFolderId(current.getId());
        state.setSelectedProcessId(null);
        view.renderFolder(current, breadcrumbFor(current));
        frame.getStatusBar().setMessage("Pasta: " + current.getName());
    }

    private void bind() {
        view.onRefresh(this::refreshTree);
        view.onSelection(this::handleSelection);
        view.onNewFolder(this::createFolder);
        view.onNewProcess(this::createProcess);
        view.onEdit(this::editProcess);
        view.onRename(this::renameSelected);
        view.onMove(this::moveSelected);
        view.onDelete(this::deleteSelected);
        view.onFavorite(this::toggleFavorite);
        view.onSave(this::saveProcess);
        view.onCancel(this::cancelEditing);
    }

    private void handleSelection(Object selected) {
        if (selected instanceof Folder folder) {
            showFolder(folder);
        } else if (selected instanceof ProcessDocument process) {
            showProcess(process);
        }
    }

    private void createFolder() {
        NewFolderDialog.Result result = new NewFolderDialog()
                .request(frame, folderService.findAll(), view.getSelectedFolderId());
        if (result == null) {
            return;
        }
        try {
            Folder created = folderService.create(result.name(), "", result.parentId());
            state.setSelectedFolderId(created.getId());
            refreshTree();
            view.selectFolder(created.getId());
            showFolder(created);
            frame.getStatusBar().setMessage("Seção criada");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void createProcess() {
        UUID folderId = view.getSelectedFolderId();
        if (folderId == null) {
            JOptionPane.showMessageDialog(frame, "Selecione uma pasta primeiro.",
                    "Helplixo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String title = JOptionPane.showInputDialog(frame, "Título do processo:", "Novo processo",
                JOptionPane.PLAIN_MESSAGE);
        if (title == null) {
            return;
        }
        try {
            ProcessDocument created = processService.create(folderId, title, "");
            pendingNewProcessId = created.getId();
            editingBaselineUpdatedAt = created.getUpdatedAt();
            state.setSelectedProcessId(created.getId());
            refreshTree();
            view.selectProcess(created.getId());
            showProcess(created);
            view.beginEditing(created);
            frame.getStatusBar().setMessage("Novo processo criado — preencha os detalhes e clique em Salvar");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void editProcess() {
        UUID processId = view.getSelectedProcessId();
        if (processId == null) {
            return;
        }
        processService.findById(processId).ifPresent(process -> {
            editingBaselineUpdatedAt = process.getUpdatedAt();
            view.beginEditing(process);
        });
        frame.getStatusBar().setMessage("Editando processo");
    }

    private void saveProcess() {
        UUID processId = view.getSelectedProcessId();
        if (processId == null) {
            return;
        }
        try {
            ProcessDocument process = processService.findById(processId)
                    .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado."));
            boolean discardingPending = processId.equals(pendingNewProcessId);
            if (!discardingPending && !Objects.equals(process.getUpdatedAt(), editingBaselineUpdatedAt)) {
                int answer = JOptionPane.showConfirmDialog(frame,
                        "Este processo foi atualizado por outra pessoa enquanto você editava.\n\n"
                                + "Se continuar, suas alterações serão salvas por cima da versão mais recente.\n"
                                + "Deseja continuar mesmo assim?",
                        "Conflito de edição", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
                logService.logEdit("Conflito de edição resolvido por sobrescrita: " + process.getTitle()
                        + " (id=" + process.getId() + ")");
            }
            applyEditorData(process, view.readEditorData());
            ProcessDocument saved = processService.save(process);
            if (discardingPending) {
                pendingNewProcessId = null;
            }
            editingBaselineUpdatedAt = saved.getUpdatedAt();
            view.renderProcess(saved, breadcrumbFor(saved));
            refreshTree();
            view.selectProcess(saved.getId());
            frame.getStatusBar().setMessage("Processo salvo com sucesso");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void renameSelected() {
        Object selected = view.getSelectedObject();
        if (selected instanceof Folder folder) {
            renameFolder(folder);
        } else if (selected instanceof ProcessDocument process) {
            renameProcess(process);
        }
    }

    private void moveSelected() {
        Object selected = view.getSelectedObject();
        if (!(selected instanceof Folder) && !(selected instanceof ProcessDocument)) return;
        List<Folder> all = folderService.findAll();
        UUID currentFolderId = selected instanceof Folder folder ? folder.getParentId()
                : ((ProcessDocument) selected).getFolderId();
        List<MoveToFolderDialog.Destination> destinations = new ArrayList<>();
        if (selected instanceof Folder) destinations.add(new MoveToFolderDialog.Destination(null, "Processos (raiz)"));
        for (Folder candidate : all) {
            if (candidate.getId().equals(currentFolderId)) continue;
            if (selected instanceof Folder moving && (candidate.getId().equals(moving.getId())
                    || isFolderDescendant(candidate, moving.getId(), all))) continue;
            destinations.add(new MoveToFolderDialog.Destination(candidate.getId(), folderPath(candidate, all)));
        }
        MoveToFolderDialog.Destination target = new MoveToFolderDialog()
                .request(frame, selected instanceof Folder f ? f.getName() : ((ProcessDocument) selected).getTitle(),
                        destinations, currentFolderId);
        if (target == null) return;
        try {
            if (selected instanceof Folder folder) {
                Folder moved = folderService.move(folder.getId(), target.folderId());
                refreshTree(); view.selectFolder(moved.getId()); showFolder(moved);
            } else {
                ProcessDocument moved = processService.moveToFolder(((ProcessDocument) selected).getId(), target.folderId());
                refreshTree(); view.selectProcess(moved.getId()); showProcess(moved);
            }
            frame.getStatusBar().setMessage("Item movido para a seção escolhida");
        } catch (RuntimeException exception) { showError(exception); }
    }

    private boolean isFolderDescendant(Folder candidate, UUID ancestorId, List<Folder> all) {
        Map<UUID, Folder> byId = new HashMap<>(); all.forEach(folder -> byId.put(folder.getId(), folder));
        UUID parent = candidate.getParentId(); int guard = 0;
        while (parent != null && guard++ < 100) {
            if (parent.equals(ancestorId)) return true;
            Folder current = byId.get(parent); parent = current == null ? null : current.getParentId();
        }
        return false;
    }

    private String folderPath(Folder folder, List<Folder> all) {
        Map<UUID, Folder> byId = new HashMap<>(); all.forEach(item -> byId.put(item.getId(), item));
        List<String> parts = new ArrayList<>(); UUID id = folder.getId(); int guard = 0;
        while (id != null && guard++ < 100) {
            Folder current = byId.get(id); if (current == null) break;
            parts.add(0, current.getName()); id = current.getParentId();
        }
        return String.join(" / ", parts);
    }

    private void renameFolder(Folder folder) {
        String name = (String) JOptionPane.showInputDialog(frame, "Novo nome:", "Renomear seção",
                JOptionPane.PLAIN_MESSAGE, null, null, folder.getName());
        if (name == null) {
            return;
        }
        try {
            Folder renamed = folderService.rename(folder.getId(), name);
            refreshTree();
            view.selectFolder(renamed.getId());
            showFolder(renamed);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void renameProcess(ProcessDocument process) {
        String title = (String) JOptionPane.showInputDialog(frame, "Novo título:", "Renomear processo",
                JOptionPane.PLAIN_MESSAGE, null, null, process.getTitle());
        if (title == null) {
            return;
        }
        try {
            ProcessDocument current = processService.findById(process.getId()).orElse(process);
            current.setTitle(title);
            ProcessDocument saved = processService.save(current);
            refreshTree();
            view.selectProcess(saved.getId());
            showProcess(saved);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void deleteSelected() {
        Object selected = view.getSelectedObject();
        if (!(selected instanceof Folder) && !(selected instanceof ProcessDocument)) {
            return;
        }
        String name = selected instanceof Folder folder ? folder.getName() : ((ProcessDocument) selected).getTitle();
        int answer = JOptionPane.showConfirmDialog(frame,
                "Mover ‘" + name + "’ para a lixeira?", "Excluir",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (selected instanceof Folder folder) {
                folderService.delete(folder.getId());
            } else {
                UUID id = ((ProcessDocument) selected).getId();
                processService.delete(id);
                if (id.equals(pendingNewProcessId)) {
                    pendingNewProcessId = null;
                }
            }
            state.setSelectedProcessId(null);
            state.setSelectedFolderId(null);
            refreshTree();
            view.renderNavigationMessage("Processos", "Selecione um item para continuar.");
            frame.getStatusBar().setMessage("Item movido para a lixeira");
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void toggleFavorite() {
        Object selected = view.getSelectedObject();
        try {
            if (selected instanceof Folder folder) {
                Folder updated = folderService.findById(folder.getId()).orElse(folder);
                if (preferences != null) updated.setFavorite(preferences.toggleFavorite("folder", folder.getId()));
                else updated = folderService.toggleFavorite(folder.getId());
                refreshTree();
                view.selectFolder(updated.getId());
                showFolder(updated);
            } else if (selected instanceof ProcessDocument process) {
                ProcessDocument updated = processService.findById(process.getId()).orElse(process);
                if (preferences != null) updated.setFavorite(preferences.toggleFavorite("process", process.getId()));
                else updated = processService.toggleFavorite(process.getId());
                refreshTree();
                view.selectProcess(updated.getId());
                showProcess(updated);
            }
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void cancelEditing() {
        UUID processId = view.getSelectedProcessId();
        boolean discardingNewProcess = processId != null && processId.equals(pendingNewProcessId);

        if (discardingNewProcess) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    "Cancelar a criação deste processo? Ele não será salvo.",
                    "Cancelar criação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            processService.delete(processId);
            pendingNewProcessId = null;
            state.setSelectedProcessId(null);
            state.setSelectedFolderId(null);
            refreshTree();
            view.renderNavigationMessage("Processos", "Selecione um item para continuar.");
            frame.getStatusBar().setMessage("Criação de processo cancelada");
            return;
        }

        if (view.hasUnsavedChanges()) {
            int answer = JOptionPane.showConfirmDialog(frame, "Descartar alterações não salvas?",
                    "Cancelar edição", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if (processId != null) {
            processService.findById(processId).ifPresent(process ->
                    view.renderProcess(process, breadcrumbFor(process)));
        } else {
            view.cancelEditing();
        }
        frame.getStatusBar().setMessage("Edição cancelada");
    }

    private void applyEditorData(ProcessDocument process, ProcessEditorData data) {
        process.setTitle(clean(data.title()));
        process.setDescription(clean(data.description()));
        process.setObjective(clean(data.objective()));
        process.setPrerequisites(clean(data.prerequisites()));
        process.setResponsible(clean(data.responsible()));
        process.setCategory(clean(data.category()));
        process.setTags(splitCommaSeparated(data.tags()));
        process.setObservations(clean(data.observations()));
        process.setLinks(splitLines(data.links()));
        process.setStepsHtml(data.steps());
        process.setSteps(List.of());
        process.setContent(data.content());
    }

    private List<String> splitCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String value : raw.split(",")) {
            if (!value.trim().isBlank()) {
                values.add(value.trim());
            }
        }
        return List.copyOf(values);
    }

    private List<String> splitLines(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return raw.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private List<ProcessPanel.BreadcrumbSegment> breadcrumbFor(ProcessDocument process) {
        List<ProcessPanel.BreadcrumbSegment> segments = new ArrayList<>(breadcrumbForFolder(process.getFolderId()));
        segments.add(new ProcessPanel.BreadcrumbSegment(process.getTitle(), null));
        return segments;
    }

    private List<ProcessPanel.BreadcrumbSegment> breadcrumbFor(Folder folder) {
        return breadcrumbForFolder(folder.getId());
    }

    private List<ProcessPanel.BreadcrumbSegment> breadcrumbForFolder(UUID folderId) {
        Map<UUID, Folder> folders = new HashMap<>();
        folderService.findAll().forEach(folder -> folders.put(folder.getId(), folder));
        List<ProcessPanel.BreadcrumbSegment> chain = new ArrayList<>();
        UUID currentId = folderId;
        int guard = 0;
        while (currentId != null && guard++ < 100) {
            Folder folder = folders.get(currentId);
            if (folder == null) {
                break;
            }
            chain.add(0, new ProcessPanel.BreadcrumbSegment(folder.getName(), folder.getId()));
            currentId = folder.getParentId();
        }
        List<ProcessPanel.BreadcrumbSegment> result = new ArrayList<>();
        result.add(new ProcessPanel.BreadcrumbSegment("Processos", null));
        result.addAll(chain);
        return result;
    }

    private void showError(RuntimeException exception) {
        String message = exception.getMessage() == null ? "Não foi possível concluir a operação." : exception.getMessage();
        JOptionPane.showMessageDialog(frame, message, "Helplixo", JOptionPane.WARNING_MESSAGE);
        frame.getStatusBar().setMessage("Operação não concluída");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
