package io.github.andyssder.ffind.detector;

import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CopyUsageInfoDetector {

    /**
     * the most important method in the plugin
     * @param element target element
     * @param options find option
     * @return usage info of target element in copy method
     */
    @NotNull
    List<CopyUsageInfo> findCopyUsageInfo(PsiElement element, FindUsagesOptions options);

    /**
     * chose detector by element
     * @param element target element
     * @return true if detector can process target element  or false if not
     */
    Boolean isEnable(PsiElement element);
}
