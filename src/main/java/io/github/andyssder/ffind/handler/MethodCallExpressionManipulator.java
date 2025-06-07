package io.github.andyssder.ffind.handler;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MethodCallExpressionManipulator implements ElementManipulator<PsiMethodCallExpression> {

    @Override
    public @NotNull PsiMethodCallExpression handleContentChange(
            @NotNull PsiMethodCallExpression element,
            @NotNull TextRange range,
            @NotNull String newContent
    ) throws IncorrectOperationException {
        String originalText = element.getText();
        String newText = originalText.substring(0, range.getStartOffset())
                + newContent
                + originalText.substring(range.getEndOffset());
        return (PsiMethodCallExpression) element.replace(
                PsiElementFactory.getInstance(element.getProject())
                        .createExpressionFromText(newText, element.getContext())
        );
    }

    @Override
    public @Nullable PsiMethodCallExpression handleContentChange(
            @NotNull PsiMethodCallExpression element,
            @NotNull String newContent
    ) throws IncorrectOperationException {
        return handleContentChange(element, TextRange.allOf(element.getText()), newContent);
    }

    @Override
    public @NotNull TextRange getRangeInElement(@NotNull PsiMethodCallExpression element) {
        // highlight all for now
        PsiReferenceExpression methodExpr = element.getMethodExpression();
        return new TextRange(0, methodExpr.getAbsoluteRange().getEndOffset());
    }
}