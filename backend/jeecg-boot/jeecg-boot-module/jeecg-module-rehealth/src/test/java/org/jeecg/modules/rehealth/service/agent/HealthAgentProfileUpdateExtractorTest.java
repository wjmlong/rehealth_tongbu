package org.jeecg.modules.rehealth.service.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthAgentProfileUpdateExtractorTest {
    private final HealthAgentProfileUpdateExtractor extractor = new HealthAgentProfileUpdateExtractor();

    @Test
    void extractsLabeledChineseBasicProfileAndConvertsUnits() {
        HealthAgentProfilePatch patch = extractor.extract("""
                姓名：张三
                性别：男
                年龄：42岁
                身高：1.76米
                体重：142斤
                """);

        assertEquals("张三", patch.name());
        assertEquals("male", patch.gender());
        assertEquals(42, patch.age());
        assertEquals(176.0, patch.heightCm());
        assertEquals(71.0, patch.weightKg());
    }

    @Test
    void extractsExplicitFirstPersonNarrative() {
        HealthAgentProfilePatch patch = extractor.extract(
                "你好，我叫李华，我是女性，我今年35岁，我的身高是165cm，我的体重是58kg。"
        );

        assertEquals("李华", patch.name());
        assertEquals("female", patch.gender());
        assertEquals(35, patch.age());
        assertEquals(165.0, patch.heightCm());
        assertEquals(58.0, patch.weightKg());
    }

    @Test
    void ignoresThirdPartyAndHypotheticalInformation() {
        HealthAgentProfilePatch patch = extractor.extract(
                "我的朋友年龄是30岁，如果身高170cm、体重70kg应该怎么做？"
        );

        assertTrue(patch.isEmpty());
    }

    @Test
    void ignoresOutOfRangeValues() {
        HealthAgentProfilePatch patch = extractor.extract("年龄：180岁，身高：20cm，体重：900kg");

        assertTrue(patch.isEmpty());
    }
}
