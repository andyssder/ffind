package io.github.andyssder.ffind.cache;

import io.github.andyssder.ffind.PsiTestUtil;
import io.github.andyssder.ffind.common.ReferenceType;
import io.github.andyssder.ffind.model.idea.CopyUsageInfo;
import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;


public class FindResultCacheTest extends LightJavaCodeInsightFixtureTestCase {

    private FindResultCache cache;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        cache = FindResultCache.getInstance();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (cache != null) {
            cache.clearCache();
        }
    }

    public void testSingletonInstance() {
        FindResultCache anotherInstance = FindResultCache.getInstance();
        assertSame("Instances should be the same", cache, anotherInstance);
    }

    public void testEmptyCache() {
        PsiField testField = createTestField("TestClass", "testField");
        Optional<List<CopyUsageInfo>> result = cache.getCachedResultByField(testField);
        assertFalse("Cache should be empty", result.isPresent());
    }

    public void testCacheStorageAndRetrieval() {
        PsiField testField = createTestField("TestClass", "testField");
        List<CopyUsageInfo> testData = createTestUsageInfos(testField);
        long expireTime = 5000;

        cache.updateCache(testField, testData, expireTime);
        Optional<List<CopyUsageInfo>> result = cache.getCachedResultByField(testField);

        assertTrue("Result should be present", result.isPresent());
        assertEquals("Should return correct data size", testData.size(), result.get().size());
        for (int i = 0; i < testData.size(); i++) {
            CopyUsageInfo copyUsageInfo = testData.get(i);
            assertSame("Should return correct data", copyUsageInfo, result.get().get(i));
        }

        PsiField anotherField = createTestField("AnotherClass", "anotherField");
        Optional<List<CopyUsageInfo>> anotherResult = cache.getCachedResultByField(anotherField);
        assertFalse("Cache should be isolated by field", anotherResult.isPresent());
    }

    public void testCacheExpiration() throws InterruptedException {
        PsiField testField = createTestField("TestClass", "testField");
        List<CopyUsageInfo> testData = createTestUsageInfos(testField);
        cache.updateCache(testField, testData, 100L);

        Optional<List<CopyUsageInfo>> result = cache.getCachedResultByField(testField);
        assertTrue("Cache should be valid before expiration", result.isPresent());

        Thread.sleep(150);

        Optional<List<CopyUsageInfo>> expiredResult = cache.getCachedResultByField(testField);
        assertFalse("Expired cache should be empty", expiredResult.isPresent());
    }

    public void testCacheClear() {
        PsiField testField = createTestField("TestClass", "testField");
        List<CopyUsageInfo> testFieldData = createTestUsageInfos(testField);
        cache.updateCache(testField, testFieldData, 5000L);
        assertTrue("Test field cache should exist before clear",
                cache.getCachedResultByField(testField).isPresent());

        PsiField anotherField = createTestField("AnotherClass", "anotherField");
        List<CopyUsageInfo> anotherFieldData = createTestUsageInfos(anotherField);
        cache.updateCache(anotherField, anotherFieldData, 5000L);
        assertTrue("Another field cache should exist before clear",
                cache.getCachedResultByField(anotherField).isPresent());

        cache.clearCache();
        Optional<List<CopyUsageInfo>> result1 = cache.getCachedResultByField(testField);
        Optional<List<CopyUsageInfo>> result2 = cache.getCachedResultByField(anotherField);

        assertFalse("Test field cache should be cleared", result1.isPresent());
        assertFalse("Another field cache should be cleared", result2.isPresent());
    }

    @NotNull
    private PsiField createTestField(String className, String fieldName) {
        return PsiTestUtil.createTestFieldWithSelfReference(myFixture, className, fieldName);
    }

    @NotNull
    private List<CopyUsageInfo> createTestUsageInfos(PsiField psiField) {
        PsiClass containingClass = psiField.getContainingClass();
        assertNotNull("Field should have a containing class", containingClass);
        // one way
        PsiReference mockReference = getFieldReference(containingClass.getQualifiedName(), psiField.getName());
        // another way
//         PsiReference mockReference = createFieldReference(psiField);

        return List.of(
                new CopyUsageInfo(mockReference, ReferenceType.INDIRECT_WRITE),
                new CopyUsageInfo(mockReference, ReferenceType.INDIRECT_READ)
        );
    }

    @NotNull
    private PsiReference getFieldReference(String className, String fieldName) {
        return PsiTestUtil.getFieldReference(myFixture, className, fieldName);
    }

//    @NotNull
//    private PsiReference createFieldReference(PsiField field) {
//        return PsiTestUtil.createFieldReference(myFixture, field);
//    }

}