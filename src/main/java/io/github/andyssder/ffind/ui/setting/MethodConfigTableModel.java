package io.github.andyssder.ffind.ui.setting;

import io.github.andyssder.ffind.model.MethodConfig;
import com.intellij.openapi.ui.Messages;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodConfigTableModel extends AbstractTableModel {
    private final List<MethodConfig> methodConfigs = new ArrayList<>();
    private final String[] columnNames = {"Class Name", "Method Name", "Parameters", "Source Index", "Target Index"};

    public List<MethodConfig> getMethodConfigs() {
        return Collections.unmodifiableList(methodConfigs);
    }

    public void setMethodConfigs(List<MethodConfig> newConfigs) {
        methodConfigs.clear();
        methodConfigs.addAll(newConfigs);
        fireTableDataChanged();
    }

    public void addMethodConfig(MethodConfig methodConfig) {
        methodConfigs.add(methodConfig);
        fireTableRowsInserted(methodConfigs.size() - 1, methodConfigs.size() - 1);
    }

    public void removeMethodConfig(int row) {
        methodConfigs.remove(row);
        fireTableRowsDeleted(row, row);
    }

    @Override
    public int getRowCount() {
        return methodConfigs.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        MethodConfig methodConfig = methodConfigs.get(row);
        return switch (column) {
            case 0 -> methodConfig.getClassName();
            case 1 -> methodConfig.getMethodName();
            case 2 -> String.join(", ", methodConfig.getParamNames());
            case 3 -> methodConfig.getSourceParamIndex();
            case 4 -> methodConfig.getTargetParamIndex();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column >= 3;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        MethodConfig methodConfig = methodConfigs.get(row);
        try {
            switch (column) {
                case 3: methodConfig.setSourceParamIndex(Integer.parseInt(value.toString())); break;
                case 4: methodConfig.setTargetParamIndex(Integer.parseInt(value.toString())); break;
            }
            fireTableCellUpdated(row, column);
        } catch (NumberFormatException e) {
            Messages.showErrorDialog("Invalid number format", "Error");
        }
    }
}