[CmdletBinding()]
param(
    [string]$ContainerName = "rehealth-software-db-1",
    [string]$ActorUsername = "admin"
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)

if ($ActorUsername -notmatch '^[A-Za-z0-9_.-]+$') {
    throw "ActorUsername contains unsupported characters."
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$deploymentDirectory = Split-Path -Parent $scriptDirectory
$sqlPath = Join-Path $scriptDirectory "seed-multi-insurer-tenant-test-data.sql"
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
    param([Parameter(Mandatory = $true)][string]$Sql)

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

SELECT COUNT(DISTINCT role_code)
FROM sys_role
WHERE role_code IN (
    'insurance_org_admin', 'insurance_department_manager', 'insurer_analyst',
    'insurance_operator', 'insurer_viewer', 'insurer_auditor'
);

SELECT COUNT(*)
FROM sys_tenant
WHERE id IN (9101, 9102, 9103)
  AND name NOT LIKE '[LOCAL QA]%';

SELECT COUNT(*)
FROM sys_user
WHERE (
        username LIKE 'local_ins_91%'
        OR username = 'local_ins_shared_auditor'
        OR phone LIKE '00091%'
        OR email LIKE '%@local.rehealth.invalid'
      )
  AND (
        username NOT LIKE 'local_ins_91%'
        AND username <> 'local_ins_shared_auditor'
        OR id <> LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user:', username)))
      );
"@

$preflightOutput = $preflightSql | docker exec -i -e MYSQL_PWD=$databasePassword $ContainerName `
    mysql --default-character-set=utf8mb4 -N -urehealth_software -D rehealth_software
if ($LASTEXITCODE -ne 0) {
    throw "Unable to validate the local database."
}

$preflightCounts = @($preflightOutput | ForEach-Object { [int]$_.Trim() })
if ($preflightCounts.Count -ne 4 -or $preflightCounts[0] -ne 1) {
    throw "Actor '$ActorUsername' does not exist in the local database."
}
if ($preflightCounts[1] -ne 6) {
    throw "The six required insurance role templates have not been applied."
}
if ($preflightCounts[2] -ne 0) {
    throw "Tenant ID 9101, 9102, or 9103 is already owned by non-QA data."
}
if ($preflightCounts[3] -ne 0) {
    throw "A synthetic username, phone, or email key is already owned by non-QA data."
}

$seedSql = Get-Content -LiteralPath $sqlPath -Raw -Encoding UTF8
$runtimePrefix = @"
SET @seed_actor = '$escapedActor';
SET @seed_time = TIMESTAMP('2026-08-13 10:00:00');
"@

Write-Host "Seeding three LOCAL_MULTI_INSURER_QA tenants and synthetic staff..."
Invoke-SoftwareDbSql -Sql ($runtimePrefix + [Environment]::NewLine + $seedSql)

$verificationSql = @"
SELECT 'tenants', COUNT(*)
FROM sys_tenant
WHERE id IN (9101, 9102, 9103)
  AND name LIKE '[LOCAL QA]%';

SELECT 'unique_users', COUNT(*)
FROM sys_user
WHERE username LIKE 'local_ins_91%'
   OR username = 'local_ins_shared_auditor';

SELECT 'active_memberships', COUNT(*)
FROM sys_user_tenant
WHERE tenant_id IN (9101, 9102, 9103)
  AND status = '1';

SELECT 'invited_memberships', COUNT(*)
FROM sys_user_tenant
WHERE tenant_id IN (9101, 9102, 9103)
  AND status = '5';

SELECT 'business_departments', COUNT(*)
FROM sys_depart
WHERE tenant_id IN (9101, 9102, 9103)
  AND org_category = '2'
  AND description = 'LOCAL_MULTI_INSURER_QA synthetic organization data';

SELECT 'tenant_role_assignments', COUNT(*)
FROM sys_user_role
WHERE tenant_id IN (9101, 9102, 9103)
  AND id IN (
      SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user-role:', sut.tenant_id, ':', su.username, ':', sr.role_code)))
      FROM sys_user_tenant sut
      JOIN sys_user su ON su.id = sut.user_id
      JOIN sys_user_role sur ON sur.user_id = su.id AND sur.tenant_id = sut.tenant_id
      JOIN sys_role sr ON sr.id = sur.role_id
      WHERE sut.tenant_id IN (9101, 9102, 9103)
  );

SELECT 'qa_actor_memberships', COUNT(*)
FROM sys_user_tenant membership
JOIN sys_user actor ON actor.id = membership.user_id
WHERE actor.username = '$escapedActor'
  AND membership.tenant_id IN (9101, 9102, 9103)
  AND membership.status = '1';

SELECT tenant.id, tenant.name,
       COUNT(DISTINCT CASE WHEN membership.status = '1' THEN membership.id END) AS active_members,
       COUNT(DISTINCT CASE WHEN membership.status = '5' THEN membership.id END) AS invited_members,
       COUNT(DISTINCT department.id) AS organization_nodes
FROM sys_tenant tenant
JOIN sys_user_tenant membership ON membership.tenant_id = tenant.id
LEFT JOIN sys_depart department ON department.tenant_id = tenant.id
    AND department.description = 'LOCAL_MULTI_INSURER_QA synthetic organization data'
WHERE tenant.id IN (9101, 9102, 9103)
GROUP BY tenant.id, tenant.name
ORDER BY tenant.id;

SELECT user.username, COUNT(DISTINCT membership.tenant_id) AS tenant_count,
       GROUP_CONCAT(DISTINCT membership.tenant_id ORDER BY membership.tenant_id) AS tenants
FROM sys_user user
JOIN sys_user_tenant membership ON membership.user_id = user.id
WHERE user.username = 'local_ins_shared_auditor'
GROUP BY user.username;
"@

Write-Host "Verification counts:"
Invoke-SoftwareDbSql -Sql $verificationSql
Write-Host "All active synthetic accounts use the local-only password: 123456"
Write-Host "QA actor '$ActorUsername' can switch to tenants 9101, 9102, and 9103 in Jeecg."
Write-Host "LOCAL_MULTI_INSURER_QA seed completed. Re-running this script is safe."
