package org.jeecg.modules.rehealth.repository.impl;

import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.jeecg.modules.rehealth.repository.BehaviorRecordRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledBehaviorRecordRepository implements BehaviorRecordRepository {
    @Override
    public Optional<BehaviorRecordDto> findByRequestId(String tenantId, String userId, String requestId) {
        return Optional.empty();
    }

    @Override
    public BehaviorRecordDto save(String tenantId, String userId, BehaviorRecordDto record) {
        throw new IllegalStateException("software_db is required for behavior records");
    }

    @Override
    public List<BehaviorRecordDto> findInWindow(
            String tenantId,
            String userId,
            Instant startInclusive,
            Instant endExclusive
    ) {
        throw new IllegalStateException("software_db is required for behavior records");
    }
}
