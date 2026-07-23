# Todo 6 Done Claim

## Outcome

The independent Spring Boot 3.5.5 / Java 17 Device Service compiles, packages,
and preserves the telemetry mobile boundary through shared v1 contracts. It
has controller/application/port layers, Jeecg internal identity and active
device authorization integration, fail-closed unavailable adapters,
Result-compatible envelopes, Actuator readiness, OpenAPI, and a protected
internal operations namespace. The architecture gate forbids dependencies on
Jeecg repository/model packages.

## Success criteria and evidence

| Scenario | Invocation | Binary observable | Captured artifact |
|---|---|---|---|
| Red baseline | `mvn -f backend/device-service/pom.xml test` before implementation | Windows Maven unavailable; WSL Maven exits 1 because the POM is absent | `red-t6-full.log`, `red-t6-wsl.log`, `red-t6-wsl.exit` |
| Independent full T6 gate | From a source-only WSL-native mirror of the workspace: `mvn -f backend/device-service/pom.xml test` | `BUILD SUCCESS`; 5 tests, 0 failures/errors/skips | `green-t6-full-final.log`, `green-t6-full-final.exit`, `surefire-final/` |
| Exact focused T6 adversarial gate | `mvn -f backend/device-service/pom.xml -Dtest=DeviceApiBoundaryIT -Dcases=unauthenticated,cross_user,malformed test` | `BUILD SUCCESS`; the three selected cases pass and happy-path test is skipped by selector | `green-t6-adversarial.log`, `green-t6-adversarial.exit` |
| Unauthenticated request | `DeviceApiBoundaryIT#rejectsUnauthenticatedWithoutWriting` | HTTP 401, `USER_TOKEN_REQUIRED`, write count remains 0 | `surefire-final/TEST-com.rehealth.device.DeviceApiBoundaryIT.xml` |
| Cross-user device | `DeviceApiBoundaryIT#rejectsCrossUserDeviceWithoutWriting` | HTTP 403, `DEVICE_BINDING_REJECTED`, write count remains 0 | `surefire-final/TEST-com.rehealth.device.DeviceApiBoundaryIT.xml` |
| Malformed request | `DeviceApiBoundaryIT#rejectsMalformedJsonWithoutWriting` | HTTP 400, `MALFORMED_REQUEST`, write count remains 0 | `surefire-final/TEST-com.rehealth.device.DeviceApiBoundaryIT.xml` |
| Happy mixed batch and scoped recent read | `DeviceApiBoundaryIT#acceptsAuthenticatedMixedBatchAndScopesRecentRead` | HTTP 200; mixed record count 3; persisted/accepted true; recent owner is `user-owner`; one measurement returned | `green-t6-full-final.log` |
| Architecture boundary | `DeviceServiceArchitectureTest` | No dependency on `org.jeecg..`, `..repository..`, or `..model..` | `surefire-final/TEST-com.rehealth.device.DeviceServiceArchitectureTest.xml` |
| Executable artifact | `mvn -f backend/device-service/pom.xml -DskipTests package` | `BUILD SUCCESS`; executable JAR is 28,352,549 bytes | `package-t6.log`, `device-service-jar.sha256.txt`, `device-service-jar-contents.txt` |
| Live readiness/OpenAPI | Packaged JAR on port 18091; GET readiness and `/v3/api-docs` | readiness HTTP 200 `{"status":"UP"}`; OpenAPI HTTP 200 contains batch and recent paths | `live-readiness.json`, `live-openapi.json`, `live-runtime.log` |

The mirror was required because the Windows host has no Java/Maven and Maven
compilation directly under WSL `/mnt/d` stalled. The mirror contains only the
current workspace `backend/device-service` and shared telemetry contract
sources; the exact Maven project and selectors are unchanged.

## Stop-hook reverification

Completion was independently rechecked after the first claim:

- The exact command against `/mnt/d/rehealthAI` was attempted again and
  deterministically stalled at `maven-compiler-plugin`; this environmental
  result is captured in `reverify-workspace-full.log` and
  `reverify-workspace-full.exit`.
- SHA-256 comparison proves all 37 Maven, Java, YAML, and shared-contract files
  used by the native WSL mirror match the current workspace with zero
  mismatches (`reverify-mirror-sha256.json`).
- After `mvn clean`, the full exact T6 selector recompiled 30 main and 2 test
  sources and passed 5/5 (`reverify-mirror-full.log`, exit 0).
- The exact adversarial selector passed again
  (`reverify-mirror-adversarial.log`, exit 0).
- A fresh package completed with exit 0, and the fresh executable JAR was
  copied to the workspace target (`reverify-package.log`,
  `reverify-device-service-jar.sha256.txt`).
- The fresh JAR was launched interactively, readiness returned HTTP 200
  `{"status":"UP"}`, OpenAPI contained both telemetry paths, and Ctrl+C
  performed a graceful shutdown. A final request confirms no server remains.
  See `reverify-live-readiness.json`, `reverify-live-openapi.json`, and
  `REVERIFY_SUMMARY.txt`.

## Adversarial cases

- Missing token is rejected before identity resolution or persistence.
- Another user's bound device is rejected by resolved authorization before persistence.
- Malformed JSON is normalized to a stable 400 envelope before persistence.
- Client-supplied ownership remains rejected by the shared contract validator.
- Missing identity or persistence adapters fail closed with stable 503 codes.

## Cleanup and review

- The live process was bounded by `timeout 30s`; no long-running service was left.
- No database, Timescale schema, Kafka, Android, model-service, or PIAS files were changed.
- `git diff --check` exits 0; the warnings in its log concern pre-existing/shared line-ending state.
- `DeviceApiBoundaryIT.java` is in the 200-250 LOC warning band (215 pure LOC);
  split it by endpoint before adding more scenarios.
- No commit or push was made, as requested.
