package br.com.magnobraccini.helplixo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.magnobraccini.helplixo.core.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private final Path configFile;
    private final ObjectMapper objectMapper;

    public ConfigManager(Path dataDirectory, ObjectMapper objectMapper) {
        this.configFile = dataDirectory.resolve("config").resolve("config.json");
        this.objectMapper = objectMapper;
    }

    public Configuration load() {
        try {
            if (Files.notExists(configFile)) {
                Configuration configuration = new Configuration();
                save(configuration);
                return configuration;
            }
            Configuration configuration = objectMapper.readValue(configFile.toFile(), Configuration.class);
            applyEnvironmentPassword(configuration);
            return configuration;
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível carregar a configuração.", exception);
        }
    }

    private void applyEnvironmentPassword(Configuration configuration) {
        if (!Constants.DEFAULT_EDIT_PASSWORD.isBlank()) {
            configuration.setEditPassword(Constants.DEFAULT_EDIT_PASSWORD);
        }
    }

    public void save(Configuration configuration) {
        try {
            Files.createDirectories(configFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile.toFile(), configuration);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível salvar a configuração.", exception);
        }
    }
}
