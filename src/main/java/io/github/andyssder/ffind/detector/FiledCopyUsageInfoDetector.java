package io.github.andyssder.ffind.detector;

import io.github.andyssder.ffind.common.FindType;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import io.github.andyssder.ffind.model.state.MethodConfigSetting;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * find copy usages for field
 */
public class FiledCopyUsageInfoDetector extends AbstractCopyUsageInfoDetector {

    @Override
    public @NotNull List<CopyUsageInfo> findCopyUsageInfo(PsiElement element, FindUsagesOptions options) {
        PsiField field = (PsiField) element;

        String cacheKey = generateKeyForField(field);
        List<CopyUsageInfo> cacheResult = getCacheResult(cacheKey);
        if (CollectionUtils.isNotEmpty(cacheResult)) {
            return cacheResult;
        }

        List<CopyUsageInfo> result = new ArrayList<>();
        Project project = field.getProject();
        ReadAction.compute(() -> {
            List<MethodConfig> methodConfigs = MethodConfigSetting.getInstance().getMethodConfigs();
            methodConfigs.forEach(methodConfig -> {
                List<PsiMethodCallExpression> psiMethodCallExpressionList = CopyMethodReferenceDetector.findMethodCalls(project, methodConfig);
                psiMethodCallExpressionList.forEach(methodCallExpression ->
                        result.addAll(findCopyUsagesForField(methodCallExpression, field, methodConfig, FindType.ALL)));
            });
            return true;
        });
        setCacheResult(cacheKey, result);
        return result;
    }

    @Override
    public Boolean isEnable(PsiElement element) {
        return element instanceof PsiField;
    }

    /**
     * generate cache key for field: className#fieldName
     * @param field the field which user wants to find
     * @return cache key
     */
    private String generateKeyForField(PsiField field) {
        AtomicReference<String> key = new AtomicReference<>();
        ReadAction.compute(() -> {
            PsiClass containingClass = field.getContainingClass();
            String className = containingClass != null ? containingClass.getQualifiedName() : "Anonymous";
            key.set(className + "#" + field.getName());
            return true;
        });
        return key.get();
    }

}