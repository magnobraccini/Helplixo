package br.com.magnobraccini.helplixo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessDocument {
    private UUID id;
    private UUID folderId;
    private String title;
    private String description;
    private String objective;
    private String prerequisites;
    private String responsible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String category;
    private List<String> tags = new ArrayList<>();
    private List<ProcessStep> steps = new ArrayList<>();
    private List<ContentBlock> content = new ArrayList<>();
    private String observations;
    private List<String> links = new ArrayList<>();
    private boolean favorite;
    private boolean archived;
    private boolean deleted;
    private String customIcon = "document";

    public ProcessDocument() {
    }

    public static ProcessDocument create(UUID folderId, String title, String description) {
        ProcessDocument process = new ProcessDocument();
        process.id = UUID.randomUUID();
        process.folderId = folderId;
        process.title = title;
        process.description = description;
        process.createdAt = LocalDateTime.now();
        process.updatedAt = process.createdAt;
        return process;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getFolderId() { return folderId; }
    public void setFolderId(UUID folderId) { this.folderId = folderId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }
    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<ProcessStep> getSteps() { return steps; }
    public void setSteps(List<ProcessStep> steps) { this.steps = steps; }
    public List<ContentBlock> getContent() { return content; }
    public void setContent(List<ContentBlock> content) { this.content = content; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public List<String> getLinks() { return links; }
    public void setLinks(List<String> links) { this.links = links; }
    @JsonIgnore
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public String getCustomIcon() { return customIcon; }
    public void setCustomIcon(String customIcon) { this.customIcon = customIcon; }

    @Override
    public String toString() {
        return title;
    }
}
