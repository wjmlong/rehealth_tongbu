# Todo 6 Independent Gate Review

## recommendation

REJECT

## originalIntent

Ship an independently buildable Spring Boot 3.5.5 / Java 17 Device Service
scaffold that preserves the Android telemetry API surface, consumes the shared
telemetry contract, derives tenant/user/device ownership from Jeecg identity
authorization, exposes truthful health/readiness and OpenAPI surfaces, and
keeps Jeecg repository/model responsibilities outside the service.

## desiredOutcome

The valid mixed-batch and scoped-recent flows should retain the Jeecg `Result`
shape; unauthenticated, cross-user, and malformed requests should return stable
4xx responses with zero writes; an independently packaged service should report
ready only when its required identity and telemetry adapters can serve those
flows.

## userOutcomeReview

The code is a genuine independent scaffold with controller/application/port
layers. It directly compiles the shared telemetry v1 sources, and the API
envelope fields match Jeecg `Result` (`success`, `message`, `code`, `result`,
`timestamp`). The exact full and focused T6 Maven gates pass in a WSL-native
mirror whose reviewed Device Service and shared-contract files have zero SHA-256
differences from the current worktree. The tests prove stable 401/403/400
responses and zero fake-store writes for the three named adversarial cases.

The shipped readiness surface is not truthful. `application.yml` enables
Actuator probes, but no readiness contributors represent identity authorization
or telemetry persistence. With no identity base URL and only
`UnavailableTelemetryStore` active, `/actuator/health/readiness` still returns
HTTP 200 `{"status":"UP"}`. In that same configuration every authenticated
batch/recent request necessarily fails 503. This conflicts with the plan's
required health/readiness surface and the already-approved topology criterion
that Device Service readiness depends on auth and Timescale availability.

## blockers

1. **violatedCriterion:** T6-READINESS — Device Service must expose a usable,
   truthful health/readiness surface (Todo 6), with readiness dependent on auth
   and telemetry persistence (Todo 2 dependency).
   **evidencePointer:** `backend/device-service/src/main/resources/application.yml:9`;
   `backend/device-service/src/main/java/com/rehealth/device/adapter/UnavailableTelemetryStore.java:15`;
   `backend/device-service/src/main/java/com/rehealth/device/adapter/UnavailableIdentityAuthorizationAdapter.java:8`;
   `.omo/evidence/complete-rehealth-backend-model-service/task-6/live-readiness.json`
   (`{"status":"UP"}`).

## checkedArtifacts

- `AGENTS.md`
- `ENGINEERING.md`
- `ACCEPTANCE_REVIEW_2026-07-16.md`
- `.omo/plans/complete-rehealth-backend-model-service.md` (T6 and Todo 6)
- `.omo/evidence/complete-rehealth-backend-model-service/task-6/DONE_CLAIM.md`
- `.omo/evidence/complete-rehealth-backend-model-service/task-6/surefire-final/`
- `.omo/evidence/complete-rehealth-backend-model-service/task-6/live-readiness.json`
- `.omo/evidence/complete-rehealth-backend-model-service/task-6/live-openapi.json`
- all files under `backend/device-service`
- shared telemetry sources under `backend/contracts/telemetry/src/main`
- Jeecg `Result.java`, `ReHealthMobileController.java`, and internal identity
  controller/authorization surface
- Android `JeecgResult` and telemetry route declarations

## reproducedEvidence

- Source mirror SHA-256 comparison: `HASH_DIFF_COUNT=0`.
- `mvn -f backend/device-service/pom.xml test`: PASS, 5 tests, 0 failures,
  0 errors, 0 skipped.
- `mvn -f backend/device-service/pom.xml -Dtest=DeviceApiBoundaryIT
  -Dcases=unauthenticated,cross_user,malformed test`: PASS, 4 discovered,
  3 selected passed, happy-path case skipped.
- Architecture test directly forbids `org.jeecg..`, `..repository..`, and
  `..model..` dependencies.
- OpenAPI artifact contains batch and recent paths.

## slopAndMaintenancePass

- No deletion-only, requested-removal, tautological, or production-code-mirroring
  tests were found.
- The boundary tests use narrow fakes and assert HTTP-observable results plus
  write counts. They do not prove the real HTTP identity adapter integration;
  that is an evidence gap, but the named T6 acceptance gate only mandates
  MockMvc boundary coverage.
- `DeviceApiBoundaryIT.java` is 215 pure LOC, below the 250-LOC blocking limit.
- No needless production extraction, speculative parser/normalizer, Jeecg
  repository/model dependency, raw-health logging, token logging, or embedded
  secret was found.

## scopeAndSecrets

The intended Todo 6 product scope is the untracked `backend/device-service/`
tree. Existing unrelated worktree changes remain in the plan, ledger, and
deployment compose file. The compose change wires the expected identity URL and
does not contain a credential value. No credential, token, raw health payload,
phone number, or private key was found in the reviewed Device Service sources.

## exactEvidenceGaps

- No readiness health contributor or test ties readiness to identity and
  telemetry-store availability.
- No real-wire integration test exercises `HttpIdentityAuthorizationAdapter`
  against the Jeecg internal identity endpoint; current scoping proof uses a
  fake authorization port.
- The DoneClaim calls readiness successful without disclosing that it is `UP`
  while all serving adapters are unavailable.

## cleanup

No product code, commit, push, database, container, or persistent service was
created by this review. A failed first copy attempt was terminated. The shared
existing `/tmp/rehealth-t6-copy` mirror was not removed because it predates this
review. The review-created partial `/tmp/rehealth-t6-verifier` directory may be
removed safely.
