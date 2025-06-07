package io.github.andyssder.ffind.model.idea;

import io.github.andyssder.ffind.common.ReferenceType;
import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;


public class CopyUsageInfo extends UsageInfo {

    private final ReferenceType type;

    public CopyUsageInfo(PsiReference reference, ReferenceType type) {
        super(reference);
        this.type = type;
    }

    public ReferenceType getType() {
        return type;
    }

}