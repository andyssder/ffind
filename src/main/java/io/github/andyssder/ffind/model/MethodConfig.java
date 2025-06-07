package io.github.andyssder.ffind.model;

import java.io.Serializable;
import java.util.List;

public class MethodConfig implements Serializable {

    private String className;
    private String methodName;
    private List<String> paramNames;
    private int sourceParamIndex;
    private int targetParamIndex;
    private boolean includeFieldParamEnable;
    private boolean excludeFiledParamEnable;

    public MethodConfig() {

    }

    public MethodConfig(String className, String methodName, List<String> paramNames,
                        int sourceParamIndex, int targetParamIndex, boolean excludeFiledParamEnable, boolean includeFieldParamEnable) {
        this.className = className;
        this.methodName = methodName;
        this.paramNames = paramNames;
        this.sourceParamIndex = sourceParamIndex;
        this.targetParamIndex = targetParamIndex;
        this.excludeFiledParamEnable = excludeFiledParamEnable;
        this.includeFieldParamEnable = includeFieldParamEnable;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public void setParamNames(List<String> paramNames) {
        this.paramNames = paramNames;
    }

    public int getSourceParamIndex() {
        return sourceParamIndex;
    }

    public void setSourceParamIndex(int sourceParamIndex) {
        this.sourceParamIndex = sourceParamIndex;
    }

    public int getTargetParamIndex() {
        return targetParamIndex;
    }

    public void setTargetParamIndex(int targetParamIndex) {
        this.targetParamIndex = targetParamIndex;
    }

    public boolean getIncludeFieldParamEnable() {
        return includeFieldParamEnable;
    }

    public void setIncludeFieldParamEnable(boolean includeFieldParamEnable) {
        this.includeFieldParamEnable = includeFieldParamEnable;
    }

    public boolean getExcludeFiledParamEnable() {
        return excludeFiledParamEnable;
    }

    public void setExcludeFiledParamEnable(boolean excludeFiledParamEnable) {
        this.excludeFiledParamEnable = excludeFiledParamEnable;
    }

    @Override
    public String toString() {
        return "MethodConfig{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", paramNameList=" + paramNames +
                ", sourceParamIndex=" + sourceParamIndex +
                ", targetParamIndex=" + targetParamIndex +
                ", includeFieldParamIndex=" + includeFieldParamEnable +
                ", excludeFiledParamIndex=" + excludeFiledParamEnable +
                '}';
    }
}