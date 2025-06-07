package io.github.andyssder.ffind.handler;

import io.github.andyssder.ffind.detector.CopyUsageInfoDetector;
import io.github.andyssder.ffind.detector.CopyUsageInfoDetectorFactory;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import com.intellij.find.findUsages.CustomUsageSearcher;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.PsiElement;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.Processor;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class FindCopyUsageSearcher extends CustomUsageSearcher {

    @Override
    public void processElementUsages(@NotNull PsiElement element, @NotNull Processor<? super Usage> processor, @NotNull FindUsagesOptions options) {

        CopyUsageInfoDetector copyUsageInfoDetector = CopyUsageInfoDetectorFactory.getDetector(element);
        if (Objects.isNull(copyUsageInfoDetector)) {
            return;
        }
        List<CopyUsageInfo> usageInfoList = copyUsageInfoDetector.findCopyUsageInfo(element, options);
        if (CollectionUtils.isEmpty(usageInfoList)) {
            return;
        }
        usageInfoList.forEach(usageInfo -> {
            Usage usage = new UsageInfo2UsageAdapter(usageInfo);
            processor.process(usage);
        });
    }
}
