package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthAgentProfileUpdateServiceTest {
    @Test
    void mergesBasicFieldsWithoutClearingClinicalProfile() {
        StubReHealthBusinessRepository repository = new StubReHealthBusinessRepository();
        PatientProfileDto existing = new PatientProfileDto();
        existing.name = "旧姓名";
        existing.age = 30;
        existing.diagnoses = List.of("高血压");
        existing.medications = List.of("既有用药");
        existing.allergies = List.of("花粉");
        existing.smoking = true;
        existing.version = 7L;
        repository.profile = existing;
        HealthAgentProfileUpdateService service = new HealthAgentProfileUpdateService(
                new HealthAgentProfileUpdateExtractor(),
                repository
        );

        HealthAgentProfileUpdateResult result = service.updateFromMessage(
                "user-a",
                "姓名：新姓名，年龄：36岁，身高：168cm，体重：60kg"
        );

        assertEquals(List.of("姓名", "年龄", "身高", "体重"), result.changedFields());
        assertEquals("新姓名", repository.profile.name);
        assertEquals(36, repository.profile.age);
        assertEquals(168.0, repository.profile.heightCm);
        assertEquals(60.0, repository.profile.weightKg);
        assertEquals(List.of("高血压"), repository.profile.diagnoses);
        assertEquals(List.of("既有用药"), repository.profile.medications);
        assertEquals(List.of("花粉"), repository.profile.allergies);
        assertEquals(true, repository.profile.smoking);
        assertEquals(7L, repository.profile.version);
        assertEquals(1, repository.profileSaveCount);
    }

    @Test
    void doesNotWriteWhenSubmittedValuesAreUnchanged() {
        StubReHealthBusinessRepository repository = new StubReHealthBusinessRepository();
        repository.profile = new PatientProfileDto();
        repository.profile.age = 36;
        HealthAgentProfileUpdateService service = new HealthAgentProfileUpdateService(
                new HealthAgentProfileUpdateExtractor(),
                repository
        );

        HealthAgentProfileUpdateResult result = service.updateFromMessage("user-a", "我今年36岁。 ");

        assertEquals(List.of(), result.changedFields());
        assertEquals(0, repository.profileSaveCount);
    }
}
