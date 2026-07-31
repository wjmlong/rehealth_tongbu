package org.jeecg.modules.rehealth.service.intervention;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpDeviceInterventionContextClientTest {
    @Test
    void sendsCredentialAndParsesTenantScopedTodayContext() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("""
                {
                  "success":true,
                  "result":{
                    "generatedAt":1785456000000,
                    "localDate":"2026-07-31",
                    "timeZone":"Asia/Shanghai",
                    "latestDataAt":1785427200000,
                    "todayBehavior":{
                      "steps":4200,
                      "activeMinutes":18,
                      "activityCaloriesKcal":126.0,
                      "dietRecords":[{
                        "mealType":"lunch",
                        "description":"午餐",
                        "consumedAt":1785413880000,
                        "caloriesKcal":780.0
                      }],
                      "measurements":[]
                    },
                    "recentChanges":[]
                  }
                }
                """);
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(response);
        HttpDeviceInterventionContextClient client =
                new HttpDeviceInterventionContextClient(
                        "http://device-service:8091/",
                        "service-secret",
                        Duration.ofSeconds(5),
                        new ObjectMapper(),
                        httpClient
                );

        DeviceInterventionContext context =
                client.fetch("tenant A", "user/1", ZoneId.of("Asia/Shanghai"));

        assertEquals("2026-07-31", context.localDate);
        assertEquals(4200, context.todayBehavior.steps);
        assertEquals(1, context.todayBehavior.dietRecords.size());
        assertEquals(780.0, context.todayBehavior.dietRecords.get(0).caloriesKcal);
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                request.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        );
        assertTrue(request.getValue().uri().toString().contains("/users/user%2F1/"));
        assertTrue(request.getValue().uri().toString().contains("tenantId=tenant%20A"));
        assertEquals(
                "service-secret",
                request.getValue().headers()
                        .firstValue("X-ReHealth-Service-Credential")
                        .orElseThrow()
        );
    }

    @Test
    void failsClosedWhenDeviceServiceRejectsRequest() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(503);
        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(response);
        HttpDeviceInterventionContextClient client =
                new HttpDeviceInterventionContextClient(
                        "http://device-service:8091",
                        "service-secret",
                        Duration.ofSeconds(5),
                        new ObjectMapper(),
                        httpClient
                );

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> client.fetch("tenant-a", "user-a", ZoneId.of("Asia/Shanghai"))
        );

        assertTrue(failure.getMessage().contains("request failed"));
    }
}
