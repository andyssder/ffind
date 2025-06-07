package io.github.andyssder.ffind.model.state;

import io.github.andyssder.ffind.model.MethodConfig;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@State(name = "MethodConfigSetting", storages = @Storage("ffindMethodConfigSetting.xml"), category = SettingsCategory.TOOLS	)
public class MethodConfigSetting implements PersistentStateComponent<MethodConfigSetting.State> {
    private State state = new State();

    public static MethodConfigSetting getInstance() {
        return ApplicationManager.getApplication().getService(MethodConfigSetting.class);
    }

    public static class State {

        @XCollection(elementName = "method-config")
        public List<MethodConfig> methodConfigs = new ArrayList<>();

        public State() {
            methodConfigs.addAll(getDefaultConfig());
        }
    }

    private static List<MethodConfig> getDefaultConfig() {
        List<MethodConfig> list = new ArrayList<>();
        list.add(new MethodConfig(
                "org.springframework.beans.BeanUtils",
                "copyProperties",
                Lists.newArrayList("source", "target"),
                0,
                1,
                false,
                false
        ));
        list.add(new MethodConfig(
                "org.springframework.beans.BeanUtils",
                "copyProperties",
                Lists.newArrayList("source", "target", "editable"),
                0,
                1,
                false,
                false
        ));
        list.add(new MethodConfig(
                "org.springframework.beans.BeanUtils",
                "copyProperties",
                Lists.newArrayList("source", "target", "ignoreProperties"),
                0,
                1,
                true,
                false
        ));
        list.add(new MethodConfig(
                "org.springframework.beans.BeanUtils",
                "copyProperties",
                Lists.newArrayList("source", "target", "editable", "ignoreProperties"),
                0,
                1,
                true,
                false
        ));
        return list;
    }

    public List<MethodConfig> getMethodConfigs() {
        return Collections.unmodifiableList(state.methodConfigs);
    }

    public void setMethodConfigs(List<MethodConfig> methodConfigs) {
        state.methodConfigs = methodConfigs;
    }

    public void addMethodConfig(MethodConfig methodConfig) {
        state.methodConfigs.add(methodConfig);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

}