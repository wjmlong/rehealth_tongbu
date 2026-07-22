package org.jeecg.modules.rehealth.mobile.dto;

import java.util.List;

public class HealthInterviewSubmitRequestDto {
    public List<HealthInterviewAnswerDto> answers;
    public List<HealthInterviewBaselineItemDto> baselineItems;
    public List<String> focusAreas;
    public Long generatedAt;
}
