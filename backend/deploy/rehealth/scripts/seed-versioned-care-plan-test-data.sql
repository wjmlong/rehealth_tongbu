-- LOCAL_VERSIONED_CARE_PLAN_QA
--
-- Repeatable local-only fixtures for the versioned institution care-plan schema.
-- Prerequisite: seed-multi-insurer-app-user-test-data.ps1 has created the 36
-- LOCAL_MULTI_INSURER_APP_QA insurer-subject relationships for tenants 9101-9103.
-- The PowerShell wrapper performs schema, ownership and collision checks first.

DROP TEMPORARY TABLE IF EXISTS tmp_versioned_care_plan_subject;
CREATE TEMPORARY TABLE tmp_versioned_care_plan_subject (
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rehealth_user_id VARCHAR(64) NOT NULL,
    source_record_id VARCHAR(128) NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64) NOT NULL,
    revision_id VARCHAR(64) NOT NULL,
    content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (tenant_id, subject_ref),
    UNIQUE KEY uk_tmp_versioned_plan (plan_id),
    UNIQUE KEY uk_tmp_versioned_revision (revision_id)
) ENGINE=InnoDB COMMENT='当前会话的版本化计划测试数据对象清单';

INSERT INTO tmp_versioned_care_plan_subject (
    tenant_id, subject_ref, rehealth_user_id, source_record_id, actor_user_id,
    plan_id, revision_id, content_hash
)
SELECT subject.tenant_id,
       subject.subject_ref,
       subject.rehealth_user_id,
       subject.source_record_id,
       actor.id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:plan:', subject.tenant_id, ':', subject.subject_ref), 256)),
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:revision:1:', subject.tenant_id, ':', subject.subject_ref), 256)),
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:content:1:', subject.tenant_id, ':', subject.subject_ref), 256))
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

INSERT INTO rehealth_care_plan (
    id, tenant_id, owner_type, owner_org_ref, subject_ref, rehealth_user_id,
    source_plan_id, status, current_revision_id, draft_revision_id, lock_version,
    created_by, created_at, updated_by, updated_at
)
SELECT plan_id,
       tenant_id,
       'insurance',
       CAST(tenant_id AS CHAR),
       subject_ref,
       rehealth_user_id,
       CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:', tenant_id, ':', source_record_id),
       'active',
       revision_id,
       NULL,
       1,
       actor_user_id,
       @seed_time,
       actor_user_id,
       @seed_time
FROM tmp_versioned_care_plan_subject
ON DUPLICATE KEY UPDATE
    owner_type = VALUES(owner_type),
    owner_org_ref = VALUES(owner_org_ref),
    subject_ref = VALUES(subject_ref),
    rehealth_user_id = VALUES(rehealth_user_id),
    source_plan_id = VALUES(source_plan_id),
    status = VALUES(status),
    current_revision_id = VALUES(current_revision_id),
    draft_revision_id = VALUES(draft_revision_id),
    lock_version = VALUES(lock_version),
    updated_by = VALUES(updated_by),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_care_plan_revision (
    id, tenant_id, plan_id, revision_no, status, title, summary, change_reason,
    content_hash, effective_from, effective_to, published_by, published_at,
    withdrawn_by, withdrawn_at, created_by, created_at, updated_by, updated_at
)
SELECT revision_id,
       tenant_id,
       plan_id,
       1,
       'published',
       CASE MOD(tenant_id, 3)
           WHEN 0 THEN '日常活力提升计划'
           WHEN 1 THEN '心血管健康管理计划'
           ELSE '生活方式改善计划'
       END,
       '通过规律活动、均衡饮食和周期回顾，逐步形成可持续的健康习惯。',
       '首次制定健康管理计划',
       content_hash,
       TIMESTAMP(@anchor_date, '00:00:00'),
       NULL,
       actor_user_id,
       @seed_time,
       NULL,
       NULL,
       actor_user_id,
       @seed_time,
       actor_user_id,
       @seed_time
FROM tmp_versioned_care_plan_subject
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    title = VALUES(title),
    summary = VALUES(summary),
    change_reason = VALUES(change_reason),
    content_hash = VALUES(content_hash),
    effective_from = VALUES(effective_from),
    effective_to = VALUES(effective_to),
    published_by = VALUES(published_by),
    published_at = VALUES(published_at),
    withdrawn_by = VALUES(withdrawn_by),
    withdrawn_at = VALUES(withdrawn_at),
    updated_by = VALUES(updated_by),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_care_plan_item (
    id, tenant_id, plan_id, revision_id, logical_item_id, category, title,
    instructions, schedule_json, scoring_weight, allow_not_applicable,
    display_order, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.tenant_id,
       seeded.plan_id,
       seeded.revision_id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:logical-item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       definition.category,
       definition.title,
       definition.instructions,
       definition.schedule_json,
       definition.scoring_weight,
       1,
       definition.item_no,
       @seed_time
FROM tmp_versioned_care_plan_subject seeded
CROSS JOIN (
    SELECT 1 item_no,
           'exercise' category,
           '规律舒缓活动' title,
           '结合自身状态完成适度活动；如有明显不适，请停止并咨询专业人员。' instructions,
           '{"type":"weekly","days":[1,3,5],"time":"19:00","timezone":"Asia/Shanghai"}' schedule_json,
           1.000 scoring_weight
    UNION ALL
    SELECT 2,
           'nutrition',
           '均衡饮食记录',
           '记录主要餐食并保持饮食多样化，按个人实际情况逐步调整。',
           '{"type":"daily","time":"20:00","timezone":"Asia/Shanghai"}',
           1.000
    UNION ALL
    SELECT 3,
           'follow_up',
           '每周健康回顾',
           '回顾本周执行感受和身体状态，有疑问时联系健康管理人员。',
           '{"type":"weekly","days":[7],"time":"10:00","timezone":"Asia/Shanghai"}',
           0.500
) definition
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    instructions = VALUES(instructions),
    schedule_json = VALUES(schedule_json),
    scoring_weight = VALUES(scoring_weight),
    allow_not_applicable = VALUES(allow_not_applicable),
    display_order = VALUES(display_order),
    created_at = VALUES(created_at);

INSERT INTO rehealth_care_plan_occurrence (
    id, tenant_id, plan_id, revision_id, plan_item_id, logical_item_id,
    subject_ref, scheduled_at, due_at, status, exclusion_reason, created_at, updated_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:occurrence:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.tenant_id,
       seeded.plan_id,
       seeded.revision_id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:logical-item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.subject_ref,
       TIMESTAMP(DATE_ADD(@anchor_date, INTERVAL definition.day_offset DAY), definition.scheduled_time),
       DATE_ADD(TIMESTAMP(DATE_ADD(@anchor_date, INTERVAL definition.day_offset DAY), definition.scheduled_time), INTERVAL 2 HOUR),
       'scheduled',
       NULL,
       @seed_time,
       @seed_time
FROM tmp_versioned_care_plan_subject seeded
CROSS JOIN (
    SELECT 1 item_no, 1 day_offset, '19:00:00' scheduled_time
    UNION ALL SELECT 2, 2, '20:00:00'
    UNION ALL SELECT 3, 3, '10:00:00'
) definition
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref),
    scheduled_at = VALUES(scheduled_at),
    due_at = VALUES(due_at),
    status = VALUES(status),
    exclusion_reason = VALUES(exclusion_reason),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_care_plan_audit_event (
    id, tenant_id, owner_type, actor_user_id, action, plan_id, revision_id,
    before_hash, after_hash, reason, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:audit:', action_seed.action_order, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.tenant_id,
       'insurance',
       seeded.actor_user_id,
       action_seed.action,
       seeded.plan_id,
       seeded.revision_id,
       CASE WHEN action_seed.action = 'publish' THEN seeded.content_hash ELSE NULL END,
       seeded.content_hash,
       '首次制定健康管理计划',
       DATE_ADD(@seed_time, INTERVAL action_seed.offset_second SECOND)
FROM tmp_versioned_care_plan_subject seeded
CROSS JOIN (
    SELECT 1 action_order, 'create_draft' action, 0 offset_second
    UNION ALL SELECT 2, 'publish', 1
) action_seed
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    actor_user_id = VALUES(actor_user_id),
    action = VALUES(action),
    before_hash = VALUES(before_hash),
    after_hash = VALUES(after_hash),
    reason = VALUES(reason),
    created_at = VALUES(created_at);

DROP TEMPORARY TABLE tmp_versioned_care_plan_subject;
