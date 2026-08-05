package org.jeecg.modules.rehealth.mobile.controller;

import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RhiManualHealthInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ReHealthMobileControllerProfileContractTest {
    @Test
    void exposesAuthenticatedProfileAndInterviewRoutes() throws Exception {
        Method getProfile = ReHealthMobileController.class.getMethod("profile");
        Method putProfile = ReHealthMobileController.class.getMethod("updateProfile", PatientProfileDto.class);
        Method getInterview = ReHealthMobileController.class.getMethod("latestInterview");
        Method postInterview = ReHealthMobileController.class.getMethod(
                "submitInterview",
                HealthInterviewSubmitRequestDto.class
        );
        Method getManualInput = ReHealthMobileController.class.getMethod("rhiManualHealthInput");
        Method putManualInput = ReHealthMobileController.class.getMethod(
                "updateRhiManualHealthInput",
                RhiManualHealthInputDto.class
        );

        assertArrayEquals(new String[]{"/profile"}, getProfile.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/profile"}, putProfile.getAnnotation(PutMapping.class).value());
        assertArrayEquals(new String[]{"/interviews/latest"}, getInterview.getAnnotation(GetMapping.class).value());
        assertArrayEquals(new String[]{"/interviews"}, postInterview.getAnnotation(PostMapping.class).value());
        assertArrayEquals(
                new String[]{"/rhi/manual-inputs"},
                getManualInput.getAnnotation(GetMapping.class).value()
        );
        assertArrayEquals(
                new String[]{"/rhi/manual-inputs"},
                putManualInput.getAnnotation(PutMapping.class).value()
        );
    }
}
