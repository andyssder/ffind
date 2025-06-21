package io.github.andyssder.ffind;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import io.github.andyssder.ffind.common.ReferenceType;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import io.github.andyssder.ffind.model.idea.MethodCallReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PsiTestUtil {

    @NotNull
    public static PsiField createTestField(JavaCodeInsightTestFixture myFixture, String className, String fieldName) {
        myFixture.configureByText(className + ".java",
                "public class " + className + " {\n" +
                        "   private String " + fieldName + ";\n" +
                        "}"
        );
        PsiClass psiClass = myFixture.findClass(className);
        assertNotNull(psiClass);
        PsiField psiField = psiClass.findFieldByName(fieldName, false);
        assertNotNull(psiField);
        return psiField;
    }

    @NotNull
    public static PsiReference createFieldReference(JavaCodeInsightTestFixture myFixture, PsiField field) {
        PsiFile containingFile = field.getContainingFile();
        assertNotNull("Field must be part of a PSI file", containingFile);

        String fieldName = field.getName();
        assertNotNull("Field must have a name", fieldName);

        PsiClass containingClass = field.getContainingClass();
        assertNotNull("Field must belong to a class", containingClass);

        String className = containingClass.getName();
        String methodText = "public void useField() { " + className + " ref = this; ref." + fieldName + " = null; }";

        Project project = field.getProject();
        return WriteCommandAction.writeCommandAction(project).compute(() -> {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            PsiMethod method = factory.createMethodFromText(methodText, containingClass);

            PsiMethod addedMethod = (PsiMethod) containingClass.add(method);

            PsiCodeBlock body = addedMethod.getBody();
            assertNotNull("Method body is null", body);

            PsiStatement[] statements = body.getStatements();
            assertFalse("Failed to create field usage statement", statements.length < 1);

            assertTrue("Expected expression statement", statements[1] instanceof PsiExpressionStatement);

            PsiExpression expression = ((PsiExpressionStatement) statements[1]).getExpression();
            assertTrue("Expected assignment expression", expression instanceof PsiAssignmentExpression);

            PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
            PsiExpression lExpression = assignment.getLExpression();
            assertTrue("Expected reference expression", lExpression instanceof PsiReferenceExpression);

            return lExpression.getReference();

        });
    }

    @NotNull
    public static PsiField createTestFieldWithSelfReference(JavaCodeInsightTestFixture myFixture, String className, String fieldName) {
        myFixture.configureByText(className + ".java",
                "public class " + className + " {\n" +
                        "   private String " + fieldName + ";\n" +
                        "   \n" +
                        "   public void useField() {\n" +
                        "       this." + fieldName + " = \"test\";\n" +
                        "   }\n" +
                        "}"
        );

        PsiClass testClass = myFixture.findClass(className);
        assertNotNull(testClass);

        PsiField testField =  testClass.findFieldByName(fieldName, false);
        assertNotNull(testField);

        return testField;
    }

    @NotNull
    public static PsiReference getFieldReference(JavaCodeInsightTestFixture myFixture, String className, String fieldName) {
        PsiClass psiClass = myFixture.findClass(className);
        assertNotNull(className + " should exist", psiClass);

        PsiMethod[] methods = psiClass.findMethodsByName("useField", false);
        assertNotEquals("useField method not found in " + className, 0, methods.length);

        PsiMethod useFieldMethod = methods[0];
        PsiCodeBlock body = useFieldMethod.getBody();
        assertNotNull("useField method body is null", body);

        PsiReference psiReference = null;
        for (PsiReferenceExpression ref : PsiTreeUtil.findChildrenOfType(body, PsiReferenceExpression.class)) {
            if (fieldName.equals(ref.getReferenceName()) && ref.resolve() instanceof PsiField) {
                psiReference = ref.getReference();
                break;
            }
        }
        assertNotNull("field find no reference", psiReference);
        return psiReference;
    }

    @NotNull
    public static PsiMethod createTestMethod(JavaCodeInsightTestFixture myFixture, String className, String methodName) {
        myFixture.configureByText(className + ".java",
                "public class " + className + " {\n" +
                        "   public void " + methodName + "() {}\n" +
                        "}"
        );
        PsiClass psiClass = myFixture.findClass(className);
        assertNotNull(psiClass);
        PsiMethod[] psiMethods = psiClass.findMethodsByName(methodName, false);
        assertNotEquals(0, psiMethods.length);
        PsiMethod psiMethod =  psiClass.findMethodsByName(methodName, false)[0];
        assertNotNull(psiMethod);
        return psiMethod;
    }


    @NotNull
    public static CopyUsageInfo createRealUsageInfos(JavaCodeInsightTestFixture myFixture, String mockClassName, PsiField testField, ReferenceType referenceType, MethodConfig methodConfig, String... strings) {

        assertTrue(ReferenceType.INDIRECT_READ.equals(referenceType) || ReferenceType.INDIRECT_WRITE.equals(referenceType));

        PsiClass testClass = testField.getContainingClass();
        assertNotNull(testClass);
        String testClassName = testClass.getQualifiedName();
        String testClassShortName = testClass.getName();

        StringBuilder classBuilder = new StringBuilder();
        classBuilder.append("import ").append(testClassName).append(";\n");
        classBuilder.append("import ").append(methodConfig.getClassName()).append(";\n\n");
        classBuilder.append("public class ").append(mockClassName).append(" {\n");
        classBuilder.append("   public void testMethod() {\n");
        classBuilder.append("       ").append(testClassShortName).append(" test = new ").append(testClassShortName).append("();\n");

        StringBuilder methodCallBuilder = new StringBuilder();
        methodCallBuilder.append(methodConfig.getClassName()).append(".").append(methodConfig.getMethodName()).append("(");

        for (int i = 0; i < methodConfig.getParamNames().size(); i++) {
            boolean isExcludeOrIncludeFieldEnable = methodConfig.getExcludeFiledParamEnable() || methodConfig.getIncludeFieldParamEnable();
            boolean isLastIndex = i == methodConfig.getParamNames().size() - 1;

            if (methodConfig.getSourceParamIndex() == i && ReferenceType.INDIRECT_READ.equals(referenceType)) {
                methodCallBuilder.append("test");
            } else if (methodConfig.getTargetParamIndex() == i && ReferenceType.INDIRECT_WRITE.equals(referenceType)) {
                methodCallBuilder.append("test");
            } else if (isExcludeOrIncludeFieldEnable && isLastIndex){
                if (strings != null && strings.length != 0) {
                    for (String item : strings) {
                        methodCallBuilder.append("\"").append(item).append("\"").append(",");
                    }
                    // add empty string so that idea can identify variable argument, don't know why
                    methodCallBuilder.append("\"\"");
                }
            } else {
                methodCallBuilder.append("null");
            }
            methodCallBuilder.append(isLastIndex? ");\n" : ",");
        }
        classBuilder.append(methodCallBuilder).append("   }\n").append("}");

        myFixture.configureByText(mockClassName + ".java", classBuilder.toString());
        myFixture.doHighlighting();

        // 查找方法调用表达式
        PsiMethodCallExpression methodCallExpression = myFixture.findElementByText(methodCallBuilder.toString(), PsiMethodCallExpression.class);

        assertNotNull(methodCallExpression.resolveMethod());

        return new CopyUsageInfo(new MethodCallReference(methodCallExpression, testField), referenceType);
    }


    public static void createCopyClasses(JavaCodeInsightTestFixture myFixture, List<MethodConfig> methodConfigs) {
        if (methodConfigs == null || methodConfigs.isEmpty()) {
            return;
        }

        Map<String, List<MethodConfig>> classMethodMap = new HashMap<>();
        for (MethodConfig config : methodConfigs) {
            String className = config.getClassName();
            classMethodMap.computeIfAbsent(className, k -> new ArrayList<>()).add(config);
        }

        for (Map.Entry<String, List<MethodConfig>> entry : classMethodMap.entrySet()) {
            String className = entry.getKey();
            List<MethodConfig> classConfigs = entry.getValue();

            StringBuilder classBuilder = new StringBuilder();
            classBuilder.append("public class ").append(className).append(" {\n");

            for (MethodConfig config : classConfigs) {
                classBuilder.append(generateCopyMethodSignature(config)).append(" {}\n");
            }

            classBuilder.append("}");

            myFixture.configureByText(className + ".java", classBuilder.toString());
            myFixture.doHighlighting();
        }
    }

    private static String generateCopyMethodSignature(MethodConfig config) {
        StringBuilder signature = new StringBuilder();
        signature.append("public static void ")
                .append(config.getMethodName())
                .append("(");

        List<String> params = new ArrayList<>();
        for (int i = 0; i < config.getParamNames().size(); i++) {
            String paramType = "Object";

            boolean isExcludeOrIncludeFieldEnable = config.getExcludeFiledParamEnable() || config.getIncludeFieldParamEnable();
            if (isExcludeOrIncludeFieldEnable && i == config.getParamNames().size() - 1) {
                // the last one is variable argument
                paramType = "String...";
            }

            params.add(paramType + " " + config.getParamNames().get(i));
        }

        signature.append(String.join(", ", params));
        signature.append(")");

        return signature.toString();
    }

    @NotNull
    public static PsiMethod createSetterOrGetterMethodForField(PsiField field, String methodType) {
        PsiClass psiClass = field.getContainingClass();
        assertNotNull(psiClass);

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(field.getProject());

        String fieldName = field.getName();
        String methodName = methodType + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String methodText;
        if ("set".equals(methodType)) {
            methodText = "public void " + methodName + "(" + field.getType().getCanonicalText() + " " + fieldName + ") {\n" +
                    "    this." + fieldName + " = " + fieldName + ";\n" +
                    "}";
        } else {
            methodText = "public " + field.getType().getCanonicalText() + " " + methodName + "() {\n" +
                    "    return this." + fieldName + ";\n" +
                    "}";
        }

        return WriteCommandAction.writeCommandAction(field.getProject()).compute(() -> {
            PsiMethod method = factory.createMethodFromText(methodText, psiClass);

            PsiElement addedMethod = psiClass.addAfter(method, field);

            if (addedMethod instanceof PsiMethod) {
                return (PsiMethod) addedMethod;
            } else {
                // 如果类型不匹配，尝试查找新添加的方法
                return PsiTreeUtil.findChildOfType(psiClass, PsiMethod.class, true);
            }
        });
    }
}
