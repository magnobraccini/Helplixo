package br.com.magnobraccini.helplixo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

/** Detecta alterações externas na pasta de dados por um retrato dos arquivos. */
public final class WorkspaceWatcherService {
    private final Path dataDirectory;
    private volatile String lastKnownSignature;

    public WorkspaceWatcherService(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.lastKnownSignature = currentSignature();
    }

    /** Retorna true se o conteúdo/metadata monitorado mudou desde o último reconhecimento. */
    public boolean hasExternalChanges() {
        return !currentSignature().equals(lastKnownSignature);
    }

    /** Marca o estado atual como conhecido e silencia avisos para mudanças já carregadas. */
    public synchronized void acknowledge() {
        lastKnownSignature = currentSignature();
    }

    private String currentSignature() {
        StringBuilder signature = new StringBuilder();
        appendFile(signature, dataDirectory.resolve("folders.json"));
        appendFile(signature, dataDirectory.resolve("phones.json"));
        appendFile(signature, dataDirectory.resolve("notices.json"));
        appendFile(signature, dataDirectory.resolve("tools.json"));
        Path processes = dataDirectory.resolve("processes");
        if (Files.notExists(processes)) return signature.toString();
        try (Stream<Path> paths = Files.walk(processes)) {
            paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> processes.relativize(path).toString()))
                    .forEach(path -> appendFile(signature, path));
        } catch (IOException exception) {
            signature.append("<unreadable-processes>");
        }
        return signature.toString();
    }

    private void appendFile(StringBuilder signature, Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            signature.append(dataDirectory.relativize(path)).append('|')
                    .append(attributes.lastModifiedTime().toMillis()).append('|')
                    .append(attributes.size()).append('\n');
        } catch (IOException exception) {
            signature.append(dataDirectory.relativize(path)).append("|missing\n");
        }
    }
}