package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class HealthAgentProfileUpdateService {
    private final HealthAgentProfileUpdateExtractor extractor;
    private final ReHealthBusinessRepository repository;

    public HealthAgentProfileUpdateService(
            HealthAgentProfileUpdateExtractor extractor,
            ReHealthBusinessRepository repository
    ) {
        this.extractor = extractor;
        this.repository = repository;
    }

    public HealthAgentProfileUpdateResult updateFromMessage(String userId, String message) {
        HealthAgentProfilePatch patch = extractor.extract(message);
        if (patch.isEmpty()) {
            return HealthAgentProfileUpdateResult.none();
        }
        try {
            return mergeAndSave(userId, patch);
        } catch (OptimisticLockingFailureException race) {
            return mergeAndSave(userId, patch);
        }
    }

    private HealthAgentProfileUpdateResult mergeAndSave(String userId, HealthAgentProfilePatch patch) {
        PatientProfileDto existing = repository.findPatientProfile(userId).orElse(null);
        PatientProfileDto merged = copy(existing);
        List<String> changedFields = new ArrayList<>();
        merged.name = changed(existing == null ? null : existing.name, patch.name(), "姓名", changedFields);
        merged.gender = changed(existing == null ? null : existing.gender, patch.gender(), "性别", changedFields);
        merged.age = changed(existing == null ? null : existing.age, patch.age(), "年龄", changedFields);
        merged.heightCm = changed(
                existing == null ? null : existing.heightCm,
                patch.heightCm(),
                "身高",
                changedFields
        );
        merged.weightKg = changed(
                existing == null ? null : existing.weightKg,
                patch.weightKg(),
                "体重",
                changedFields
        );
        if (changedFields.isEmpty()) {
            return HealthAgentProfileUpdateResult.none();
        }
        repository.savePatientProfile(userId, merged);
        return new HealthAgentProfileUpdateResult(List.copyOf(changedFields));
    }

    private <T> T changed(T existing, T incoming, String label, List<String> changedFields) {
        if (incoming == null) {
            return existing;
        }
        if (!Objects.equals(existing, incoming)) {
            changedFields.add(label);
        }
        return incoming;
    }

    private PatientProfileDto copy(PatientProfileDto existing) {
        PatientProfileDto copy = new PatientProfileDto();
        if (existing == null) {
            return copy;
        }
        copy.name = existing.name;
        copy.gender = existing.gender;
        copy.age = existing.age;
        copy.heightCm = existing.heightCm;
        copy.weightKg = existing.weightKg;
        copy.diagnoses = existing.diagnoses;
        copy.medications = existing.medications;
        copy.allergies = existing.allergies;
        copy.familyHistory = existing.familyHistory;
        copy.smoking = existing.smoking;
        copy.drinking = existing.drinking;
        copy.diabetesHistory = existing.diabetesHistory;
        copy.hypertensionHistory = existing.hypertensionHistory;
        copy.version = existing.version;
        return copy;
    }
}

record HealthAgentProfileUpdateResult(List<String> changedFields) {
    static HealthAgentProfileUpdateResult none() {
        return new HealthAgentProfileUpdateResult(List.of());
    }

    boolean changed() {
        return !changedFields.isEmpty();
    }
}
