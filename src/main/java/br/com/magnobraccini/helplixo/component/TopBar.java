package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.core.AppSection;
import br.com.magnobraccini.helplixo.core.GearAction;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class TopBar extends JPanel {
    private static final int LIVE_SEARCH_DEBOUNCE_MS = 180;
    private static final int MINIMUM_QUERY_LENGTH = 2;

    private static final Color HEADER = Palette.SURFACE_DARK;
    private static final Color TEXT = Palette.TEXT_ON_DARK;
    private final JTextField searchField = new JTextField();
    private final JButton editModeButton = new JButton("Ativar edição");
    private final JButton gearButton = new JButton("⚙");
    private final Map<AppSection, JToggleButton> navigationButtons = new EnumMap<>(AppSection.class);
    private final Timer debounceTimer;
    private SearchResultsPopup resultsPopup;
    private Consumer<String> liveSearchListener = ignored -> { };
    private Consumer<Object> resultSelectionListener = ignored -> { };
    private Consumer<GearAction> gearListener = ignored -> { };

    public TopBar() {
        setLayout(new BorderLayout());
        setBackground(HEADER);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getForeground()));
        setPreferredSize(new Dimension(0, 116));
        debounceTimer = new Timer(LIVE_SEARCH_DEBOUNCE_MS, event -> runLiveSearch());
        debounceTimer.setRepeats(false);
        add(createBrandRow(), BorderLayout.NORTH);
        add(createNavigationRow(), BorderLayout.CENTER);
        wireSearchField();
    }

    /** Chamado a cada alteração no campo de busca (com debounce) enquanto o usuário digita. */
    public void onLiveSearch(Consumer<String> listener) {
        liveSearchListener = listener == null ? ignored -> { } : listener;
    }

    /** Chamado quando o usuário seleciona (clique ou Enter) um item nos resultados em tempo real. */
    public void onSearchResultSelected(Consumer<Object> listener) {
        resultSelectionListener = listener == null ? ignored -> { } : listener;
    }

    /** Exibe a lista de resultados instantâneos logo abaixo do campo de busca. */
    public void showSearchResults(List<?> results) {
        ensurePopup();
        resultsPopup.showResults(results, searchField);
    }

    public void hideSearchResults() {
        if (resultsPopup != null) {
            resultsPopup.hidePopup();
        }
    }

    public void onEditMode(java.awt.event.ActionListener listener) {
        editModeButton.addActionListener(listener);
    }

    public void onGearAction(Consumer<GearAction> listener) {
        gearListener = listener == null ? ignored -> { } : listener;
    }

    public void onNavigation(Consumer<AppSection> listener) {
        navigationButtons.forEach((section, button) ->
                button.addActionListener(event -> listener.accept(section)));
    }

    public void select(AppSection section) {
        JToggleButton button = navigationButtons.get(section);
        if (button != null) {
            button.setSelected(true);
        }
    }

    public void setEditMode(boolean editMode) {
        editModeButton.setText(editMode ? "Edição ativa" : "Ativar edição");
        editModeButton.putClientProperty("JButton.buttonType", editMode ? "success" : "default");
    }

    public void focusSearch() {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    private void wireSearchField() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                debounceTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                debounceTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                debounceTimer.restart();
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (resultsPopup == null) {
                    return;
                }
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_DOWN -> {
                        resultsPopup.selectNext();
                        event.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        resultsPopup.selectPrevious();
                        event.consume();
                    }
                    case KeyEvent.VK_ENTER -> {
                        if (resultsPopup.isShowingResults()) {
                            resultsPopup.confirmSelection();
                            event.consume();
                        }
                    }
                    case KeyEvent.VK_ESCAPE -> resultsPopup.hidePopup();
                    default -> { }
                }
            }
        });

        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                SwingUtilities.invokeLater(() -> {
                    if (resultsPopup != null) {
                        resultsPopup.hidePopup();
                    }
                });
            }
        });
    }

    private void runLiveSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().length() < MINIMUM_QUERY_LENGTH) {
            hideSearchResults();
            return;
        }
        liveSearchListener.accept(query);
    }

    private void ensurePopup() {
        if (resultsPopup == null) {
            resultsPopup = new SearchResultsPopup(SwingUtilities.getWindowAncestor(this));
            resultsPopup.onSelect(value -> resultSelectionListener.accept(value));
        }
    }

    private JPanel createBrandRow() {
        JPanel row = new JPanel(new BorderLayout(24, 0));
        row.setBackground(HEADER);
        row.setBorder(BorderFactory.createEmptyBorder(10, 18, 8, 18));
        row.setPreferredSize(new Dimension(0, 64));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setBackground(HEADER);
        JLabel logo = buildLogo();
        JLabel name = new JLabel("Helplixo");
        name.setForeground(TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 24f));
        brand.add(logo);
        brand.add(name);

        searchField.putClientProperty("JTextField.placeholderText", "Pesquisar...");
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.putClientProperty("JTextField.arc", 18);
        searchField.setBackground(Palette.SURFACE_DARK_ALT);
        searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER_DARK),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        searchField.setPreferredSize(new Dimension(460, 38));

        JPanel searchWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        searchWrapper.setBackground(HEADER);
        searchWrapper.add(searchField);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(HEADER);
        editModeButton.setFocusable(false);
        actions.add(editModeButton);
        actions.add(createGearButton());

        row.add(brand, BorderLayout.WEST);
        row.add(searchWrapper, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JButton createGearButton() {
        gearButton.setFocusable(false);
        gearButton.setToolTipText("Configurações");
        gearButton.setFont(gearButton.getFont().deriveFont(18f));
        gearButton.putClientProperty("JButton.buttonType", "toolBarButton");

        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("Configurações", GearAction.SETTINGS));
        menu.addSeparator();
        menu.add(menuItem("Fazer backup (salvar cópia)...", GearAction.BACKUP));
        menu.addSeparator();
        menu.add(menuItem("Alterar senha", GearAction.CHANGE_PASSWORD));
        menu.addSeparator();
        menu.add(menuItem("Autoclicker (F6)...", GearAction.AUTOCLICKER));
        menu.addSeparator();
        menu.add(menuItem("Sobre", GearAction.ABOUT));
        menu.add(menuItem("Sair", GearAction.EXIT));

        gearButton.addActionListener(event -> menu.show(gearButton, 0, gearButton.getHeight()));
        return gearButton;
    }

    private JMenuItem menuItem(String label, GearAction action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> gearListener.accept(action));
        return item;
    }

    private JPanel createNavigationRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        row.setBackground(HEADER);
        row.setBorder(BorderFactory.createEmptyBorder(0, 12, 5, 12));
        ButtonGroup group = new ButtonGroup();
        for (AppSection section : AppSection.values()) {
            if (section == AppSection.SETTINGS) {
                continue;
            }
            JToggleButton button = new JToggleButton(labelFor(section));
            button.setFocusable(false);
            button.setForeground(TEXT);
            button.setBackground(HEADER);
            button.setOpaque(true);
            button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            button.addItemListener(event -> button.setBackground(
                    button.isSelected() ? Palette.ACCENT : HEADER));
            button.setPreferredSize(new Dimension(widthFor(section), 40));
            button.putClientProperty("JButton.buttonType", "tab");
            group.add(button);
            navigationButtons.put(section, button);
            row.add(button);
        }
        row.add(Box.createHorizontalStrut(6));
        select(AppSection.DASHBOARD);
        return row;
    }

    private String labelFor(AppSection section) {
        return switch (section) {
            case DASHBOARD -> "⌂  Dashboard";
            case PROCESSES -> "▤  Processos";
            case PHONES -> "☎  Ramais";
            case NOTICES -> "◇  Avisos";
            case TOOLS -> "⌘  Ferramentas";
            case SETTINGS -> "⚙  Configurações";
        };
    }

    private int widthFor(AppSection section) {
        return switch (section) {
            case DASHBOARD -> 140;
            case PROCESSES -> 140;
            case PHONES -> 120;
            case NOTICES -> 115;
            case TOOLS -> 150;
            case SETTINGS -> 160;
        };
    }

    private JLabel buildLogo() {
        java.net.URL resource = TopBar.class.getResource("/icons/logo_32.png");
        if (resource != null) {
            return new JLabel(new ImageIcon(resource));
        }
        JLabel fallback = new JLabel("▣");
        fallback.setForeground(TEXT);
        fallback.setFont(fallback.getFont().deriveFont(Font.BOLD, 28f));
        return fallback;
    }
}
