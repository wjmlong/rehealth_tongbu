package com.rehealth.device;

import com.rehealth.device.adapter.HttpIdentityAuthorizationAdapter;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.config.IdentityServiceEndpoints;
import com.rehealth.device.config.ServiceCredentialProvider;
import com.rehealth.device.domain.DeviceClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class HttpIdentityAuthorizationAdapterTest {
    private MockRestServiceServer server;
    private HttpIdentityAuthorizationAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpIdentityAuthorizationAdapter(
                builder,
                new IdentityServiceEndpoints(
                        "http://identity.internal",
                        "http://identity.internal/actuator/health/readiness"
                ),
                new ServiceCredentialProvider("service-secret", "")
        );
    }

    @Test
    void reportsReadyOnlyWhenIdentityReadinessEndpointIsUp() {
        server.expect(requestTo("http://identity.internal/actuator/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertTrue(adapter.ready());
        server.verify();
    }

    @Test
    void sendsServiceAndUserCredentialsAndReturnsResolvedClaims() {
        server.expect(once(), requestTo("http://identity.internal/authorize-device"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-ReHealth-Service-Credential", "service-secret"))
                .andExpect(header("X-Access-Token", "user-token"))
                .andExpect(content().json("{\"tenantId\":\"tenant-a\",\"deviceId\":\"ring-owner\"}"))
                .andRespond(withSuccess("""
                        {
                          "authorized": true,
                          "code": "AUTHORIZED",
                          "userId": "user-owner",
                          "tenantId": "tenant-a",
                          "deviceId": "ring-owner"
                        }
                        """, MediaType.APPLICATION_JSON));

        DeviceClaims claims = adapter.authorize("user-token", "tenant-a", "ring-owner");

        assertEquals(new DeviceClaims("user-owner", "tenant-a", "ring-owner"), claims);
        server.verify();
    }

    @Test
    void mapsRejectedUserTokenToStableUnauthorizedError() {
        server.expect(requestTo("http://identity.internal/authorize-device"))
                .andRespond(withUnauthorizedRequest());

        DeviceRequestException exception = assertThrows(
                DeviceRequestException.class,
                () -> adapter.authorize("revoked-token", "tenant-a", "ring-owner")
        );

        assertEquals(401, exception.status().value());
        assertEquals("USER_TOKEN_REJECTED", exception.errorCode());
        server.verify();
    }
}
