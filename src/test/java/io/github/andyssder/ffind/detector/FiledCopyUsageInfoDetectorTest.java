package io.github.andyssder.ffind.detector;

import io.github.andyssder.ffind.PsiTestUtil;
import io.github.andyssder.ffind.cache.FindResultCache;
import io.github.andyssder.ffind.common.ReferenceType;
import io.github.andyssder.ffind.model.GeneralConfig;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import io.github.andyssder.ffind.model.idea.MethodCallReference;
import io.github.andyssder.ffind.model.state.GeneralSetting;
import io.github.andyssder.ffind.model.state.MethodConfigSetting;
import com.intellij.openapi.application.Application;
import com.intellij.psi.*;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static org.mockito.Mockito.*;

public class FiledCopyUsageInfoDetectorTest extends LightJavaCodeInsightFixtureTestCase {

    private FiledCopyUsageInfoDetector detector;
    private FindResultCache cache;
    private GeneralSetting generalSetting;
    private MethodConfigSetting methodConfigSetting;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        detector = new FiledCopyUsageInfoDetector();
        cache = FindResultCache.getInstance();

        generalSetting = mock(GeneralSetting.class);
        methodConfigSetting = mock(MethodConfigSetting.class);

        // replace with mock setting
        Application application = getApplication();
        ServiceContainerUtil.replaceService(application, GeneralSetting.class, generalSetting, getTestRootDisposable());
        ServiceContainerUtil.replaceService(application, MethodConfigSetting.class, methodConfigSetting, getTestRootDisposable());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (cache != null) {
            cache.clearCache();
        }
    }

    public void testIsEnable() {
        PsiField field = createTestField("TestClass", "testField");
        assertTrue("Should be enabled for PsiField", detector.isEnable(field));

        PsiMethod method = createTestMethod("TestClass", "testMethod");
        assertFalse("Should be disabled for non-PsiField", detector.isEnable(method));
    }

    public void testFindCopyUsageInfoWhenNotField() {
        PsiMethod method = createTestMethod("TestClass", "testMethod");
        List<CopyUsageInfo> result = detector.findCopyUsageInfo(method, null);
        assertTrue("Should return empty list for non-field", result.isEmpty());
    }

    public void testFindCopyUsageInfoWithCacheHit() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(true);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        PsiField field = createTestField("TestClass", "testField");
        List<CopyUsageInfo> cachedData = createMockUsageInfos(field);

        cache.updateCache(field, cachedData, 5000L);

        List<CopyUsageInfo> result = detector.findCopyUsageInfo(field, null);
        assertEquals("Should return cached data", cachedData, result);
    }

    public void testFindCopyUsageInfoWithNormalCopyMethod() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(false);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        MethodConfig methodConfig = new MethodConfig(
                "BeanUtils","copyProperties", List.of("source", "target"), 0, 1, false, false);
        List<MethodConfig> methodConfigList = List.of(methodConfig);
        createCopyClasses(methodConfigList);
        when(methodConfigSetting.getMethodConfigs()).thenReturn(methodConfigList);

        PsiField testField = createTestField("TestClass", "testField");
        List<CopyUsageInfo> expected = new ArrayList<>();
        expected.add(createRealUsageInfos("ReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig));
        expected.add(createRealUsageInfos("WriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig));

        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testField,  null);
        assertEquals("Should return same usage infos", expected, actual);
    }

    public void testFindCopyUsageInfoWithCustomCopyMethod() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(false);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        MethodConfig methodConfig = new MethodConfig(
                "BeanUtils","copyProperties", List.of("param1", "param2", "param3", "target", "source"), 4, 3, false, false);

        List<MethodConfig> methodConfigList = List.of(methodConfig);
        createCopyClasses(methodConfigList);
        when(methodConfigSetting.getMethodConfigs()).thenReturn(methodConfigList);

        PsiField testField = createTestField("TestClass", "testField");
        List<CopyUsageInfo> expected = new ArrayList<>();
        expected.add(createRealUsageInfos("ReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig));
        expected.add(createRealUsageInfos("WriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig));

        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testField,  null);
        assertEquals("Should return same usage infos", expected, actual);
    }

    public void testProcessMatchedCallWithExcludeCondition() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(false);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        MethodConfig methodConfig = new MethodConfig(
                "BeanUtils","copyProperties", List.of("source", "target", "fields"), 0, 1, true, false);
        List<MethodConfig> methodConfigList = List.of(methodConfig);
        createCopyClasses(methodConfigList);
        when(methodConfigSetting.getMethodConfigs()).thenReturn(methodConfigList);

        PsiField testField = createTestField("TestClass", "testField");
        List<CopyUsageInfo> expected = new ArrayList<>();
        expected.add(createRealUsageInfos("ReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig, "excludeField"));
        expected.add(createRealUsageInfos("WriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig, "excludeField"));
        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testField,  null);
        assertTrue("Should return same usage infos",  CollectionUtils.isEqualCollection(expected, actual));

        PsiField excludeField = createTestField("ExcludeClass", "excludeField");
        createRealUsageInfos("ExcludeReadReferenceClass", excludeField, ReferenceType.INDIRECT_READ, methodConfig, "excludeField");
        createRealUsageInfos("ExcludeWriteReferenceClass", excludeField, ReferenceType.INDIRECT_WRITE, methodConfig, "excludeField");
        actual = detector.findCopyUsageInfo(excludeField,  null);
        assertEmpty("Should return empty usage infos", actual);
    }

    public void testProcessMatchedCallWithIncludeCondition() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(false);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        MethodConfig methodConfig = new MethodConfig(
                "BeanUtils","copyProperties", List.of("source", "target", "fields"), 0, 1, false, true);
        List<MethodConfig> methodConfigList = List.of(methodConfig);
        createCopyClasses(methodConfigList);
        when(methodConfigSetting.getMethodConfigs()).thenReturn(methodConfigList);

        PsiField excludeField = createTestField("TestClass", "testField");
        createRealUsageInfos("ReadReferenceClass", excludeField, ReferenceType.INDIRECT_READ, methodConfig, "includeField");
        createRealUsageInfos("WriteReferenceClass", excludeField, ReferenceType.INDIRECT_WRITE, methodConfig, "includeField");
        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(excludeField,  null);
        assertEmpty("Should return empty usage infos", actual);

        PsiField testField = createTestField("IncludeClass", "includeField");
        List<CopyUsageInfo> expected = new ArrayList<>();
        expected.add(createRealUsageInfos("IncludeReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig, "includeField"));
        expected.add(createRealUsageInfos("IncludeWriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig, "includeField"));
        actual = detector.findCopyUsageInfo(testField,  null);
        assertTrue("Should return same usage infos",  CollectionUtils.isEqualCollection(expected, actual));

    }

    @NotNull
    private PsiField createTestField(String className, String fieldName) {
        return PsiTestUtil.createTestFieldWithSelfReference(myFixture, className, fieldName);
    }

    @NotNull
    private PsiMethod createTestMethod(String className, String methodName) {
        return PsiTestUtil.createTestMethod(myFixture, className, methodName);
    }

    @NotNull
    private List<CopyUsageInfo> createMockUsageInfos(PsiField psiField) {
        PsiClass containingClass = psiField.getContainingClass();
        assertNotNull("Field should have a containing class", containingClass);
        PsiReference mockReference = PsiTestUtil.getFieldReference(myFixture, containingClass.getQualifiedName(), psiField.getName());

        return List.of(new CopyUsageInfo(mockReference, ReferenceType.INDIRECT_READ));
    }

    @NotNull
    private CopyUsageInfo createRealUsageInfos(String mockClassName, PsiField testField, ReferenceType referenceType, MethodConfig methodConfig, String... strings) {

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
                    // add a empty string so that idea can identify variable argument, don't know why
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

    private void createCopyClasses(List<MethodConfig> methodConfigs) {
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

    private String generateCopyMethodSignature(MethodConfig config) {
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

}