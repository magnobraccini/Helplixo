package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.model.DashboardSummary;
import br.com.magnobraccini.helplixo.model.ProcessDocument;

import java.util.ArrayList;
import java.util.List;

public final class DashboardService {
    private final FolderService folderService;
    private final ProcessService processService;
    private final PhoneContactService phoneContactService;
    private final NoticeService noticeService;
    private final ToolService toolService;
    private final RecentService recentService;
    private final UserPreferencesService preferences;

    public DashboardService(FolderService folderService, ProcessService processService,
                             PhoneContactService phoneContactService, NoticeService noticeService,
                             ToolService toolService, RecentService recentService) {
        this(folderService, processService, phoneContactService, noticeService, toolService, recentService, null);
    }

    public DashboardService(FolderService folderService, ProcessService processService,
                             PhoneContactService phoneContactService, NoticeService noticeService,
                             ToolService toolService, RecentService recentService, UserPreferencesService preferences) {
        this.folderService = folderService;
        this.processService = processService;
        this.phoneContactService = phoneContactService;
        this.noticeService = noticeService;
        this.toolService = toolService;
        this.recentService = recentService;
        this.preferences = preferences;
    }

    public DashboardSummary loadSummary() {
        long favorites = loadFavorites().size();
        return new DashboardSummary(
                folderService.count(),
                processService.count(),
                favorites,
                recentService.findRecentProcesses(20).size());
    }

    public List<ProcessDocument> loadRecentProcesses() {
        return recentService.findRecentProcesses(8);
    }

    /** Notícias mais recentes, para a caixa de Avisos do Dashboard. */
    public List<Object> loadNotices() {
        return new ArrayList<>(noticeService.findAll());
    }

    /** Pastas, processos, ramais, avisos e ferramentas favoritados, combinados para a caixa de Favoritos. */
    public List<Object> loadFavorites() {
        List<Object> favorites = new ArrayList<>();
        if (preferences == null) {
            favorites.addAll(folderService.findFavorites());
            favorites.addAll(processService.findFavorites(20));
            favorites.addAll(phoneContactService.findFavorites(20));
            favorites.addAll(noticeService.findFavorites(20));
            favorites.addAll(toolService.findFavorites(20));
        } else {
            folderService.findAll().stream().filter(f -> preferences.isFavorite("folder", f.getId()))
                    .peek(f -> f.setFavorite(true)).forEach(favorites::add);
            for (var folder : folderService.findAll()) {
                processService.findByFolderId(folder.getId()).stream()
                        .filter(p -> preferences.isFavorite("process", p.getId()))
                        .peek(p -> p.setFavorite(true)).forEach(favorites::add);
            }
            phoneContactService.findAll().stream().filter(x -> preferences.isFavorite("phone", x.getId()))
                    .peek(x -> x.setFavorite(true)).forEach(favorites::add);
            noticeService.findAll().stream().filter(x -> preferences.isFavorite("notice", x.getId()))
                    .peek(x -> x.setFavorite(true)).forEach(favorites::add);
            toolService.findAll().stream().filter(x -> preferences.isFavorite("tool", x.getId()))
                    .peek(x -> x.setFavorite(true)).forEach(favorites::add);
        }
        return favorites;
    }
}
