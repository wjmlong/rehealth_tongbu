package org.jeecg.modules.rehealth.ingest.writer;

import java.util.ArrayList;
import java.util.List;

public class HardwareWriteResult {
    public boolean persisted;
    public boolean duplicate;
    public String receiptId;
    public String status;
    public String stage;
    public String writerType;
    public List<String> warnings = new ArrayList<>();
}
