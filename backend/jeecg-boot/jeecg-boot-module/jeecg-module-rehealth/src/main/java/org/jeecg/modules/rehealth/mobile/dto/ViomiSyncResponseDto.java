package org.jeecg.modules.rehealth.mobile.dto;

import java.util.ArrayList;
import java.util.List;

public class ViomiSyncResponseDto {
    public String deviceId;
    public String status;
    public boolean persisted;
    public int recordCount;
    public List<Measurement> measurements = new ArrayList<>();

    public static class Measurement {
        public String id;
        public String metricType;
        public long measuredAt;
        public double primaryValue;
        public Double secondaryValue;
        public String unit;
        public String source;
    }
}
