package io.github.andyssder.ffind.detector;

import io.github.andyssder.ffind.cache.FindResultCache;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import io.github.andyssder.ffind.model.idea.MethodCallReference;
import io.github.andyssder.ffind.common.ReferenceType;
import io.github.andyssder.ffind.model.GeneralConfig;
import io.github.andyssder.ffind.model.state.GeneralSetting;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.*;
import io.github.andyssder.ffind.model.state.MethodConfigSetting;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FiledCopyUsageInfoDetector implements CopyUsageInfoDetector {

    @Override
    public @NotNull List<CopyUsageInfo> findCopyUsageInfo(PsiElement element, FindUsagesOptions options) {
        if (!isEnable(element)) {
            return new ArrayList<>();
        }
        PsiField field = (PsiField) element;
        return doFind(field);
    }

    /**
     * real find method
     */
    private List<CopyUsageInfo> doFind(PsiField field) {
        List<CopyUsageInfo> cacheResult = getCache(field);
        if (CollectionUtils.isNotEmpty(cacheResult)) {
            return cacheResult;
        }

        List<CopyUsageInfo> result = new ArrayList<>();

        Project project = field.getProject();
        ReadAction.compute(() -> ProjectRootManager.getInstance(project)
                .getFileIndex()
                .iterateContent(file -> {
                    if (!file.getName().endsWith(".java")) {
                        return true;
                    }
                    PsiJavaFile psiFile = (PsiJavaFile) PsiManager.getInstance(project).findFile(file);
                    if (psiFile == null) {
                        return true;
                    }
                    PsiTreeUtil.processElements(psiFile, element -> {
                        if (element instanceof PsiMethodCallExpression call) {
                            List<MethodConfig> methodConfigs = MethodConfigSetting.getInstance().getMethodConfigs();
                            methodConfigs.forEach(config -> {
                                if (MethodSignatureMatcher.matches(call.resolveMethod(), config)) {
                                    processMatchedCall(call, field, config, result);
                                }
                            });
                        }
                        return true;
                    });

                    return true;
                }));
        setCache(field, result);
        return result;
    }

    @Override
    public Boolean isEnable(PsiElement element) {
        return element instanceof PsiField;
    }

    private List<CopyUsageInfo> getCache(PsiField field) {
        GeneralConfig generalConfig = GeneralSetting.getInstance().getGeneralConfig();
        if (generalConfig.getCacheEnable()) {
            Optional<List<CopyUsageInfo>> cached = FindResultCache.getInstance().getCachedResultByField(field);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        return null;
    }

    private void setCache(PsiField field, List<CopyUsageInfo> result) {
        if (CollectionUtils.isEmpty(result)) {
            return;
        }
        GeneralConfig generalConfig = GeneralSetting.getInstance().getGeneralConfig();
        if (generalConfig.getCacheEnable()) {
            FindResultCache.getInstance().updateCache(field, result, generalConfig.getCacheTime());
        }
    }

    
    private void processMatchedCall(PsiMethodCallExpression matchedCall,
                                           PsiField searchField,
                                           MethodConfig methodConfig,
                                           List<CopyUsageInfo> results) {
        PsiClass searchContainingClass = searchField.getContainingClass();
        if (searchContainingClass == null) {
            return;
        }

        String searchClassName = searchContainingClass.getQualifiedName();
        if (StringUtils.isEmpty(searchClassName)) {
            return;
        }

        PsiExpression[] matchedCallArgs = matchedCall.getArgumentList().getExpressions();
        int sourceParamIndex = methodConfig.getSourceParamIndex();
        int targetParamIndex = methodConfig.getTargetParamIndex();
        if (matchedCallArgs.length <= Math.max(sourceParamIndex, targetParamIndex)) {
            return;
        }

        PsiExpression sourceArg = matchedCallArgs[sourceParamIndex];
        PsiExpression targetArg = matchedCallArgs[targetParamIndex];

        if (methodConfig.getIncludeFieldParamEnable() ^ methodConfig.getExcludeFiledParamEnable()) {
            boolean isContainSearchField = false;
            int methodParamNum = methodConfig.getParamNames().size();
            for (int i = methodParamNum - 1; i < matchedCallArgs.length; i++) {
                PsiExpression arg = matchedCallArgs[i];

                String target = searchField.getName();
                String argText = arg.getText().replaceAll("\"", "");
                if (StringUtils.equals(argText, target)) {
                    isContainSearchField = true;
                    break;
                }
            }
            boolean includeCondition = methodConfig.getIncludeFieldParamEnable() && !isContainSearchField;
            boolean excludeCondition = methodConfig.getExcludeFiledParamEnable() && isContainSearchField;
            if (includeCondition || excludeCondition) {
                return;
            }
        }
        if (sourceArg.getType() != null && sourceArg.getType().equalsToText(searchClassName)) {
            results.add(new CopyUsageInfo(new MethodCallReference(matchedCall, sourceArg), ReferenceType.INDIRECT_READ));
        }
        if (targetArg.getType() != null && targetArg.getType().equalsToText(searchClassName)) {
            results.add(new CopyUsageInfo(new MethodCallReference(matchedCall, targetArg), ReferenceType.INDIRECT_WRITE));
        }
    }

}