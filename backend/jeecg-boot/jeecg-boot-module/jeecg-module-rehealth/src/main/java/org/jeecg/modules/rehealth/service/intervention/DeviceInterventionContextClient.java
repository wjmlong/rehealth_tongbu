package org.jeecg.modules.rehealth.service.intervention;

import java.time.ZoneId;

public interface DeviceInterventionContextClient {
    DeviceInterventionContext fetch(String tenantId, String userId, ZoneId timeZone);
}
