package br.com.magnobraccini.helplixo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notice {
    private UUID id;
    private String title;
    private String message;
    private NoticeCategory category;
    private String createdBy;
    private boolean favorite;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Notice() {
    }

    public static Notice create(String title, String message, NoticeCategory category, String createdBy) {
        Notice notice = new Notice();
        notice.id = UUID.randomUUID();
        notice.title = title;
        notice.message = message;
        notice.category = category;
        notice.createdBy = createdBy;
        notice.createdAt = LocalDateTime.now();
        notice.updatedAt = notice.createdAt;
        return notice;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public NoticeCategory getCategory() { return category; }
    public void setCategory(NoticeCategory category) { this.category = category; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    @JsonIgnore
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
        return title;
    }
}
