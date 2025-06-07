package io.github.andyssder.ffind.ui.setting;

import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.state.MethodConfigSetting;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class MethodConfigurable implements Configurable {
    private final MethodConfigTableModel tableModel = new MethodConfigTableModel();
    private JBTable methodConfigTable;

    @Override
    public String getDisplayName() {
        return "Method Config";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(UIUtil.getPanelBackground());

        methodConfigTable = new JBTable(tableModel);
        methodConfigTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        // Class Name
        methodConfigTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        // Method Name
        methodConfigTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        // Parameters
        methodConfigTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        methodConfigTable.setRowHeight(25);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(methodConfigTable)
                .setAddAction(e -> showAddDialog())
                .setEditAction(e -> editSelectedMethodConfig())
                .setRemoveAction(e -> removeSelectedMethodConfig())
                .disableUpDownActions();

        return decorator.createPanel();
    }

    private void showAddDialog() {
        MethodConfigEditorDialog dialog = new MethodConfigEditorDialog();
        if (dialog.showAndGet()) {
            MethodConfig newConfig = dialog.getConfig();
            tableModel.addMethodConfig(newConfig);
            tableModel.fireTableDataChanged();
        }
    }

    private void editSelectedMethodConfig() {
        int row = methodConfigTable.getSelectedRow();
        if (row >= 0) {
            MethodConfig methodConfig = tableModel.getMethodConfigs().get(row);
            MethodConfigEditorDialog dialog = new MethodConfigEditorDialog(methodConfig);
            if (dialog.showAndGet()) {
                List<MethodConfig> methodConfigs = new ArrayList<>(tableModel.getMethodConfigs());
                methodConfigs.set(row, dialog.getConfig());
                tableModel.setMethodConfigs(methodConfigs);
            }
        }
    }

    private void removeSelectedMethodConfig() {
        int row = methodConfigTable.getSelectedRow();
        if (row >= 0) {
            tableModel.removeMethodConfig(row);
        }
    }

    @Override
    public boolean isModified() {
        return !tableModel.getMethodConfigs().equals(getMethodConfig());
    }

    @Override
    public void apply() {
        setMethodConfig(tableModel.getMethodConfigs());
    }

    @Override
    public void reset() {
        tableModel.setMethodConfigs(getMethodConfig());
    }


    private void setMethodConfig(List<MethodConfig> methodConfigs) {
        MethodConfigSetting.getInstance().setMethodConfigs(methodConfigs);
    }


    private List<MethodConfig> getMethodConfig() {
        return MethodConfigSetting.getInstance().getMethodConfigs();
    }

}