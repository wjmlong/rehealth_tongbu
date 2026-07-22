# Mobile contract blocker fix evidence

## Android route contract

- Scenario: Retrofit client receives a MockWebServer base URL ending in `/jeecg-boot/` and invokes `evaluateFeatures`, `getRiskLatest`, `getInterventionsToday`, and `submitInterventionFeedback`.
- Invocation: `cd Android-apk; $env:JAVA_HOME='D:\Android_Studio\jbr'; .\gradlew.bat :app:testDebugUnitTest --tests com.rehealth.genie.network.ReHealthMobileApiRouteContractTest --tests com.rehealth.genie.network.ReHealthMobileApiAttributionTest --tests com.rehealth.genie.network.ReHealthMobileApiMeasurementTest`.
- Observable: Gradle `BUILD SUCCESSFUL`; MockWebServer assertions verify `/jeecg-boot/rehealth/mobile/features/evaluate`, `/jeecg-boot/rehealth/mobile/risk/latest`, `/jeecg-boot/rehealth/mobile/interventions/today`, `/jeecg-boot/rehealth/mobile/interventions/plan-7/feedback`, plus `X-Access-Token: synthetic-test-token`.
- Artifact: `Android-apk/app/build/test-results/testDebugUnitTest/TEST-com.rehealth.genie.network.ReHealthMobileApiRouteContractTest.xml` (and attribution/measurement XMLs in the same directory).

## Jackson DTO binding

- Scenario: Jackson deserializes canonical snake_case and camelCase aliases, then serializes the DTO using canonical snake_case names.
- Invocation: `backend/jeecg-boot/jeecg-boot-module -Dtest=CvdFeatureVectorDtoJacksonBindingTest test` was attempted but cannot run because this environment has neither Maven nor a Maven wrapper; backend Maven gate remains for WSL.
- Observable: Source-level assertions are in `CvdFeatureVectorDtoJacksonBindingTest`; `CvdFeatureVectorDto` retains `@JSONField` and adds `@JsonProperty`/`@JsonAlias` for all six affected fields.
- Artifact: `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/test/java/org/jeecg/modules/rehealth/mobile/dto/CvdFeatureVectorDtoJacksonBindingTest.java`.

## Hygiene

- Invocation: `git diff --check`.
- Observable: exit code 0 (only existing line-ending warnings from the dirty shared worktree).
