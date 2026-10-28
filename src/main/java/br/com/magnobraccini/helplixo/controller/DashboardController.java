package br.com.magnobraccini.helplixo.controller;

import br.com.magnobraccini.helplixo.service.DashboardService;
import br.com.magnobraccini.helplixo.view.dashboard.DashboardPanel;

public final class DashboardController {
    private final DashboardService service;
    private final DashboardPanel view;

    public DashboardController(DashboardService service, DashboardPanel view) {
        this.service = service;
        this.view = view;
    }

    public void refresh() {
        view.render(service.loadSummary(), service.loadRecentProcesses(), service.loadFavorites(),
                service.loadNotices());
    }
}
