package br.com.magnobraccini.helplixo.view.tools;

import br.com.magnobraccini.helplixo.model.Tool;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ToolPanel extends JPanel {
    private static final int SEARCH_DEBOUNCE_MS = 150;

    private final ToolTableModel tableModel = new ToolTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField();
    private final JButton newButton = new JButton("Nova Ferramenta");
    private final JButton editButton = new JButton("Editar");
    private final JButton deleteButton = new JButton("Excluir");
    private final JButton favoriteButton = new JButton("Favoritar");
    private final JButton executeButton = new JButton("Executar");
    private final JButton copyFileButton = new JButton("📋 Copiar arquivo");
    private final JButton autoClickerButton = new JButton("⌨ Auto F6");
    private final Timer debounceTimer;

    private Consumer<String> searchListener = ignored -> { };
    private Runnable newListener = () -> { };
    private Consumer<Tool> editListener = ignored -> { };
    private Consumer<Tool> deleteListener = ignored -> { };
    private Consumer<Tool> favoriteListener = ignored -> { };
    private Consumer<Tool> executeListener = ignored -> { };
    private Consumer<Tool> copyFileListener = ignored -> { };
    private Runnable autoClickerListener = () -> { };

    public ToolPanel() {
        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Ferramentas");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(buildToolbar(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(0).setMaxWidth(36);
        table.getColumnModel().getColumn(1).setMaxWidth(40);
        table.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && getSelected() != null) {
                    executeListener.accept(getSelected());
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

        searchField.putClientProperty("JTextField.placeholderText", "Pesquisar por nome, categoria ou descrição...");
        searchField.setPreferredSize(new Dimension(320, 32));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        executeButton.putClientProperty("JButton.buttonType", "success");
        autoClickerButton.setFocusable(false);
        buttons.add(autoClickerButton);
        buttons.add(newButton);
        buttons.add(editButton);
        buttons.add(favoriteButton);
        buttons.add(deleteButton);
        buttons.add(copyFileButton);
        buttons.add(executeButton);

        autoClickerButton.addActionListener(event -> autoClickerListener.run());
        newButton.addActionListener(event -> newListener.run());
        editButton.addActionListener(event -> editListener.accept(getSelected()));
        deleteButton.addActionListener(event -> deleteListener.accept(getSelected()));
        favoriteButton.addActionListener(event -> favoriteListener.accept(getSelected()));
        copyFileButton.addActionListener(event -> copyFileListener.accept(getSelected()));
        executeButton.addActionListener(event -> executeListener.accept(getSelected()));

        toolbar.add(searchField, BorderLayout.WEST);
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
        executeButton.setEnabled(hasSelection);
        copyFileButton.setEnabled(hasSelection);
    }

    public void onSearch(Consumer<String> listener) {
        searchListener = listener == null ? ignored -> { } : listener;
    }

    public void onNew(Runnable listener) {
        newListener = listener == null ? () -> { } : listener;
    }

    public void onEdit(Consumer<Tool> listener) {
        editListener = listener == null ? ignored -> { } : listener;
    }

    public void onDelete(Consumer<Tool> listener) {
        deleteListener = listener == null ? ignored -> { } : listener;
    }

    public void onFavorite(Consumer<Tool> listener) {
        favoriteListener = listener == null ? ignored -> { } : listener;
    }

    public void onExecute(Consumer<Tool> listener) {
        executeListener = listener == null ? ignored -> { } : listener;
    }

    public void onCopyFile(Consumer<Tool> listener) {
        copyFileListener = listener == null ? ignored -> { } : listener;
    }

    public void onAutoClicker(Runnable listener) {
        autoClickerListener = listener == null ? () -> { } : listener;
    }

    public void render(List<Tool> tools) {
        tableModel.setTools(tools);
        updateButtonState();
    }

    public void selectTool(UUID id) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (tableModel.getAt(row).getId().equals(id)) {
                table.setRowSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
                return;
            }
        }
    }

    private Tool getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return tableModel.getAt(table.convertRowIndexToModel(row));
    }
}
