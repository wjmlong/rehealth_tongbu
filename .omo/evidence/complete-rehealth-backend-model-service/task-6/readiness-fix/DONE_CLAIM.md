# T6 Readiness Fix Done Claim

## Root cause

Spring Boot exposed only lifecycle readiness. The unavailable identity and
telemetry adapters had no readiness signal, so `/actuator/health/readiness`
returned HTTP 200 `UP` although authenticated operations failed with 503.

## Fix

- Added a `requiredDependencies` health contributor covering identity,
  telemetry read, and telemetry write ports.
- Added it to the Actuator readiness group. Any unavailable required port
  produces `OUT_OF_SERVICE` and HTTP 503.
- The HTTP identity adapter probes an explicit Jeecg readiness URL with
  two-second connect/read timeouts; missing credentials or a failed probe is
  not ready.
- Added deployment variables for enabling identity and its readiness URL.
- Kafka is not referenced by the health contributor and remains irrelevant to
  ingest readiness.

## Direct evidence

| Scenario | Invocation | Observable | Artifact |
|---|---|---|---|
| Red regression | `mvn -f backend/device-service/pom.xml -Dtest=DeviceReadinessIT test` before fix | Expected 503, observed HTTP 200 `UP`; exit 1 | `readiness-red.log`, `readiness-red.exit` |
| Focused readiness | `mvn -f backend/device-service/pom.xml -Dtest=DeviceReadinessIT,DeviceApiBoundaryIT test` | Default unavailable is 503; bounded ready fakes are 200; exit 0 | `readiness-green-focused.log`, `readiness-green-focused.exit` |
| Identity wire contract | `mvn -f backend/device-service/pom.xml -Dtest=HttpIdentityAuthorizationAdapterTest test` | Authorization headers/body/claims, 401 mapping, and readiness probe pass | `identity-wire2.log`, `identity-wire2.exit` |
| Clean full gate | `mvn -f backend/device-service/pom.xml test` after `mvn clean` | 11 tests, 0 failures/errors/skips; build success | `readiness-final-full.log`, `readiness-final-full.exit` |
| Exact T6 adversarial | `mvn -f backend/device-service/pom.xml -Dtest=DeviceApiBoundaryIT -Dcases=unauthenticated,cross_user,malformed test` | Three selected adversarial cases pass; two non-selected happy/readiness cases skip; build success | `readiness-final-adversarial.log`, `readiness-final-adversarial.exit` |
| Live unavailable package | Fresh executable JAR on port 18095, `curl --noproxy '*' /actuator/health/readiness` | HTTP 503 `{"status":"OUT_OF_SERVICE"}` | `live-final-unavailable.json`, `live-final-unavailable.txt` |
| Live bounded ready profile | `DeviceReadyProfileLiveIT` starts an embedded server on a random port under profile `test-ready` and performs a real HTTP GET | HTTP 200 body contains `{"status":"UP"}`; exit 0 | `live-ready-profile.log`, `live-ready-profile.exit` |

The Windows host Maven/JBR compiler blocks on the shared `D:` filesystem.
Tests use the existing WSL-native source mirror; a final SHA-256 manifest
proves every Device Service and shared-contract input is identical to the
workspace.

## Cleanup

- Interactive packaged service stopped through Ctrl+C and graceful Tomcat shutdown.
- Port 18095 no longer serves readiness.
- No database, Kafka, Android, model-service, or PIAS implementation changed.
- No commit or push performed.
