package br.com.magnobraccini.helplixo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Per-Windows-account UI preferences; shared content remains in the common data directory. */
public final class UserPreferencesService {
    private static final TypeReference<List<String>> KEYS = new TypeReference<>() { };
    private final Path favoritesFile;
    private final ObjectMapper mapper;

    public UserPreferencesService(Path dataDirectory, ObjectMapper mapper) {
        this.mapper = mapper;
        String account = System.getProperty("user.name", "unknown");
        Path userDirectory = userDirectory(dataDirectory, account);
        this.favoritesFile = userDirectory.resolve("favorites.json");
    }

    public synchronized boolean isFavorite(String type, UUID id) {
        return read().contains(key(type, id));
    }

    public synchronized boolean toggleFavorite(String type, UUID id) {
        List<String> keys = read();
        String key = key(type, id);
        boolean favorite;
        if (keys.remove(key)) favorite = false;
        else { keys.add(key); favorite = true; }
        write(keys);
        return favorite;
    }

    public synchronized List<String> favoriteKeys() { return List.copyOf(read()); }

    private List<String> read() {
        try {
            return Files.exists(favoritesFile) ? new ArrayList<>(mapper.readValue(favoritesFile.toFile(), KEYS)) : new ArrayList<>();
        } catch (IOException e) { throw new IllegalStateException("Não foi possível ler os favoritos pessoais.", e); }
    }

    private void write(List<String> keys) {
        try {
            Files.createDirectories(favoritesFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(favoritesFile.toFile(), keys);
        } catch (IOException e) { throw new IllegalStateException("Não foi possível salvar os favoritos pessoais.", e); }
    }

    public static String key(String type, UUID id) { return type + ":" + id; }

    public static Path userDirectory(Path dataDirectory, String account) {
        return dataDirectory.resolve("users").resolve(hash(account));
    }

    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
