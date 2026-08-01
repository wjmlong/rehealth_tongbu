package org.jeecg.modules.rehealth.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side tool that can only resolve the authenticated user attached to the current request. */
@Component
public class CurrentUserProfileTool {
    static final String NAME = "get_current_user_profile";
    private final ReHealthBusinessRepository repository;
    private final ObjectMapper objectMapper;

    public CurrentUserProfileTool(ReHealthBusinessRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public String execute(String authenticatedUserId) {
        PatientProfileDto profile = repository.findPatientProfile(authenticatedUserId).orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", profile != null);
        if (profile != null) {
            put(result, "name", bounded(profile.name, 80));
            put(result, "gender", bounded(profile.gender, 24));
            put(result, "age", profile.age);
            put(result, "heightCm", profile.heightCm);
            put(result, "weightKg", profile.weightKg);
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("current user profile tool result cannot be serialized", failure);
        }
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null && (!(value instanceof String text) || !text.isBlank())) {
            target.put(key, value);
        }
    }

    private String bounded(String value, int limit) {
        if (value == null) return null;
        String normalized = value.strip();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }
}
