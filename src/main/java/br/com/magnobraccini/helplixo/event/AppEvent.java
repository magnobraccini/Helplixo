package br.com.magnobraccini.helplixo.event;

import java.time.LocalDateTime;

public interface AppEvent {
    LocalDateTime occurredAt();
}
