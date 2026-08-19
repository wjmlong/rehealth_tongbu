[CmdletBinding()]
param(
    [string]$SoftwareContainer = 'rehealth-software-db-1',
    [string]$HardwareContainer = 'rehealth-hardware-db-1',
    [datetime]$AnchorDate = (Get-Date).Date,
    [switch]$Cleanup
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom

$deployRoot = Split-Path -Parent $PSScriptRoot
$softwarePasswordPath = Join-Path $deployRoot 'secrets\software_db_password'
$hardwarePasswordPath = Join-Path $deployRoot 'secrets\hardware_db_password'
$softwareSeedPath = Join-Path $PSScriptRoot 'seed-medical-workspace-test-data.sql'
$hardwareSeedPath = Join-Path $PSScriptRoot 'seed-medical-workspace-hardware-test-data.sql'
$softwareCleanupPath = Join-Path $PSScriptRoot 'cleanup-medical-workspace-test-data.sql'
$hardwareCleanupPath = Join-Path $PSScriptRoot 'cleanup-medical-workspace-hardware-test-data.sql'

foreach ($requiredPath in @(
    $softwarePasswordPath, $hardwarePasswordPath,
    $softwareSeedPath, $hardwareSeedPath,
    $softwareCleanupPath, $hardwareCleanupPath
)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required medical test-data dependency is missing: $requiredPath"
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

function Invoke-SoftwareSql {
    param([Parameter(Mandatory=$true)][string]$Sql, [switch]$NamesOnly)
    $arguments = @('--default-character-set=utf8mb4', '-u', 'rehealth_software', '-D', 'rehealth_software')
    if ($NamesOnly) { $arguments = @('--default-character-set=utf8mb4', '-N', '-B', '-u', 'rehealth_software', '-D', 'rehealth_software') }
    $result = @($Sql | & docker exec -i -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer mysql @arguments)
    if ($LASTEXITCODE -ne 0) { throw "software_db command failed with exit code $LASTEXITCODE." }
    return $result
}

function Invoke-HardwareSql {
    param([Parameter(Mandatory=$true)][string]$Sql, [switch]$TuplesOnly)
    $arguments = @('-X', '-q', '-v', "anchor_date=$anchorDateText", '-U', 'rehealth_hardware', '-d', 'rehealth_hardware')
    if ($TuplesOnly) { $arguments = @('-X', '-q', '-A', '-t', '-F', "`t", '-v', "anchor_date=$anchorDateText", '-U', 'rehealth_hardware', '-d', 'rehealth_hardware') }
    $result = @($Sql | & docker exec -i -e "PGPASSWORD=$hardwarePassword" $HardwareContainer psql @arguments)
    if ($LASTEXITCODE -ne 0) { throw "hardware_db command failed with exit code $LASTEXITCODE." }
    return $result
}

$anchorDateText = $AnchorDate.ToString('yyyy-MM-dd')

if ($Cleanup) {
    Write-Host 'Removing LOCAL_MEDICAL_TEST_SEED data from hardware_db...'
    Invoke-HardwareSql -Sql (Get-Content -Raw -Encoding UTF8 -LiteralPath $hardwareCleanupPath) | Out-Host
    Write-Host 'Removing LOCAL_MEDICAL_TEST_SEED data from software_db...'
    Invoke-SoftwareSql -Sql (Get-Content -Raw -Encoding UTF8 -LiteralPath $softwareCleanupPath) | Out-Host
    Write-Host 'Medical test data cleanup completed.'
    exit 0
}

$softwareSql = Get-Content -Raw -Encoding UTF8 -LiteralPath $softwareSeedPath
$runtimePrefix = "SET @anchor_date=DATE('$anchorDateText'); SET @seed_time=TIMESTAMP('$anchorDateText 10:00:00');"
Write-Host "Seeding LOCAL_MEDICAL_TEST_SEED software data for anchor $anchorDateText..."
Invoke-SoftwareSql -Sql ($runtimePrefix + [Environment]::NewLine + $softwareSql) | Out-Host

Write-Host 'Seeding 30-day synthetic device histories in hardware_db...'
Invoke-HardwareSql -Sql (Get-Content -Raw -Encoding UTF8 -LiteralPath $hardwareSeedPath) | Out-Host

$softwareVerification = @"
SELECT 'medical_tenants',COUNT(*) FROM sys_tenant WHERE id IN (9261,9262);
SELECT 'staff_accounts',COUNT(*) FROM sys_user WHERE create_by='LOCAL_MEDICAL_TEST_SEED' AND username NOT LIKE 'local_medical_patient_%';
SELECT 'patient_accounts',COUNT(*) FROM sys_user WHERE create_by='LOCAL_MEDICAL_TEST_SEED' AND username LIKE 'local_medical_patient_%';
SELECT 'profiles',COUNT(*) FROM rehealth_patient_profile WHERE JSON_UNQUOTE(JSON_EXTRACT(profile_json,'$.source'))='LOCAL_MEDICAL_TEST_SEED';
SELECT 'device_bindings',COUNT(*) FROM rehealth_device_binding WHERE device_id LIKE 'mhqa-device-%';
SELECT 'mock_risks',COUNT(*) FROM rehealth_cvd_risk_result WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL' AND is_mock=1;
SELECT 'plans',COUNT(*) FROM rehealth_intervention_plan WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL' AND is_mock=1;
SELECT 'feedback',COUNT(*) FROM rehealth_intervention_feedback WHERE idempotency_key LIKE 'mhqa-feedback-%';
SELECT 'rhi_snapshots',COUNT(*) FROM rehealth_rhi_daily_snapshot WHERE calculation_source='LOCAL_MEDICAL_TEST_SEED';
SELECT 'rdi_snapshots',COUNT(*) FROM rehealth_rdi_daily_snapshot WHERE calculation_source='LOCAL_MEDICAL_TEST_SEED' AND is_mock=1;
SELECT 'rdi_contributions',COUNT(*) FROM rehealth_rdi_contribution WHERE source_code='LOCAL_MEDICAL_TEST_SEED';
"@
$expectedSoftware = [ordered]@{
    medical_tenants=2; staff_accounts=4; patient_accounts=24; profiles=24;
    device_bindings=18; mock_risks=20; plans=20; feedback=60;
    rhi_snapshots=140; rdi_snapshots=140; rdi_contributions=420
}
$softwareRows = Invoke-SoftwareSql -Sql $softwareVerification -NamesOnly
foreach ($row in $softwareRows) {
    $parts = $row -split "`t"
    if ($parts.Count -ne 2 -or -not $expectedSoftware.Contains($parts[0]) -or [int]$parts[1] -ne $expectedSoftware[$parts[0]]) {
        throw "Unexpected software verification result: $row"
    }
}
if ($softwareRows.Count -ne $expectedSoftware.Count) { throw 'Software verification returned an incomplete metric set.' }

$hardwareVerification = @"
SELECT 'hardware_batches',COUNT(*) FROM hardware_upload_batch WHERE source='LOCAL_MEDICAL_TEST_SEED';
SELECT 'measurements',COUNT(*) FROM hardware_measurement WHERE source='LOCAL_MEDICAL_TEST_SEED';
SELECT 'sleep_sessions',COUNT(*) FROM hardware_sleep_session WHERE source='LOCAL_MEDICAL_TEST_SEED';
SELECT 'activities',COUNT(*) FROM hardware_activity WHERE source='LOCAL_MEDICAL_TEST_SEED';
"@
$expectedHardware = [ordered]@{ hardware_batches=18; measurements=2700; sleep_sessions=540; activities=540 }
$hardwareRows = Invoke-HardwareSql -Sql $hardwareVerification -TuplesOnly | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
foreach ($row in $hardwareRows) {
    $parts = $row -split "`t"
    if ($parts.Count -ne 2 -or -not $expectedHardware.Contains($parts[0]) -or [int]$parts[1] -ne $expectedHardware[$parts[0]]) {
        throw "Unexpected hardware verification result: $row"
    }
}
if ($hardwareRows.Count -ne $expectedHardware.Count) { throw 'Hardware verification returned an incomplete metric set.' }

Write-Host 'Medical test data is ready and all row-count assertions passed.'
