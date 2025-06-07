package io.github.andyssder.ffind.detector;

import com.intellij.psi.*;
import io.github.andyssder.ffind.model.MethodConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class MethodSignatureMatcher {

    /**
     * return match result of given call and method config
     * @param targetMethod method find in files
     * @param methodConfig user config
     * @return true when matched or false when not
     */
    public static boolean matches(PsiMethod targetMethod, MethodConfig methodConfig) {
        if (targetMethod == null) {
            return false;
        }
        PsiClass containingClass = targetMethod.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        // 安全获取类全限定名
        String className = containingClass.getQualifiedName();
        if (!StringUtils.equals(className, methodConfig.getClassName())) {
            return false;
        }
        String methodName = targetMethod.getName();
        if (!StringUtils.equals(methodName, methodConfig.getMethodName())) {
            return false;
        }

        PsiParameterList psiParameterList = targetMethod.getParameterList();
        List<String> paramNameList = Arrays.stream(psiParameterList.getParameters()).map(PsiParameter::getName).toList();
        List<String> paramConfigList = methodConfig.getParamNames();

        return paramNameList.equals(paramConfigList);
    }
}
