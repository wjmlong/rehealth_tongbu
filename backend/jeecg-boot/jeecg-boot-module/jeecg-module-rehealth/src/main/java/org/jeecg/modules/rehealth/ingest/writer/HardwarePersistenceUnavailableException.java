package org.jeecg.modules.rehealth.ingest.writer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class HardwarePersistenceUnavailableException extends RuntimeException {
    public HardwarePersistenceUnavailableException(String message) {
        super(message);
    }

    public HardwarePersistenceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
