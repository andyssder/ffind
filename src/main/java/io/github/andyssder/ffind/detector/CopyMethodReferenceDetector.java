package io.github.andyssder.ffind.detector;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import io.github.andyssder.ffind.model.MethodConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CopyMethodReferenceDetector {

    /**
     * return reference method of target method config
     * @param project idea project
     * @param methodConfig user config
     * @return list of target method call expression
     */
    public static List<PsiMethodCallExpression> findMethodCalls(Project project, MethodConfig methodConfig) {

        List<PsiMethodCallExpression> result = new ArrayList<>();

        PsiMethod psiMethod = findPsiMethod(project, methodConfig);
        if (psiMethod == null) {
            return result;
        }

        Query<PsiReference> query = ReferencesSearch.search(psiMethod);
        query.forEach(ref -> {
            PsiElement element = ref.getElement();

            PsiMethodCallExpression callExpression = findCallExpression(element);
            if (callExpression != null) {
                result.add(callExpression);
            }
            return true;
        });

        return result;
    }

    /**
     * find psi method by method config
     */
    private static PsiMethod findPsiMethod(Project project, MethodConfig methodConfig) {

        String targetClassName = methodConfig.getClassName();
        String targetMethodName = methodConfig.getMethodName();

        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(targetClassName, GlobalSearchScope.allScope(project));

        if (psiClass == null) {
            return null;
        }

        PsiMethod[] methods = psiClass.findMethodsByName(targetMethodName, true);
        for (PsiMethod method : methods) {
            boolean matchResult = matches(method, methodConfig);
            if (matchResult) {
                return method;
            }
        }
        return null;
    }

    /**
     * find psi method call expression by psi method
     */
    private static PsiMethodCallExpression findCallExpression(PsiElement element) {
        while (element != null && !(element instanceof PsiMethodCallExpression)) {
            element = element.getParent();
        }
        return (PsiMethodCallExpression) element;
    }


    /**
     * return match result of given call and method config
     * @param targetMethod method find in files
     * @param methodConfig user config
     * @return true when matched or false when not
     */
    public static boolean matches(PsiMethod targetMethod, MethodConfig methodConfig) {
        if (targetMethod == null) {
            return false;
        }
        PsiClass containingClass = targetMethod.getContainingClass();
        if (containingClass == null) {
            return false;
        }
        String className = containingClass.getQualifiedName();
        if (!StringUtils.equals(className, methodConfig.getClassName())) {
            return false;
        }
        String methodName = targetMethod.getName();
        if (!StringUtils.equals(methodName, methodConfig.getMethodName())) {
            return false;
        }

        PsiParameterList psiParameterList = targetMethod.getParameterList();
        List<String> paramNameList = Arrays.stream(psiParameterList.getParameters()).map(PsiParameter::getName).toList();
        List<String> paramConfigList = methodConfig.getParamNames();

        return paramNameList.equals(paramConfigList);
    }
}
