package org.jeecg.modules.rehealth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReHealthIngestProperties {
    @Value("${rehealth.ingest.mode:durable-direct}")
    private String ingestMode;

    @Value("${rehealth.hardware-db.enabled:false}")
    private boolean hardwareDbEnabled;

    @Value("${rehealth.raw-signal-upload.enabled:false}")
    private boolean rawSignalUploadEnabled;

    @Value("${rehealth.ingest.queue.type:direct-hardware-db}")
    private String queueType;

    @Value("${rehealth.ingest.max-records-per-batch:5000}")
    private int maxRecordsPerBatch;

    public String getIngestMode() {
        return ingestMode;
    }

    public boolean isHardwareDbEnabled() {
        return hardwareDbEnabled;
    }

    public boolean isRawSignalUploadEnabled() {
        return rawSignalUploadEnabled;
    }

    public String getQueueType() {
        return queueType;
    }

    public int getMaxRecordsPerBatch() {
        return maxRecordsPerBatch;
    }

}
