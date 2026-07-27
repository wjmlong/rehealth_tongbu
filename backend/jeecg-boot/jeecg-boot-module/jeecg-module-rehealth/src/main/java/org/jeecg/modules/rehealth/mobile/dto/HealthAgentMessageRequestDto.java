package org.jeecg.modules.rehealth.mobile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public class HealthAgentMessageRequestDto {
    public String requestId;
    public String message;
    public String locale;
}
