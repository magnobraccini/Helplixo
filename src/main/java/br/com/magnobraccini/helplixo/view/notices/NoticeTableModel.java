package br.com.magnobraccini.helplixo.view.notices;

import br.com.magnobraccini.helplixo.model.Notice;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class NoticeTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"★", "Categoria", "Título", "Mensagem", "Atualizado em"};
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<Notice> notices = new ArrayList<>();

    void setNotices(List<Notice> notices) {
        this.notices = new ArrayList<>(notices);
        fireTableDataChanged();
    }

    Notice getAt(int rowIndex) {
        return notices.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return notices.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Notice notice = notices.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> notice.isFavorite() ? "★" : "";
            case 1 -> notice.getCategory().getDisplayName();
            case 2 -> notice.getTitle();
            case 3 -> preview(notice.getMessage());
            case 4 -> notice.getUpdatedAt() == null ? "" : notice.getUpdatedAt().format(FORMAT);
            default -> "";
        };
    }

    private String preview(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String singleLine = message.replace('\n', ' ').trim();
        return singleLine.length() > 80 ? singleLine.substring(0, 80) + "…" : singleLine;
    }
}
