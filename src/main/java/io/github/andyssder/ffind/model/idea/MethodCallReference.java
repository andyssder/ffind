package io.github.andyssder.ffind.model.idea;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;

public class MethodCallReference extends PsiReferenceBase<PsiMethodCallExpression> {

    private final PsiElement targetElement;

    public MethodCallReference(@NotNull PsiMethodCallExpression element, @NotNull PsiElement target) {
        super(element);
        this.targetElement = target;
    }

    @Override
    public PsiElement resolve() {
        return targetElement;
    }

}