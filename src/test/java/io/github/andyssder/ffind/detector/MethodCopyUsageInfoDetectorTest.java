package io.github.andyssder.ffind.detector;

import com.intellij.openapi.application.Application;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import io.github.andyssder.ffind.PsiTestUtil;
import io.github.andyssder.ffind.cache.FindResultCache;
import io.github.andyssder.ffind.common.ReferenceType;
import io.github.andyssder.ffind.model.GeneralConfig;
import io.github.andyssder.ffind.model.MethodConfig;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import io.github.andyssder.ffind.model.state.GeneralSetting;
import io.github.andyssder.ffind.model.state.MethodConfigSetting;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.openapi.application.ApplicationManager.getApplication;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodCopyUsageInfoDetectorTest extends LightJavaCodeInsightFixtureTestCase {

    private MethodCopyUsageInfoDetector detector;
    private FindResultCache cache;
    private GeneralSetting generalSetting;
    private MethodConfigSetting methodConfigSetting;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        detector = new MethodCopyUsageInfoDetector();
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
        PsiField field = createTestField("TestFieldClass", "testField");
        assertFalse("Should be disabled for PsiField", detector.isEnable(field));

        PsiMethod commonMethod = createCommonMethod("TestMethodClass", "testMethod");
        assertFalse("Should be disabled for no setter/getter method", detector.isEnable(commonMethod));

        PsiMethod setterMethod = createSetterOrGetterMethodForField(field, "set");
        assertTrue("Should be enable for setter method", detector.isEnable(setterMethod));

        PsiMethod getterMethod = createSetterOrGetterMethodForField(field, "get");
        assertTrue("Should be enable for getter method", detector.isEnable(getterMethod));
    }

    public void testFindCopyUsageInfoWithCacheHit() {
        PsiField field = createTestField("TestClass", "testField");
        PsiMethod setterMethod = createSetterOrGetterMethodForField(field, "set");

        PsiField anotherField = createTestField("AnotherTestClass", "anotherField");
        createSetterOrGetterMethodForField(field, "set");
        List<CopyUsageInfo> anotherMethodUsages = createMockUsageInfos(anotherField);

        String cacheKey = generateKeyForMethod(setterMethod);
        cache.updateCacheResult(cacheKey, anotherMethodUsages, 5000L);

        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setCacheEnable(true);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        List<CopyUsageInfo> result = detector.findCopyUsageInfo(setterMethod, null);
        assertEquals("Should return cached data", anotherMethodUsages, result);

        generalConfig.setCacheEnable(false);
        when(generalSetting.getGeneralConfig()).thenReturn(generalConfig);

        result = detector.findCopyUsageInfo(setterMethod, null);
        assertEmpty("Should return empty data", result);
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
        PsiMethod testSetMethod = createSetterOrGetterMethodForField(testField, "set");
        PsiMethod testGetMethod = createSetterOrGetterMethodForField(testField, "get");

        CopyUsageInfo readCopyUsageInfo = createRealUsageInfos("ReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig);
        CopyUsageInfo writeCopyUsageInfo = createRealUsageInfos("WriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig);

        List<CopyUsageInfo> expected = new ArrayList<>();
        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testSetMethod,  null);
        expected.add(writeCopyUsageInfo);
        assertEquals("Should return same usage infos", expected, actual);

        actual = detector.findCopyUsageInfo(testGetMethod,  null);
        expected.clear();
        expected.add(readCopyUsageInfo);
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
        PsiMethod testSetMethod = createSetterOrGetterMethodForField(testField, "set");
        PsiMethod testGetMethod = createSetterOrGetterMethodForField(testField, "get");

        CopyUsageInfo readCopyUsageInfo = createRealUsageInfos("ReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig);
        CopyUsageInfo writeCopyUsageInfo = createRealUsageInfos("WriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig);

        List<CopyUsageInfo> expected = new ArrayList<>();
        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testSetMethod,  null);
        expected.add(writeCopyUsageInfo);
        assertEquals("Should return same usage infos", expected, actual);

        actual = detector.findCopyUsageInfo(testGetMethod,  null);
        expected.clear();
        expected.add(readCopyUsageInfo);
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
        PsiMethod testSetMethod = createSetterOrGetterMethodForField(testField, "set");
        PsiMethod testGetMethod = createSetterOrGetterMethodForField(testField, "get");
        CopyUsageInfo readCopyUsageInfo = createRealUsageInfos("ReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig, "excludeField");
        CopyUsageInfo writeCopyUsageInfo = createRealUsageInfos("WriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig, "excludeField");

        List<CopyUsageInfo> expected = new ArrayList<>();
        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testSetMethod,  null);
        expected.add(writeCopyUsageInfo);
        assertEquals("Should return same usage infos", expected, actual);
        actual = detector.findCopyUsageInfo(testGetMethod,  null);
        expected.clear();
        expected.add(readCopyUsageInfo);
        assertEquals("Should return same usage infos", expected, actual);

        expected.clear();

        PsiField excludeField = createTestField("ExcludeClass", "excludeField");
        PsiMethod excludeFieldSetMethod = createSetterOrGetterMethodForField(excludeField, "set");
        PsiMethod excludeFieldGetMethod = createSetterOrGetterMethodForField(excludeField, "get");
        createRealUsageInfos("ExcludeReadReferenceClass", excludeField, ReferenceType.INDIRECT_READ, methodConfig, "excludeField");
        createRealUsageInfos("ExcludeWriteReferenceClass", excludeField, ReferenceType.INDIRECT_WRITE, methodConfig, "excludeField");
        actual = detector.findCopyUsageInfo(excludeFieldSetMethod,  null);
        assertEmpty("Should return empty usage infos", actual);
        actual = detector.findCopyUsageInfo(excludeFieldGetMethod,  null);
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

        PsiField testField = createTestField("IncludeClass", "includeField");
        PsiMethod testSetMethod = createSetterOrGetterMethodForField(testField, "set");
        PsiMethod testGetMethod = createSetterOrGetterMethodForField(testField, "get");
        CopyUsageInfo readCopyUsageInfo = createRealUsageInfos("IncludeReadReferenceClass", testField, ReferenceType.INDIRECT_READ, methodConfig, "includeField");
        CopyUsageInfo writeCopyUsageInfo = createRealUsageInfos("IncludeWriteReferenceClass", testField, ReferenceType.INDIRECT_WRITE, methodConfig, "includeField");
        List<CopyUsageInfo> expected = new ArrayList<>();
        List<CopyUsageInfo> actual = detector.findCopyUsageInfo(testSetMethod,  null);
        expected.add(writeCopyUsageInfo);
        assertEquals("Should return same usage infos", expected, actual);
        actual = detector.findCopyUsageInfo(testGetMethod,  null);
        expected.clear();
        expected.add(readCopyUsageInfo);
        assertEquals("Should return same usage infos", expected, actual);

        PsiField excludeField = createTestField("TestClass", "testField");
        PsiMethod excludeFieldSetMethod = createSetterOrGetterMethodForField(excludeField, "set");
        PsiMethod excludeFieldGetMethod = createSetterOrGetterMethodForField(excludeField, "get");
        createRealUsageInfos("ExcludeReadReferenceClass", excludeField, ReferenceType.INDIRECT_READ, methodConfig, "includeField");
        createRealUsageInfos("ExcludeWriteReferenceClass", excludeField, ReferenceType.INDIRECT_WRITE, methodConfig, "includeField");
        actual = detector.findCopyUsageInfo(excludeFieldSetMethod,  null);
        assertEmpty("Should return empty usage infos", actual);
        actual = detector.findCopyUsageInfo(excludeFieldGetMethod,  null);
        assertEmpty("Should return empty usage infos", actual);

    }

    /****************************private method****************************/


    @NotNull
    private PsiField createTestField(String className, String fieldName) {
        return PsiTestUtil.createTestFieldWithSelfReference(myFixture, className, fieldName);
    }

    @NotNull
    private PsiMethod createCommonMethod(String className, String methodName) {
        return PsiTestUtil.createTestMethod(myFixture, className, methodName);
    }

    @NotNull
    private PsiMethod createSetterOrGetterMethodForField(PsiField field, String methodType) {
        return PsiTestUtil.createSetterOrGetterMethodForField(field, methodType);
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
        return PsiTestUtil.createRealUsageInfos(myFixture, mockClassName, testField, referenceType, methodConfig, strings);
    }

    private void createCopyClasses(List<MethodConfig> methodConfigs) {
        PsiTestUtil.createCopyClasses(myFixture, methodConfigs);
    }

    private String generateKeyForMethod(PsiMethod paramMethod) {
        try {
            Method method = MethodCopyUsageInfoDetector.class.getDeclaredMethod("generateKeyForMethod", PsiMethod.class);
            method.setAccessible(true);
            return (String) method.invoke(detector, paramMethod);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke generateKeyForField", e);
        }
    }
}