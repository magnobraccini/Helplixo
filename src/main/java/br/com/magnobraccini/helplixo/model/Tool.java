package br.com.magnobraccini.helplixo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Tool {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String executablePath;
    private String command;
    private String icon;
    private boolean favorite;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Tool() {
    }

    public static Tool create(String name, String description, String category, String executablePath,
                               String command, String icon) {
        Tool tool = new Tool();
        tool.id = UUID.randomUUID();
        tool.name = name;
        tool.description = description;
        tool.category = category;
        tool.executablePath = executablePath;
        tool.command = command;
        tool.icon = (icon == null || icon.isBlank()) ? "🛠" : icon;
        tool.createdAt = LocalDateTime.now();
        tool.updatedAt = tool.createdAt;
        return tool;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getExecutablePath() { return executablePath; }
    public void setExecutablePath(String executablePath) { this.executablePath = executablePath; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name;
    }
}
