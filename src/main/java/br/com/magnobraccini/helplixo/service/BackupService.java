package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.logging.LogService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Responsável por criar backups ZIP de toda a pasta de dados do Helplixo
 * (configuração, pastas, processos, ramais, avisos, ferramentas, histórico
 * etc.) e restaurar a partir de um backup existente.
 * <p>
 * A pasta de destino dos backups ({@code backups/}) e a pasta temporária
 * ({@code temp/}) nunca são incluídas no arquivo gerado.
 */
public final class BackupService {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String BACKUP_PREFIX = "helplixo_backup_";

    private final Path dataDirectory;
    private final Path backupsDirectory;
    private final LogService logService;

    public BackupService(Path dataDirectory, LogService logService) {
        this.dataDirectory = dataDirectory;
        this.backupsDirectory = dataDirectory.resolve("backups");
        this.logService = logService;
    }

    /** Cria um novo backup ZIP e remove backups excedentes conforme a retenção informada. */
    public synchronized Path createBackup(int retentionCount) {
        try {
            Files.createDirectories(backupsDirectory);
            String fileName = BACKUP_PREFIX + LocalDateTime.now().format(FILE_TIMESTAMP) + ".zip";
            Path target = backupsDirectory.resolve(fileName);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
                try (Stream<Path> paths = Files.walk(dataDirectory)) {
                    paths.filter(Files::isRegularFile)
                            .filter(this::isBackupable)
                            .forEach(path -> addEntry(zip, path));
                }
            }
            pruneOldBackups(retentionCount);
            logService.logBackup("Backup criado: " + fileName);
            return target;
        } catch (IOException exception) {
            logService.logError("Backup", exception);
            throw new IllegalStateException("Não foi possível criar o backup.", exception);
        }
    }

    /** Restaura os dados a partir de um arquivo de backup ZIP. Cria um backup de segurança antes de restaurar. */
    public synchronized void restoreBackup(Path zipFile, int retentionCount) {
        createBackup(retentionCount);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = dataDirectory.resolve(entry.getName()).normalize();
                if (!target.startsWith(dataDirectory)) {
                    throw new IOException("Entrada de backup inválida: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
            logService.logBackup("Backup restaurado a partir de: " + zipFile.getFileName());
        } catch (IOException exception) {
            logService.logError("Restauração de backup", exception);
            throw new IllegalStateException("Não foi possível restaurar o backup.", exception);
        }
    }

    public List<Path> listBackups() {
        try {
            if (Files.notExists(backupsDirectory)) {
                return List.of();
            }
            try (Stream<Path> paths = Files.list(backupsDirectory)) {
                return paths.filter(path -> path.getFileName().toString().endsWith(".zip"))
                        .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                        .toList();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível listar os backups.", exception);
        }
    }

    public Path getBackupsDirectory() {
        return backupsDirectory;
    }

    private void pruneOldBackups(int retentionCount) {
        if (retentionCount <= 0) {
            return;
        }
        List<Path> backups = listBackups();
        for (int i = retentionCount; i < backups.size(); i++) {
            try {
                Files.deleteIfExists(backups.get(i));
            } catch (IOException ignored) {
                // manter backups em caso de falha ao remover não é crítico
            }
        }
    }

    private boolean isBackupable(Path path) {
        Path relative = dataDirectory.relativize(path);
        String first = relative.getNameCount() > 0 ? relative.getName(0).toString() : "";
        return !first.equals("backups") && !first.equals("temp");
    }

    private void addEntry(ZipOutputStream zip, Path file) {
        try {
            String entryName = dataDirectory.relativize(file).toString().replace(java.io.File.separatorChar, '/');
            zip.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zip);
            zip.closeEntry();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private java.time.Instant lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            return java.time.Instant.EPOCH;
        }
    }
}
