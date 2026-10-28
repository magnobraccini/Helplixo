package br.com.magnobraccini.helplixo.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProcessCreatedEvent(UUID processId, LocalDateTime occurredAt) implements AppEvent {
    public ProcessCreatedEvent(UUID processId) {
        this(processId, LocalDateTime.now());
    }
}
