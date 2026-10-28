package br.com.magnobraccini.helplixo.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FolderCreatedEvent(UUID folderId, LocalDateTime occurredAt) implements AppEvent {
    public FolderCreatedEvent(UUID folderId) {
        this(folderId, LocalDateTime.now());
    }
}
