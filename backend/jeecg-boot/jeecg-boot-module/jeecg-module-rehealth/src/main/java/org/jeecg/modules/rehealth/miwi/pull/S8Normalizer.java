package org.jeecg.modules.rehealth.miwi.pull;

import org.jeecg.modules.rehealth.miwi.MiwiHealthDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Normalizes S8 vendor "bytime" OpenAPI responses into ReHealth measurement maps.
 *
 * The response envelope shape is tolerant: it finds the record array under
 * {@code data}, {@code data.list}, {@code list}, {@code items}, or {@code records},
 * and also accepts a single object. Each record's timestamp is normalized to UTC
 * via {@link MiwiHealthDataMapper#resolveMeasuredAt(Map, long)} (epoch sec/ms or
 * Beijing-local date strings). Per-metric value extraction lives in {@link S8Metric}.
 */
@Component
public class S8Normalizer {

    private static final Logger log = LoggerFactory.getLogger(S8Normalizer.class);

    private final MiwiHealthDataMapper mapper;

    public S8Normalizer(MiwiHealthDataMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> normalizeBytime(
            S8Metric metric,
            Map<String, Object> response,
            long fallbackUtcMillis
    ) {
        List<Map<String, Object>> records = extractRecords(response);
        List<Map<String, Object>> measurements = new ArrayList<>();
        for (Map<String, Object> item : records) {
            long measuredAt = mapper.resolveMeasuredAt(item, fallbackUtcMillis);
            Map<String, Object> measurement = metric.toMeasurement(item, mapper, measuredAt);
            if (measurement != null) {
                measurements.add(measurement);
            }
        }
        return measurements;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRecords(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object data = firstNonNull(response, "data", "Data", "result", "Result");
        if (data instanceof List<?> list) {
            return toMapList(list);
        }
        if (data instanceof Map<?, ?> dataMap) {
            Object inner = firstNonNull((Map<String, Object>) dataMap, "list", "items", "records", "List", "Items");
            if (inner instanceof List<?> innerList) {
                return toMapList(innerList);
            }
            // data is a single object
            return List.of((Map<String, Object>) dataMap);
        }
        Object direct = firstNonNull(response, "list", "items", "records", "List", "Items");
        if (direct instanceof List<?> directList) {
            return toMapList(directList);
        }
        log.debug("miwi bytime response had no recognizable record array");
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(List<?> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    private Object firstNonNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
