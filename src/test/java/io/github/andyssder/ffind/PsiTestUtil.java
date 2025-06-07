package io.github.andyssder.ffind;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.jetbrains.annotations.NotNull;

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
    public static  PsiReference createFieldReference(JavaCodeInsightTestFixture myFixture, PsiField field) {
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
}
