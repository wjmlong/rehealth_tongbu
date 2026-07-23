package com.rehealth.device.config;

import com.rehealth.device.adapter.HttpIdentityAuthorizationAdapter;
import com.rehealth.device.adapter.UnavailableIdentityAuthorizationAdapter;
import com.rehealth.device.port.IdentityAuthorizationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class IdentityAdapterConfiguration {
    @Bean
    @ConditionalOnProperty(name = "rehealth.identity.enabled", havingValue = "true")
    IdentityAuthorizationPort httpIdentityAuthorization(
            RestClient.Builder builder,
            ServiceCredentialProvider credentialProvider,
            org.springframework.core.env.Environment environment
    ) {
        String baseUrl = environment.getRequiredProperty("rehealth.identity.base-url");
        String readinessUrl = environment.getRequiredProperty("rehealth.identity.readiness-url");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(2_000);
        return new HttpIdentityAuthorizationAdapter(
                builder.requestFactory(requestFactory),
                new IdentityServiceEndpoints(baseUrl, readinessUrl),
                credentialProvider
        );
    }

    @Bean
    @ConditionalOnMissingBean(IdentityAuthorizationPort.class)
    IdentityAuthorizationPort unavailableIdentityAuthorization() {
        return new UnavailableIdentityAuthorizationAdapter();
    }
}
