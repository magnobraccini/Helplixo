package br.com.magnobraccini.helplixo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

public class Folder {
    private UUID id;
    private UUID parentId;
    private String name;
    private String description;
    private String icon = "folder";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean favorite;
    private boolean visible = true;
    private boolean deleted;
    private int order;

    public Folder() {
    }

    public static Folder create(String name, String description, UUID parentId) {
        Folder folder = new Folder();
        folder.id = UUID.randomUUID();
        folder.name = name;
        folder.description = description;
        folder.parentId = parentId;
        folder.createdAt = LocalDateTime.now();
        folder.updatedAt = folder.createdAt;
        return folder;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    @JsonIgnore
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    @Override
    public String toString() {
        return name;
    }
}
