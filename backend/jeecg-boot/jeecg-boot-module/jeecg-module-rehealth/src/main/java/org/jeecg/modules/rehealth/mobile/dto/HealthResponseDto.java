package org.jeecg.modules.rehealth.mobile.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class HealthResponseDto {
    public String status;
    public String service;
    public String module;
    public boolean modelServiceConfigured;
    public Map<String, Object> dependencies = new LinkedHashMap<>();
}
