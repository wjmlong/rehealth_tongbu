package org.jeecg.modules.rehealth.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.repository.impl.JdbcSoftwareDbReHealthBusinessRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class InternalIdentityAndDeviceAuthorizationIT {
    private static final String SERVICE_CREDENTIAL = "injected-test-service-credential";
    private static final String VALID_TOKEN = "opaque-valid-user-token";
    private static final Set<String> SUPPORTED_CASES = Set.of(
            "valid",
            "revoked",
            "cross_device",
            "unbound",
            "cross_tenant",
            "wrong_service",
            "spoofed_header",
            "auth_unavailable"
    );

    private JdbcTemplate jdbcTemplate;
    private InternalIdentityController controller;
    private int initialBindingCount;

    @BeforeAll
    static void validateSelectedCases() {
        Set<String> unknown = selectedCases().stream()
                .filter(caseName -> !SUPPORTED_CASES.contains(caseName))
                .collect(Collectors.toSet());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unsupported -Dcases values: " + unknown);
        }
    }

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
        DeviceBindRequestDto otherBinding = new DeviceBindRequestDto();
        otherBinding.deviceId = "ring-other-user";
        repository.recordDeviceBinding("user-other", otherBinding);
        initialBindingCount = bindingCount();

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
        runCase("valid");
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
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void rejectsRevokedTokenWithoutWriting() {
        runCase("revoked");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                "revoked-token",
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("USER_TOKEN_REJECTED", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void rejectsAnotherUsersDeviceWithoutWriting() {
        runCase("cross_device");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-a",
                "ring-other-user",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("DEVICE_BINDING_REJECTED", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void rejectsUnboundDeviceWithoutWriting() {
        runCase("unbound");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-a",
                "ring-unbound",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("DEVICE_BINDING_REJECTED", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void rejectsTenantMismatchWithoutWriting() {
        runCase("cross_tenant");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-other",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("TENANT_MEMBERSHIP_REJECTED", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void rejectsWrongServiceCredentialWithoutResolvingIdentity() {
        runCase("wrong_service");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                "wrong-service-credential",
                VALID_TOKEN,
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("SERVICE_CREDENTIAL_REJECTED", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void rejectsSpoofedResolvedIdentityHeader() {
        runCase("spoofed_header");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                VALID_TOKEN,
                "tenant-a",
                "ring-owner",
                "user-attacker"
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("SPOOFED_IDENTITY_HEADER", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
    }

    @Test
    void failsClosedWhenIdentityProviderIsUnavailable() {
        runCase("auth_unavailable");
        ResponseEntity<InternalDeviceAuthorizationResponseDto> response = authorize(
                SERVICE_CREDENTIAL,
                "auth-unavailable-token",
                "tenant-a",
                "ring-owner",
                null
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("IDENTITY_PROVIDER_UNAVAILABLE", response.getBody().code);
        assertEquals(initialBindingCount, bindingCount());
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

    private void runCase(String caseName) {
        Set<String> selected = selectedCases();
        assumeTrue(selected.isEmpty() || selected.contains(caseName), () -> "case not selected: " + caseName);
    }

    private static Set<String> selectedCases() {
        String cases = System.getProperty("cases", "").trim();
        if (cases.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(cases.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
