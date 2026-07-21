# G3 Privacy Audit Prompt (2026-07-20)

## Context

G3 (privacy/log audit) is a **P0 release blocker** that must be completed before any alpha/beta release. This audit scans release builds for:
1. PII (Personally Identifiable Information) leaks
2. Raw health data in logs
3. Hardcoded secrets (tokens, API keys, passwords)
4. Excessive logging in production builds
5. Insecure data storage

**Current Status**:
- D3 auth implementation includes `SessionStore` with token management
- P0b includes risk evaluation with health data
- E2.1 includes telemetry persistence
- Release build audit: ❌ **NOT COMPLETED** (this task)

**Reference**:
- `ACCEPTANCE_REVIEW_2026-07-10.md` Section 10 (阶段 D)
- `ACCEPTANCE_REVIEW_2026-07-20.md` Section 6 (G3 pending)

---

## Task: G3 Privacy and Log Audit for Release Build

Scan Android APK and backend JAR for privacy risks, PII leaks, excessive logging, and hardcoded secrets.

---

## Prerequisites

### Software Requirements

1. **Android Studio** or **jadx** (for APK decompilation)
   - Download jadx: https://github.com/skylot/jadx/releases
   
2. **ProGuard enabled** (for release build)
   - Verify `app/build.gradle.kts` has `isMinifyEnabled = true`

3. **grep** or **ripgrep** (for log pattern search)
   - Windows: Install via Git Bash or WSL
   - Ripgrep: https://github.com/BurntSushi/ripgrep

4. **Optional: MobSF** (Mobile Security Framework)
   - Docker: `docker run -it -p 8000:8000 opensecurity/mobile-security-framework-mobsf`
   - URL: http://localhost:8000

---

## Working Directory

```
D:\rehealthAI
```

---

## Phase 1: Build Release APK

### Step 1.1: Clean and Build Release

```powershell
cd D:\rehealthAI\Android-apk

# Clean previous builds
.\gradlew.bat clean

# Build release APK (ProGuard enabled)
.\gradlew.bat assembleRelease
```

**Expected Output**:
```
BUILD SUCCESSFUL
```

**Release APK location**:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

**Validation**:
```powershell
# Check file exists and size >10MB
ls -lh app/build/outputs/apk/release/app-release-unsigned.apk
```

### Step 1.2: Verify ProGuard Enabled

Open `app/build.gradle.kts`, find `release` build type:

```kotlin
release {
    isMinifyEnabled = true  // ✅ Must be true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**If `isMinifyEnabled = false`**:
- ❌ STOP - Enable ProGuard first, rebuild
- ProGuard obfuscates code and removes debug logs

---

## Phase 2: Android APK Privacy Audit

### Step 2.1: Decompile APK

Using **jadx**:

```powershell
# Extract jadx
Expand-Archive jadx-1.x.x.zip -DestinationPath .\tools\jadx

# Decompile release APK
.\tools\jadx\bin\jadx.bat -d .\apk-decompiled app\build\outputs\apk\release\app-release-unsigned.apk
```

**Output**: `apk-decompiled/` folder with Java source

**Validation**:
```powershell
ls apk-decompiled/sources/com/rehealth/genie/
```

### Step 2.2: Search for PII Patterns

**Pattern 1: Hardcoded Tokens/Passwords**

```powershell
cd apk-decompiled/sources

# Search for common secret patterns
rg -i "password\s*=\s*\"" .
rg -i "token\s*=\s*\"[A-Za-z0-9]{20,}\"" .
rg -i "api[_-]?key\s*=\s*\"" .
rg -i "secret\s*=\s*\"" .
rg -i "private[_-]?key\s*=\s*\"" .
```

**Expected Result**: ❌ **No matches** (all secrets should be in secure storage or env vars)

**If found**:
- ❌ CRITICAL - Hardcoded secrets in release build
- Must fix: Move to `BuildConfig`, `local.properties`, or remote config

**Record**:
```
Hardcoded secrets: ✅ None found / ❌ Found: [list files]
```

---

**Pattern 2: Phone Numbers / Email Addresses**

```powershell
rg -i "phone\s*=\s*\"\+?[0-9]{10,}" .
rg -i "email\s*=\s*\"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\"" .
rg -i "\+86[0-9]{11}" .  # Chinese phone numbers
rg -i "1[3-9][0-9]{9}" .  # Mobile numbers
```

**Expected Result**: ❌ **No matches** (only placeholder/test data acceptable like "test@example.com")

**If found**:
- Check if it's real user data or test placeholder
- Real user data = ❌ CRITICAL leak

**Record**:
```
Phone/Email in code: ✅ None found / ⚠️ Test data only / ❌ Real data found
```

---

**Pattern 3: Health Data in Logs**

```powershell
rg -i "Log\.[dwie]\(.*heart[_-]?rate" .
rg -i "Log\.[dwie]\(.*spo2" .
rg -i "Log\.[dwie]\(.*blood[_-]?pressure" .
rg -i "Log\.[dwie]\(.*temperature" .
rg -i "Log\.[dwie]\(.*weight" .
rg -i "Log\.[dwie]\(.*glucose" .
```

**Expected Result**: ❌ **No matches** (ProGuard should strip `Log.d/Log.v`)

**Acceptable**: 
- `Log.e()` for error messages WITHOUT raw values
- Example: ✅ `Log.e("ReHealth", "Failed to sync telemetry")`
- Example: ❌ `Log.d("ReHealth", "Heart rate: $heartRate bpm")`

**If found**:
- Check if it's `Log.d` (debug, should be stripped by ProGuard)
- If `Log.e` or `Log.w` with health data = ❌ CRITICAL

**Record**:
```
Health data in logs: ✅ None found / ⚠️ Debug only (ProGuard strips) / ❌ Production logs found
```

---

**Pattern 4: User Identity in Logs**

```powershell
rg -i "Log\.[dwie]\(.*username" .
rg -i "Log\.[dwie]\(.*user[_-]?id" .
rg -i "Log\.[dwie]\(.*patient[_-]?id" .
rg -i "Log\.[dwie]\(.*login" .
```

**Expected Result**: ❌ **No username/password in logs**

**Acceptable**:
- Anonymized IDs: ✅ `Log.d("ReHealth", "User: ${userId.hashCode()}")`
- Session IDs: ✅ `Log.d("ReHealth", "Session started")`

**If found with raw username/ID**:
- ⚠️ If debug log (ProGuard strips) = acceptable
- ❌ If production log = CRITICAL

**Record**:
```
User identity in logs: ✅ None or anonymized / ❌ Raw IDs found
```

---

### Step 2.3: Check SharedPreferences Security

Search for insecure storage:

```powershell
rg "SharedPreferences" . | rg -v "Encrypted"
```

**Expected Result**:
- ✅ `EncryptedSharedPreferences` used for tokens/passwords
- ⚠️ Plain `SharedPreferences` acceptable for non-sensitive data (UI state, preferences)

**Check SessionStore implementation**:
```powershell
rg "class SessionStore" -A 20 .
```

**Must use**:
- ✅ `EncryptedSharedPreferences` for token storage
- OR ✅ Android Keystore for encryption keys

**If plain SharedPreferences for tokens**:
- ❌ CRITICAL - Tokens readable by rooted device or ADB backup

**Record**:
```
Token storage: ✅ Encrypted / ❌ Plain SharedPreferences
```

---

### Step 2.4: Check Network Security

Search for insecure HTTP:

```powershell
rg "http://" . | rg -v "localhost\|127.0.0.1\|example.com"
```

**Expected Result**: ❌ **No production HTTP URLs** (only HTTPS)

**Acceptable**:
- ✅ `http://localhost` or `http://127.0.0.1` (dev only)
- ✅ `http://example.com` (placeholder)

**If found**:
- ❌ CRITICAL if production backend URL is `http://`
- Must use `https://` for all remote APIs

**Record**:
```
HTTP URLs: ✅ None or dev/example only / ❌ Production HTTP found
```

---

### Step 2.5: Automated Scan with MobSF (Optional)

If MobSF installed:

1. **Start MobSF**:
   ```powershell
   docker run -it -p 8000:8000 opensecurity/mobile-security-framework-mobsf
   ```

2. **Upload APK**: http://localhost:8000
   - Upload `app-release-unsigned.apk`

3. **Review Scan Results**:
   - Security Score
   - Permissions analysis
   - Code analysis (hardcoded secrets, insecure APIs)
   - Network security

4. **Export Report**: Download PDF for evidence

**Record**:
```
MobSF Score: ___ / 100
Critical issues: ___
High issues: ___
Report: mobsf_report_YYYYMMDD.pdf
```

---

## Phase 3: Backend JAR Privacy Audit

### Step 3.1: Build Backend JAR

```powershell
cd D:\rehealthAI\backend

# Build backend (skip tests for speed)
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-start -am package -DskipTests
```

**JAR location**:
```
jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.9.2.jar
```

### Step 3.2: Extract JAR

```powershell
# Create extraction folder
mkdir backend-decompiled

# Extract JAR
cd backend-decompiled
jar xf ../jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.9.2.jar
```

**Validation**:
```powershell
ls BOOT-INF/classes/org/jeecg/modules/rehealth/
```

### Step 3.3: Search for PII in Backend

**Pattern 1: Database Passwords in Config**

```powershell
cd BOOT-INF/classes

# Search application.yml and application-prod.yml
rg "password:" . -A 1 | rg -v "\$\{" | rg -v "ENC\("
```

**Expected Result**:
- ✅ `password: ${DB_PASSWORD}` (environment variable)
- ✅ `password: ENC(...)` (Jasypt encrypted)
- ❌ `password: mypassword123` (hardcoded)

**If hardcoded password found**:
- ❌ CRITICAL - Must use env vars or encrypted properties

**Record**:
```
DB passwords: ✅ Env vars or encrypted / ❌ Hardcoded
```

---

**Pattern 2: API Tokens in Code**

```powershell
rg "token\s*=\s*\"[A-Za-z0-9]{20,}\"" .
rg "api[_-]?key\s*=\s*\"[A-Za-z0-9]{20,}\"" .
rg "X-Access-Token.*=.*\"[A-Za-z0-9]{20,}\"" .
```

**Expected Result**: ❌ **No hardcoded tokens**

**Acceptable**:
- Test tokens in test resources: ✅ (if clearly marked as test)

**If found in production code**:
- ❌ CRITICAL - Tokens must be in secure config or database

**Record**:
```
API tokens: ✅ None or test only / ❌ Production tokens found
```

---

**Pattern 3: Telemetry Logging**

```powershell
# Search ReHealth module specifically
rg "log\.(debug|info|warn)\(.*heart" org/jeecg/modules/rehealth/
rg "log\.(debug|info|warn)\(.*spo2" org/jeecg/modules/rehealth/
rg "log\.(debug|info|warn)\(.*telemetry" org/jeecg/modules/rehealth/
```

**Expected Result**:
- ❌ No raw telemetry data in logs
- ✅ Aggregated/anonymized logs only

**Acceptable**:
- ✅ `log.info("Processed {} telemetry records", count)`
- ❌ `log.debug("Heart rate: {}", heartRate)`

**If raw health data found**:
- Check log level: `debug` acceptable (disabled in prod)
- `info/warn/error` with raw data = ❌ CRITICAL

**Record**:
```
Telemetry logging: ✅ Aggregated only / ⚠️ Debug logs (disabled in prod) / ❌ Raw data in info/warn
```

---

**Pattern 4: User Credentials in Logs**

```powershell
rg "log\.(debug|info|warn|error)\(.*password" org/jeecg/
rg "log\.(debug|info|warn|error)\(.*token" org/jeecg/modules/rehealth/
rg "log\.(debug|info|warn|error)\(.*username.*password" org/jeecg/
```

**Expected Result**: ❌ **No passwords/tokens in logs**

**Check E1.2 auth contract implementation**:
- Per E1.2 status: "清理登录/Shiro 中的密码、token、username 普通日志"
- Should be already cleaned

**If found**:
- ❌ CRITICAL - Passwords/tokens must never be logged

**Record**:
```
Credentials in logs: ✅ None found / ❌ Found: [list files]
```

---

### Step 3.4: Check SQL Injection Risks

Search for string concatenation in SQL:

```powershell
rg "\"SELECT.*\+\s*" org/jeecg/modules/rehealth/ -A 2
rg "\"INSERT.*\+\s*" org/jeecg/modules/rehealth/ -A 2
rg "\"UPDATE.*\+\s*" org/jeecg/modules/rehealth/ -A 2
```

**Expected Result**: ❌ **No string concatenation in SQL**

**Must use**:
- ✅ Prepared statements: `jdbcTemplate.query(sql, params)`
- ✅ MyBatis parameterized queries: `#{userId}`

**If found**:
- ❌ CRITICAL SQL injection risk

**Record**:
```
SQL injection risks: ✅ None (parameterized only) / ❌ Found: [list files]
```

---

## Phase 4: Configuration Review

### Step 4.1: Check application-prod.yml

```powershell
cd D:\rehealthAI\backend\jeecg-boot\jeecg-system-start\src\main\resources

cat application-prod.yml
```

**Check**:
1. ✅ Database password: `${DB_PASSWORD}` or `ENC(...)`
2. ✅ Redis password: `${REDIS_PASSWORD}` or `ENC(...)`
3. ✅ JWT secret: `${JWT_SECRET}` or strong random value (not "secret123")
4. ✅ Log level: `INFO` or `WARN` (not `DEBUG`)
5. ✅ Swagger disabled: `swagger.production: false` or removed

**Record**:
```
application-prod.yml:
- DB password: ✅ Secure / ❌ Hardcoded
- Redis password: ✅ Secure / ❌ Hardcoded
- JWT secret: ✅ Strong / ❌ Weak
- Log level: ✅ INFO/WARN / ❌ DEBUG
- Swagger: ✅ Disabled / ❌ Enabled
```

### Step 4.2: Check ProGuard Rules (Android)

```powershell
cd D:\rehealthAI\Android-apk

cat app/proguard-rules.pro
```

**Check for dangerous `-dontshrink` or `-dontobfuscate`**:

**Expected**:
- ❌ No `-dontshrink` (keeps all code, including debug)
- ❌ No `-dontobfuscate` (code readable by decompiler)
- ✅ `-keepattributes SourceFile,LineNumberTable` (for crash reports)
- ✅ Specific `-keep` rules for libraries only

**If found**:
- ❌ CRITICAL if entire app is not obfuscated

**Record**:
```
ProGuard rules: ✅ Secure / ❌ Disables obfuscation
```

---

## Phase 5: Results Documentation

### Step 5.1: Create Privacy Audit Report

Create `codex-runs/2026-07-20/G3_privacy_audit_report.md`:

```markdown
# G3 Privacy Audit Report

Date: [YYYY-MM-DD]
Auditor: [Name]
Android APK: app-release-unsigned.apk (vX.X.X)
Backend JAR: jeecg-system-start-3.9.2.jar

## Executive Summary

Overall Risk Level: ✅ LOW / ⚠️ MEDIUM / ❌ HIGH

Critical Issues: [N]
High Issues: [N]
Medium Issues: [N]
Low Issues: [N]

## Android APK Audit

### 2.2 PII Pattern Search

| Pattern | Result | Details |
|---------|--------|---------|
| Hardcoded secrets | ✅ / ❌ | |
| Phone/Email | ✅ / ❌ | |
| Health data in logs | ✅ / ❌ | |
| User identity in logs | ✅ / ❌ | |

### 2.3 Storage Security

Token storage: ✅ Encrypted / ❌ Plain

### 2.4 Network Security

HTTP URLs: ✅ None / ❌ Found: [list]

### 2.5 MobSF Scan (if applicable)

Score: ___ / 100
Report: [attach PDF]

## Backend JAR Audit

### 3.3 PII in Backend

| Pattern | Result | Details |
|---------|--------|---------|
| DB passwords | ✅ / ❌ | |
| API tokens | ✅ / ❌ | |
| Telemetry logging | ✅ / ❌ | |
| Credentials in logs | ✅ / ❌ | |

### 3.4 SQL Injection

SQL injection risks: ✅ None / ❌ Found: [list]

## Configuration Review

application-prod.yml: ✅ Secure / ⚠️ Issues: [list]
ProGuard rules: ✅ Secure / ❌ Disabled

## Critical Findings

[List all CRITICAL issues found]

Example:
1. ❌ CRITICAL: Hardcoded API token in `NetworkConfig.kt`
2. ❌ CRITICAL: Plain text password in `application-prod.yml`

## Recommendations

[List required fixes before release]

Example:
1. Move API token to BuildConfig or remote config
2. Use ENC() for database password in application-prod.yml
3. Enable ProGuard obfuscation for release build

## Conclusion

✅ PASS - No critical issues, ready for release
⚠️ PASS WITH FIXES - Critical issues found, must fix before release
❌ FAIL - Multiple critical issues, release blocked
```

### Step 5.2: Update Acceptance Review

Update `ACCEPTANCE_REVIEW_2026-07-20.md`:

Find **Section 2. Release Blockers**:

```markdown
5. **G3 Privacy Audit** ✅ **RESOLVED** / ⚠️ **PASS WITH FIXES** / ❌ **BLOCKED**
   - Evidence: `codex-runs/2026-07-20/G3_privacy_audit_report.md`
   - Android APK: ✅ / ⚠️ / ❌
   - Backend JAR: ✅ / ⚠️ / ❌
   - Critical issues: [N]
   - Status: [PASS/PASS WITH FIXES/FAIL]
```

### Step 5.3: Commit Report

```powershell
git add codex-runs/2026-07-20/G3_privacy_audit_report.md
git commit -m "audit(privacy): G3 privacy and log audit for release build

Scanned Android release APK and backend JAR for privacy risks.

Results:
- Hardcoded secrets: ✅ None / ❌ Found
- PII in logs: ✅ None / ❌ Found
- Health data in logs: ✅ None / ❌ Found
- Token storage: ✅ Encrypted / ❌ Plain
- SQL injection: ✅ None / ❌ Found

[PASS/PASS WITH FIXES/FAIL]

Ref: ACCEPTANCE_REVIEW_2026-07-20.md G3 privacy audit blocker
"
```

---

## Definition of Done

- [ ] Release APK built with ProGuard enabled
- [ ] APK decompiled and searched for PII patterns
- [ ] No hardcoded secrets found
- [ ] No raw health data in logs
- [ ] Token storage uses EncryptedSharedPreferences
- [ ] No production HTTP URLs
- [ ] Backend JAR extracted and searched
- [ ] No database passwords hardcoded
- [ ] No telemetry data in production logs
- [ ] No SQL injection risks
- [ ] application-prod.yml reviewed (env vars, log level, swagger disabled)
- [ ] ProGuard rules reviewed (obfuscation enabled)
- [ ] Privacy audit report created
- [ ] Acceptance review updated
- [ ] Git commit with report

---

## Success Criteria

**PASS** (ready for release):
- ✅ No critical issues found
- ✅ All secrets use env vars or encrypted storage
- ✅ No PII/health data in production logs
- ✅ ProGuard enabled and configured correctly
- ✅ Network uses HTTPS for production
- ✅ No SQL injection risks

**PASS WITH FIXES** (must fix before release):
- ⚠️ 1-2 critical issues found (fixable in <1 day)
- Examples: Hardcoded test token, one debug log with health data

**FAIL** (release blocked):
- ❌ ≥3 critical issues
- ❌ Hardcoded production passwords/tokens
- ❌ Raw health data in production logs
- ❌ ProGuard disabled
- ❌ SQL injection risks

---

## Troubleshooting

### Issue: ProGuard removes too much code

**Symptom**: Release APK crashes but debug works

**Solution**: Add `-keep` rules in `proguard-rules.pro`:
```
-keep class com.rehealth.genie.network.** { *; }
-keep class com.rehealth.genie.data.** { *; }
```

### Issue: Cannot decompile APK

**Solution**: Try alternative tools:
- **JADX-GUI**: https://github.com/skylot/jadx
- **APKTool**: https://ibotpeaches.github.io/Apktool/
- **Online**: http://www.javadecompilers.com/apk

### Issue: MobSF scan takes too long

**Solution**: Skip MobSF, manual patterns are sufficient for MVP

---

**End of G3 Privacy Audit Prompt**
