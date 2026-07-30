package org.jeecg.modules.rehealth.mobile.dto;

import java.util.List;

public class HealthAgentConversationDto {
    public String conversationId;
    public String title;
    public String status;
    public Long createdAt;
    public Long updatedAt;
    public List<HealthAgentHistoryMessageDto> messages = List.of();
}
