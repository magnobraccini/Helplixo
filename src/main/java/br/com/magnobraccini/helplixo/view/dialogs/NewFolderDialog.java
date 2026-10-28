package br.com.magnobraccini.helplixo.view.dialogs;

import br.com.magnobraccini.helplixo.model.Folder;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Diálogo de criação de seção (pasta). Permite escolher explicitamente a
 * pasta-pai, incluindo a opção de criar no nível superior, em vez de sempre
 * herdar a pasta atualmente selecionada na árvore.
 */
public final class NewFolderDialog {

    public record Result(String name, UUID parentId) {
    }

    public Result request(Component parent, List<Folder> allFolders, UUID defaultParentId) {
        JTextField nameField = new JTextField(24);
        JComboBox<FolderOption> parentCombo = new JComboBox<>();
        parentCombo.addItem(new FolderOption(null, "— Nível superior (sem pasta pai) —"));
        for (FolderOption option : orderedOptions(allFolders)) {
            parentCombo.addItem(option);
        }
        selectDefault(parentCombo, defaultParentId);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Nome da nova seção:"));
        panel.add(nameField);
        panel.add(new JLabel("Criar dentro de:"));
        panel.add(parentCombo);

        int option = JOptionPane.showConfirmDialog(parent, panel, "Nova seção",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            return null;
        }
        FolderOption selected = (FolderOption) parentCombo.getSelectedItem();
        UUID parentId = selected == null ? null : selected.id();
        return new Result(name.trim(), parentId);
    }

    private void selectDefault(JComboBox<FolderOption> combo, UUID defaultParentId) {
        if (defaultParentId == null) {
            combo.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (defaultParentId.equals(combo.getItemAt(i).id())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    private List<FolderOption> orderedOptions(List<Folder> allFolders) {
        Map<UUID, List<Folder>> childrenByParent = new HashMap<>();
        for (Folder folder : allFolders) {
            childrenByParent.computeIfAbsent(folder.getParentId(), ignored -> new ArrayList<>()).add(folder);
        }
        childrenByParent.values().forEach(list -> list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName())));
        List<FolderOption> options = new ArrayList<>();
        appendChildren(null, childrenByParent, options, 0);
        return options;
    }

    private void appendChildren(UUID parentId, Map<UUID, List<Folder>> childrenByParent,
                                 List<FolderOption> options, int depth) {
        for (Folder folder : childrenByParent.getOrDefault(parentId, List.of())) {
            String indent = "    ".repeat(depth);
            options.add(new FolderOption(folder.getId(), indent + folder.getName()));
            appendChildren(folder.getId(), childrenByParent, options, depth + 1);
        }
    }

    private record FolderOption(UUID id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
