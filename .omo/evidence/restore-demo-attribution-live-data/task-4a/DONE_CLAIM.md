# Secure Attribution Proxy DoneClaim

## Outcome

Android attribution now sends local Room risk history only to the existing authenticated Jeecg mobile endpoint. JeecgBoot forwards the PIAS-shaped request server-side and passes through current-state, forecast, ATT, report, and partial-status fields without generating forecast or ATT values locally.

## Success Criteria Evidence

| Scenario | Invocation | Binary observable | Artifact |
| --- | --- | --- | --- |
| Android authenticated attribution contract | `./gradlew.bat --no-daemon testDebugUnitTest --tests 'com.rehealth.genie.network.ReHealthMobileApiAttributionTest'` | Exit `0`; `BUILD SUCCESSFUL`; recorded request path `/rehealth/mobile/attribution/events`, method `POST`, header `X-Access-Token`, and PIAS-shaped synthetic body | `Android-apk/app/build/test-results/testDebugUnitTest/TEST-com.rehealth.genie.network.ReHealthMobileApiAttributionTest.xml` |
| Backend PIAS proxy and partial response | `mvn -pl jeecg-boot-module/jeecg-module-rehealth -am -DskipTests=false -Dtest=HttpModelServiceClientAttributionTest,ReHealthMobileControllerAttributionContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | Exit `0`; `BUILD SUCCESS`; 3 tests, 0 failures/errors; exact PIAS v2 path and rich/accumulating response assertions | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/surefire-reports/TEST-org.jeecg.modules.rehealth.model.impl.HttpModelServiceClientAttributionTest.xml` |
| Authenticated backend controller route | Same Maven invocation | Controller method maps exact `/attribution/events` and lacks `@IgnoreAuth` | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/surefire-reports/TEST-org.jeecg.modules.rehealth.mobile.controller.ReHealthMobileControllerAttributionContractTest.xml` |
| Release fails closed for direct PIAS/cleartext | `rg -n "PiasApiClient|MODEL_SERVICE_BASE_URL|REHEALTH_MODEL_SERVICE_BASE_URL|/attribute/individual|api/pias/v2" Android-apk/app/src/main Android-apk/app/build.gradle.kts -g '!*.disabled' -g '!*.backup'` plus manifest/build-config scan | No active Android direct PIAS references; release placeholder is `usesCleartextTraffic=false`, debug explicitly overrides to `true` | `.omo/evidence/restore-demo-attribution-live-data/task-4a/security-audit.txt` |
| Patch hygiene | `git diff --check -- <attribution allowlist>` | Exit `0`; no whitespace errors | `.omo/evidence/restore-demo-attribution-live-data/task-4a/security-audit.txt` |

## Changed Files

- Backend: attribution request/response DTOs, PIAS v2 HTTP client mapping, dev attribution base URL, focused controller/client tests.
- Android: Retrofit/authenticated-client attribution method, typed mobile DTOs, PHM/application wiring, release cleartext policy, focused request contract test.
- Removed: direct unauthenticated Android `PiasApiClient` and its test.
- Authorized prerequisite repair: added the missing `JSONField` import to the committed `CvdFeatureVectorDto.java` compile blocker; no other changes to that file.

## Security and Privacy

- No token, risk history, or health identifier logging was added.
- Test history and token values are synthetic.
- Android production sources contain no direct PIAS URL/client reference.
- Release disallows cleartext traffic; local debug retains explicit emulator HTTP support.

## Residual Risk

- Backend attribution event persistence remains pending by design; Android local Room history remains request input.
- A live authenticated curl through a newly rebuilt local backend was not run in this focused lane; wire behavior is exercised using real local HTTP servers in both Android and backend tests.
- Full Android lint/assemble variants were intentionally left to the root verification wave to avoid shared KSP cache races; the focused Android test compiles the changed production Kotlin.

## Architectural Review

- Single responsibility: DTOs model the wire contract, the existing authenticated client owns auth/error mapping, and the backend HTTP client owns downstream proxying.
- Boundary purity: untrusted HTTP payloads are parsed into typed DTOs at Retrofit/Fastjson boundaries.
- No mock/fallback attribution was introduced; unavailable/accumulating data remains partial.
- No health history or token logging was introduced.
