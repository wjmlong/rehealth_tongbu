[CmdletBinding()]
param(
    [string]$ContainerName = "rehealth-software-db-1",
    [int]$TenantId = 1000,
    [string]$ActorUsername = "admin"
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

if ($TenantId -le 0) {
    throw "TenantId must be a positive integer."
}

if ($ActorUsername -notmatch '^[A-Za-z0-9_.-]+$') {
    throw "ActorUsername contains unsupported characters."
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$deploymentDirectory = Split-Path -Parent $scriptDirectory
$sqlPath = Join-Path $scriptDirectory "seed-insurance-workflow-test-data.sql"
$passwordPath = Join-Path $deploymentDirectory "secrets/software_db_password"

if (-not (Test-Path -LiteralPath $sqlPath -PathType Leaf)) {
    throw "Seed SQL was not found: $sqlPath"
}

if (-not (Test-Path -LiteralPath $passwordPath -PathType Leaf)) {
    throw "Local software_db password file was not found: $passwordPath"
}

$containerState = docker inspect --format '{{.State.Running}}' $ContainerName 2>$null
if ($LASTEXITCODE -ne 0 -or $containerState.Trim() -ne "true") {
    throw "MySQL container '$ContainerName' is not running."
}

$databasePassword = (Get-Content -LiteralPath $passwordPath -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($databasePassword)) {
    throw "Local software_db password file is empty."
}

function Invoke-SoftwareDbSql {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $previousEncoding = [Console]::OutputEncoding
    try {
        [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $Sql | docker exec -i -e MYSQL_PWD=$databasePassword $ContainerName `
            mysql --default-character-set=utf8mb4 -N -urehealth_software -D rehealth_software
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL command failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        [Console]::OutputEncoding = $previousEncoding
    }
}

$escapedActor = $ActorUsername.Replace("'", "''")
$preflightSql = @"
SELECT COUNT(*)
FROM sys_user
WHERE username = '$escapedActor'
  AND del_flag = 0;

SELECT COUNT(*)
FROM sys_user_tenant sut
JOIN sys_user su ON su.id = sut.user_id
WHERE su.username = '$escapedActor'
  AND su.del_flag = 0
  AND sut.tenant_id = $TenantId
  AND sut.status = '1';
"@

$preflightOutput = $preflightSql | docker exec -i -e MYSQL_PWD=$databasePassword $ContainerName `
    mysql --default-character-set=utf8mb4 -N -urehealth_software -D rehealth_software
if ($LASTEXITCODE -ne 0) {
    throw "Unable to validate the local actor and tenant."
}

$preflightCounts = @($preflightOutput | ForEach-Object { [int]$_.Trim() })
if ($preflightCounts.Count -ne 2 -or $preflightCounts[0] -ne 1) {
    throw "Actor '$ActorUsername' does not exist in the local database."
}
if ($preflightCounts[1] -ne 1) {
    throw "Actor '$ActorUsername' is not an active member of tenant $TenantId."
}

$seedSql = Get-Content -LiteralPath $sqlPath -Raw -Encoding UTF8
$runtimePrefix = @"
SET @seed_tenant_id = $TenantId;
SET @seed_actor = '$escapedActor';
"@

Write-Host "Seeding LOCAL_INSURANCE_QA fixtures into tenant $TenantId..."
Invoke-SoftwareDbSql -Sql ($runtimePrefix + [Environment]::NewLine + $seedSql)

$verificationSql = @"
SELECT 'tenant_members', COUNT(*)
FROM sys_user_tenant sut
JOIN sys_user su ON su.id = sut.user_id
WHERE sut.tenant_id = $TenantId
  AND sut.status = '1'
  AND su.username LIKE 'local_insurance_qa_%';

SELECT 'subjects', COUNT(*)
FROM rehealth_insurance_subject
WHERE tenant_id = $TenantId
  AND source_system = 'LOCAL_INSURANCE_QA';

SELECT 'active_policies', COUNT(*)
FROM rehealth_insurance_policy
WHERE tenant_id = $TenantId
  AND source_system = 'LOCAL_INSURANCE_QA'
  AND status = 'active';

SELECT 'paid_claims', COUNT(*)
FROM rehealth_insurance_claim
WHERE tenant_id = $TenantId
  AND source_system = 'LOCAL_INSURANCE_QA'
  AND status = 'paid';

SELECT 'treated', COUNT(*)
FROM rehealth_insurance_intervention
WHERE tenant_id = $TenantId
  AND source_system = 'LOCAL_INSURANCE_QA'
  AND status IN ('active', 'enrolled', 'completed');

SELECT 'controls', COUNT(*)
FROM rehealth_insurance_subject s
WHERE s.tenant_id = $TenantId
  AND s.source_system = 'LOCAL_INSURANCE_QA'
  AND NOT EXISTS (
      SELECT 1
      FROM rehealth_insurance_intervention i
      WHERE i.tenant_id = s.tenant_id
        AND i.subject_ref = s.subject_ref
        AND i.status IN ('active', 'enrolled', 'completed')
  );

SELECT 'draft_study', COUNT(*)
FROM rehealth_insurance_study
WHERE tenant_id = $TenantId
  AND study_no = 'AH-CVD-RWE-2026';

SELECT 'departments', COUNT(*)
FROM sys_depart
WHERE tenant_id = $TenantId
  AND org_code IN ('AH0101', 'AH0102');

SELECT 'managers', COUNT(*)
FROM sys_user
WHERE login_tenant_id = $TenantId
  AND username LIKE 'local_insurance_manager_%';

SELECT 'manager_assignments', COUNT(*)
FROM rehealth_insurance_subject_manager
WHERE tenant_id = $TenantId
  AND source_system = 'LOCAL_INSURANCE_QA'
  AND status = 'active';

SELECT manager_user_id, department_id, COUNT(*) AS assigned_subjects
FROM rehealth_insurance_subject_manager
WHERE tenant_id = $TenantId
  AND source_system = 'LOCAL_INSURANCE_QA'
  AND status = 'active'
GROUP BY manager_user_id, department_id
ORDER BY manager_user_id;
"@

Write-Host "Verification counts:"
Invoke-SoftwareDbSql -Sql $verificationSql
Write-Host "Manager logins: local_insurance_manager_01 / 123456; local_insurance_manager_02 / 123456"
Write-Host "LOCAL_INSURANCE_QA seed completed. Re-running this script is safe."
