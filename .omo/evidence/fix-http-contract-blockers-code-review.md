# HTTP Contract Blockers Code Review

## Verdict

- `codeQualityStatus`: **BLOCK**
- `recommendation`: **REQUEST_CHANGES**
- Overall: **FAIL** — both requested HTTP blockers remain open in at least one production path.
- Review mode: read-only; no production or test code was changed.
- Attempt lookup: `omo ulw-loop status --json` was unavailable because the `omo` executable is not installed/on `PATH`; this report therefore uses the required fallback evidence path.

## Scope Reviewed

- Android Retrofit base URL/path resolution, Moshi request/response DTOs, and the two new MockWebServer contract tests.
- Backend Spring MVC Jackson configuration, attribution request/response DTOs, CVD feature DTO, and `HttpModelServiceClient` Fastjson boundary.
- Repository-wide references to `AttributionResponseDto`, including JavaBean getters and direct-field consumers.
- Existing evidence under `.omo/evidence/fix-http-contract-blockers-subagent/` and `.omo/evidence/restore-backend-response-dto/`; both were treated as untrusted and independently checked.

## CRITICAL

None.

## HIGH

### 1. Android canonical ReHealth routes still discard the JeecgBoot context path

- `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthMobileApi.kt:60` builds Retrofit with a base such as `http://10.0.2.2:8080/jeecg-boot/`.
- The newly added measurement and attribution methods correctly use relative paths at `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt:52` and `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt:57`. Their wire tests prove requests resolve to `/jeecg-boot/rehealth/mobile/...`.
- However, the canonical risk and related production methods still use leading-slash absolute paths at `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt:31`, `:34`, `:37`, `:40`, `:43`, and `:46`. OkHttp/Retrofit resolves these from the host root, producing `/rehealth/mobile/...` and dropping `/jeecg-boot`.
- This is a correctness blocker, not documentation-only risk: `RemotePhmService.evaluateFeatures` calls the affected `evaluateFeatures` route at `Android-apk/app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt:91`.
- Required before approval: make every backend-relative `rehealth/mobile/...` Retrofit path preserve the configured context path and add a MockWebServer assertion using a `/jeecg-boot/` base for at least the canonical feature-evaluation route (preferably all affected routes).

### 2. Spring Jackson still cannot bind the Android snake_case CVD feature fields

- Android deliberately emits `fasting_glucose`, `total_cholesterol`, `exercise_days`, `diabetes_history`, `hypertension_history`, and `family_history` from `Android-apk/app/src/main/java/com/rehealth/genie/network/dto/FeatureEvaluationDtos.kt:35`, `:36`, `:40`, `:43`, `:44`, and `:45`.
- Spring MVC installs a Jackson `MappingJackson2HttpMessageConverter` with a default-naming `ObjectMapper` at `backend/jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/config/WebMvcConfiguration.java:108` and `:118`; it does not configure `SNAKE_CASE`.
- `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/CvdFeatureVectorDto.java:21`, `:23`, `:28`, `:32`, `:34`, and `:36` add only Fastjson `@JSONField` annotations. Jackson does not consume those aliases, so the six fields remain null on Android -> controller binding; unknown fields are silently ignored by the configured mapper.
- `HttpModelServiceClient` serializes outbound model requests with Fastjson at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/model/impl/HttpModelServiceClient.java:104`, so its Fastjson mapping is correct but cannot recover values already lost at the Spring/Jackson boundary.
- Required before approval: add Jackson property/alias mappings (or an equivalent correctly scoped naming strategy) for all six fields and add a real Jackson/Spring request-binding regression test that starts from the Android wire shape and asserts populated Java values plus the downstream Fastjson wire shape.

## MEDIUM

None beyond the missing boundary tests already included in the HIGH findings. No deletion-only, removal-verification, tautological, prompt-text, or implementation-constant-mirroring tests were found in the reviewed scope.

## LOW

- The class comment at `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt:26` says all endpoints are relative, while six methods are absolute. This is misleading but subsumed by HIGH finding 1.
- The explanatory comment at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/CvdFeatureVectorDto.java:14` claims the Fastjson annotations fix App -> backend inbound parsing, but the actual inbound converter is Jackson. This should be corrected with the production fix.

## Passing Contract Checks

- **Attribution Jackson request binding: PASS.** `AttributionEventsRequestDto` carries both Jackson and Fastjson mappings for `risk_history`, `forecast_days`, `Y`, and `Z` at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/AttributionEventsRequestDto.java:11` through `:29`.
- **Attribution response binding: PASS.** `AttributionResponseDto` supplies Jackson `@JsonProperty`/`@JsonAlias` and Fastjson `@JSONField` across the top-level and nested snake_case response fields at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/AttributionResponseDto.java:10` through `:53`.
- **HttpModelServiceClient attribution mapping: PASS.** The client posts to `/api/pias/v2/attribute/individual`, uses Fastjson on both request and response, unwraps the envelope, and preserves the rich nested response at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/model/impl/HttpModelServiceClient.java:66` through `:85` and `:100` through `:115`.
- **Android attribution Moshi mapping: PASS.** Kotlin property names exactly match `risk_history`, `forecast_days`, `Y`, `Z`, and the nested response keys in `Android-apk/app/src/main/java/com/rehealth/genie/network/dto/AttributionDto.kt:14` through `:93`; `RemotePhmService` consumes those exact properties at `Android-apk/app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt:166` through `:208`.
- **AttributionResponseDto compatibility: PASS for repository consumers.** All top-level fields have getters/setters at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/AttributionResponseDto.java:62` through `:87`. Repository consumers use direct public-field access; no legacy getter/setter call sites or nested JavaBean getter consumers were found. The pre-change DTO exposed only four public fields and no getters, so the nested DTOs do not remove an existing getter API.

## Verification Evidence

- Android focused command: `gradlew.bat --no-daemon testDebugUnitTest --tests com.rehealth.genie.network.ReHealthMobileApiAttributionTest --tests com.rehealth.genie.network.ReHealthMobileApiMeasurementTest` — **BUILD SUCCESSFUL**; production and test Kotlin compilation completed and the task passed/up-to-date.
- The Android tests assert actual request paths, authentication headers, request JSON keys, and response deserialization at `Android-apk/app/src/test/java/com/rehealth/genie/network/ReHealthMobileApiAttributionTest.kt:38` through `:49` and `Android-apk/app/src/test/java/com/rehealth/genie/network/ReHealthMobileApiMeasurementTest.kt:41` through `:48`.
- Backend build/tests: **not run** because neither `mvn` nor `mvnw(.cmd)` is available. The prior evidence file reports the same limitation and therefore is not proof of compile success.
- `git diff --check` on the reviewed contract files: **PASS** (line-ending warnings only; no whitespace errors).

## Skill-Perspective Check

- `remove-ai-slops`: **ran**. The reviewed tests are behavior-oriented wire tests, not deletion-only/tautological/constant-mirroring tests. The misleading serializer comment and missing true Jackson-boundary test violate the perspective and contribute to HIGH finding 2.
- `programming`: **ran**. The attribution DTO boundary is explicitly typed and correctly mapped. The CVD boundary violates parse-at-the-boundary discipline because the production parser is Jackson while the DTO is annotated only for Fastjson; the existing tests do not exercise that actual boundary.
- `rehealth-android-mvp`: **ran**. Review respected the Android -> backend -> model-service boundary and did not reopen unrelated telemetry/BLE work.

## Blockers

1. Fix all `/rehealth/mobile/...` Retrofit annotations that currently start with `/`, and prove canonical feature evaluation retains `/jeecg-boot/` with a wire-level test.
2. Add Jackson mappings for all six snake_case CVD fields and prove Android JSON binds to non-null Java values before `HttpModelServiceClient` serializes the model-service request.

