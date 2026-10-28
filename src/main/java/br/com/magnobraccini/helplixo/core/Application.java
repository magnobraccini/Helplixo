package br.com.magnobraccini.helplixo.core;

import br.com.magnobraccini.helplixo.controller.MainController;
import br.com.magnobraccini.helplixo.theme.ThemeManager;
import br.com.magnobraccini.helplixo.theme.ThemeType;
import br.com.magnobraccini.helplixo.view.main.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.nio.file.Path;

public final class Application {
    /**
     * Tema fixo do Helplixo. A troca de tema em tempo de execução foi removida
     * (não fazia sentido para o uso real do sistema); o visual escuro
     * corporativo é aplicado sempre, independentemente do que estiver salvo
     * em config.json de instalações antigas.
     */
    private static final ThemeType FIXED_THEME = ThemeType.DARK;

    public void start() {
        try {
            Path dataDirectory = resolveDataDirectory();
            ApplicationContext context = ApplicationContext.bootstrap(dataDirectory);
            new DemoDataSeeder().seed(context);
            new ThemeManager().install(FIXED_THEME);
            SwingUtilities.invokeLater(() -> showMainWindow(context));
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(null,
                    "O Helplixo não pôde ser iniciado.\n" + exception.getMessage(),
                    Constants.APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMainWindow(ApplicationContext context) {
        MainFrame frame = new MainFrame();
        MainController controller = new MainController(context, frame);
        frame.setSize(context.getConfiguration().getWindowWidth(), context.getConfiguration().getWindowHeight());
        frame.setLocationRelativeTo(null);
        if (context.getConfiguration().isMaximized()) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        controller.initialize();
        frame.setVisible(true);
    }

    private Path resolveDataDirectory() {
        String customDirectory = System.getProperty("helplixo.dataDir");
        return customDirectory == null || customDirectory.isBlank()
                ? Path.of("data-dev").toAbsolutePath().normalize()
                : Path.of(customDirectory).toAbsolutePath().normalize();
    }
}
