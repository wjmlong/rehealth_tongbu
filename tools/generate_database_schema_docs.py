#!/usr/bin/env python3
"""Generate the ReHealth database structure delivery documentation.

The generator reads only schema/catalog metadata from the local development
MySQL and TimescaleDB containers plus Room's exported schema JSON. It never
queries business column values and never writes database credentials.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import date
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
ROOM_SCHEMA = (
    ROOT
    / "Android-apk"
    / "app"
    / "schemas"
    / "com.rehealth.genie.data.AppDatabase"
    / "16.json"
)
MYSQL_CONTAINER = "rehealth-software-db-1"
POSTGRES_CONTAINER = "rehealth-hardware-db-1"


@dataclass
class Column:
    name: str
    data_type: str
    full_type: str
    length: str
    nullable: bool
    default: str | None
    primary: bool = False
    auto_increment: bool = False
    comment: str = ""
    ordinal: int = 0
    field_path: str = ""


@dataclass
class Index:
    name: str
    columns: list[str]
    unique: bool
    primary: bool = False
    index_type: str = "BTREE"
    definition: str = ""


@dataclass
class Relation:
    source_columns: list[str]
    target_table: str
    target_columns: list[str]
    physical: bool
    name: str = ""
    on_delete: str = ""
    note: str = ""


@dataclass
class Constraint:
    name: str
    kind: str
    definition: str


@dataclass
class Table:
    name: str
    database: str
    module: str
    chinese_name: str
    purpose: str
    columns: list[Column] = field(default_factory=list)
    indexes: list[Index] = field(default_factory=list)
    relations: list[Relation] = field(default_factory=list)
    constraints: list[Constraint] = field(default_factory=list)
    comment: str = ""
    engine: str = ""
    row_count: int | None = None
    row_count_note: str = "未知"
    core: str = "否"
    evidence: str = ""
    hypertable: bool = False


TABLE_INFO: dict[str, tuple[str, str]] = {
    # Android Room.
    "health_records": ("通用健康记录表", "早期通用健康记录骨架；当前未在 AppDatabase 暴露 DAO，实际用途待确认。"),
    "attribution_logs": ("本地归因审计骨架表", "保存本地归因完整度、证据等级和审计哈希；当前未在 AppDatabase 暴露 DAO。"),
    "ring_measurements": ("可穿戴测量表", "保存按用户和设备隔离的心率、血氧、血压等规范化标量测量。"),
    "ring_sleep_sessions": ("可穿戴睡眠会话表", "保存设备睡眠阶段、厂商总睡眠时长及用户/设备归属。"),
    "ring_activities": ("可穿戴活动表", "保存步数、距离、热量、活动时长和平均心率等活动事实。"),
    "ring_signal_chunks": ("本地信号与 ECG 分块表", "保存仅限本机使用的信号/ECG 波形和导联、采样、校准元数据；波形不上传云端。"),
    "sync_upload_queue": ("离线上传队列表", "保存先落库后上传的持久化任务，支持认证暂停、退避重试和死信。"),
    "intervention_feedback_queue": ("干预反馈上传队列表", "保存用户对具体干预项的反馈及上传重试状态。"),
    "cvd_risk_history": ("本地 CVD 风险历史表", "按用户和自然日保存已确认、非 Mock 的云端 CVD/RDI-16 风险结果。"),
    "health_chat_conversations": ("本地健康问答会话表", "按用户保存健康问答会话列表、激活和逻辑删除状态。"),
    "health_chat_messages": ("本地健康问答消息表", "在请求服务端前先保存用户消息，并跟踪请求、模型和投递状态。"),
    "rdi_daily_snapshots": ("RDI 每日快照表", "保存本地 RDI 规则引擎每日快照；不替代云端 CVD 临床风险。"),
    "rdi_contribution_records": ("RDI 因素贡献表", "保存每日 RDI 快照的逐因素证据、置信度和贡献分。"),
    "rdi_baselines": ("RDI 个人基线表", "保存按用户和因素版本化、冻结期内不覆盖的个人稳健基线。"),
    "rdi_confirmed_labs": ("已确认化验锚点表", "保存用户确认后的化验指标锚点；未确认 OCR 不计入评分。"),
    "rdi_confirmed_meals": ("已确认餐食锚点表", "保存用户确认后的餐食营养区间与餐食影响证据。"),
    "rhi_manual_health_inputs": ("RHI 手工健康输入表", "保存久坐、腰围、VO2max、化验和经确认袖带血压等用户手填输入。"),
    "rhi_daily_health_index": ("RHI 每日健康指数表", "保存每个用户每日唯一的 RHI 总分、可信度、冷启动状态和算法版本。"),
    "rhi_daily_domain_score": ("RHI 每日领域分表", "保存 RHI 日快照的五领域分解；无有效指标的领域分数保持 NULL。"),
    "rhi_daily_feature_snapshot": ("RHI 每日特征快照表", "保存产生每日 RHI 的特征值、置信度和个人基线统计。"),
    "rhi_data_quality_snapshot": ("RHI 数据质量快照表", "保存每日 RHI 的缺失字段、低置信字段、质量警告和设备变化标志。"),
    "diet_records": ("本地饮食记录表", "保存手工或经确认拍照产生的餐食，随后通过遥测离线队列上传。"),

    # ReHealth software database.
    "rehealth_patient_profile": ("患者健康档案表", "保存认证用户的类型化健康档案、BMI 和乐观锁版本。"),
    "rehealth_patient_diagnosis": ("患者诊断史表", "保存健康档案下的有序诊断史条目。"),
    "rehealth_patient_medication": ("患者用药史表", "保存健康档案下的有序用药条目。"),
    "rehealth_patient_allergy": ("患者过敏史表", "保存健康档案下的有序过敏条目。"),
    "rehealth_health_interview": ("健康访谈主表", "保存认证用户每次结构化健康访谈的主记录和兼容 JSON 快照。"),
    "rehealth_health_interview_answer": ("健康访谈回答表", "保存访谈下的有序问答明细。"),
    "rehealth_health_interview_baseline": ("健康访谈基线表", "保存访谈提取的有序健康基线指标。"),
    "rehealth_health_interview_focus": ("健康访谈关注项表", "保存访谈识别出的重点健康关注项。"),
    "rehealth_device_binding": ("用户设备绑定表", "保存认证用户与产品、稳定设备身份及状态的绑定关系。"),
    "rehealth_cvd_feature_vector": ("CVD 特征向量表", "保存一次 CVD-16 评估使用的版本化特征向量和质量证据。"),
    "rehealth_cvd_risk_result": ("CVD 风险结果表", "保存模型风险分数、等级、模型贡献、Factor16 贡献、警告和模型版本。"),
    "rehealth_intervention_plan": ("健康干预计划表", "保存基于权威画像、风险和设备行为上下文生成的结构化保守干预计划。"),
    "rehealth_intervention_contraindication": ("干预禁忌表", "保存某次干预计划包含的有序禁忌与安全限制。"),
    "rehealth_intervention_feedback": ("干预反馈表", "保存用户对具体干预计划/行动的完成、跳过或不适用反馈。"),
    "rehealth_care_plan": ("机构干预计划主表", "保存按租户、机构类型和服务对象隔离的计划聚合、当前/草稿版本指针及乐观锁。"),
    "rehealth_care_plan_revision": ("机构干预计划版本表", "保存草稿、已发布和已撤回的计划版本；已发布内容不可原地覆盖。"),
    "rehealth_care_plan_item": ("机构干预计划项目表", "保存绑定到具体版本的患者可见计划项目快照及稳定逻辑项目标识。"),
    "rehealth_care_plan_occurrence": ("机构干预任务实例表", "保存绑定计划版本和项目的到期任务实例，为后续真实依从性分母提供稳定标识。"),
    "rehealth_care_plan_audit_event": ("机构干预计划审计表", "保存不含计划正文的版本生命周期操作、内容哈希和变更原因。"),
    "rehealth_attribution_event": ("归因请求事件表", "保存提交给 PIAS 的个体归因请求元数据和版本化输入快照。"),
    "rehealth_attribution_result": ("个体归因结果表", "保存 PIAS 个体归因结果及模型证据快照。"),
    "rehealth_model_request_log": ("模型请求审计表", "保存不含原始 PII/遥测的模型调用元数据、状态、耗时和错误码。"),
    "rehealth_ai_conversation": ("服务端健康问答会话表", "保存按租户和用户隔离的权威健康问答会话。"),
    "rehealth_ai_message": ("服务端健康问答消息表", "保存健康问答完整消息历史、请求幂等键、Provider 和模型版本。"),
    "rehealth_rhi_manual_health_input": ("云端 RHI 手工输入表", "保存认证用户 Room-first 手工健康输入的云端副本，并按 updated_at 合并。"),
    "rehealth_rhi_daily_snapshot": ("云端 RHI 每日聚合快照表", "保存认证用户从 App 上传的日级 RHI 分数、领域、特征与质量聚合快照；不保存原始遥测。"),
    "rehealth_behavior_record": ("结构化行为记录表", "保存拍照食物/OCR 的已验证结构化结果；不保存原始图片。"),
    "rehealth_telemetry_event_projection": ("遥测事件运营投影表", "保存 Kafka 遥测生命周期事件的隐私安全运营投影。"),
    "rehealth_telemetry_quality_case": ("遥测质量工单表", "保存由遥测质量事件派生的运营质量工单。"),
    "rehealth_website_record": ("官网业务记录表", "保存官网侧按租户隔离的结构化业务记录；具体记录类型由业务代码定义。"),
    "rehealth_schema_migration": ("ReHealth 迁移版本表", "记录 ReHealth 自定义软件库迁移版本；不是业务数据。"),

    # Insurance.
    "rehealth_insurance_subject": ("保险业务主体表", "保存租户隔离、去标识化的保险主体与 ReHealth 用户映射。"),
    "rehealth_insurance_policy": ("保险保单表", "保存租户内保单、产品、金额、期限和被保主体引用。"),
    "rehealth_insurance_coverage": ("保险保障责任表", "保存保单下的保障代码、限额、免赔额和有效期。"),
    "rehealth_insurance_consent": ("保险授权同意表", "保存主体按类型和版本授予或撤销的授权及证据哈希。"),
    "rehealth_insurance_intervention": ("保险干预参与表", "保存主体加入健康干预计划的状态与反馈时间。"),
    "rehealth_insurance_intervention_action": ("保险人工干预行动表", "保存租户和负责人范围内的随访、任务与人工复核行动及完成结果。"),
    "rehealth_insurance_claim": ("保险理赔表", "保存理赔事件、金额、状态和保障代码。"),
    "rehealth_insurance_study": ("保险研究定义表", "保存真实世界研究人群、干预、结局规则和审批状态。"),
    "rehealth_insurance_study_snapshot": ("保险研究快照表", "保存研究人群不可变快照、来源水位和内容哈希。"),
    "rehealth_insurance_study_member": ("保险研究成员表", "保存研究快照中的去标识主体、队列分组和结局值。"),
    "rehealth_insurance_study_result": ("保险研究结果表", "保存 PSM/真实世界研究估计、区间、平衡和成本结果。"),
    "rehealth_insurance_rwe_report": ("真实世界证据报告表", "保存版本化 RWE 报告及审批证据。"),
    "rehealth_insurance_settlement_package": ("保险结算包表", "保存由研究和报告形成的版本化结算证据包。"),
    "rehealth_insurance_settlement_approval": ("保险结算审批记录表", "保存结算包的审批动作、意见和请求幂等键。"),
    "rehealth_insurance_audit_event": ("保险操作审计表", "保存租户内保险资源操作的不可变审计事件和前后哈希。"),

    # Current TimescaleDB and legacy hardware names.
    "hardware_upload_batch": ("硬件上传批次表", "保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。"),
    "hardware_measurement": ("硬件标量测量表", "保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。"),
    "hardware_sleep_session": ("硬件睡眠会话表", "保存规范化睡眠会话和阶段分钟数。"),
    "hardware_activity": ("硬件活动表", "保存规范化活动、步数、距离、热量、时长和心率。"),
    "hardware_diet_record": ("硬件域饮食行为表", "保存随 telemetry-v2 批次提交的规范化饮食行为。"),
    "hardware_signal_chunk_metadata": ("硬件信号元数据表", "只保存信号时间窗、采样率和质量元数据，不保存原始波形。"),
    "hardware_data_quality_event": ("硬件数据质量事件表", "保存遥测质量事件、严重程度和详情码。"),
    "hardware_reconciliation": ("硬件批次对账表", "保存每个上传批次唯一的对账状态、重试和人工处理元数据。"),
    "hardware_outbox": ("遥测事务 Outbox 表", "与遥测事实同事务写入，随后可靠发布隐私安全 Kafka 事件。"),
    "hardware_migration_checkpoint": ("硬件迁移检查点表", "保存旧 MySQL 硬件数据迁移位置、行数、哈希和校验状态。"),
    "flyway_schema_history": ("Flyway 迁移历史表", "记录 Flyway 数据库迁移执行历史；不是业务数据。"),
}


COMMON_COLUMN_INFO: dict[str, tuple[str, str]] = {
    "id": ("主键 ID", "当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。"),
    "tenant_id": ("租户 ID", "用于多租户数据隔离；通常逻辑关联 sys_tenant.id。"),
    "user_id": ("用户 ID", "当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。"),
    "owner_user_id": ("所属用户 ID", "Android 本地数据的认证用户作用域；旧迁移行允许为空且不会向其他账号展示。"),
    "device_id": ("稳定设备 ID", "数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。"),
    "created_at": ("创建时间", "记录首次创建时间。"),
    "updated_at": ("更新时间", "记录最后更新时间；部分表用于客户端与服务端新旧副本合并。"),
    "create_time": ("创建时间", "Jeecg 公共创建时间字段。"),
    "update_time": ("更新时间", "Jeecg 公共更新时间字段。"),
    "create_by": ("创建人", "Jeecg 公共审计字段，记录创建用户。"),
    "update_by": ("更新人", "Jeecg 公共审计字段，记录最后更新用户。"),
    "sys_org_code": ("所属组织编码", "Jeecg 公共组织/部门作用域字段。"),
    "del_flag": ("逻辑删除标记", "Jeecg 逻辑删除字段；具体值以实体 @TableLogic 或字典配置为准。"),
    "deleted": ("逻辑删除标记", "逻辑删除字段；具体枚举值待确认。"),
    "version": ("版本", "记录或配置版本；是否为乐观锁需结合实体 @Version 判断。"),
    "status": ("状态", "状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。"),
    "request_id": ("请求幂等 ID", "用于请求追踪与幂等控制，不能作为用户身份来源。"),
    "source_record_id": ("来源记录 ID", "上游数据源中的稳定记录标识，通常参与幂等唯一约束。"),
    "source_system": ("来源系统", "标识记录来自哪个受信业务系统。"),
    "source": ("数据来源", "标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。"),
    "model_version": ("模型版本", "产生当前模型输出的版本标识。"),
    "algorithm_version": ("算法版本", "产生当前规则或算法结果的版本标识。"),
    "profile_id": ("健康档案记录 ID", "物理关联 rehealth_patient_profile.id。"),
    "interview_id": ("健康访谈记录 ID", "物理关联 rehealth_health_interview.id。"),
    "feature_vector_id": ("特征向量记录 ID", "物理关联 rehealth_cvd_feature_vector.id。"),
    "plan_record_id": ("干预计划记录 ID", "物理关联 rehealth_intervention_plan.id。"),
    "conversation_id": ("会话 ID", "标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。"),
    "upload_batch_id": ("上传批次 ID", "关联产生或上传当前记录的批次；具体目标表取决于存储域。"),
    "event_id": ("事件 ID", "标识遥测或业务事件；具体关联以物理外键或事件契约为准。"),
    "subject_ref": ("去标识主体引用", "保险域内去标识的稳定主体引用，逻辑关联 rehealth_insurance_subject.subject_ref。"),
    "policy_id": ("保单记录 ID", "逻辑关联 rehealth_insurance_policy.id。"),
    "study_id": ("研究记录 ID", "逻辑关联 rehealth_insurance_study.id。"),
    "snapshot_id": ("快照记录 ID", "逻辑关联本业务域的快照主记录。"),
    "index_id": ("RHI 指数记录 ID", "逻辑关联 Room rhi_daily_health_index.id。"),
    "package_id": ("结算包 ID", "逻辑关联 rehealth_insurance_settlement_package.id。"),
    "recorded_at": ("记录时间", "业务事件发生或数据记录时间；具体时区/单位见存储域说明。"),
    "measured_at": ("测量时间", "健康指标实际测量时间。"),
    "observed_at": ("观测时间", "硬件标量测量发生时间，使用 TIMESTAMPTZ。"),
    "started_at": ("开始时间", "会话、活动或信号时间窗开始时间。"),
    "ended_at": ("结束时间", "会话、活动或信号时间窗结束时间。"),
    "received_at": ("接收时间", "服务端收到上传批次的时间。"),
    "committed_at": ("持久化完成时间", "批次完成约定 durable write 的时间。"),
    "scored_on": ("评分日期", "评分所属本地自然日，使用 ISO-8601 日期。"),
    "evaluated_at": ("评估时间", "模型或规则完成评估的时间。"),
    "generated_at": ("生成时间", "计划或结果完成生成的时间。"),
    "metadata_json": ("扩展元数据 JSON", "保存版本化扩展信息；不是核心字段的唯一权威表示。"),
    "payload_json": ("载荷 JSON", "保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。"),
    "response_json": ("响应证据 JSON", "保存模型或 Provider 的版本化结构化响应快照。"),
}

# Meanings confirmed from Room Entity/KDoc, ReHealth migration SQL, database
# constraints and the JDBC repositories. These deliberately describe the
# stored fact without inventing an unsupported domain enum.
COMMON_COLUMN_INFO.update({
    "name": ("名称", "当前业务对象的名称；是否属于直接身份信息取决于所在表。"),
    "title": ("标题", "当前会话、研究、报告或业务对象的展示标题。"),
    "description": ("描述", "当前记录的业务内容描述。"),
    "type": ("类型", "当前记录的分类类型；具体枚举值需以所在模块代码或字典为准。"),
    "kind": ("任务种类", "标识离线队列载荷的业务种类，由上传调度器选择对应处理客户端。"),
    "role": ("消息角色", "标识健康问答消息发送方角色；服务端和本地会话代码据此组装上下文。"),
    "content": ("消息内容", "保存当前健康问答消息正文。"),
    "message": ("消息文本", "保存当前事件、错误或业务消息文本。"),
    "note": ("备注", "保存用户或业务操作的可选补充说明。"),
    "comment": ("审批/操作意见", "保存审批或操作人员提交的说明文本。"),
    "summary": ("摘要", "保存当前结果的结构化或可展示摘要。"),
    "summary_text": ("摘要文本", "保存当前结果的人类可读摘要。"),
    "unit": ("计量单位", "说明数值字段采用的计量单位，解释数值时必须同时读取。"),
    "value": ("记录值", "保存通用健康记录的值；具体类型由同表 type 和 unit 解释。"),
    "primary_value": ("主测量值", "规范化测量的主要数值，例如单值指标或血压收缩压分量。"),
    "secondary_value": ("次测量值", "规范化测量的可选第二数值，例如成对测量的第二分量。"),
    "current_value": ("当前值", "当前因素或指标参与计算时使用的实际值。"),
    "baseline_value": ("基线值", "用于与当前值比较的个人或研究基线值。"),
    "measured_value": ("实测值", "用户确认的化验或临床测量值，必须结合 unit 解释。"),
    "outcome_value": ("结局值", "保险研究成员在既定结局定义下的观测结果值。"),
    "metric_type": ("指标类型", "标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。"),
    "activity_type": ("活动类型", "标识活动记录的类型；具体允许值由设备 Provider 映射定义。"),
    "signal_type": ("信号类型", "标识信号/ECG 分块或元数据的信号类别。"),
    "event_type": ("事件类型", "标识质量、Outbox、归因或审计事件的业务类型。"),
    "resource_type": ("资源类型", "保险审计事件所操作资源的类型。"),
    "resource_id": ("资源 ID", "保险审计事件所操作资源的记录标识。"),
    "action": ("操作动作", "保存审批或审计动作；具体枚举由对应业务服务定义。"),
    "operation": ("操作名称", "保存模型请求或业务审计的操作名称。"),
    "category": ("分类", "当前行为、干预或业务记录的分类；具体枚举待对应代码确认。"),
    "provider": ("服务提供方", "标识产生消息、模型结果或设备数据的 Provider。"),
    "manufacturer": ("设备制造商", "绑定设备的制造商标识。"),
    "model": ("设备型号", "绑定设备的型号标识。"),
    "device_model": ("设备型号", "设备上报或绑定时记录的具体型号。"),
    "device_name": ("设备名称", "设备绑定或上报中的可展示设备名称。"),
    "firmware_version": ("固件版本", "采集当前数据时设备固件的版本。"),
    "product_code": ("产品编码", "选择设备 Provider 和能力目录的稳定产品编码。"),
    "product_name": ("产品名称", "保险或设备产品的可展示名称。"),
    "product_tier": ("产品数据层级", "RHI 根据当前可用证据确定的 LITE/STANDARD/CLINICAL 数据层级。"),
    "hardware_address_hash": ("硬件地址摘要", "设备硬件地址的不可逆摘要；不保存原始 BLE MAC。"),
    "bound_at": ("绑定时间", "用户与设备绑定建立的时间。"),
    "age": ("年龄", "健康档案中用户明确提供或经访谈确认的年龄。"),
    "gender": ("性别", "健康档案中用户明确提供或经访谈确认的性别；完整枚举待产品契约确认。"),
    "height_cm": ("身高", "健康档案中的身高，单位厘米。"),
    "weight_kg": ("体重", "健康档案中的体重，单位千克。"),
    "weight": ("权重", "领域、因素或规则参与汇总计算时使用的权重。"),
    "bmi": ("体质指数 BMI", "服务端根据档案身高和体重计算的 BMI。"),
    "family_history": ("家族史标志", "标识健康档案是否记录相关家族病史；空值表示未确认。"),
    "smoking": ("吸烟标志", "标识健康档案中的吸烟情况；空值表示未确认。"),
    "drinking": ("饮酒标志", "标识健康档案中的饮酒情况；空值表示未确认。"),
    "diabetes_history": ("糖尿病史标志", "标识健康档案中是否有糖尿病史；空值表示未确认。"),
    "hypertension_history": ("高血压史标志", "标识健康档案中是否有高血压史；空值表示未确认。"),
    "profile_version": ("档案版本号", "由 Repository 显式维护的乐观锁版本，更新档案时用于冲突检测。"),
    "answers_json": ("访谈回答兼容快照", "保存完整访谈回答的版本化 JSON；类型化回答表是主要查询结构。"),
    "question_id": ("问题 ID", "标识访谈回答对应的稳定问题。"),
    "item_value": ("回答/基线值", "访谈明细中的类型化或文本值。"),
    "sort_order": ("排序序号", "控制同一主记录下明细的稳定展示和处理顺序。"),
    "focus_area": ("健康关注领域", "访谈识别出的重点健康关注领域。"),
    "baseline_json": ("基线证据 JSON", "保存风险、归因或研究计算使用的版本化基线快照。"),
    "feature_json": ("特征向量 JSON", "保存一次模型评估实际使用的版本化特征向量。"),
    "quality_json": ("特征质量 JSON", "保存特征缺失、质量和来源等版本化元数据。"),
    "feature_schema_version": ("特征协议版本", "标识特征向量遵循的字段协议版本。"),
    "risk_score": ("风险分数", "模型返回的风险数值；解释范围和概率语义必须以模型契约为准。"),
    "risk_level": ("风险等级", "模型基于风险分数返回的离散等级；完整枚举待模型契约确认。"),
    "current_risk_score": ("当前风险分数", "保险风险查询中读取的最新已确认 CVD 风险分数。"),
    "current_risk_level": ("当前风险等级", "保险风险查询中读取的最新已确认风险等级。"),
    "trend_delta": ("趋势变化值", "当前风险或指标相对既定历史参考的变化量。"),
    "current_trend": ("当前趋势", "当前业务对象的描述性趋势；不表示因果或诊断。"),
    "contribution_json": ("模型贡献 JSON", "保存模型原始特征贡献，用于模型审计。"),
    "factor_contribution_json": ("Factor16 贡献 JSON", "保存独立 Factor16 规则的逐字段贡献。"),
    "factor_contribution_version": ("Factor16 规则版本", "标识产生 Factor16 贡献的规则版本。"),
    "factor_measured_component_json": ("Factor16 实测分量 JSON", "保存 Factor16 中经确认实测部分的贡献分量。"),
    "factor_control_support_json": ("Factor16 控制支持分量 JSON", "保存 Factor16 中有证据的控制支持趋势分量。"),
    "missing_fields_json": ("缺失字段 JSON", "保存本次模型评估缺少的输入字段列表。"),
    "quality_warnings_json": ("质量警告 JSON", "保存本次模型评估产生的数据质量警告。"),
    "fallback_reason": ("回退原因", "记录模型为何使用回退路径；生产不得静默伪装 Mock。"),
    "artifact_name": ("模型制品名称", "标识产生结果时使用的已加载模型制品。"),
    "priority_intervention": ("优先干预摘要", "结构化干预计划中优先级最高行动的摘要。"),
    "rationale": ("干预依据", "解释干预行动与权威画像、风险或行为上下文之间的依据。"),
    "expected_impact": ("预期影响", "保守描述执行干预可能带来的健康行为影响，不构成疗效保证。"),
    "medical_disclaimer": ("医疗免责声明", "声明建议仅供健康参考、不能替代医疗诊断或医生。"),
    "items_json": ("干预行动列表 JSON", "保存有序结构化干预行动和证据引用。"),
    "intervention_id": ("干预行动 ID", "标识用户反馈所针对的具体干预行动。"),
    "adherence": ("依从性", "用户反馈中记录的干预执行或依从情况。"),
    "checked_at": ("反馈打卡时间", "用户对干预行动提交反馈的时间。"),
    "attempts": ("已尝试次数", "离线队列执行当前任务的累计尝试次数，用于退避和死信判断。"),
    "attempt_count": ("尝试次数", "对账、Outbox 或迁移任务的累计处理次数。"),
    "upload_attempts": ("上传尝试次数", "反馈队列上传当前记录的累计尝试次数。"),
    "last_error": ("最近错误", "最近一次队列处理失败的脱敏错误信息。"),
    "last_error_code": ("最近错误码", "最近一次失败的脱敏稳定错误码。"),
    "next_retry_at": ("下次重试时间", "队列项允许再次处理的最早时间，支持指数退避。"),
    "upload_status": ("上传状态", "当前本地记录的离线上传生命周期状态。"),
    "delivery_status": ("消息投递状态", "本地健康问答消息发送到服务端的状态。"),
    "is_active": ("是否活动会话", "标识该用户当前正在使用的健康问答会话。"),
    "is_deleted": ("是否逻辑删除", "标识本地会话是否已被用户逻辑删除。"),
    "is_mock": ("是否模拟数据", "明确标识结果是否来自 Mock/合成路径；生产结果不得为真。"),
    "date": ("日期", "当前本地记录所属的自然日。"),
    "evaluated_on": ("评估日期", "已确认风险结果所属的用户本地自然日。"),
    "completeness": ("数据完整度", "归因审计中记录的输入完整度。"),
    "evidenceGrade": ("证据等级", "本地归因审计使用的证据等级。"),
    "auditHash": ("审计哈希", "本地归因证据的完整性摘要。"),
    "raw_score": ("原始分数", "平滑或展示转换前的当日算法分数。"),
    "display_score": ("展示分数", "经过规定平滑后用于产品展示的分数。"),
    "data_confidence": ("数据可信度", "算法对当前输入覆盖和质量的综合可信度。"),
    "available_days": ("有效天数", "评分回看窗口内具有可用证据的天数。"),
    "available_feature_count": ("可用特征数", "本次评分实际提取到的有效特征数量。"),
    "smoothing_alpha": ("平滑系数", "原始分与历史展示分合并时使用的平滑参数。"),
    "calculation_source": ("计算来源", "标识当前 RHI 快照由哪个受控计算路径产生。"),
    "domain": ("健康领域", "RHI/RDI 因素所属健康领域；RHI 五领域枚举见约束说明。"),
    "score": ("领域/规则分数", "当前领域或规则的计算分数；空值表示该领域未参与评分。"),
    "feature": ("特征名称", "RHI 32 维协议中的稳定特征字段名。"),
    "confidence": ("置信度", "当前特征、因素、识别结果或计划的可信程度。"),
    "baseline_median": ("基线中位数", "个人历史基线的稳健中位数。"),
    "baseline_mad": ("基线 MAD", "个人历史基线的中位数绝对偏差。"),
    "baseline_sample_count": ("基线样本数", "建立当前个人基线时使用的有效样本数量。"),
    "confidence_score": ("置信度分数", "RHI 数据质量的数值化可信度。"),
    "confidence_grade": ("置信度等级", "由 confidence_score 映射得到的 A–D 等级。"),
    "missing_fields": ("缺失字段", "逗号分隔的缺失特征名；空字符串表示无缺失。"),
    "low_confidence_fields": ("低置信字段", "逗号分隔的低置信特征名。"),
    "warning_codes": ("质量警告码", "逗号分隔的稳定质量警告代码。"),
    "warning_messages": ("质量警告说明", "与 warning_codes 对应的人类可读质量说明。"),
    "device_change_detected": ("是否检测到设备变化", "标识评分窗口内是否发现可能影响可比性的设备变更。"),
    "factor_code": ("因素编码", "RDI 因素的稳定代码。"),
    "source_factor_id": ("来源因素 ID", "关联产生当前贡献的稳定来源因素。"),
    "raw_points": ("原始贡献分", "乘入置信度等修正前的因素贡献分。"),
    "final_points": ("最终贡献分", "考虑置信度和规则修正后实际使用的贡献分。"),
    "evidence_text": ("证据说明", "解释当前因素贡献所依据的用户数据。"),
    "mad": ("中位数绝对偏差", "个人基线的稳健离散程度指标。"),
    "established_on": ("基线建立日期", "个人基线首次达到建立条件的本地日期。"),
    "frozen_until": ("基线冻结截止日期", "在此日期前保持基线不变，以维持历史可比性。"),
    "marker_code": ("化验指标代码", "用户确认化验指标的稳定代码，例如 LDL_C。"),
    "control_trend": ("控制支持趋势", "近期控制行为的支持趋势分，不替代临床实测值。"),
    "meal_type": ("餐次类型", "餐食所属的早餐、午餐、晚餐或加餐类别。"),
    "meal_impact": ("餐食影响分", "RDI 规则使用的已确认单餐影响分。"),
    "reason_text": ("原因说明", "解释餐食影响或业务决策的文本。"),
    "calories_kcal": ("热量", "餐食或活动能量，单位千卡。"),
    "protein_grams": ("蛋白质", "餐食蛋白质估计值，单位克。"),
    "carbohydrate_grams": ("碳水化合物", "餐食碳水化合物估计值，单位克。"),
    "fat_grams": ("脂肪", "餐食脂肪估计值，单位克。"),
    "fiber_grams": ("膳食纤维", "餐食膳食纤维估计值，单位克。"),
    "sodium_milligrams": ("钠", "餐食钠估计值，单位毫克。"),
    "consumed_at": ("进餐时间", "用户实际进餐或记录餐食的时间。"),
    "sedentary_hours_per_day": ("日均久坐时长", "用户确认的日均久坐小时数。"),
    "waist_circumference_cm": ("腰围", "用户确认的腰围，单位厘米。"),
    "vo2_max_ml_kg_min": ("最大摄氧量", "正式 VO2max，单位 ml/kg/min。"),
    "hba1c_percent": ("糖化血红蛋白", "用户确认的 HbA1c 百分比。"),
    "egfr_ml_min_1_73m2": ("估算肾小球滤过率", "用户确认的 eGFR，单位 ml/min/1.73m²。"),
    "cuff_sbp_7d_mean": ("7 日袖带收缩压均值", "经确认上臂袖带测量的 3–7 日收缩压均值。"),
    "cuff_dbp_7d_mean": ("7 日袖带舒张压均值", "经确认上臂袖带测量的 3–7 日舒张压均值。"),
    "cuff_valid_days": ("袖带有效天数", "计算袖带血压均值时包含的有效自然日数。"),
    "cuff_confirmed": ("袖带血压是否确认", "只有用户确认且满足规则的上臂袖带血压才进入正式特征。"),
    "fasting_glucose_mmol_l": ("空腹血糖", "用户确认的空腹血糖，单位 mmol/L。"),
    "total_cholesterol_mmol_l": ("总胆固醇", "用户确认的总胆固醇，单位 mmol/L。"),
    "ldl_mmol_l": ("低密度脂蛋白胆固醇", "用户确认的 LDL-C，单位 mmol/L。"),
    "hdl_mmol_l": ("高密度脂蛋白胆固醇", "用户确认的 HDL-C，单位 mmol/L。"),
    "triglycerides_mmol_l": ("甘油三酯", "用户确认的甘油三酯，单位 mmol/L。"),
    "lab_confirmed": ("化验是否确认", "只有用户确认且带日期的医院化验值才进入正式特征。"),
    "lab_recorded_at": ("化验日期时间", "经确认医院化验报告的记录时间。"),
    "quality": ("质量值", "Provider 提供或规范化后的测量质量；具体量纲按指标实现确认。"),
    "quality_code": ("质量代码", "规范化的设备或遥测质量代码。"),
    "raw_payload": ("原始扩展载荷", "本地保存的受控 Provider 扩展数据；原始波形禁止通过遥测接口上传。"),
    "total_sleep_minutes": ("厂商总睡眠时长", "Provider 明确返回的权威睡眠总分钟数；无值时不以起止跨度替代。"),
    "deep_minutes": ("深睡时长", "深睡阶段分钟数。"),
    "light_minutes": ("浅睡时长", "浅睡阶段分钟数。"),
    "awake_minutes": ("清醒时长", "睡眠会话内清醒分钟数。"),
    "rem_minutes": ("REM 时长", "快速眼动睡眠阶段分钟数。"),
    "interruption_minutes": ("中断时长", "睡眠中断分钟数。"),
    "steps": ("步数", "活动时间窗或自然日内的设备步数。"),
    "distance_meters": ("距离", "活动距离，单位米。"),
    "duration_minutes": ("持续时长", "活动持续分钟数。"),
    "average_heart_rate": ("平均心率", "活动或 ECG 测量期间的平均心率。"),
    "sample_rate_hz": ("采样率", "信号采样频率，单位 Hz。"),
    "sample_count": ("采样点数", "当前信号块包含的样本数量。"),
    "encoding": ("信号编码", "本地信号 payload 的编码格式。"),
    "payload": ("信号载荷", "Android 本地保存的信号/ECG 二进制波形；不进入云端遥测上传。"),
    "draw_frequency_hz": ("绘制频率", "ECG 界面绘制或重采样频率，单位 Hz。"),
    "duration_seconds": ("信号时长", "信号/ECG 记录持续秒数。"),
    "lead_type": ("导联类型", "ECG 记录使用的导联类型。"),
    "ecg_type": ("ECG 类型", "厂商 SDK 返回的 ECG 类型代码；具体枚举待 SDK 证据确认。"),
    "calibration_type": ("校准方式", "ADC 换算到 mV 时使用的校准方式。"),
    "contact_quality": ("接触质量", "ECG 电极接触质量状态；具体枚举待 SDK 证据确认。"),
    "batch_id": ("客户端批次 ID", "客户端生成的稳定遥测批次业务键，重试时保持不变。"),
    "receipt_id": ("持久化收据 ID", "服务端为已接收批次生成的唯一收据标识。"),
    "collected_from": ("采集窗口起点", "上传批次覆盖的最早采集时间。"),
    "collected_to": ("采集窗口终点", "上传批次覆盖的最晚采集时间。"),
    "record_count": ("记录总数", "批次中全部规范化记录数量。"),
    "measurement_count": ("测量记录数", "批次中的标量测量条数。"),
    "sleep_session_count": ("睡眠会话数", "批次中的睡眠会话条数。"),
    "activity_count": ("活动记录数", "批次中的活动记录条数。"),
    "diet_record_count": ("饮食记录数", "批次中的饮食行为条数。"),
    "signal_metadata_count": ("信号元数据数", "批次中的信号元数据条数，不含原始波形。"),
    "signal_chunk_count": ("信号分块数", "旧 MySQL 批次记录的信号分块计数。"),
    "quality_summary": ("质量摘要", "批次级结构化质量汇总，不保存原始健康载荷。"),
    "severity": ("严重程度", "质量事件严重程度，受数据库 CHECK 约束。"),
    "detail_code": ("质量详情码", "质量事件的稳定细分代码。"),
    "event_at": ("事件发生时间", "硬件质量事件实际发生时间。"),
    "aggregate_type": ("聚合类型", "Outbox 事件所属业务聚合类型。"),
    "aggregate_id": ("聚合 ID", "Outbox 事件所属业务聚合标识。"),
    "event_version": ("事件版本", "同一聚合事件类型的版本号，必须大于零。"),
    "event_metadata": ("事件元数据", "Kafka 发布所需的最小隐私安全元数据，不含原始健康值。"),
    "available_at": ("可处理时间", "Outbox 事件允许发布器领取的最早时间。"),
    "published_at": ("发布时间", "Outbox 事件成功发布到 Kafka 的时间。"),
    "operator_actor_id": ("处理人 ID", "对账进入人工处理时的操作者标识。"),
    "operator_reason": ("处理原因", "人工对账或处置的原因说明。"),
    "source_name": ("迁移来源名称", "旧数据迁移来源系统或表的稳定名称。"),
    "checkpoint_key": ("迁移检查点键", "标识某个迁移分片或范围的稳定检查点。"),
    "source_position": ("来源位置", "已处理到的来源偏移、水位或主键位置。"),
    "source_hash": ("来源哈希", "迁移来源范围的完整性摘要。"),
    "target_hash": ("目标哈希", "迁移目标范围的完整性摘要，用于与来源对账。"),
    "row_count": ("迁移行数", "当前迁移检查点覆盖的记录行数。"),
    "subject_ref": ("去标识保险主体引用", "租户内稳定的去标识主体引用，不保存直接患者标识。"),
    "rehealth_user_id": ("ReHealth 用户 ID", "保险主体映射到的内部认证用户 ID，逻辑关联 sys_user.id。"),
    "external_subject_ref_hash": ("外部主体引用摘要", "外部系统主体标识的不可逆摘要。"),
    "enrollment_status": ("纳入状态", "保险主体在当前租户业务中的纳入状态。"),
    "consent_status": ("授权状态", "保险主体当前授权状态；完整枚举由保险服务定义。"),
    "consent_version": ("授权版本", "主体同意的授权文本或协议版本。"),
    "consented_at": ("授权时间", "主体完成当前授权的时间。"),
    "policy_no": ("保单号", "租户内唯一的保单业务编号。"),
    "policy_type": ("保单类型", "保险产品的保单类型；完整枚举待保险业务确认。"),
    "policyholder_subject_ref": ("投保主体引用", "投保人的去标识主体引用。"),
    "insured_subject_ref": ("被保主体引用", "被保险人的去标识主体引用。"),
    "coverage_amount": ("保额", "保单总保障金额。"),
    "premium_amount": ("保费", "保单保费金额。"),
    "deductible_amount": ("免赔额", "保单或保障责任的免赔金额。"),
    "waiting_period_days": ("等待期天数", "保单责任生效前的等待期天数。"),
    "effective_on": ("生效日期", "保单或保障责任开始生效日期。"),
    "expires_on": ("到期日期", "保单或保障责任到期日期。"),
    "coverage_code": ("保障责任编码", "保险产品保障责任的稳定代码。"),
    "coverage_name": ("保障责任名称", "保险保障责任的可展示名称。"),
    "limit_amount": ("保障限额", "当前保障责任的最高限额。"),
    "consent_type": ("授权类型", "主体授权覆盖的数据或用途类型。"),
    "granted_at": ("授权授予时间", "授权状态变为 granted 的时间。"),
    "revoked_at": ("授权撤销时间", "主体撤销授权的时间。"),
    "evidence_ref": ("授权证据引用", "指向受控授权证据的引用，不直接保存证据正文。"),
    "evidence_hash": ("证据哈希", "授权、报告或结算证据内容的完整性摘要。"),
    "plan_id": ("干预计划业务 ID", "保险干预参与记录中的稳定计划业务标识。"),
    "source_plan_id": ("来源干预计划 ID", "逻辑引用 ReHealth 原始干预计划。"),
    "consent_id": ("授权记录 ID", "逻辑关联允许当前保险干预使用数据的授权记录。"),
    "enrolled_at": ("加入干预时间", "主体加入保险健康干预计划的时间。"),
    "last_feedback_at": ("最近反馈时间", "主体最近一次干预反馈时间。"),
    "claim_no": ("理赔号", "租户内唯一的理赔业务编号。"),
    "claim_type": ("理赔类型", "理赔事件分类；完整枚举待保险业务确认。"),
    "event_on": ("出险日期", "理赔对应保险事件的发生日期。"),
    "submitted_at": ("提交时间", "理赔、报告或审批流程提交时间。"),
    "decided_at": ("理赔决定时间", "保险理赔完成审核决定的时间。"),
    "billed_amount": ("申请金额", "理赔申请或医疗账单金额。"),
    "approved_amount": ("批准金额", "审核或结算批准的金额。"),
    "paid_amount": ("已支付金额", "理赔实际支付金额。"),
    "currency": ("币种", "金额字段采用的三字符货币代码，默认 CNY。"),
    "outcome_code": ("理赔结局代码", "理赔审核或支付结局的稳定代码。"),
    "study_no": ("研究编号", "租户内唯一的保险真实世界研究编号。"),
    "period_start": ("研究/报告起始日期", "研究或报告纳入数据的开始日期。"),
    "period_end": ("研究/报告结束日期", "研究或报告纳入数据的结束日期。"),
    "population_rule_json": ("研究人群规则", "定义研究人群纳入排除条件的版本化 JSON。"),
    "intervention_rule_json": ("研究干预规则", "定义研究处理/干预暴露的版本化 JSON。"),
    "outcome_rule_json": ("研究结局规则", "定义研究结局计算口径的版本化 JSON。"),
    "methodology": ("研究方法", "真实世界研究使用的方法，当前默认 psm。"),
    "created_by": ("创建用户 ID", "创建当前研究、报告、结算包或快照的内部用户。"),
    "approved_by": ("审批用户 ID", "批准当前研究、报告或结算包的内部用户。"),
    "approved_at": ("审批时间", "审批完成时间。"),
    "snapshot_version": ("快照版本", "同一研究下不可变人群快照的递增版本。"),
    "snapshot_hash": ("快照哈希", "研究或结算证据快照的内容完整性摘要。"),
    "source_watermark": ("来源水位", "生成研究快照时上游数据的版本或时间水位。"),
    "cohort_total": ("队列总人数", "研究快照中的去标识主体总数。"),
    "treated_total": ("处理组人数", "研究快照中处理/干预组主体数。"),
    "control_total": ("对照组人数", "研究快照中对照组主体数。"),
    "source_summary_json": ("来源摘要 JSON", "保存研究快照来源和覆盖情况的结构化摘要。"),
    "immutable": ("是否不可变", "标识研究快照生成后是否禁止修改，默认 true。"),
    "cohort_group": ("队列分组", "研究成员所属处理组或对照组。"),
    "baseline_risk": ("基线风险", "研究成员在干预前的基线风险值。"),
    "intervention_status": ("干预状态", "研究成员在结局窗口内的干预状态。"),
    "result_version": ("结果版本", "同一研究结果的递增版本。"),
    "att_estimate": ("ATT 估计值", "对已处理者平均处理效应的估计值。"),
    "ci_lower": ("区间下界", "研究估计区间的下界。"),
    "ci_upper": ("区间上界", "研究估计区间的上界。"),
    "matched_pairs": ("匹配对数", "PSM 等匹配方法最终形成的匹配样本对数。"),
    "balance_json": ("协变量平衡 JSON", "保存匹配前后协变量平衡诊断。"),
    "cost_basis_json": ("成本口径 JSON", "保存经济性或结算计算使用的成本口径。"),
    "result_json": ("研究结果 JSON", "保存完整版本化研究结果。"),
    "report_no": ("报告编号", "租户内唯一的 RWE 报告编号。"),
    "report_type": ("报告类型", "报告业务类型，当前默认 rwe。"),
    "report_version": ("报告版本", "同一研究下报告的递增版本。"),
    "report_json": ("报告内容 JSON", "保存版本化结构化 RWE 报告。"),
    "package_no": ("结算包编号", "租户内唯一的结算证据包编号。"),
    "report_id": ("报告 ID", "逻辑关联形成结算包的 RWE 报告。"),
    "package_version": ("结算包版本", "同一研究下结算证据包的递增版本。"),
    "estimated_savings": ("预计节省金额", "基于已批准研究口径估算的节省金额。"),
    "evidence_manifest_json": ("证据清单 JSON", "保存结算包包含的证据引用和哈希清单。"),
    "package_json": ("结算包内容 JSON", "保存完整版本化结算内容。"),
    "content_hash": ("内容哈希", "结算包或业务内容的完整性摘要。"),
    "actor_user_id": ("操作用户 ID", "执行审批或审计动作的内部用户。"),
    "before_hash": ("变更前哈希", "资源变更前内容的完整性摘要。"),
    "after_hash": ("变更后哈希", "资源变更后内容的完整性摘要。"),
})


ROOM_ENUMS: dict[tuple[str, str], list[tuple[str, str]]] = {
    ("sync_upload_queue", "kind"): [
        ("telemetry_batch", "遥测批次"),
        ("health_interview", "健康访谈"),
        ("rhi_daily_snapshot", "RHI 日快照"),
        ("rhi_manual_health_input", "RHI 手工健康输入"),
    ],
    ("sync_upload_queue", "status"): [
        ("pending", "待上传"), ("uploading", "上传中"), ("done", "已完成"),
        ("failed", "可重试失败"), ("dead_letter", "死信"),
    ],
    ("intervention_feedback_queue", "status"): [
        ("completed", "已完成"), ("partially_completed", "部分完成"),
        ("skipped", "已跳过"), ("not_applicable", "不适用"),
    ],
    ("intervention_feedback_queue", "upload_status"): [
        ("pending", "待上传"), ("uploading", "上传中"),
        ("done", "已完成"), ("failed", "失败"),
    ],
    ("diet_records", "meal_type"): [
        ("breakfast", "早餐"), ("lunch", "午餐"),
        ("dinner", "晚餐"), ("snack", "加餐"),
    ],
    ("rhi_daily_health_index", "status"): [
        ("provisional", "少于 7 个有效日"), ("initial", "7–13 个有效日"),
        ("baseline_confirmed", "14–27 个有效日"), ("confirmed", "至少 28 个有效日"),
    ],
    ("rhi_daily_domain_score", "domain"): [
        ("hemodynamic", "血流动力学"), ("activity_fitness", "活动与体适能"),
        ("sleep_recovery", "睡眠恢复"), ("metabolic_control", "代谢控制"),
        ("behavior_adherence", "行为依从"),
    ],
    ("rhi_data_quality_snapshot", "confidence_grade"): [
        ("A", "置信度 ≥ 0.85"), ("B", "0.70–0.8499"),
        ("C", "0.50–0.6999"), ("D", "置信度 < 0.50"),
    ],
}


LOGICAL_RELATIONS: dict[tuple[str, str], tuple[str, str, str]] = {
    ("health_chat_messages", "conversation_id"): ("health_chat_conversations", "conversation_id", "同一用户作用域下的本地会话"),
    ("rdi_contribution_records", "snapshot_id"): ("rdi_daily_snapshots", "id", "RDI 快照的逐因素明细"),
    ("rhi_daily_domain_score", "index_id"): ("rhi_daily_health_index", "id", "RHI 日指数的领域分解"),
    ("rhi_daily_feature_snapshot", "index_id"): ("rhi_daily_health_index", "id", "RHI 日指数的特征证据"),
    ("rhi_data_quality_snapshot", "index_id"): ("rhi_daily_health_index", "id", "RHI 日指数的数据质量证据"),
    ("rehealth_insurance_coverage", "policy_id"): ("rehealth_insurance_policy", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_claim", "policy_id"): ("rehealth_insurance_policy", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_study_snapshot", "study_id"): ("rehealth_insurance_study", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_study_member", "snapshot_id"): ("rehealth_insurance_study_snapshot", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_study_result", "study_id"): ("rehealth_insurance_study", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_study_result", "snapshot_id"): ("rehealth_insurance_study_snapshot", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_rwe_report", "study_id"): ("rehealth_insurance_study", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_settlement_package", "study_id"): ("rehealth_insurance_study", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_insurance_settlement_approval", "package_id"): ("rehealth_insurance_settlement_package", "id", "保险域逻辑外键，数据库未声明 FOREIGN KEY"),
    ("rehealth_care_plan", "current_revision_id"): ("rehealth_care_plan_revision", "id", "当前最新已发布版本逻辑外键"),
    ("rehealth_care_plan", "draft_revision_id"): ("rehealth_care_plan_revision", "id", "单一可变草稿版本逻辑外键"),
    ("rehealth_care_plan_revision", "plan_id"): ("rehealth_care_plan", "id", "计划版本所属聚合逻辑外键"),
    ("rehealth_care_plan_item", "plan_id"): ("rehealth_care_plan", "id", "计划项目所属聚合逻辑外键"),
    ("rehealth_care_plan_item", "revision_id"): ("rehealth_care_plan_revision", "id", "计划项目所属不可变版本逻辑外键"),
    ("rehealth_care_plan_occurrence", "plan_id"): ("rehealth_care_plan", "id", "任务实例所属计划逻辑外键"),
    ("rehealth_care_plan_occurrence", "revision_id"): ("rehealth_care_plan_revision", "id", "任务实例生成版本逻辑外键"),
    ("rehealth_care_plan_occurrence", "plan_item_id"): ("rehealth_care_plan_item", "id", "任务实例生成项目逻辑外键"),
    ("rehealth_care_plan_audit_event", "plan_id"): ("rehealth_care_plan", "id", "计划版本审计所属聚合逻辑外键"),
    ("rehealth_care_plan_audit_event", "revision_id"): ("rehealth_care_plan_revision", "id", "计划版本审计目标版本逻辑外键"),
}


def run(command: list[str], *, stdin: str | None = None) -> str:
    completed = subprocess.run(
        command,
        input=stdin.encode("utf-8") if stdin is not None else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if completed.returncode != 0:
        stderr = completed.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"Command failed ({completed.returncode}): {' '.join(command)}\n{stderr}")
    return completed.stdout.decode("utf-8", errors="strict")


def mysql_lines(sql: str) -> list[str]:
    command = [
        "docker", "exec", "-i", MYSQL_CONTAINER, "sh", "-lc",
        'export MYSQL_PWD="$(cat "$MYSQL_PASSWORD_FILE")"; '
        'exec mysql --default-character-set=utf8mb4 -N -B --raw '
        '-u"$MYSQL_USER" "$MYSQL_DATABASE"',
    ]
    return [line for line in run(command, stdin=sql).splitlines() if line.strip()]


def postgres_lines(sql: str) -> list[str]:
    command = [
        "docker", "exec", "-i", POSTGRES_CONTAINER, "sh", "-lc",
        'exec psql -X -A -t -U "$POSTGRES_USER" -d "$POSTGRES_DB"',
    ]
    return [line for line in run(command, stdin=sql).splitlines() if line.strip()]


def json_rows(lines: Iterable[str]) -> list[dict[str, Any]]:
    return [json.loads(line) for line in lines]


def mysql_metadata() -> tuple[list[Table], dict[str, str]]:
    version_rows = mysql_lines("SELECT JSON_OBJECT('version', VERSION(), 'database', DATABASE());")
    identity = json.loads(version_rows[0])

    tables = json_rows(mysql_lines("""
        SELECT JSON_OBJECT(
            'table_name', TABLE_NAME,
            'table_type', TABLE_TYPE,
            'engine', COALESCE(ENGINE, ''),
            'table_rows', TABLE_ROWS,
            'table_comment', COALESCE(TABLE_COMMENT, ''),
            'collation', COALESCE(TABLE_COLLATION, '')
        )
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
        ORDER BY TABLE_NAME;
    """))
    columns = json_rows(mysql_lines("""
        SELECT JSON_OBJECT(
            'table_name', TABLE_NAME,
            'ordinal', ORDINAL_POSITION,
            'column_name', COLUMN_NAME,
            'column_type', COLUMN_TYPE,
            'data_type', DATA_TYPE,
            'char_length', CHARACTER_MAXIMUM_LENGTH,
            'numeric_precision', NUMERIC_PRECISION,
            'numeric_scale', NUMERIC_SCALE,
            'nullable', IS_NULLABLE,
            'default_value', COLUMN_DEFAULT,
            'extra', EXTRA,
            'column_key', COLUMN_KEY,
            'comment', COALESCE(COLUMN_COMMENT, '')
        )
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        ORDER BY TABLE_NAME, ORDINAL_POSITION;
    """))
    indexes = json_rows(mysql_lines("""
        SELECT JSON_OBJECT(
            'table_name', TABLE_NAME,
            'index_name', INDEX_NAME,
            'non_unique', NON_UNIQUE,
            'seq', SEQ_IN_INDEX,
            'column_name', COALESCE(COLUMN_NAME, EXPRESSION, ''),
            'index_type', INDEX_TYPE,
            'sub_part', SUB_PART
        )
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
    """))
    foreign_keys = json_rows(mysql_lines("""
        SELECT JSON_OBJECT(
            'table_name', k.TABLE_NAME,
            'constraint_name', k.CONSTRAINT_NAME,
            'column_name', k.COLUMN_NAME,
            'referenced_table', k.REFERENCED_TABLE_NAME,
            'referenced_column', k.REFERENCED_COLUMN_NAME,
            'ordinal', k.ORDINAL_POSITION,
            'delete_rule', r.DELETE_RULE
        )
        FROM information_schema.KEY_COLUMN_USAGE k
        JOIN information_schema.REFERENTIAL_CONSTRAINTS r
          ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
         AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
        WHERE k.TABLE_SCHEMA = DATABASE()
          AND k.REFERENCED_TABLE_NAME IS NOT NULL
        ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION;
    """))
    checks = json_rows(mysql_lines("""
        SELECT JSON_OBJECT(
            'table_name', tc.TABLE_NAME,
            'constraint_name', tc.CONSTRAINT_NAME,
            'clause', cc.CHECK_CLAUSE
        )
        FROM information_schema.TABLE_CONSTRAINTS tc
        JOIN information_schema.CHECK_CONSTRAINTS cc
          ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
         AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
        WHERE tc.CONSTRAINT_SCHEMA = DATABASE()
          AND tc.CONSTRAINT_TYPE = 'CHECK'
        ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;
    """))

    by_table_columns: dict[str, list[dict[str, Any]]] = defaultdict(list)
    by_table_indexes: dict[str, dict[str, list[dict[str, Any]]]] = defaultdict(lambda: defaultdict(list))
    by_table_fks: dict[str, dict[str, list[dict[str, Any]]]] = defaultdict(lambda: defaultdict(list))
    by_table_checks: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in columns:
        by_table_columns[row["table_name"]].append(row)
    for row in indexes:
        by_table_indexes[row["table_name"]][row["index_name"]].append(row)
    for row in foreign_keys:
        by_table_fks[row["table_name"]][row["constraint_name"]].append(row)
    for row in checks:
        by_table_checks[row["table_name"]].append(row)

    result: list[Table] = []
    for item in tables:
        name = item["table_name"]
        module = mysql_module(name)
        chinese_name, purpose = table_description(name, item["table_comment"])
        table = Table(
            name=name,
            database=identity["database"],
            module=module,
            chinese_name=chinese_name,
            purpose=purpose,
            comment=item["table_comment"],
            engine=item["engine"],
            row_count=item["table_rows"],
            row_count_note=(
                f"当前本地实例约 {item['table_rows']} 行（InnoDB 统计估算，非生产容量）"
                if item["table_rows"] is not None
                else "未知"
            ),
            core=core_flag(name, "mysql"),
            evidence="运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository",
        )
        for column in by_table_columns[name]:
            length = mysql_length(column)
            table.columns.append(Column(
                name=column["column_name"],
                data_type=column["data_type"],
                full_type=column["column_type"],
                length=length,
                nullable=column["nullable"] == "YES",
                default=column["default_value"],
                primary=column["column_key"] == "PRI",
                auto_increment="auto_increment" in column["extra"],
                comment=column["comment"],
                ordinal=int(column["ordinal"]),
            ))
        for index_name, parts in by_table_indexes[name].items():
            parts.sort(key=lambda row: int(row["seq"]))
            table.indexes.append(Index(
                name=index_name,
                columns=[part["column_name"] for part in parts],
                unique=int(parts[0]["non_unique"]) == 0,
                primary=index_name == "PRIMARY",
                index_type=parts[0]["index_type"],
            ))
        for constraint_name, parts in by_table_fks[name].items():
            parts.sort(key=lambda row: int(row["ordinal"]))
            table.relations.append(Relation(
                source_columns=[part["column_name"] for part in parts],
                target_table=parts[0]["referenced_table"],
                target_columns=[part["referenced_column"] for part in parts],
                physical=True,
                name=constraint_name,
                on_delete=parts[0]["delete_rule"],
            ))
        for check in by_table_checks[name]:
            table.constraints.append(Constraint(check["constraint_name"], "CHECK", check["clause"]))
        add_logical_relations(table)
        result.append(table)
    return result, {"version": identity["version"], "database": identity["database"]}


def postgres_metadata() -> tuple[list[Table], dict[str, str]]:
    identity = json.loads(postgres_lines("""
        SELECT json_build_object(
            'version', current_setting('server_version'),
            'database', current_database(),
            'timescale_version', (SELECT extversion FROM pg_extension WHERE extname='timescaledb')
        );
    """)[0])
    tables = json_rows(postgres_lines("""
        SELECT row_to_json(x) FROM (
            SELECT table_name, table_type
            FROM information_schema.tables
            WHERE table_schema='public'
            ORDER BY table_name
        ) x;
    """))
    columns = json_rows(postgres_lines("""
        SELECT row_to_json(x) FROM (
            SELECT table_name, ordinal_position AS ordinal, column_name, data_type,
                   udt_name, character_maximum_length AS char_length,
                   numeric_precision, numeric_scale,
                   is_nullable AS nullable, column_default AS default_value
            FROM information_schema.columns
            WHERE table_schema='public'
            ORDER BY table_name, ordinal_position
        ) x;
    """))
    index_rows = json_rows(postgres_lines("""
        SELECT row_to_json(x) FROM (
            SELECT tablename AS table_name, indexname AS index_name, indexdef AS definition
            FROM pg_indexes
            WHERE schemaname='public'
            ORDER BY tablename, indexname
        ) x;
    """))
    constraints = json_rows(postgres_lines("""
        SELECT row_to_json(x) FROM (
            SELECT c.conrelid::regclass::text AS table_name, c.conname AS constraint_name,
                   c.contype::text AS constraint_type, pg_get_constraintdef(c.oid) AS definition,
                   CASE WHEN c.contype='f' THEN c.confrelid::regclass::text ELSE NULL END AS referenced_table,
                   CASE WHEN c.contype='f' THEN c.confdeltype::text ELSE NULL END AS delete_type
            FROM pg_constraint c
            JOIN pg_namespace n ON n.oid=(SELECT relnamespace FROM pg_class WHERE oid=c.conrelid)
            WHERE n.nspname='public'
            ORDER BY table_name, constraint_name
        ) x;
    """))
    hypertables = {
        row["hypertable_name"]
        for row in json_rows(postgres_lines("""
            SELECT row_to_json(x) FROM (
                SELECT hypertable_name, num_chunks
                FROM timescaledb_information.hypertables
                WHERE hypertable_schema='public'
                ORDER BY hypertable_name
            ) x;
        """))
    }
    by_table_columns: dict[str, list[dict[str, Any]]] = defaultdict(list)
    by_table_indexes: dict[str, list[dict[str, Any]]] = defaultdict(list)
    by_table_constraints: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in columns:
        by_table_columns[row["table_name"]].append(row)
    for row in index_rows:
        by_table_indexes[row["table_name"]].append(row)
    for row in constraints:
        by_table_constraints[row["table_name"]].append(row)

    result: list[Table] = []
    for item in tables:
        name = item["table_name"]
        chinese_name, purpose = table_description(name, "")
        row_count = int(postgres_lines(f'SELECT count(*) FROM public."{name}";')[0])
        table = Table(
            name=name,
            database=identity["database"],
            module="迁移元数据" if name == "flyway_schema_history" else timescale_module(name),
            chinese_name=chinese_name,
            purpose=purpose,
            engine="TimescaleDB Hypertable" if name in hypertables else "PostgreSQL",
            row_count=row_count,
            row_count_note=f"当前本地实例 {row_count} 行（精确计数，非生产容量）",
            core=core_flag(name, "postgres"),
            evidence="运行中 PostgreSQL catalog；TimescaleDB information；Flyway V1–V4",
            hypertable=name in hypertables,
        )
        pk_columns: set[str] = set()
        for constraint in by_table_constraints[name]:
            if constraint["constraint_type"] == "p":
                pk_columns.update(parse_pg_constraint_columns(constraint["definition"]))
        for column in by_table_columns[name]:
            table.columns.append(Column(
                name=column["column_name"],
                data_type=column["data_type"],
                full_type=postgres_full_type(column),
                length=postgres_length(column),
                nullable=column["nullable"] == "YES",
                default=column["default_value"],
                primary=column["column_name"] in pk_columns,
                auto_increment=(column["default_value"] or "").startswith("nextval("),
                ordinal=int(column["ordinal"]),
            ))
        for index in by_table_indexes[name]:
            table.indexes.append(parse_postgres_index(index))
        for constraint in by_table_constraints[name]:
            kind = constraint["constraint_type"]
            definition = constraint["definition"]
            if kind == "f":
                source_columns, target_columns = parse_pg_fk_columns(definition)
                table.relations.append(Relation(
                    source_columns=source_columns,
                    target_table=constraint["referenced_table"],
                    target_columns=target_columns,
                    physical=True,
                    name=constraint["constraint_name"],
                    on_delete=parse_pg_delete_rule(definition),
                ))
            elif kind == "c":
                table.constraints.append(Constraint(constraint["constraint_name"], "CHECK", definition))
        add_logical_relations(table)
        result.append(table)
    return result, identity


def room_metadata() -> tuple[list[Table], dict[str, str]]:
    raw = json.loads(ROOM_SCHEMA.read_text(encoding="utf-8"))
    database = raw["database"]
    result: list[Table] = []
    for entity in database["entities"]:
        name = entity["tableName"]
        chinese_name, purpose = table_description(name, "")
        module = room_module(name)
        pk_columns = set(entity["primaryKey"]["columnNames"])
        defaults = parse_room_defaults(entity["createSql"])
        table = Table(
            name=name,
            database="rehealth-local.db",
            module=module,
            chinese_name=chinese_name,
            purpose=purpose,
            engine="SQLite / Room",
            row_count=None,
            row_count_note="未知（当前无已连接 Android 设备；结构来自 Room v16 导出 schema）",
            core=core_flag(name, "room"),
            evidence="Room 导出 schema 16.json；Entity/DAO；显式迁移 1→16",
        )
        for ordinal, item in enumerate(entity["fields"], start=1):
            table.columns.append(Column(
                name=item["columnName"],
                data_type=item["affinity"],
                full_type=item["affinity"],
                length="不适用（SQLite 动态类型）",
                nullable=not bool(item.get("notNull", False)),
                default=item.get("defaultValue", defaults.get(item["columnName"])),
                primary=item["columnName"] in pk_columns,
                auto_increment=bool(entity["primaryKey"].get("autoGenerate", False)),
                ordinal=ordinal,
                field_path=item.get("fieldPath", ""),
            ))
        for item in entity.get("indices", []) or []:
            table.indexes.append(Index(
                name=item["name"],
                columns=item["columnNames"],
                unique=bool(item["unique"]),
                definition=item.get("createSql", ""),
            ))
        if pk_columns:
            table.indexes.insert(0, Index("PRIMARY", list(entity["primaryKey"]["columnNames"]), True, True))
        for foreign_key in entity.get("foreignKeys", []) or []:
            table.relations.append(Relation(
                source_columns=foreign_key["columnNames"],
                target_table=foreign_key["table"],
                target_columns=foreign_key["referencedColumnNames"],
                physical=True,
                on_delete=foreign_key.get("onDelete", ""),
            ))
        add_logical_relations(table)
        result.append(table)
    return result, {"version": str(database["version"]), "database": "rehealth-local.db"}


def mysql_module(name: str) -> str:
    if name in {"flyway_schema_history", "rehealth_schema_migration"}:
        return "迁移元数据"
    if name.startswith("rehealth_insurance_"):
        return "ReHealth 保险业务"
    if name.startswith("rehealth_"):
        if name in {"rehealth_model_request_log"}:
            return "ReHealth 审计日志"
        if name.startswith("rehealth_telemetry_"):
            return "ReHealth 运营投影"
        return "ReHealth 核心业务"
    if name.startswith("hardware_"):
        return "旧 MySQL 硬件兼容"
    if name.startswith("QRTZ_") or name == "sys_quartz_job":
        return "调度"
    if name.startswith("onl_"):
        return "Jeecg Online"
    if name.startswith("jimu_"):
        return "Jimu 报表"
    if name.startswith("airag_") or name == "aigc_word_template":
        return "AirAG / AI 平台"
    if name.startswith("open_api"):
        return "OpenAPI"
    if name.startswith("sys_") or name in {"oauth2_registered_client", "oss_file"}:
        if name in {"sys_dict", "sys_dict_item"}:
            return "Jeecg 字典"
        if name in {"sys_log", "sys_data_log"}:
            return "Jeecg 日志"
        if any(token in name for token in ("role", "permission", "user", "tenant", "depart", "position", "ugroup")):
            return "Jeecg 用户权限"
        return "Jeecg 系统"
    if name.startswith("test_") or "demo" in name or name == "ccc":
        return "演示/测试"
    if name.startswith("jeecg_order_"):
        return "上游订单示例"
    return "其他平台"


def room_module(name: str) -> str:
    if name.startswith("ring_"):
        return "Android 可穿戴数据"
    if name in {"sync_upload_queue", "intervention_feedback_queue"}:
        return "Android 离线同步"
    if name.startswith("health_chat_"):
        return "Android 健康问答"
    if name.startswith("rdi_"):
        return "Android RDI"
    if name.startswith("rhi_"):
        return "Android RHI"
    if name == "diet_records":
        return "Android 饮食"
    if name == "cvd_risk_history":
        return "Android CVD 风险"
    return "Android 早期骨架"


def timescale_module(name: str) -> str:
    if name == "hardware_upload_batch":
        return "硬件上传批次"
    if name in {"hardware_measurement", "hardware_sleep_session", "hardware_activity", "hardware_diet_record"}:
        return "硬件时序事实"
    if name in {"hardware_signal_chunk_metadata", "hardware_data_quality_event"}:
        return "硬件信号与质量"
    if name in {"hardware_reconciliation", "hardware_outbox"}:
        return "硬件可靠性"
    if name == "hardware_migration_checkpoint":
        return "硬件迁移对账"
    return "硬件库其他"


def table_description(name: str, comment: str) -> tuple[str, str]:
    if name in TABLE_INFO:
        return TABLE_INFO[name]
    if comment:
        return comment, comment
    return "待确认", "数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。"


def core_flag(name: str, source: str) -> str:
    if name in {"flyway_schema_history", "rehealth_schema_migration"}:
        return "否（迁移元数据）"
    if source == "room":
        return "否（遗留骨架）" if name in {"health_records", "attribution_logs"} else "是"
    if source == "postgres":
        return "是" if name != "hardware_migration_checkpoint" else "否（迁移支持）"
    if name.startswith("rehealth_insurance_"):
        return "是（保险域）"
    if name.startswith("rehealth_"):
        return "否（日志/支持）" if name in {"rehealth_model_request_log"} else "是"
    if name in {"sys_user", "sys_role", "sys_permission", "sys_tenant", "sys_user_role", "sys_role_permission"}:
        return "是（平台基础）"
    if name.startswith("hardware_"):
        return "否（迁移兼容）"
    return "否"


def mysql_length(column: dict[str, Any]) -> str:
    if column["char_length"] is not None:
        return str(column["char_length"])
    if column["numeric_precision"] is not None:
        if column["numeric_scale"] is not None:
            return f"{column['numeric_precision']},{column['numeric_scale']}"
        return str(column["numeric_precision"])
    return "不适用"


def postgres_length(column: dict[str, Any]) -> str:
    if column["char_length"] is not None:
        return str(column["char_length"])
    if column["numeric_precision"] is not None:
        scale = column["numeric_scale"]
        return f"{column['numeric_precision']},{scale}" if scale is not None else str(column["numeric_precision"])
    return "不适用"


def postgres_full_type(column: dict[str, Any]) -> str:
    data_type = column["data_type"]
    if column["char_length"] is not None:
        return f"{data_type}({column['char_length']})"
    if data_type == "numeric" and column["numeric_precision"] is not None:
        return f"numeric({column['numeric_precision']},{column['numeric_scale']})"
    return data_type


def parse_postgres_index(row: dict[str, Any]) -> Index:
    definition = row["definition"]
    unique = " UNIQUE INDEX " in f" {definition.upper()} "
    columns_match = re.search(r"\((.+?)\)(?:\s+WHERE|$)", definition)
    columns: list[str] = []
    if columns_match:
        columns = [re.sub(r"\s+(ASC|DESC)(\s+NULLS\s+(FIRST|LAST))?$", "", part.strip(), flags=re.I)
                   for part in split_sql_list(columns_match.group(1))]
    return Index(
        name=row["index_name"],
        columns=columns,
        unique=unique,
        primary=row["index_name"].endswith("_pkey"),
        index_type="BTREE" if " USING btree " in definition else "待确认",
        definition=definition,
    )


def split_sql_list(value: str) -> list[str]:
    parts: list[str] = []
    current: list[str] = []
    depth = 0
    for char in value:
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
        if char == "," and depth == 0:
            parts.append("".join(current))
            current = []
        else:
            current.append(char)
    if current:
        parts.append("".join(current))
    return parts


def parse_pg_constraint_columns(definition: str) -> list[str]:
    match = re.search(r"\(([^)]+)\)", definition)
    return [part.strip().strip('"') for part in match.group(1).split(",")] if match else []


def parse_pg_fk_columns(definition: str) -> tuple[list[str], list[str]]:
    match = re.search(r"FOREIGN KEY \(([^)]+)\) REFERENCES [^(]+\(([^)]+)\)", definition)
    if not match:
        return [], []
    source = [part.strip().strip('"') for part in match.group(1).split(",")]
    target = [part.strip().strip('"') for part in match.group(2).split(",")]
    return source, target


def parse_pg_delete_rule(definition: str) -> str:
    match = re.search(r"ON DELETE (CASCADE|RESTRICT|SET NULL|SET DEFAULT|NO ACTION)", definition)
    return match.group(1) if match else "NO ACTION"


def parse_room_defaults(create_sql: str) -> dict[str, str]:
    defaults: dict[str, str] = {}
    for match in re.finditer(r"`([^`]+)`\s+[^,]+?\s+DEFAULT\s+([^,\s)]+)", create_sql, flags=re.I):
        defaults[match.group(1)] = match.group(2).strip("'\"")
    return defaults


def add_logical_relations(table: Table) -> None:
    physical_columns = {column for relation in table.relations for column in relation.source_columns}
    for column in table.columns:
        key = (table.name, column.name)
        if key in LOGICAL_RELATIONS and column.name not in physical_columns:
            target_table, target_column, note = LOGICAL_RELATIONS[key]
            table.relations.append(Relation([column.name], target_table, [target_column], False, note=note))
        if table.name.startswith("rehealth_insurance_") and column.name == "subject_ref":
            if table.name != "rehealth_insurance_subject" and column.name not in physical_columns:
                table.relations.append(Relation(
                    [column.name], "rehealth_insurance_subject", ["subject_ref"], False,
                    note="保险域去标识主体逻辑外键，需同时使用 tenant_id 限定",
                ))
        if table.database == "rehealth_software" and column.name in {"user_id", "rehealth_user_id"}:
            table.relations.append(Relation(
                [column.name], "sys_user", ["id"], False,
                note="认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者",
            ))
        if table.database == "rehealth_software" and column.name == "tenant_id":
            table.relations.append(Relation(
                [column.name], "sys_tenant", ["id"], False,
                note="租户隔离逻辑外键",
            ))
        if table.database == "rehealth-local.db" and column.name in {"user_id", "owner_user_id"}:
            table.relations.append(Relation(
                [column.name], "认证用户", ["sys_user.id"], False,
                note="跨端逻辑归属，不存在 SQLite 外键",
            ))
        if table.database == "rehealth-local.db" and column.name == "device_id":
            table.relations.append(Relation(
                [column.name], "rehealth_device_binding", ["稳定设备标识"], False,
                note="跨库设备逻辑归属，不存在 SQLite 外键",
            ))


def column_info(table: Table, column: Column) -> tuple[str, str]:
    if column.comment:
        return column.comment, column.comment
    if column.name in COMMON_COLUMN_INFO:
        return COMMON_COLUMN_INFO[column.name]
    # Only apply conservative, structural suffix interpretations.
    if column.name.endswith("_json"):
        return "JSON 快照", "保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。"
    if column.name.endswith("_count"):
        return "数量", "当前表业务对象的计数值；具体计数口径待确认。"
    if column.name.endswith("_hash"):
        return "哈希值", "保存不可逆摘要，用于完整性、幂等或去标识；具体算法待确认。"
    if column.name.endswith("_version"):
        return "版本", "保存对应对象、契约或算法版本；具体版本规则待确认。"
    if column.name.endswith("_at"):
        return "时间", "该字段记录的具体业务事件时间待确认。"
    if column.name.endswith("_on"):
        return "日期", "该字段记录的具体业务日期待确认。"
    return "待确认", "数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。"


def enum_text(table: Table, column: Column) -> str:
    values = ROOM_ENUMS.get((table.name, column.name))
    if values:
        return "；".join(f"{value}={meaning}" for value, meaning in values)
    if column.comment and any(token in column.comment for token in ("=", "：", ":", "0", "1")):
        return column.comment
    matching_checks = [c.definition for c in table.constraints if re.search(rf"\b{re.escape(column.name)}\b", c.definition)]
    if matching_checks:
        return "；".join(matching_checks)
    if column.name in {"status", "state", "type", "source", "role", "level", "category", "flag", "gender", "enable", "enabled", "audit_status", "order_status"}:
        return "具体枚举值待确认"
    return "—"


def index_membership(table: Table, column: Column) -> tuple[str, str]:
    memberships: list[str] = []
    unique_memberships: list[str] = []
    for index in table.indexes:
        normalized = [re.sub(r"[^A-Za-z0-9_]", "", item.split("::")[0]) for item in index.columns]
        if column.name in normalized or any(re.search(rf"\b{re.escape(column.name)}\b", item) for item in index.columns):
            memberships.append(index.name)
            if index.unique:
                label = index.name if len(index.columns) == 1 else f"联合唯一:{index.name}"
                unique_memberships.append(label)
    return ("、".join(unique_memberships) or "否", "、".join(memberships) or "否")


def relation_text(table: Table, column: Column) -> str:
    relations = [relation for relation in table.relations if column.name in relation.source_columns]
    if not relations:
        return "否/待确认" if column.name.endswith("_id") else "否"
    items = []
    for relation in relations:
        rel_type = "物理" if relation.physical else "逻辑"
        target = ",".join(relation.target_columns)
        items.append(f"{rel_type}→{relation.target_table}.{target}")
    return "；".join(items)


def default_text(value: str | None) -> str:
    if value is None:
        return "无/NULL"
    text = str(value)
    return text.replace("|", "\\|").replace("\n", " ")


def safe(value: Any) -> str:
    if value is None:
        return ""
    return str(value).replace("|", "\\|").replace("\r", " ").replace("\n", "<br>")


def table_anchor(name: str) -> str:
    return name.lower().replace("_", "-")


def render_table_detail(table: Table, number: int) -> str:
    pk = ", ".join(column.name for column in table.columns if column.primary) or "无/待确认"
    lines = [
        f"## {number}. 表：`{table.name}` {safe(table.chinese_name)}",
        "",
        "### 基本信息",
        "",
        "| 项目 | 内容 |",
        "| --- | --- |",
        f"| 表名 | `{table.name}` |",
        f"| 中文名称 | {safe(table.chinese_name)} |",
        f"| 所属数据库 | `{table.database}` |",
        f"| 所属模块 | {safe(table.module)} |",
        f"| 业务作用 | {safe(table.purpose)} |",
        f"| 主键 | `{pk}` |",
        f"| 存储引擎 | {safe(table.engine or '待确认')} |",
        f"| 数据量级 | {safe(table.row_count_note)} |",
        f"| 是否核心表 | {table.core} |",
        f"| 结构依据 | {safe(table.evidence)} |",
        "",
        "### 字段",
        "",
        "| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |",
        "| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for position, column in enumerate(table.columns, start=1):
        chinese, business = column_info(table, column)
        unique, indexes = index_membership(table, column)
        public = "是" if column.name in COMMON_COLUMN_INFO and column.name in {
            "id", "tenant_id", "user_id", "create_by", "create_time", "update_by", "update_time",
            "created_at", "updated_at", "sys_org_code", "del_flag", "deleted", "version", "status",
        } else "否"
        lines.append(
            "| " + " | ".join([
                str(position), f"`{column.name}`", safe(chinese), f"`{safe(column.full_type)}`",
                safe(column.length), "是" if column.nullable else "否", f"`{default_text(column.default)}`",
                "是" if column.primary else "否", "是" if column.auto_increment else "否",
                safe(unique), safe(indexes), public, safe(relation_text(table, column)),
                safe(enum_text(table, column)), safe(business),
            ]) + " |"
        )
    lines.extend(["", "### 索引", "", "| 索引名称 | 字段 | 类型 | 作用 |", "| --- | --- | --- | --- |"])
    if table.indexes:
        for index in table.indexes:
            index_type = "主键" if index.primary else ("唯一索引" if index.unique else "普通索引")
            if len(index.columns) > 1:
                index_type += "（联合）"
            purpose = index_purpose(index)
            lines.append(f"| `{safe(index.name)}` | `{safe(', '.join(index.columns))}` | {index_type} | {safe(purpose)} |")
    else:
        lines.append("| — | — | 无独立索引 | 未发现索引定义。 |")
    lines.extend(["", "### 关联关系", ""])
    if table.relations:
        for relation in table.relations:
            source = ", ".join(relation.source_columns)
            target = ", ".join(relation.target_columns)
            rel_type = "物理外键" if relation.physical else "逻辑外键"
            suffix = f"；ON DELETE {relation.on_delete}" if relation.physical and relation.on_delete else ""
            note = f"；{relation.note}" if relation.note else ""
            lines.append(f"- `{table.name}.({source})` → `{relation.target_table}.({target})`：{rel_type}{suffix}{note}。")
    else:
        lines.append("- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。")
    lines.extend(["", "### 枚举与约束", ""])
    enum_rows = []
    for column in table.columns:
        value = enum_text(table, column)
        if value not in {"—", "具体枚举值待确认"}:
            enum_rows.append((column.name, value))
    if enum_rows:
        for column_name, value in enum_rows:
            lines.append(f"- `{column_name}`：{safe(value)}。")
    unknown_enum_columns = [
        column.name for column in table.columns if enum_text(table, column) == "具体枚举值待确认"
    ]
    if unknown_enum_columns:
        lines.append(f"- `{', '.join(unknown_enum_columns)}`：状态/类型类字段，完整枚举值待确认。")
    for constraint in table.constraints:
        lines.append(f"- `{constraint.name}`（{constraint.kind}）：`{safe(constraint.definition)}`。")
    if not enum_rows and not unknown_enum_columns and not table.constraints:
        lines.append("- 未发现数据库 CHECK 或可确认的代码枚举。")
    lines.extend(["", "### 业务说明", "", table.purpose, ""])
    if table.name.startswith("hardware_") and table.database == "rehealth_software":
        lines.append("注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。")
        lines.append("")
    return "\n".join(lines)


def index_purpose(index: Index) -> str:
    if index.primary:
        return "保证记录唯一并支持主键定位。"
    if index.unique:
        return "保证字段组合唯一，并支持按该组合进行幂等或业务键查询。"
    columns = " ".join(index.columns).lower()
    if "status" in columns and any(token in columns for token in ("time", "at", "date")):
        return "支持按状态和时间扫描任务、日志或业务记录。"
    if "tenant_id" in columns and "user_id" in columns:
        return "支持租户与用户作用域查询。"
    if any(token in columns for token in ("created_at", "updated_at", "started_at", "observed_at", "received_at")):
        return "支持按业务作用域和时间范围查询或排序。"
    return "支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。"


def render_appendix(title: str, intro: str, tables: list[Table]) -> str:
    module_groups: dict[str, list[Table]] = defaultdict(list)
    for table in tables:
        module_groups[table.module].append(table)
    lines = [
        f"# {title}", "",
        "> 本文件由 `tools/generate_database_schema_docs.py` 根据只读结构元数据生成。",
        "> 不包含数据库账号、密码、业务行内容或原始健康数据。", "",
        intro, "",
        "## 表清单", "",
        "| 序号 | 表名 | 中文名称 | 模块 | 主要用途 | 核心表 |",
        "| ---: | --- | --- | --- | --- | --- |",
    ]
    for number, table in enumerate(tables, start=1):
        lines.append(
            f"| {number} | [`{table.name}`](#{table_anchor(table.name)}) | {safe(table.chinese_name)} | "
            f"{safe(table.module)} | {safe(table.purpose)} | {table.core} |"
        )
    lines.extend(["", "## 模块统计", "", "| 模块 | 表数 |", "| --- | ---: |"])
    for module in sorted(module_groups):
        lines.append(f"| {safe(module)} | {len(module_groups[module])} |")
    lines.append("")
    for number, table in enumerate(tables, start=1):
        lines.append(render_table_detail(table, number))
    return "\n".join(lines).rstrip() + "\n"


def render_master(
    room_tables: list[Table], mysql_tables: list[Table], pg_tables: list[Table],
    room_identity: dict[str, str], mysql_identity: dict[str, str], pg_identity: dict[str, str],
) -> str:
    total = len(room_tables) + len(mysql_tables) + len(pg_tables)
    mysql_views = sum(1 for table in mysql_tables if table.engine == "")
    rehealth_specific = (
        len(room_tables)
        + sum(1 for table in mysql_tables if table.name.startswith("rehealth_") and table.name != "rehealth_schema_migration")
        + sum(1 for table in pg_tables if table.name != "flyway_schema_history")
    )
    module_counts: dict[str, int] = defaultdict(int)
    for table in room_tables + mysql_tables + pg_tables:
        module_counts[table.module] += 1
    failed_flyway = "MySQL `flyway_schema_history` 当前存在 `3.9.2.0 all upgrade` 失败记录；ReHealth 自定义迁移已到 `software-V20260812.3`。"
    return f"""# 数据库表结构说明文档

> 最后生成：{date.today().isoformat()}。
> 结构基线来自当前运行中的本地开发数据库 catalog、Room v16 导出 schema、SQL 迁移和业务代码。
> 本文档不包含账号、密码、业务数据明细、原始健康值或直接身份信息。

## 1. 文档说明

本文档是 ReHealth 数据库结构的交付与维护入口。逐表字段和索引明细拆分为三个附录：

- [Android Room v16 逐表结构](database/ROOM_SCHEMA_V16.md)
- [MySQL software_db 逐表结构](database/SOFTWARE_DB_TABLES.md)
- [TimescaleDB hardware_db 逐表结构](database/HARDWARE_DB_TABLES.md)

事实优先级为：运行数据库 catalog → Room 导出 schema / CREATE TABLE / 迁移 SQL → 数据库 COMMENT → Entity/DAO/Repository/Mapper → 业务代码。无法确认的含义统一标记为“待确认”。行数只用于说明当前本地开发实例量级，不代表生产容量。

## 2. 数据库整体介绍

ReHealth 不是单库系统，而是三个相互隔离的关系型存储域：

| 数据域 | 实际名称 | 类型与版本 | 基础表 | 视图 | 权威职责 |
| --- | --- | --- | ---: | ---: | --- |
| Android 本地库 | `{room_identity['database']}` | SQLite / Room schema v{room_identity['version']} | {len(room_tables)} | 0 | 本地遥测、离线队列、聊天、RHI/RDI、饮食 |
| 软件业务库 | `{mysql_identity['database']}`（逻辑名 `software_db`） | MySQL {mysql_identity['version']} | {len(mysql_tables)} | 0 | Jeecg 用户权限及 ReHealth 业务权威数据 |
| 硬件时序库 | `{pg_identity['database']}`（逻辑名 `hardware_db`） | PostgreSQL {pg_identity['version']} + TimescaleDB {pg_identity['timescale_version']} | {len(pg_tables)} | 0 个业务普通视图 | 规范化硬件时序数据、Outbox 和对账 |

总计 **{total} 张基础表**。其中 ReHealth 专属业务域表 **{rehealth_specific} 张**：Room {len(room_tables)} 张、MySQL ReHealth 业务表 {sum(1 for t in mysql_tables if t.name.startswith('rehealth_') and t.name != 'rehealth_schema_migration')} 张、TimescaleDB 业务表 {sum(1 for t in pg_tables if t.name != 'flyway_schema_history')} 张。Kafka 是事件传递系统、Redis 是短期状态存储，均不计入关系表总数。

明确可识别的特殊表类别：

| 类别 | 数量 | 口径 |
| --- | ---: | --- |
| 核心/ReHealth 专属业务域表 | {rehealth_specific} | 含本地队列、质量、审计和保险域；排除迁移元数据 |
| 字典表 | 4 | `sys_dict`、`sys_dict_item`、`jimu_dict`、`jimu_dict_item` |
| 明确日志/审计表 | 8 | 本地归因、模型请求、保险审计、系统/数据/OpenAPI/报表导出日志、硬件质量事件 |
| 明确中间/关系表 | 17 | 用户角色权限、租户/部门关系、访谈明细、RHI/RDI 明细、研究成员等 |
| 迁移元数据表 | 3 | Room 使用 schema JSON；MySQL 2 张、TimescaleDB 1 张迁移表 |
| 历史/备份/年/月分表 | 0 | 未发现 `*_history` 之外的物理历史/备份或按年月命名分表；`cvd_risk_history` 是业务历史表，不是备份表 |

{failed_flyway}

## 3. 数据库表清单与模块划分

| 存储域/模块 | 表数 | 主要作用 |
| --- | ---: | --- |
""" + "\n".join(
        f"| {safe(module)} | {count} | {module_summary(module)} |"
        for module, count in sorted(module_counts.items())
    ) + f"""

完整 211 表清单及逐字段说明见三个逐表附录。MySQL 中还保留 6 张旧 `hardware_*` 兼容表；当前硬件遥测权威写入已经属于 Device Service/TimescaleDB，旧表只能作为迁移来源或兼容遗留，不能形成双写权威。

## 4. 核心业务表

| 核心聚合 | 主表/表组 | 业务作用 |
| --- | --- | --- |
| 用户与租户 | `sys_user`、`sys_tenant`、`sys_role`、`sys_permission` | 提供认证账号、租户和后台授权基础 |
| 健康档案 | `rehealth_patient_profile` 及 diagnosis/medication/allergy | 权威类型化个人健康档案和病史明细 |
| 健康访谈 | `rehealth_health_interview` 及 answer/baseline/focus | 保存结构化访谈和风险评估上下文 |
| 设备绑定 | `rehealth_device_binding` | 连接认证用户、产品和稳定设备身份 |
| 本地采集 | Room `ring_*` | BLE/厂商数据先本地落库，与网络解耦 |
| 离线同步 | `sync_upload_queue` | 支持幂等、401 暂停、退避和死信 |
| 硬件接入 | `hardware_upload_batch` 及遥测事实表 | 以批次事务写入 TimescaleDB 并返回 durable write 语义 |
| 可靠事件 | `hardware_outbox`、`hardware_reconciliation` | 可靠发布 Kafka 生命周期事件并处理对账 |
| CVD 风险 | `rehealth_cvd_feature_vector`、`rehealth_cvd_risk_result` | 保存版本化 CVD-16 输入、模型输出和解释证据 |
| 干预闭环 | `rehealth_intervention_plan`、contraindication、feedback | 保存保守干预计划、安全限制和用户反馈 |
| 健康问答 | `rehealth_ai_conversation`、`rehealth_ai_message` | 服务端权威完整聊天历史；Room 保存本地副本 |
| RHI/RDI | Room `rhi_*`、`rdi_*`；MySQL `rehealth_rhi_manual_health_input` | 保存本地透明评分、证据及手工健康输入云端副本 |
| 饮食/行为 | Room `diet_records`、Timescale `hardware_diet_record`、MySQL `rehealth_behavior_record` | 连接手工/拍照行为、本地队列、硬件域事实和结构化业务记录 |
| 保险/RWE | `rehealth_insurance_*` 14 表 | 支持去标识主体、保单、理赔、研究、RWE、结算和审计；当前本地实例均为空表 |

## 5. 主要表关系

### 5.1 物理外键

MySQL ReHealth 业务域已确认 11 组物理关系：档案到诊断/用药/过敏，访谈到回答/基线/关注项，特征向量到风险结果，干预计划到禁忌/反馈，AI 会话到消息，遥测投影到质量工单。

TimescaleDB 已确认 8 组物理外键，均由 `hardware_upload_batch.id` 指向测量、睡眠、活动、饮食、信号元数据、质量事件、Outbox 和对账。`hardware_reconciliation.upload_batch_id` 另有唯一约束，因此批次与对账为一对一；其余主要为一对多。

Room v16 没有声明 SQLite FOREIGN KEY，关系由复合主键、唯一索引、DAO `@Transaction` 和 Repository 写入顺序维护。

### 5.2 逻辑外键

- MySQL `rehealth_*.user_id` → `sys_user.id`，用户来自认证上下文。
- MySQL/Timescale `tenant_id` → `sys_tenant.id`，跨库只做逻辑关联，不做跨库事务。
- Room `owner_user_id/user_id` → 当前认证用户；`device_id` → 服务端设备绑定。
- Room RHI/RDI 子表通过 `index_id/snapshot_id` 逻辑关联日快照主表。
- 保险 14 表使用 `tenant_id + subject_ref/policy_id/study_id/snapshot_id/package_id` 维护逻辑关系，当前没有数据库 FOREIGN KEY。

## 6. ER 关系图

### 6.1 端到端核心关系

```mermaid
flowchart LR
    User["sys_user / sys_tenant"] --> Profile["rehealth_patient_profile"]
    User --> Binding["rehealth_device_binding"]
    User --> Interview["rehealth_health_interview"]
    Device["HBand / Viomi"] --> Room["Room ring_*"]
    Room --> Queue["sync_upload_queue"]
    Queue --> Batch["hardware_upload_batch"]
    Binding -. "授权校验" .-> Batch
    Batch --> Facts["measurement / sleep / activity / diet"]
    Batch --> Outbox["hardware_outbox"]
    Outbox --> Projection["Kafka / telemetry projection"]
    Profile --> Feature["rehealth_cvd_feature_vector"]
    Interview --> Feature
    Feature --> Risk["rehealth_cvd_risk_result"]
    Risk --> Plan["rehealth_intervention_plan"]
    Facts -. "有界行为摘要" .-> Plan
    Plan --> Feedback["rehealth_intervention_feedback"]
```

### 6.2 MySQL 健康业务 ER

```mermaid
erDiagram
    REHEALTH_PATIENT_PROFILE ||--o{{ REHEALTH_PATIENT_DIAGNOSIS : contains
    REHEALTH_PATIENT_PROFILE ||--o{{ REHEALTH_PATIENT_MEDICATION : contains
    REHEALTH_PATIENT_PROFILE ||--o{{ REHEALTH_PATIENT_ALLERGY : contains
    REHEALTH_HEALTH_INTERVIEW ||--o{{ REHEALTH_HEALTH_INTERVIEW_ANSWER : answers
    REHEALTH_HEALTH_INTERVIEW ||--o{{ REHEALTH_HEALTH_INTERVIEW_BASELINE : baselines
    REHEALTH_HEALTH_INTERVIEW ||--o{{ REHEALTH_HEALTH_INTERVIEW_FOCUS : focuses
    REHEALTH_CVD_FEATURE_VECTOR ||--|| REHEALTH_CVD_RISK_RESULT : produces
    REHEALTH_INTERVENTION_PLAN ||--o{{ REHEALTH_INTERVENTION_CONTRAINDICATION : limits
    REHEALTH_INTERVENTION_PLAN ||--o{{ REHEALTH_INTERVENTION_FEEDBACK : receives
    REHEALTH_AI_CONVERSATION ||--o{{ REHEALTH_AI_MESSAGE : contains
```

### 6.3 TimescaleDB 遥测 ER

```mermaid
erDiagram
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_MEASUREMENT : contains
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_SLEEP_SESSION : contains
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_ACTIVITY : contains
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_DIET_RECORD : contains
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_SIGNAL_CHUNK_METADATA : contains
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_DATA_QUALITY_EVENT : contains
    HARDWARE_UPLOAD_BATCH ||--o{{ HARDWARE_OUTBOX : publishes
    HARDWARE_UPLOAD_BATCH ||--|| HARDWARE_RECONCILIATION : reconciles
```

### 6.4 保险逻辑 ER

```mermaid
erDiagram
    REHEALTH_INSURANCE_SUBJECT ||--o{{ REHEALTH_INSURANCE_POLICY : insured_by
    REHEALTH_INSURANCE_SUBJECT ||--o{{ REHEALTH_INSURANCE_CONSENT : grants
    REHEALTH_INSURANCE_SUBJECT ||--o{{ REHEALTH_INSURANCE_CLAIM : submits
    REHEALTH_INSURANCE_POLICY ||--o{{ REHEALTH_INSURANCE_COVERAGE : contains
    REHEALTH_INSURANCE_POLICY ||--o{{ REHEALTH_INSURANCE_CLAIM : covers
    REHEALTH_INSURANCE_STUDY ||--o{{ REHEALTH_INSURANCE_STUDY_SNAPSHOT : snapshots
    REHEALTH_INSURANCE_STUDY_SNAPSHOT ||--o{{ REHEALTH_INSURANCE_STUDY_MEMBER : contains
    REHEALTH_INSURANCE_STUDY_SNAPSHOT ||--o{{ REHEALTH_INSURANCE_STUDY_RESULT : produces
    REHEALTH_INSURANCE_STUDY ||--o{{ REHEALTH_INSURANCE_RWE_REPORT : reports
    REHEALTH_INSURANCE_STUDY ||--o{{ REHEALTH_INSURANCE_SETTLEMENT_PACKAGE : settles
    REHEALTH_INSURANCE_SETTLEMENT_PACKAGE ||--o{{ REHEALTH_INSURANCE_SETTLEMENT_APPROVAL : approvals
```

保险图中的关系为逻辑外键，不代表数据库已声明 FOREIGN KEY。

## 7. 公共字段说明

| 字段 | 含义 | 说明 |
| --- | --- | --- |
| `id` | 主键 ID | Room/ReHealth 多使用业务生成的字符串或 UUID；Jeecg 多数实体使用 MyBatis-Plus `ASSIGN_ID`，必须逐表核对 |
| `tenant_id` | 租户 ID | 多租户隔离字段；跨库逻辑关联 `sys_tenant.id` |
| `user_id` / `owner_user_id` | 用户归属 | 来自认证上下文；Android v15/v16 为旧遥测增加可空用户作用域 |
| `create_by/create_time/update_by/update_time/sys_org_code` | Jeecg 公共审计字段 | 由 Jeecg/MyBatis-Plus 基础设施和业务代码维护 |
| `created_at/updated_at` | ReHealth 公共时间 | 使用 `DATETIME(3)`、`TIMESTAMPTZ` 或 Room epoch milliseconds，不能跨库直接比较而忽略时区 |
| `status/state` | 生命周期状态 | 必须以 CHECK、注释或代码枚举为准；没有证据时标记待确认 |
| `request_id/source_record_id` | 幂等键 | 用于请求或上游记录去重，不承担身份认证 |
| `model_version/algorithm_version` | 模型/算法版本 | 保证结果可追溯和可解释 |
| `metadata_json/payload_json/response_json` | 版本化 JSON | 仅用于扩展、证据和重放，不应替代核心类型化字段 |

Jeecg 实体已发现 MyBatis-Plus `@TableId(type = IdType.ASSIGN_ID)` 和部分 `@TableLogic`；是否启用逻辑删除必须按具体实体核对。本轮没有发现 ReHealth 业务实体使用 `@Version`，`rehealth_patient_profile.profile_version` 的乐观锁由 Repository SQL 显式维护。

## 8. 重点枚举字段

| 存储域/表字段 | 已确认值 |
| --- | --- |
| Room `sync_upload_queue.status` | `pending`、`uploading`、`done`、`failed`、`dead_letter` |
| Room `intervention_feedback_queue.status` | `completed`、`partially_completed`、`skipped`、`not_applicable` |
| Room `diet_records.meal_type` | `breakfast`、`lunch`、`dinner`、`snack` |
| Room `rhi_daily_health_index.status` | `provisional`、`initial`、`baseline_confirmed`、`confirmed` |
| Room `rhi_data_quality_snapshot.confidence_grade` | A/B/C/D，阈值见 Room 附录 |
| Timescale `hardware_upload_batch.status` | `RECEIVED`、`PERSISTED`、`EVENT_PENDING`、`EVENT_PUBLISHED`、`REJECTED`、`RETRYABLE_FAILURE`、`DLQ_REVIEW`、`RESOLVED` |
| Timescale `hardware_outbox.status` | `PENDING`、`PUBLISHING`、`PUBLISHED`、`FAILED`、`DLQ_REVIEW` |
| Timescale `hardware_data_quality_event.severity` | `INFO`、`WARN`、`ERROR` |
| Timescale `hardware_diet_record.meal_type` | `breakfast`、`lunch`、`dinner`、`snack` |

MySQL 大量 `status/type/source` 字段没有 CHECK，且部分依赖 Jeecg 字典或业务代码。逐表附录只呈现数据库 COMMENT 或本轮已确认代码值；其他均标“具体枚举值待确认”，不根据字段名编造。

## 9. 索引与主键总体说明

- MySQL 当前有 448 个不同索引，其中 241 个唯一/主键索引；存在大量 Jeecg 平台元数据索引。
- TimescaleDB public schema 当前有 40 个索引、22 个唯一索引和 8 个物理外键。
- Room v16 使用字符串主键、复合主键和用户/时间联合索引；没有自增主键和物理外键。
- Timescale Hypertable 主键包含分区时间列，例如 `hardware_measurement(id, observed_at)`。
- Timescale 来源唯一键同时包含租户、用户、设备、时间、记录类型和来源记录 ID，用于批次重试幂等。

## 10. 日志、字典、历史和生命周期

- 字典：`sys_dict/sys_dict_item` 与 `jimu_dict/jimu_dict_item`。Jeecg 的部分状态含义依赖字典配置，不能仅从列名确定。
- 日志：`sys_log`、`sys_data_log`、`open_api_log`、`jimu_report_export_log`、`rehealth_model_request_log`、`rehealth_insurance_audit_event`、Room `attribution_logs`、Timescale `hardware_data_quality_event`。
- Timescale 测量、睡眠、活动、饮食和质量事件为 Hypertable；默认迁移配置对测量类数据保留 730 天、信号元数据 90 天、运营历史 1,095 天、已发布 Outbox 30 天。
- 未发现物理年表、月表、`*_bak` 备份表。`cvd_risk_history` 是正常业务历史，不是备份。

## 11. Entity/Repository 映射

- Room Entity 与表逐一映射，精确列名来自 `Android-apk/app/schemas/com.rehealth.genie.data.AppDatabase/16.json`；各附录字段行即数据库列映射，字段使用 `@ColumnInfo` 时由导出 schema 解析最终列名。
- 代表性 Room 映射：`RingMeasurementEntity` → `ring_measurements`（`metricType` → `metric_type`、`measuredAt` → `measured_at`）；`UploadQueueEntity` → `sync_upload_queue`；`RdiDailySnapshotEntity` → `rdi_daily_snapshots`；`RhiDailyIndexEntity` → `rhi_daily_health_index`；`DietRecordEntity` → `diet_records`。
- `cvd_risk_cache` 虽有 `@Entity` 与 DAO，但未注册进 `AppDatabase.entities`，不是 Room v16 实际表。
- `health_records`、`attribution_logs` 已注册，但当前 `AppDatabase` 不暴露对应 DAO，属于待清理或待接线骨架。
- ReHealth MySQL 核心业务主要由 `JdbcSoftwareDbReHealthBusinessRepository`、`JdbcHealthAgentConversationRepository`、`JdbcBehaviorRecordRepository` 和保险 JDBC Repository 显式 SQL 映射；保险域另有 `InsurancePolicyEntity` → `rehealth_insurance_policy`、`InsuranceClaimEntity` → `rehealth_insurance_claim` 等 MyBatis-Plus 映射，主键使用 `IdType.INPUT`。
- Jeecg 平台表主要通过 MyBatis-Plus `@TableName/@TableId/@TableLogic` 与 Mapper/XML 映射；例如 `SysUser` 按默认驼峰规则映射 `sys_user`，`id` 使用 `ASSIGN_ID`，`delFlag` → `del_flag` 且带 `@TableLogic`。
- Device Service 通过 Flyway 和 JDBC adapter 映射 TimescaleDB；`hardware_upload_batch` 是批次聚合根，其他事实/可靠性表通过 `upload_batch_id` 关联，Jeecg 不直接查询 Timescale 表。

## 12. 字段命名规范观察

- Room 早期 `health_records.recordedAt` 使用驼峰列名，其他新增表主要使用下划线，存在历史命名不一致。
- MySQL Quartz 表名为大写 `QRTZ_*`，其余主要小写下划线；属于第三方框架差异。
- 用户标识同时出现 `user_id`、`owner_user_id`、`rehealth_user_id`、`actor_user_id`，语义不同，接口和查询中不得混用。
- 时间同时使用 Room epoch milliseconds、MySQL `DATETIME(3)/TIMESTAMP(3)`、PostgreSQL `TIMESTAMPTZ` 和 `DATE`；文档和 API 必须明确单位及时区。
- `software_db`/`hardware_db` 是逻辑名，当前本地实际数据库为 `{mysql_identity['database']}`/`{pg_identity['database']}`，部署文档必须避免混淆。

## 13. 数据库设计问题

1. MySQL 仍保留 6 张旧硬件兼容表，容易与 TimescaleDB 权威表混淆；需要保留明确只读/迁移门禁并制定退役条件。
2. 当前 MySQL Flyway `3.9.2.0 all upgrade` 有失败记录；虽然 ReHealth 自定义迁移已完成，平台基线仍需修复并重新验证。
3. 保险 14 表大量使用逻辑外键且当前没有 FOREIGN KEY；应用必须在同一事务中维护引用完整性，并增加孤儿数据巡检。
4. Room RHI/RDI/聊天子表没有物理外键；DAO 已用事务维护主要写入，但删除和覆盖路径仍应持续做迁移测试。
5. Timescale 多个子表的 `upload_batch_id` 没有独立普通索引；如频繁按批次联查或级联删除，应先用真实 `EXPLAIN` 验证后补索引。
6. ReHealth 自建 MySQL 表和字段普遍缺少 COMMENT，导致状态含义依赖代码；应在后续迁移中补充不改变结构的注释。
7. MySQL 实例仍有 18 张 demo/test 表和 3 张上游订单示例表；生产资源置备应使用白名单或单独 schema，避免误授权和备份膨胀。
8. JSON/LONGTEXT 用于模型证据是有意设计，但凡参与过滤、排序、唯一性或外部报表的字段应保持类型化列，避免 JSON 全表扫描。
9. 当前没有业务视图；如为后台或保险分析提供脱敏读取，应优先使用受控 API，确需数据库视图时必须加入租户和最小字段边界。
10. `jimu_report_share` 同一 `report_id` 同时存在 `uniq_jrs_report_id` 与 `uniq_report_id` 两个唯一索引，属于已确认的等价重复索引；应核对上游升级脚本后保留一个。其他前缀重叠索引可能服务不同排序/覆盖查询，未取得 `EXPLAIN` 证据前不应直接删除。

## 14. 优化建议

1. 修复失败的 Jeecg Flyway 迁移，并在发布门禁中同时校验 `flyway_schema_history` 与 `rehealth_schema_migration`。
2. 为保险逻辑关系建立定期孤儿检测 SQL；根据删除策略评审是否逐步增加物理外键。
3. 用生产形态数据对 Timescale `upload_batch_id` 联查、级联删除和 Outbox 扫描执行 `EXPLAIN (ANALYZE, BUFFERS)`，只补有证据的索引。
4. 为 ReHealth 自建表逐步添加 TABLE/COLUMN COMMENT 和版本化枚举说明。
5. 制定旧 MySQL 硬件表和 demo/test 表的归档/移除方案；生产操作必须通过迁移和备份，不得直接破坏性删除。
6. 将本生成器纳入文档检查：数据库结构变化后重新生成附录并检查表数、字段数和链接。
7. 在受控变更中合并 `jimu_report_share` 的重复唯一索引；其余疑似冗余索引先结合慢查询与 `EXPLAIN` 复核。

## 15. 资源置备与迁移

- MySQL ReHealth 基础与追加迁移位于 `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql`。
- TimescaleDB 使用 `backend/device-service/src/main/resources/db/migration/timescale` 下的 Flyway V1–V4；缺少 Timescale 扩展时必须失败关闭。
- Room 使用显式 1→16 迁移并导出 schema；升级不得用破坏性迁移替代正式迁移。
- 旧 MySQL `DATETIME(3)` 迁移到 `TIMESTAMPTZ` 前按 UTC 解释，调用方不得重复应用会话时区。
- 任何跨 `software_db`/`hardware_db` 一致性均通过状态、事件和重试实现，不使用分布式事务。

## 16. 文档生成与复核

```powershell
python tools/generate_database_schema_docs.py
python tools/validate_database_schema_docs.py
```

生成器只读取 catalog 和 Room schema，不读取业务列值。运行实例不可用时，应保留上一次已审核文档，并明确标记无法重新验证，不能以字段名猜测新结构。
"""


def module_summary(module: str) -> str:
    summaries = {
        "Android 可穿戴数据": "本地优先保存设备测量、睡眠、活动和信号",
        "Android 离线同步": "持久化上传与反馈重试",
        "Android 健康问答": "按用户隔离的本地会话和消息",
        "Android CVD 风险": "已确认风险日历史",
        "Android RDI": "本地 RDI 快照、贡献和证据",
        "Android RHI": "本地 RHI 日指数、领域、特征、质量和手工输入",
        "Android 饮食": "本地餐食及上传关联",
        "Android 早期骨架": "已注册但未完整接线的早期结构",
        "ReHealth 核心业务": "档案、访谈、绑定、风险、干预、问答和行为",
        "ReHealth 保险业务": "保险主体、保单、理赔、RWE、结算与审计",
        "ReHealth 审计日志": "模型请求最小审计元数据",
        "ReHealth 运营投影": "Kafka 生命周期和质量运营投影",
        "Jeecg 用户权限": "账号、角色、权限、租户、部门和关系",
        "Jeecg 字典": "系统字典及字典项",
        "Jeecg 日志": "系统操作和数据变更日志",
        "Jeecg 系统": "公告、配置、消息、文件和系统能力",
        "Jeecg Online": "在线表单/报表和拖拽页面元数据",
        "Jimu 报表": "积木报表配置、分享和导出",
        "AirAG / AI 平台": "AI 应用、知识库、模型、流程和提示词",
        "调度": "Quartz 调度元数据",
        "OpenAPI": "开放接口、授权、权限和调用日志",
        "旧 MySQL 硬件兼容": "权威切换前的硬件表，仅迁移兼容",
        "演示/测试": "上游示例和测试数据表",
        "上游订单示例": "Jeecg 示例订单结构",
        "迁移元数据": "Flyway/ReHealth 迁移历史",
        "硬件上传批次": "遥测批次和幂等收据",
        "硬件时序事实": "TimescaleDB 规范化测量、睡眠、活动和饮食",
        "硬件信号与质量": "信号元数据和质量事件",
        "硬件可靠性": "Outbox 和批次对账",
        "硬件迁移对账": "旧库迁移检查点",
    }
    return summaries.get(module, "具体用途见逐表附录")


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT, help="Repository root")
    args = parser.parse_args()
    root = args.root.resolve()

    mysql_tables, mysql_identity = mysql_metadata()
    pg_tables, pg_identity = postgres_metadata()
    room_tables, room_identity = room_metadata()

    docs_dir = root / "backend" / "docs"
    appendix_dir = docs_dir / "database"
    write_file(
        appendix_dir / "ROOM_SCHEMA_V16.md",
        render_appendix(
            "Android Room v16 数据库逐表结构",
            "结构来自 Room 导出的 `16.json`，共 22 张实际注册表；当前无已连接 Android 设备，因此数据量记为未知。",
            room_tables,
        ),
    )
    write_file(
        appendix_dir / "SOFTWARE_DB_TABLES.md",
        render_appendix(
            "MySQL software_db 数据库逐表结构",
            f"结构来自运行中的 `{mysql_identity['database']}`（MySQL {mysql_identity['version']}）information_schema，共 {len(mysql_tables)} 张基础表。InnoDB 行数为当前本地实例估算。",
            mysql_tables,
        ),
    )
    write_file(
        appendix_dir / "HARDWARE_DB_TABLES.md",
        render_appendix(
            "TimescaleDB hardware_db 数据库逐表结构",
            f"结构来自运行中的 `{pg_identity['database']}`（PostgreSQL {pg_identity['version']} / TimescaleDB {pg_identity['timescale_version']}），共 {len(pg_tables)} 张基础表。",
            pg_tables,
        ),
    )
    write_file(
        docs_dir / "REHEALTH_DB_SCHEMA.md",
        render_master(room_tables, mysql_tables, pg_tables, room_identity, mysql_identity, pg_identity),
    )
    print(json.dumps({
        "room_tables": len(room_tables),
        "mysql_tables": len(mysql_tables),
        "timescale_tables": len(pg_tables),
        "total_tables": len(room_tables) + len(mysql_tables) + len(pg_tables),
        "output": str(docs_dir / "REHEALTH_DB_SCHEMA.md"),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
