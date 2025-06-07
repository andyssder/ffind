package io.github.andyssder.ffind.detector;

import io.github.andyssder.ffind.model.MethodConfig;
import com.intellij.psi.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MethodSignatureMatcherTest {

    @Test
    public void shouldReturnTrueWhenAllConditionsMatch() {
        PsiMethod method = createMockMethod("com.example.MyService", "processData", "arg1", "arg2");
        MethodConfig config = createMockMethodConfig("com.example.MyService", "processData", List.of("arg1", "arg2"));
        assertTrue(MethodSignatureMatcher.matches(method, config));
    }

    @Test
    public void shouldReturnTrueWhenHasVariableArgument() {
        MethodConfig config = createMockMethodConfig("com.example.MyService", "processData", List.of("arg1", "arg2", "agr3"), true, false);

        PsiMethod method = createMockMethod("com.example.MyService", "processData", "arg1", "arg2", "agr3");
        assertTrue(MethodSignatureMatcher.matches(method, config));

        method = createMockMethod("com.example.MyService", "processData", "arg1", "arg2", "agr3", "arg4", "agr5");
        assertFalse(MethodSignatureMatcher.matches(method, config));

        method = createMockMethod("com.example.MyService", "processData", "arg1", "arg2");
        assertFalse(MethodSignatureMatcher.matches(method, config));

        method = createMockMethod("com.example.MyService", "processData");
        assertFalse(MethodSignatureMatcher.matches(method, config));

    }

    @Test
    public void shouldReturnFalseWhenMethodCannotResolve() {
        MethodConfig methodConfig = createMockMethodConfig("any", "any", List.of());
        assertFalse(MethodSignatureMatcher.matches(null, methodConfig));
    }

    @Test
    public void shouldReturnFalseWhenNotMatch() {
        PsiMethod method = createMockMethod("com.example.MyService", "save", "arg1", "arg2", "arg3", "arg4");
        MethodConfig config = createMockMethodConfig("com.example.MyService", "save", Arrays.asList("arg1", "arg2", "agr3"));
        assertFalse(MethodSignatureMatcher.matches(method, config));
    }

    @Test
    public void shouldReturnFalseWhenContainingClassIsNull() {
        MethodConfig methodConfig = createMockMethodConfig("any", "any", List.of());
        PsiMethod method = createMockMethod(null, "any");
        assertFalse(MethodSignatureMatcher.matches(method, methodConfig));
    }

    @Test
    public void shouldReturnFalseWhenQualifiedNameIsBlank() {
        MethodConfig methodConfig = createMockMethodConfig("any", "any", List.of());
        PsiMethod method = createMockMethod("", "any");
        assertFalse(MethodSignatureMatcher.matches(method, methodConfig));
    }

    @Test
    public void shouldHandleNullParamNames() {
        PsiMethod method = createMockMethod("com.example.MyService", "save", null, "arg2");
        MethodConfig config = createMockMethodConfig("com.example.MyService", "save", Arrays.asList(null, "arg2"));
        assertTrue(MethodSignatureMatcher.matches(method, config));
    }

//    @ParameterizedTest
//    @MethodSource("mismatchScenarios")
//    void shouldReturnFalseWhenAnyConditionFails(String className, String methodName, List<String> params, boolean expected) {
//        PsiMethod method = createMockMethod("com.example.MyService", "processData", "arg1", "arg2");
//        MethodConfig config = createMockMethodConfig(className, methodName, params);
//        assertEquals(expected, MethodSignatureMatcher.matches(method, config));
//    }
//
//    static Stream<Object[]> mismatchScenarios() {
//        return Stream.of(
//                new Object[]{
//                        "com.example.WrongClass", "processData", List.of("arg1", "arg2"), false
//                },
//                new Object[]{
//                        "com.example.MyService", "wrongMethod", List.of("arg1", "arg2"), false
//                },
//                new Object[]{
//                        "com.example.MyService", "processData", List.of("arg2", "arg1"), false
//                },
//                new Object[]{
//                        "com.example.MyService", "processData", List.of("arg1"), false
//                }
//        );
//    }

    private PsiMethod createMockMethod(String className, String methodName, String... paramNames) {
        PsiMethod method = mock(PsiMethod.class);
        PsiClass containingClass = mock(PsiClass.class);
        when(method.getContainingClass()).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(className);
        when(method.getName()).thenReturn(methodName);
        PsiParameterList paramList = createMockPsiParameterList(paramNames);
        doReturn(paramList).when(method).getParameterList();
        return method;
    }

    private MethodConfig createMockMethodConfig(String className, String methodName, List<String> params) {
        return createMockMethodConfig(className, methodName, params, false, false);
    }

    private MethodConfig createMockMethodConfig(String className, String methodName, List<String> params,
                                                boolean excludeFiledParamEnable, boolean includeFieldParamEnable) {
        return new MethodConfig(className, methodName, params,
                0, 1, excludeFiledParamEnable, includeFieldParamEnable);
    }

    private PsiParameterList createMockPsiParameterList(String... paramNames) {
        PsiParameterList paramList = mock(PsiParameterList.class);
        PsiParameter[] parameters = Arrays.stream(paramNames)
                .map(name -> {
                    PsiParameter param = mock(PsiParameter.class);
                    when(param.getName()).thenReturn(name);
                    return param;
                }).toArray(PsiParameter[]::new);
        when(paramList.getParameters()).thenReturn(parameters);
        return paramList;
    }
}
