package br.com.magnobraccini.helplixo.view.dialogs;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Escolhe a seção de destino sem duplicar o processo ou a árvore de seções. */
public final class MoveToFolderDialog {
    public record Destination(UUID folderId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public Destination request(Component parent, String itemName, List<Destination> destinations,
                                UUID selectedFolderId) {
        if (destinations == null || destinations.isEmpty()) {
            return null;
        }
        JComboBox<Destination> choices = new JComboBox<>();
        destinations.forEach(choices::addItem);
        destinations.stream()
                .filter(destination -> Objects.equals(destination.folderId(), selectedFolderId))
                .findFirst()
                .ifPresent(choices::setSelectedItem);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(new JLabel("Escolha a seção de destino para “" + itemName + "”:"), BorderLayout.NORTH);
        content.add(choices, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(parent, content, "Mover para seção",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return result == JOptionPane.OK_OPTION ? (Destination) choices.getSelectedItem() : null;
    }
}
