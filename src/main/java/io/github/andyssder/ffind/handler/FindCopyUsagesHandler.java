package io.github.andyssder.ffind.handler;

import io.github.andyssder.ffind.detector.CopyUsageInfoDetector;
import io.github.andyssder.ffind.detector.CopyUsageInfoDetectorFactory;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.*;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Processor;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@Deprecated
public class FindCopyUsagesHandler extends FindUsagesHandler {

    public FindCopyUsagesHandler(PsiElement element) {
        super(element);
    }

    @Override
    public boolean processElementUsages(@NotNull PsiElement element,
                                        @NotNull Processor<? super UsageInfo> processor,
                                        @NotNull FindUsagesOptions options) {

        boolean baseResult = super.processElementUsages(element, processor, options);
        if (!baseResult) {
            return false;
        }
        if (!options.isUsages) {
            return true;
        }

        CopyUsageInfoDetector copyUsageInfoDetector = CopyUsageInfoDetectorFactory.getDetector(element);
        if (Objects.isNull(copyUsageInfoDetector)) {
            return true;
        }
        List<CopyUsageInfo> usageInfoList = copyUsageInfoDetector.findCopyUsageInfo(element, options);
        if (CollectionUtils.isEmpty(usageInfoList)) {
            return true;
        }

        usageInfoList.forEach(processor::process);
        return true;
    }

}