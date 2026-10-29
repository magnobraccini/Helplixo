package br.com.magnobraccini.helplixo.controller;

import br.com.magnobraccini.helplixo.core.AppSection;
import br.com.magnobraccini.helplixo.core.ApplicationContext;
import br.com.magnobraccini.helplixo.core.ApplicationState;
import br.com.magnobraccini.helplixo.core.Constants;
import br.com.magnobraccini.helplixo.core.GearAction;
import br.com.magnobraccini.helplixo.core.Version;
import br.com.magnobraccini.helplixo.service.AutoClickerService;
import br.com.magnobraccini.helplixo.service.SearchService;
import br.com.magnobraccini.helplixo.view.dialogs.AutoClickerDialog;
import br.com.magnobraccini.helplixo.view.dialogs.ChangePasswordDialog;
import br.com.magnobraccini.helplixo.view.dialogs.EditPasswordDialog;
import br.com.magnobraccini.helplixo.view.main.MainFrame;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public final class MainController {
    private static final int EXTERNAL_CHANGE_POLL_MS = 6000;

    private final ApplicationContext context;
    private final ApplicationState state;
    private final MainFrame frame;
    private final DashboardController dashboardController;
    private final ProcessController processController;
    private final PhoneContactController phoneContactController;
    private final NoticeController noticeController;
    private final ToolController toolController;
    private Timer externalChangeTimer;

    public MainController(ApplicationContext context, MainFrame frame) {
        this.context = context;
        this.state = context.getApplicationState();
        this.frame = frame;
        this.dashboardController = new DashboardController(context.getDashboardService(), frame.getDashboardPanel());
        this.processController = new ProcessController(context.getFolderService(), context.getProcessService(),
                context.getRecentService(), state, frame.getProcessPanel(), frame,
                context.getLogService(), context.getWorkspaceWatcherService(), context.getUserPreferencesService());
        this.phoneContactController = new PhoneContactController(context.getPhoneContactService(),
                frame.getPhoneContactPanel(), frame, context.getWorkspaceWatcherService(), context.getUserPreferencesService());
        this.noticeController = new NoticeController(context.getNoticeService(),
                frame.getNoticePanel(), frame, context.getWorkspaceWatcherService(), context.getUserPreferencesService());
        this.toolController = new ToolController(context.getToolService(),
                frame.getToolPanel(), frame, context.getWorkspaceWatcherService(), context.getUserPreferencesService());
        bind();
    }

    public void initialize() {
        dashboardController.refresh();
        processController.refreshTree();
        phoneContactController.refresh();
        noticeController.refresh();
        toolController.refresh();
        context.getWorkspaceWatcherService().acknowledge();
        navigate(AppSection.DASHBOARD);
        frame.getProcessPanel().setEditMode(false);
        startExternalChangeWatcher();
    }

    private void startExternalChangeWatcher() {
        externalChangeTimer = new Timer(EXTERNAL_CHANGE_POLL_MS, event -> {
            if (context.getWorkspaceWatcherService().hasExternalChanges()) {
                frame.getNotificationBanner().show(
                        "Alguém atualizou dados do Helplixo. As informações na tela podem estar desatualizadas.",
                        "Atualizar agora", this::refreshAfterExternalChange);
            }
        });
        externalChangeTimer.setRepeats(true);
        externalChangeTimer.start();
    }

    private void refreshAfterExternalChange() {
        dashboardController.refresh();
        processController.refreshTree();
        phoneContactController.refresh();
        noticeController.refresh();
        toolController.refresh();
        UUID selectedProcessId = frame.getProcessPanel().getSelectedProcessId();
        if (selectedProcessId != null && !frame.getProcessPanel().hasUnsavedChanges()) {
            context.getProcessService().findById(selectedProcessId)
                    .ifPresent(processController::showProcess);
        }
        context.getWorkspaceWatcherService().acknowledge();
    }

    private void bind() {
        frame.initializeNavigation(this::navigate);
        frame.getTopBar().onEditMode(event -> toggleEditMode());
        frame.getTopBar().onLiveSearch(this::performLiveSearch);
        frame.getTopBar().onSearchResultSelected(this::onSearchResultSelected);
        frame.getTopBar().onGearAction(this::handleGearAction);
        frame.getDashboardPanel().onOpenRecent(process -> {
            navigate(AppSection.PROCESSES);
            processController.showProcess(process);
            frame.getProcessPanel().selectProcess(process.getId());
        });
        frame.getDashboardPanel().onOpenFavoriteProcess(process -> {
            navigate(AppSection.PROCESSES);
            processController.showProcess(process);
            frame.getProcessPanel().selectProcess(process.getId());
        });
        frame.getDashboardPanel().onOpenFavoriteFolder(folder -> {
            navigate(AppSection.PROCESSES);
            processController.showFolder(folder);
            frame.getProcessPanel().selectFolder(folder.getId());
        });
        frame.getDashboardPanel().onOpenFavoritePhoneContact(contact -> {
            navigate(AppSection.PHONES);
            phoneContactController.selectContact(contact.getId());
        });
        frame.getDashboardPanel().onOpenFavoriteNotice(notice -> {
            navigate(AppSection.NOTICES);
            noticeController.selectNotice(notice.getId());
        });
        frame.getDashboardPanel().onOpenFavoriteTool(tool -> {
            navigate(AppSection.TOOLS);
            toolController.selectTool(tool.getId());
        });
        frame.getDashboardPanel().onOpenNotice(notice -> {
            navigate(AppSection.NOTICES);
            noticeController.selectNotice(notice.getId());
        });
        frame.getSettingsPanel().onChangePassword(this::runChangePassword);
        frame.getSettingsPanel().onBackupNow(this::runBackup);
        frame.getSettingsPanel().onOpenDataFolder(this::openDataFolder);
        frame.getSettingsPanel().onExport(this::exportData);
        frame.getSettingsPanel().onImport(this::runRestoreBackup);
        frame.getToolPanel().onAutoClicker(this::openAutoClickerControls);
        state.addPropertyChangeListener(event -> {
            if ("editMode".equals(event.getPropertyName())) {
                boolean editMode = state.isEditMode();
                frame.getTopBar().setEditMode(editMode);
                frame.getStatusBar().setEditMode(editMode);
                frame.getProcessPanel().setEditMode(editMode);
                logEditModeChange(editMode);
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                shutdown();
            }
        });
    }

    private void navigate(AppSection section) {
        state.setCurrentSection(section);
        frame.showSection(section);
        frame.getStatusBar().setMessage(section.getDisplayName());
        if (section == AppSection.DASHBOARD) {
            dashboardController.refresh();
        } else if (section == AppSection.PROCESSES) {
            processController.refreshTree();
        } else if (section == AppSection.PHONES) {
            phoneContactController.refresh();
        } else if (section == AppSection.NOTICES) {
            noticeController.refresh();
        } else if (section == AppSection.TOOLS) {
            toolController.refresh();
        }
    }

    private void toggleEditMode() {
        if (state.isEditMode()) {
            if (frame.getProcessPanel().hasUnsavedChanges()) {
                int answer = JOptionPane.showConfirmDialog(frame,
                        "Existem alterações não salvas. Desativar o modo de edição e descartá-las?",
                        "Desativar edição", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            state.setEditMode(false);
            return;
        }
        if (!context.getSecurityService().requiresPassword()) {
            state.setEditMode(true);
            return;
        }
        char[] password = new EditPasswordDialog().request(frame);
        if (password == null) {
            return;
        }
        if (context.getSecurityService().validateEditPassword(password)) {
            state.setEditMode(true);
        } else {
            JOptionPane.showMessageDialog(frame, "Senha inválida.", "Helplixo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logEditModeChange(boolean editMode) {
        context.getLogService().logEditMode(editMode);
    }

    private void handleGearAction(GearAction action) {
        switch (action) {
            case SETTINGS -> navigate(AppSection.SETTINGS);
            case BACKUP -> runBackup();
            case CHANGE_PASSWORD -> runChangePassword();
            case AUTOCLICKER -> configureAutoClicker();
            case ABOUT -> showAbout();
            case EXIT -> shutdown();
        }
    }

    /**
     * Salva uma cópia (versão) de todos os dados na pasta que o usuário escolher.
     * Sem agendamento automático e sem limite de retenção: cada cópia fica guardada
     * onde o usuário decidiu salvá-la.
     */
    private void runBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar cópia dos dados");
        chooser.setSelectedFile(new java.io.File("helplixo_backup_"
                + java.time.LocalDate.now() + ".zip"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path backup = context.getBackupService().createBackup(0);
            Path destination = chooser.getSelectedFile().toPath();
            Files.copy(backup, destination, StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(frame, "Cópia dos dados salva com sucesso em:\n" + destination,
                    "Backup", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException | IOException exception) {
            JOptionPane.showMessageDialog(frame, "Não foi possível salvar a cópia dos dados.\n" + exception.getMessage(),
                    "Backup", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runRestoreBackup() {
        List<Path> backups = context.getBackupService().listBackups();
        if (backups.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Nenhum backup encontrado.", "Restaurar backup",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(context.getBackupService().getBackupsDirectory().toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("Backup do Helplixo (*.zip)", "zip"));
        chooser.setDialogTitle("Selecione o backup a restaurar");
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(frame,
                "Restaurar este backup substituirá os dados atuais (um backup de segurança será criado antes).\n"
                        + "Após restaurar, será necessário reiniciar o Helplixo. Continuar?",
                "Restaurar backup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            context.getBackupService().restoreBackup(chooser.getSelectedFile().toPath(),
                    context.getConfiguration().getBackupRetentionCount());
            JOptionPane.showMessageDialog(frame,
                    "Backup restaurado com sucesso. Feche e reabra o Helplixo para carregar os dados restaurados.",
                    "Restaurar backup", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(frame, "Não foi possível restaurar o backup.\n" + exception.getMessage(),
                    "Restaurar backup", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runChangePassword() {
        ChangePasswordDialog.Result result = new ChangePasswordDialog().request(frame);
        if (result == null) {
            return;
        }
        boolean changed = context.getSecurityService().changePassword(result.masterPassword(), result.newPassword());
        if (!changed) {
            JOptionPane.showMessageDialog(frame, "Senha mestre incorreta.", "Alterar senha",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        context.getConfigManager().save(context.getConfiguration());
        context.getLogService().logEdit("Senha de edição alterada (autorizado por senha mestre).");
        JOptionPane.showMessageDialog(frame, "Senha alterada com sucesso.", "Alterar senha",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(frame,
                Constants.APP_NAME + " — versão " + Version.CURRENT + "\nCriado por: Magno Braccini",
                "Sobre", JOptionPane.INFORMATION_MESSAGE);
    }

    private br.com.magnobraccini.helplixo.view.dialogs.AutoClickerControlDialog autoClickerDialog;
    private void openAutoClickerControls() {
        AutoClickerService autoClicker = context.getAutoClickerService();
        if (!autoClicker.isSupported()) {
            JOptionPane.showMessageDialog(frame,
                    "O repetidor de F6 não é suportado neste ambiente (sem acesso ao teclado do sistema).",
                    "Autoclicker", JOptionPane.WARNING_MESSAGE);
            return;
        }
        autoClicker.start();
        if (autoClicker.isActive()) {
            if (autoClickerDialog == null) {
                autoClickerDialog = new br.com.magnobraccini.helplixo.view.dialogs.AutoClickerControlDialog(
                        frame, autoClicker, () -> {
                            autoClickerDialog = null;
                            frame.getStatusBar().setMessage("Autoclicker desligado");
                            context.getLogService().logEdit("Repetidor de F6 desligado.");
                        }, this::refreshAutoClickerStatus);
                autoClickerDialog.setVisible(true);
                refreshAutoClickerStatus();
            } else {
                autoClickerDialog.refreshState();
                autoClickerDialog.toFront();
            }
        } else if (autoClickerDialog != null) {
            autoClickerDialog.dispose();
            autoClickerDialog = null;
        }
    }

    private void refreshAutoClickerStatus() {
        AutoClickerService autoClicker = context.getAutoClickerService();
        boolean running = autoClicker.isRunning();
        frame.getStatusBar().setMessage(running
                ? "Repetidor F6 rodando — " + autoClicker.getIntervalMillis() + "ms"
                : "Repetidor F6 pausado");
        context.getLogService().logEdit(running ? "Repetidor de F6 retomado." : "Repetidor de F6 pausado.");
        if (autoClickerDialog != null) {
            autoClickerDialog.refreshState();
        }
    }

    private void configureAutoClicker() {
        AutoClickerService autoClicker = context.getAutoClickerService();
        if (!autoClicker.isSupported()) {
            JOptionPane.showMessageDialog(frame,
                    "O repetidor de F6 não é suportado neste ambiente (sem acesso ao teclado do sistema).",
                    "Autoclicker", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AutoClickerDialog.Result result = new AutoClickerDialog()
                .request(frame, autoClicker.getIntervalMillis());
        if (result == null) {
            return;
        }
        autoClicker.setIntervalMillis(result.intervalMillis());
        JOptionPane.showMessageDialog(frame,
                "Configuração salva. O repetidor envia F6 até ser pausado ou desligado pelos controles.",
                "Autoclicker", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openDataFolder() {
        try {
            Desktop.getDesktop().open(context.getDataDirectory().toFile());
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(frame, "Não foi possível abrir a pasta de dados.\n" + exception.getMessage(),
                    "Local dos dados", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportData() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar dados do Helplixo");
        chooser.setSelectedFile(new java.io.File("helplixo_export.zip"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path backup = context.getBackupService().createBackup(context.getConfiguration().getBackupRetentionCount());
            Path destination = chooser.getSelectedFile().toPath();
            Files.copy(backup, destination, StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(frame, "Dados exportados com sucesso para:\n" + destination,
                    "Exportar dados", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException | IOException exception) {
            JOptionPane.showMessageDialog(frame, "Não foi possível exportar os dados.\n" + exception.getMessage(),
                    "Exportar dados", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performLiveSearch(String query) {
        state.setSearchQuery(query);
        List<SearchService.SearchResult> results = context.getSearchService().search(query);
        frame.getTopBar().showSearchResults(results);
    }

    private void onSearchResultSelected(Object rawResult) {
        if (rawResult instanceof SearchService.SearchResult result) {
            openResult(result);
        }
    }

    private void openResult(SearchService.SearchResult result) {
        if (result.process() != null) {
            navigate(AppSection.PROCESSES);
            processController.showProcess(result.process());
            frame.getProcessPanel().selectProcess(result.process().getId());
        } else if (result.folder() != null) {
            navigate(AppSection.PROCESSES);
            processController.showFolder(result.folder());
            frame.getProcessPanel().selectFolder(result.folder().getId());
        } else if (result.phoneContact() != null) {
            navigate(AppSection.PHONES);
            phoneContactController.selectContact(result.phoneContact().getId());
        } else if (result.notice() != null) {
            navigate(AppSection.NOTICES);
            noticeController.selectNotice(result.notice().getId());
        } else if (result.tool() != null) {
            navigate(AppSection.TOOLS);
            toolController.selectTool(result.tool().getId());
        }
    }

    private void shutdown() {
        if (frame.getProcessPanel().hasUnsavedChanges()) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    "Existem alterações não salvas. Deseja sair mesmo assim?",
                    "Fechar Helplixo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        context.getConfigManager().save(context.getConfiguration());
        context.getAutoSaveService().shutdown();
        context.getAutoClickerService().stop();
        if (externalChangeTimer != null) {
            externalChangeTimer.stop();
        }
        frame.dispose();
        System.exit(0);
    }
}
