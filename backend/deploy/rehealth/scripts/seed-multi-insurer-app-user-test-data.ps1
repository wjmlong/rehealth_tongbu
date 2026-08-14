[CmdletBinding()]
param(
    [string]$SoftwareContainer = 'rehealth-software-db-1',
    [string]$HardwareContainer = 'rehealth-hardware-db-1',
    [string]$ActorUsername = 'admin',
    [datetime]$AnchorDate = (Get-Date).Date,
    [switch]$SkipOrganizationSeed
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom

if ($ActorUsername -notmatch '^[A-Za-z0-9_.-]+$') {
    throw 'ActorUsername contains unsupported characters.'
}

$deployRoot = Split-Path -Parent $PSScriptRoot
$softwarePasswordPath = Join-Path $deployRoot 'secrets\software_db_password'
$hardwarePasswordPath = Join-Path $deployRoot 'secrets\hardware_db_password'
$organizationSeedPath = Join-Path $PSScriptRoot 'seed-multi-insurer-tenant-test-data.ps1'
$softwareSqlPath = Join-Path $PSScriptRoot 'seed-multi-insurer-app-user-test-data.sql'
$hardwareSqlPath = Join-Path $PSScriptRoot 'seed-multi-insurer-app-user-hardware-test-data.sql'

foreach ($requiredPath in @(
    $softwarePasswordPath,
    $hardwarePasswordPath,
    $organizationSeedPath,
    $softwareSqlPath,
    $hardwareSqlPath
)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required local seed dependency is missing: $requiredPath"
    }
}

$softwarePassword = (Get-Content -Raw -LiteralPath $softwarePasswordPath).Trim()
$hardwarePassword = (Get-Content -Raw -LiteralPath $hardwarePasswordPath).Trim()
if ([string]::IsNullOrWhiteSpace($softwarePassword) -or [string]::IsNullOrWhiteSpace($hardwarePassword)) {
    throw 'Database password files must not be empty.'
}

foreach ($container in @($SoftwareContainer, $HardwareContainer)) {
    $running = & docker inspect --format '{{.State.Running}}' $container 2>$null
    if ($LASTEXITCODE -ne 0 -or ($running | Select-Object -First 1) -ne 'true') {
        throw "Required local database container is not running: $container"
    }
}

if (-not $SkipOrganizationSeed) {
    Write-Host 'Reusing the existing LOCAL_MULTI_INSURER_QA organization and staff seed...'
    & $organizationSeedPath -ContainerName $SoftwareContainer -ActorUsername $ActorUsername
    if ($LASTEXITCODE -ne 0) {
        throw 'The prerequisite insurer organization/staff seed failed.'
    }
}

function Invoke-SoftwareSql {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $Sql | & docker exec -i -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
        mysql --default-character-set=utf8mb4 -N -u rehealth_software -D rehealth_software
    if ($LASTEXITCODE -ne 0) {
        throw "software_db command failed with exit code $LASTEXITCODE."
    }
}

$escapedActor = $ActorUsername.Replace("'", "''")
$preflightSql = @"
SELECT COUNT(*)
FROM sys_user
WHERE username = '$escapedActor' AND status = 1 AND del_flag = 0;

SELECT COUNT(*)
FROM sys_tenant
WHERE id IN (9101, 9102, 9103)
  AND name LIKE '[LOCAL QA]%'
  AND status = 1
  AND del_flag = 0;

SELECT COUNT(*)
FROM sys_user
WHERE username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$'
  AND id <> LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username)));

SELECT COUNT(*)
FROM sys_user
WHERE (phone BETWEEN '00092000001' AND '00092000014'
       OR email REGEXP '^app(0[1-9]|1[0-4])@local\\.qa\\.invalid$')
  AND username NOT REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$';
"@

$preflightOutput = @($preflightSql | & docker exec -i -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -N -u rehealth_software -D rehealth_software)
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to validate the local software database.'
}
$preflightCounts = @($preflightOutput | ForEach-Object { [int]$_.Trim() })
if ($preflightCounts.Count -ne 4 -or $preflightCounts[0] -ne 1) {
    throw "Active local seed actor '$ActorUsername' was not found."
}
if ($preflightCounts[1] -ne 3) {
    throw 'The three LOCAL_MULTI_INSURER_QA tenants are not ready.'
}
if ($preflightCounts[2] -ne 0 -or $preflightCounts[3] -ne 0) {
    throw 'A reserved synthetic APP username, phone, or email is owned by non-seed data.'
}

$anchorDateText = $AnchorDate.ToString('yyyy-MM-dd')
$softwareSql = Get-Content -LiteralPath $softwareSqlPath -Raw -Encoding UTF8
$runtimePrefix = @"
SET @seed_actor = '$escapedActor';
SET @anchor_date = DATE('$anchorDateText');
SET @seed_time = TIMESTAMP('$anchorDateText 10:00:00');
"@

Write-Host "Seeding complete LOCAL_MULTI_INSURER_APP_QA software data for anchor $anchorDateText..."
Invoke-SoftwareSql -Sql ($runtimePrefix + [Environment]::NewLine + $softwareSql)

$hardwareSql = Get-Content -LiteralPath $hardwareSqlPath -Raw -Encoding UTF8
Write-Host 'Seeding Android Debug full-chain compatible 118-day TimescaleDB histories...'
$hardwareSql | & docker exec -i -e "PGPASSWORD=$hardwarePassword" $HardwareContainer `
    psql -X -q -v "anchor_date=$anchorDateText" -U rehealth_hardware -d rehealth_hardware
if ($LASTEXITCODE -ne 0) {
    throw "hardware_db command failed with exit code $LASTEXITCODE."
}

$softwareVerificationSql = @"
SELECT 'app_users', COUNT(*) FROM sys_user
WHERE username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$'
  AND id = LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username)));
SELECT 'app_tenant_memberships', COUNT(*) FROM sys_user_tenant membership
JOIN sys_user app ON app.id = membership.user_id
WHERE app.username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$';
SELECT 'app_roles', COUNT(*) FROM sys_user_role user_role
JOIN sys_user app ON app.id = user_role.user_id
JOIN sys_role role ON role.id = user_role.role_id
WHERE app.username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$'
  AND role.role_code IN ('app_user', 'insurance_service_user') AND user_role.tenant_id = 0;
SELECT 'profiles', COUNT(*) FROM rehealth_patient_profile profile
JOIN sys_user app ON BINARY app.id = BINARY profile.user_id
WHERE app.username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$';
SELECT 'manual_inputs', COUNT(*) FROM rehealth_rhi_manual_health_input input
JOIN sys_user app ON BINARY app.id = BINARY input.user_id
WHERE app.username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$';
SELECT 'device_bindings', COUNT(*) FROM rehealth_device_binding binding
JOIN sys_user app ON BINARY app.id = BINARY binding.user_id
WHERE app.username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$'
  AND binding.id = LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:device-binding:', app.username), 256));
SELECT 'interviews', COUNT(*) FROM rehealth_health_interview WHERE id IN (
    SELECT LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview:', username), 256)) FROM sys_user
    WHERE username REGEXP '^local_app_(910[123]_[0-9][0-9]|shared_0[12])$'
);
SELECT 'behavior_records', COUNT(*) FROM rehealth_behavior_record WHERE model_version = 'LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL';
SELECT 'feature_vectors', COUNT(*) FROM rehealth_cvd_feature_vector WHERE request_id LIKE 'miqa-cvd16-%';
SELECT 'risk_results', COUNT(*) FROM rehealth_cvd_risk_result WHERE request_id LIKE 'miqa-cvd16-%';
SELECT 'attribution_results', COUNT(*) FROM rehealth_attribution_result WHERE request_id LIKE 'miqa-pias-%';
SELECT 'subjects', COUNT(*) FROM rehealth_insurance_subject WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'policies', COUNT(*) FROM rehealth_insurance_policy WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'coverages', COUNT(*) FROM rehealth_insurance_coverage WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'consents', COUNT(*) FROM rehealth_insurance_consent WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'plan_bindings', COUNT(*) FROM rehealth_insurance_plan_binding WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'interventions', COUNT(*) FROM rehealth_insurance_intervention WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'feedback', COUNT(*) FROM rehealth_insurance_intervention_feedback WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'claims', COUNT(*) FROM rehealth_insurance_claim WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'assignments', COUNT(*) FROM rehealth_insurance_subject_manager WHERE source_system = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'assignment_audits', COUNT(*) FROM rehealth_insurance_audit_event WHERE request_id LIKE 'miqa-assignment-%';
"@

$softwareVerificationOutput = @($softwareVerificationSql | & docker exec -i -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -N -u rehealth_software -D rehealth_software)
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to verify complete multi-insurer software fixtures.'
}

$expectedSoftwareCounts = [ordered]@{
    app_users = 14
    app_tenant_memberships = 0
    app_roles = 28
    profiles = 14
    manual_inputs = 14
    device_bindings = 14
    interviews = 14
    behavior_records = 54
    feature_vectors = 420
    risk_results = 420
    attribution_results = 14
    subjects = 18
    policies = 18
    coverages = 18
    consents = 18
    plan_bindings = 18
    interventions = 18
    feedback = 18
    claims = 18
    assignments = 48
    assignment_audits = 48
}

$actualSoftwareCounts = @{}
foreach ($line in $softwareVerificationOutput) {
    $parts = $line -split "`t"
    if ($parts.Count -eq 2) {
        $actualSoftwareCounts[$parts[0]] = [int]$parts[1]
    }
}
foreach ($entry in $expectedSoftwareCounts.GetEnumerator()) {
    if (-not $actualSoftwareCounts.ContainsKey($entry.Key) -or $actualSoftwareCounts[$entry.Key] -ne $entry.Value) {
        throw "Software fixture verification failed for $($entry.Key): expected $($entry.Value), actual $($actualSoftwareCounts[$entry.Key])."
    }
}

$hardwareVerificationSql = @"
SELECT 'upload_batches', count(*) FROM hardware_upload_batch WHERE source = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'measurements', count(*) FROM hardware_measurement WHERE source = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'sleep_sessions', count(*) FROM hardware_sleep_session WHERE source = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'activities', count(*) FROM hardware_activity WHERE source = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'diet_records', count(*) FROM hardware_diet_record WHERE source = 'LOCAL_MULTI_INSURER_APP_QA';
SELECT 'history_windows', count(*) FROM (
    SELECT tenant_id, user_id
    FROM hardware_measurement
    WHERE source = 'LOCAL_MULTI_INSURER_APP_QA'
    GROUP BY tenant_id, user_id
    HAVING count(DISTINCT (observed_at AT TIME ZONE 'Asia/Shanghai')::date) = 118
) complete;
"@

$hardwareVerificationOutput = @($hardwareVerificationSql | & docker exec -i -e "PGPASSWORD=$hardwarePassword" $HardwareContainer `
    psql -X -q -At -F "`t" -U rehealth_hardware -d rehealth_hardware)
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to verify complete multi-insurer hardware fixtures.'
}

$expectedHardwareCounts = [ordered]@{
    upload_batches = 18
    measurements = 21240
    sleep_sessions = 2124
    activities = 2124
    diet_records = 2124
    history_windows = 18
}

$actualHardwareCounts = @{}
foreach ($line in $hardwareVerificationOutput) {
    $parts = $line -split "`t"
    if ($parts.Count -eq 2) {
        $actualHardwareCounts[$parts[0]] = [int]$parts[1]
    }
}
foreach ($entry in $expectedHardwareCounts.GetEnumerator()) {
    if (-not $actualHardwareCounts.ContainsKey($entry.Key) -or $actualHardwareCounts[$entry.Key] -ne $entry.Value) {
        throw "Hardware fixture verification failed for $($entry.Key): expected $($entry.Value), actual $($actualHardwareCounts[$entry.Key])."
    }
}

Write-Output 'Complete multi-insurer APP-user test data seeded successfully.'
Write-Output '  insurers: 9101, 9102, 9103'
Write-Output '  staff: existing LOCAL_MULTI_INSURER_QA administrators/managers/analysts/operators/viewers/auditor'
Write-Output '  APP accounts: 14 unique accounts, 18 insurer service relationships'
Write-Output '  assignments: 48 active staff-to-subject relationships'
Write-Output '  per relationship: policy, coverage, consent, plan binding, intervention, feedback, claim'
Write-Output '  per APP account: profile, interview, complete RHI manual fields, device, 30 CVD-16 risks, PIAS result'
Write-Output '  telemetry: 118 days, 10 measurements/day, sleep, activity, and diet'
Write-Output '  password for all active synthetic staff and APP accounts: 123456'
Write-Output '  source: LOCAL_MULTI_INSURER_APP_QA (local synthetic QA only)'
