package org.jeecg.modules.rehealth.insurance;

import java.time.LocalDateTime;
import java.util.List;

public final class InsuranceImportResponse {
    private InsuranceImportResponse() {
    }

    public record BatchResult(
            String batchId,
            String importType,
            String status,
            int totalCount,
            int successCount,
            int failureCount,
            boolean idempotentReplay,
            LocalDateTime completedAt,
            List<RecordResult> records
    ) {
    }

    public record RecordResult(
            int rowNumber,
            String id,
            String businessKey,
            String subjectRef,
            String status
    ) {
    }
}
