# G3 Release Privacy and Log Audit

Date: 2026-07-23

Branch: `codex/real-device`

Scope: Android release APK, JeecgBoot executable JAR, release configuration, ReHealth SQL/logging source

## Executive summary

**Verdict: PASS WITH DEPLOYMENT CONDITIONS**

- Critical findings after remediation: **0**
- High findings after remediation: **0**
- Android release is minified, non-debuggable, blocks cleartext traffic and does not allow backup.
- Local provider credentials, Jeecg signing secrets and emulator backend URLs are absent from the release APK.
- JeecgBoot production credential fields are environment-backed or empty; no credential literal remains in the packaged production YAML.
- Runtime logcat QA, MobSF and signed-release installation were not performed because MuMu is not installed and no physical MR11 device is available.

This resolves the code-level G3 release blocker. Deployment remains blocked until the real HTTPS backend URL and required backend secrets are supplied through the deployment environment, the APK is signed, and runtime QA is executed.

## Remediation applied

### Android

- Enabled R8 optimization/minification for `release`.
- Added only the MRD reflection/callback keep rule required by the vendor SDK.
- Removed raw BLE frame logging from `BleAdapter`.
- Removed feedback row/intervention identifiers from worker logs.
- Stripped all `android.util.Log` calls from release bytecode.
- Forced release backend configuration to HTTPS. The fail-closed default is `https://api.rehealth.invalid/`.
- Forced `DEEPSEEK_API_KEY` and `JEECG_SIGN_SECRET` to empty values in release builds; debug builds may still use local-only configuration.

### Backend

- Externalized 18 production credential fields, including database, Redis, mail, Druid, Jeecg signature, search/map providers, XXL Job, Knife4j and OAuth secrets.
- `DB_PASSWORD` and `JEECG_SIGNATURE_SECRET` have no insecure fallback and therefore fail closed when missing.
- Optional integrations use empty defaults and remain unavailable until explicitly configured.

## Android artifact evidence

Artifact: `Android-apk/app/build/outputs/apk/release/app-release-unsigned.apk`

| Check | Result |
|---|---|
| SHA-256 | `5dee2cc61ccf286b8450bfac1aec7182cb3c41603a106a7b553d443eac02b1fc` |
| Size | 2,936,052 bytes |
| Application ID | `com.rehealth.genie` |
| Debuggable | `false` |
| Cleartext traffic | `false` |
| Android backup | `false` |
| Local configuration secrets | None found in DEX |
| Emulator backend URL | Absent |
| Fail-closed HTTPS URL | Present |
| Sensitive logs in `BleAdapter` / `MeasurementSyncWorker` bytecode | None |
| Token storage | `EncryptedSharedPreferences`, AES-256 backed by Android Keystore |

The APK requests the expected BLE, foreground service, notification, network and boot permissions. No unrelated high-risk permission was introduced by this remediation.

## Backend artifact evidence

Artifact: `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.9.2.jar`

| Check | Result |
|---|---|
| SHA-256 | `09087770dcc8464e5b54e9e836391950bf44dbaada37d6dcbdb1f8cfb3218b8b` |
| Size | 323,690,656 bytes |
| Packaged ReHealth classes | 57 |
| Hardcoded credential fields in packaged `application-prod.yml` | 0 |
| Raw health-log class matches in ReHealth module | 0 |
| Dynamic SQL concatenation matches in ReHealth source | 0 |
| Knife4j production shielding | `true` |

## Commands run

```text
./gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --console=plain
BUILD SUCCESSFUL (102 tasks)

mvn.cmd -pl jeecg-module-system/jeecg-system-start -am -DskipTests package
BUILD SUCCESS (11/11 reactor modules)
```

Additional artifact checks used Android `apkanalyzer`, direct DEX inspection and in-memory ZIP/JAR inspection. No extracted audit directory was left behind.

## Deployment conditions and residual risk

1. Configure `REHEALTH_RELEASE_API_BASE_URL` (or `rehealth.release.api.base.url`) with the real HTTPS JeecgBoot endpoint. The placeholder intentionally cannot reach production.
2. Supply at minimum `DB_PASSWORD` and `JEECG_SIGNATURE_SECRET` to the backend runtime. Configure optional provider secrets only when those integrations are enabled.
3. Keep DeepSeek provider credentials behind a backend proxy before enabling public AI chat; release APKs intentionally contain no provider key.
4. Produce and install a signed release artifact, then capture logcat during login, BLE scan/connect, collection, upload, scoring, attribution and feedback.
5. Run physical MR11 QA when hardware is available. MuMu cannot prove BLE hardware behavior.
6. MobSF was not run; it is optional in the G3 prompt and remains a recommended CI hardening step.

## Conclusion

The static Android and backend release privacy gate passes after remediation. This report does not claim production deployment readiness or physical MR11 validation.
