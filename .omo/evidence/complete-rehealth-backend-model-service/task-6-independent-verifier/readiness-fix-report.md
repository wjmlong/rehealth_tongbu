# Todo 6 Readiness Fix Reverification

## verdict

CONFIRMED

The prior `T6-READINESS` blocker is resolved on the current shared worktree.

## reproduced checks

- Created a fresh WSL-native mirror from the current
  `backend/device-service` tree plus shared telemetry contract sources.
- `mvn -f backend/device-service/pom.xml test`
  - PASS
  - 11 tests, 0 failures, 0 errors, 0 skipped
  - Includes the API boundary, unavailable readiness, ready-adapter readiness,
    HTTP identity adapter, and architecture tests.
- Exact focused adversarial gate:
  `mvn -f backend/device-service/pom.xml -Dtest=DeviceApiBoundaryIT
  -Dcases=unauthenticated,cross_user,malformed test`
  - PASS
  - The three selected 401/403/400 zero-write scenarios pass.
  - Two non-selected scenarios are skipped by the documented selector.
- Focused fix gate:
  `mvn -f backend/device-service/pom.xml
  -Dtest=DeviceReadinessIT,DeviceReadyProfileLiveIT,HttpIdentityAuthorizationAdapterTest
  test`
  - PASS
  - 5 tests, 0 failures, 0 errors, 0 skipped.

## readiness findings

- `DeviceReadinessIT` starts the real embedded HTTP server with the default
  unavailable identity and telemetry adapters. The readiness endpoint returns
  HTTP 503 and contains `"status":"OUT_OF_SERVICE"`.
- `DeviceReadyProfileLiveIT` supplies ready identity/read/write adapters through
  the normal Spring context. The readiness endpoint returns HTTP 200 and
  contains `"status":"UP"`.
- `RequiredDependenciesHealthIndicator` includes only identity authorization
  and telemetry read/write readiness. Kafka is absent from the Device Service
  Maven dependencies, configuration, source, and readiness group.
- The ready-adapter test passes in an environment with no Kafka service or
  Device Service Kafka client, confirming Kafka absence does not make ingest
  readiness fail.

## identity adapter findings

`HttpIdentityAuthorizationAdapterTest` exercises the real `RestClient` adapter
through `MockRestServiceServer` and confirms:

- the Jeecg authorization URL and POST method;
- internal service credential and user-token headers;
- tenant/device request JSON;
- immutable resolved user/tenant/device claims from the response;
- stable 401 `USER_TOKEN_REJECTED` mapping;
- identity readiness succeeds only on a successful readiness response.

This closes the earlier evidence gap at the HTTP adapter boundary. It is a
real HTTP-client serialization/response-mapping test, while intentionally using
an in-process mock server rather than launching JeecgBoot.

## unchanged boundary semantics

The full `DeviceApiBoundaryIT` suite remains green. The authenticated mixed
batch and scoped recent-read path still passes, as do the stable
unauthenticated, cross-user, malformed, and unavailable-adapter behaviors.

## cleanup

- The Maven test JVMs and embedded Tomcat servers terminated normally.
- A post-test WSL process and listening-port audit found no Java, Maven, or
  Device Service process.
- Removed the verifier-created `/tmp/rehealth-t6-reverify` mirror and temporary
  source archive.
- No product file, commit, push, container, database, or external service was
  changed by this verification.
