[CmdletBinding()]
param(
    [string]$SoftwareContainer = 'rehealth-software-db-1',
    [datetime]$AnchorDate = (Get-Date).Date
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom

if ($SoftwareContainer -notmatch '^[A-Za-z0-9_.-]+$') {
    throw 'SoftwareContainer contains unsupported characters.'
}

$deployRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Split-Path -Parent (Split-Path -Parent $deployRoot)
$softwarePasswordPath = Join-Path $deployRoot 'secrets\software_db_password'
$sqlPath = Join-Path $backendRoot 'jeecg-boot\jeecg-boot-module\jeecg-module-rehealth\src\main\resources\db\testdata\software\mysql\seed-versioned-care-plan-test-data.sql'
foreach ($requiredPath in @($softwarePasswordPath, $sqlPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required local seed dependency is missing: $requiredPath"
    }
}

$softwarePassword = (Get-Content -Raw -LiteralPath $softwarePasswordPath).Trim()
if ([string]::IsNullOrWhiteSpace($softwarePassword)) {
    throw 'The software database password file must not be empty.'
}

$running = & docker inspect --format '{{.State.Running}}' $SoftwareContainer 2>$null
if ($LASTEXITCODE -ne 0 -or ($running | Select-Object -First 1) -ne 'true') {
    throw "Required local database container is not running: $SoftwareContainer"
}

function Invoke-SoftwareSql {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $result = @($Sql | & docker exec -i -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
        mysql --default-character-set=utf8mb4 -N -u rehealth_software -D rehealth_software)
    if ($LASTEXITCODE -ne 0) {
        throw "software_db command failed with exit code $LASTEXITCODE."
    }
    return $result
}

$expectedSetupSql = @'
DROP TEMPORARY TABLE IF EXISTS tmp_expected_versioned_plan;
CREATE TEMPORARY TABLE tmp_expected_versioned_plan AS
SELECT subject.tenant_id,
       subject.subject_ref,
       subject.rehealth_user_id,
       subject.source_record_id,
       actor.id actor_user_id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:plan:', subject.tenant_id, ':', subject.subject_ref), 256)) plan_id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:revision:1:', subject.tenant_id, ':', subject.subject_ref), 256)) revision_id
FROM rehealth_insurance_subject subject
JOIN sys_user actor
  ON actor.username = CONCAT('local_ins_', subject.tenant_id, '_admin')
 AND actor.status = 1
 AND actor.del_flag = 0
JOIN sys_user_tenant membership
  ON membership.user_id = actor.id
 AND membership.tenant_id = subject.tenant_id
 AND membership.status = 1
WHERE subject.source_system = 'LOCAL_MULTI_INSURER_APP_QA'
  AND subject.tenant_id IN (9101, 9102, 9103)
  AND subject.enrollment_status = 'active';
'@

$preflightSql = $expectedSetupSql + @'

SELECT 'schema_tables', COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'rehealth_care_plan',
      'rehealth_care_plan_revision',
      'rehealth_care_plan_item',
      'rehealth_care_plan_occurrence',
      'rehealth_care_plan_audit_event'
  );
SELECT 'subjects', COUNT(*) FROM tmp_expected_versioned_plan;
SELECT 'tenants', COUNT(DISTINCT tenant_id) FROM tmp_expected_versioned_plan;
SELECT 'actors', COUNT(DISTINCT actor_user_id) FROM tmp_expected_versioned_plan;
SELECT 'plan_collisions', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan plan ON plan.id = expected.plan_id
WHERE plan.tenant_id <> expected.tenant_id
   OR BINARY plan.subject_ref <> BINARY expected.subject_ref
   OR plan.rehealth_user_id <> expected.rehealth_user_id
   OR plan.source_plan_id <> CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:', expected.tenant_id, ':', expected.source_record_id);
SELECT 'revision_collisions', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan_revision revision ON revision.id = expected.revision_id
WHERE revision.tenant_id <> expected.tenant_id
   OR revision.plan_id <> expected.plan_id
   OR revision.revision_no <> 1;
SELECT 'item_collisions', COUNT(*)
FROM tmp_expected_versioned_plan expected
CROSS JOIN (SELECT 1 item_no UNION ALL SELECT 2 UNION ALL SELECT 3) definition
JOIN rehealth_care_plan_item item
  ON item.id = LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:', definition.item_no, ':', expected.tenant_id, ':', expected.subject_ref), 256))
WHERE item.tenant_id <> expected.tenant_id
   OR item.plan_id <> expected.plan_id
   OR item.revision_id <> expected.revision_id
   OR item.display_order <> definition.item_no;
SELECT 'occurrence_collisions', COUNT(*)
FROM tmp_expected_versioned_plan expected
CROSS JOIN (SELECT 1 item_no UNION ALL SELECT 2 UNION ALL SELECT 3) definition
JOIN rehealth_care_plan_occurrence occurrence
  ON occurrence.id = LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:occurrence:', definition.item_no, ':', expected.tenant_id, ':', expected.subject_ref), 256))
WHERE occurrence.tenant_id <> expected.tenant_id
   OR occurrence.plan_id <> expected.plan_id
   OR occurrence.revision_id <> expected.revision_id;
SELECT 'audit_collisions', COUNT(*)
FROM tmp_expected_versioned_plan expected
CROSS JOIN (SELECT 1 action_order, 'create_draft' action UNION ALL SELECT 2, 'publish') definition
JOIN rehealth_care_plan_audit_event audit
  ON audit.id = LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:audit:', definition.action_order, ':', expected.tenant_id, ':', expected.subject_ref), 256))
WHERE audit.tenant_id <> expected.tenant_id
   OR audit.plan_id <> expected.plan_id
   OR audit.revision_id <> expected.revision_id
   OR audit.action <> definition.action;
'@

function ConvertFrom-LabelledCounts {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Lines
    )

    $counts = @{}
    foreach ($line in $Lines) {
        $parts = ([string]$line).Trim() -split "`t"
        if ($parts.Count -ne 2) {
            throw "Unexpected database verification output: $line"
        }
        $counts[$parts[0]] = [int]$parts[1]
    }
    return $counts
}

$preflight = ConvertFrom-LabelledCounts -Lines @(Invoke-SoftwareSql -Sql $preflightSql)
if ($preflight['schema_tables'] -ne 5) {
    throw 'The five V20260819_1 versioned care-plan tables are not available.'
}
if ($preflight['subjects'] -ne 36 -or $preflight['tenants'] -ne 3 -or $preflight['actors'] -ne 3) {
    throw 'Expected 36 active LOCAL_MULTI_INSURER_APP_QA subjects and one active insurer admin in each of tenants 9101-9103. Run seed-multi-insurer-app-user-test-data.ps1 first.'
}
foreach ($collisionLabel in @('plan_collisions', 'revision_collisions', 'item_collisions', 'occurrence_collisions', 'audit_collisions')) {
    if ($preflight[$collisionLabel] -ne 0) {
        throw "A reserved LOCAL_VERSIONED_CARE_PLAN_QA identifier has an incompatible $collisionLabel mapping; refusing to overwrite it."
    }
}

$anchorDateText = $AnchorDate.ToString('yyyy-MM-dd')
$seedSql = Get-Content -LiteralPath $sqlPath -Raw -Encoding UTF8
$runtimePrefix = @"
SET @anchor_date = DATE('$anchorDateText');
SET @seed_time = TIMESTAMP('$anchorDateText 09:00:00');
"@

Write-Host "Seeding LOCAL_VERSIONED_CARE_PLAN_QA data for anchor $anchorDateText..."
[void](Invoke-SoftwareSql -Sql ($runtimePrefix + [Environment]::NewLine + $seedSql))

$verificationSql = $expectedSetupSql + @'

SELECT 'plans', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan plan ON plan.id = expected.plan_id
WHERE plan.tenant_id = expected.tenant_id
  AND BINARY plan.subject_ref = BINARY expected.subject_ref
  AND plan.source_plan_id = CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:', expected.tenant_id, ':', expected.source_record_id)
  AND plan.status = 'active'
  AND plan.current_revision_id = expected.revision_id
  AND plan.draft_revision_id IS NULL;
SELECT 'revisions', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan_revision revision ON revision.id = expected.revision_id
WHERE revision.plan_id = expected.plan_id
  AND revision.status = 'published'
  AND revision.revision_no = 1;
SELECT 'items', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan_item item
  ON item.plan_id = expected.plan_id
 AND item.revision_id = expected.revision_id
WHERE item.id IN (
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:1:', expected.tenant_id, ':', expected.subject_ref), 256)),
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:2:', expected.tenant_id, ':', expected.subject_ref), 256)),
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:3:', expected.tenant_id, ':', expected.subject_ref), 256))
);
SELECT 'occurrences', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan_occurrence occurrence
  ON occurrence.plan_id = expected.plan_id
 AND occurrence.revision_id = expected.revision_id
WHERE occurrence.id IN (
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:occurrence:1:', expected.tenant_id, ':', expected.subject_ref), 256)),
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:occurrence:2:', expected.tenant_id, ':', expected.subject_ref), 256)),
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:occurrence:3:', expected.tenant_id, ':', expected.subject_ref), 256))
)
  AND occurrence.status = 'scheduled';
SELECT 'audit_events', COUNT(*)
FROM tmp_expected_versioned_plan expected
JOIN rehealth_care_plan_audit_event audit
  ON audit.plan_id = expected.plan_id
 AND audit.revision_id = expected.revision_id
WHERE audit.id IN (
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:audit:1:', expected.tenant_id, ':', expected.subject_ref), 256)),
    LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:audit:2:', expected.tenant_id, ':', expected.subject_ref), 256))
)
  AND audit.action IN ('create_draft', 'publish');
SELECT 'chinese_table_comments', COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'rehealth_care_plan',
      'rehealth_care_plan_revision',
      'rehealth_care_plan_item',
      'rehealth_care_plan_occurrence',
      'rehealth_care_plan_audit_event'
  )
  AND CHAR_LENGTH(TABLE_COMMENT) < LENGTH(TABLE_COMMENT);
SELECT 'chinese_column_comments', COUNT(*)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'rehealth_care_plan',
      'rehealth_care_plan_revision',
      'rehealth_care_plan_item',
      'rehealth_care_plan_occurrence',
      'rehealth_care_plan_audit_event'
  )
  AND CHAR_LENGTH(COLUMN_COMMENT) < LENGTH(COLUMN_COMMENT);
SELECT 'blank_column_comments', COUNT(*)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
      'rehealth_care_plan',
      'rehealth_care_plan_revision',
      'rehealth_care_plan_item',
      'rehealth_care_plan_occurrence',
      'rehealth_care_plan_audit_event'
  )
  AND COLUMN_COMMENT = '';
SELECT 'visible_fixture_markers', COUNT(*)
FROM rehealth_care_plan_revision revision
JOIN tmp_expected_versioned_plan expected ON expected.revision_id = revision.id
WHERE revision.title REGEXP '(测试|合成|QA|LOCAL_)'
   OR revision.summary REGEXP '(测试|合成|QA|LOCAL_)';
'@

$verification = ConvertFrom-LabelledCounts -Lines @(Invoke-SoftwareSql -Sql $verificationSql)
$expectedCounts = @{
    plans = 36
    revisions = 36
    items = 108
    occurrences = 108
    audit_events = 72
    chinese_table_comments = 5
    chinese_column_comments = 71
    blank_column_comments = 0
    visible_fixture_markers = 0
}
foreach ($label in $expectedCounts.Keys) {
    if (-not $verification.ContainsKey($label) -or $verification[$label] -ne $expectedCounts[$label]) {
        $actual = if ($verification.ContainsKey($label)) { $verification[$label] } else { '<missing>' }
        throw "Verification failed for ${label}: expected $($expectedCounts[$label]), got $actual."
    }
}

Write-Host 'Versioned care-plan seed verification passed:'
foreach ($label in @('plans', 'revisions', 'items', 'occurrences', 'audit_events', 'chinese_table_comments', 'chinese_column_comments', 'blank_column_comments', 'visible_fixture_markers')) {
    Write-Host "  $label=$($verification[$label])"
}
