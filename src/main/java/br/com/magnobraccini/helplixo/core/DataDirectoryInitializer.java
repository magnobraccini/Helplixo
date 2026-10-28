package br.com.magnobraccini.helplixo.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DataDirectoryInitializer {
    private static final List<String> DIRECTORIES = List.of(
            "config", "processes", "attachments", "backups", "temp", "logs");

    public void initialize(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
            for (String directory : DIRECTORIES) {
                Files.createDirectories(dataDirectory.resolve(directory));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível inicializar a pasta de dados.", exception);
        }
    }
}
