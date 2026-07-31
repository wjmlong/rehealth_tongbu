package org.jeecg.modules.rehealth.repository;

import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BehaviorRecordRepository {
    Optional<BehaviorRecordDto> findByRequestId(String tenantId, String userId, String requestId);

    BehaviorRecordDto save(String tenantId, String userId, BehaviorRecordDto record);

    List<BehaviorRecordDto> findInWindow(
            String tenantId,
            String userId,
            Instant startInclusive,
            Instant endExclusive
    );
}
