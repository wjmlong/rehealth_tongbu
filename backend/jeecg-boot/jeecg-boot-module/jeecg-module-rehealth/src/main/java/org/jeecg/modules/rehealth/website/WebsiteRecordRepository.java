package org.jeecg.modules.rehealth.website;

import com.fasterxml.jackson.databind.JsonNode;
import org.jeecg.common.system.vo.LoginUser;

import java.util.List;
import java.util.Optional;

public interface WebsiteRecordRepository {
    WebsiteRecord save(LoginUser user, String tenantId, String resource, JsonNode payload);
    List<WebsiteRecord> list(String tenantId, String resource, int pageNo, int pageSize, String keyword, String status);
    long count(String tenantId, String resource, String keyword, String status);
    Optional<WebsiteRecord> find(String tenantId, String resource, String id);
    boolean delete(String tenantId, String resource, String id);
}
