package br.com.magnobraccini.helplixo.view.dialogs;

import br.com.magnobraccini.helplixo.service.AutoClickerService;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Caixa manual de controle do repetidor de F6. Não-modal e "always on top" para permitir que o
 * usuário continue usando outra janela como destino dos eventos. Expõe pausa, retomada e
 * desligamento; fechar a janela também desliga o serviço.
 */
public final class AutoClickerControlDialog extends JDialog {
    private final AutoClickerService service;
    private final JLabel status = new JLabel();
    private final JButton pauseResumeButton = new JButton();

    public AutoClickerControlDialog(Window owner, AutoClickerService service, Runnable onStopped,
                                    Runnable onStateChanged) {
        super(owner, "Autoclicker", ModalityType.MODELESS);
        this.service = service;
        setAlwaysOnTop(true);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        content.setBackground(Palette.CONTENT_BACKGROUND);

        status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));
        content.add(status, BorderLayout.NORTH);

        JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(service.getIntervalMillis(), 20, 60000, 10));
        intervalSpinner.addChangeListener(event -> service.setIntervalMillis((Integer) intervalSpinner.getValue()));

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 6));
        form.setOpaque(false);
        form.add(new JLabel("Intervalo entre F6 (ms):"));
        form.add(intervalSpinner);
        content.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        pauseResumeButton.addActionListener(event -> {
            if (service.isRunning()) {
                service.pause();
            } else {
                service.resume();
            }
            refreshState();
            onStateChanged.run();
        });

        JButton stopButton = new JButton("⏻  Desligar");
        stopButton.putClientProperty("JButton.buttonType", "default");
        stopButton.setHorizontalAlignment(SwingConstants.CENTER);
        stopButton.addActionListener(event -> {
            service.stop();
            onStopped.run();
            dispose();
        });
        actions.add(pauseResumeButton);
        actions.add(stopButton);
        content.add(actions, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                service.stop();
                onStopped.run();
                dispose();
            }
        });

        setContentPane(content);
        pack();
        refreshState();
        setLocationByPlatform(true);
        if (owner != null) {
            setLocation(owner.getX() + owner.getWidth() - getWidth() - 40, owner.getY() + 90);
        }
    }

    /** Atualiza os rótulos após uma mudança feita pelos controles manuais. */
    public void refreshState() {
        boolean running = service.isRunning();
        status.setText(running ? "● Autoclicker rodando" : "● Autoclicker pausado");
        status.setForeground(running ? Palette.SUCCESS : Palette.WARNING);
        pauseResumeButton.setText(running ? "⏸  Pausar" : "▶  Retomar");
    }
}
