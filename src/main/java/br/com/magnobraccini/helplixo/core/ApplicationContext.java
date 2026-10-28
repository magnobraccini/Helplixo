package br.com.magnobraccini.helplixo.core;

import br.com.magnobraccini.helplixo.config.ConfigManager;
import br.com.magnobraccini.helplixo.config.Configuration;
import br.com.magnobraccini.helplixo.event.EventBus;
import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.repository.FolderRepository;
import br.com.magnobraccini.helplixo.repository.NoticeRepository;
import br.com.magnobraccini.helplixo.repository.PhoneContactRepository;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;
import br.com.magnobraccini.helplixo.repository.RecentRepository;
import br.com.magnobraccini.helplixo.repository.ToolRepository;
import br.com.magnobraccini.helplixo.repository.json.JsonFolderRepository;
import br.com.magnobraccini.helplixo.repository.json.JsonMapperFactory;
import br.com.magnobraccini.helplixo.repository.json.JsonNoticeRepository;
import br.com.magnobraccini.helplixo.repository.json.JsonPhoneContactRepository;
import br.com.magnobraccini.helplixo.repository.json.JsonProcessRepository;
import br.com.magnobraccini.helplixo.repository.json.JsonRecentRepository;
import br.com.magnobraccini.helplixo.repository.json.JsonToolRepository;
import br.com.magnobraccini.helplixo.service.AutoClickerService;
import br.com.magnobraccini.helplixo.service.AutoSaveService;
import br.com.magnobraccini.helplixo.service.BackupService;
import br.com.magnobraccini.helplixo.service.DashboardService;
import br.com.magnobraccini.helplixo.service.FolderService;
import br.com.magnobraccini.helplixo.service.NoticeService;
import br.com.magnobraccini.helplixo.service.PhoneContactService;
import br.com.magnobraccini.helplixo.service.ProcessService;
import br.com.magnobraccini.helplixo.service.RecentService;
import br.com.magnobraccini.helplixo.service.SearchService;
import br.com.magnobraccini.helplixo.service.SecurityService;
import br.com.magnobraccini.helplixo.service.ToolService;
import br.com.magnobraccini.helplixo.service.WorkspaceWatcherService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public final class ApplicationContext {
    private final Path dataDirectory;
    private final Configuration configuration;
    private final ConfigManager configManager;
    private final ApplicationState applicationState;
    private final EventBus eventBus;
    private final FolderService folderService;
    private final ProcessService processService;
    private final PhoneContactService phoneContactService;
    private final NoticeService noticeService;
    private final ToolService toolService;
    private final RecentService recentService;
    private final DashboardService dashboardService;
    private final SearchService searchService;
    private final SecurityService securityService;
    private final LogService logService;
    private final BackupService backupService;
    private final AutoSaveService autoSaveService;
    private final AutoClickerService autoClickerService;
    private final WorkspaceWatcherService workspaceWatcherService;

    private ApplicationContext(Path dataDirectory, Configuration configuration, ConfigManager configManager,
                               ApplicationState applicationState, EventBus eventBus, FolderService folderService,
                               ProcessService processService, PhoneContactService phoneContactService,
                               NoticeService noticeService, ToolService toolService,
                               RecentService recentService,
                               DashboardService dashboardService, SearchService searchService,
                               SecurityService securityService, LogService logService,
                               BackupService backupService, AutoSaveService autoSaveService,
                               WorkspaceWatcherService workspaceWatcherService,
                               AutoClickerService autoClickerService) {
        this.dataDirectory = dataDirectory;
        this.configuration = configuration;
        this.configManager = configManager;
        this.applicationState = applicationState;
        this.eventBus = eventBus;
        this.folderService = folderService;
        this.processService = processService;
        this.phoneContactService = phoneContactService;
        this.noticeService = noticeService;
        this.toolService = toolService;
        this.recentService = recentService;
        this.dashboardService = dashboardService;
        this.searchService = searchService;
        this.securityService = securityService;
        this.logService = logService;
        this.backupService = backupService;
        this.autoSaveService = autoSaveService;
        this.workspaceWatcherService = workspaceWatcherService;
        this.autoClickerService = autoClickerService;
    }

    public static ApplicationContext bootstrap(Path dataDirectory) {
        new DataDirectoryInitializer().initialize(dataDirectory);
        ObjectMapper objectMapper = JsonMapperFactory.create();
        ConfigManager configManager = new ConfigManager(dataDirectory, objectMapper);
        Configuration configuration = configManager.load();
        EventBus eventBus = new EventBus();
        LogService logService = LogService.forDataDirectory(dataDirectory);
        FolderRepository folderRepository = new JsonFolderRepository(dataDirectory, objectMapper);
        ProcessRepository processRepository = new JsonProcessRepository(dataDirectory, objectMapper);
        RecentRepository recentRepository = new JsonRecentRepository(dataDirectory, objectMapper);
        PhoneContactRepository phoneContactRepository = new JsonPhoneContactRepository(dataDirectory, objectMapper);
        NoticeRepository noticeRepository = new JsonNoticeRepository(dataDirectory, objectMapper);
        ToolRepository toolRepository = new JsonToolRepository(dataDirectory, objectMapper);
        FolderService folderService = new FolderService(folderRepository, processRepository, eventBus, logService);
        ProcessService processService = new ProcessService(processRepository, eventBus, logService);
        PhoneContactService phoneContactService = new PhoneContactService(phoneContactRepository, logService);
        NoticeService noticeService = new NoticeService(noticeRepository, logService);
        ToolService toolService = new ToolService(toolRepository, logService);
        RecentService recentService = new RecentService(recentRepository, processRepository);
        DashboardService dashboardService = new DashboardService(folderService, processService, phoneContactService, noticeService, toolService, recentService);
        SearchService searchService = new SearchService(processRepository, folderRepository, phoneContactRepository, noticeRepository, toolRepository);
        SecurityService securityService = new SecurityService(configuration);
        BackupService backupService = new BackupService(dataDirectory, logService);
        AutoSaveService autoSaveService = new AutoSaveService();
        WorkspaceWatcherService workspaceWatcherService = new WorkspaceWatcherService(dataDirectory);
        AutoClickerService autoClickerService = new AutoClickerService();
        return new ApplicationContext(dataDirectory, configuration, configManager, new ApplicationState(),
                eventBus, folderService, processService, phoneContactService, noticeService, toolService,
                recentService, dashboardService,
                searchService, securityService, logService, backupService, autoSaveService,
                workspaceWatcherService, autoClickerService);
    }

    public Path getDataDirectory() { return dataDirectory; }
    public Configuration getConfiguration() { return configuration; }
    public ConfigManager getConfigManager() { return configManager; }
    public ApplicationState getApplicationState() { return applicationState; }
    public EventBus getEventBus() { return eventBus; }
    public FolderService getFolderService() { return folderService; }
    public ProcessService getProcessService() { return processService; }
    public PhoneContactService getPhoneContactService() { return phoneContactService; }
    public NoticeService getNoticeService() { return noticeService; }
    public ToolService getToolService() { return toolService; }
    public RecentService getRecentService() { return recentService; }
    public DashboardService getDashboardService() { return dashboardService; }
    public SearchService getSearchService() { return searchService; }
    public SecurityService getSecurityService() { return securityService; }
    public LogService getLogService() { return logService; }
    public BackupService getBackupService() { return backupService; }
    public AutoSaveService getAutoSaveService() { return autoSaveService; }
    public WorkspaceWatcherService getWorkspaceWatcherService() { return workspaceWatcherService; }
    public AutoClickerService getAutoClickerService() { return autoClickerService; }
}
