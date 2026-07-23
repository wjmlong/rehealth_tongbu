package org.jeecg.modules.rehealth.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.repository.impl.JdbcSoftwareDbReHealthBusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalIdentityAndDeviceAuthorizationIT {
    private static final String SERVICE_CREDENTIAL = "injected-test-service-credential";
    private static final String VALID_TOKEN = "opaque-valid-user-token";

    private JdbcTemplate jdbcTemplate;
    private InternalIdentityController controller;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:internal-auth-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/software/mysql/V1__create_rehealth_software_tables.sql")
        ).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcSoftwareDbReHealthBusinessRepository repository =
                new JdbcSoftwareDbReHealthBusinessRepository(jdbcTemplate, new ObjectMapper());
        DeviceBindRequestDto binding = new DeviceBindRequestDto();
        binding.deviceId = "ring-owner";
        repository.recordDeviceBinding("user-owner", binding);

        InternalServiceAuthProperties properties = new InternalServiceAuthProperties(SERVICE_CREDENTIAL);
        UserTokenIdentityProvider identityProvider = token -> switch (token) {
            case VALID_TOKEN -> new ResolvedUserIdentity("user-owner", Set.of("tenant-a"));
            case "revoked-token" -> throw new IdentityResolutionException(
                    IdentityResolutionException.Kind.TOKEN_REJECTED
            );
            case "auth-unavailable-token" -> throw new IdentityResolutionException(
                    IdentityResolutionException.Kind.PROVIDER_UNAVAILABLE
            );
            default -> throw new IdentityResolutionException(IdentityResolutionException.Kind.TOKEN_REJECTED);
        };
        InternalIdentityAuthorizationService service = new InternalIdentityAuthorizationService(
                new ServiceCredentialVerifier(properties),
                identityProvider,
                repository
        );
        controller = new InternalIdentityController(service);
    }

    @Test
    void authorizesAuthenticatedOwnerForActiveBinding() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("user-owner", response.getBody().userId);
        assertEquals("tenant-a", response.getBody().tenantId);
        assertEquals("ring-owner", response.getBody().deviceId);
        assertEquals(1, bindingCount());
    }

    @Test
    void rejectsRevokedTokenWithoutWriting() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                "revoked-token",
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("USER_TOKEN_REJECTED", response.getBody().code);
        assertEquals(1, bindingCount());
    }

    @Test
    void rejectsAnotherUsersOrUnboundDeviceWithoutWriting() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-a",
                "ring-other-user",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("DEVICE_BINDING_REJECTED", response.getBody().code);
        assertEquals(1, bindingCount());
    }

    @Test
    void rejectsTenantMismatchWithoutWriting() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-other",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("TENANT_MEMBERSHIP_REJECTED", response.getBody().code);
        assertEquals(1, bindingCount());
    }

    @Test
    void rejectsWrongServiceCredentialWithoutResolvingIdentity() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                "wrong-service-credential",
                VALID_TOKEN,
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("SERVICE_CREDENTIAL_REJECTED", response.getBody().code);
        assertEquals(1, bindingCount());
    }

    @Test
    void rejectsSpoofedResolvedIdentityHeader() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-a",
                "ring-owner",
                "user-attacker"
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("SPOOFED_IDENTITY_HEADER", response.getBody().code);
        assertEquals(1, bindingCount());
    }

    @Test
    void failsClosedWhenIdentityProviderIsUnavailable() {
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                "auth-unavailable-token",
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("IDENTITY_PROVIDER_UNAVAILABLE", response.getBody().code);
        assertEquals(1, bindingCount());
    }

    private ResponseEntity<InternalDeviceAuthorizationResponseDto> authorize(
            String serviceCredential,
            String accessToken,
            String tenantId,
            String deviceId,
            String spoofedUserId
    ) {
        InternalDeviceAuthorizationRequestDto request = new InternalDeviceAuthorizationRequestDto();
        request.tenantId = tenantId;
        request.deviceId = deviceId;
        return controller.authorizeDevice(serviceCredential, accessToken, spoofedUserId, null, null, request);
    }

    private int bindingCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rehealth_device_binding", Integer.class);
    }
}
