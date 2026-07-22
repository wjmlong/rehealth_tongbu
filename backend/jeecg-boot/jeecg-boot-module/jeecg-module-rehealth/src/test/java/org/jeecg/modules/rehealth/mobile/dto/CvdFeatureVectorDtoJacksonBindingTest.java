package org.jeecg.modules.rehealth.mobile.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CvdFeatureVectorDtoJacksonBindingTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bindsCanonicalSnakeCaseAndCamelCaseAliases() throws Exception {
        CvdFeatureVectorDto canonical = objectMapper.readValue(
                """
                {"fasting_glucose":5.4,"total_cholesterol":4.8,"exercise_days":3,
                 "diabetes_history":1,"hypertension_history":0,"family_history":1}
                """,
                CvdFeatureVectorDto.class
        );
        assertEquals(5.4, canonical.fastingGlucose);
        assertEquals(4.8, canonical.totalCholesterol);
        assertEquals(3, canonical.exerciseDays);
        assertEquals(1, canonical.diabetesHistory);
        assertEquals(0, canonical.hypertensionHistory);
        assertEquals(1, canonical.familyHistory);

        CvdFeatureVectorDto camelCase = objectMapper.readValue(
                """
                {"fastingGlucose":5.5,"totalCholesterol":4.9,"exerciseDays":4,
                 "diabetesHistory":0,"hypertensionHistory":1,"familyHistory":0}
                """,
                CvdFeatureVectorDto.class
        );
        assertEquals(5.5, camelCase.fastingGlucose);
        assertEquals(4.9, camelCase.totalCholesterol);
        assertEquals(4, camelCase.exerciseDays);
        assertEquals(0, camelCase.diabetesHistory);
        assertEquals(1, camelCase.hypertensionHistory);
        assertEquals(0, camelCase.familyHistory);
    }

    @Test
    void serializesCanonicalSnakeCaseNames() throws Exception {
        CvdFeatureVectorDto vector = new CvdFeatureVectorDto();
        vector.fastingGlucose = 5.4;
        vector.totalCholesterol = 4.8;
        vector.exerciseDays = 3;
        vector.diabetesHistory = 1;
        vector.hypertensionHistory = 0;
        vector.familyHistory = 1;

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(vector));

        assertEquals(5.4, json.get("fasting_glucose").doubleValue());
        assertEquals(4.8, json.get("total_cholesterol").doubleValue());
        assertEquals(3, json.get("exercise_days").intValue());
        assertEquals(1, json.get("diabetes_history").intValue());
        assertEquals(0, json.get("hypertension_history").intValue());
        assertEquals(1, json.get("family_history").intValue());
        assertFalse(json.has("fastingGlucose"));
        assertFalse(json.has("totalCholesterol"));
        assertFalse(json.has("exerciseDays"));
        assertFalse(json.has("diabetesHistory"));
        assertFalse(json.has("hypertensionHistory"));
        assertFalse(json.has("familyHistory"));
    }
}
