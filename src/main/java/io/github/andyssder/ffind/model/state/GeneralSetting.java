package io.github.andyssder.ffind.model.state;

import io.github.andyssder.ffind.model.GeneralConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import org.jetbrains.annotations.NotNull;


@State(name = "GeneralSetting", storages = @Storage("ffindGeneralSetting.xml"))
public class GeneralSetting implements PersistentStateComponent<GeneralSetting.State> {
    private State state = new State();

    public static GeneralSetting getInstance() {
        return ApplicationManager.getApplication().getService(GeneralSetting.class);
    }

    public static class State {

        public GeneralConfig generalConfig;

        public State() {
            generalConfig = getDefaultConfig();
        }
    }

    private static GeneralConfig getDefaultConfig() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(true);
        generalConfig.setCacheTime(60 * 1000L);
        return generalConfig;
    }

    public GeneralConfig getGeneralConfig() {
        return state.generalConfig;
    }

    public void setGeneralConfig(GeneralConfig generalConfig) {
        state.generalConfig = generalConfig;
    }

    @Override
    public GeneralSetting.State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull GeneralSetting.State state) {
        this.state = state;
    }

}