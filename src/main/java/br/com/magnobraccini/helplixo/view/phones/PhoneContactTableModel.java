package br.com.magnobraccini.helplixo.view.phones;

import br.com.magnobraccini.helplixo.model.PhoneContact;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

final class PhoneContactTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"★", "Nome", "Setor", "Andar", "Telefone", "Ramal", "Email"};

    private List<PhoneContact> contacts = new ArrayList<>();

    void setContacts(List<PhoneContact> contacts) {
        this.contacts = new ArrayList<>(contacts);
        fireTableDataChanged();
    }

    PhoneContact getAt(int rowIndex) {
        return contacts.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return contacts.size();
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
        PhoneContact contact = contacts.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> contact.isFavorite() ? "★" : "";
            case 1 -> contact.getName();
            case 2 -> contact.getSector();
            case 3 -> contact.getFloor();
            case 4 -> contact.getPhone();
            case 5 -> contact.getExtension();
            case 6 -> contact.getEmail();
            default -> "";
        };
    }
}
