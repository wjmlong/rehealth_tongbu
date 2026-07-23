package com.rehealth.device.config;

import com.rehealth.contracts.telemetry.v1.TelemetryContractValidator;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceServiceConfiguration {
    @Bean
    TelemetryContractValidator telemetryContractValidator() {
        return new TelemetryContractValidator(TelemetryValidationPolicy.productionDefault());
    }
}
