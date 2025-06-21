package io.github.andyssder.ffind.detector;

import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PropertyUtil;
import io.github.andyssder.ffind.common.FindType;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import io.github.andyssder.ffind.model.state.MethodConfigSetting;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


/**
 * find copy usages for setter/getter method
 */
public class MethodCopyUsageInfoDetector extends AbstractCopyUsageInfoDetector {

    @Override
    public @NotNull List<CopyUsageInfo> findCopyUsageInfo(PsiElement element, FindUsagesOptions options) {
        PsiMethod method = (PsiMethod) element;

        boolean isSetter = PropertyUtil.isSimplePropertySetter(method);
        PsiField targetField;
        FindType findType;
        if (isSetter) {
            targetField = PropertyUtil.getFieldOfSetter(method);
            findType = FindType.INDIRECT_WRITE;
        } else {
            targetField = PropertyUtil.getFieldOfGetter(method);
            findType = FindType.INDIRECT_READ;
        }
        if (Objects.isNull(targetField)) {
            return new ArrayList<>();
        }

        String cacheKey = generateKeyForMethod(method);
        List<CopyUsageInfo> cacheResult = getCacheResult(cacheKey);
        if (CollectionUtils.isNotEmpty(cacheResult)) {
            return cacheResult;
        }

        List<CopyUsageInfo> result = new ArrayList<>();

        Project project = targetField.getProject();
        ReadAction.compute(() -> {
            List<MethodConfig> methodConfigs = MethodConfigSetting.getInstance().getMethodConfigs();
            methodConfigs.forEach(methodConfig -> {
                List<PsiMethodCallExpression> psiMethodCallExpressionList = CopyMethodReferenceDetector.findMethodCalls(project, methodConfig);
                psiMethodCallExpressionList.forEach(methodCallExpression ->
                        result.addAll(findCopyUsagesForField(methodCallExpression, targetField, methodConfig, findType)));
            });
            return true;
        });

        setCacheResult(cacheKey, result);
        return result;
    }

    @Override
    public Boolean isEnable(PsiElement element) {
        if (! (element instanceof PsiMethod method)) {
            return false;
        }
        return PropertyUtil.isSimplePropertySetter(method) || PropertyUtil.isSimplePropertyGetter(method);
    }

    /**
     * generate cache key for method: className#MethodName
     * @param method target setter/getter method which user wants to find
     * @return cache key
     */
    private String generateKeyForMethod(PsiMethod method) {
        AtomicReference<String> key = new AtomicReference<>();
        ReadAction.compute(() -> {
            PsiClass containingClass = method.getContainingClass();
            String className = containingClass != null ? containingClass.getQualifiedName() : "Anonymous";
            key.set(className + "#" + method.getName());
            return true;
        });
        return key.get();
    }
}
