package org.jeecg.modules.rehealth.mobile.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BehaviorRecordControllerContractTest {
    @Test
    void exposesAuthenticatedPhotoAnalysisAndTodayRoutes() throws Exception {
        Method analyze = BehaviorRecordController.class.getMethod(
                "analyzePhoto", String.class, String.class, long.class, MultipartFile.class
        );
        Method today = BehaviorRecordController.class.getMethod(
                "today", String.class, java.time.LocalDate.class, int.class
        );

        assertArrayEquals(new String[]{"/analyze-photo"}, analyze.getAnnotation(PostMapping.class).value());
        assertArrayEquals(new String[]{"/today"}, today.getAnnotation(GetMapping.class).value());
    }
}
