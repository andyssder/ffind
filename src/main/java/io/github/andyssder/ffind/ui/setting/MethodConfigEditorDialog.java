package io.github.andyssder.ffind.ui.setting;

import io.github.andyssder.ffind.model.MethodConfig;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MethodConfigEditorDialog extends DialogWrapper {

    private JTextField classNameField;
    private JTextField methodNameField;
    private JComboBox<Integer> sourceIndexCombo;
    private JComboBox<Integer> targetIndexCombo;
    private JCheckBox includeEnableCombo;
    private JCheckBox excludeEnableCombo;
    private JBList<String> paramsList;
    private DefaultListModel<String> paramsListModel;
    private MethodConfig originalMethodConfig;

    public MethodConfigEditorDialog() {
        super(true);
        init();
        setTitle("Edit Method Config");
        addParamsListListener();
    }

    public MethodConfigEditorDialog(MethodConfig methodConfig) {
        this();
        this.originalMethodConfig = methodConfig;
        loadMethodConfigData();
        addParamsListListener();

    }

    private void addParamsListListener() {
        paramsListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                updateIndexComboOptions();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                updateIndexComboOptions();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {

            }
        });
    }

    private void updateIndexComboOptions() {
        int paramCount = paramsListModel.size();
        Integer[] indexes = IntStream.range(-1, paramCount)
                .boxed().toArray(Integer[]::new);
        sourceIndexCombo.setModel(new DefaultComboBoxModel<>(indexes));
        targetIndexCombo.setModel(new DefaultComboBoxModel<>(indexes));
    }

    private void loadMethodConfigData() {
        classNameField.setText(originalMethodConfig.getClassName());
        methodNameField.setText(originalMethodConfig.getMethodName());
        originalMethodConfig.getParamNames().forEach(paramsListModel::addElement);
        sourceIndexCombo.setSelectedItem(originalMethodConfig.getSourceParamIndex());
        targetIndexCombo.setSelectedItem(originalMethodConfig.getTargetParamIndex());
        includeEnableCombo.setSelected(originalMethodConfig.getIncludeFieldParamEnable());
        excludeEnableCombo.setSelected(originalMethodConfig.getExcludeFiledParamEnable());
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5, 10); // 统一间距
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Class Name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createLabeledField("Class Name:", classNameField = new JBTextField(25)), gbc);

        // Method Name
        gbc.gridy++;
        panel.add(createLabeledField("Method Name:", methodNameField = new JBTextField(20)), gbc);

        // Parameters
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(createParamsSection(), gbc);

        // Index Selectors
        gbc.gridy++;
        gbc.weighty = 0;
        panel.add(createIndexSelectors(), gbc);

        // bottom
        gbc.gridy++;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return new JBScrollPane(panel);
    }

    private void addParam(AnActionButton button) {
        String param = Messages.showInputDialog("Enter parameter name:", "Add Parameter", null);
        if (param != null && !param.trim().isEmpty()) {
            paramsListModel.addElement(param.trim());
        }
    }

    private void removeParam(AnActionButton button) {
        int selectedIndex = paramsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            paramsListModel.remove(selectedIndex);
        }
    }

    private void editParam(AnActionButton button) {
        int selectedIndex = paramsList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String oldParam = paramsListModel.getElementAt(selectedIndex);
            String newParam = Messages.showInputDialog("Edit parameter:", "Edit Parameter", null, oldParam, null);
            if (newParam != null && !newParam.trim().isEmpty()) {
                paramsListModel.set(selectedIndex, newParam.trim());
            }
        }
    }

    @Override
    protected void doOKAction() {
        if (validateInput()) {
            super.doOKAction();
        }
    }

    private boolean validateInput() {
        if (classNameField.getText().trim().isEmpty()) {
            showError("Class name cannot be empty");
            return false;
        }
        if (methodNameField.getText().trim().isEmpty()) {
            showError("Method name cannot be empty");
            return false;
        }
        return validateParamIndexes();
    }

    private boolean validateParamIndexes() {
        int paramCount = paramsListModel.size();
        int sourceIndex = (Integer) sourceIndexCombo.getSelectedItem();
        int targetIndex = (Integer) targetIndexCombo.getSelectedItem();

        boolean includeEnable = includeEnableCombo.isSelected();
        boolean excludeEnable = excludeEnableCombo.isSelected();

        if (sourceIndex >= paramCount || targetIndex >= paramCount) {
            showError("Param index exceeds parameter count");
            return false;
        }
        if (includeEnable && excludeEnable) {
            showError("Include Fields Enable and exclude Fields Enable can't be true at the same time");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Messages.showErrorDialog(message, "Input Error");
    }

    public MethodConfig getConfig() {
        List<String> params = new ArrayList<>();
        for (int i = 0; i < paramsListModel.size(); i++) {
            params.add(paramsListModel.getElementAt(i));
        }

        return new MethodConfig(
                classNameField.getText().trim(),
                methodNameField.getText().trim(),
                params,
                (Integer) sourceIndexCombo.getSelectedItem(),
                (Integer) targetIndexCombo.getSelectedItem(),
                excludeEnableCombo.isSelected(),
                includeEnableCombo.isSelected()
        );
    }


    private JComponent createLabeledField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(new JBLabel(label), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createParamsSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Parameters"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        paramsListModel = new DefaultListModel<>();
        paramsList = new JBList<>(paramsListModel);
        paramsList.setVisibleRowCount(5);
        paramsList.setBackground(UIUtil.getPanelBackground());
        paramsList.setForeground(UIUtil.getLabelForeground());

        paramsList.getEmptyText().setText("No parameters defined");

        return ToolbarDecorator.createDecorator(paramsList)
                .setAddAction(this::addParam)
                .setRemoveAction(this::removeParam)
                .setEditAction(this::editParam)
                .setPreferredSize(new Dimension(350, 180))
                .createPanel();
    }

    private JComponent createIndexPanel(String labelText, JComboBox<Integer> comboBox) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        comboBox.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(@NotNull JList<? extends Integer> list, Integer value,
                                  int index, boolean selected, boolean hasFocus) {
                setText(value == -1 ? "N/A" : value.toString());
                setForeground(selected ? UIManager.getColor("List.selectionForeground") : UIUtil.getLabelForeground());
            }
        });

        comboBox.setModel(new DefaultComboBoxModel<>(
                IntStream.range(-1, paramsListModel.size())
                        .boxed()
                        .toArray(Integer[]::new)
        ));

        JLabel label = new JBLabel(labelText);
        label.setLabelFor(comboBox);
        panel.add(label, BorderLayout.WEST);

        comboBox.setPreferredSize(new Dimension(120, 28));
        comboBox.setBackground(UIUtil.getTextFieldBackground());
        comboBox.setForeground(UIUtil.getTextFieldForeground());
        panel.add(comboBox, BorderLayout.CENTER);

        return panel;
    }

    private JComponent createIndexSelectors() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 15, 0));

        JPanel panel1 = new JPanel(new GridLayout(1, 2, 15, 0));
        sourceIndexCombo = new ComboBox<>();
        targetIndexCombo = new ComboBox<>();
        panel1.add(createIndexPanel("Source Filed Index:", sourceIndexCombo));
        panel1.add(createIndexPanel("Target Filed Index:", targetIndexCombo));
        updateIndexComboOptions();

        JPanel panel2 = new JPanel(new GridLayout(1, 2, 15, 0));
        includeEnableCombo = new JCheckBox("Include Fields Enable", false);
        panel2.add(includeEnableCombo);
        excludeEnableCombo = new JCheckBox("Exclude Fields Enable", false);
        panel2.add(excludeEnableCombo);

        panel.add(panel1);
        panel.add(panel2);
        return panel;
    }

}
