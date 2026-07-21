package org.jeecg.modules.rehealth.ingest;

import java.util.ArrayList;
import java.util.List;

public class TelemetryBatchValidationResult {
    public boolean valid;
    public int recordCount;
    public int measurementCount;
    public int sleepSessionCount;
    public int activitySessionCount;
    public int signalChunkCount;
    public List<String> warnings = new ArrayList<>();
    public List<String> errors = new ArrayList<>();
}
