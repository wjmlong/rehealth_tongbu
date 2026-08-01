package org.jeecg.modules.rehealth.service.behavior;

import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.jeecg.modules.rehealth.repository.BehaviorRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BehaviorRecordService {
    private final BehaviorPhotoAnalysisService analysisService;
    private final BehaviorRecordRepository repository;

    public BehaviorRecordService(
            BehaviorPhotoAnalysisService analysisService,
            BehaviorRecordRepository repository
    ) {
        this.analysisService = analysisService;
        this.repository = repository;
    }

    public BehaviorRecordDto analyzeAndSave(
            String tenantId,
            String userId,
            String requestId,
            long occurredAt,
            MultipartFile image
    ) {
        validateRequestId(requestId);
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("image is required");
        return repository.findByRequestId(tenantId, userId, requestId).orElseGet(() -> {
            byte[] bytes;
            try {
                bytes = image.getBytes();
            } catch (IOException failure) {
                throw new IllegalArgumentException("image could not be read", failure);
            }
            BehaviorRecordDto record = analysisService.analyze(bytes, image.getContentType());
            long now = System.currentTimeMillis();
            record.id = UUID.randomUUID().toString();
            record.requestId = requestId;
            record.occurredAt = occurredAt > 0 && occurredAt <= now + 86_400_000L ? occurredAt : now;
            record.createdAt = now;
            return repository.save(tenantId, userId, record);
        });
    }

    public List<BehaviorRecordDto> inWindow(
            String tenantId,
            String userId,
            Instant startInclusive,
            Instant endExclusive
    ) {
        return repository.findInWindow(tenantId, userId, startInclusive, endExclusive);
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 128) {
            throw new IllegalArgumentException("requestId is required and must not exceed 128 characters");
        }
    }
}
