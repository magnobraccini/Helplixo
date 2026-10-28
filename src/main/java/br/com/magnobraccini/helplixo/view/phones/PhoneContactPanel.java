package br.com.magnobraccini.helplixo.view.phones;

import br.com.magnobraccini.helplixo.model.PhoneContact;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class PhoneContactPanel extends JPanel {
    private static final int SEARCH_DEBOUNCE_MS = 150;
    private static final String ALL_SECTORS = "Todos os setores";
    private static final String ALL_FLOORS = "Todos os andares";

    private final PhoneContactTableModel tableModel = new PhoneContactTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> sectorFilter = new JComboBox<>();
    private final JComboBox<String> floorFilter = new JComboBox<>();
    private final JButton newButton = new JButton("Novo Ramal");
    private final JButton editButton = new JButton("Editar");
    private final JButton deleteButton = new JButton("Excluir");
    private final JButton favoriteButton = new JButton("Favoritar");
    private final Timer debounceTimer;

    private Consumer<String> searchListener = ignored -> { };
    private Consumer<String> sectorListener = ignored -> { };
    private Consumer<String> floorListener = ignored -> { };
    private Runnable newListener = () -> { };
    private Consumer<PhoneContact> editListener = ignored -> { };
    private Consumer<PhoneContact> deleteListener = ignored -> { };
    private Consumer<PhoneContact> favoriteListener = ignored -> { };
    private boolean updatingFilters;

    public PhoneContactPanel() {
        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Ramais");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(buildToolbar(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(36);
        table.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        updateButtonState();
        wireSearch();
        debounceTimer = new Timer(SEARCH_DEBOUNCE_MS, event -> searchListener.accept(searchField.getText()));
        debounceTimer.setRepeats(false);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));

        searchField.putClientProperty("JTextField.placeholderText",
                "Pesquisar por nome, setor, telefone, ramal ou email...");
        searchField.setPreferredSize(new Dimension(300, 32));

        sectorFilter.addItem(ALL_SECTORS);
        floorFilter.addItem(ALL_FLOORS);
        sectorFilter.addActionListener(event -> {
            if (!updatingFilters) {
                String selected = (String) sectorFilter.getSelectedItem();
                sectorListener.accept(ALL_SECTORS.equals(selected) ? null : selected);
            }
        });
        floorFilter.addActionListener(event -> {
            if (!updatingFilters) {
                String selected = (String) floorFilter.getSelectedItem();
                floorListener.accept(ALL_FLOORS.equals(selected) ? null : selected);
            }
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.add(searchField);
        filters.add(sectorFilter);
        filters.add(floorFilter);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(newButton);
        buttons.add(editButton);
        buttons.add(favoriteButton);
        buttons.add(deleteButton);

        newButton.addActionListener(event -> newListener.run());
        editButton.addActionListener(event -> editListener.accept(getSelected()));
        deleteButton.addActionListener(event -> deleteListener.accept(getSelected()));
        favoriteButton.addActionListener(event -> favoriteListener.accept(getSelected()));

        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private void wireSearch() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { debounceTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { debounceTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { debounceTimer.restart(); }
        });
    }

    private void onSelectionChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            updateButtonState();
        }
    }

    private void updateButtonState() {
        boolean hasSelection = getSelected() != null;
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
        favoriteButton.setEnabled(hasSelection);
    }

    public void onSearch(Consumer<String> listener) {
        searchListener = listener == null ? ignored -> { } : listener;
    }

    public void onSectorFilter(Consumer<String> listener) {
        sectorListener = listener == null ? ignored -> { } : listener;
    }

    public void onFloorFilter(Consumer<String> listener) {
        floorListener = listener == null ? ignored -> { } : listener;
    }

    /** Repopula as opções dos filtros de setor/andar a partir dos valores existentes, preservando a seleção atual. */
    public void updateFilterOptions(List<String> sectors, List<String> floors) {
        updatingFilters = true;
        try {
            repopulate(sectorFilter, ALL_SECTORS, sectors);
            repopulate(floorFilter, ALL_FLOORS, floors);
        } finally {
            updatingFilters = false;
        }
    }

    private void repopulate(JComboBox<String> combo, String allLabel, List<String> values) {
        Object previouslySelected = combo.getSelectedItem();
        combo.removeAllItems();
        combo.addItem(allLabel);
        for (String value : values) {
            combo.addItem(value);
        }
        if (previouslySelected != null && values.contains(previouslySelected)) {
            combo.setSelectedItem(previouslySelected);
        } else {
            combo.setSelectedItem(allLabel);
        }
    }

    public void onNew(Runnable listener) {
        newListener = listener == null ? () -> { } : listener;
    }

    public void onEdit(Consumer<PhoneContact> listener) {
        editListener = listener == null ? ignored -> { } : listener;
    }

    public void onDelete(Consumer<PhoneContact> listener) {
        deleteListener = listener == null ? ignored -> { } : listener;
    }

    public void onFavorite(Consumer<PhoneContact> listener) {
        favoriteListener = listener == null ? ignored -> { } : listener;
    }

    public void render(List<PhoneContact> contacts) {
        tableModel.setContacts(contacts);
        updateButtonState();
    }

    public void selectContact(UUID id) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (tableModel.getAt(row).getId().equals(id)) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                return;
            }
        }
    }

    private PhoneContact getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return tableModel.getAt(table.convertRowIndexToModel(row));
    }
}
