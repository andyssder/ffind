package io.github.andyssder.ffind.model;

import java.io.Serializable;

public class GeneralConfig implements Serializable {

    private Long cacheTime = 0L;

    private Boolean cacheEnable = true;

    public GeneralConfig() {}

    public Long getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(Long cacheTime) {
        this.cacheTime = cacheTime;
    }

    public Boolean getCacheEnable() {
        return cacheEnable;
    }

    public void setCacheEnable(Boolean cacheEnable) {
        this.cacheEnable = cacheEnable;
    }
}
