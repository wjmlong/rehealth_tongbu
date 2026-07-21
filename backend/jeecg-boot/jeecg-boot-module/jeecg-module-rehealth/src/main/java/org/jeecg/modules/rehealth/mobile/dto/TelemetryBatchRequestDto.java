package org.jeecg.modules.rehealth.mobile.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelemetryBatchRequestDto {
    public String batchId;
    public String userId;
    public String deviceId;
    public Long collectedFrom;
    public Long collectedTo;
    public String source;
    public List<Map<String, Object>> measurements = new ArrayList<>();
    public List<Map<String, Object>> sleepSessions = new ArrayList<>();
    public List<Map<String, Object>> activitySessions = new ArrayList<>();
    public List<Map<String, Object>> signalChunks = new ArrayList<>();
    public Map<String, Object> quality = new LinkedHashMap<>();
}
