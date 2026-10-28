package br.com.magnobraccini.helplixo.view.main;

import br.com.magnobraccini.helplixo.component.NotificationBanner;
import br.com.magnobraccini.helplixo.component.StatusBar;
import br.com.magnobraccini.helplixo.component.TopBar;
import br.com.magnobraccini.helplixo.core.AppSection;
import br.com.magnobraccini.helplixo.core.Constants;
import br.com.magnobraccini.helplixo.view.dashboard.DashboardPanel;
import br.com.magnobraccini.helplixo.view.notices.NoticePanel;
import br.com.magnobraccini.helplixo.view.phones.PhoneContactPanel;
import br.com.magnobraccini.helplixo.view.process.ProcessPanel;
import br.com.magnobraccini.helplixo.view.settings.SettingsPanel;
import br.com.magnobraccini.helplixo.view.tools.ToolPanel;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public final class MainFrame extends JFrame {
    private final TopBar topBar = new TopBar();
    private final NotificationBanner notificationBanner = new NotificationBanner();
    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final ProcessPanel processPanel = new ProcessPanel();
    private final PhoneContactPanel phoneContactPanel = new PhoneContactPanel();
    private final NoticePanel noticePanel = new NoticePanel();
    private final ToolPanel toolPanel = new ToolPanel();
    private final SettingsPanel settingsPanel = new SettingsPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<AppSection, String> cardNames = new EnumMap<>(AppSection.class);
    private final StatusBar statusBar;

    public MainFrame() {
        super(Constants.APP_NAME);
        statusBar = new StatusBar();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(Constants.MINIMUM_WIDTH, Constants.MINIMUM_HEIGHT));
        setLayout(new BorderLayout());
        java.net.URL iconResource = MainFrame.class.getResource("/icons/logo_64.png");
        if (iconResource != null) {
            setIconImage(new javax.swing.ImageIcon(iconResource).getImage());
        }
        initializeCards();
        installShortcuts();
        JPanel header = new JPanel(new BorderLayout());
        header.add(topBar, BorderLayout.NORTH);
        header.add(notificationBanner, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    public NotificationBanner getNotificationBanner() { return notificationBanner; }

    public void initializeNavigation(Consumer<AppSection> listener) {
        topBar.onNavigation(listener);
    }

    public void showSection(AppSection section) {
        cardLayout.show(contentPanel, cardNames.get(section));
        topBar.select(section);
    }

    public TopBar getTopBar() { return topBar; }
    public DashboardPanel getDashboardPanel() { return dashboardPanel; }
    public ProcessPanel getProcessPanel() { return processPanel; }
    public PhoneContactPanel getPhoneContactPanel() { return phoneContactPanel; }
    public NoticePanel getNoticePanel() { return noticePanel; }
    public ToolPanel getToolPanel() { return toolPanel; }
    public SettingsPanel getSettingsPanel() { return settingsPanel; }
    public StatusBar getStatusBar() { return statusBar; }

    private void initializeCards() {
        register(AppSection.DASHBOARD, dashboardPanel);
        register(AppSection.PROCESSES, processPanel);
        register(AppSection.PHONES, phoneContactPanel);
        register(AppSection.NOTICES, noticePanel);
        register(AppSection.TOOLS, toolPanel);
        register(AppSection.SETTINGS, settingsPanel);
    }

    private void register(AppSection section, JPanel panel) {
        String name = section.name();
        cardNames.put(section, name);
        contentPanel.add(panel, name);
    }

    private void installShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        getRootPane().getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                topBar.focusSearch();
            }
        });

    }
}
