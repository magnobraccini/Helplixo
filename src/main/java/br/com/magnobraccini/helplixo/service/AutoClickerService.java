package br.com.magnobraccini.helplixo.service;

import javax.swing.Timer;
import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

/**
 * Repetidor de F6: envia a tecla F6 em intervalos configuráveis, usando
 * {@link Robot}. A operação inicia pausada e deve ser retomada, pausada ou
 * desligada pelos controles manuais da interface.
 * <p>
 * Não é um hook global de teclado (isso exigiria uma biblioteca nativa fora
 * do classpath padrão do projeto); os eventos F6 simulados são enviados à
 * janela que estiver com foco no sistema operacional.
 * <p>
 * Proteções contra "liga sozinho e fecha sozinho":
 * <ul>
 *   <li>A primeira ativação fica pausada; nenhum evento F6 é enviado até
 *       o usuário retomar manualmente.</li>
 *   <li>Há uma contagem inicial ({@link #START_DELAY_MILLIS}) ao retomar.</li>
 * </ul>
 */
public final class AutoClickerService {
    /** Tempo, em ms, entre retomar e o primeiro F6 enviado. */
    public static final int START_DELAY_MILLIS = 2000;

    private final Robot robot;
    private Timer timer;
    private boolean active;
    private boolean paused = true;
    private int intervalMillis = 200;

    public AutoClickerService() {
        Robot created;
        try {
            created = new Robot();
            created.setAutoWaitForIdle(false);
        } catch (AWTException | HeadlessException exception) {
            created = null;
        }
        this.robot = created;
    }

    /** Indica se o repetidor pode funcionar neste ambiente (requer Robot/desktop). */
    public boolean isSupported() {
        return robot != null;
    }

    public boolean isRunning() {
        return active && !paused && timer != null && timer.isRunning();
    }

    /** Informa se o serviço foi ativado, inclusive quando está pausado. */
    public boolean isActive() {
        return active;
    }

    /** O serviço inicia pausado; retomar inicia o envio repetido de F6. */
    public boolean isPaused() {
        return paused;
    }

    public int getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(int intervalMillis) {
        this.intervalMillis = Math.max(20, intervalMillis);
        if (isRunning()) {
            timer.setDelay(this.intervalMillis);
        }
    }

    /** Ativa o serviço em pausa, sem enviar teclas. */
    public void start() {
        if (!isSupported() || active) {
            return;
        }
        active = true;
        paused = true;
    }

    /** Retoma o envio de F6 após a ativação ou uma pausa. */
    public void resume() {
        if (!isSupported() || !active || !paused) {
            return;
        }
        paused = false;
        timer = new Timer(intervalMillis, event -> sendF6());
        timer.setInitialDelay(START_DELAY_MILLIS);
        timer.setRepeats(true);
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        paused = true;
        active = false;
    }

    /** Pausa os eventos F6 simulados, mantendo o serviço ativado. */
    public void pause() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (active) {
            paused = true;
        }
    }

    private void sendF6() {
        robot.keyPress(KeyEvent.VK_F6);
        robot.keyRelease(KeyEvent.VK_F6);
    }
}
