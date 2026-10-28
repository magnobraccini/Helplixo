package br.com.magnobraccini.helplixo.component;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Popup leve, não focalizável, usado para exibir resultados de pesquisa em
 * tempo real logo abaixo do campo de busca. Por não ser focalizável, clicar
 * em um item do popup não tira o foco do campo de texto, permitindo digitar
 * e navegar com as setas sem perder a digitação.
 */
public final class SearchResultsPopup extends JWindow {
    private static final int ROW_HEIGHT = 26;
    private static final int MAX_VISIBLE_ROWS = 10;

    private final DefaultListModel<Object> model = new DefaultListModel<>();
    private final JList<Object> list = new JList<>(model);
    private Consumer<Object> selectionListener = ignored -> { };

    public SearchResultsPopup(Window owner) {
        super(owner);
        setFocusableWindowState(false);
        setAlwaysOnTop(true);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(MAX_VISIBLE_ROWS);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                confirmSelection();
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(list.getForeground().darker(), 1));
        getContentPane().add(scrollPane);
    }

    public void onSelect(Consumer<Object> listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    public void showResults(List<?> results, Component anchor) {
        if (results.isEmpty()) {
            hidePopup();
            return;
        }
        model.clear();
        results.forEach(model::addElement);
        list.setSelectedIndex(0);

        Point location = anchor.getLocationOnScreen();
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, results.size());
        setLocation(location.x, location.y + anchor.getHeight() + 2);
        setSize(anchor.getWidth(), visibleRows * ROW_HEIGHT + 6);
        setVisible(true);
        toFront();
    }

    public void hidePopup() {
        setVisible(false);
    }

    public boolean isShowingResults() {
        return isVisible();
    }

    public void selectNext() {
        if (!isVisible() || model.isEmpty()) {
            return;
        }
        int next = Math.min(list.getSelectedIndex() + 1, model.getSize() - 1);
        list.setSelectedIndex(next);
        list.ensureIndexIsVisible(next);
    }

    public void selectPrevious() {
        if (!isVisible() || model.isEmpty()) {
            return;
        }
        int previous = Math.max(list.getSelectedIndex() - 1, 0);
        list.setSelectedIndex(previous);
        list.ensureIndexIsVisible(previous);
    }

    public void confirmSelection() {
        Object selected = list.getSelectedValue();
        if (selected != null) {
            hidePopup();
            selectionListener.accept(selected);
        }
    }
}
