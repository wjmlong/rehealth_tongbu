package org.jeecg.modules.rehealth.mobile.dto;

import java.util.ArrayList;
import java.util.List;

public class TelemetryBatchResponseDto {
    public String batchId;
    public String receiptId;
    public String status;
    public boolean accepted;
    public boolean persisted;
    public boolean queued;
    public boolean durableQueue;
    public boolean hardwareDbEnabled;
    public boolean rawSignalUploadEnabled;
    public String ingestStage;
    public String ingestMode;
    public String queueType;
    public int recordCount;
    public int measurementCount;
    public int sleepSessionCount;
    public int activitySessionCount;
    public int signalChunkCount;
    public int rejectedCount;
    public List<String> warnings = new ArrayList<>();
}
