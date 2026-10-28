package br.com.magnobraccini.helplixo.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Registra eventos relevantes do sistema (criação, edição, exclusão, erros,
 * backup e mudanças de modo de edição) em um arquivo de log em texto simples.
 * <p>
 * A escrita nunca deve interromper o fluxo da aplicação: qualquer falha de
 * I/O ao gravar o log é silenciosamente ignorada.
 */
public final class LogService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile;
    private final boolean enabled;

    private LogService(Path logFile, boolean enabled) {
        this.logFile = logFile;
        this.enabled = enabled;
    }

    public static LogService forDataDirectory(Path dataDirectory) {
        return new LogService(dataDirectory.resolve("logs").resolve("helplixo.log"), true);
    }

    /**
     * Instância inerte, usada em testes unitários ou em contextos onde a
     * persistência em disco não é desejada.
     */
    public static LogService disabled() {
        return new LogService(null, false);
    }

    public void logCreate(String message) {
        write(LogEventType.CRIACAO, message);
    }

    public void logEdit(String message) {
        write(LogEventType.EDICAO, message);
    }

    public void logDelete(String message) {
        write(LogEventType.EXCLUSAO, message);
    }

    public void logError(String context, Throwable error) {
        write(LogEventType.ERRO, context + " -> " + error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    public void logError(String context, String message) {
        write(LogEventType.ERRO, context + " -> " + message);
    }

    public void logBackup(String message) {
        write(LogEventType.BACKUP, message);
    }

    public void logEditMode(boolean active) {
        write(LogEventType.MODO_EDICAO, active ? "Modo de edição ativado" : "Modo de edição desativado");
    }

    public synchronized List<String> readLastLines(int maxLines) {
        List<String> result = new ArrayList<>();
        if (!enabled || Files.notExists(logFile)) {
            return result;
        }
        try {
            List<String> allLines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            int from = Math.max(0, allLines.size() - maxLines);
            return new ArrayList<>(allLines.subList(from, allLines.size()));
        } catch (IOException exception) {
            return result;
        }
    }

    private synchronized void write(LogEventType type, String message) {
        if (!enabled) {
            return;
        }
        try {
            Files.createDirectories(logFile.getParent());
            String line = LocalDateTime.now().format(TIMESTAMP) + " | " + type.name() + " | " + message
                    + System.lineSeparator();
            Files.writeString(logFile, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            // A falha de log nunca deve derrubar a aplicação.
        }
    }
}
