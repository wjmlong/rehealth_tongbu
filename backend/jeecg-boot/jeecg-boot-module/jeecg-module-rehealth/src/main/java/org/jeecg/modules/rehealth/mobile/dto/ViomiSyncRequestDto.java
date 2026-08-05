package org.jeecg.modules.rehealth.mobile.dto;

import java.util.LinkedHashSet;
import java.util.Set;

public class ViomiSyncRequestDto {
    public String imei;
    public Long beginAt;
    public Long endAt;
    public Set<String> metrics = new LinkedHashSet<>();
}
