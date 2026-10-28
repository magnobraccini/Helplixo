package br.com.magnobraccini.helplixo.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class PhoneContact {
    private UUID id;
    private String name;
    private String sector;
    private String floor;
    private String phone;
    private String extension;
    private String email;
    private boolean favorite;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PhoneContact() {
    }

    public static PhoneContact create(String name, String sector, String floor, String phone, String extension,
                                       String email) {
        PhoneContact contact = new PhoneContact();
        contact.id = UUID.randomUUID();
        contact.name = name;
        contact.sector = sector;
        contact.floor = floor;
        contact.phone = phone;
        contact.extension = extension;
        contact.email = email;
        contact.createdAt = LocalDateTime.now();
        contact.updatedAt = contact.createdAt;
        return contact;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
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
