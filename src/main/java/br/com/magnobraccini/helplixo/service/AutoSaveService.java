package br.com.magnobraccini.helplixo.service;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Infraestrutura genérica de salvamento automático. Qualquer tela que
 * mantenha alterações pendentes (ex.: o editor de processos) pode registrar
 * uma verificação de pendência e uma ação de salvamento; o serviço cuida do
 * agendamento periódico em uma thread dedicada, sempre disparando a ação de
 * salvar de volta na Event Dispatch Thread do Swing.
 * <p>
 * Esta classe não depende de nenhuma tela específica — a integração com o
 * editor de processos (Notion-like) acontece em uma fase posterior da v0.3.
 */
public final class AutoSaveService {
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> currentTask;

    public AutoSaveService() {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "helplixo-autosave");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Inicia (ou reinicia) o auto-save com o intervalo informado.
     *
     * @param intervalSeconds   intervalo em segundos; valores &lt;= 0 desativam o auto-save
     * @param hasPendingChanges callback que indica se há alterações pendentes
     * @param saveAction        ação de salvamento, executada na EDT
     */
    public synchronized void start(int intervalSeconds, BooleanSupplier hasPendingChanges, Runnable saveAction) {
        stop();
        if (intervalSeconds <= 0) {
            return;
        }
        currentTask = executor.scheduleWithFixedDelay(() -> {
            try {
                if (hasPendingChanges.getAsBoolean()) {
                    SwingUtilities.invokeLater(saveAction);
                }
            } catch (RuntimeException ignored) {
                // uma falha na verificação não deve interromper o agendamento
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (currentTask != null) {
            currentTask.cancel(false);
            currentTask = null;
        }
    }

    public void shutdown() {
        stop();
        executor.shutdownNow();
    }
}
