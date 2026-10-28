package br.com.magnobraccini.helplixo.view.notices;

import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.model.NoticeCategory;

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

public final class NoticePanel extends JPanel {
    private static final int SEARCH_DEBOUNCE_MS = 150;
    private static final String ALL_CATEGORIES = "Todas as categorias";

    private final NoticeTableModel tableModel = new NoticeTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JButton newButton = new JButton("Novo Aviso");
    private final JButton editButton = new JButton("Editar");
    private final JButton deleteButton = new JButton("Excluir");
    private final JButton favoriteButton = new JButton("Favoritar");
    private final Timer debounceTimer;

    private Consumer<String> searchListener = ignored -> { };
    private Consumer<NoticeCategory> categoryListener = ignored -> { };
    private Runnable newListener = () -> { };
    private Consumer<Notice> editListener = ignored -> { };
    private Consumer<Notice> deleteListener = ignored -> { };
    private Consumer<Notice> favoriteListener = ignored -> { };

    public NoticePanel() {
        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Avisos");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(buildToolbar(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(36);
        table.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                Notice selected = getSelected();
                if (selected != null) {
                    br.com.magnobraccini.helplixo.view.dialogs.NoticeDetailDialog.show(NoticePanel.this, selected, null);
                }
            }
        });
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
        updateButtonState();
        wireSearch();
        debounceTimer = new Timer(SEARCH_DEBOUNCE_MS, event -> searchListener.accept(searchField.getText()));
        debounceTimer.setRepeats(false);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Pesquisar avisos...");
        searchField.setPreferredSize(new Dimension(300, 32));

        categoryFilter.addItem(ALL_CATEGORIES);
        for (NoticeCategory category : NoticeCategory.values()) {
            categoryFilter.addItem(category.getDisplayName());
        }
        categoryFilter.addActionListener(event -> {
            String selected = (String) categoryFilter.getSelectedItem();
            categoryListener.accept(ALL_CATEGORIES.equals(selected) ? null : categoryFromLabel(selected));
        });

        filters.add(searchField);
        filters.add(categoryFilter);

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

    private NoticeCategory categoryFromLabel(String label) {
        for (NoticeCategory category : NoticeCategory.values()) {
            if (category.getDisplayName().equals(label)) {
                return category;
            }
        }
        return null;
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

    public void onCategoryFilter(Consumer<NoticeCategory> listener) {
        categoryListener = listener == null ? ignored -> { } : listener;
    }

    public void onNew(Runnable listener) {
        newListener = listener == null ? () -> { } : listener;
    }

    public void onEdit(Consumer<Notice> listener) {
        editListener = listener == null ? ignored -> { } : listener;
    }

    public void onDelete(Consumer<Notice> listener) {
        deleteListener = listener == null ? ignored -> { } : listener;
    }

    public void onFavorite(Consumer<Notice> listener) {
        favoriteListener = listener == null ? ignored -> { } : listener;
    }

    public void render(List<Notice> notices) {
        tableModel.setNotices(notices);
        updateButtonState();
    }

    public void selectNotice(UUID id) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (tableModel.getAt(row).getId().equals(id)) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                return;
            }
        }
    }

    private Notice getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return tableModel.getAt(table.convertRowIndexToModel(row));
    }
}
