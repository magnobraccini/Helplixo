package br.com.magnobraccini.helplixo.view.tools;

import br.com.magnobraccini.helplixo.model.Tool;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class ToolTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"★", "", "Nome", "Categoria", "Descrição", "Comando"};

    private List<Tool> tools = new ArrayList<>();

    void setTools(List<Tool> tools) {
        this.tools = new ArrayList<>(tools);
        fireTableDataChanged();
    }

    Tool getAt(int rowIndex) {
        return tools.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return tools.size();
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
        Tool tool = tools.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> tool.isFavorite() ? "★" : "";
            case 1 -> tool.getIcon();
            case 2 -> tool.getName();
            case 3 -> tool.getCategory();
            case 4 -> tool.getDescription();
            case 5 -> tool.getCommand() == null || tool.getCommand().isBlank()
                    ? tool.getExecutablePath() : tool.getCommand();
            default -> "";
        };
    }
}
