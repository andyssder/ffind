package io.github.andyssder.ffind.detector;

import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.*;
import io.github.andyssder.ffind.cache.FindResultCache;
import io.github.andyssder.ffind.common.FindType;
import io.github.andyssder.ffind.common.ReferenceType;
import io.github.andyssder.ffind.model.GeneralConfig;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import io.github.andyssder.ffind.model.idea.MethodCallReference;
import io.github.andyssder.ffind.model.state.GeneralSetting;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractCopyUsageInfoDetector implements CopyUsageInfoDetector{

    /**
     * the most important method in the plugin
     * @param element target element
     * @param options find option
     * @return usage info of target element in copy method
     */
    @NotNull
    public abstract List<CopyUsageInfo> findCopyUsageInfo(PsiElement element, FindUsagesOptions options);

    /**
     * chose detector by element
     * @param element target element
     * @return true if detector can process target element  or false if not
     */
    public abstract Boolean isEnable(PsiElement element);

    /**
     * find usages in given PsiMethodCallExpression for target PsiField
     * @return list of copy usages for target field
     */
    List<CopyUsageInfo> findCopyUsagesForField(PsiMethodCallExpression matchedCall, PsiField targetField, MethodConfig methodConfig, FindType findType) {

        List<CopyUsageInfo> results = new ArrayList<>();

        PsiClass searchContainingClass = targetField.getContainingClass();
        if (searchContainingClass == null) {
            return results;
        }

        String searchClassName = searchContainingClass.getQualifiedName();
        if (StringUtils.isEmpty(searchClassName)) {
            return results;
        }

        PsiExpression[] matchedCallArgs = matchedCall.getArgumentList().getExpressions();
        int sourceParamIndex = methodConfig.getSourceParamIndex();
        int targetParamIndex = methodConfig.getTargetParamIndex();
        if (matchedCallArgs.length <= Math.max(sourceParamIndex, targetParamIndex)) {
            return results;
        }

        PsiExpression sourceArg = matchedCallArgs[sourceParamIndex];
        PsiExpression targetArg = matchedCallArgs[targetParamIndex];

        if (methodConfig.getIncludeFieldParamEnable() ^ methodConfig.getExcludeFiledParamEnable()) {
            boolean isContainSearchField = false;
            int methodParamNum = methodConfig.getParamNames().size();
            for (int i = methodParamNum - 1; i < matchedCallArgs.length; i++) {
                PsiExpression arg = matchedCallArgs[i];

                String target = targetField.getName();
                String argText = arg.getText().replaceAll("\"", "");
                if (StringUtils.equals(argText, target)) {
                    isContainSearchField = true;
                    break;
                }
            }
            boolean includeCondition = methodConfig.getIncludeFieldParamEnable() && !isContainSearchField;
            boolean excludeCondition = methodConfig.getExcludeFiledParamEnable() && isContainSearchField;
            if (includeCondition || excludeCondition) {
                return results;
            }
        }
        if (!FindType.INDIRECT_WRITE.equals(findType) && sourceArg.getType() != null && sourceArg.getType().equalsToText(searchClassName)) {
            results.add(new CopyUsageInfo(new MethodCallReference(matchedCall, sourceArg), ReferenceType.INDIRECT_READ));
        }
        if (!FindType.INDIRECT_READ.equals(findType) && targetArg.getType() != null && targetArg.getType().equalsToText(searchClassName)) {
            results.add(new CopyUsageInfo(new MethodCallReference(matchedCall, targetArg), ReferenceType.INDIRECT_WRITE));
        }

        return results;
    }

    List<CopyUsageInfo> getCacheResult(String cacheKey) {
        GeneralConfig generalConfig = GeneralSetting.getInstance().getGeneralConfig();
        if (generalConfig.getCacheEnable()) {
            Optional<List<CopyUsageInfo>> cached = FindResultCache.getInstance().getCachedResul(cacheKey);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        return null;
    }

    void setCacheResult(String cacheKey, List<CopyUsageInfo> result) {
        if (CollectionUtils.isEmpty(result)) {
            return;
        }
        GeneralConfig generalConfig = GeneralSetting.getInstance().getGeneralConfig();
        if (generalConfig.getCacheEnable()) {
            FindResultCache.getInstance().updateCacheResult(cacheKey, result, generalConfig.getCacheTime());
        }
    }

}
