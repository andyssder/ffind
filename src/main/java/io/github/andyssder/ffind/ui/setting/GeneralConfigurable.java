package io.github.andyssder.ffind.ui.setting;

import io.github.andyssder.ffind.model.GeneralConfig;
import io.github.andyssder.ffind.model.state.GeneralSetting;
import com.intellij.openapi.options.Configurable;

import javax.swing.*;
import java.awt.*;

public class GeneralConfigurable implements Configurable {
    private JCheckBox cacheEnableCheckbox;
    private JTextField cacheTimeField;
    private final GeneralSetting settings = GeneralSetting.getInstance();

    @Override
    public String getDisplayName() {
        return "General Settings";
    }

    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(createSeparator(), BorderLayout.NORTH);
        panel.add(createConfigRow(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSeparator() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JLabel label = new JLabel("Cache");
        label.setAlignmentY(Component.CENTER_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        panel.add(label);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(separator);

        return panel;
    }

    private JPanel createConfigRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));

        cacheEnableCheckbox = new JCheckBox("Enable Cache", getGeneralConfig().getCacheEnable());
        panel.add(cacheEnableCheckbox);

        panel.add(createTimeField());
        return panel;
    }

    private JPanel createTimeField() {
        JPanel container = new JPanel(new BorderLayout(5, 0));
        container.add(new JLabel("Expire Time:"), BorderLayout.WEST);

        cacheTimeField = new JTextField(8);
        cacheTimeField.setText(String.valueOf(settings.getGeneralConfig().getCacheTime()));
        container.add(cacheTimeField, BorderLayout.CENTER);

        container.add(new JLabel("ms"), BorderLayout.EAST);
        return container;
    }

    @Override
    public boolean isModified() {
        return cacheEnableCheckbox.isSelected() != getGeneralConfig().getCacheEnable() ||
                !cacheTimeField.getText().equals(String.valueOf(getGeneralConfig().getCacheTime()));
    }

    @Override
    public void apply() {
        GeneralConfig newConfig = new GeneralConfig();
        newConfig.setCacheEnable(cacheEnableCheckbox.isSelected());
        newConfig.setCacheTime(Long.parseLong(cacheTimeField.getText()));
        setGeneralConfig(newConfig);
    }

    private GeneralConfig getGeneralConfig() {
        return settings.getGeneralConfig();
    }

    private void setGeneralConfig(GeneralConfig generalConfig) {
        settings.setGeneralConfig(generalConfig);
    }
}
