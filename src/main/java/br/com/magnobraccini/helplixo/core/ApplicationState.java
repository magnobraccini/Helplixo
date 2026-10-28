package br.com.magnobraccini.helplixo.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.UUID;

public final class ApplicationState {
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private AppSection currentSection = AppSection.DASHBOARD;
    private UUID selectedFolderId;
    private UUID selectedProcessId;
    private boolean editMode;
    private String searchQuery = "";

    public AppSection getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(AppSection currentSection) {
        AppSection oldValue = this.currentSection;
        this.currentSection = currentSection;
        changes.firePropertyChange("currentSection", oldValue, currentSection);
    }

    public UUID getSelectedFolderId() {
        return selectedFolderId;
    }

    public void setSelectedFolderId(UUID selectedFolderId) {
        UUID oldValue = this.selectedFolderId;
        this.selectedFolderId = selectedFolderId;
        changes.firePropertyChange("selectedFolderId", oldValue, selectedFolderId);
    }

    public UUID getSelectedProcessId() {
        return selectedProcessId;
    }

    public void setSelectedProcessId(UUID selectedProcessId) {
        UUID oldValue = this.selectedProcessId;
        this.selectedProcessId = selectedProcessId;
        changes.firePropertyChange("selectedProcessId", oldValue, selectedProcessId);
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        boolean oldValue = this.editMode;
        this.editMode = editMode;
        changes.firePropertyChange("editMode", oldValue, editMode);
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        String oldValue = this.searchQuery;
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        changes.firePropertyChange("searchQuery", oldValue, this.searchQuery);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }
}
