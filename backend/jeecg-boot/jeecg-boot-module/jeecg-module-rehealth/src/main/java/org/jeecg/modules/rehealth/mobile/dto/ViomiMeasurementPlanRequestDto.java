package org.jeecg.modules.rehealth.mobile.dto;

import java.util.LinkedHashSet;
import java.util.Set;

public class ViomiMeasurementPlanRequestDto {
    public String imei;
    public boolean enabled;
    public int intervalMinutes = 5;
    public Set<String> metrics = new LinkedHashSet<>();
}
