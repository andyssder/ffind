package io.github.andyssder.ffind.detector;

import com.intellij.psi.PsiElement;

import java.util.Arrays;
import java.util.List;

public class CopyUsageInfoDetectorFactory {

    // init detectors
    private static final List<CopyUsageInfoDetector> detectors = Arrays.asList(
            new FiledCopyUsageInfoDetector()
    );

    /**
     * return detector by element type
     * @param element the element which user wants to find
     * @return detector which help find in copy method or null
     */
    public static CopyUsageInfoDetector getDetector(PsiElement element) {
        return detectors.stream()
                .filter(detector -> detector.isEnable(element))
                .findFirst()
                .orElse(null);
    }
}
