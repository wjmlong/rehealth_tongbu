package org.jeecg.modules.rehealth.mobile.dto;

import java.util.ArrayList;
import java.util.List;

public class MobileConfigResponseDto {
    public String apiVersion;
    public String modelContract;
    public boolean softwareDbPersistenceEnabled;
    public boolean hardwareIngestEnabled;
    public String ingestMode;
    public String ingestQueueType;
    public boolean hardwareDbEnabled;
    public boolean rawSignalUploadEnabled;
    public List<String> endpoints = new ArrayList<>();
    public List<String> limitations = new ArrayList<>();
}
