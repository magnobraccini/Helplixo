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

    public DashboardService(FolderService folderService, ProcessService processService,
                             PhoneContactService phoneContactService, NoticeService noticeService,
                             ToolService toolService, RecentService recentService) {
        this.folderService = folderService;
        this.processService = processService;
        this.phoneContactService = phoneContactService;
        this.noticeService = noticeService;
        this.toolService = toolService;
        this.recentService = recentService;
    }

    public DashboardSummary loadSummary() {
        long favorites = folderService.countFavorites() + processService.countFavorites()
                + phoneContactService.countFavorites() + noticeService.countFavorites() + toolService.countFavorites();
        return new DashboardSummary(
                folderService.count(),
                processService.count(),
                favorites,
                recentService.findRecentProcesses(20).size());
    }

    public List<ProcessDocument> loadRecentProcesses() {
        List<ProcessDocument> recent = recentService.findRecentProcesses(8);
        return recent.isEmpty() ? processService.findRecent(8) : recent;
    }

    /** Notícias mais recentes, para a caixa de Avisos do Dashboard. */
    public List<Object> loadNotices() {
        return new ArrayList<>(noticeService.findAll());
    }

    /** Pastas, processos, ramais, avisos e ferramentas favoritados, combinados para a caixa de Favoritos. */
    public List<Object> loadFavorites() {
        List<Object> favorites = new ArrayList<>();
        favorites.addAll(folderService.findFavorites());
        favorites.addAll(processService.findFavorites(20));
        favorites.addAll(phoneContactService.findFavorites(20));
        favorites.addAll(noticeService.findFavorites(20));
        favorites.addAll(toolService.findFavorites(20));
        return favorites;
    }
}
