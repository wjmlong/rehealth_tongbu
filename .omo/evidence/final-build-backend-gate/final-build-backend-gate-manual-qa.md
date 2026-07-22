# final-build-backend-gate manual QA

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| AND-GATE-1 | Android build gate | Windows PowerShell / Gradle | `cd Android-apk; JAVA_HOME=D:\\Android_Studio\\jbr; .\\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --max-workers=1 --console=plain` | PASS | `android-gate` |
| BACKEND-GATE-1 | Backend compile + focused tests | WSL bash / Maven reactor | `cd /mnt/d/rehealthAI/backend/jeecg-boot && mvn -pl jeecg-boot-module/jeecg-module-rehealth -am -Dtest='*Attribution*,*CvdFeatureVectorDto*' -Dsurefire.failIfNoSpecifiedTests=false test` | PASS | `backend-focused-tests` |
| STATIC-GATE-1 | Formatting/static gate | Windows PowerShell / git | `git diff --check` | PASS | `static-contract-check` |
| STATIC-GATE-2 | Relative mobile paths + Jackson DTO contract | Repository source scan | `rg` scan of `ReHealthApi.kt`, `CvdFeatureVectorDto.java`, `AttributionResponseDto.java` | PASS | `static-contract-check` |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| ADV-REL-1 | Android release artifact integrity | release packaging | Release task emits a non-empty unsigned APK with reproducible SHA/size evidence | PASS | `android-gate` |
| ADV-PATH-1 | Mobile Retrofit path safety | base URL path resolution | ReHealth mobile routes remain relative; only non-mobile system auth routes are absolute | PASS | `static-contract-check` |
| ADV-DTO-1 | DTO wire compatibility | snake_case/camelCase payloads | Jackson annotations accept canonical snake_case and camelCase aliases | PASS | `backend-focused-tests`, `static-contract-check` |
| ADV-TEST-1 | Focused test selection | no matching tests / false positives | Surefire reports exact matching Attribution/Cvd tests and zero failures | PASS | `backend-focused-tests` |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| `android-gate` | build-log | Android four-task serial gate and APK SHA/size | `.omo/evidence/final-build-backend-gate/android-gate.md` |
| `backend-focused-tests` | test-report | WSL Maven reactor result and Surefire report paths | `.omo/evidence/final-build-backend-gate/backend-focused-tests.md` |
| `static-contract-check` | static-analysis | diff check, relative API paths, Jackson annotations, SHA | `.omo/evidence/final-build-backend-gate/static-contract-check.md` |

