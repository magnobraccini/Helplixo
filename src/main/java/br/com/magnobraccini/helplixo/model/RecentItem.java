package br.com.magnobraccini.helplixo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class RecentItem {
    private UUID id;
    private UUID processId;
    private LocalDateTime openedAt;

    public RecentItem() {
    }

    public static RecentItem create(UUID processId) {
        RecentItem item = new RecentItem();
        item.id = UUID.randomUUID();
        item.processId = processId;
        item.openedAt = LocalDateTime.now();
        return item;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProcessId() { return processId; }
    public void setProcessId(UUID processId) { this.processId = processId; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
}
