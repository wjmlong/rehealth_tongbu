# MySQL software_db 数据库逐表结构

> 本文件由 `tools/generate_database_schema_docs.py` 根据只读结构元数据生成。
> 不包含数据库账号、密码、业务行内容或原始健康数据。

结构来自运行中的 `rehealth_software`（MySQL 8.4.6）information_schema，共 193 张基础表。InnoDB 行数为当前本地实例估算。

## 表清单

| 序号 | 表名 | 中文名称 | 模块 | 主要用途 | 核心表 |
| ---: | --- | --- | --- | --- | --- |
| 1 | [`QRTZ_BLOB_TRIGGERS`](#qrtz-blob-triggers) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 2 | [`QRTZ_CALENDARS`](#qrtz-calendars) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 3 | [`QRTZ_CRON_TRIGGERS`](#qrtz-cron-triggers) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 4 | [`QRTZ_FIRED_TRIGGERS`](#qrtz-fired-triggers) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 5 | [`QRTZ_JOB_DETAILS`](#qrtz-job-details) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 6 | [`QRTZ_LOCKS`](#qrtz-locks) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 7 | [`QRTZ_PAUSED_TRIGGER_GRPS`](#qrtz-paused-trigger-grps) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 8 | [`QRTZ_SCHEDULER_STATE`](#qrtz-scheduler-state) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 9 | [`QRTZ_SIMPLE_TRIGGERS`](#qrtz-simple-triggers) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 10 | [`QRTZ_SIMPROP_TRIGGERS`](#qrtz-simprop-triggers) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 11 | [`QRTZ_TRIGGERS`](#qrtz-triggers) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 12 | [`aigc_word_template`](#aigc-word-template) | Word模版 | AirAG / AI 平台 | Word模版 | 否 |
| 13 | [`airag_app`](#airag-app) | 待确认 | AirAG / AI 平台 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 14 | [`airag_ext_data`](#airag-ext-data) | 通用扩展数据表 | AirAG / AI 平台 | 通用扩展数据表 | 否 |
| 15 | [`airag_flow`](#airag-flow) | 待确认 | AirAG / AI 平台 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 16 | [`airag_knowledge`](#airag-knowledge) | 待确认 | AirAG / AI 平台 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 17 | [`airag_knowledge_doc`](#airag-knowledge-doc) | 待确认 | AirAG / AI 平台 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 18 | [`airag_mcp`](#airag-mcp) | AI MCP | AirAG / AI 平台 | AI MCP | 否 |
| 19 | [`airag_model`](#airag-model) | 待确认 | AirAG / AI 平台 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 20 | [`airag_prompts`](#airag-prompts) | AI提示词表 | AirAG / AI 平台 | AI提示词表 | 否 |
| 21 | [`ccc`](#ccc) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 22 | [`demo`](#demo) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 23 | [`flyway_schema_history`](#flyway-schema-history) | Flyway 迁移历史表 | 迁移元数据 | 记录 Flyway 数据库迁移执行历史；不是业务数据。 | 否（迁移元数据） |
| 24 | [`hardware_activity`](#hardware-activity) | 硬件活动表 | 旧 MySQL 硬件兼容 | 保存规范化活动、步数、距离、热量、时长和心率。 | 否（迁移兼容） |
| 25 | [`hardware_data_quality_event`](#hardware-data-quality-event) | 硬件数据质量事件表 | 旧 MySQL 硬件兼容 | 保存遥测质量事件、严重程度和详情码。 | 否（迁移兼容） |
| 26 | [`hardware_measurement`](#hardware-measurement) | 硬件标量测量表 | 旧 MySQL 硬件兼容 | 保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。 | 否（迁移兼容） |
| 27 | [`hardware_signal_chunk_metadata`](#hardware-signal-chunk-metadata) | 硬件信号元数据表 | 旧 MySQL 硬件兼容 | 只保存信号时间窗、采样率和质量元数据，不保存原始波形。 | 否（迁移兼容） |
| 28 | [`hardware_sleep_session`](#hardware-sleep-session) | 硬件睡眠会话表 | 旧 MySQL 硬件兼容 | 保存规范化睡眠会话和阶段分钟数。 | 否（迁移兼容） |
| 29 | [`hardware_upload_batch`](#hardware-upload-batch) | 硬件上传批次表 | 旧 MySQL 硬件兼容 | 保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。 | 否（迁移兼容） |
| 30 | [`jeecg_order_customer`](#jeecg-order-customer) | 待确认 | 上游订单示例 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 31 | [`jeecg_order_main`](#jeecg-order-main) | 待确认 | 上游订单示例 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 32 | [`jeecg_order_ticket`](#jeecg-order-ticket) | 待确认 | 上游订单示例 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 33 | [`jimu_dict`](#jimu-dict) | 待确认 | Jimu 报表 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 34 | [`jimu_dict_item`](#jimu-dict-item) | 待确认 | Jimu 报表 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 35 | [`jimu_report`](#jimu-report) | 在线excel设计器 | Jimu 报表 | 在线excel设计器 | 否 |
| 36 | [`jimu_report_category`](#jimu-report-category) | 分类 | Jimu 报表 | 分类 | 否 |
| 37 | [`jimu_report_data_source`](#jimu-report-data-source) | 待确认 | Jimu 报表 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 38 | [`jimu_report_db`](#jimu-report-db) | 待确认 | Jimu 报表 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 39 | [`jimu_report_db_field`](#jimu-report-db-field) | 待确认 | Jimu 报表 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 40 | [`jimu_report_db_param`](#jimu-report-db-param) | 待确认 | Jimu 报表 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 41 | [`jimu_report_export_job`](#jimu-report-export-job) | 积木报表导出计划表 | Jimu 报表 | 积木报表导出计划表 | 否 |
| 42 | [`jimu_report_export_log`](#jimu-report-export-log) | 积木报表自动导出记录表 | Jimu 报表 | 积木报表自动导出记录表 | 否 |
| 43 | [`jimu_report_ext_data`](#jimu-report-ext-data) | 通用扩展数据表 | Jimu 报表 | 通用扩展数据表 | 否 |
| 44 | [`jimu_report_icon_lib`](#jimu-report-icon-lib) | 积木图库表 | Jimu 报表 | 积木图库表 | 否 |
| 45 | [`jimu_report_link`](#jimu-report-link) | 超链接配置表 | Jimu 报表 | 超链接配置表 | 否 |
| 46 | [`jimu_report_map`](#jimu-report-map) | 地图配置表 | Jimu 报表 | 地图配置表 | 否 |
| 47 | [`jimu_report_share`](#jimu-report-share) | 积木报表预览权限表 | Jimu 报表 | 积木报表预览权限表 | 否 |
| 48 | [`jimu_report_sheet`](#jimu-report-sheet) | 报表Sheet表 | Jimu 报表 | 报表Sheet表 | 否 |
| 49 | [`joa_demo`](#joa-demo) | 流程测试 | 演示/测试 | 流程测试 | 否 |
| 50 | [`oauth2_registered_client`](#oauth2-registered-client) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 51 | [`onl_auth_data`](#onl-auth-data) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 52 | [`onl_auth_page`](#onl-auth-page) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 53 | [`onl_auth_relation`](#onl-auth-relation) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 54 | [`onl_cgform_button`](#onl-cgform-button) | Online表单自定义按钮 | Jeecg Online | Online表单自定义按钮 | 否 |
| 55 | [`onl_cgform_enhance_java`](#onl-cgform-enhance-java) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 56 | [`onl_cgform_enhance_js`](#onl-cgform-enhance-js) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 57 | [`onl_cgform_enhance_sql`](#onl-cgform-enhance-sql) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 58 | [`onl_cgform_field`](#onl-cgform-field) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 59 | [`onl_cgform_head`](#onl-cgform-head) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 60 | [`onl_cgform_index`](#onl-cgform-index) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 61 | [`onl_cgreport_head`](#onl-cgreport-head) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 62 | [`onl_cgreport_item`](#onl-cgreport-item) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 63 | [`onl_cgreport_param`](#onl-cgreport-param) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 64 | [`onl_drag_comp`](#onl-drag-comp) | 组件库 | Jeecg Online | 组件库 | 否 |
| 65 | [`onl_drag_dataset_head`](#onl-drag-dataset-head) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 66 | [`onl_drag_dataset_item`](#onl-drag-dataset-item) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 67 | [`onl_drag_dataset_param`](#onl-drag-dataset-param) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 68 | [`onl_drag_page`](#onl-drag-page) | 可视化拖拽界面 | Jeecg Online | 可视化拖拽界面 | 否 |
| 69 | [`onl_drag_page_comp`](#onl-drag-page-comp) | 可视化拖拽页面组件 | Jeecg Online | 可视化拖拽页面组件 | 否 |
| 70 | [`onl_drag_share`](#onl-drag-share) | 仪表盘预览分享表 | Jeecg Online | 仪表盘预览分享表 | 否 |
| 71 | [`onl_drag_table_relation`](#onl-drag-table-relation) | 仪表盘聚合表 | Jeecg Online | 仪表盘聚合表 | 否 |
| 72 | [`onl_graphreport_head`](#onl-graphreport-head) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 73 | [`onl_graphreport_item`](#onl-graphreport-item) | jform_graphreport_item | Jeecg Online | jform_graphreport_item | 否 |
| 74 | [`onl_graphreport_params`](#onl-graphreport-params) | Online图表：参数表 | Jeecg Online | Online图表：参数表 | 否 |
| 75 | [`onl_graphreport_templet`](#onl-graphreport-templet) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 76 | [`onl_graphreport_templet_item`](#onl-graphreport-templet-item) | 待确认 | Jeecg Online | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 77 | [`open_api`](#open-api) | 接口表 | OpenAPI | 接口表 | 否 |
| 78 | [`open_api_auth`](#open-api-auth) | 权限表 | OpenAPI | 权限表 | 否 |
| 79 | [`open_api_log`](#open-api-log) | 调用记录表 | OpenAPI | 调用记录表 | 否 |
| 80 | [`open_api_permission`](#open-api-permission) | openapi授权 | OpenAPI | openapi授权 | 否 |
| 81 | [`oss_file`](#oss-file) | Oss File | Jeecg 系统 | Oss File | 否 |
| 82 | [`rehealth_ai_conversation`](#rehealth-ai-conversation) | 服务端健康问答会话表 | ReHealth 核心业务 | 保存按租户和用户隔离的权威健康问答会话。 | 是 |
| 83 | [`rehealth_ai_message`](#rehealth-ai-message) | 服务端健康问答消息表 | ReHealth 核心业务 | 保存健康问答完整消息历史、请求幂等键、Provider 和模型版本。 | 是 |
| 84 | [`rehealth_attribution_event`](#rehealth-attribution-event) | 归因请求事件表 | ReHealth 核心业务 | 保存提交给 PIAS 的个体归因请求元数据和版本化输入快照。 | 是 |
| 85 | [`rehealth_attribution_result`](#rehealth-attribution-result) | 个体归因结果表 | ReHealth 核心业务 | 保存 PIAS 个体归因结果及模型证据快照。 | 是 |
| 86 | [`rehealth_behavior_record`](#rehealth-behavior-record) | 结构化行为记录表 | ReHealth 核心业务 | 保存拍照食物/OCR 的已验证结构化结果；不保存原始图片。 | 是 |
| 87 | [`rehealth_care_plan`](#rehealth-care-plan) | 机构干预计划主表 | ReHealth 核心业务 | 保存按租户、机构类型和服务对象隔离的计划聚合、当前/草稿版本指针及乐观锁。 | 是 |
| 88 | [`rehealth_care_plan_audit_event`](#rehealth-care-plan-audit-event) | 机构干预计划审计表 | ReHealth 核心业务 | 保存不含计划正文的版本生命周期操作、内容哈希和变更原因。 | 是 |
| 89 | [`rehealth_care_plan_item`](#rehealth-care-plan-item) | 机构干预计划项目表 | ReHealth 核心业务 | 保存绑定到具体版本的患者可见计划项目快照及稳定逻辑项目标识。 | 是 |
| 90 | [`rehealth_care_plan_occurrence`](#rehealth-care-plan-occurrence) | 机构干预任务实例表 | ReHealth 核心业务 | 保存绑定计划版本和项目的到期任务实例，为后续真实依从性分母提供稳定标识。 | 是 |
| 91 | [`rehealth_care_plan_revision`](#rehealth-care-plan-revision) | 机构干预计划版本表 | ReHealth 核心业务 | 保存草稿、已发布和已撤回的计划版本；已发布内容不可原地覆盖。 | 是 |
| 92 | [`rehealth_cvd_feature_vector`](#rehealth-cvd-feature-vector) | CVD 特征向量表 | ReHealth 核心业务 | 保存一次 CVD-16 评估使用的版本化特征向量和质量证据。 | 是 |
| 93 | [`rehealth_cvd_risk_result`](#rehealth-cvd-risk-result) | CVD 风险结果表 | ReHealth 核心业务 | 保存模型风险分数、等级、模型贡献、Factor16 贡献、警告和模型版本。 | 是 |
| 94 | [`rehealth_device_binding`](#rehealth-device-binding) | 用户设备绑定表 | ReHealth 核心业务 | 保存认证用户与产品、稳定设备身份及状态的绑定关系。 | 是 |
| 95 | [`rehealth_health_interview`](#rehealth-health-interview) | 健康访谈主表 | ReHealth 核心业务 | 保存认证用户每次结构化健康访谈的主记录和兼容 JSON 快照。 | 是 |
| 96 | [`rehealth_health_interview_answer`](#rehealth-health-interview-answer) | 健康访谈回答表 | ReHealth 核心业务 | 保存访谈下的有序问答明细。 | 是 |
| 97 | [`rehealth_health_interview_baseline`](#rehealth-health-interview-baseline) | 健康访谈基线表 | ReHealth 核心业务 | 保存访谈提取的有序健康基线指标。 | 是 |
| 98 | [`rehealth_health_interview_focus`](#rehealth-health-interview-focus) | 健康访谈关注项表 | ReHealth 核心业务 | 保存访谈识别出的重点健康关注项。 | 是 |
| 99 | [`rehealth_insurance_audit_event`](#rehealth-insurance-audit-event) | 保险操作审计表 | ReHealth 保险业务 | 保存租户内保险资源操作的不可变审计事件和前后哈希。 | 是（保险域） |
| 100 | [`rehealth_insurance_claim`](#rehealth-insurance-claim) | 保险理赔表 | ReHealth 保险业务 | 保存理赔事件、金额、状态和保障代码。 | 是（保险域） |
| 101 | [`rehealth_insurance_consent`](#rehealth-insurance-consent) | 保险授权同意表 | ReHealth 保险业务 | 保存主体按类型和版本授予或撤销的授权及证据哈希。 | 是（保险域） |
| 102 | [`rehealth_insurance_coverage`](#rehealth-insurance-coverage) | 保险保障责任表 | ReHealth 保险业务 | 保存保单下的保障代码、限额、免赔额和有效期。 | 是（保险域） |
| 103 | [`rehealth_insurance_import_batch`](#rehealth-insurance-import-batch) | 待确认 | ReHealth 保险业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是（保险域） |
| 104 | [`rehealth_insurance_intervention`](#rehealth-insurance-intervention) | 保险干预参与表 | ReHealth 保险业务 | 保存主体加入健康干预计划的状态与反馈时间。 | 是（保险域） |
| 105 | [`rehealth_insurance_intervention_action`](#rehealth-insurance-intervention-action) | 保险人工干预行动表 | ReHealth 保险业务 | 保存租户和负责人范围内的随访、任务与人工复核行动及完成结果。 | 是（保险域） |
| 106 | [`rehealth_insurance_intervention_feedback`](#rehealth-insurance-intervention-feedback) | 待确认 | ReHealth 保险业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是（保险域） |
| 107 | [`rehealth_insurance_plan_binding`](#rehealth-insurance-plan-binding) | 待确认 | ReHealth 保险业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是（保险域） |
| 108 | [`rehealth_insurance_policy`](#rehealth-insurance-policy) | 保险保单表 | ReHealth 保险业务 | 保存租户内保单、产品、金额、期限和被保主体引用。 | 是（保险域） |
| 109 | [`rehealth_insurance_rwe_report`](#rehealth-insurance-rwe-report) | 真实世界证据报告表 | ReHealth 保险业务 | 保存版本化 RWE 报告及审批证据。 | 是（保险域） |
| 110 | [`rehealth_insurance_settlement_approval`](#rehealth-insurance-settlement-approval) | 保险结算审批记录表 | ReHealth 保险业务 | 保存结算包的审批动作、意见和请求幂等键。 | 是（保险域） |
| 111 | [`rehealth_insurance_settlement_package`](#rehealth-insurance-settlement-package) | 保险结算包表 | ReHealth 保险业务 | 保存由研究和报告形成的版本化结算证据包。 | 是（保险域） |
| 112 | [`rehealth_insurance_study`](#rehealth-insurance-study) | 保险研究定义表 | ReHealth 保险业务 | 保存真实世界研究人群、干预、结局规则和审批状态。 | 是（保险域） |
| 113 | [`rehealth_insurance_study_job`](#rehealth-insurance-study-job) | 待确认 | ReHealth 保险业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是（保险域） |
| 114 | [`rehealth_insurance_study_member`](#rehealth-insurance-study-member) | 保险研究成员表 | ReHealth 保险业务 | 保存研究快照中的去标识主体、队列分组和结局值。 | 是（保险域） |
| 115 | [`rehealth_insurance_study_result`](#rehealth-insurance-study-result) | 保险研究结果表 | ReHealth 保险业务 | 保存 PSM/真实世界研究估计、区间、平衡和成本结果。 | 是（保险域） |
| 116 | [`rehealth_insurance_study_snapshot`](#rehealth-insurance-study-snapshot) | 保险研究快照表 | ReHealth 保险业务 | 保存研究人群不可变快照、来源水位和内容哈希。 | 是（保险域） |
| 117 | [`rehealth_insurance_subject`](#rehealth-insurance-subject) | 保险业务主体表 | ReHealth 保险业务 | 保存租户隔离、去标识化的保险主体与 ReHealth 用户映射。 | 是（保险域） |
| 118 | [`rehealth_insurance_subject_manager`](#rehealth-insurance-subject-manager) | 待确认 | ReHealth 保险业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是（保险域） |
| 119 | [`rehealth_insurance_tenant_profile`](#rehealth-insurance-tenant-profile) | 待确认 | ReHealth 保险业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是（保险域） |
| 120 | [`rehealth_intervention_contraindication`](#rehealth-intervention-contraindication) | 干预禁忌表 | ReHealth 核心业务 | 保存某次干预计划包含的有序禁忌与安全限制。 | 是 |
| 121 | [`rehealth_intervention_feedback`](#rehealth-intervention-feedback) | 干预反馈表 | ReHealth 核心业务 | 保存用户对具体干预计划/行动的完成、跳过或不适用反馈。 | 是 |
| 122 | [`rehealth_intervention_plan`](#rehealth-intervention-plan) | 健康干预计划表 | ReHealth 核心业务 | 保存基于权威画像、风险和设备行为上下文生成的结构化保守干预计划。 | 是 |
| 123 | [`rehealth_model_request_log`](#rehealth-model-request-log) | 模型请求审计表 | ReHealth 审计日志 | 保存不含原始 PII/遥测的模型调用元数据、状态、耗时和错误码。 | 否（日志/支持） |
| 124 | [`rehealth_patient_allergy`](#rehealth-patient-allergy) | 患者过敏史表 | ReHealth 核心业务 | 保存健康档案下的有序过敏条目。 | 是 |
| 125 | [`rehealth_patient_diagnosis`](#rehealth-patient-diagnosis) | 患者诊断史表 | ReHealth 核心业务 | 保存健康档案下的有序诊断史条目。 | 是 |
| 126 | [`rehealth_patient_medication`](#rehealth-patient-medication) | 患者用药史表 | ReHealth 核心业务 | 保存健康档案下的有序用药条目。 | 是 |
| 127 | [`rehealth_patient_profile`](#rehealth-patient-profile) | 患者健康档案表 | ReHealth 核心业务 | 保存认证用户的类型化健康档案、BMI 和乐观锁版本。 | 是 |
| 128 | [`rehealth_rdi_contribution`](#rehealth-rdi-contribution) | 待确认 | ReHealth 核心业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是 |
| 129 | [`rehealth_rdi_daily_snapshot`](#rehealth-rdi-daily-snapshot) | 待确认 | ReHealth 核心业务 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 是 |
| 130 | [`rehealth_rhi_daily_snapshot`](#rehealth-rhi-daily-snapshot) | 云端 RHI 每日聚合快照表 | ReHealth 核心业务 | 保存认证用户从 App 上传的日级 RHI 分数、领域、特征与质量聚合快照；不保存原始遥测。 | 是 |
| 131 | [`rehealth_rhi_manual_health_input`](#rehealth-rhi-manual-health-input) | 云端 RHI 手工输入表 | ReHealth 核心业务 | 保存认证用户 Room-first 手工健康输入的云端副本，并按 updated_at 合并。 | 是 |
| 132 | [`rehealth_schema_migration`](#rehealth-schema-migration) | ReHealth 迁移版本表 | 迁移元数据 | 记录 ReHealth 自定义软件库迁移版本；不是业务数据。 | 否（迁移元数据） |
| 133 | [`rehealth_telemetry_event_projection`](#rehealth-telemetry-event-projection) | 遥测事件运营投影表 | ReHealth 运营投影 | 保存 Kafka 遥测生命周期事件的隐私安全运营投影。 | 是 |
| 134 | [`rehealth_telemetry_quality_case`](#rehealth-telemetry-quality-case) | 遥测质量工单表 | ReHealth 运营投影 | 保存由遥测质量事件派生的运营质量工单。 | 是 |
| 135 | [`rehealth_website_record`](#rehealth-website-record) | 官网业务记录表 | ReHealth 核心业务 | 保存官网侧按租户隔离的结构化业务记录；具体记录类型由业务代码定义。 | 是 |
| 136 | [`rep_demo_dxtj`](#rep-demo-dxtj) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 137 | [`rep_demo_employee`](#rep-demo-employee) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 138 | [`rep_demo_gongsi`](#rep-demo-gongsi) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 139 | [`rep_demo_jianpiao`](#rep-demo-jianpiao) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 140 | [`rep_demo_order_main`](#rep-demo-order-main) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 141 | [`rep_demo_order_product`](#rep-demo-order-product) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 142 | [`sys_announcement`](#sys-announcement) | 系统通告表 | Jeecg 系统 | 系统通告表 | 否 |
| 143 | [`sys_announcement_send`](#sys-announcement-send) | 用户通告阅读标记表 | Jeecg 系统 | 用户通告阅读标记表 | 否 |
| 144 | [`sys_category`](#sys-category) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 145 | [`sys_check_rule`](#sys-check-rule) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 146 | [`sys_comment`](#sys-comment) | 系统评论回复表 | Jeecg 系统 | 系统评论回复表 | 否 |
| 147 | [`sys_data_log`](#sys-data-log) | 待确认 | Jeecg 日志 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 148 | [`sys_data_source`](#sys-data-source) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 149 | [`sys_depart`](#sys-depart) | 组织机构表 | Jeecg 用户权限 | 组织机构表 | 否 |
| 150 | [`sys_depart_permission`](#sys-depart-permission) | 部门权限表 | Jeecg 用户权限 | 部门权限表 | 否 |
| 151 | [`sys_depart_role`](#sys-depart-role) | 部门角色表 | Jeecg 用户权限 | 部门角色表 | 否 |
| 152 | [`sys_depart_role_permission`](#sys-depart-role-permission) | 部门角色权限表 | Jeecg 用户权限 | 部门角色权限表 | 否 |
| 153 | [`sys_depart_role_user`](#sys-depart-role-user) | 部门角色用户表 | Jeecg 用户权限 | 部门角色用户表 | 否 |
| 154 | [`sys_dict`](#sys-dict) | 待确认 | Jeecg 字典 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 155 | [`sys_dict_item`](#sys-dict-item) | 待确认 | Jeecg 字典 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 156 | [`sys_files`](#sys-files) | 知识库-文档管理 | Jeecg 系统 | 知识库-文档管理 | 否 |
| 157 | [`sys_fill_rule`](#sys-fill-rule) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 158 | [`sys_form_file`](#sys-form-file) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 159 | [`sys_gateway_route`](#sys-gateway-route) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 160 | [`sys_log`](#sys-log) | 系统日志表 | Jeecg 日志 | 系统日志表 | 否 |
| 161 | [`sys_permission`](#sys-permission) | 菜单权限表 | Jeecg 用户权限 | 菜单权限表 | 是（平台基础） |
| 162 | [`sys_permission_data_rule`](#sys-permission-data-rule) | 待确认 | Jeecg 用户权限 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 163 | [`sys_position`](#sys-position) | 职务级别 | Jeecg 用户权限 | 职务级别 | 否 |
| 164 | [`sys_quartz_job`](#sys-quartz-job) | 待确认 | 调度 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 165 | [`sys_role`](#sys-role) | 角色表 | Jeecg 用户权限 | 角色表 | 是（平台基础） |
| 166 | [`sys_role_index`](#sys-role-index) | 角色首页表 | Jeecg 用户权限 | 角色首页表 | 否 |
| 167 | [`sys_role_permission`](#sys-role-permission) | 角色权限表 | Jeecg 用户权限 | 角色权限表 | 是（平台基础） |
| 168 | [`sys_sms`](#sys-sms) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 169 | [`sys_sms_template`](#sys-sms-template) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 170 | [`sys_table_white_list`](#sys-table-white-list) | 系统表白名单 | Jeecg 系统 | 系统表白名单 | 否 |
| 171 | [`sys_tenant`](#sys-tenant) | 多租户信息表 | Jeecg 用户权限 | 多租户信息表 | 是（平台基础） |
| 172 | [`sys_tenant_pack`](#sys-tenant-pack) | 租户产品包 | Jeecg 用户权限 | 租户产品包 | 否 |
| 173 | [`sys_tenant_pack_perms`](#sys-tenant-pack-perms) | 租户产品包和菜单关系表 | Jeecg 用户权限 | 租户产品包和菜单关系表 | 否 |
| 174 | [`sys_tenant_pack_user`](#sys-tenant-pack-user) | 租户套餐人员表 | Jeecg 用户权限 | 租户套餐人员表 | 否 |
| 175 | [`sys_third_account`](#sys-third-account) | 待确认 | Jeecg 系统 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 176 | [`sys_third_app_config`](#sys-third-app-config) | 租户第三方配置表 | Jeecg 系统 | 租户第三方配置表 | 否 |
| 177 | [`sys_ugroup`](#sys-ugroup) | 用户组表 | Jeecg 用户权限 | 用户组表 | 否 |
| 178 | [`sys_ugroup_user`](#sys-ugroup-user) | 用户组关系表 | Jeecg 用户权限 | 用户组关系表 | 否 |
| 179 | [`sys_user`](#sys-user) | 用户表 | Jeecg 用户权限 | 用户表 | 是（平台基础） |
| 180 | [`sys_user_dep_post`](#sys-user-dep-post) | 待确认 | Jeecg 用户权限 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 181 | [`sys_user_depart`](#sys-user-depart) | 待确认 | Jeecg 用户权限 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 182 | [`sys_user_position`](#sys-user-position) | 待确认 | Jeecg 用户权限 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 183 | [`sys_user_role`](#sys-user-role) | 用户角色表 | Jeecg 用户权限 | 用户角色表 | 是（平台基础） |
| 184 | [`sys_user_tenant`](#sys-user-tenant) | 用户租户关系表 | Jeecg 用户权限 | 用户租户关系表 | 否 |
| 185 | [`test_demo`](#test-demo) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 186 | [`test_enhance_select`](#test-enhance-select) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 187 | [`test_note`](#test-note) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 188 | [`test_online_link`](#test-online-link) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 189 | [`test_order_customer`](#test-order-customer) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 190 | [`test_order_main`](#test-order-main) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 191 | [`test_order_product`](#test-order-product) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 192 | [`test_person`](#test-person) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |
| 193 | [`test_shoptype_tree`](#test-shoptype-tree) | 待确认 | 演示/测试 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 | 否 |

## 模块统计

| 模块 | 表数 |
| --- | ---: |
| AirAG / AI 平台 | 9 |
| Jeecg Online | 26 |
| Jeecg 字典 | 2 |
| Jeecg 日志 | 2 |
| Jeecg 用户权限 | 23 |
| Jeecg 系统 | 17 |
| Jimu 报表 | 16 |
| OpenAPI | 4 |
| ReHealth 保险业务 | 21 |
| ReHealth 审计日志 | 1 |
| ReHealth 核心业务 | 29 |
| ReHealth 运营投影 | 2 |
| 上游订单示例 | 3 |
| 旧 MySQL 硬件兼容 | 6 |
| 演示/测试 | 18 |
| 调度 | 12 |
| 迁移元数据 | 2 |

## 1. 表：`QRTZ_BLOB_TRIGGERS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_BLOB_TRIGGERS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `TRIGGER_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `BLOB_DATA` | 待确认 | `blob` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- `QRTZ_BLOB_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)` → `QRTZ_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)`：物理外键；ON DELETE RESTRICT。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 2. 表：`QRTZ_CALENDARS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_CALENDARS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, CALENDAR_NAME` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `CALENDAR_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `CALENDAR` | 待确认 | `blob` | 65535 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, CALENDAR_NAME` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 3. 表：`QRTZ_CRON_TRIGGERS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_CRON_TRIGGERS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `TRIGGER_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `CRON_EXPRESSION` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `TIME_ZONE_ID` | 待确认 | `varchar(80)` | 80 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- `QRTZ_CRON_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)` → `QRTZ_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)`：物理外键；ON DELETE RESTRICT。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 4. 表：`QRTZ_FIRED_TRIGGERS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_FIRED_TRIGGERS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, ENTRY_ID` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `ENTRY_ID` | 待确认 | `varchar(95)` | 95 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `TRIGGER_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `INSTANCE_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `FIRED_TIME` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `SCHED_TIME` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `PRIORITY` | 待确认 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `STATE` | 待确认 | `varchar(16)` | 16 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `JOB_NAME` | 待确认 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `JOB_GROUP` | 待确认 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `IS_NONCONCURRENT` | 待确认 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 13 | `REQUESTS_RECOVERY` | 待确认 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, ENTRY_ID` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 5. 表：`QRTZ_JOB_DETAILS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_JOB_DETAILS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, JOB_NAME, JOB_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `JOB_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `JOB_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `DESCRIPTION` | 待确认 | `varchar(250)` | 250 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `JOB_CLASS_NAME` | 待确认 | `varchar(250)` | 250 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `IS_DURABLE` | 待确认 | `varchar(1)` | 1 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `IS_NONCONCURRENT` | 待确认 | `varchar(1)` | 1 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `IS_UPDATE_DATA` | 待确认 | `varchar(1)` | 1 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `REQUESTS_RECOVERY` | 待确认 | `varchar(1)` | 1 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `JOB_DATA` | 待确认 | `blob` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, JOB_NAME, JOB_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 6. 表：`QRTZ_LOCKS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_LOCKS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, LOCK_NAME` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 2 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `LOCK_NAME` | 待确认 | `varchar(40)` | 40 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, LOCK_NAME` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 7. 表：`QRTZ_PAUSED_TRIGGER_GRPS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_PAUSED_TRIGGER_GRPS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, TRIGGER_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, TRIGGER_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 8. 表：`QRTZ_SCHEDULER_STATE` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_SCHEDULER_STATE` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, INSTANCE_NAME` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 1 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `INSTANCE_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `LAST_CHECKIN_TIME` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `CHECKIN_INTERVAL` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, INSTANCE_NAME` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 9. 表：`QRTZ_SIMPLE_TRIGGERS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_SIMPLE_TRIGGERS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `TRIGGER_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `REPEAT_COUNT` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `REPEAT_INTERVAL` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `TIMES_TRIGGERED` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- `QRTZ_SIMPLE_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)` → `QRTZ_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)`：物理外键；ON DELETE RESTRICT。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 10. 表：`QRTZ_SIMPROP_TRIGGERS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_SIMPROP_TRIGGERS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `TRIGGER_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 物理→QRTZ_TRIGGERS.SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `STR_PROP_1` | 待确认 | `varchar(512)` | 512 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `STR_PROP_2` | 待确认 | `varchar(512)` | 512 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `STR_PROP_3` | 待确认 | `varchar(512)` | 512 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `INT_PROP_1` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `INT_PROP_2` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `LONG_PROP_1` | 待确认 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `LONG_PROP_2` | 待确认 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `DEC_PROP_1` | 待确认 | `decimal(13,4)` | 13,4 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `DEC_PROP_2` | 待确认 | `decimal(13,4)` | 13,4 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 13 | `BOOL_PROP_1` | 待确认 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 14 | `BOOL_PROP_2` | 待确认 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |

### 关联关系

- `QRTZ_SIMPROP_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)` → `QRTZ_TRIGGERS.(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)`：物理外键；ON DELETE RESTRICT。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 11. 表：`QRTZ_TRIGGERS` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `QRTZ_TRIGGERS` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `SCHED_NAME` | 待确认 | `varchar(120)` | 120 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY、SCHED_NAME | 否 | 物理→QRTZ_JOB_DETAILS.SCHED_NAME,JOB_NAME,JOB_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `TRIGGER_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `TRIGGER_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 是 | 否 | 联合唯一:PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `JOB_NAME` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | SCHED_NAME | 否 | 物理→QRTZ_JOB_DETAILS.SCHED_NAME,JOB_NAME,JOB_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `JOB_GROUP` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | SCHED_NAME | 否 | 物理→QRTZ_JOB_DETAILS.SCHED_NAME,JOB_NAME,JOB_GROUP | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `DESCRIPTION` | 待确认 | `varchar(250)` | 250 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `NEXT_FIRE_TIME` | 待确认 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `PREV_FIRE_TIME` | 待确认 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `PRIORITY` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `TRIGGER_STATE` | 待确认 | `varchar(16)` | 16 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `TRIGGER_TYPE` | 待确认 | `varchar(8)` | 8 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `START_TIME` | 待确认 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 13 | `END_TIME` | 待确认 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 14 | `CALENDAR_NAME` | 待确认 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 15 | `MISFIRE_INSTR` | 待确认 | `smallint` | 5,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 16 | `JOB_DATA` | 待确认 | `blob` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP` | 主键（联合） | 保证记录唯一并支持主键定位。 |
| `SCHED_NAME` | `SCHED_NAME, JOB_NAME, JOB_GROUP` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `QRTZ_TRIGGERS.(SCHED_NAME, JOB_NAME, JOB_GROUP)` → `QRTZ_JOB_DETAILS.(SCHED_NAME, JOB_NAME, JOB_GROUP)`：物理外键；ON DELETE RESTRICT。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 12. 表：`aigc_word_template` Word模版

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `aigc_word_template` |
| 中文名称 | Word模版 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | Word模版 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `name` | 模版名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模版名称 |
| 8 | `code` | 模版编码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模版编码 |
| 9 | `header` | 页眉 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 页眉 |
| 10 | `footer` | 页脚 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 页脚 |
| 11 | `main` | 主体内容 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主体内容 |
| 12 | `margins` | 页边距 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 页边距 |
| 13 | `width` | 宽度 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 宽度 |
| 14 | `height` | 高度 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 高度 |
| 15 | `paper_direction` | 纸张方向 vertical纵向 horizontal横向 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 纸张方向 vertical纵向 horizontal横向 |
| 16 | `watermark` | 水印 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 水印 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

Word模版

## 13. 表：`airag_app` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_app` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 19 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `name` | 应用名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 应用名称 |
| 9 | `descr` | 应用描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 应用描述 |
| 10 | `icon` | 应用图标 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 应用图标 |
| 11 | `type` | 应用类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 应用类型 |
| 12 | `prologue` | 开场白 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 开场白 |
| 13 | `prompt` | 提示词 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 提示词 |
| 14 | `model_id` | 模型id | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 模型id |
| 15 | `knowledge_ids` | 知识库 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 知识库 |
| 16 | `flow_id` | 流程id（多个以逗号分隔） | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 流程id（多个以逗号分隔） |
| 17 | `status` | 状态（enable=启用、disable=禁用、release=发布） | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态（enable=启用、disable=禁用、release=发布） | 状态（enable=启用、disable=禁用、release=发布） |
| 18 | `msg_num` | 历史消息数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 历史消息数 |
| 19 | `metadata` | 元数据 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据 |
| 20 | `preset_question` | 预设问题 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 预设问题 |
| 21 | `quick_command` | 快捷指令 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 快捷指令 |
| 22 | `plugins` | 插件 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 插件 |
| 23 | `memory_id` | 记忆库(知识库的id) | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 记忆库(知识库的id) |
| 24 | `variables` | 存放变量的配置 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 存放变量的配置 |
| 25 | `iz_open_memory` | 是否开启记忆(0 不开启，1开启) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否开启记忆(0 不开启，1开启) | 是否开启记忆(0 不开启，1开启) |
| 26 | `memory_prompt` | 记忆和变量提示词 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 记忆和变量提示词 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_app.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态（enable=启用、disable=禁用、release=发布）。
- `iz_open_memory`：是否开启记忆(0 不开启，1开启)。
- `type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 14. 表：`airag_ext_data` 通用扩展数据表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_ext_data` |
| 中文名称 | 通用扩展数据表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | 通用扩展数据表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键ID |
| 2 | `biz_type` | 业务类型标识（ evaluator:评估器；track:测试追踪 ） | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | idx_biz | 否 | 否 | 业务类型标识（ evaluator:评估器；track:测试追踪 ） | 业务类型标识（ evaluator:评估器；track:测试追踪 ） |
| 3 | `name` | 名称 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称 |
| 4 | `descr` | 描述信息 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述信息 |
| 5 | `tags` | 标签，多个用逗号分隔 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标签，多个用逗号分隔 |
| 6 | `data_value` | 实际存储内容，json | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 实际存储内容，json |
| 7 | `status` | 状态（run:进行中 completed：已完成） | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态（run:进行中 completed：已完成） | 状态（run:进行中 completed：已完成） |
| 8 | `dataset_value` | 评测集数据 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 评测集数据 |
| 9 | `metadata` | 元数据，用于存储补充业务数据信息 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据，用于存储补充业务数据信息 |
| 10 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 11 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `CURRENT_TIMESTAMP` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 12 | `update_by` | 修改人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 13 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `CURRENT_TIMESTAMP` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 14 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 15 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 16 | `version` | 版本1开始 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 版本1开始 | 版本1开始 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_biz` | `biz_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_ext_data.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `biz_type`：业务类型标识（ evaluator:评估器；track:测试追踪 ）。
- `status`：状态（run:进行中 completed：已完成）。
- `version`：版本1开始。

### 业务说明

通用扩展数据表

## 15. 表：`airag_flow` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_flow` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 20 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `application_name` | 应用名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 应用名称 |
| 9 | `name` | 名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称 |
| 10 | `descr` | 描述 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 11 | `icon` | 应用图标 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 应用图标 |
| 12 | `chain` | 编排规则 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 编排规则 |
| 13 | `design` | 编排设计 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 编排设计 |
| 14 | `status` | 状态（enable=启用、disable=禁用、release=发布） | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态（enable=启用、disable=禁用、release=发布） | 状态（enable=启用、disable=禁用、release=发布） |
| 15 | `metadata` | 元数据 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据 |
| 16 | `trigger_cron` | cron定时任务触发器配置JSON | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | cron定时任务触发器配置JSON |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_flow.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态（enable=启用、disable=禁用、release=发布）。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 16. 表：`airag_knowledge` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_knowledge` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `name` | 知识库名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 知识库名称 |
| 9 | `descr` | 描述 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 10 | `embed_id` | 向量模型id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 向量模型id |
| 11 | `status` | 状态 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态 |
| 12 | `type` | 类型(knowledge知识 memory 记忆) | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 类型(knowledge知识 memory 记忆) |
| 13 | `metadata` | 元数据 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_knowledge.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status, type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 17. 表：`airag_knowledge_doc` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_knowledge_doc` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 41 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `knowledge_id` | 知识库id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 知识库id |
| 9 | `title` | 标题 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标题 |
| 10 | `type` | 类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 类型 |
| 11 | `content` | 内容 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 内容 |
| 12 | `status` | 状态 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态 |
| 13 | `metadata` | 元数据 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_knowledge_doc.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `type, status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 18. 表：`airag_mcp` AI MCP

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_mcp` |
| 中文名称 | AI MCP |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | AI MCP |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 9 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `icon` | 图标 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图标 |
| 3 | `name` | 名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称 |
| 4 | `descr` | 描述 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 5 | `category` | 类型（plugin=插件，mcp=MCP） | `varchar(20)` | 20 | 是 | `mcp` | 否 | 否 | 否 | 否 | 否 | 否 | 类型（plugin=插件，mcp=MCP） | 类型（plugin=插件，mcp=MCP） |
| 6 | `type` | mcp类型（sse：sse类型；stdio：标准类型） | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | mcp类型（sse：sse类型；stdio：标准类型） | mcp类型（sse：sse类型；stdio：标准类型） |
| 7 | `endpoint` | 服务端点（SSE类型为URL，stdio类型为命令） | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 服务端点（SSE类型为URL，stdio类型为命令） |
| 8 | `headers` | 请求头（sse类型）、环境变量（stdio类型） | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求头（sse类型）、环境变量（stdio类型） |
| 9 | `tools` | 工具列表 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 工具列表 |
| 10 | `status` | 状态（enable=启用、disable=禁用） | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态（enable=启用、disable=禁用） | 状态（enable=启用、disable=禁用） |
| 11 | `synced` | 是否同步 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否同步 |
| 12 | `metadata` | 元数据 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据 |
| 13 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 14 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 15 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 16 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 17 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 18 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_mcp.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `category`：类型（plugin=插件，mcp=MCP）。
- `type`：mcp类型（sse：sse类型；stdio：标准类型）。
- `status`：状态（enable=启用、disable=禁用）。

### 业务说明

AI MCP

## 19. 表：`airag_model` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_model` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `name` | 名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称 |
| 9 | `provider` | 供应者 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 供应者 |
| 10 | `model_name` | 模型名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型名称 |
| 11 | `credential` | 凭证信息 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 凭证信息 |
| 12 | `base_url` | API域名 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | API域名 |
| 13 | `model_type` | 模型类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型类型 |
| 14 | `model_params` | 模型参数 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型参数 |
| 15 | `activate_flag` | 是否激活（1=是，0=否） | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否激活（1=是，0=否） | 是否激活（1=是，0=否） |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `airag_model.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `activate_flag`：是否激活（1=是，0=否）。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 20. 表：`airag_prompts` AI提示词表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `airag_prompts` |
| 中文名称 | AI提示词表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | AirAG / AI 平台 |
| 业务作用 | AI提示词表 |
| 主键 | `无/待确认` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 5 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 主键ID |
| 2 | `name` | 提示词名称 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | idx_name | 否 | 否 | — | 提示词名称 |
| 3 | `prompt_key` | 提示词key | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | uni_key | uni_key | 否 | 否 | — | 提示词key |
| 4 | `description` | 提示词功能描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 提示词功能描述 |
| 5 | `content` | 提示词模板内容，支持变量占位符如 {{variable}} | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 提示词模板内容，支持变量占位符如 {{variable}} |
| 6 | `category` | 提示词分类 | `varchar(60)` | 60 | 是 | `无/NULL` | 否 | 否 | 否 | idx_category | 否 | 否 | 具体枚举值待确认 | 提示词分类 |
| 7 | `tags` | 标签，多个逗号分割 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标签，多个逗号分割 |
| 8 | `model_id` | 适配的大模型ID | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 适配的大模型ID |
| 9 | `model_param` | 大模型的参数配置 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 大模型的参数配置 |
| 10 | `status` | 状态（0:未发布 1:已发布） | `varchar(25)` | 25 | 是 | `0` | 否 | 否 | 否 | idx_status | 是 | 否 | 状态（0:未发布 1:已发布） | 状态（0:未发布 1:已发布） |
| 11 | `version` | 版本号(格式 0.0.1) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 版本号(格式 0.0.1) | 版本号(格式 0.0.1) |
| 12 | `del_flag` | 删除状态（0未删除 1已删除） | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 删除状态（0未删除 1已删除） | 删除状态（0未删除 1已删除） |
| 13 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 14 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 15 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 16 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 17 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 18 | `tenant_id` | 租户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_category` | `category` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_name` | `name` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `uni_key` | `prompt_key` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `airag_prompts.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态（0:未发布 1:已发布）。
- `version`：版本号(格式 0.0.1)。
- `del_flag`：删除状态（0未删除 1已删除）。
- `category`：状态/类型类字段，完整枚举值待确认。

### 业务说明

AI提示词表

## 21. 表：`ccc` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `ccc` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `has_child` | 是否有子节点 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否有子节点 |
| 3 | `pid` | 父级节点 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 父级节点 |
| 4 | `name` | name | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | name |
| 5 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 7 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 8 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 9 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 22. 表：`demo` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `demo` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(50)` | 50 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键ID |
| 2 | `name` | 姓名 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 姓名 |
| 3 | `key_word` | 关键词 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 关键词 |
| 4 | `punch_time` | 打卡时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 打卡时间 |
| 5 | `salary_money` | 工资 | `decimal(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 工资 |
| 6 | `bonus_money` | 奖金 | `double(10,2)` | 10,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 奖金 |
| 7 | `sex` | 性别 {男:1,女:2} | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 性别 {男:1,女:2} | 性别 {男:1,女:2} |
| 8 | `age` | 年龄 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 年龄 |
| 9 | `birthday` | 生日 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 生日 |
| 10 | `email` | 邮箱 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 邮箱 |
| 11 | `content` | 个人简介 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 个人简介 |
| 12 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 13 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 14 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 15 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 16 | `sys_org_code` | 所属部门编码 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门编码 |
| 17 | `tenant_id` | 租户 ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 18 | `update_count` | 乐观锁测试 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 乐观锁测试 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `demo.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `sex`：性别 {男:1,女:2}。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 23. 表：`flyway_schema_history` Flyway 迁移历史表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `flyway_schema_history` |
| 中文名称 | Flyway 迁移历史表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 迁移元数据 |
| 业务作用 | 记录 Flyway 数据库迁移执行历史；不是业务数据。 |
| 主键 | `installed_rank` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 17 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移元数据） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `installed_rank` | 待确认 | `int` | 10,0 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `version` | 版本 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录或配置版本；是否为乐观锁需结合实体 @Version 判断。 |
| 3 | `description` | 描述 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前记录的业务内容描述。 |
| 4 | `type` | 类型 | `varchar(20)` | 20 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 当前记录的分类类型；具体枚举值需以所在模块代码或字典为准。 |
| 5 | `script` | 待确认 | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `checksum` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `installed_by` | 待确认 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `installed_on` | 日期 | `timestamp` | 不适用 | 否 | `CURRENT_TIMESTAMP` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务日期待确认。 |
| 9 | `execution_time` | 待确认 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `success` | 待确认 | `tinyint(1)` | 3,0 | 否 | `无/NULL` | 否 | 否 | 否 | flyway_schema_history_s_idx | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `flyway_schema_history_s_idx` | `success` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `installed_rank` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

记录 Flyway 数据库迁移执行历史；不是业务数据。

## 24. 表：`hardware_activity` 硬件活动表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_activity` |
| 中文名称 | 硬件活动表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 旧 MySQL 硬件兼容 |
| 业务作用 | 保存规范化活动、步数、距离、热量、时长和心率。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移兼容） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | fk_hardware_activity_batch | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `client_record_id` | 待确认 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_activity_user_time | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_activity_device_time | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `started_at` | 开始时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_activity_device_time、idx_hardware_activity_user_time | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 7 | `ended_at` | 结束时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗结束时间。 |
| 8 | `activity_type` | 活动类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识活动记录的类型；具体允许值由设备 Provider 映射定义。 |
| 9 | `steps` | 步数 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动时间窗或自然日内的设备步数。 |
| 10 | `distance_meters` | 距离 | `decimal(20,3)` | 20,3 | 否 | `0.000` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动距离，单位米。 |
| 11 | `calories_kcal` | 热量 | `decimal(20,3)` | 20,3 | 否 | `0.000` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食或活动能量，单位千卡。 |
| 12 | `duration_minutes` | 持续时长 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动持续分钟数。 |
| 13 | `average_heart_rate` | 平均心率 | `decimal(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 活动或 ECG 测量期间的平均心率。 |
| 14 | `source` | 数据来源 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 15 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_hardware_activity_batch` | `upload_batch_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_activity_device_time` | `device_id, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_hardware_activity_user_time` | `user_id, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `hardware_activity.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE NO ACTION。
- `hardware_activity.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存规范化活动、步数、距离、热量、时长和心率。

注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。

## 25. 表：`hardware_data_quality_event` 硬件数据质量事件表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_data_quality_event` |
| 中文名称 | 硬件数据质量事件表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 旧 MySQL 硬件兼容 |
| 业务作用 | 保存遥测质量事件、严重程度和详情码。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移兼容） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | fk_hardware_quality_batch | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_quality_user_time | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_quality_device_time | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 5 | `event_type` | 事件类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识质量、Outbox、归因或审计事件的业务类型。 |
| 6 | `severity` | 严重程度 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 质量事件严重程度，受数据库 CHECK 约束。 |
| 7 | `message` | 消息文本 | `varchar(512)` | 512 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前事件、错误或业务消息文本。 |
| 8 | `occurred_at` | 时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_quality_device_time、idx_hardware_quality_user_time | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 9 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_hardware_quality_batch` | `upload_batch_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_quality_device_time` | `device_id, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_quality_user_time` | `user_id, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `hardware_data_quality_event.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE NO ACTION。
- `hardware_data_quality_event.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存遥测质量事件、严重程度和详情码。

注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。

## 26. 表：`hardware_measurement` 硬件标量测量表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_measurement` |
| 中文名称 | 硬件标量测量表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 旧 MySQL 硬件兼容 |
| 业务作用 | 保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 62 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移兼容） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | fk_hardware_measurement_batch | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `client_record_id` | 待确认 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_measurement_user_time | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_measurement_device_time | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `metric_type` | 指标类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_measurement_metric_time | 否 | 否 | — | 标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。 |
| 7 | `measured_at` | 测量时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_measurement_device_time、idx_hardware_measurement_metric_time、idx_hardware_measurement_user_time | 否 | 否 | — | 健康指标实际测量时间。 |
| 8 | `primary_value` | 主测量值 | `decimal(20,6)` | 20,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化测量的主要数值，例如单值指标或血压收缩压分量。 |
| 9 | `secondary_value` | 次测量值 | `decimal(20,6)` | 20,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化测量的可选第二数值，例如成对测量的第二分量。 |
| 10 | `unit` | 计量单位 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 11 | `quality_code` | 质量代码 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规范化的设备或遥测质量代码。 |
| 12 | `source` | 数据来源 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 13 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_hardware_measurement_batch` | `upload_batch_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_measurement_device_time` | `device_id, measured_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_measurement_metric_time` | `metric_type, measured_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_measurement_user_time` | `user_id, measured_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `hardware_measurement.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE NO ACTION。
- `hardware_measurement.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。

注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。

## 27. 表：`hardware_signal_chunk_metadata` 硬件信号元数据表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_signal_chunk_metadata` |
| 中文名称 | 硬件信号元数据表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 旧 MySQL 硬件兼容 |
| 业务作用 | 只保存信号时间窗、采样率和质量元数据，不保存原始波形。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移兼容） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | fk_hardware_signal_batch | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 5 | `signal_type` | 信号类型 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识信号/ECG 分块或元数据的信号类别。 |
| 6 | `started_at` | 开始时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 7 | `sample_rate_hz` | 采样率 | `decimal(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 信号采样频率，单位 Hz。 |
| 8 | `sample_count` | 采样点数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前信号块包含的样本数量。 |
| 9 | `payload_ref` | 待确认 | `varchar(512)` | 512 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `retention_expires_at` | 时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_signal_retention | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 11 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_hardware_signal_batch` | `upload_batch_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_signal_retention` | `retention_expires_at` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `hardware_signal_chunk_metadata.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE NO ACTION。
- `hardware_signal_chunk_metadata.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

只保存信号时间窗、采样率和质量元数据，不保存原始波形。

注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。

## 28. 表：`hardware_sleep_session` 硬件睡眠会话表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_sleep_session` |
| 中文名称 | 硬件睡眠会话表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 旧 MySQL 硬件兼容 |
| 业务作用 | 保存规范化睡眠会话和阶段分钟数。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移兼容） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `upload_batch_id` | 上传批次 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | fk_hardware_sleep_batch | 否 | 物理→hardware_upload_batch.id | — | 关联产生或上传当前记录的批次；具体目标表取决于存储域。 |
| 3 | `client_record_id` | 待确认 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_sleep_user_time | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_sleep_device_time | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `started_at` | 开始时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_hardware_sleep_device_time、idx_hardware_sleep_user_time | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 7 | `ended_at` | 结束时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗结束时间。 |
| 8 | `deep_minutes` | 深睡时长 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 深睡阶段分钟数。 |
| 9 | `light_minutes` | 浅睡时长 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 浅睡阶段分钟数。 |
| 10 | `awake_minutes` | 清醒时长 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 睡眠会话内清醒分钟数。 |
| 11 | `rem_minutes` | REM 时长 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 快速眼动睡眠阶段分钟数。 |
| 12 | `interruption_minutes` | 中断时长 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 睡眠中断分钟数。 |
| 13 | `source` | 数据来源 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 14 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_hardware_sleep_batch` | `upload_batch_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_sleep_device_time` | `device_id, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_hardware_sleep_user_time` | `user_id, started_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `hardware_sleep_session.(upload_batch_id)` → `hardware_upload_batch.(id)`：物理外键；ON DELETE NO ACTION。
- `hardware_sleep_session.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `source`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存规范化睡眠会话和阶段分钟数。

注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。

## 29. 表：`hardware_upload_batch` 硬件上传批次表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `hardware_upload_batch` |
| 中文名称 | 硬件上传批次表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 旧 MySQL 硬件兼容 |
| 业务作用 | 保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 8 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移兼容） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `receipt_id` | 持久化收据 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | uk_hardware_batch_receipt | uk_hardware_batch_receipt | 否 | 否/待确认 | — | 服务端为已接收批次生成的唯一收据标识。 |
| 3 | `batch_id` | 客户端批次 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_hardware_batch_owner_device | uk_hardware_batch_owner_device | 否 | 否/待确认 | — | 客户端生成的稳定遥测批次业务键，重试时保持不变。 |
| 4 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_hardware_batch_owner_device | idx_hardware_batch_user_time、uk_hardware_batch_owner_device | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_hardware_batch_owner_device | idx_hardware_batch_device_time、uk_hardware_batch_owner_device | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 6 | `source` | 数据来源 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。 |
| 7 | `collected_from` | 采集窗口起点 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_hardware_batch_device_time、idx_hardware_batch_user_time | 否 | 否 | — | 上传批次覆盖的最早采集时间。 |
| 8 | `collected_to` | 采集窗口终点 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 上传批次覆盖的最晚采集时间。 |
| 9 | `received_at` | 接收时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 服务端收到上传批次的时间。 |
| 10 | `committed_at` | 持久化完成时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次完成约定 durable write 的时间。 |
| 11 | `status` | 状态 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 12 | `record_count` | 记录总数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次中全部规范化记录数量。 |
| 13 | `measurement_count` | 测量记录数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次中的标量测量条数。 |
| 14 | `sleep_session_count` | 睡眠会话数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次中的睡眠会话条数。 |
| 15 | `activity_count` | 活动记录数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次中的活动记录条数。 |
| 16 | `signal_chunk_count` | 信号分块数 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 旧 MySQL 批次记录的信号分块计数。 |
| 17 | `quality_json` | 特征质量 JSON | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存特征缺失、质量和来源等版本化元数据。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_hardware_batch_device_time` | `device_id, collected_from` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_hardware_batch_user_time` | `user_id, collected_from` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_hardware_batch_owner_device` | `user_id, device_id, batch_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_hardware_batch_receipt` | `receipt_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `hardware_upload_batch.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `source, status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。

注意：本表位于旧 MySQL 硬件兼容结构；当前权威硬件遥测写入属于 Device Service/TimescaleDB，不得视为并行权威写入。

## 30. 表：`jeecg_order_customer` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jeecg_order_customer` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 上游订单示例 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 62 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `name` | 客户名 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 客户名 |
| 3 | `sex` | 性别 | `varchar(4)` | 4 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 性别 |
| 4 | `idcard` | 身份证号码 | `varchar(18)` | 18 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 身份证号码 |
| 5 | `idcard_pic` | 身份证扫描件 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 身份证扫描件 |
| 6 | `telphone` | 电话1 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 电话1 | 电话1 |
| 7 | `order_id` | 外键 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 外键 |
| 8 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 10 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 11 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `telphone`：电话1。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 31. 表：`jeecg_order_main` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jeecg_order_main` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 上游订单示例 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 14 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `order_code` | 订单号 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单号 |
| 3 | `ctype` | 订单类型 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单类型 |
| 4 | `order_date` | 订单日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单日期 |
| 5 | `order_money` | 订单金额 | `double(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单金额 |
| 6 | `content` | 订单备注 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单备注 |
| 7 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 8 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 9 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 10 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 11 | `bpm_status` | 流程状态 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 流程状态 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 32. 表：`jeecg_order_ticket` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jeecg_order_ticket` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 上游订单示例 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 46 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `ticket_code` | 航班号 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 航班号 |
| 3 | `tickect_date` | 航班时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 航班时间 |
| 4 | `order_id` | 外键 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 外键 |
| 5 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 8 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 33. 表：`jimu_dict` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_dict` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 45 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `dict_name` | 字典名称 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典名称 |
| 3 | `dict_code` | 字典编码 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | uk_sd_dict_code | uk_sd_dict_code | 否 | 否 | — | 字典编码 |
| 4 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 5 | `del_flag` | 删除状态 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 删除状态 |
| 6 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 7 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 8 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 9 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 10 | `type` | 字典类型0为string,1为number | `int(1) unsigned zerofill` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 字典类型0为string,1为number | 字典类型0为string,1为number |
| 11 | `tenant_id` | 多租户标识 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 多租户标识 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_sd_dict_code` | `dict_code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `jimu_dict.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `type`：字典类型0为string,1为number。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 34. 表：`jimu_dict_item` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_dict_item` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 144 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `dict_id` | 字典id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdi_dict_val、idx_sdi_role_dict_id | 否 | 否/待确认 | — | 字典id |
| 3 | `item_text` | 字典项文本 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典项文本 |
| 4 | `item_value` | 字典项值 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | idx_sdi_dict_val | 否 | 否 | — | 字典项值 |
| 5 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 6 | `sort_order` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdi_role_sort_order | 否 | 否 | — | 排序 |
| 7 | `status` | 状态（1启用 0不启用） | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdi_status | 是 | 否 | 状态（1启用 0不启用） | 状态（1启用 0不启用） |
| 8 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共创建时间字段。 |
| 10 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录最后更新用户。 |
| 11 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共更新时间字段。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sdi_dict_val` | `dict_id, item_value` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sdi_role_dict_id` | `dict_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sdi_role_sort_order` | `sort_order` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sdi_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态（1启用 0不启用）。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 35. 表：`jimu_report` 在线excel设计器

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report` |
| 中文名称 | 在线excel设计器 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 在线excel设计器 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 30 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `code` | 编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | uniq_jmreport_code | uniq_jmreport_code | 否 | 否 | — | 编码 |
| 3 | `name` | 名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称 |
| 4 | `note` | 说明 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明 |
| 5 | `status` | 状态 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态 |
| 6 | `type` | 类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 类型 |
| 7 | `json_str` | json字符串 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | json字符串 |
| 8 | `api_url` | 请求地址 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求地址 |
| 9 | `thumb` | 缩略图 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 缩略图 |
| 10 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | uniq_jmreport_createby | 是 | 否 | — | 创建人 |
| 11 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 12 | `update_by` | 修改人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 13 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 14 | `del_flag` | 删除标识0-正常,1-已删除 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | uniq_jmreport_delflag | 是 | 否 | 删除标识0-正常,1-已删除 | 删除标识0-正常,1-已删除 |
| 15 | `api_method` | 请求方法0-get,1-post | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 请求方法0-get,1-post | 请求方法0-get,1-post |
| 16 | `api_code` | 请求编码 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求编码 |
| 17 | `template` | 是否是模板 0不是,1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否是模板 0不是,1是 | 是否是模板 0不是,1是 |
| 18 | `view_count` | 浏览次数 | `bigint` | 19,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 浏览次数 |
| 19 | `css_str` | css增强 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | css增强 |
| 20 | `js_str` | js增强 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | js增强 |
| 21 | `py_str` | py增强 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | py增强 |
| 22 | `tenant_id` | 多租户标识 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 多租户标识 |
| 23 | `update_count` | 乐观锁版本 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 乐观锁版本 |
| 24 | `submit_form` | 是否填报报表 0不是,1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否填报报表 0不是,1是 | 是否填报报表 0不是,1是 |
| 25 | `is_multi_sheet` | 是否多sheet报表 1是 0否 | `tinyint` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否多sheet报表 1是 0否 | 是否多sheet报表 1是 0否 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_jmreport_code` | `code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_jmreport_createby` | `create_by` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `uniq_jmreport_delflag` | `del_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `jimu_report.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `del_flag`：删除标识0-正常,1-已删除。
- `api_method`：请求方法0-get,1-post。
- `template`：是否是模板 0不是,1是。
- `submit_form`：是否填报报表 0不是,1是。
- `is_multi_sheet`：是否多sheet报表 1是 0否。
- `status, type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

在线excel设计器

## 36. 表：`jimu_report_category` 分类

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_category` |
| 中文名称 | 分类 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 分类 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 5 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `name` | 分类名称 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 分类名称 |
| 3 | `parent_id` | 父级id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 父级id |
| 4 | `iz_leaf` | 是否为叶子节点(0 否 1是) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否为叶子节点(0 否 1是) | 是否为叶子节点(0 否 1是) |
| 5 | `source_type` | 来源类型( report 积木报表 screen 大屏  drag 仪表盘) | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 来源类型( report 积木报表 screen 大屏  drag 仪表盘) |
| 6 | `del_flag` | 删除标识(0 正常 1 已删除) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 删除标识(0 正常 1 已删除) | 删除标识(0 正常 1 已删除) |
| 7 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 8 | `create_time` | 创建时间 | `timestamp` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 9 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 10 | `update_time` | 更新时间 | `timestamp` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 11 | `tenant_id` | 租户id | `varchar(11)` | 11 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 12 | `sort_no` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `jimu_report_category.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `iz_leaf`：是否为叶子节点(0 否 1是)。
- `del_flag`：删除标识(0 正常 1 已删除)。

### 业务说明

分类

## 37. 表：`jimu_report_data_source` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_data_source` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `name` | 数据源名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据源名称 |
| 3 | `report_id` | 报表_id | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | idx_jmdatasource_report_id | 否 | 否/待确认 | — | 报表_id |
| 4 | `code` | 编码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | idx_jmdatasource_code | 否 | 否 | — | 编码 |
| 5 | `remark` | 备注 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 6 | `db_type` | 数据库类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库类型 |
| 7 | `db_driver` | 驱动类 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 驱动类 |
| 8 | `db_url` | 数据源地址 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据源地址 |
| 9 | `db_username` | 用户名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户名 |
| 10 | `db_password` | 密码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 密码 |
| 11 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 12 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 13 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 14 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 15 | `connect_times` | 连接失败次数 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 连接失败次数 |
| 16 | `tenant_id` | 多租户标识 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 多租户标识 |
| 17 | `type` | 类型(report:报表;drag:仪表盘) | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 类型(report:报表;drag:仪表盘) | 类型(report:报表;drag:仪表盘) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_jmdatasource_code` | `code` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_jmdatasource_report_id` | `report_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `jimu_report_data_source.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `type`：类型(report:报表;drag:仪表盘)。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 38. 表：`jimu_report_db` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_db` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 58 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `jimu_report_id` | 主键字段 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_jimu_report_id | 否 | 否/待确认 | — | 主键字段 |
| 3 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 4 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 5 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 6 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 7 | `db_code` | 数据集编码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据集编码 |
| 8 | `db_ch_name` | 数据集名字 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据集名字 |
| 9 | `db_type` | 数据源类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据源类型 |
| 10 | `db_table_name` | 数据库表名 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库表名 |
| 11 | `db_dyn_sql` | 动态查询SQL | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 动态查询SQL |
| 12 | `db_key` | 数据源KEY | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_jmreportdb_db_key | 否 | 否 | — | 数据源KEY |
| 13 | `tb_db_key` | 填报数据源 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 填报数据源 |
| 14 | `tb_db_table_name` | 填报数据表 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 填报数据表 |
| 15 | `java_type` | java类数据集  类型（spring:springkey,class:java类名） | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | java类数据集  类型（spring:springkey,class:java类名） | java类数据集  类型（spring:springkey,class:java类名） |
| 16 | `java_value` | java类数据源  数值（bean key/java类名） | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | java类数据源  数值（bean key/java类名） |
| 17 | `api_url` | 请求地址 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求地址 |
| 18 | `api_method` | 请求方法0-get,1-post | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 请求方法0-get,1-post | 请求方法0-get,1-post |
| 19 | `is_list` | 是否是列表0否1是 默认0 | `varchar(10)` | 10 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否是列表0否1是 默认0 | 是否是列表0否1是 默认0 |
| 20 | `is_page` | 是否作为分页,0:不分页，1:分页 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否作为分页,0:不分页，1:分页 | 是否作为分页,0:不分页，1:分页 |
| 21 | `db_source` | 数据源 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | idx_db_source_id | 否 | 否 | — | 数据源 |
| 22 | `db_source_type` | 数据库类型 MYSQL ORACLE SQLSERVER | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库类型 MYSQL ORACLE SQLSERVER |
| 23 | `json_data` | json数据，直接解析json内容 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | json数据，直接解析json内容 |
| 24 | `api_convert` | api转换器 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | api转换器 |
| 25 | `iz_shared_source` | 是否为共享数据源(0 否 1 是) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否为共享数据源(0 否 1 是) | 是否为共享数据源(0 否 1 是) |
| 26 | `jimu_shared_source_id` | 指向共享数据集的id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 指向共享数据集的id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_db_source_id` | `db_source` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_jimu_report_id` | `jimu_report_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_jmreportdb_db_key` | `db_key` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `java_type`：java类数据集  类型（spring:springkey,class:java类名）。
- `api_method`：请求方法0-get,1-post。
- `is_list`：是否是列表0否1是 默认0。
- `is_page`：是否作为分页,0:不分页，1:分页。
- `iz_shared_source`：是否为共享数据源(0 否 1 是)。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 39. 表：`jimu_report_db_field` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_db_field` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 526 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `jimu_report_db_id` | 数据源ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_jrdf_jimu_report_db_id | 否 | 否/待确认 | — | 数据源ID |
| 7 | `field_name` | 字段名 | `varchar(80)` | 80 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段名 |
| 8 | `field_name_physics` | 物理字段名（文件数据集使用，存的是excel的字段标题） | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 物理字段名（文件数据集使用，存的是excel的字段标题） |
| 9 | `field_text` | 字段文本 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段文本 |
| 10 | `widget_type` | 控件类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 控件类型 |
| 11 | `widget_width` | 控件宽度 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 控件宽度 |
| 12 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_dbfield_order_num | 否 | 否 | — | 排序 |
| 13 | `search_flag` | 查询标识0否1是 默认0 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 查询标识0否1是 默认0 | 查询标识0否1是 默认0 |
| 14 | `search_mode` | 查询模式1简单2范围 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 查询模式1简单2范围 | 查询模式1简单2范围 |
| 15 | `dict_code` | 字典编码支持从表中取数据 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典编码支持从表中取数据 |
| 16 | `search_value` | 查询默认值 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询默认值 |
| 17 | `search_format` | 查询时间格式化表达式 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询时间格式化表达式 |
| 18 | `ext_json` | 参数配置 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数配置 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_dbfield_order_num` | `order_num` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_jrdf_jimu_report_db_id` | `jimu_report_db_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `search_flag`：查询标识0否1是 默认0。
- `search_mode`：查询模式1简单2范围。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 40. 表：`jimu_report_db_param` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_db_param` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 21 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `jimu_report_head_id` | 动态报表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | idx_jrdp_jimu_report_head_id | 否 | 否/待确认 | — | 动态报表ID |
| 3 | `param_name` | 参数字段 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数字段 |
| 4 | `param_txt` | 参数文本 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数文本 |
| 5 | `param_value` | 参数默认值 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数默认值 |
| 6 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 7 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 8 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 9 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 10 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 11 | `search_flag` | 查询标识0否1是 默认0 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 查询标识0否1是 默认0 | 查询标识0否1是 默认0 |
| 12 | `widget_type` | 查询控件类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询控件类型 |
| 13 | `search_mode` | 查询模式1简单2范围 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 查询模式1简单2范围 | 查询模式1简单2范围 |
| 14 | `dict_code` | 字典 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典 |
| 15 | `search_format` | 查询时间格式化表达式 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询时间格式化表达式 |
| 16 | `ext_json` | 参数配置 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数配置 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_jrdp_jimu_report_head_id` | `jimu_report_head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `search_flag`：查询标识0否1是 默认0。
- `search_mode`：查询模式1简单2范围。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 41. 表：`jimu_report_export_job` 积木报表导出计划表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_export_job` |
| 中文名称 | 积木报表导出计划表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 积木报表导出计划表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `name` | 任务名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 任务名称 |
| 3 | `begin_time` | 开始时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 开始时间 |
| 4 | `end_time` | 结束时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 结束时间 |
| 5 | `exec_interval` | 执行频率 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 执行频率 |
| 6 | `report_conf` | 导出报表配置 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 导出报表配置 |
| 7 | `last_run_time` | 最后执行时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最后执行时间 |
| 8 | `receiver_email` | 接收通知的邮件 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 接收通知的邮件 |
| 9 | `file_sync_path` | 文件同步路径 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件同步路径 |
| 10 | `status` | 状态(0:停止;1:启动) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态(0:停止;1:启动) | 状态(0:停止;1:启动) |
| 11 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 12 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 13 | `update_by` | 修改人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 14 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 15 | `tenant_id` | 多租户标识 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 多租户标识 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `jimu_report_export_job.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态(0:停止;1:启动)。

### 业务说明

积木报表导出计划表

## 42. 表：`jimu_report_export_log` 积木报表自动导出记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_export_log` |
| 中文名称 | 积木报表自动导出记录表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 积木报表自动导出记录表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `batch_no` | 批次编号 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次编号 |
| 3 | `export_channel` | 导出渠道 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 导出渠道 |
| 4 | `export_type` | 导出类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 导出类型 |
| 5 | `report_id` | 报表id | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 报表id |
| 6 | `download_path` | 下载路径 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下载路径 |
| 7 | `status` | 状态 | `varchar(15)` | 15 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态 |
| 8 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 10 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 11 | `tenant_id` | 多租户标识 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 多租户标识 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `jimu_report_export_log.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

积木报表自动导出记录表

## 43. 表：`jimu_report_ext_data` 通用扩展数据表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_ext_data` |
| 中文名称 | 通用扩展数据表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 通用扩展数据表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键ID |
| 2 | `biz_type` | 业务类型标识，如 report_share、temp_config 等 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | idx_biz | 否 | 否 | — | 业务类型标识，如 report_share、temp_config 等 |
| 3 | `name` | 名称，展示用 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称，展示用 |
| 4 | `descr` | 描述信息 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述信息 |
| 5 | `tags` | 标签，多个用逗号分隔 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标签，多个用逗号分隔 |
| 6 | `data_value` | 实际存储内容 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 实际存储内容 |
| 7 | `metadata` | 元数据，用于存储补充信息 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 元数据，用于存储补充信息 |
| 8 | `status` | 状态标识：1正常 0禁用 | `tinyint` | 3,0 | 是 | `1` | 否 | 否 | 否 | 否 | 是 | 否 | 状态标识：1正常 0禁用 | 状态标识：1正常 0禁用 |
| 9 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 10 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `CURRENT_TIMESTAMP` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 11 | `update_by` | 修改人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 12 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `CURRENT_TIMESTAMP` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_biz` | `biz_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态标识：1正常 0禁用。

### 业务说明

通用扩展数据表

## 44. 表：`jimu_report_icon_lib` 积木图库表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_icon_lib` |
| 中文名称 | 积木图库表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 积木图库表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 211 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `name` | 图片名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图片名称 |
| 3 | `type` | 图片类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 图片类型 |
| 4 | `image_url` | 图片地址 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图片地址 |
| 5 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 8 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 9 | `tenant_id` | 租户id | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `jimu_report_icon_lib.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

积木图库表

## 45. 表：`jimu_report_link` 超链接配置表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_link` |
| 中文名称 | 超链接配置表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 超链接配置表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `report_id` | 积木设计器id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | uniq_link_reportid | 否 | 否/待确认 | — | 积木设计器id |
| 3 | `parameter` | 参数 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数 |
| 4 | `eject_type` | 弹出方式（0 当前页面 1 新窗口） | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 弹出方式（0 当前页面 1 新窗口） | 弹出方式（0 当前页面 1 新窗口） |
| 5 | `link_name` | 链接名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 链接名称 |
| 6 | `api_method` | 请求方法0-get,1-post | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 请求方法0-get,1-post | 请求方法0-get,1-post |
| 7 | `link_type` | 链接方式(0 网络报表 1 网络连接 2 图表联动) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 链接方式(0 网络报表 1 网络连接 2 图表联动) | 链接方式(0 网络报表 1 网络连接 2 图表联动) |
| 8 | `api_url` | 外网api | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 外网api |
| 9 | `link_chart_id` | 联动图表的ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 联动图表的ID |
| 10 | `expression` | 表达式 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表达式 |
| 11 | `requirement` | 条件 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 条件 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_link_reportid` | `report_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `eject_type`：弹出方式（0 当前页面 1 新窗口）。
- `api_method`：请求方法0-get,1-post。
- `link_type`：链接方式(0 网络报表 1 网络连接 2 图表联动)。

### 业务说明

超链接配置表

## 46. 表：`jimu_report_map` 地图配置表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_map` |
| 中文名称 | 地图配置表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 地图配置表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `label` | 地图名称 | `varchar(125)` | 125 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 地图名称 |
| 3 | `name` | 地图编码 | `varchar(125)` | 125 | 是 | `无/NULL` | 否 | 否 | uniq_jmreport_map_name | uniq_jmreport_map_name | 否 | 否 | — | 地图编码 |
| 4 | `data` | 地图数据 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 地图数据 |
| 5 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 8 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 9 | `del_flag` | 0表示未删除,1表示删除 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 0表示未删除,1表示删除 | 0表示未删除,1表示删除 |
| 10 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_jmreport_map_name` | `name` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `del_flag`：0表示未删除,1表示删除。

### 业务说明

地图配置表

## 47. 表：`jimu_report_share` 积木报表预览权限表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_share` |
| 中文名称 | 积木报表预览权限表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 积木报表预览权限表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `report_id` | 在线excel设计器id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | uniq_jrs_report_id、uniq_report_id | uniq_jrs_report_id、uniq_report_id | 否 | 否/待确认 | — | 在线excel设计器id |
| 3 | `preview_url` | 预览地址 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 预览地址 |
| 4 | `preview_lock` | 密码锁 | `varchar(4)` | 4 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 密码锁 |
| 5 | `last_update_time` | 最后更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最后更新时间 |
| 6 | `term_of_validity` | 有效期(0:永久有效，1:1天，2:7天) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 有效期(0:永久有效，1:1天，2:7天) | 有效期(0:永久有效，1:1天，2:7天) |
| 7 | `status` | 是否过期(0未过期，1已过期) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 是否过期(0未过期，1已过期) | 是否过期(0未过期，1已过期) |
| 8 | `preview_lock_status` | 是否为密码锁(0 否,1是) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否为密码锁(0 否,1是) | 是否为密码锁(0 否,1是) |
| 9 | `share_token` | 分享token | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_jrs_share_token | 否 | 否 | — | 分享token |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_jrs_share_token` | `share_token` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_jrs_report_id` | `report_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_report_id` | `report_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `term_of_validity`：有效期(0:永久有效，1:1天，2:7天)。
- `status`：是否过期(0未过期，1已过期)。
- `preview_lock_status`：是否为密码锁(0 否,1是)。

### 业务说明

积木报表预览权限表

## 48. 表：`jimu_report_sheet` 报表Sheet表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `jimu_report_sheet` |
| 中文名称 | 报表Sheet表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jimu 报表 |
| 业务作用 | 报表Sheet表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键（Sheet ID） | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键（Sheet ID） |
| 2 | `report_id` | 报表ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_report_id、idx_sheet_order | 否 | 否/待确认 | — | 报表ID |
| 3 | `sheet_name` | Sheet名称 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | Sheet名称 |
| 4 | `sheet_order` | 排序（可以为负数，负数表示在默认sheet前面） | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | idx_sheet_order | 否 | 否 | — | 排序（可以为负数，负数表示在默认sheet前面） |
| 5 | `json_str` | 该sheet的完整jsonStr | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该sheet的完整jsonStr |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 8 | `create_by` | 创建人 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 9 | `update_by` | 更新人 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_report_id` | `report_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sheet_order` | `report_id, sheet_order` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

报表Sheet表

## 49. 表：`joa_demo` 流程测试

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `joa_demo` |
| 中文名称 | 流程测试 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 流程测试 |
| 主键 | `无/待确认` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | ID |
| 2 | `name` | 请假人 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假人 |
| 3 | `days` | 请假天数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假天数 |
| 4 | `begin_date` | 开始时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 开始时间 |
| 5 | `end_date` | 请假结束时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假结束时间 |
| 6 | `reason` | 请假原因 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假原因 |
| 7 | `bpm_status` | 流程状态 | `varchar(50)` | 50 | 是 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 流程状态 |
| 8 | `create_by` | 创建人id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人id |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 10 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 11 | `update_by` | 修改人id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| — | — | 无独立索引 | 未发现索引定义。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

流程测试

## 50. 表：`oauth2_registered_client` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `oauth2_registered_client` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(100)` | 100 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `client_id` | 待确认 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `client_id_issued_at` | 时间 | `timestamp` | 不适用 | 否 | `CURRENT_TIMESTAMP` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 4 | `client_secret` | 待确认 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `client_secret_expires_at` | 时间 | `timestamp` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 6 | `client_name` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `client_authentication_methods` | 待确认 | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `authorization_grant_types` | 待确认 | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `redirect_uris` | 待确认 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `post_logout_redirect_uris` | 待确认 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `scopes` | 待确认 | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `client_settings` | 待确认 | `varchar(2000)` | 2000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 13 | `token_settings` | 待确认 | `varchar(2000)` | 2000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 51. 表：`onl_auth_data` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_auth_data` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `cgform_id` | online表ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | online表ID |
| 3 | `rule_name` | 规则名 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则名 |
| 4 | `rule_column` | 规则列 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则列 |
| 5 | `rule_operator` | 规则条件 大于小于like | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则条件 大于小于like |
| 6 | `rule_value` | 规则值 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则值 |
| 7 | `status` | 1有效 0无效 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 1有效 0无效 | 1有效 0无效 |
| 8 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 9 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 10 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 11 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：1有效 0无效。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 52. 表：`onl_auth_page` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_auth_page` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` |  主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — |  主键 |
| 2 | `cgform_id` | online表id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_onl_auth_page_cgform_id | 否 | 否/待确认 | — | online表id |
| 3 | `code` | 字段名/按钮编码 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | idx_onl_auth_page_code | 否 | 否 | — | 字段名/按钮编码 |
| 4 | `type` | 1字段 2按钮 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 1字段 2按钮 | 1字段 2按钮 |
| 5 | `control` | 3可编辑 5可见(仅支持两种状态值3,5) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 3可编辑 5可见(仅支持两种状态值3,5) |
| 6 | `page` | 3列表 5表单(仅支持两种状态值3,5) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 3列表 5表单(仅支持两种状态值3,5) |
| 7 | `status` | 1有效 0无效 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 1有效 0无效 | 1有效 0无效 |
| 8 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 9 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 10 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 11 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_onl_auth_page_cgform_id` | `cgform_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_onl_auth_page_code` | `code` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `type`：1字段 2按钮。
- `status`：1有效 0无效。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 53. 表：`onl_auth_relation` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_auth_relation` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `role_id` | 角色id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 角色id |
| 3 | `auth_id` | 权限id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 权限id |
| 4 | `type` | 1字段 2按钮 3数据权限 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 1字段 2按钮 3数据权限 | 1字段 2按钮 3数据权限 |
| 5 | `cgform_id` | online表单ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | online表单ID |
| 6 | `auth_mode` | 授权方式role角色，depart部门，user人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 授权方式role角色，depart部门，user人 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `type`：1字段 2按钮 3数据权限。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 54. 表：`onl_cgform_button` Online表单自定义按钮

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_button` |
| 中文名称 | Online表单自定义按钮 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | Online表单自定义按钮 |
| 主键 | `ID` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 10 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `ID` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否 | — | 主键ID |
| 2 | `BUTTON_CODE` | 按钮编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ocb_BUTTON_CODE | 否 | 否 | — | 按钮编码 |
| 3 | `BUTTON_ICON` | 按钮图标 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 按钮图标 |
| 4 | `BUTTON_NAME` | 按钮名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 按钮名称 |
| 5 | `BUTTON_STATUS` | 按钮状态 | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ocb_BUTTON_STATUS | 否 | 否 | — | 按钮状态 |
| 6 | `BUTTON_STYLE` | 按钮样式 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 按钮样式 |
| 7 | `EXP` | 表达式 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表达式 |
| 8 | `CGFORM_HEAD_ID` | 表单ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ocb_CGFORM_HEAD_ID | 否 | 否 | — | 表单ID |
| 9 | `OPT_TYPE` | 按钮类型 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 按钮类型 |
| 10 | `ORDER_NUM` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ocb_ORDER_NUM | 否 | 否 | — | 排序 |
| 11 | `OPT_POSITION` | 按钮位置1侧面 2底部 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 按钮位置1侧面 2底部 | 按钮位置1侧面 2底部 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ocb_BUTTON_CODE` | `BUTTON_CODE` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ocb_BUTTON_STATUS` | `BUTTON_STATUS` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_ocb_CGFORM_HEAD_ID` | `CGFORM_HEAD_ID` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ocb_ORDER_NUM` | `ORDER_NUM` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `ID` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `OPT_POSITION`：按钮位置1侧面 2底部。

### 业务说明

Online表单自定义按钮

## 55. 表：`onl_cgform_enhance_java` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_enhance_java` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `ID` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `ID` | 待确认 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 2 | `BUTTON_CODE` | 按钮编码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ocej_BUTTON_CODE | 否 | 否 | — | 按钮编码 |
| 3 | `CG_JAVA_TYPE` | 类型 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 类型 |
| 4 | `CG_JAVA_VALUE` | 数值 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数值 |
| 5 | `CGFORM_HEAD_ID` | 表单ID | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_ejava_cgform_head_id | 否 | 否 | — | 表单ID |
| 6 | `ACTIVE_STATUS` | 生效状态 | `varchar(2)` | 2 | 是 | `1` | 否 | 否 | 否 | idx_ocej_ACTIVE_STATUS | 否 | 否 | — | 生效状态 |
| 7 | `EVENT` | 事件状态(end:结束，start:开始) | `varchar(10)` | 10 | 否 | `end` | 否 | 否 | 否 | 否 | 否 | 否 | 事件状态(end:结束，start:开始) | 事件状态(end:结束，start:开始) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ejava_cgform_head_id` | `CGFORM_HEAD_ID` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ocej_ACTIVE_STATUS` | `ACTIVE_STATUS` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_ocej_BUTTON_CODE` | `BUTTON_CODE` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `ID` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `EVENT`：事件状态(end:结束，start:开始)。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 56. 表：`onl_cgform_enhance_js` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_enhance_js` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `ID` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 21 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `ID` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否 | — | 主键ID |
| 2 | `CG_JS` | JS增强内容 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | JS增强内容 |
| 3 | `CG_JS_TYPE` | 类型 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ejs_cg_js_type | 否 | 否 | — | 类型 |
| 4 | `CONTENT` | 备注 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 5 | `CGFORM_HEAD_ID` | 表单ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ejs_cgform_head_id | 否 | 否 | — | 表单ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ejs_cg_js_type` | `CG_JS_TYPE` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ejs_cgform_head_id` | `CGFORM_HEAD_ID` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `ID` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 57. 表：`onl_cgform_enhance_sql` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_enhance_sql` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `ID` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `ID` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否 | — | 主键ID |
| 2 | `BUTTON_CODE` | 按钮编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 按钮编码 |
| 3 | `CGB_SQL` | SQL内容 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | SQL内容 |
| 4 | `CGB_SQL_NAME` | Sql名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | Sql名称 |
| 5 | `CONTENT` | 备注 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 6 | `CGFORM_HEAD_ID` | 表单ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_oces_CGFORM_HEAD_ID | 否 | 否 | — | 表单ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_oces_CGFORM_HEAD_ID` | `CGFORM_HEAD_ID` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `ID` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 58. 表：`onl_cgform_field` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_field` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 719 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键ID |
| 2 | `cgform_head_id` | 表ID | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_ocf_cgform_head_id | 否 | 否/待确认 | — | 表ID |
| 3 | `db_field_name` | 字段名字 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段名字 |
| 4 | `db_field_txt` | 字段备注 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段备注 |
| 5 | `db_field_name_old` | 原字段名 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 原字段名 |
| 6 | `db_is_key` | 是否主键 0否 1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否主键 0否 1是 | 是否主键 0否 1是 |
| 7 | `db_is_null` | 是否允许为空0否 1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否允许为空0否 1是 | 是否允许为空0否 1是 |
| 8 | `db_is_persist` | 是否需要同步数据库字段， 1是0否 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否需要同步数据库字段， 1是0否 | 是否需要同步数据库字段， 1是0否 |
| 9 | `db_type` | 数据库字段类型 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库字段类型 |
| 10 | `db_length` | 数据库字段长度 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库字段长度 |
| 11 | `db_point_length` | 小数点 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 小数点 |
| 12 | `db_default_val` | 表字段默认值 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表字段默认值 |
| 13 | `dict_field` | 字典code | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典code |
| 14 | `dict_table` | 字典表 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典表 |
| 15 | `dict_text` | 字典Text | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典Text |
| 16 | `field_show_type` | 表单控件类型 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表单控件类型 |
| 17 | `field_href` | 跳转URL | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 跳转URL |
| 18 | `field_length` | 表单控件长度 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表单控件长度 |
| 19 | `field_valid_type` | 表单字段校验规则 | `varchar(300)` | 300 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表单字段校验规则 |
| 20 | `field_must_input` | 字段是否必填 | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段是否必填 |
| 21 | `field_extend_json` | 扩展参数JSON | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 扩展参数JSON |
| 22 | `field_default_value` | 控件默认值，不同的表达式展示不同的结果。 <br>1. 纯字符串直接赋给默认值； <br>2. #{普通变量}； <br>3. {{ 动态JS表达式 }}； <br>4. ${填值规则编码}； <br>填值规则表达式只允许存在一个，且不能和其他规则混用。 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 控件默认值，不同的表达式展示不同的结果。 <br>1. 纯字符串直接赋给默认值； <br>2. #{普通变量}； <br>3. {{ 动态JS表达式 }}； <br>4. ${填值规则编码}； <br>填值规则表达式只允许存在一个，且不能和其他规则混用。 | 控件默认值，不同的表达式展示不同的结果。 <br>1. 纯字符串直接赋给默认值； <br>2. #{普通变量}； <br>3. {{ 动态JS表达式 }}； <br>4. ${填值规则编码}； <br>填值规则表达式只允许存在一个，且不能和其他规则混用。 |
| 23 | `is_query` | 是否查询条件0否 1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否查询条件0否 1是 | 是否查询条件0否 1是 |
| 24 | `is_show_form` | 表单是否显示0否 1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 表单是否显示0否 1是 | 表单是否显示0否 1是 |
| 25 | `is_show_list` | 列表是否显示0否 1是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 列表是否显示0否 1是 | 列表是否显示0否 1是 |
| 26 | `is_read_only` | 是否是只读（1是 0否） | `tinyint(1)` | 3,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否是只读（1是 0否） | 是否是只读（1是 0否） |
| 27 | `query_mode` | 查询模式 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询模式 |
| 28 | `main_table` | 外键主表名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 外键主表名 |
| 29 | `main_field` | 外键主键字段 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 外键主键字段 |
| 30 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 31 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 32 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 33 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 34 | `create_by` | 创建人 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 35 | `converter` | 自定义值转换器 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 自定义值转换器 |
| 36 | `query_def_val` | 查询默认值 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询默认值 |
| 37 | `query_dict_text` | 查询配置字典text | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询配置字典text |
| 38 | `query_dict_field` | 查询配置字典code | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询配置字典code |
| 39 | `query_dict_table` | 查询配置字典table | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询配置字典table |
| 40 | `query_show_type` | 查询显示控件 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询显示控件 |
| 41 | `query_config_flag` | 是否启用查询配置1是0否 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否启用查询配置1是0否 | 是否启用查询配置1是0否 |
| 42 | `query_valid_type` | 查询字段校验类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询字段校验类型 |
| 43 | `query_must_input` | 查询字段是否必填1是0否 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 查询字段是否必填1是0否 | 查询字段是否必填1是0否 |
| 44 | `sort_flag` | 是否支持排序1是0否 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否支持排序1是0否 | 是否支持排序1是0否 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ocf_cgform_head_id` | `cgform_head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `db_is_key`：是否主键 0否 1是。
- `db_is_null`：是否允许为空0否 1是。
- `db_is_persist`：是否需要同步数据库字段， 1是0否。
- `field_default_value`：控件默认值，不同的表达式展示不同的结果。 <br>1. 纯字符串直接赋给默认值； <br>2. #{普通变量}； <br>3. {{ 动态JS表达式 }}； <br>4. ${填值规则编码}； <br>填值规则表达式只允许存在一个，且不能和其他规则混用。。
- `is_query`：是否查询条件0否 1是。
- `is_show_form`：表单是否显示0否 1是。
- `is_show_list`：列表是否显示0否 1是。
- `is_read_only`：是否是只读（1是 0否）。
- `query_config_flag`：是否启用查询配置1是0否。
- `query_must_input`：查询字段是否必填1是0否。
- `sort_flag`：是否支持排序1是0否。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 59. 表：`onl_cgform_head` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_head` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 12 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键ID |
| 2 | `table_name` | 表名 | `varchar(50)` | 50 | 否 | `无/NULL` | 否 | 否 | 否 | idx_och_cgform_head_id | 否 | 否 | — | 表名 |
| 3 | `table_type` | 表类型: 0单表、1主表、2附表 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 表类型: 0单表、1主表、2附表 | 表类型: 0单表、1主表、2附表 |
| 4 | `table_version` | 表版本 | `int` | 10,0 | 是 | `1` | 否 | 否 | 否 | idx_och_table_version | 否 | 否 | — | 表版本 |
| 5 | `table_txt` | 表说明 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表说明 |
| 6 | `is_checkbox` | 是否带checkbox | `varchar(5)` | 5 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否带checkbox |
| 7 | `is_db_synch` | 同步数据库状态 | `varchar(20)` | 20 | 否 | `N` | 否 | 否 | 否 | 否 | 否 | 否 | — | 同步数据库状态 |
| 8 | `is_page` | 是否分页 | `varchar(5)` | 5 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否分页 |
| 9 | `is_tree` | 是否是树 | `varchar(5)` | 5 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否是树 |
| 10 | `id_sequence` | 主键生成序列 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主键生成序列 |
| 11 | `id_type` | 主键类型 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主键类型 |
| 12 | `query_mode` | 查询模式 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询模式 |
| 13 | `relation_type` | 映射关系 0一对多  1一对一 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 映射关系 0一对多  1一对一 | 映射关系 0一对多  1一对一 |
| 14 | `sub_table_str` | 子表 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 子表 |
| 15 | `tab_order_num` | 附表排序序号 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 附表排序序号 |
| 16 | `tree_parent_id_field` | 树形表单父id | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 树形表单父id |
| 17 | `tree_id_field` | 树表主键字段 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 树表主键字段 |
| 18 | `tree_fieldname` | 树开表单列字段 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 树开表单列字段 |
| 19 | `form_category` | 表单分类 | `varchar(50)` | 50 | 否 | `bdfl_ptbd` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表单分类 |
| 20 | `form_template` | PC表单模板 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_och_table_name | 否 | 否 | — | PC表单模板 |
| 21 | `form_template_mobile` | 表单模板样式(移动端) | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_och_form_template_mobile | 否 | 否 | — | 表单模板样式(移动端) |
| 22 | `scroll` | 是否有横向滚动条 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否有横向滚动条 |
| 23 | `copy_version` | 复制版本号 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 复制版本号 |
| 24 | `copy_type` | 复制表类型1为复制表 0为原始表 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 复制表类型1为复制表 0为原始表 | 复制表类型1为复制表 0为原始表 |
| 25 | `physic_id` | 原始表ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 原始表ID |
| 26 | `ext_config_json` | 扩展JSON | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 扩展JSON |
| 27 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 28 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 29 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 30 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 31 | `theme_template` | 主题模板 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主题模板 |
| 32 | `is_des_form` | 是否用设计器表单 | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否用设计器表单 |
| 33 | `des_form_code` | 设计器表单编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 设计器表单编码 |
| 34 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 35 | `low_app_id` | 关联的应用ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 关联的应用ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_och_cgform_head_id` | `table_name` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_och_form_template_mobile` | `form_template_mobile` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_och_table_name` | `form_template` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_och_table_version` | `table_version` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `onl_cgform_head.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `table_type`：表类型: 0单表、1主表、2附表。
- `relation_type`：映射关系 0一对多  1一对一。
- `copy_type`：复制表类型1为复制表 0为原始表。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 60. 表：`onl_cgform_index` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgform_index` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `cgform_head_id` | 主表id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_oci_cgform_head_id | 否 | 否/待确认 | — | 主表id |
| 3 | `index_name` | 索引名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 索引名称 |
| 4 | `index_name_old` | 原索引名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 原索引名称 |
| 5 | `index_field` | 索引栏位 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 索引栏位 |
| 6 | `index_type` | 索引类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 索引类型 |
| 7 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 8 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 9 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 10 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 11 | `is_db_synch` | 是否同步数据库 N未同步 Y已同步 | `varchar(2)` | 2 | 是 | `N` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否同步数据库 N未同步 Y已同步 |
| 12 | `del_flag` | 是否删除 0未删除 1删除 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 否 | 是否删除 0未删除 1删除 | 是否删除 0未删除 1删除 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_oci_cgform_head_id` | `cgform_head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `del_flag`：是否删除 0未删除 1删除。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 61. 表：`onl_cgreport_head` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgreport_head` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `code` | 报表编码 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | index_onlinereport_code | index_onlinereport_code | 否 | 否 | — | 报表编码 |
| 3 | `name` | 报表名字 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 报表名字 |
| 4 | `cgr_sql` | 报表SQL | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 报表SQL |
| 5 | `return_val_field` | 返回值字段 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 返回值字段 |
| 6 | `return_txt_field` | 返回文本字段 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 返回文本字段 |
| 7 | `return_type` | 返回类型，单选或多选 | `varchar(2)` | 2 | 是 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 返回类型，单选或多选 |
| 8 | `db_source` | 动态数据源 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 动态数据源 |
| 9 | `content` | 描述 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 10 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 11 | `low_app_id` | 关联的应用ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 关联的应用ID |
| 12 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 13 | `update_by` | 修改人id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人id |
| 14 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 15 | `create_by` | 创建人id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `index_onlinereport_code` | `code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `onl_cgreport_head.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 62. 表：`onl_cgreport_item` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgreport_item` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 56 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `cgrhead_id` | 报表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | idx_oci_cgrhead_id | 否 | 否/待确认 | — | 报表ID |
| 3 | `field_name` | 字段名字 | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段名字 |
| 4 | `field_txt` | 字段文本 | `varchar(300)` | 300 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段文本 |
| 5 | `field_width` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `field_type` | 字段类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段类型 |
| 7 | `search_mode` | 查询模式 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询模式 |
| 8 | `is_order` | 是否排序  0否,1是 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否排序  0否,1是 | 是否排序  0否,1是 |
| 9 | `is_search` | 是否查询  0否,1是 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否查询  0否,1是 | 是否查询  0否,1是 |
| 10 | `dict_code` | 字典CODE | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典CODE |
| 11 | `field_href` | 字段跳转URL | `varchar(120)` | 120 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段跳转URL |
| 12 | `is_show` | 是否显示  0否,1显示 | `int` | 10,0 | 是 | `1` | 否 | 否 | 否 | idx_oci_is_show | 否 | 否 | 是否显示  0否,1显示 | 是否显示  0否,1显示 |
| 13 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_oci_order_num | 否 | 否 | — | 排序 |
| 14 | `replace_val` | 取值表达式 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 取值表达式 |
| 15 | `is_total` | 是否合计 0否,1是（仅对数值有效） | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否合计 0否,1是（仅对数值有效） | 是否合计 0否,1是（仅对数值有效） |
| 16 | `group_title` | 分组标题 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 分组标题 |
| 17 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 18 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 19 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 20 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_oci_cgrhead_id` | `cgrhead_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_oci_is_show` | `is_show` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_oci_order_num` | `order_num` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `is_order`：是否排序  0否,1是。
- `is_search`：是否查询  0否,1是。
- `is_show`：是否显示  0否,1显示。
- `is_total`：是否合计 0否,1是（仅对数值有效）。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 63. 表：`onl_cgreport_param` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_cgreport_param` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `cgrhead_id` | 动态报表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | idx_ocp_cgrhead_id | 否 | 否/待确认 | — | 动态报表ID |
| 3 | `param_name` | 参数字段 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数字段 |
| 4 | `param_txt` | 参数文本 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数文本 |
| 5 | `param_value` | 参数默认值 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数默认值 |
| 6 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 7 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 8 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 9 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 10 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ocp_cgrhead_id` | `cgrhead_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 64. 表：`onl_drag_comp` 组件库

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_comp` |
| 中文名称 | 组件库 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 组件库 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 93 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `parent_id` | 待确认 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `comp_name` | 组件名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件名称 |
| 4 | `comp_type` | 待确认 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `icon` | 图标 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图标 |
| 6 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 7 | `type_id` | 组件类型 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 组件类型 |
| 8 | `comp_config` | 组件配置 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件配置 |
| 9 | `status` | 状态0:无效 1:有效 | `varchar(2)` | 2 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 否 | 状态0:无效 1:有效 | 状态0:无效 1:有效 |
| 10 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 11 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 12 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 13 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态0:无效 1:有效。

### 业务说明

组件库

## 65. 表：`onl_drag_dataset_head` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_dataset_head` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 86 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `name` | 名称 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 名称 |
| 3 | `code` | 编码 | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 编码 |
| 4 | `parent_id` | 父id | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 父id |
| 5 | `db_source` | 动态数据源 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 动态数据源 |
| 6 | `query_sql` | 查询数据SQL | `varchar(5000)` | 5000 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询数据SQL |
| 7 | `content` | 描述 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 8 | `iz_agent` | iz_agent | `varchar(10)` | 10 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | iz_agent |
| 9 | `data_type` | 数据类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据类型 |
| 10 | `api_method` | api方法：get/post | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | api方法：get/post | api方法：get/post |
| 11 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共创建时间字段。 |
| 12 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 13 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共更新时间字段。 |
| 14 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录最后更新用户。 |
| 15 | `low_app_id` | 应用ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 应用ID |
| 16 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `onl_drag_dataset_head.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `api_method`：api方法：get/post。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 66. 表：`onl_drag_dataset_item` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_dataset_item` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 231 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `head_id` | 主表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | idx_oddi_head_id | 否 | 否/待确认 | — | 主表ID |
| 3 | `field_name` | 字段名 | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段名 |
| 4 | `field_txt` | 字段文本 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段文本 |
| 5 | `field_type` | 字段类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段类型 |
| 6 | `widget_type` | 控件类型 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 控件类型 |
| 7 | `dict_code` | 字典Code | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典Code |
| 8 | `dict_table` | 待确认 | `varchar(125)` | 125 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `dict_text` | 待确认 | `varchar(125)` | 125 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `iz_show` | 是否列表显示 | `varchar(5)` | 5 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否列表显示 |
| 11 | `iz_search` | 是否查询 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否查询 |
| 12 | `iz_total` | 是否计算总计（仅对数值有效） | `varchar(5)` | 5 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否计算总计（仅对数值有效） |
| 13 | `search_mode` | 查询模式 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询模式 |
| 14 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 15 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 16 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 17 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 18 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_oddi_head_id` | `head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 67. 表：`onl_drag_dataset_param` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_dataset_param` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 5 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `head_id` | 动态报表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | idx_oddp_head_id | 否 | 否/待确认 | — | 动态报表ID |
| 3 | `param_name` | 参数字段 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数字段 |
| 4 | `param_txt` | 参数文本 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数文本 |
| 5 | `param_value` | 参数默认值 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数默认值 |
| 6 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 7 | `iz_search` | 查询标识0否1是 默认0 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 查询标识0否1是 默认0 | 查询标识0否1是 默认0 |
| 8 | `widget_type` | 查询控件类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询控件类型 |
| 9 | `search_mode` | 查询模式1简单2范围 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 查询模式1简单2范围 | 查询模式1简单2范围 |
| 10 | `dict_code` | 字典 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典 |
| 11 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 12 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 13 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 14 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_oddp_head_id` | `head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `iz_search`：查询标识0否1是 默认0。
- `search_mode`：查询模式1简单2范围。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 68. 表：`onl_drag_page` 可视化拖拽界面

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_page` |
| 中文名称 | 可视化拖拽界面 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 可视化拖拽界面 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 37 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(50)` | 50 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `name` | 界面名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 界面名称 |
| 3 | `path` | 访问路径 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访问路径 |
| 4 | `background_color` | 背景色 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 背景色 |
| 5 | `background_image` | 背景图 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 背景图 |
| 6 | `design_type` | 设计模式(1:pc,2:手机,3:平板) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 设计模式(1:pc,2:手机,3:平板) | 设计模式(1:pc,2:手机,3:平板) |
| 7 | `theme` | 主题色 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主题色 |
| 8 | `style` | 面板主题 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 面板主题 |
| 9 | `cover_url` | 封面图 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 封面图 |
| 10 | `des_json` | 仪表盘主配置JSON | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 仪表盘主配置JSON |
| 11 | `template` | 布局json | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 布局json |
| 12 | `protection_code` | 保护码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保护码 |
| 13 | `type` | 所属分类 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 所属分类 |
| 14 | `iz_template` | 是否模板(1:是；0不是) | `varchar(10)` | 10 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否模板(1:是；0不是) | 是否模板(1:是；0不是) |
| 15 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 16 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 17 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 18 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 19 | `low_app_id` | 应用ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 应用ID |
| 20 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 21 | `update_count` | 数量 | `int` | 10,0 | 是 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 22 | `visits_num` | 访问次数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访问次数 |
| 23 | `del_flag` | 删除状态( 0未删除 1已删除) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 删除状态( 0未删除 1已删除) | 删除状态( 0未删除 1已删除) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `onl_drag_page.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `design_type`：设计模式(1:pc,2:手机,3:平板)。
- `iz_template`：是否模板(1:是；0不是)。
- `del_flag`：删除状态( 0未删除 1已删除)。
- `type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

可视化拖拽界面

## 69. 表：`onl_drag_page_comp` 可视化拖拽页面组件

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_page_comp` |
| 中文名称 | 可视化拖拽页面组件 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 可视化拖拽页面组件 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 1058 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `parent_id` | 父组件ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 父组件ID |
| 3 | `page_Id` | 界面ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 界面ID |
| 4 | `comp_id` | 组件库ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 组件库ID |
| 5 | `component` | 组件名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件名称 |
| 6 | `config` | 组件配置 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件配置 |
| 7 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 8 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 9 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 10 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

可视化拖拽页面组件

## 70. 表：`onl_drag_share` 仪表盘预览分享表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_share` |
| 中文名称 | 仪表盘预览分享表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 仪表盘预览分享表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 12 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `drag_id` | 在线仪表盘设计器id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | uniq_ods_drag_id | uniq_ods_drag_id | 否 | 否/待确认 | — | 在线仪表盘设计器id |
| 3 | `preview_url` | 预览地址 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 预览地址 |
| 4 | `preview_lock` | 密码锁 | `varchar(4)` | 4 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 密码锁 |
| 5 | `last_update_time` | 最后更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最后更新时间 |
| 6 | `term_of_validity` | 有效期(0:永久有效，1:1天，7:7天) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 有效期(0:永久有效，1:1天，7:7天) | 有效期(0:永久有效，1:1天，7:7天) |
| 7 | `status` | 是否过期(0未过期，1已过期) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 是否过期(0未过期，1已过期) | 是否过期(0未过期，1已过期) |
| 8 | `preview_lock_status` | 是否为密码锁(0 否,1是) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否为密码锁(0 否,1是) | 是否为密码锁(0 否,1是) |
| 9 | `share_token` | 分享token | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 分享token |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_ods_drag_id` | `drag_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `term_of_validity`：有效期(0:永久有效，1:1天，7:7天)。
- `status`：是否过期(0未过期，1已过期)。
- `preview_lock_status`：是否为密码锁(0 否,1是)。

### 业务说明

仪表盘预览分享表

## 71. 表：`onl_drag_table_relation` 仪表盘聚合表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_drag_table_relation` |
| 中文名称 | 仪表盘聚合表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 仪表盘聚合表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(50)` | 50 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `aggregation_name` | 聚合表名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | idx_aggregation_name | 否 | 否 | — | 聚合表名称 |
| 3 | `aggregation_desc` | 聚合表描述 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 聚合表描述 |
| 4 | `relation_forms` | 关联表单 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 关联表单 |
| 5 | `filter_condition` | 过滤条件 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 过滤条件 |
| 6 | `header_fields` | 表头字段 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 表头字段 |
| 7 | `calculate_fields` | 公式字段 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 公式字段 |
| 8 | `validate_info` | 校验信息 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 校验信息 |
| 9 | `del_flag` | 删除状态(0-正常,1-已删除) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_del_flag | 是 | 否 | 删除状态(0-正常,1-已删除) | 删除状态(0-正常,1-已删除) |
| 10 | `low_app_id` | 应用ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 应用ID |
| 11 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 12 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_create_by | 是 | 否 | — | 创建人登录名称 |
| 13 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 14 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 15 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_aggregation_name` | `aggregation_name` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_create_by` | `create_by` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_del_flag` | `del_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `onl_drag_table_relation.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `del_flag`：删除状态(0-正常,1-已删除)。

### 业务说明

仪表盘聚合表

## 72. 表：`onl_graphreport_head` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_graphreport_head` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `name` | 图表名称 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图表名称 |
| 3 | `code` | 图表编码 | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | uniq_gpreport_code | uniq_gpreport_code | 否 | 否 | — | 图表编码 |
| 4 | `cgr_sql` | 查询数据SQL | `varchar(5000)` | 5000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询数据SQL |
| 5 | `xaxis_field` | X轴数据字段 | `varchar(100)` | 100 | 否 | `` | 否 | 否 | 否 | 否 | 否 | 否 | — | X轴数据字段 |
| 6 | `yaxis_field` | Y轴数据字段 | `varchar(100)` | 100 | 否 | `` | 否 | 否 | 否 | 否 | 否 | 否 | — | Y轴数据字段 |
| 7 | `yaxis_text` | y轴文字描述 | `varchar(100)` | 100 | 否 | `` | 否 | 否 | 否 | 否 | 否 | 否 | — | y轴文字描述 |
| 8 | `content` | 描述 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 9 | `extend_js` | 扩展JS | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 扩展JS |
| 10 | `graph_type` | 图表类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图表类型 |
| 11 | `is_combination` | 是否组合 | `varchar(50)` | 50 | 是 | `combination` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否组合 |
| 12 | `display_template` | 展示模板 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 展示模板 |
| 13 | `data_type` | 数据类型 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据类型 |
| 14 | `db_source` | 动态数据源 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 动态数据源 |
| 15 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 16 | `low_app_id` | 关联的应用ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 关联的应用ID |
| 17 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共创建时间字段。 |
| 18 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 19 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共更新时间字段。 |
| 20 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录最后更新用户。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_gpreport_code` | `code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `onl_graphreport_head.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 73. 表：`onl_graphreport_item` jform_graphreport_item

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_graphreport_item` |
| 中文名称 | jform_graphreport_item |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | jform_graphreport_item |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 21 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `graphreport_head_id` | 主表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | idx_ogi_graphreport_head_id | 否 | 否/待确认 | — | 主表ID |
| 3 | `field_name` | 字段名 | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段名 |
| 4 | `field_txt` | 字段文本 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段文本 |
| 5 | `is_show` | 是否列表显示 | `varchar(5)` | 5 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ogi_is_show | 否 | 否 | — | 是否列表显示 |
| 6 | `is_total` | 是否计算总计（仅对数值有效） | `varchar(5)` | 5 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否计算总计（仅对数值有效） |
| 7 | `search_flag` | 是否查询 | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否查询 |
| 8 | `search_mode` | 查询模式 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 查询模式 |
| 9 | `dict_code` | 字典Code | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典Code |
| 10 | `field_href` | 字段href | `varchar(120)` | 120 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段href |
| 11 | `field_type` | 字段类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段类型 |
| 12 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 13 | `replace_val` | 取值表达式 | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 取值表达式 |
| 14 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 15 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 16 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 17 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ogi_graphreport_head_id` | `graphreport_head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ogi_is_show` | `is_show` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

jform_graphreport_item

## 74. 表：`onl_graphreport_params` Online图表：参数表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_graphreport_params` |
| 中文名称 | Online图表：参数表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | Online图表：参数表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `head_id` | Online图表ID | `varchar(36)` | 36 | 否 | `无/NULL` | 否 | 否 | 否 | onl_graphreport_param_head_id | 否 | 否/待确认 | — | Online图表ID |
| 3 | `param_name` | 参数字段 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数字段 |
| 4 | `param_txt` | 参数文本 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数文本 |
| 5 | `param_value` | 参数默认值 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数默认值 |
| 6 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 7 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 8 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 9 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 10 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `onl_graphreport_param_head_id` | `head_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

Online图表：参数表

## 75. 表：`onl_graphreport_templet` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_graphreport_templet` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `templet_code` | 待确认 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `templet_name` | 报表名称 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 报表名称 |
| 4 | `templet_style` | 报表风格模板（单排、双排、Tab模式、分组模式-根据配置动态展示、可自定义...） | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 报表风格模板（单排、双排、Tab模式、分组模式-根据配置动态展示、可自定义...） |
| 5 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共创建时间字段。 |
| 6 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 7 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共更新时间字段。 |
| 8 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录最后更新用户。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 76. 表：`onl_graphreport_templet_item` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `onl_graphreport_templet_item` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg Online |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 13 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `graphreport_templet_id` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_ogti_grreport_tempid | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `graphreport_code` | 图表编码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图表编码 |
| 4 | `graphreport_type` | 图表类型（饼状图、曲线图、柱状图、数据列表等） | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 图表类型（饼状图、曲线图、柱状图、数据列表等） |
| 5 | `group_num` | 组合数字，默认值0 非必填 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 组合数字，默认值0 非必填 | 组合数字，默认值0 非必填 |
| 6 | `group_style` | 组合展示风格（1 卡片，2 tab）非必填 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 组合展示风格（1 卡片，2 tab）非必填 | 组合展示风格（1 卡片，2 tab）非必填 |
| 7 | `group_txt` | 分组描述 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 分组描述 |
| 8 | `order_num` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 9 | `is_show` | 是否显示 1显示 0不显示，默认1 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否显示 1显示 0不显示，默认1 | 是否显示 1显示 0不显示，默认1 |
| 10 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共创建时间字段。 |
| 11 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 12 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共更新时间字段。 |
| 13 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录最后更新用户。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ogti_grreport_tempid` | `graphreport_templet_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `group_num`：组合数字，默认值0 非必填。
- `group_style`：组合展示风格（1 卡片，2 tab）非必填。
- `is_show`：是否显示 1显示 0不显示，默认1。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 77. 表：`open_api` 接口表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `open_api` |
| 中文名称 | 接口表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | OpenAPI |
| 业务作用 | 接口表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `name` | 接口名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 接口名称 |
| 3 | `request_method` | 请求方法 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求方法 |
| 4 | `request_url` | 接口地址 | `varchar(300)` | 300 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 接口地址 |
| 5 | `white_list` | IP白名单，支持IP、CIDR、通配符，逗号或换行分隔 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | IP白名单，支持IP、CIDR、通配符，逗号或换行分隔 |
| 6 | `comment` | 白名单备注说明 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 白名单备注说明 |
| 7 | `body` | 请求体内容 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求体内容 |
| 8 | `origin_url` | 原始地址 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 原始地址 |
| 9 | `status` | 状态 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态 |
| 10 | `del_flag` | 删除标识 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 删除标识 |
| 11 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 12 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 13 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 14 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 15 | `headers_json` | 请求头json | `json` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求头json |
| 16 | `params_json` | 请求参数json | `json` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求参数json |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

接口表

## 78. 表：`open_api_auth` 权限表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `open_api_auth` |
| 中文名称 | 权限表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | OpenAPI |
| 业务作用 | 权限表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `name` | 授权名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 授权名称 |
| 3 | `ak` | AK | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | AK |
| 4 | `sk` | SK | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | SK |
| 5 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 8 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 9 | `system_user_id` | 关联系统用户名 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 关联系统用户名 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

权限表

## 79. 表：`open_api_log` 调用记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `open_api_log` |
| 中文名称 | 调用记录表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | OpenAPI |
| 业务作用 | 调用记录表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 27 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `api_id` | 接口ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 接口ID |
| 3 | `call_auth_id` | 调用ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 调用ID |
| 4 | `call_time` | 调用时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 调用时间 |
| 5 | `used_time` | 耗时 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 耗时 |
| 6 | `response_time` | 响应时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 响应时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

调用记录表

## 80. 表：`open_api_permission` openapi授权

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `open_api_permission` |
| 中文名称 | openapi授权 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | OpenAPI |
| 业务作用 | openapi授权 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `api_id` | 接口ID | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 接口ID |
| 3 | `api_auth_id` | 认证ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 认证ID |
| 4 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 5 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 6 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 7 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

openapi授权

## 81. 表：`oss_file` Oss File

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `oss_file` |
| 中文名称 | Oss File |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | Oss File |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `file_name` | 文件名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件名称 |
| 3 | `url` | 文件地址 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件地址 |
| 4 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 5 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 6 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 7 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

Oss File

## 82. 表：`rehealth_ai_conversation` 服务端健康问答会话表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_ai_conversation` |
| 中文名称 | 服务端健康问答会话表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存按租户和用户隔离的权威健康问答会话。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 21 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_ai_conversation_owner_updated | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_ai_conversation_owner_updated | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `title` | 标题 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前会话、研究、报告或业务对象的展示标题。 |
| 5 | `status` | 状态 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 6 | `summary_text` | 摘要文本 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前结果的人类可读摘要。 |
| 7 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 8 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_ai_conversation_owner_updated | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_ai_conversation_owner_updated` | `tenant_id, user_id, updated_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_ai_conversation.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_ai_conversation.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存按租户和用户隔离的权威健康问答会话。

## 83. 表：`rehealth_ai_message` 服务端健康问答消息表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_ai_message` |
| 中文名称 | 服务端健康问答消息表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存健康问答完整消息历史、请求幂等键、Provider 和模型版本。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 66 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `conversation_id` | 会话 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_ai_message_request_role | idx_rehealth_ai_message_conversation_created、uk_rehealth_ai_message_request_role | 否 | 物理→rehealth_ai_conversation.id | — | 标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。 |
| 3 | `tenant_id` | 租户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_ai_message_owner_created | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 4 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_ai_message_owner_created | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 5 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_ai_message_request_role | uk_rehealth_ai_message_request_role | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 6 | `role` | 消息角色 | `varchar(16)` | 16 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_ai_message_request_role | uk_rehealth_ai_message_request_role | 否 | 否 | 具体枚举值待确认 | 标识健康问答消息发送方角色；服务端和本地会话代码据此组装上下文。 |
| 7 | `content` | 消息内容 | `text` | 65535 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前健康问答消息正文。 |
| 8 | `status` | 状态 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 9 | `provider` | 服务提供方 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识产生消息、模型结果或设备数据的 Provider。 |
| 10 | `model_version` | 模型版本 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 11 | `retryable` | 待确认 | `tinyint(1)` | 3,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_ai_message_conversation_created、idx_rehealth_ai_message_owner_created | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_ai_message_conversation_created` | `conversation_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_rehealth_ai_message_owner_created` | `tenant_id, user_id, created_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_ai_message_request_role` | `conversation_id, request_id, role` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_ai_message.(conversation_id)` → `rehealth_ai_conversation.(id)`：物理外键；ON DELETE CASCADE。
- `rehealth_ai_message.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_ai_message.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `role, status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存健康问答完整消息历史、请求幂等键、Provider 和模型版本。

## 84. 表：`rehealth_attribution_event` 归因请求事件表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_attribution_event` |
| 中文名称 | 归因请求事件表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存提交给 PIAS 的个体归因请求元数据和版本化输入快照。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_attribution_user_date | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `attribution_request_id` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_attribution_request | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `event_date` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_attribution_user_date | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `risk_score` | 风险分数 | `double` | 22 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型返回的风险数值；解释范围和概率语义必须以模型契约为准。 |
| 6 | `intervention_id` | 干预行动 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 标识用户反馈所针对的具体干预行动。 |
| 7 | `adherence` | 依从性 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户反馈中记录的干预执行或依从情况。 |
| 8 | `baseline_risk_score` | 待确认 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_attribution_request` | `attribution_request_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_rehealth_attribution_user_date` | `user_id, event_date` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_attribution_event.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存提交给 PIAS 的个体归因请求元数据和版本化输入快照。

## 85. 表：`rehealth_attribution_result` 个体归因结果表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_attribution_result` |
| 中文名称 | 个体归因结果表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存 PIAS 个体归因结果及模型证据快照。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 14 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_attribution_result_user_created | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `status` | 状态 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 4 | `model_version` | 模型版本 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 5 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 6 | `attribution_mode` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `is_mock` | 是否模拟数据 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 明确标识结果是否来自 Mock/合成路径；生产结果不得为真。 |
| 8 | `provider` | 服务提供方 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识产生消息、模型结果或设备数据的 Provider。 |
| 9 | `history_days` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `min_history_days` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `intervention_days` | 待确认 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `intervention_data_sufficient` | 待确认 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 13 | `current_risk_score` | 当前风险分数 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险风险查询中读取的最新已确认 CVD 风险分数。 |
| 14 | `current_risk_level` | 当前风险等级 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险风险查询中读取的最新已确认风险等级。 |
| 15 | `current_trend` | 当前趋势 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前业务对象的描述性趋势；不表示因果或诊断。 |
| 16 | `individual_att` | 待确认 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 17 | `trend_delta` | 趋势变化值 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前风险或指标相对既定历史参考的变化量。 |
| 18 | `adherence_average` | 待确认 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 19 | `interpretation` | 待确认 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 20 | `error_code` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 21 | `retryable` | 待确认 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 22 | `request_json` | JSON 快照 | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 23 | `response_json` | 响应证据 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存模型或 Provider 的版本化结构化响应快照。 |
| 24 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_attribution_result_user_created | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_attribution_result_user_created` | `user_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_attribution_result.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存 PIAS 个体归因结果及模型证据快照。

## 86. 表：`rehealth_behavior_record` 结构化行为记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_behavior_record` |
| 中文名称 | 结构化行为记录表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存拍照食物/OCR 的已验证结构化结果；不保存原始图片。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 110 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_behavior_owner_request | idx_rehealth_behavior_owner_occurred、uk_rehealth_behavior_owner_request | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_behavior_owner_request | idx_rehealth_behavior_owner_occurred、uk_rehealth_behavior_owner_request | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_behavior_owner_request | uk_rehealth_behavior_owner_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 5 | `category` | 分类 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 当前行为、干预或业务记录的分类；具体枚举待对应代码确认。 |
| 6 | `title` | 标题 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前会话、研究、报告或业务对象的展示标题。 |
| 7 | `summary` | 摘要 | `varchar(2000)` | 2000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前结果的结构化或可展示摘要。 |
| 8 | `items_json` | 干预行动列表 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存有序结构化干预行动和证据引用。 |
| 9 | `calories_kcal` | 热量 | `decimal(10,2)` | 10,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食或活动能量，单位千卡。 |
| 10 | `protein_grams` | 蛋白质 | `decimal(10,2)` | 10,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食蛋白质估计值，单位克。 |
| 11 | `carbohydrate_grams` | 碳水化合物 | `decimal(10,2)` | 10,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食碳水化合物估计值，单位克。 |
| 12 | `fat_grams` | 脂肪 | `decimal(10,2)` | 10,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 餐食脂肪估计值，单位克。 |
| 13 | `ocr_text` | 待确认 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 14 | `confidence` | 置信度 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 15 | `model_version` | 模型版本 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 16 | `occurred_at` | 时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_behavior_owner_occurred | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 17 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_behavior_owner_occurred` | `tenant_id, user_id, occurred_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_behavior_owner_request` | `tenant_id, user_id, request_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_behavior_record.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_behavior_record.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `category`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存拍照食物/OCR 的已验证结构化结果；不保存原始图片。

## 87. 表：`rehealth_care_plan` 机构干预计划主表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_care_plan` |
| 中文名称 | 机构干预计划主表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存按租户、机构类型和服务对象隔离的计划聚合、当前/草稿版本指针及乐观锁。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 关怀计划主键 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 关怀计划主键 |
| 2 | `tenant_id` | 所属 Jeecg 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_current_revision、idx_care_plan_draft_revision、idx_care_plan_subject、idx_care_plan_user | 是 | 逻辑→sys_tenant.id | — | 所属 Jeecg 租户 ID |
| 3 | `owner_type` | 计划所属机构类型：保险、医疗或个人 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_subject | 否 | 否 | 计划所属机构类型：保险、医疗或个人 | 计划所属机构类型：保险、医疗或个人 |
| 4 | `owner_org_ref` | 所属机构引用；保险机构当前使用租户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 所属机构引用；保险机构当前使用租户 ID |
| 5 | `subject_ref` | 租户范围内的服务对象引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_subject | 否 | 否 | — | 租户范围内的服务对象引用 |
| 6 | `rehealth_user_id` | 由可信服务关系解析的 ReHealth APP 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_user | 否 | 逻辑→sys_user.id | — | 由可信服务关系解析的 ReHealth APP 用户 ID |
| 7 | `source_plan_id` | 可选的历史计划或外部计划标识 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 可选的历史计划或外部计划标识 |
| 8 | `status` | 计划生命周期状态：草稿、生效或已撤回 | `varchar(32)` | 32 | 否 | `draft` | 否 | 否 | 否 | idx_care_plan_subject、idx_care_plan_user | 是 | 否 | 计划生命周期状态：草稿、生效或已撤回 | 计划生命周期状态：草稿、生效或已撤回 |
| 9 | `current_revision_id` | 最新发布版本 ID；该版本可在未来时间生效 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_current_revision | 否 | 逻辑→rehealth_care_plan_revision.id | — | 最新发布版本 ID；该版本可在未来时间生效 |
| 10 | `draft_revision_id` | 当前唯一可编辑的草稿版本 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_draft_revision | 否 | 逻辑→rehealth_care_plan_revision.id | — | 当前唯一可编辑的草稿版本 ID |
| 11 | `lock_version` | 计划全部变更使用的乐观锁版本号 | `bigint` | 19,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 计划全部变更使用的乐观锁版本号 |
| 12 | `created_by` | 创建计划的认证用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建计划的认证用户 ID |
| 13 | `created_at` | 计划创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 计划创建时间 |
| 14 | `updated_by` | 最后更新计划的认证用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最后更新计划的认证用户 ID |
| 15 | `updated_at` | 计划最后更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_subject、idx_care_plan_user | 是 | 否 | — | 计划最后更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_care_plan_current_revision` | `tenant_id, current_revision_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_care_plan_draft_revision` | `tenant_id, draft_revision_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_care_plan_subject` | `tenant_id, owner_type, subject_ref, status, updated_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_care_plan_user` | `tenant_id, rehealth_user_id, status, updated_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_care_plan.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_care_plan.(rehealth_user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。
- `rehealth_care_plan.(current_revision_id)` → `rehealth_care_plan_revision.(id)`：逻辑外键；当前最新已发布版本逻辑外键。
- `rehealth_care_plan.(draft_revision_id)` → `rehealth_care_plan_revision.(id)`：逻辑外键；单一可变草稿版本逻辑外键。

### 枚举与约束

- `owner_type`：计划所属机构类型：保险、医疗或个人。
- `status`：计划生命周期状态：草稿、生效或已撤回。

### 业务说明

保存按租户、机构类型和服务对象隔离的计划聚合、当前/草稿版本指针及乐观锁。

## 88. 表：`rehealth_care_plan_audit_event` 机构干预计划审计表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_care_plan_audit_event` |
| 中文名称 | 机构干预计划审计表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存不含计划正文的版本生命周期操作、内容哈希和变更原因。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 计划审计事件主键 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 计划审计事件主键 |
| 2 | `tenant_id` | 所属 Jeecg 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_audit_actor、idx_care_plan_audit_plan | 是 | 逻辑→sys_tenant.id | — | 所属 Jeecg 租户 ID |
| 3 | `owner_type` | 用于审计筛选的计划所属机构类型 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用于审计筛选的计划所属机构类型 |
| 4 | `actor_user_id` | 执行操作的认证用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_audit_actor | 否 | 否/待确认 | — | 执行操作的认证用户 ID |
| 5 | `action` | 版本操作，例如创建草稿、更新草稿、克隆版本、发布或撤回 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 版本操作，例如创建草稿、更新草稿、克隆版本、发布或撤回 |
| 6 | `plan_id` | 受影响的关怀计划 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_audit_plan | 否 | 逻辑→rehealth_care_plan.id | — | 受影响的关怀计划 ID |
| 7 | `revision_id` | 受影响的计划版本 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 逻辑→rehealth_care_plan_revision.id | — | 受影响的计划版本 ID |
| 8 | `before_hash` | 操作前的计划内容摘要 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作前的计划内容摘要 |
| 9 | `after_hash` | 操作后的计划内容摘要 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作后的计划内容摘要 |
| 10 | `reason` | 长度受限的机构变更或撤回原因 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 长度受限的机构变更或撤回原因 |
| 11 | `created_at` | 审计事件创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_audit_actor、idx_care_plan_audit_plan | 是 | 否 | — | 审计事件创建时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_care_plan_audit_actor` | `tenant_id, actor_user_id, created_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `idx_care_plan_audit_plan` | `tenant_id, plan_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_care_plan_audit_event.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_care_plan_audit_event.(plan_id)` → `rehealth_care_plan.(id)`：逻辑外键；计划版本审计所属聚合逻辑外键。
- `rehealth_care_plan_audit_event.(revision_id)` → `rehealth_care_plan_revision.(id)`：逻辑外键；计划版本审计目标版本逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存不含计划正文的版本生命周期操作、内容哈希和变更原因。

## 89. 表：`rehealth_care_plan_item` 机构干预计划项目表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_care_plan_item` |
| 中文名称 | 机构干预计划项目表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存绑定到具体版本的患者可见计划项目快照及稳定逻辑项目标识。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 版本内计划项目主键 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 版本内计划项目主键 |
| 2 | `tenant_id` | 从计划主表复制的所属 Jeecg 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_item_logical、联合唯一:uk_care_plan_item_order | idx_care_plan_item_plan、uk_care_plan_item_logical、uk_care_plan_item_order | 是 | 逻辑→sys_tenant.id | — | 从计划主表复制的所属 Jeecg 租户 ID |
| 3 | `plan_id` | 所属关怀计划 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_item_plan | 否 | 逻辑→rehealth_care_plan.id | — | 所属关怀计划 ID |
| 4 | `revision_id` | 包含该不可变项目快照的计划版本 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_item_logical、联合唯一:uk_care_plan_item_order | idx_care_plan_item_plan、uk_care_plan_item_logical、uk_care_plan_item_order | 否 | 逻辑→rehealth_care_plan_revision.id | — | 包含该不可变项目快照的计划版本 ID |
| 5 | `logical_item_id` | 克隆新版本时保持不变的逻辑项目 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_item_logical | uk_care_plan_item_logical | 否 | 否/待确认 | — | 克隆新版本时保持不变的逻辑项目 ID |
| 6 | `category` | 保守干预分类，例如运动、营养、睡眠或随访 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 保守干预分类，例如运动、营养、睡眠或随访 |
| 7 | `title` | 用户可见的计划项目标题 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户可见的计划项目标题 |
| 8 | `instructions` | 长度受限的用户可见执行说明 | `varchar(4000)` | 4000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 长度受限的用户可见执行说明 |
| 9 | `schedule_json` | 结构化计划规则，由独立任务实例生成器展开 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 结构化计划规则，由独立任务实例生成器展开 |
| 10 | `scoring_weight` | 每个已生成任务实例的依从性计分权重 | `decimal(10,3)` | 10,3 | 否 | `1.000` | 否 | 否 | 否 | 否 | 否 | 否 | — | 每个已生成任务实例的依从性计分权重 |
| 11 | `allow_not_applicable` | 用户是否可以将任务标记为不适用 | `tinyint(1)` | 3,0 | 否 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户是否可以将任务标记为不适用 |
| 12 | `display_order` | 当前版本内稳定的展示顺序 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_item_order | uk_care_plan_item_order | 否 | 否 | — | 当前版本内稳定的展示顺序 |
| 13 | `created_at` | 计划项目快照创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 计划项目快照创建时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_care_plan_item_plan` | `tenant_id, plan_id, revision_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_care_plan_item_logical` | `tenant_id, revision_id, logical_item_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_care_plan_item_order` | `tenant_id, revision_id, display_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_care_plan_item.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_care_plan_item.(plan_id)` → `rehealth_care_plan.(id)`：逻辑外键；计划项目所属聚合逻辑外键。
- `rehealth_care_plan_item.(revision_id)` → `rehealth_care_plan_revision.(id)`：逻辑外键；计划项目所属不可变版本逻辑外键。

### 枚举与约束

- `category`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存绑定到具体版本的患者可见计划项目快照及稳定逻辑项目标识。

## 90. 表：`rehealth_care_plan_occurrence` 机构干预任务实例表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_care_plan_occurrence` |
| 中文名称 | 机构干预任务实例表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存绑定计划版本和项目的到期任务实例，为后续真实依从性分母提供稳定标识。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 用于反馈幂等的计划任务实例主键 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 用于反馈幂等的计划任务实例主键 |
| 2 | `tenant_id` | 从计划主表复制的所属 Jeecg 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_occurrence_due | idx_care_plan_occurrence_revision、idx_care_plan_occurrence_subject_due、uk_care_plan_occurrence_due | 是 | 逻辑→sys_tenant.id | — | 从计划主表复制的所属 Jeecg 租户 ID |
| 3 | `plan_id` | 所属关怀计划 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 逻辑→rehealth_care_plan.id | — | 所属关怀计划 ID |
| 4 | `revision_id` | 生成该任务实例的已发布版本 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_occurrence_revision | 否 | 逻辑→rehealth_care_plan_revision.id | — | 生成该任务实例的已发布版本 ID |
| 5 | `plan_item_id` | 生成该任务实例的版本内计划项目 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_occurrence_due | uk_care_plan_occurrence_due | 否 | 逻辑→rehealth_care_plan_item.id | — | 生成该任务实例的版本内计划项目 ID |
| 6 | `logical_item_id` | 跨计划版本保持稳定的逻辑项目 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 跨计划版本保持稳定的逻辑项目 ID |
| 7 | `subject_ref` | 租户范围内的服务对象引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_occurrence_subject_due | 否 | 否 | — | 租户范围内的服务对象引用 |
| 8 | `scheduled_at` | 按统一服务端时间记录的计划执行时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_occurrence_due | idx_care_plan_occurrence_revision、uk_care_plan_occurrence_due | 否 | 否 | — | 按统一服务端时间记录的计划执行时间 |
| 9 | `due_at` | 用于计算依从性时间窗口的截止时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_occurrence_subject_due | 否 | 否 | — | 用于计算依从性时间窗口的截止时间 |
| 10 | `status` | 任务实例状态：待执行或已取消；执行事实单独存储 | `varchar(32)` | 32 | 否 | `scheduled` | 否 | 否 | 否 | idx_care_plan_occurrence_revision、idx_care_plan_occurrence_subject_due | 是 | 否 | 任务实例状态：待执行或已取消；执行事实单独存储 | 任务实例状态：待执行或已取消；执行事实单独存储 |
| 11 | `exclusion_reason` | 已取消任务不计入依从性的原因 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 已取消任务不计入依从性的原因 |
| 12 | `created_at` | 任务实例创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 任务实例创建时间 |
| 13 | `updated_at` | 任务实例最后更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 任务实例最后更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_care_plan_occurrence_revision` | `tenant_id, revision_id, status, scheduled_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_care_plan_occurrence_subject_due` | `tenant_id, subject_ref, status, due_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_care_plan_occurrence_due` | `tenant_id, plan_item_id, scheduled_at` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_care_plan_occurrence.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_care_plan_occurrence.(plan_id)` → `rehealth_care_plan.(id)`：逻辑外键；任务实例所属计划逻辑外键。
- `rehealth_care_plan_occurrence.(revision_id)` → `rehealth_care_plan_revision.(id)`：逻辑外键；任务实例生成版本逻辑外键。
- `rehealth_care_plan_occurrence.(plan_item_id)` → `rehealth_care_plan_item.(id)`：逻辑外键；任务实例生成项目逻辑外键。

### 枚举与约束

- `status`：任务实例状态：待执行或已取消；执行事实单独存储。

### 业务说明

保存绑定计划版本和项目的到期任务实例，为后续真实依从性分母提供稳定标识。

## 91. 表：`rehealth_care_plan_revision` 机构干预计划版本表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_care_plan_revision` |
| 中文名称 | 机构干预计划版本表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存草稿、已发布和已撤回的计划版本；已发布内容不可原地覆盖。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 计划版本主键 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 计划版本主键 |
| 2 | `tenant_id` | 从计划主表复制的所属 Jeecg 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_revision_no | idx_care_plan_revision_effective、idx_care_plan_revision_hash、uk_care_plan_revision_no | 是 | 逻辑→sys_tenant.id | — | 从计划主表复制的所属 Jeecg 租户 ID |
| 3 | `plan_id` | 所属关怀计划 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_revision_no | idx_care_plan_revision_effective、uk_care_plan_revision_no | 否 | 逻辑→rehealth_care_plan.id | — | 所属关怀计划 ID |
| 4 | `revision_no` | 计划内单调递增的版本序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_care_plan_revision_no | uk_care_plan_revision_no | 否 | 否 | — | 计划内单调递增的版本序号 |
| 5 | `status` | 版本状态：草稿、已发布或已撤回 | `varchar(32)` | 32 | 否 | `draft` | 否 | 否 | 否 | idx_care_plan_revision_effective | 是 | 否 | 版本状态：草稿、已发布或已撤回 | 版本状态：草稿、已发布或已撤回 |
| 6 | `title` | 用户可见的计划标题 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户可见的计划标题 |
| 7 | `summary` | 长度受限的用户可见计划摘要 | `varchar(2000)` | 2000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 长度受限的用户可见计划摘要 |
| 8 | `change_reason` | 机构填写的本次版本变更原因 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 机构填写的本次版本变更原因 |
| 9 | `content_hash` | 版本元数据及有序计划项目的 SHA-256 摘要 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_revision_hash | 否 | 否 | — | 版本元数据及有序计划项目的 SHA-256 摘要 |
| 10 | `effective_from` | 发布时设置的版本生效时间，包含该时间点 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_revision_effective | 否 | 否 | — | 发布时设置的版本生效时间，包含该时间点 |
| 11 | `effective_to` | 由新版本或撤回设置的失效时间，不包含该时间点 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_care_plan_revision_effective | 否 | 否 | — | 由新版本或撤回设置的失效时间，不包含该时间点 |
| 12 | `published_by` | 发布版本的认证用户 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 发布版本的认证用户 ID |
| 13 | `published_at` | 版本发布时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 版本发布时间 |
| 14 | `withdrawn_by` | 撤回版本的认证用户 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 撤回版本的认证用户 ID |
| 15 | `withdrawn_at` | 版本撤回时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 版本撤回时间 |
| 16 | `created_by` | 创建版本的认证用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建版本的认证用户 ID |
| 17 | `created_at` | 版本创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 版本创建时间 |
| 18 | `updated_by` | 最后编辑草稿的认证用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 最后编辑草稿的认证用户 ID |
| 19 | `updated_at` | 版本最后更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 版本最后更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_care_plan_revision_effective` | `tenant_id, plan_id, status, effective_from, effective_to` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_care_plan_revision_hash` | `tenant_id, content_hash` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_care_plan_revision_no` | `tenant_id, plan_id, revision_no` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_care_plan_revision.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_care_plan_revision.(plan_id)` → `rehealth_care_plan.(id)`：逻辑外键；计划版本所属聚合逻辑外键。

### 枚举与约束

- `status`：版本状态：草稿、已发布或已撤回。

### 业务说明

保存草稿、已发布和已撤回的计划版本；已发布内容不可原地覆盖。

## 92. 表：`rehealth_cvd_feature_vector` CVD 特征向量表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_cvd_feature_vector` |
| 中文名称 | CVD 特征向量表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存一次 CVD-16 评估使用的版本化特征向量和质量证据。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 414 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_feature_user_request | idx_feature_user_created、uk_rehealth_feature_user_request | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_feature_user_request | uk_rehealth_feature_user_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 4 | `feature_schema_version` | 特征协议版本 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识特征向量遵循的字段协议版本。 |
| 5 | `feature_json` | 特征向量 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存一次模型评估实际使用的版本化特征向量。 |
| 6 | `quality_json` | 特征质量 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存特征缺失、质量和来源等版本化元数据。 |
| 7 | `payload_json` | 载荷 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。 |
| 8 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_feature_user_created | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_feature_user_created` | `user_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_feature_user_request` | `user_id, request_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_cvd_feature_vector.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存一次 CVD-16 评估使用的版本化特征向量和质量证据。

## 93. 表：`rehealth_cvd_risk_result` CVD 风险结果表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_cvd_risk_result` |
| 中文名称 | CVD 风险结果表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存模型风险分数、等级、模型贡献、Factor16 贡献、警告和模型版本。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 400 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `feature_vector_id` | 特征向量记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | fk_rehealth_risk_feature | 否 | 物理→rehealth_cvd_feature_vector.id | — | 物理关联 rehealth_cvd_feature_vector.id。 |
| 3 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_risk_user_request | idx_risk_user_created、uk_rehealth_risk_user_request | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 4 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_risk_user_request | uk_rehealth_risk_user_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 5 | `feature_schema_version` | 特征协议版本 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识特征向量遵循的字段协议版本。 |
| 6 | `model_version` | 模型版本 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 7 | `scorer_mode` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `is_mock` | 是否模拟数据 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 明确标识结果是否来自 Mock/合成路径；生产结果不得为真。 |
| 9 | `artifact_name` | 模型制品名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识产生结果时使用的已加载模型制品。 |
| 10 | `fallback_reason` | 回退原因 | `varchar(512)` | 512 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 记录模型为何使用回退路径；生产不得静默伪装 Mock。 |
| 11 | `contribution_method` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 12 | `factor_contribution_version` | Factor16 规则版本 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识产生 Factor16 贡献的规则版本。 |
| 13 | `risk_score` | 风险分数 | `double` | 22 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型返回的风险数值；解释范围和概率语义必须以模型契约为准。 |
| 14 | `risk_level` | 风险等级 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模型基于风险分数返回的离散等级；完整枚举待模型契约确认。 |
| 15 | `contribution_json` | 模型贡献 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存模型原始特征贡献，用于模型审计。 |
| 16 | `factor_contribution_json` | Factor16 贡献 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存独立 Factor16 规则的逐字段贡献。 |
| 17 | `factor_measured_component_json` | Factor16 实测分量 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存 Factor16 中经确认实测部分的贡献分量。 |
| 18 | `factor_control_support_json` | Factor16 控制支持分量 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存 Factor16 中有证据的控制支持趋势分量。 |
| 19 | `missing_fields_json` | 缺失字段 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存本次模型评估缺少的输入字段列表。 |
| 20 | `quality_warnings_json` | 质量警告 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存本次模型评估产生的数据质量警告。 |
| 21 | `summary` | 摘要 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前结果的结构化或可展示摘要。 |
| 22 | `response_json` | 响应证据 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存模型或 Provider 的版本化结构化响应快照。 |
| 23 | `evaluated_at` | 评估时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_risk_user_created | 否 | 否 | — | 模型或规则完成评估的时间。 |
| 24 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_rehealth_risk_feature` | `feature_vector_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_risk_user_created` | `user_id, evaluated_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_risk_user_request` | `user_id, request_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_cvd_risk_result.(feature_vector_id)` → `rehealth_cvd_feature_vector.(id)`：物理外键；ON DELETE NO ACTION。
- `rehealth_cvd_risk_result.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存模型风险分数、等级、模型贡献、Factor16 贡献、警告和模型版本。

## 94. 表：`rehealth_device_binding` 用户设备绑定表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_device_binding` |
| 中文名称 | 用户设备绑定表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存认证用户与产品、稳定设备身份及状态的绑定关系。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 39 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_device_user_device | idx_rehealth_device_user_updated、uk_rehealth_device_user_device | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `device_id` | 稳定设备 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_device_user_device | uk_rehealth_device_user_device | 否 | 否/待确认 | — | 数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。 |
| 4 | `device_name` | 设备名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 设备绑定或上报中的可展示设备名称。 |
| 5 | `manufacturer` | 设备制造商 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 绑定设备的制造商标识。 |
| 6 | `device_model` | 设备型号 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 设备上报或绑定时记录的具体型号。 |
| 7 | `model` | 设备型号 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 绑定设备的型号标识。 |
| 8 | `firmware_version` | 固件版本 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 采集当前数据时设备固件的版本。 |
| 9 | `hardware_address_hash` | 硬件地址摘要 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 设备硬件地址的不可逆摘要；不保存原始 BLE MAC。 |
| 10 | `status` | 状态 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 11 | `bound_at` | 绑定时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户与设备绑定建立的时间。 |
| 12 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_device_user_updated | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_device_user_updated` | `user_id, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_device_user_device` | `user_id, device_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_device_binding.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存认证用户与产品、稳定设备身份及状态的绑定关系。

## 95. 表：`rehealth_health_interview` 健康访谈主表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_health_interview` |
| 中文名称 | 健康访谈主表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存认证用户每次结构化健康访谈的主记录和兼容 JSON 快照。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 33 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_health_interview_user_created | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `generated_at` | 生成时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 计划或结果完成生成的时间。 |
| 4 | `answers_json` | 访谈回答兼容快照 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存完整访谈回答的版本化 JSON；类型化回答表是主要查询结构。 |
| 5 | `baseline_json` | 基线证据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存风险、归因或研究计算使用的版本化基线快照。 |
| 6 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_health_interview_user_created | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_health_interview_user_created` | `user_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_health_interview.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存认证用户每次结构化健康访谈的主记录和兼容 JSON 快照。

## 96. 表：`rehealth_health_interview_answer` 健康访谈回答表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_health_interview_answer` |
| 中文名称 | 健康访谈回答表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存访谈下的有序问答明细。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 190 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `interview_id` | 健康访谈记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_interview_answer_order | uk_rehealth_interview_answer_order | 否 | 物理→rehealth_health_interview.id | — | 物理关联 rehealth_health_interview.id。 |
| 3 | `question_id` | 问题 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 标识访谈回答对应的稳定问题。 |
| 4 | `topic` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `content` | 消息内容 | `text` | 65535 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前健康问答消息正文。 |
| 6 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_interview_answer_order | uk_rehealth_interview_answer_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_interview_answer_order` | `interview_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_health_interview_answer.(interview_id)` → `rehealth_health_interview.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存访谈下的有序问答明细。

## 97. 表：`rehealth_health_interview_baseline` 健康访谈基线表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_health_interview_baseline` |
| 中文名称 | 健康访谈基线表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存访谈提取的有序健康基线指标。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 189 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `interview_id` | 健康访谈记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_interview_baseline_order | uk_rehealth_interview_baseline_order | 否 | 物理→rehealth_health_interview.id | — | 物理关联 rehealth_health_interview.id。 |
| 3 | `label` | 待确认 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `item_value` | 回答/基线值 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访谈明细中的类型化或文本值。 |
| 5 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_interview_baseline_order | uk_rehealth_interview_baseline_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_interview_baseline_order` | `interview_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_health_interview_baseline.(interview_id)` → `rehealth_health_interview.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存访谈提取的有序健康基线指标。

## 98. 表：`rehealth_health_interview_focus` 健康访谈关注项表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_health_interview_focus` |
| 中文名称 | 健康访谈关注项表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存访谈识别出的重点健康关注项。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 90 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `interview_id` | 健康访谈记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_interview_focus_order | uk_rehealth_interview_focus_order | 否 | 物理→rehealth_health_interview.id | — | 物理关联 rehealth_health_interview.id。 |
| 3 | `focus_area` | 健康关注领域 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访谈识别出的重点健康关注领域。 |
| 4 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_interview_focus_order | uk_rehealth_interview_focus_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_interview_focus_order` | `interview_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_health_interview_focus.(interview_id)` → `rehealth_health_interview.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存访谈识别出的重点健康关注项。

## 99. 表：`rehealth_insurance_audit_event` 保险操作审计表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_audit_event` |
| 中文名称 | 保险操作审计表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存租户内保险资源操作的不可变审计事件和前后哈希。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 140 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_audit_request、idx_insurance_audit_tenant_actor、idx_insurance_audit_tenant_resource | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `actor_user_id` | 操作用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_audit_tenant_actor | 否 | 否/待确认 | — | 执行审批或审计动作的内部用户。 |
| 4 | `action` | 操作动作 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存审批或审计动作；具体枚举由对应业务服务定义。 |
| 5 | `resource_type` | 资源类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_audit_tenant_resource | 否 | 否 | — | 保险审计事件所操作资源的类型。 |
| 6 | `resource_id` | 资源 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_audit_tenant_resource | 否 | 否/待确认 | — | 保险审计事件所操作资源的记录标识。 |
| 7 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_audit_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 8 | `before_hash` | 变更前哈希 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 资源变更前内容的完整性摘要。 |
| 9 | `after_hash` | 变更后哈希 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 资源变更后内容的完整性摘要。 |
| 10 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 11 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_audit_tenant_actor、idx_insurance_audit_tenant_resource | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_audit_request` | `tenant_id, request_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_insurance_audit_tenant_actor` | `tenant_id, actor_user_id, created_at` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `idx_insurance_audit_tenant_resource` | `tenant_id, resource_type, resource_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_insurance_audit_event.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存租户内保险资源操作的不可变审计事件和前后哈希。

## 100. 表：`rehealth_insurance_claim` 保险理赔表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_claim` |
| 中文名称 | 保险理赔表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存理赔事件、金额、状态和保障代码。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 49 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_claim_source_record、联合唯一:uk_insurance_claim_tenant_no | idx_insurance_claim_period、idx_insurance_claim_policy、idx_insurance_claim_subject_status、uk_insurance_claim_source_record、uk_insurance_claim_tenant_no | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `claim_no` | 理赔号 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_claim_tenant_no | uk_insurance_claim_tenant_no | 否 | 否 | — | 租户内唯一的理赔业务编号。 |
| 4 | `policy_id` | 保单记录 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_claim_policy | 否 | 逻辑→rehealth_insurance_policy.id | — | 逻辑关联 rehealth_insurance_policy.id。 |
| 5 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_claim_subject_status | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 6 | `claim_type` | 理赔类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 理赔事件分类；完整枚举待保险业务确认。 |
| 7 | `event_on` | 出险日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_claim_period、idx_insurance_claim_policy | 否 | 否 | — | 理赔对应保险事件的发生日期。 |
| 8 | `submitted_at` | 提交时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 理赔、报告或审批流程提交时间。 |
| 9 | `decided_at` | 理赔决定时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险理赔完成审核决定的时间。 |
| 10 | `status` | 状态 | `varchar(32)` | 32 | 否 | `submitted` | 否 | 否 | 否 | idx_insurance_claim_period、idx_insurance_claim_subject_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 11 | `billed_amount` | 申请金额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 理赔申请或医疗账单金额。 |
| 12 | `approved_amount` | 批准金额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 审核或结算批准的金额。 |
| 13 | `paid_amount` | 已支付金额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 理赔实际支付金额。 |
| 14 | `currency` | 币种 | `char(3)` | 3 | 否 | `CNY` | 否 | 否 | 否 | 否 | 否 | 否 | — | 金额字段采用的三字符货币代码，默认 CNY。 |
| 15 | `coverage_code` | 保障责任编码 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险产品保障责任的稳定代码。 |
| 16 | `outcome_code` | 理赔结局代码 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 理赔审核或支付结局的稳定代码。 |
| 17 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_claim_source_record | uk_insurance_claim_source_record | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 18 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_claim_source_record | uk_insurance_claim_source_record | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 19 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 20 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 21 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_claim_period` | `tenant_id, event_on, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_claim_policy` | `tenant_id, policy_id, event_on` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_insurance_claim_subject_status` | `tenant_id, subject_ref, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_claim_source_record` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_claim_tenant_no` | `tenant_id, claim_no` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_claim.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_claim.(policy_id)` → `rehealth_insurance_policy.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。
- `rehealth_insurance_claim.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存理赔事件、金额、状态和保障代码。

## 101. 表：`rehealth_insurance_consent` 保险授权同意表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_consent` |
| 中文名称 | 保险授权同意表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存主体按类型和版本授予或撤销的授权及证据哈希。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 48 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_consent_version | idx_insurance_consent_current、idx_insurance_consent_updated、uk_insurance_consent_version | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_consent_version | idx_insurance_consent_current、uk_insurance_consent_version | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 4 | `consent_type` | 授权类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_consent_version | idx_insurance_consent_current、uk_insurance_consent_version | 否 | 否 | — | 主体授权覆盖的数据或用途类型。 |
| 5 | `consent_version` | 授权版本 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_consent_version | uk_insurance_consent_version | 否 | 否 | — | 主体同意的授权文本或协议版本。 |
| 6 | `status` | 状态 | `varchar(32)` | 32 | 否 | `granted` | 否 | 否 | 否 | idx_insurance_consent_current | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 7 | `granted_at` | 授权授予时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 授权状态变为 granted 的时间。 |
| 8 | `revoked_at` | 授权撤销时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主体撤销授权的时间。 |
| 9 | `evidence_ref` | 授权证据引用 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 指向受控授权证据的引用，不直接保存证据正文。 |
| 10 | `evidence_hash` | 证据哈希 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 授权、报告或结算证据内容的完整性摘要。 |
| 11 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `rehealth_app` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 12 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 13 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 14 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 15 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_consent_updated | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_consent_current` | `tenant_id, subject_ref, consent_type, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_consent_updated` | `tenant_id, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_consent_version` | `tenant_id, subject_ref, consent_type, consent_version` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_consent.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_consent.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存主体按类型和版本授予或撤销的授权及证据哈希。

## 102. 表：`rehealth_insurance_coverage` 保险保障责任表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_coverage` |
| 中文名称 | 保险保障责任表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存保单下的保障代码、限额、免赔额和有效期。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 48 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_coverage_source_record | idx_insurance_coverage_policy、idx_insurance_coverage_subject、uk_insurance_coverage_source_record | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `policy_id` | 保单记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_coverage_policy | 否 | 逻辑→rehealth_insurance_policy.id | — | 逻辑关联 rehealth_insurance_policy.id。 |
| 4 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_coverage_subject | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 5 | `coverage_code` | 保障责任编码 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险产品保障责任的稳定代码。 |
| 6 | `coverage_name` | 保障责任名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险保障责任的可展示名称。 |
| 7 | `limit_amount` | 保障限额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前保障责任的最高限额。 |
| 8 | `deductible_amount` | 免赔额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单或保障责任的免赔金额。 |
| 9 | `effective_on` | 生效日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单或保障责任开始生效日期。 |
| 10 | `expires_on` | 到期日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单或保障责任到期日期。 |
| 11 | `status` | 状态 | `varchar(32)` | 32 | 否 | `active` | 否 | 否 | 否 | idx_insurance_coverage_policy、idx_insurance_coverage_subject | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 12 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_coverage_source_record | uk_insurance_coverage_source_record | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 13 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_coverage_source_record | uk_insurance_coverage_source_record | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 14 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 15 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 16 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_coverage_policy` | `tenant_id, policy_id, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_coverage_subject` | `tenant_id, subject_ref, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_coverage_source_record` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_coverage.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_coverage.(policy_id)` → `rehealth_insurance_policy.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。
- `rehealth_insurance_coverage.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存保单下的保障代码、限额、免赔额和有效期。

## 103. 表：`rehealth_insurance_import_batch` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_import_batch` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_import_idempotency | idx_insurance_import_status、uk_insurance_import_idempotency | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `import_type` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_import_idempotency | uk_insurance_import_idempotency | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 5 | `idempotency_key` | 待确认 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_import_idempotency | uk_insurance_import_idempotency | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `content_hash` | 内容哈希 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 结算包或业务内容的完整性摘要。 |
| 7 | `status` | 状态 | `varchar(32)` | 32 | 否 | `processing` | 否 | 否 | 否 | idx_insurance_import_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `total_count` | 数量 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 9 | `success_count` | 数量 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 10 | `failure_count` | 数量 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 11 | `error_json` | JSON 快照 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 12 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 13 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_import_status | 是 | 否 | — | 记录首次创建时间。 |
| 14 | `completed_at` | 时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_import_status` | `tenant_id, status, created_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_import_idempotency` | `tenant_id, import_type, idempotency_key` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_import_batch.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 104. 表：`rehealth_insurance_intervention` 保险干预参与表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_intervention` |
| 中文名称 | 保险干预参与表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存主体加入健康干预计划的状态与反馈时间。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 42 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_intervention_plan、联合唯一:uk_insurance_intervention_source_record | idx_insurance_intervention_status、idx_insurance_intervention_subject、uk_insurance_intervention_plan、uk_insurance_intervention_source_record | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_intervention_plan | idx_insurance_intervention_subject、uk_insurance_intervention_plan | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 4 | `plan_id` | 干预计划业务 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_intervention_plan | uk_insurance_intervention_plan | 否 | 否/待确认 | — | 保险干预参与记录中的稳定计划业务标识。 |
| 5 | `source_plan_id` | 来源干预计划 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 逻辑引用 ReHealth 原始干预计划。 |
| 6 | `consent_id` | 授权记录 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 逻辑关联允许当前保险干预使用数据的授权记录。 |
| 7 | `status` | 状态 | `varchar(32)` | 32 | 否 | `enrolled` | 否 | 否 | 否 | idx_insurance_intervention_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `enrolled_at` | 加入干预时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_intervention_status | 否 | 否 | — | 主体加入保险健康干预计划的时间。 |
| 9 | `ended_at` | 结束时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗结束时间。 |
| 10 | `last_feedback_at` | 最近反馈时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主体最近一次干预反馈时间。 |
| 11 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `rehealth_app` | 否 | 否 | 联合唯一:uk_insurance_intervention_source_record | uk_insurance_intervention_source_record | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 12 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_intervention_source_record | uk_insurance_intervention_source_record | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 13 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 14 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 15 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_intervention_subject | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_intervention_status` | `tenant_id, status, enrolled_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_intervention_subject` | `tenant_id, subject_ref, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_intervention_plan` | `tenant_id, subject_ref, plan_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_intervention_source_record` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_intervention.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_intervention.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存主体加入健康干预计划的状态与反馈时间。

## 105. 表：`rehealth_insurance_intervention_action` 保险人工干预行动表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_intervention_action` |
| 中文名称 | 保险人工干预行动表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存租户和负责人范围内的随访、任务与人工复核行动及完成结果。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 116 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_action_request | idx_insurance_action_assignee、idx_insurance_action_subject、uk_insurance_action_request | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_action_subject | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 4 | `plan_id` | 干预计划业务 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 保险干预参与记录中的稳定计划业务标识。 |
| 5 | `action_type` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `title` | 标题 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前会话、研究、报告或业务对象的展示标题。 |
| 7 | `content` | 消息内容 | `varchar(2000)` | 2000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存当前健康问答消息正文。 |
| 8 | `assignee_user_id` | 待确认 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_action_assignee | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `status` | 状态 | `varchar(32)` | 32 | 否 | `pending` | 否 | 否 | 否 | idx_insurance_action_assignee | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 10 | `due_at` | 时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_action_assignee | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 11 | `completed_at` | 时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 12 | `result_json` | 研究结果 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存完整版本化研究结果。 |
| 13 | `created_by` | 创建用户 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 14 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_action_request | uk_insurance_action_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 15 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 16 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_action_subject | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_action_assignee` | `tenant_id, assignee_user_id, status, due_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_action_subject` | `tenant_id, subject_ref, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_action_request` | `tenant_id, request_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_intervention_action.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_intervention_action.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存租户和负责人范围内的随访、任务与人工复核行动及完成结果。

## 106. 表：`rehealth_insurance_intervention_feedback` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_intervention_feedback` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 108 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_feedback_source | idx_insurance_feedback_binding、idx_insurance_feedback_item_period、idx_insurance_feedback_subject、uk_insurance_feedback_source | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `binding_id` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_feedback_binding、idx_insurance_feedback_item_period | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_feedback_subject | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 5 | `intervention_id` | 干预行动 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 标识用户反馈所针对的具体干预行动。 |
| 6 | `plan_item_id` | 待确认 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_feedback_item_period | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `feedback_type` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `occurred_at` | 时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_feedback_binding、idx_insurance_feedback_item_period、idx_insurance_feedback_subject | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 9 | `completion_rate` | 待确认 | `decimal(8,6)` | 8,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `adherence_score` | 待确认 | `decimal(8,6)` | 8,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `expected_count` | 数量 | `decimal(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 12 | `completed_count` | 数量 | `decimal(10,3)` | 10,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 13 | `verification_type` | 待确认 | `varchar(32)` | 32 | 否 | `self_report` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 14 | `calculation_version` | 版本 | `varchar(64)` | 64 | 否 | `legacy-client-score` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存对应对象、契约或算法版本；具体版本规则待确认。 |
| 15 | `outcome_summary_json` | JSON 快照 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 16 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `rehealth_app` | 否 | 否 | 联合唯一:uk_insurance_feedback_source | uk_insurance_feedback_source | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 17 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_feedback_source | uk_insurance_feedback_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 18 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_feedback_binding` | `tenant_id, binding_id, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_insurance_feedback_item_period` | `tenant_id, binding_id, plan_item_id, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_insurance_feedback_subject` | `tenant_id, subject_ref, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_feedback_source` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_intervention_feedback.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_intervention_feedback.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 107. 表：`rehealth_insurance_plan_binding` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_plan_binding` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 36 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_plan_binding、联合唯一:uk_insurance_plan_binding_source | idx_insurance_plan_binding_status、uk_insurance_plan_binding、uk_insurance_plan_binding_source | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_plan_binding | uk_insurance_plan_binding | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 4 | `policy_id` | 保单记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_plan_binding | uk_insurance_plan_binding | 否 | 否/待确认 | — | 逻辑关联 rehealth_insurance_policy.id。 |
| 5 | `plan_id` | 干预计划业务 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_plan_binding | uk_insurance_plan_binding | 否 | 否/待确认 | — | 保险干预参与记录中的稳定计划业务标识。 |
| 6 | `consent_id` | 授权记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 逻辑关联允许当前保险干预使用数据的授权记录。 |
| 7 | `status` | 状态 | `varchar(32)` | 32 | 否 | `active` | 否 | 否 | 否 | idx_insurance_plan_binding_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `bound_at` | 绑定时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户与设备绑定建立的时间。 |
| 9 | `unbound_at` | 时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 10 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `rehealth_app` | 否 | 否 | 联合唯一:uk_insurance_plan_binding_source | uk_insurance_plan_binding_source | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 11 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_plan_binding_source | uk_insurance_plan_binding_source | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 12 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 13 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 14 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_plan_binding_status | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_plan_binding_status` | `tenant_id, status, updated_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_plan_binding` | `tenant_id, subject_ref, policy_id, plan_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_plan_binding_source` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_plan_binding.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_plan_binding.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 108. 表：`rehealth_insurance_policy` 保险保单表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_policy` |
| 中文名称 | 保险保单表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存租户内保单、产品、金额、期限和被保主体引用。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 49 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_policy_source_record、联合唯一:uk_insurance_policy_tenant_no | idx_insurance_policy_effective、idx_insurance_policy_subject_status、uk_insurance_policy_source_record、uk_insurance_policy_tenant_no | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `policy_no` | 保单号 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_policy_tenant_no | uk_insurance_policy_tenant_no | 否 | 否 | — | 租户内唯一的保单业务编号。 |
| 4 | `product_code` | 产品编码 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 选择设备 Provider 和能力目录的稳定产品编码。 |
| 5 | `product_name` | 产品名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险或设备产品的可展示名称。 |
| 6 | `policy_type` | 保单类型 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险产品的保单类型；完整枚举待保险业务确认。 |
| 7 | `policyholder_subject_ref` | 投保主体引用 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 投保人的去标识主体引用。 |
| 8 | `insured_subject_ref` | 被保主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_policy_subject_status | 否 | 否 | — | 被保险人的去标识主体引用。 |
| 9 | `coverage_amount` | 保额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单总保障金额。 |
| 10 | `premium_amount` | 保费 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单保费金额。 |
| 11 | `deductible_amount` | 免赔额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单或保障责任的免赔金额。 |
| 12 | `waiting_period_days` | 等待期天数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保单责任生效前的等待期天数。 |
| 13 | `effective_on` | 生效日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_policy_effective | 否 | 否 | — | 保单或保障责任开始生效日期。 |
| 14 | `expires_on` | 到期日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_policy_effective | 否 | 否 | — | 保单或保障责任到期日期。 |
| 15 | `status` | 状态 | `varchar(32)` | 32 | 否 | `active` | 否 | 否 | 否 | idx_insurance_policy_subject_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 16 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_policy_source_record | uk_insurance_policy_source_record | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 17 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_policy_source_record | uk_insurance_policy_source_record | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 18 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 19 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 20 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_policy_effective` | `tenant_id, effective_on, expires_on` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_insurance_policy_subject_status` | `tenant_id, insured_subject_ref, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_policy_source_record` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_policy_tenant_no` | `tenant_id, policy_no` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_policy.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存租户内保单、产品、金额、期限和被保主体引用。

## 109. 表：`rehealth_insurance_rwe_report` 真实世界证据报告表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_rwe_report` |
| 中文名称 | 真实世界证据报告表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存版本化 RWE 报告及审批证据。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_rwe_report_no、联合唯一:uk_insurance_rwe_report_version | idx_insurance_rwe_report_status、uk_insurance_rwe_report_no、uk_insurance_rwe_report_version | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `report_no` | 报告编号 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_rwe_report_no | uk_insurance_rwe_report_no | 否 | 否 | — | 租户内唯一的 RWE 报告编号。 |
| 4 | `study_id` | 研究记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_rwe_report_version | uk_insurance_rwe_report_version | 否 | 逻辑→rehealth_insurance_study.id | — | 逻辑关联 rehealth_insurance_study.id。 |
| 5 | `report_type` | 报告类型 | `varchar(64)` | 64 | 否 | `rwe` | 否 | 否 | 否 | 否 | 否 | 否 | — | 报告业务类型，当前默认 rwe。 |
| 6 | `report_version` | 报告版本 | `int` | 10,0 | 否 | `1` | 否 | 否 | 联合唯一:uk_insurance_rwe_report_version | uk_insurance_rwe_report_version | 否 | 否 | — | 同一研究下报告的递增版本。 |
| 7 | `title` | 标题 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前会话、研究、报告或业务对象的展示标题。 |
| 8 | `period_start` | 研究/报告起始日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究或报告纳入数据的开始日期。 |
| 9 | `period_end` | 研究/报告结束日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究或报告纳入数据的结束日期。 |
| 10 | `status` | 状态 | `varchar(32)` | 32 | 否 | `draft` | 否 | 否 | 否 | idx_insurance_rwe_report_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 11 | `evidence_hash` | 证据哈希 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 授权、报告或结算证据内容的完整性摘要。 |
| 12 | `report_json` | 报告内容 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化结构化 RWE 报告。 |
| 13 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 14 | `submitted_at` | 提交时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 理赔、报告或审批流程提交时间。 |
| 15 | `approved_by` | 审批用户 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批准当前研究、报告或结算包的内部用户。 |
| 16 | `approved_at` | 审批时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 审批完成时间。 |
| 17 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 18 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_rwe_report_status | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_rwe_report_status` | `tenant_id, status, updated_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_rwe_report_no` | `tenant_id, report_no` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_rwe_report_version` | `tenant_id, study_id, report_version` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_rwe_report.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_rwe_report.(study_id)` → `rehealth_insurance_study.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存版本化 RWE 报告及审批证据。

## 110. 表：`rehealth_insurance_settlement_approval` 保险结算审批记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_settlement_approval` |
| 中文名称 | 保险结算审批记录表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存结算包的审批动作、意见和请求幂等键。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_settlement_approval_request | idx_insurance_settlement_approval_package、uk_insurance_settlement_approval_request | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `package_id` | 结算包 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_settlement_approval_request | idx_insurance_settlement_approval_package、uk_insurance_settlement_approval_request | 否 | 逻辑→rehealth_insurance_settlement_package.id | — | 逻辑关联 rehealth_insurance_settlement_package.id。 |
| 4 | `action` | 操作动作 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存审批或审计动作；具体枚举由对应业务服务定义。 |
| 5 | `comment` | 审批/操作意见 | `varchar(2000)` | 2000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存审批或操作人员提交的说明文本。 |
| 6 | `actor_user_id` | 操作用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 执行审批或审计动作的内部用户。 |
| 7 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_settlement_approval_request | uk_insurance_settlement_approval_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 8 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_settlement_approval_package | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_settlement_approval_package` | `tenant_id, package_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_settlement_approval_request` | `tenant_id, package_id, request_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_settlement_approval.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_settlement_approval.(package_id)` → `rehealth_insurance_settlement_package.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存结算包的审批动作、意见和请求幂等键。

## 111. 表：`rehealth_insurance_settlement_package` 保险结算包表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_settlement_package` |
| 中文名称 | 保险结算包表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存由研究和报告形成的版本化结算证据包。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_settlement_package_no、联合唯一:uk_insurance_settlement_package_version | idx_insurance_settlement_status、uk_insurance_settlement_package_no、uk_insurance_settlement_package_version | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `package_no` | 结算包编号 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_settlement_package_no | uk_insurance_settlement_package_no | 否 | 否 | — | 租户内唯一的结算证据包编号。 |
| 4 | `study_id` | 研究记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_settlement_package_version | uk_insurance_settlement_package_version | 否 | 逻辑→rehealth_insurance_study.id | — | 逻辑关联 rehealth_insurance_study.id。 |
| 5 | `report_id` | 报告 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 逻辑关联形成结算包的 RWE 报告。 |
| 6 | `package_version` | 结算包版本 | `int` | 10,0 | 否 | `1` | 否 | 否 | 联合唯一:uk_insurance_settlement_package_version | uk_insurance_settlement_package_version | 否 | 否 | — | 同一研究下结算证据包的递增版本。 |
| 7 | `status` | 状态 | `varchar(32)` | 32 | 否 | `draft` | 否 | 否 | 否 | idx_insurance_settlement_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `currency` | 币种 | `char(3)` | 3 | 否 | `CNY` | 否 | 否 | 否 | 否 | 否 | 否 | — | 金额字段采用的三字符货币代码，默认 CNY。 |
| 9 | `estimated_savings` | 预计节省金额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 基于已批准研究口径估算的节省金额。 |
| 10 | `approved_amount` | 批准金额 | `decimal(18,2)` | 18,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 审核或结算批准的金额。 |
| 11 | `snapshot_hash` | 快照哈希 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究或结算证据快照的内容完整性摘要。 |
| 12 | `evidence_manifest_json` | 证据清单 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结算包包含的证据引用和哈希清单。 |
| 13 | `package_json` | 结算包内容 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存完整版本化结算内容。 |
| 14 | `content_hash` | 内容哈希 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 结算包或业务内容的完整性摘要。 |
| 15 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 16 | `approved_by` | 审批用户 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批准当前研究、报告或结算包的内部用户。 |
| 17 | `approved_at` | 审批时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 审批完成时间。 |
| 18 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 19 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_settlement_status | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_settlement_status` | `tenant_id, status, updated_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_settlement_package_no` | `tenant_id, package_no` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_settlement_package_version` | `tenant_id, study_id, package_version` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_settlement_package.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_settlement_package.(study_id)` → `rehealth_insurance_study.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存由研究和报告形成的版本化结算证据包。

## 112. 表：`rehealth_insurance_study` 保险研究定义表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_study` |
| 中文名称 | 保险研究定义表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存真实世界研究人群、干预、结局规则和审批状态。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_tenant_no | idx_insurance_study_period、idx_insurance_study_status、uk_insurance_study_tenant_no | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `study_no` | 研究编号 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_tenant_no | uk_insurance_study_tenant_no | 否 | 否 | — | 租户内唯一的保险真实世界研究编号。 |
| 4 | `title` | 标题 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前会话、研究、报告或业务对象的展示标题。 |
| 5 | `period_start` | 研究/报告起始日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_study_period | 否 | 否 | — | 研究或报告纳入数据的开始日期。 |
| 6 | `period_end` | 研究/报告结束日期 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_insurance_study_period | 否 | 否 | — | 研究或报告纳入数据的结束日期。 |
| 7 | `population_rule_json` | 研究人群规则 | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 定义研究人群纳入排除条件的版本化 JSON。 |
| 8 | `intervention_rule_json` | 研究干预规则 | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 定义研究处理/干预暴露的版本化 JSON。 |
| 9 | `outcome_rule_json` | 研究结局规则 | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 定义研究结局计算口径的版本化 JSON。 |
| 10 | `methodology` | 研究方法 | `varchar(64)` | 64 | 否 | `psm` | 否 | 否 | 否 | 否 | 否 | 否 | — | 真实世界研究使用的方法，当前默认 psm。 |
| 11 | `status` | 状态 | `varchar(32)` | 32 | 否 | `draft` | 否 | 否 | 否 | idx_insurance_study_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 12 | `model_version` | 模型版本 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 13 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 14 | `approved_by` | 审批用户 ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批准当前研究、报告或结算包的内部用户。 |
| 15 | `approved_at` | 审批时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 审批完成时间。 |
| 16 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 17 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_study_status | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_study_period` | `tenant_id, period_start, period_end` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_insurance_study_status` | `tenant_id, status, updated_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_study_tenant_no` | `tenant_id, study_no` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_study.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存真实世界研究人群、干预、结局规则和审批状态。

## 113. 表：`rehealth_insurance_study_job` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_study_job` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_job_request | idx_insurance_study_job_snapshot、idx_insurance_study_job_status、uk_insurance_study_job_request | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `study_id` | 研究记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_job_request | uk_insurance_study_job_request | 否 | 否/待确认 | — | 逻辑关联 rehealth_insurance_study.id。 |
| 4 | `snapshot_id` | 快照记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_study_job_snapshot | 否 | 否/待确认 | — | 逻辑关联本业务域的快照主记录。 |
| 5 | `job_type` | 待确认 | `varchar(32)` | 32 | 否 | `psm` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `status` | 状态 | `varchar(32)` | 32 | 否 | `queued` | 否 | 否 | 否 | idx_insurance_study_job_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 7 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_job_request | uk_insurance_study_job_request | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 8 | `attempt` | 待确认 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `error_message` | 待确认 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `result_id` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 12 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_study_job_snapshot、idx_insurance_study_job_status | 是 | 否 | — | 记录首次创建时间。 |
| 13 | `started_at` | 开始时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 会话、活动或信号时间窗开始时间。 |
| 14 | `finished_at` | 时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 15 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_study_job_snapshot` | `tenant_id, snapshot_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_insurance_study_job_status` | `tenant_id, status, created_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_study_job_request` | `tenant_id, study_id, request_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_study_job.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 114. 表：`rehealth_insurance_study_member` 保险研究成员表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_study_member` |
| 中文名称 | 保险研究成员表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存研究快照中的去标识主体、队列分组和结局值。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 39 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_member | idx_insurance_snapshot_member_group、uk_insurance_snapshot_member | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `snapshot_id` | 快照记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_member | idx_insurance_snapshot_member_group、uk_insurance_snapshot_member | 否 | 逻辑→rehealth_insurance_study_snapshot.id | — | 逻辑关联本业务域的快照主记录。 |
| 4 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_member | uk_insurance_snapshot_member | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 5 | `cohort_group` | 队列分组 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_snapshot_member_group | 否 | 否 | — | 研究成员所属处理组或对照组。 |
| 6 | `baseline_risk` | 基线风险 | `decimal(10,6)` | 10,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究成员在干预前的基线风险值。 |
| 7 | `outcome_value` | 结局值 | `decimal(18,6)` | 18,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保险研究成员在既定结局定义下的观测结果值。 |
| 8 | `intervention_status` | 干预状态 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究成员在结局窗口内的干预状态。 |
| 9 | `covariate_json` | JSON 快照 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 10 | `source_row_hash` | 哈希值 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存不可逆摘要，用于完整性、幂等或去标识；具体算法待确认。 |
| 11 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_snapshot_member_group` | `tenant_id, snapshot_id, cohort_group` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_snapshot_member` | `tenant_id, snapshot_id, subject_ref` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_study_member.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_study_member.(snapshot_id)` → `rehealth_insurance_study_snapshot.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。
- `rehealth_insurance_study_member.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存研究快照中的去标识主体、队列分组和结局值。

## 115. 表：`rehealth_insurance_study_result` 保险研究结果表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_study_result` |
| 中文名称 | 保险研究结果表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存 PSM/真实世界研究估计、区间、平衡和成本结果。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_result_version | idx_insurance_study_result_status、uk_insurance_study_result_version | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `study_id` | 研究记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_result_version | idx_insurance_study_result_status、uk_insurance_study_result_version | 否 | 逻辑→rehealth_insurance_study.id | — | 逻辑关联 rehealth_insurance_study.id。 |
| 4 | `snapshot_id` | 快照记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 逻辑→rehealth_insurance_study_snapshot.id | — | 逻辑关联本业务域的快照主记录。 |
| 5 | `result_version` | 结果版本 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_study_result_version | uk_insurance_study_result_version | 否 | 否 | — | 同一研究结果的递增版本。 |
| 6 | `status` | 状态 | `varchar(32)` | 32 | 否 | `calculated` | 否 | 否 | 否 | idx_insurance_study_result_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 7 | `att_estimate` | ATT 估计值 | `decimal(18,8)` | 18,8 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 对已处理者平均处理效应的估计值。 |
| 8 | `ci_lower` | 区间下界 | `decimal(18,8)` | 18,8 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究估计区间的下界。 |
| 9 | `ci_upper` | 区间上界 | `decimal(18,8)` | 18,8 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究估计区间的上界。 |
| 10 | `matched_pairs` | 匹配对数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | PSM 等匹配方法最终形成的匹配样本对数。 |
| 11 | `balance_json` | 协变量平衡 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存匹配前后协变量平衡诊断。 |
| 12 | `cost_basis_json` | 成本口径 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存经济性或结算计算使用的成本口径。 |
| 13 | `model_version` | 模型版本 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 14 | `result_json` | 研究结果 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存完整版本化研究结果。 |
| 15 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 16 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_study_result_status | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_study_result_status` | `tenant_id, study_id, status, created_at` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_study_result_version` | `tenant_id, study_id, result_version` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_study_result.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_study_result.(study_id)` → `rehealth_insurance_study.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。
- `rehealth_insurance_study_result.(snapshot_id)` → `rehealth_insurance_study_snapshot.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存 PSM/真实世界研究估计、区间、平衡和成本结果。

## 116. 表：`rehealth_insurance_study_snapshot` 保险研究快照表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_study_snapshot` |
| 中文名称 | 保险研究快照表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存研究人群不可变快照、来源水位和内容哈希。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 2 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_hash、联合唯一:uk_insurance_snapshot_version | idx_insurance_snapshot_study、uk_insurance_snapshot_hash、uk_insurance_snapshot_version | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `study_id` | 研究记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_hash、联合唯一:uk_insurance_snapshot_version | idx_insurance_snapshot_study、uk_insurance_snapshot_hash、uk_insurance_snapshot_version | 否 | 逻辑→rehealth_insurance_study.id | — | 逻辑关联 rehealth_insurance_study.id。 |
| 4 | `snapshot_version` | 快照版本 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_version | uk_insurance_snapshot_version | 否 | 否 | — | 同一研究下不可变人群快照的递增版本。 |
| 5 | `snapshot_hash` | 快照哈希 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_snapshot_hash | uk_insurance_snapshot_hash | 否 | 否 | — | 研究或结算证据快照的内容完整性摘要。 |
| 6 | `source_watermark` | 来源水位 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 生成研究快照时上游数据的版本或时间水位。 |
| 7 | `cohort_total` | 队列总人数 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究快照中的去标识主体总数。 |
| 8 | `treated_total` | 处理组人数 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究快照中处理/干预组主体数。 |
| 9 | `control_total` | 对照组人数 | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 研究快照中对照组主体数。 |
| 10 | `source_summary_json` | 来源摘要 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存研究快照来源和覆盖情况的结构化摘要。 |
| 11 | `immutable` | 是否不可变 | `tinyint(1)` | 3,0 | 否 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识研究快照生成后是否禁止修改，默认 true。 |
| 12 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 13 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_snapshot_study | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_snapshot_study` | `tenant_id, study_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_snapshot_hash` | `tenant_id, study_id, snapshot_hash` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_snapshot_version` | `tenant_id, study_id, snapshot_version` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_study_snapshot.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_study_snapshot.(study_id)` → `rehealth_insurance_study.(id)`：逻辑外键；保险域逻辑外键，数据库未声明 FOREIGN KEY。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存研究人群不可变快照、来源水位和内容哈希。

## 117. 表：`rehealth_insurance_subject` 保险业务主体表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_subject` |
| 中文名称 | 保险业务主体表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 保存租户隔离、去标识化的保险主体与 ReHealth 用户映射。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 49 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_source_record、联合唯一:uk_insurance_subject_tenant_ref、联合唯一:uk_insurance_subject_tenant_user | idx_insurance_subject_tenant_status、idx_insurance_subject_tenant_updated、uk_insurance_subject_source_record、uk_insurance_subject_tenant_ref、uk_insurance_subject_tenant_user | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_tenant_ref | uk_insurance_subject_tenant_ref | 否 | 否 | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 4 | `rehealth_user_id` | ReHealth 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_tenant_user | uk_insurance_subject_tenant_user | 否 | 逻辑→sys_user.id | — | 保险主体映射到的内部认证用户 ID，逻辑关联 sys_user.id。 |
| 5 | `external_subject_ref_hash` | 外部主体引用摘要 | `char(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 外部系统主体标识的不可逆摘要。 |
| 6 | `enrollment_status` | 纳入状态 | `varchar(32)` | 32 | 否 | `active` | 否 | 否 | 否 | idx_insurance_subject_tenant_status | 否 | 否 | — | 保险主体在当前租户业务中的纳入状态。 |
| 7 | `consent_status` | 授权状态 | `varchar(32)` | 32 | 否 | `pending` | 否 | 否 | 否 | idx_insurance_subject_tenant_status | 否 | 否 | — | 保险主体当前授权状态；完整枚举由保险服务定义。 |
| 8 | `consent_version` | 授权版本 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主体同意的授权文本或协议版本。 |
| 9 | `consented_at` | 授权时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 主体完成当前授权的时间。 |
| 10 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `rehealth` | 否 | 否 | 联合唯一:uk_insurance_subject_source_record | uk_insurance_subject_source_record | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 11 | `source_record_id` | 来源记录 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_source_record | uk_insurance_subject_source_record | 否 | 否/待确认 | — | 上游数据源中的稳定记录标识，通常参与幂等唯一约束。 |
| 12 | `metadata_json` | 扩展元数据 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存版本化扩展信息；不是核心字段的唯一权威表示。 |
| 13 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 14 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_subject_tenant_updated | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_subject_tenant_status` | `tenant_id, enrollment_status, consent_status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_subject_tenant_updated` | `tenant_id, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_subject_source_record` | `tenant_id, source_system, source_record_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_subject_tenant_ref` | `tenant_id, subject_ref` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_insurance_subject_tenant_user` | `tenant_id, rehealth_user_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_subject.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_subject.(rehealth_user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存租户隔离、去标识化的保险主体与 ReHealth 用户映射。

## 118. 表：`rehealth_insurance_subject_manager` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_subject_manager` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 132 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_manager | idx_insurance_manager_subject、idx_insurance_subject_manager_department、uk_insurance_subject_manager | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `manager_user_id` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_manager | idx_insurance_manager_subject、uk_insurance_subject_manager | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `department_id` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_insurance_subject_manager_department | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `subject_ref` | 去标识保险主体引用 | `char(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_insurance_subject_manager | uk_insurance_subject_manager | 否 | 逻辑→rehealth_insurance_subject.subject_ref | — | 租户内稳定的去标识主体引用，不保存直接患者标识。 |
| 6 | `status` | 状态 | `varchar(16)` | 16 | 否 | `active` | 否 | 否 | 否 | idx_insurance_manager_subject、idx_insurance_subject_manager_department | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 7 | `source_system` | 来源系统 | `varchar(64)` | 64 | 否 | `LOCAL_INSURANCE_QA` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识记录来自哪个受信业务系统。 |
| 8 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 9 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_insurance_manager_subject` | `tenant_id, manager_user_id, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_insurance_subject_manager_department` | `tenant_id, department_id, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_subject_manager` | `tenant_id, manager_user_id, subject_ref` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_subject_manager.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。
- `rehealth_insurance_subject_manager.(subject_ref)` → `rehealth_insurance_subject.(subject_ref)`：逻辑外键；保险域去标识主体逻辑外键，需同时使用 tenant_id 限定。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 119. 表：`rehealth_insurance_tenant_profile` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_insurance_tenant_profile` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 保险业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（保险域） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | uk_insurance_tenant_profile_tenant | uk_insurance_tenant_profile_tenant | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `organization_name` | 待确认 | `varchar(200)` | 200 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `license_no` | 待确认 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `insurance_type` | 待确认 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `compliance_email` | 待确认 | `varchar(120)` | 120 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `regulatory_email` | 待确认 | `varchar(120)` | 120 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `data_retention_years` | 待确认 | `int` | 10,0 | 否 | `7` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `mask_sensitive_data` | 待确认 | `tinyint(1)` | 3,0 | 否 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `access_log_enabled` | 待确认 | `tinyint(1)` | 3,0 | 否 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `notification_config_json` | JSON 快照 | `json` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 12 | `version` | 版本 | `int` | 10,0 | 否 | `1` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录或配置版本；是否为乐观锁需结合实体 @Version 判断。 |
| 13 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 14 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_insurance_tenant_profile_tenant` | `tenant_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_insurance_tenant_profile.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 120. 表：`rehealth_intervention_contraindication` 干预禁忌表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_intervention_contraindication` |
| 中文名称 | 干预禁忌表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存某次干预计划包含的有序禁忌与安全限制。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 9 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `plan_record_id` | 干预计划记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_contraindication_order | uk_rehealth_contraindication_order | 否 | 物理→rehealth_intervention_plan.id | — | 物理关联 rehealth_intervention_plan.id。 |
| 3 | `item_value` | 回答/基线值 | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访谈明细中的类型化或文本值。 |
| 4 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_contraindication_order | uk_rehealth_contraindication_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_contraindication_order` | `plan_record_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_intervention_contraindication.(plan_record_id)` → `rehealth_intervention_plan.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存某次干预计划包含的有序禁忌与安全限制。

## 121. 表：`rehealth_intervention_feedback` 干预反馈表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_intervention_feedback` |
| 中文名称 | 干预反馈表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存用户对具体干预计划/行动的完成、跳过或不适用反馈。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 82 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_feedback_user_key | idx_feedback_user_created、uk_rehealth_feedback_user_key | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `plan_record_id` | 干预计划记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | fk_rehealth_feedback_plan | 否 | 物理→rehealth_intervention_plan.id | — | 物理关联 rehealth_intervention_plan.id。 |
| 4 | `plan_id` | 干预计划业务 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 保险干预参与记录中的稳定计划业务标识。 |
| 5 | `intervention_id` | 干预行动 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 标识用户反馈所针对的具体干预行动。 |
| 6 | `idempotency_key` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_feedback_user_key | uk_rehealth_feedback_user_key | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `status` | 状态 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `adherence` | 依从性 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户反馈中记录的干预执行或依从情况。 |
| 9 | `note` | 备注 | `varchar(2000)` | 2000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存用户或业务操作的可选补充说明。 |
| 10 | `checked_at` | 反馈打卡时间 | `datetime(3)` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户对干预行动提交反馈的时间。 |
| 11 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_feedback_user_created | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `fk_rehealth_feedback_plan` | `plan_record_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_feedback_user_created` | `user_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_feedback_user_key` | `user_id, idempotency_key` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_intervention_feedback.(plan_record_id)` → `rehealth_intervention_plan.(id)`：物理外键；ON DELETE NO ACTION。
- `rehealth_intervention_feedback.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存用户对具体干预计划/行动的完成、跳过或不适用反馈。

## 122. 表：`rehealth_intervention_plan` 健康干预计划表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_intervention_plan` |
| 中文名称 | 健康干预计划表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存基于权威画像、风险和设备行为上下文生成的结构化保守干预计划。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 44 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_plan_user_plan | idx_plan_user_generated、uk_rehealth_plan_user_plan | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `plan_id` | 干预计划业务 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_plan_user_plan | uk_rehealth_plan_user_plan | 否 | 否/待确认 | — | 保险干预参与记录中的稳定计划业务标识。 |
| 4 | `source_request_id` | 待确认 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `feature_schema_version` | 特征协议版本 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识特征向量遵循的字段协议版本。 |
| 6 | `model_version` | 模型版本 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 7 | `scorer_mode` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `is_mock` | 是否模拟数据 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 明确标识结果是否来自 Mock/合成路径；生产结果不得为真。 |
| 9 | `artifact_name` | 模型制品名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识产生结果时使用的已加载模型制品。 |
| 10 | `priority_intervention` | 优先干预摘要 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 结构化干预计划中优先级最高行动的摘要。 |
| 11 | `rationale` | 干预依据 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 解释干预行动与权威画像、风险或行为上下文之间的依据。 |
| 12 | `expected_impact` | 预期影响 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保守描述执行干预可能带来的健康行为影响，不构成疗效保证。 |
| 13 | `confidence` | 置信度 | `double` | 22 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 14 | `medical_disclaimer` | 医疗免责声明 | `varchar(2000)` | 2000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 声明建议仅供健康参考、不能替代医疗诊断或医生。 |
| 15 | `generated_at` | 生成时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_plan_user_generated | 否 | 否 | — | 计划或结果完成生成的时间。 |
| 16 | `response_json` | 响应证据 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存模型或 Provider 的版本化结构化响应快照。 |
| 17 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_plan_user_generated` | `user_id, generated_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_plan_user_plan` | `user_id, plan_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_intervention_plan.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存基于权威画像、风险和设备行为上下文生成的结构化保守干预计划。

## 123. 表：`rehealth_model_request_log` 模型请求审计表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_model_request_log` |
| 中文名称 | 模型请求审计表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 审计日志 |
| 业务作用 | 保存不含原始 PII/遥测的模型调用元数据、状态、耗时和错误码。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 681 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（日志/支持） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_model_request_user_created | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `request_id` | 请求幂等 ID | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 用于请求追踪与幂等控制，不能作为用户身份来源。 |
| 4 | `operation` | 操作名称 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存模型请求或业务审计的操作名称。 |
| 5 | `model_version` | 模型版本 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前模型输出的版本标识。 |
| 6 | `outcome` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `error_code` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `latency_ms` | 待确认 | `bigint` | 19,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_model_request_user_created | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_model_request_user_created` | `user_id, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_model_request_log.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存不含原始 PII/遥测的模型调用元数据、状态、耗时和错误码。

## 124. 表：`rehealth_patient_allergy` 患者过敏史表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_patient_allergy` |
| 中文名称 | 患者过敏史表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存健康档案下的有序过敏条目。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `profile_id` | 健康档案记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_allergy_order | uk_rehealth_allergy_order | 否 | 物理→rehealth_patient_profile.id | — | 物理关联 rehealth_patient_profile.id。 |
| 3 | `item_value` | 回答/基线值 | `varchar(512)` | 512 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访谈明细中的类型化或文本值。 |
| 4 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_allergy_order | uk_rehealth_allergy_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |
| 5 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_allergy_order` | `profile_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_patient_allergy.(profile_id)` → `rehealth_patient_profile.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存健康档案下的有序过敏条目。

## 125. 表：`rehealth_patient_diagnosis` 患者诊断史表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_patient_diagnosis` |
| 中文名称 | 患者诊断史表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存健康档案下的有序诊断史条目。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `profile_id` | 健康档案记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_diagnosis_order | uk_rehealth_diagnosis_order | 否 | 物理→rehealth_patient_profile.id | — | 物理关联 rehealth_patient_profile.id。 |
| 3 | `item_value` | 回答/基线值 | `varchar(512)` | 512 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访谈明细中的类型化或文本值。 |
| 4 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_diagnosis_order | uk_rehealth_diagnosis_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |
| 5 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_diagnosis_order` | `profile_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_patient_diagnosis.(profile_id)` → `rehealth_patient_profile.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存健康档案下的有序诊断史条目。

## 126. 表：`rehealth_patient_medication` 患者用药史表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_patient_medication` |
| 中文名称 | 患者用药史表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存健康档案下的有序用药条目。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `profile_id` | 健康档案记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_medication_order | uk_rehealth_medication_order | 否 | 物理→rehealth_patient_profile.id | — | 物理关联 rehealth_patient_profile.id。 |
| 3 | `item_value` | 回答/基线值 | `varchar(512)` | 512 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访谈明细中的类型化或文本值。 |
| 4 | `sort_order` | 排序序号 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rehealth_medication_order | uk_rehealth_medication_order | 否 | 否 | — | 控制同一主记录下明细的稳定展示和处理顺序。 |
| 5 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_medication_order` | `profile_id, sort_order` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_patient_medication.(profile_id)` → `rehealth_patient_profile.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存健康档案下的有序用药条目。

## 127. 表：`rehealth_patient_profile` 患者健康档案表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_patient_profile` |
| 中文名称 | 患者健康档案表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存认证用户的类型化健康档案、BMI 和乐观锁版本。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 52 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | uk_rehealth_profile_user | uk_rehealth_profile_user | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `name` | 名称 | `varchar(128)` | 128 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前业务对象的名称；是否属于直接身份信息取决于所在表。 |
| 4 | `gender` | 性别 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 健康档案中用户明确提供或经访谈确认的性别；完整枚举待产品契约确认。 |
| 5 | `age` | 年龄 | `smallint` | 5,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 健康档案中用户明确提供或经访谈确认的年龄。 |
| 6 | `height_cm` | 身高 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 健康档案中的身高，单位厘米。 |
| 7 | `weight_kg` | 体重 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 健康档案中的体重，单位千克。 |
| 8 | `bmi` | 体质指数 BMI | `decimal(5,2)` | 5,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 服务端根据档案身高和体重计算的 BMI。 |
| 9 | `family_history` | 家族史标志 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识健康档案是否记录相关家族病史；空值表示未确认。 |
| 10 | `smoking` | 吸烟标志 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识健康档案中的吸烟情况；空值表示未确认。 |
| 11 | `drinking` | 饮酒标志 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识健康档案中的饮酒情况；空值表示未确认。 |
| 12 | `diabetes_history` | 糖尿病史标志 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识健康档案中是否有糖尿病史；空值表示未确认。 |
| 13 | `hypertension_history` | 高血压史标志 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识健康档案中是否有高血压史；空值表示未确认。 |
| 14 | `profile_version` | 档案版本号 | `bigint` | 19,0 | 否 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | — | 由 Repository 显式维护的乐观锁版本，更新档案时用于冲突检测。 |
| 15 | `profile_json` | JSON 快照 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 16 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 17 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rehealth_profile_user` | `user_id` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_patient_profile.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `gender`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存认证用户的类型化健康档案、BMI 和乐观锁版本。

## 128. 表：`rehealth_rdi_contribution` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_rdi_contribution` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 712 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `snapshot_id` | 快照记录 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rdi_contribution_snapshot_factor | idx_rdi_contribution_snapshot_points、uk_rdi_contribution_snapshot_factor | 否 | 物理→rehealth_rdi_daily_snapshot.id | — | 逻辑关联本业务域的快照主记录。 |
| 3 | `factor_code` | 因素编码 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rdi_contribution_snapshot_factor | uk_rdi_contribution_snapshot_factor | 否 | 否 | — | RDI 因素的稳定代码。 |
| 4 | `domain_code` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `source_code` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `current_value` | 当前值 | `decimal(16,6)` | 16,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前因素或指标参与计算时使用的实际值。 |
| 7 | `baseline_value` | 基线值 | `decimal(16,6)` | 16,6 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用于与当前值比较的个人或研究基线值。 |
| 8 | `unit` | 计量单位 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 说明数值字段采用的计量单位，解释数值时必须同时读取。 |
| 9 | `raw_points` | 原始贡献分 | `decimal(10,6)` | 10,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 乘入置信度等修正前的因素贡献分。 |
| 10 | `confidence` | 置信度 | `decimal(8,6)` | 8,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前特征、因素、识别结果或计划的可信程度。 |
| 11 | `final_points` | 最终贡献分 | `decimal(10,6)` | 10,6 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rdi_contribution_snapshot_points | 否 | 否 | — | 考虑置信度和规则修正后实际使用的贡献分。 |
| 12 | `source_factor_id` | 来源因素 ID | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 关联产生当前贡献的稳定来源因素。 |
| 13 | `algorithm_version` | 算法版本 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 14 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rdi_contribution_snapshot_points` | `snapshot_id, final_points` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rdi_contribution_snapshot_factor` | `snapshot_id, factor_code` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_rdi_contribution.(snapshot_id)` → `rehealth_rdi_daily_snapshot.(id)`：物理外键；ON DELETE CASCADE。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 129. 表：`rehealth_rdi_daily_snapshot` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_rdi_daily_snapshot` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 239 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rdi_daily_user_date | idx_rdi_daily_user_updated、uk_rdi_daily_user_date | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `scored_on` | 评分日期 | `date` | 不适用 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rdi_daily_user_date | idx_rdi_daily_user_updated、uk_rdi_daily_user_date | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 4 | `raw_score` | 原始分数 | `decimal(8,4)` | 8,4 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 平滑或展示转换前的当日算法分数。 |
| 5 | `display_score` | 展示分数 | `decimal(8,4)` | 8,4 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经过规定平滑后用于产品展示的分数。 |
| 6 | `data_confidence` | 数据可信度 | `decimal(8,6)` | 8,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 算法对当前输入覆盖和质量的综合可信度。 |
| 7 | `status` | 状态 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `is_mock` | 是否模拟数据 | `tinyint(1)` | 3,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 明确标识结果是否来自 Mock/合成路径；生产结果不得为真。 |
| 9 | `algorithm_version` | 算法版本 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 10 | `calculation_source` | 计算来源 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识当前 RHI 快照由哪个受控计算路径产生。 |
| 11 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 12 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rdi_daily_user_updated | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rdi_daily_user_updated` | `user_id, scored_on, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rdi_daily_user_date` | `user_id, scored_on` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_rdi_daily_snapshot.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 130. 表：`rehealth_rhi_daily_snapshot` 云端 RHI 每日聚合快照表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_rhi_daily_snapshot` |
| 中文名称 | 云端 RHI 每日聚合快照表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存认证用户从 App 上传的日级 RHI 分数、领域、特征与质量聚合快照；不保存原始遥测。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 329 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rhi_daily_user_date | idx_rhi_daily_user_updated、uk_rhi_daily_user_date | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 3 | `scored_on` | 评分日期 | `date` | 不适用 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uk_rhi_daily_user_date | idx_rhi_daily_user_updated、uk_rhi_daily_user_date | 否 | 否 | — | 评分所属本地自然日，使用 ISO-8601 日期。 |
| 4 | `raw_score` | 原始分数 | `decimal(8,4)` | 8,4 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 平滑或展示转换前的当日算法分数。 |
| 5 | `display_score` | 展示分数 | `decimal(8,4)` | 8,4 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经过规定平滑后用于产品展示的分数。 |
| 6 | `data_confidence` | 数据可信度 | `decimal(8,6)` | 8,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 算法对当前输入覆盖和质量的综合可信度。 |
| 7 | `status` | 状态 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 8 | `product_tier` | 产品数据层级 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | RHI 根据当前可用证据确定的 LITE/STANDARD/CLINICAL 数据层级。 |
| 9 | `available_days` | 有效天数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 评分回看窗口内具有可用证据的天数。 |
| 10 | `available_feature_count` | 可用特征数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 本次评分实际提取到的有效特征数量。 |
| 11 | `smoothing_alpha` | 平滑系数 | `decimal(8,6)` | 8,6 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 原始分与历史展示分合并时使用的平滑参数。 |
| 12 | `algorithm_version` | 算法版本 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产生当前规则或算法结果的版本标识。 |
| 13 | `calculation_source` | 计算来源 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识当前 RHI 快照由哪个受控计算路径产生。 |
| 14 | `domains_json` | JSON 快照 | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 15 | `features_json` | JSON 快照 | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存结构化 JSON；具体对象语义需结合本表用途和版本字段确认。 |
| 16 | `quality_json` | 特征质量 JSON | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存特征缺失、质量和来源等版本化元数据。 |
| 17 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 18 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rhi_daily_user_updated | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rhi_daily_user_updated` | `user_id, scored_on, updated_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_rhi_daily_user_date` | `user_id, scored_on` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `rehealth_rhi_daily_snapshot.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存认证用户从 App 上传的日级 RHI 分数、领域、特征与质量聚合快照；不保存原始遥测。

## 131. 表：`rehealth_rhi_manual_health_input` 云端 RHI 手工输入表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_rhi_manual_health_input` |
| 中文名称 | 云端 RHI 手工输入表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存认证用户 Room-first 手工健康输入的云端副本，并按 updated_at 合并。 |
| 主键 | `user_id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 15 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `user_id` | 用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 逻辑→sys_user.id | — | 当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。 |
| 2 | `sedentary_hours_per_day` | 日均久坐时长 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的日均久坐小时数。 |
| 3 | `waist_circumference_cm` | 腰围 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的腰围，单位厘米。 |
| 4 | `vo2_max_ml_kg_min` | 最大摄氧量 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 正式 VO2max，单位 ml/kg/min。 |
| 5 | `hba1c_percent` | 糖化血红蛋白 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 HbA1c 百分比。 |
| 6 | `egfr_ml_min_1_73m2` | 估算肾小球滤过率 | `decimal(7,2)` | 7,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 eGFR，单位 ml/min/1.73m²。 |
| 7 | `cuff_sbp_7d_mean` | 7 日袖带收缩压均值 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经确认上臂袖带测量的 3–7 日收缩压均值。 |
| 8 | `cuff_dbp_7d_mean` | 7 日袖带舒张压均值 | `decimal(6,2)` | 6,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经确认上臂袖带测量的 3–7 日舒张压均值。 |
| 9 | `cuff_valid_days` | 袖带有效天数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 计算袖带血压均值时包含的有效自然日数。 |
| 10 | `cuff_confirmed` | 袖带血压是否确认 | `tinyint(1)` | 3,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 只有用户确认且满足规则的上臂袖带血压才进入正式特征。 |
| 11 | `fasting_glucose_mmol_l` | 空腹血糖 | `decimal(7,3)` | 7,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的空腹血糖，单位 mmol/L。 |
| 12 | `total_cholesterol_mmol_l` | 总胆固醇 | `decimal(7,3)` | 7,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的总胆固醇，单位 mmol/L。 |
| 13 | `ldl_mmol_l` | 低密度脂蛋白胆固醇 | `decimal(7,3)` | 7,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 LDL-C，单位 mmol/L。 |
| 14 | `hdl_mmol_l` | 高密度脂蛋白胆固醇 | `decimal(7,3)` | 7,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的 HDL-C，单位 mmol/L。 |
| 15 | `triglycerides_mmol_l` | 甘油三酯 | `decimal(7,3)` | 7,3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户确认的甘油三酯，单位 mmol/L。 |
| 16 | `lab_confirmed` | 化验是否确认 | `tinyint(1)` | 3,0 | 否 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | — | 只有用户确认且带日期的医院化验值才进入正式特征。 |
| 17 | `lab_recorded_at` | 化验日期时间 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 经确认医院化验报告的记录时间。 |
| 18 | `client_updated_at` | 时间 | `bigint` | 19,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 19 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |
| 20 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rhi_manual_updated_at | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rhi_manual_updated_at` | `updated_at` | 普通索引 | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `user_id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_rhi_manual_health_input.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存认证用户 Room-first 手工健康输入的云端副本，并按 updated_at 合并。

## 132. 表：`rehealth_schema_migration` ReHealth 迁移版本表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_schema_migration` |
| 中文名称 | ReHealth 迁移版本表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 迁移元数据 |
| 业务作用 | 记录 ReHealth 自定义软件库迁移版本；不是业务数据。 |
| 主键 | `version` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 23 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否（迁移元数据） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `version` | 版本 | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 记录或配置版本；是否为乐观锁需结合实体 @Version 判断。 |
| 2 | `applied_at` | 时间 | `timestamp(3)` | 不适用 | 否 | `CURRENT_TIMESTAMP(3)` | 否 | 否 | 否 | 否 | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `version` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

记录 ReHealth 自定义软件库迁移版本；不是业务数据。

## 133. 表：`rehealth_telemetry_event_projection` 遥测事件运营投影表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_telemetry_event_projection` |
| 中文名称 | 遥测事件运营投影表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 运营投影 |
| 业务作用 | 保存 Kafka 遥测生命周期事件的隐私安全运营投影。 |
| 主键 | `event_id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 9 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `event_id` | 事件 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否/待确认 | — | 标识遥测或业务事件；具体关联以物理外键或事件契约为准。 |
| 2 | `event_type` | 事件类型 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标识质量、Outbox、归因或审计事件的业务类型。 |
| 3 | `schema_id` | 待确认 | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `batch_id` | 客户端批次 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 客户端生成的稳定遥测批次业务键，重试时保持不变。 |
| 5 | `tenant_ref` | 待确认 | `varchar(160)` | 160 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_telemetry_projection_device_time、idx_rehealth_telemetry_projection_tenant_time | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `user_ref` | 待确认 | `varchar(160)` | 160 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `device_ref` | 待确认 | `varchar(160)` | 160 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_telemetry_projection_device_time | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `record_count` | 记录总数 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 批次中全部规范化记录数量。 |
| 9 | `persistence_status` | 待确认 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `quality_status` | 待确认 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 11 | `occurred_at` | 时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_telemetry_projection_device_time、idx_rehealth_telemetry_projection_tenant_time | 否 | 否 | — | 该字段记录的具体业务事件时间待确认。 |
| 12 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_telemetry_projection_device_time` | `tenant_ref, device_ref, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_rehealth_telemetry_projection_tenant_time` | `tenant_ref, occurred_at` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `event_id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存 Kafka 遥测生命周期事件的隐私安全运营投影。

## 134. 表：`rehealth_telemetry_quality_case` 遥测质量工单表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_telemetry_quality_case` |
| 中文名称 | 遥测质量工单表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 运营投影 |
| 业务作用 | 保存由遥测质量事件派生的运营质量工单。 |
| 主键 | `event_id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `event_id` | 事件 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 物理→rehealth_telemetry_event_projection.event_id | — | 标识遥测或业务事件；具体关联以物理外键或事件契约为准。 |
| 2 | `batch_id` | 客户端批次 ID | `varchar(128)` | 128 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 客户端生成的稳定遥测批次业务键，重试时保持不变。 |
| 3 | `tenant_ref` | 待确认 | `varchar(160)` | 160 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_quality_case_tenant_time | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `device_ref` | 待确认 | `varchar(160)` | 160 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `accepted_count` | 数量 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 6 | `rejected_count` | 数量 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 当前表业务对象的计数值；具体计数口径待确认。 |
| 7 | `quality_status` | 待确认 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_rehealth_quality_case_tenant_time | 是 | 否 | — | 记录首次创建时间。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_rehealth_quality_case_tenant_time` | `tenant_ref, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `PRIMARY` | `event_id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_telemetry_quality_case.(event_id)` → `rehealth_telemetry_event_projection.(event_id)`：物理外键；ON DELETE NO ACTION。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

保存由遥测质量事件派生的运营质量工单。

## 135. 表：`rehealth_website_record` 官网业务记录表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rehealth_website_record` |
| 中文名称 | 官网业务记录表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | ReHealth 核心业务 |
| 业务作用 | 保存官网侧按租户隔离的结构化业务记录；具体记录类型由业务代码定义。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | idx_website_record_tenant_resource_created、idx_website_record_tenant_status | 是 | 逻辑→sys_tenant.id | — | 用于多租户数据隔离；通常逻辑关联 sys_tenant.id。 |
| 3 | `resource_type` | 资源类型 | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_website_record_tenant_resource_created、idx_website_record_tenant_status | 否 | 否 | — | 保险审计事件所操作资源的类型。 |
| 4 | `status` | 状态 | `varchar(32)` | 32 | 否 | `active` | 否 | 否 | 否 | idx_website_record_tenant_status | 是 | 否 | 具体枚举值待确认 | 状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。 |
| 5 | `payload_json` | 载荷 JSON | `longtext` | 4294967295 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。 |
| 6 | `created_by` | 创建用户 ID | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建当前研究、报告、结算包或快照的内部用户。 |
| 7 | `created_at` | 创建时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | idx_website_record_tenant_resource_created | 是 | 否 | — | 记录首次创建时间。 |
| 8 | `updated_at` | 更新时间 | `datetime(3)` | 不适用 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 记录最后更新时间；部分表用于客户端与服务端新旧副本合并。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_website_record_tenant_resource_created` | `tenant_id, resource_type, created_at` | 普通索引（联合） | 支持按业务作用域和时间范围查询或排序。 |
| `idx_website_record_tenant_status` | `tenant_id, resource_type, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `rehealth_website_record.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态/类型类字段，完整枚举值待确认。

### 业务说明

保存官网侧按租户隔离的结构化业务记录；具体记录类型由业务代码定义。

## 136. 表：`rep_demo_dxtj` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rep_demo_dxtj` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 24 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `name` | 姓名 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 姓名 |
| 3 | `gtime` | 雇佣日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 雇佣日期 |
| 4 | `update_by` | 职务 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 职务 |
| 5 | `jphone` | 家庭电话 | `varchar(125)` | 125 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 家庭电话 |
| 6 | `birth` | 出生日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 出生日期 |
| 7 | `hukou` | 户口所在地 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 户口所在地 |
| 8 | `laddress` | 联系地址 | `varchar(125)` | 125 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 联系地址 |
| 9 | `jperson` | 紧急联系人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 紧急联系人 |
| 10 | `sex` | xingbie | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | xingbie |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 137. 表：`rep_demo_employee` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rep_demo_employee` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 2 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(10)` | 10 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `num` | 编号 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 编号 |
| 3 | `name` | 姓名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 姓名 |
| 4 | `sex` | 性别 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 性别 |
| 5 | `birthday` | 出生日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 出生日期 |
| 6 | `nation` | 民族 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 民族 |
| 7 | `political` | 政治面貌 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 政治面貌 |
| 8 | `native_place` | 籍贯 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 籍贯 |
| 9 | `height` | 身高 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 身高 |
| 10 | `weight` | 体重 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 体重 |
| 11 | `health` | 健康状况 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 健康状况 |
| 12 | `id_card` | 身份证号 | `varchar(80)` | 80 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 身份证号 |
| 13 | `education` | 学历 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 学历 |
| 14 | `school` | 毕业学校 | `varchar(80)` | 80 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 毕业学校 |
| 15 | `major` | 专业 | `varchar(80)` | 80 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 专业 |
| 16 | `address` | 联系地址 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 联系地址 |
| 17 | `zip_code` | 邮编 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 邮编 |
| 18 | `email` | Email | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | Email |
| 19 | `phone` | 手机号 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 手机号 |
| 20 | `foreign_language` | 外语语种 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 外语语种 |
| 21 | `foreign_language_level` | 外语水平 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 外语水平 |
| 22 | `computer_level` | 计算机水平 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 计算机水平 |
| 23 | `graduation_time` | 毕业时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 毕业时间 |
| 24 | `arrival_time` | 到职时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 到职时间 |
| 25 | `positional_titles` | 职称 | `varchar(30)` | 30 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 职称 |
| 26 | `education_experience` | 教育经历 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 教育经历 |
| 27 | `work_experience` | 工作经历 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 工作经历 |
| 28 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 29 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 30 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 31 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 32 | `del_flag` | 删除标识0-正常,1-已删除 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 删除标识0-正常,1-已删除 | 删除标识0-正常,1-已删除 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `del_flag`：删除标识0-正常,1-已删除。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 138. 表：`rep_demo_gongsi` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rep_demo_gongsi` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 2 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `int` | 10,0 | 否 | `无/NULL` | 是 | 是 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `gname` | 货品名称 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 货品名称 |
| 3 | `gdata` | 返利 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 返利 |
| 4 | `tdata` | 备注 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 5 | `didian` | 待确认 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `zhaiyao` | 待确认 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `num` | 待确认 | `varchar(255)` | 255 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 139. 表：`rep_demo_jianpiao` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rep_demo_jianpiao` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 86 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `int` | 10,0 | 否 | `无/NULL` | 是 | 是 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `bnum` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 3 | `ftime` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 4 | `sfkong` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 5 | `kaishi` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 6 | `jieshu` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 7 | `hezairen` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 8 | `jpnum` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 9 | `shihelv` | 待确认 | `varchar(125)` | 125 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |
| 10 | `s_id` | 待确认 | `int` | 10,0 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 数据库 COMMENT、约束和扫描到的代码注释不足以确认该字段业务语义，待确认。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 140. 表：`rep_demo_order_main` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rep_demo_order_main` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `order_code` | 订单编码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单编码 |
| 7 | `order_date` | 下单时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下单时间 |
| 8 | `descc` | 描述 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 9 | `xiala` | 下拉多选 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下拉多选 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 141. 表：`rep_demo_order_product` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `rep_demo_order_product` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 37 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `product_name` | 产品名字 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产品名字 |
| 7 | `price` | 价格 | `double(32,0)` | 32,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 价格 |
| 8 | `num` | 数量 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数量 |
| 9 | `descc` | 描述 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 10 | `order_fk_id` | 订单外键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 订单外键ID |
| 11 | `pro_type` | 产品类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产品类型 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 142. 表：`sys_announcement` 系统通告表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_announcement` |
| 中文名称 | 系统通告表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 系统通告表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `titile` | 标题 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 标题 |
| 3 | `msg_content` | 内容 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 内容 |
| 4 | `start_time` | 开始时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_start_time | 否 | 否 | — | 开始时间 |
| 5 | `end_time` | 结束时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_endtime | 否 | 否 | — | 结束时间 |
| 6 | `sender` | 发布人 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_sender | 否 | 否 | — | 发布人 |
| 7 | `priority` | 优先级（L低，M中，H高） | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 优先级（L低，M中，H高） |
| 8 | `msg_category` | 消息类型1:通知公告2:系统消息 | `varchar(10)` | 10 | 否 | `2` | 否 | 否 | 否 | 否 | 否 | 否 | 消息类型1:通知公告2:系统消息 | 消息类型1:通知公告2:系统消息 |
| 9 | `msg_type` | 通告对象类型（USER:指定用户，ALL:全体用户） | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_msg_type | 否 | 否 | 通告对象类型（USER:指定用户，ALL:全体用户） | 通告对象类型（USER:指定用户，ALL:全体用户） |
| 10 | `send_status` | 发布状态（0未发布，1已发布，2已撤销） | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_send_status | 否 | 否 | 发布状态（0未发布，1已发布，2已撤销） | 发布状态（0未发布，1已发布，2已撤销） |
| 11 | `send_time` | 发布时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 发布时间 |
| 12 | `cancel_time` | 撤销时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 撤销时间 |
| 13 | `del_flag` | 删除状态（0，正常，1已删除） | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_del_flag | 是 | 否 | 删除状态（0，正常，1已删除） | 删除状态（0，正常，1已删除） |
| 14 | `bus_type` | 业务类型(email:邮件 bpm:流程 tenant_invite:租户邀请) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 业务类型(email:邮件 bpm:流程 tenant_invite:租户邀请) | 业务类型(email:邮件 bpm:流程 tenant_invite:租户邀请) |
| 15 | `bus_id` | 业务id | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 业务id |
| 16 | `open_type` | 打开方式(组件：component 路由：url) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 打开方式(组件：component 路由：url) | 打开方式(组件：component 路由：url) |
| 17 | `open_page` | 组件/路由 地址 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件/路由 地址 |
| 18 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 19 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sanno_create_time | 是 | 否 | — | 创建时间 |
| 20 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 21 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 22 | `user_ids` | 指定用户 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 指定用户 |
| 23 | `msg_abstract` | 摘要/扩展业务参数 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 摘要/扩展业务参数 |
| 24 | `dt_task_id` | 钉钉task_id，用于撤回消息 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 钉钉task_id，用于撤回消息 |
| 25 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | idx_sanno_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 26 | `files` | 附件 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 附件 |
| 27 | `visits_num` | 访问次数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 访问次数 |
| 28 | `iz_top` | 是否置顶（0:否;  1:是） | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否置顶（0:否;  1:是） | 是否置顶（0:否;  1:是） |
| 29 | `iz_approval` | 是否审批（0否 1是） | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否审批（0否 1是） | 是否审批（0否 1是） |
| 30 | `bpm_status` | 流程状态 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 流程状态 |
| 31 | `msg_classify` | 消息归类 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 消息归类 |
| 32 | `notice_type` | 通知类型(system:系统消息、file:知识库、flow:流程、plan:日程计划、meeting:会议) | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 通知类型(system:系统消息、file:知识库、flow:流程、plan:日程计划、meeting:会议) | 通知类型(system:系统消息、file:知识库、flow:流程、plan:日程计划、meeting:会议) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sanno_create_time` | `create_time` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sanno_del_flag` | `del_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sanno_endtime` | `end_time` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sanno_msg_type` | `msg_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sanno_send_status` | `send_status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_sanno_sender` | `sender` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sanno_start_time` | `start_time` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sanno_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_announcement.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `msg_category`：消息类型1:通知公告2:系统消息。
- `msg_type`：通告对象类型（USER:指定用户，ALL:全体用户）。
- `send_status`：发布状态（0未发布，1已发布，2已撤销）。
- `del_flag`：删除状态（0，正常，1已删除）。
- `bus_type`：业务类型(email:邮件 bpm:流程 tenant_invite:租户邀请)。
- `open_type`：打开方式(组件：component 路由：url)。
- `iz_top`：是否置顶（0:否;  1:是）。
- `iz_approval`：是否审批（0否 1是）。
- `notice_type`：通知类型(system:系统消息、file:知识库、flow:流程、plan:日程计划、meeting:会议)。

### 业务说明

系统通告表

## 143. 表：`sys_announcement_send` 用户通告阅读标记表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_announcement_send` |
| 中文名称 | 用户通告阅读标记表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 用户通告阅读标记表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 29 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `annt_id` | 通告ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sacm_annt_id | 否 | 否/待确认 | — | 通告ID |
| 3 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sacm_user_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 4 | `read_flag` | 阅读状态（0未读，1已读） | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sacm_read_flag | 否 | 否 | 阅读状态（0未读，1已读） | 阅读状态（0未读，1已读） |
| 5 | `read_time` | 阅读时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 阅读时间 |
| 6 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 7 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 8 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 9 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 10 | `star_flag` | 标星状态( 1为标星 空/0没有标星) | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sacm_star_flag | 否 | 否 | 标星状态( 1为标星 空/0没有标星) | 标星状态( 1为标星 空/0没有标星) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sacm_annt_id` | `annt_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sacm_read_flag` | `read_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sacm_star_flag` | `star_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sacm_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_announcement_send.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- `read_flag`：阅读状态（0未读，1已读）。
- `star_flag`：标星状态( 1为标星 空/0没有标星)。

### 业务说明

用户通告阅读标记表

## 144. 表：`sys_category` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_category` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 29 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `pid` | 父级节点 | `varchar(36)` | 36 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 父级节点 |
| 3 | `name` | 类型名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 类型名称 |
| 4 | `code` | 类型编码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | index_scg_code | index_scg_code | 否 | 否 | — | 类型编码 |
| 5 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 7 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 8 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 9 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 10 | `has_child` | 是否有子节点 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否有子节点 |
| 11 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `index_scg_code` | `code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_category.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 145. 表：`sys_check_rule` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_check_rule` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 2 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `rule_name` | 规则名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则名称 |
| 3 | `rule_code` | 规则Code | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uk_scr_rule_code | uk_scr_rule_code | 否 | 否 | — | 规则Code |
| 4 | `rule_json` | 规则JSON | `varchar(1024)` | 1024 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则JSON |
| 5 | `rule_description` | 规则描述 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则描述 |
| 6 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 7 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 8 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_scr_rule_code` | `rule_code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 146. 表：`sys_comment` 系统评论回复表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_comment` |
| 中文名称 | 系统评论回复表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 系统评论回复表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 20 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `table_name` | 表名 | `varchar(50)` | 50 | 否 | `无/NULL` | 否 | 否 | 否 | idx_table_data_id | 否 | 否 | — | 表名 |
| 3 | `table_data_id` | 数据id | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_table_data_id | 否 | 否/待确认 | — | 数据id |
| 4 | `from_user_id` | 来源用户id | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 来源用户id |
| 5 | `to_user_id` | 发送给用户id(允许为空) | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 发送给用户id(允许为空) |
| 6 | `comment_id` | 评论id(允许为空，不为空时，则为回复) | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 评论id(允许为空，不为空时，则为回复) |
| 7 | `comment_content` | 回复内容 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 回复内容 |
| 8 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 9 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 10 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 11 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_table_data_id` | `table_name, table_data_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

系统评论回复表

## 147. 表：`sys_data_log` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_data_log` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 日志 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 28 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | id |
| 2 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 3 | `create_name` | 创建人真实名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 创建人真实名称 |
| 4 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 5 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 6 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 7 | `data_table` | 表名 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdl_data_table_id | 否 | 否 | — | 表名 |
| 8 | `data_id` | 数据ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdl_data_table_id | 否 | 否/待确认 | — | 数据ID |
| 9 | `data_content` | 数据内容 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据内容 |
| 10 | `data_version` | 版本号 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 版本号 |
| 11 | `type` | 类型 | `varchar(20)` | 20 | 是 | `json` | 否 | 否 | 否 | 否 | 否 | 否 | 具体枚举值待确认 | 类型 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sdl_data_table_id` | `data_table, data_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `type`：状态/类型类字段，完整枚举值待确认。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 148. 表：`sys_data_source` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_data_source` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `code` | 数据源编码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uk_sdc_rule_code | uk_sdc_rule_code | 否 | 否 | — | 数据源编码 |
| 3 | `name` | 数据源名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据源名称 |
| 4 | `remark` | 备注 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 5 | `db_type` | 数据库类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库类型 |
| 6 | `db_driver` | 驱动类 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 驱动类 |
| 7 | `db_url` | 数据源地址 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据源地址 |
| 8 | `db_name` | 数据库名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据库名称 |
| 9 | `db_username` | 用户名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户名 |
| 10 | `db_password` | 密码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 密码 |
| 11 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 12 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 13 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 14 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 15 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 16 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_sdc_rule_code` | `code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `sys_data_source.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 149. 表：`sys_depart` 组织机构表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_depart` |
| 中文名称 | 组织机构表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 组织机构表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 71 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | ID |
| 2 | `parent_id` | 父机构ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sd_parent_id | 否 | 否/待确认 | — | 父机构ID |
| 3 | `depart_name` | 机构/部门名称 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 机构/部门名称 |
| 4 | `depart_name_en` | 英文名 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 英文名 |
| 5 | `depart_name_abbr` | 缩写 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 缩写 |
| 6 | `depart_order` | 排序 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | idx_sd_depart_order | 否 | 否 | — | 排序 |
| 7 | `description` | 描述 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 8 | `org_category` | 机构类别 1公司，2部门，3岗位，4子公司 | `varchar(10)` | 10 | 否 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | 机构类别 1公司，2部门，3岗位，4子公司 | 机构类别 1公司，2部门，3岗位，4子公司 |
| 9 | `org_type` | 树深度层级level | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 树深度层级level |
| 10 | `org_code` | 机构编码 | `varchar(64)` | 64 | 否 | `无/NULL` | 否 | 否 | 联合唯一:uniq_depart_tenant_org_code | uniq_depart_tenant_org_code | 否 | 否 | — | 机构编码 |
| 11 | `mobile` | 手机号 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 手机号 |
| 12 | `fax` | 传真 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 传真 |
| 13 | `address` | 地址 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 地址 |
| 14 | `memo` | 备注 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 15 | `status` | 状态（1启用，0不启用） | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态（1启用，0不启用） | 状态（1启用，0不启用） |
| 16 | `del_flag` | 删除状态（0，正常，1已删除） | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 删除状态（0，正常，1已删除） | 删除状态（0，正常，1已删除） |
| 17 | `qywx_identifier` | 对接企业微信的ID | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 对接企业微信的ID |
| 18 | `ding_identifier` | 对接钉钉部门的ID | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 对接钉钉部门的ID |
| 19 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 20 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 21 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 22 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 23 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 联合唯一:uniq_depart_tenant_org_code | uniq_depart_tenant_org_code | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 24 | `iz_leaf` | 是否有叶子节点: 1是0否 | `tinyint(1)` | 3,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否有叶子节点: 1是0否 | 是否有叶子节点: 1是0否 |
| 25 | `position_id` | 职级id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sd_position_id | 否 | 否/待确认 | — | 职级id |
| 26 | `dep_post_parent_id` | 上级岗位id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sd_dep_post_parent_id | 否 | 否/待确认 | — | 上级岗位id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sd_dep_post_parent_id` | `dep_post_parent_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sd_depart_order` | `depart_order` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sd_parent_id` | `parent_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sd_position_id` | `position_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_depart_tenant_org_code` | `tenant_id, org_code` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `sys_depart.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `org_category`：机构类别 1公司，2部门，3岗位，4子公司。
- `status`：状态（1启用，0不启用）。
- `del_flag`：删除状态（0，正常，1已删除）。
- `iz_leaf`：是否有叶子节点: 1是0否。

### 业务说明

组织机构表

## 150. 表：`sys_depart_permission` 部门权限表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_depart_permission` |
| 中文名称 | 部门权限表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 部门权限表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `depart_id` | 部门id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 部门id |
| 3 | `permission_id` | 权限id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 权限id |
| 4 | `data_rule_ids` | 数据规则id | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据规则id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

部门权限表

## 151. 表：`sys_depart_role` 部门角色表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_depart_role` |
| 中文名称 | 部门角色表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 部门角色表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `depart_id` | 部门id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 部门id |
| 3 | `role_name` | 部门角色名称 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 部门角色名称 |
| 4 | `role_code` | 部门角色编码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 部门角色编码 |
| 5 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 6 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 7 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 8 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 9 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

部门角色表

## 152. 表：`sys_depart_role_permission` 部门角色权限表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_depart_role_permission` |
| 中文名称 | 部门角色权限表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 部门角色权限表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 2 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `depart_id` | 部门id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 部门id |
| 3 | `role_id` | 角色id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdrp_role_id、idx_sdrp_role_per_id | 否 | 否/待确认 | — | 角色id |
| 4 | `permission_id` | 权限id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdrp_per_id、idx_sdrp_role_per_id | 否 | 否/待确认 | — | 权限id |
| 5 | `data_rule_ids` | 数据权限ids | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据权限ids |
| 6 | `operate_date` | 操作时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作时间 |
| 7 | `operate_ip` | 操作ip | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作ip |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sdrp_per_id` | `permission_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sdrp_role_id` | `role_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sdrp_role_per_id` | `role_id, permission_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

部门角色权限表

## 153. 表：`sys_depart_role_user` 部门角色用户表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_depart_role_user` |
| 中文名称 | 部门角色用户表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 部门角色用户表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdr_user_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `drole_id` | 角色id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sdr_role_id | 否 | 否/待确认 | — | 角色id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sdr_role_id` | `drole_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sdr_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_depart_role_user.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

部门角色用户表

## 154. 表：`sys_dict` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_dict` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 字典 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 57 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `dict_name` | 字典名称 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典名称 |
| 3 | `dict_code` | 字典编码 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | uk_sd_dict_code | uk_sd_dict_code | 否 | 否 | — | 字典编码 |
| 4 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 5 | `del_flag` | 删除状态 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 删除状态 |
| 6 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 7 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 8 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 9 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 10 | `type` | 字典类型0为string,1为number | `int(1) unsigned zerofill` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 字典类型0为string,1为number | 字典类型0为string,1为number |
| 11 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | uk_sd_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 12 | `low_app_id` | 低代码应用ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 低代码应用ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_sd_dict_code` | `dict_code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uk_sd_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |

### 关联关系

- `sys_dict.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `type`：字典类型0为string,1为number。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 155. 表：`sys_dict_item` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_dict_item` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 字典 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 214 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `dict_id` | 字典id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sditem_dict_val、idx_sditem_role_dict_id | 否 | 否/待确认 | — | 字典id |
| 3 | `item_text` | 字典项文本 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典项文本 |
| 4 | `item_value` | 字典项值 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | 否 | idx_sditem_dict_val | 否 | 否 | — | 字典项值 |
| 5 | `item_color` | 字典项颜色 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字典项颜色 |
| 6 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 7 | `sort_order` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sditem_role_sort_order | 否 | 否 | — | 排序 |
| 8 | `status` | 状态（1启用 0不启用） | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sditem_status | 是 | 否 | 状态（1启用 0不启用） | 状态（1启用 0不启用） |
| 9 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 10 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共创建时间字段。 |
| 11 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录最后更新用户。 |
| 12 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共更新时间字段。 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sditem_dict_val` | `dict_id, item_value` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sditem_role_dict_id` | `dict_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sditem_role_sort_order` | `sort_order` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sditem_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态（1启用 0不启用）。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 156. 表：`sys_files` 知识库-文档管理

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_files` |
| 中文名称 | 知识库-文档管理 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 知识库-文档管理 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `file_name` | 文件名称 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件名称 |
| 3 | `url` | 文件地址 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件地址 |
| 4 | `file_type` | 文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf） | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf） | 文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf） |
| 5 | `store_type` | 文件上传类型(temp/本地上传(临时文件) manage/知识库) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件上传类型(temp/本地上传(临时文件) manage/知识库) |
| 6 | `parent_id` | 父级id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 父级id |
| 7 | `tenant_id` | 租户id | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | index_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `file_size` | 文件大小（kb） | `double(13,2)` | 13,2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 文件大小（kb） |
| 9 | `iz_folder` | 是否文件夹(1：是  0：否) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否文件夹(1：是  0：否) | 是否文件夹(1：是  0：否) |
| 10 | `iz_root_folder` | 是否为1级文件夹，允许为空 (1：是 ) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否为1级文件夹，允许为空 (1：是 ) | 是否为1级文件夹，允许为空 (1：是 ) |
| 11 | `iz_star` | 是否标星(1：是  0：否) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否标星(1：是  0：否) | 是否标星(1：是  0：否) |
| 12 | `down_count` | 下载次数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下载次数 |
| 13 | `read_count` | 阅读次数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 阅读次数 |
| 14 | `share_url` | 分享链接 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 分享链接 |
| 15 | `share_perms` | 分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看) | 分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看) |
| 16 | `enable_down` | 是否允许下载(1：是  0：否) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否允许下载(1：是  0：否) | 是否允许下载(1：是  0：否) |
| 17 | `enable_updat` | 是否允许修改(1：是  0：否) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否允许修改(1：是  0：否) | 是否允许修改(1：是  0：否) |
| 18 | `del_flag` | 删除状态(0-正常,1-删除至回收站) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | index_del_flag | 是 | 否 | 删除状态(0-正常,1-删除至回收站) | 删除状态(0-正常,1-删除至回收站) |
| 19 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 20 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 21 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 22 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `index_del_flag` | `del_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_files.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `file_type`：文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）。
- `iz_folder`：是否文件夹(1：是  0：否)。
- `iz_root_folder`：是否为1级文件夹，允许为空 (1：是 )。
- `iz_star`：是否标星(1：是  0：否)。
- `share_perms`：分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看)。
- `enable_down`：是否允许下载(1：是  0：否)。
- `enable_updat`：是否允许修改(1：是  0：否)。
- `del_flag`：删除状态(0-正常,1-删除至回收站)。

### 业务说明

知识库-文档管理

## 157. 表：`sys_fill_rule` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_fill_rule` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 3 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键ID |
| 2 | `rule_name` | 规则名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则名称 |
| 3 | `rule_code` | 规则Code | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uk_sfr_rule_code | uk_sfr_rule_code | 否 | 否 | — | 规则Code |
| 4 | `rule_class` | 规则实现类 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则实现类 |
| 5 | `rule_params` | 规则参数 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则参数 |
| 6 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 7 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 8 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_sfr_rule_code` | `rule_code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 158. 表：`sys_form_file` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_form_file` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `table_name` | 表名 | `varchar(50)` | 50 | 否 | `无/NULL` | 否 | 否 | 否 | idx_table_form | 否 | 否 | — | 表名 |
| 3 | `table_data_id` | 数据id | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | idx_table_form | 否 | 否/待确认 | — | 数据id |
| 4 | `file_id` | 关联文件id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | index_file_id | 否 | 否/待确认 | — | 关联文件id |
| 5 | `file_type` | 文件类型(text:文本, excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 文件类型(text:文本, excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）) | 文件类型(text:文本, excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）) |
| 6 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 7 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_table_form` | `table_name, table_data_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_file_id` | `file_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `file_type`：文件类型(text:文本, excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）)。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 159. 表：`sys_gateway_route` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_gateway_route` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 5 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `router_id` | 路由ID | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 路由ID |
| 3 | `name` | 服务名 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 服务名 |
| 4 | `uri` | 服务地址 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 服务地址 |
| 5 | `predicates` | 断言 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 断言 |
| 6 | `filters` | 过滤器 | `text` | 65535 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 过滤器 |
| 7 | `retryable` | 是否重试:0-否 1-是 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否重试:0-否 1-是 | 是否重试:0-否 1-是 |
| 8 | `strip_prefix` | 是否忽略前缀0-否 1-是 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否忽略前缀0-否 1-是 | 是否忽略前缀0-否 1-是 |
| 9 | `persistable` | 是否为保留数据:0-否 1-是 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否为保留数据:0-否 1-是 | 是否为保留数据:0-否 1-是 |
| 10 | `show_api` | 是否在接口文档中展示:0-否 1-是 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否在接口文档中展示:0-否 1-是 | 是否在接口文档中展示:0-否 1-是 |
| 11 | `status` | 状态:0-无效 1-有效 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态:0-无效 1-有效 | 状态:0-无效 1-有效 |
| 12 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 13 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 14 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 15 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 16 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 17 | `del_flag` | 删除状态 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 删除状态 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `retryable`：是否重试:0-否 1-是。
- `strip_prefix`：是否忽略前缀0-否 1-是。
- `persistable`：是否为保留数据:0-否 1-是。
- `show_api`：是否在接口文档中展示:0-否 1-是。
- `status`：状态:0-无效 1-有效。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 160. 表：`sys_log` 系统日志表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_log` |
| 中文名称 | 系统日志表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 日志 |
| 业务作用 | 系统日志表 |
| 主键 | `id` |
| 存储引擎 | MyISAM |
| 数据量级 | 当前本地实例约 1375 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `log_type` | 日志类型（1登录日志，2操作日志, 3.租户操作日志） | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sl_log_type | 否 | 否 | 日志类型（1登录日志，2操作日志, 3.租户操作日志） | 日志类型（1登录日志，2操作日志, 3.租户操作日志） |
| 3 | `log_content` | 日志内容 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 日志内容 |
| 4 | `operate_type` | 操作类型 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sl_operate_type | 否 | 否 | — | 操作类型 |
| 5 | `userid` | 操作用户账号 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sl_userid | 否 | 否 | — | 操作用户账号 |
| 6 | `username` | 操作用户名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作用户名称 |
| 7 | `ip` | IP | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | IP |
| 8 | `method` | 请求java方法 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求java方法 |
| 9 | `request_url` | 请求路径 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求路径 |
| 10 | `request_param` | 请求参数 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求参数 |
| 11 | `request_type` | 请求类型 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请求类型 |
| 12 | `cost_time` | 耗时 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 耗时 |
| 13 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 14 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sl_create_time | 是 | 否 | — | 创建时间 |
| 15 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 16 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 17 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 18 | `client_type` | 客户端类型 pc:电脑端 app:手机端 h5:移动网页端 | `varchar(5)` | 5 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 客户端类型 pc:电脑端 app:手机端 h5:移动网页端 | 客户端类型 pc:电脑端 app:手机端 h5:移动网页端 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sl_create_time` | `create_time` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sl_log_type` | `log_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sl_operate_type` | `operate_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sl_userid` | `userid` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_log.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `log_type`：日志类型（1登录日志，2操作日志, 3.租户操作日志）。
- `client_type`：客户端类型 pc:电脑端 app:手机端 h5:移动网页端。

### 业务说明

系统日志表

## 161. 表：`sys_permission` 菜单权限表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_permission` |
| 中文名称 | 菜单权限表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 菜单权限表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 510 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（平台基础） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `parent_id` | 父id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 父id |
| 3 | `name` | 菜单标题 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 菜单标题 |
| 4 | `url` | 路径 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | index_menu_url | 否 | 否 | — | 路径 |
| 5 | `component` | 组件 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件 |
| 6 | `is_route` | 是否路由菜单: 0:不是  1:是（默认值1） | `tinyint(1)` | 3,0 | 是 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | 是否路由菜单: 0:不是  1:是（默认值1） | 是否路由菜单: 0:不是  1:是（默认值1） |
| 7 | `component_name` | 组件名字 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件名字 |
| 8 | `redirect` | 一级菜单跳转地址 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 一级菜单跳转地址 |
| 9 | `menu_type` | 菜单类型(0:一级菜单; 1:子菜单:2:按钮权限) | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | index_menu_type | 否 | 否 | 菜单类型(0:一级菜单; 1:子菜单:2:按钮权限) | 菜单类型(0:一级菜单; 1:子菜单:2:按钮权限) |
| 10 | `perms` | 菜单权限编码 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 菜单权限编码 |
| 11 | `perms_type` | 权限策略1显示2禁用 | `varchar(10)` | 10 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 权限策略1显示2禁用 | 权限策略1显示2禁用 |
| 12 | `sort_no` | 菜单排序 | `double(8,2)` | 8,2 | 是 | `无/NULL` | 否 | 否 | 否 | index_menu_sort_no | 否 | 否 | — | 菜单排序 |
| 13 | `always_show` | 聚合子路由: 1是0否 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 聚合子路由: 1是0否 | 聚合子路由: 1是0否 |
| 14 | `icon` | 菜单图标 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 菜单图标 |
| 15 | `is_leaf` | 是否叶子节点:    1是0否 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否叶子节点:    1是0否 | 是否叶子节点:    1是0否 |
| 16 | `keep_alive` | 是否缓存该页面:    1:是   0:不是 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否缓存该页面:    1:是   0:不是 | 是否缓存该页面:    1:是   0:不是 |
| 17 | `hidden` | 是否隐藏路由: 0否,1是 | `tinyint` | 3,0 | 是 | `0` | 否 | 否 | 否 | index_menu_hidden | 否 | 否 | 是否隐藏路由: 0否,1是 | 是否隐藏路由: 0否,1是 |
| 18 | `hide_tab` | 是否隐藏tab: 0否,1是 | `tinyint` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否隐藏tab: 0否,1是 | 是否隐藏tab: 0否,1是 |
| 19 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 20 | `create_by` | 创建人 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 21 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 22 | `update_by` | 更新人 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 23 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 24 | `del_flag` | 删除状态 0正常 1已删除 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | index_menu_del_flag | 是 | 否 | 删除状态 0正常 1已删除 | 删除状态 0正常 1已删除 |
| 25 | `rule_flag` | 是否添加数据权限1是0否 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 否 | 否 | 是否添加数据权限1是0否 | 是否添加数据权限1是0否 |
| 26 | `status` | 按钮权限状态(0无效1有效) | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | index_menu_status | 是 | 否 | 按钮权限状态(0无效1有效) | 按钮权限状态(0无效1有效) |
| 27 | `internal_or_external` | 外链菜单打开方式 0/内部打开 1/外部打开 | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 外链菜单打开方式 0/内部打开 1/外部打开 | 外链菜单打开方式 0/内部打开 1/外部打开 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `index_menu_del_flag` | `del_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_menu_hidden` | `hidden` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_menu_sort_no` | `sort_no` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_menu_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `index_menu_type` | `menu_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `index_menu_url` | `url` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `is_route`：是否路由菜单: 0:不是  1:是（默认值1）。
- `menu_type`：菜单类型(0:一级菜单; 1:子菜单:2:按钮权限)。
- `perms_type`：权限策略1显示2禁用。
- `always_show`：聚合子路由: 1是0否。
- `is_leaf`：是否叶子节点:    1是0否。
- `keep_alive`：是否缓存该页面:    1:是   0:不是。
- `hidden`：是否隐藏路由: 0否,1是。
- `hide_tab`：是否隐藏tab: 0否,1是。
- `del_flag`：删除状态 0正常 1已删除。
- `rule_flag`：是否添加数据权限1是0否。
- `status`：按钮权限状态(0无效1有效)。
- `internal_or_external`：外链菜单打开方式 0/内部打开 1/外部打开。

### 业务说明

菜单权限表

## 162. 表：`sys_permission_data_rule` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_permission_data_rule` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 25 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | ID |
| 2 | `permission_id` | 菜单ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_spdr_permission_id | 否 | 否/待确认 | — | 菜单ID |
| 3 | `rule_name` | 规则名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则名称 |
| 4 | `rule_column` | 字段 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 字段 |
| 5 | `rule_conditions` | 条件 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 条件 |
| 6 | `rule_value` | 规则值 | `varchar(300)` | 300 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 规则值 |
| 7 | `status` | 权限有效状态1有0否 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 权限有效状态1有0否 | 权限有效状态1有0否 |
| 8 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 9 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | Jeecg 公共审计字段，记录创建用户。 |
| 10 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 11 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_spdr_permission_id` | `permission_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：权限有效状态1有0否。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 163. 表：`sys_position` 职务级别

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_position` |
| 中文名称 | 职务级别 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 职务级别 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `code` | 职务编码 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uniq_code | uniq_code | 否 | 否 | — | 职务编码 |
| 3 | `name` | 职务级别名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 职务级别名称 |
| 4 | `post_level` | 职务等级 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 职务等级 |
| 5 | `company_id` | 公司id | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 公司id |
| 6 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 7 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 8 | `update_by` | 修改人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 9 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 10 | `sys_org_code` | 组织机构编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 组织机构编码 |
| 11 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_code` | `code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `sys_position.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

职务级别

## 164. 表：`sys_quartz_job` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_quartz_job` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 调度 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 4 | `del_flag` | 删除状态 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 删除状态 |
| 5 | `update_by` | 修改人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 6 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |
| 7 | `job_class_name` | 任务类名 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 任务类名 |
| 8 | `cron_expression` | cron表达式 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | cron表达式 |
| 9 | `parameter` | 参数 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 参数 |
| 10 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 11 | `status` | 状态 0正常 -1停止 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态 0正常 -1停止 | 状态 0正常 -1停止 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态 0正常 -1停止。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 165. 表：`sys_role` 角色表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_role` |
| 中文名称 | 角色表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 角色表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 19 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（平台基础） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `role_name` | 角色名称 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 角色名称 |
| 3 | `role_code` | 角色编码 | `varchar(100)` | 100 | 否 | `无/NULL` | 否 | 否 | uniq_sys_role_role_code | uniq_sys_role_role_code | 否 | 否 | — | 角色编码 |
| 4 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 5 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 8 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 9 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | idx_sysrole_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sysrole_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_sys_role_role_code` | `role_code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `sys_role.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

角色表

## 166. 表：`sys_role_index` 角色首页表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_role_index` |
| 中文名称 | 角色首页表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 角色首页表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `role_code` | 角色编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sri_role_code | 否 | 否 | — | 角色编码 |
| 3 | `url` | 路由地址 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 路由地址 |
| 4 | `component` | 组件 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 组件 |
| 5 | `is_route` | 是否路由菜单: 0:不是  1:是（默认值1） | `tinyint(1)` | 3,0 | 是 | `1` | 否 | 否 | 否 | 否 | 否 | 否 | 是否路由菜单: 0:不是  1:是（默认值1） | 是否路由菜单: 0:不是  1:是（默认值1） |
| 6 | `priority` | 优先级 | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | idx_sri_priority | 否 | 否 | — | 优先级 |
| 7 | `status` | 状态0:无效 1:有效 | `varchar(2)` | 2 | 是 | `0` | 否 | 否 | 否 | idx_sri_status | 是 | 否 | 状态0:无效 1:有效 | 状态0:无效 1:有效 |
| 8 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 9 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 10 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 11 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 12 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 13 | `relation_type` | 关联关系(ROLE:角色 USER:用户) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 关联关系(ROLE:角色 USER:用户) | 关联关系(ROLE:角色 USER:用户) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sri_priority` | `priority` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sri_role_code` | `role_code` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sri_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `is_route`：是否路由菜单: 0:不是  1:是（默认值1）。
- `status`：状态0:无效 1:有效。
- `relation_type`：关联关系(ROLE:角色 USER:用户)。

### 业务说明

角色首页表

## 167. 表：`sys_role_permission` 角色权限表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_role_permission` |
| 中文名称 | 角色权限表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 角色权限表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 1313 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（平台基础） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `role_id` | 角色id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_srp_role_id、idx_srp_role_per_id | 否 | 否/待确认 | — | 角色id |
| 3 | `permission_id` | 权限id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_srp_permission_id、idx_srp_role_per_id | 否 | 否/待确认 | — | 权限id |
| 4 | `data_rule_ids` | 数据权限ids | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数据权限ids |
| 5 | `operate_date` | 操作时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作时间 |
| 6 | `operate_ip` | 操作ip | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 操作ip |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_srp_permission_id` | `permission_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_srp_role_id` | `role_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_srp_role_per_id` | `role_id, permission_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

角色权限表

## 168. 表：`sys_sms` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_sms` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | ID |
| 2 | `es_title` | 消息标题 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 消息标题 |
| 3 | `es_type` | 发送方式：参考枚举MessageTypeEnum | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ss_es_type | 否 | 否 | 发送方式：参考枚举MessageTypeEnum | 发送方式：参考枚举MessageTypeEnum |
| 4 | `es_receiver` | 接收人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ss_es_receiver | 否 | 否 | — | 接收人 |
| 5 | `es_param` | 发送所需参数Json格式 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 发送所需参数Json格式 |
| 6 | `es_content` | 推送内容 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 推送内容 |
| 7 | `es_send_time` | 推送时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ss_es_send_time | 否 | 否 | — | 推送时间 |
| 8 | `es_send_status` | 推送状态 0未推送 1推送成功 2推送失败 -1失败不再发送 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | idx_ss_es_send_status | 否 | 否 | 推送状态 0未推送 1推送成功 2推送失败 -1失败不再发送 | 推送状态 0未推送 1推送成功 2推送失败 -1失败不再发送 |
| 9 | `es_send_num` | 发送次数 超过5次不再发送 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 发送次数 超过5次不再发送 |
| 10 | `es_result` | 推送失败原因 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 推送失败原因 |
| 11 | `remark` | 备注 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 12 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 13 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 14 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 15 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_ss_es_receiver` | `es_receiver` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ss_es_send_status` | `es_send_status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_ss_es_send_time` | `es_send_time` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_ss_es_type` | `es_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `es_type`：发送方式：参考枚举MessageTypeEnum。
- `es_send_status`：推送状态 0未推送 1推送成功 2推送失败 -1失败不再发送。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 169. 表：`sys_sms_template` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_sms_template` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `template_name` | 模板标题 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模板标题 |
| 3 | `template_code` | 模板CODE | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | uk_sst_template_code | uk_sst_template_code | 否 | 否 | — | 模板CODE |
| 4 | `template_type` | 模板类型：1短信 2邮件 3微信 | `varchar(1)` | 1 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 模板类型：1短信 2邮件 3微信 | 模板类型：1短信 2邮件 3微信 |
| 5 | `template_category` | 模版分类：notice通知公告 other其他 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 模版分类：notice通知公告 other其他 | 模版分类：notice通知公告 other其他 |
| 6 | `template_content` | 模板内容 | `varchar(1000)` | 1000 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模板内容 |
| 7 | `template_test_json` | 模板测试json | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 模板测试json |
| 8 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 9 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 10 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 11 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 12 | `use_status` | 是否使用中 1是0否 | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否使用中 1是0否 | 是否使用中 1是0否 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uk_sst_template_code` | `template_code` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `template_type`：模板类型：1短信 2邮件 3微信。
- `template_category`：模版分类：notice通知公告 other其他。
- `use_status`：是否使用中 1是0否。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 170. 表：`sys_table_white_list` 系统表白名单

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_table_white_list` |
| 中文名称 | 系统表白名单 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 系统表白名单 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 27 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `table_name` | 允许的表名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uniq_sys_table_white_list_table_name | uniq_sys_table_white_list_table_name | 否 | 否 | — | 允许的表名 |
| 3 | `field_name` | 允许的字段名，多个用逗号分割 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 允许的字段名，多个用逗号分割 |
| 4 | `status` | 状态，1=启用，0=禁用 | `varchar(10)` | 10 | 是 | `1` | 否 | 否 | 否 | 否 | 是 | 否 | 状态，1=启用，0=禁用 | 状态，1=启用，0=禁用 |
| 5 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 8 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_sys_table_white_list_table_name` | `table_name` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态，1=启用，0=禁用。

### 业务说明

系统表白名单

## 171. 表：`sys_tenant` 多租户信息表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_tenant` |
| 中文名称 | 多租户信息表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 多租户信息表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 9 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（平台基础） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 租户编码 | `int` | 10,0 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 租户编码 |
| 2 | `name` | 租户名称 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 租户名称 |
| 3 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 4 | `create_by` | 创建人 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 5 | `begin_date` | 开始时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 开始时间 |
| 6 | `end_date` | 结束时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 结束时间 |
| 7 | `status` | 状态 1正常 0冻结 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态 1正常 0冻结 | 状态 1正常 0冻结 |
| 8 | `trade` | 所属行业 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 所属行业 |
| 9 | `company_size` | 公司规模 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 公司规模 |
| 10 | `company_address` | 公司地址 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 公司地址 |
| 11 | `company_logo` | 公司logo | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 公司logo |
| 12 | `house_number` | 门牌号 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 门牌号 |
| 13 | `work_place` | 工作地点 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 工作地点 |
| 14 | `secondary_domain` | 二级域名 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 二级域名 |
| 15 | `login_bkgd_img` | 登录背景图片 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 登录背景图片 |
| 16 | `position` | 职级 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 职级 |
| 17 | `department` | 部门 | `varchar(10)` | 10 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 部门 |
| 18 | `del_flag` | 删除状态(0-正常,1-已删除) | `tinyint(1)` | 3,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 否 | 删除状态(0-正常,1-已删除) | 删除状态(0-正常,1-已删除) |
| 19 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 20 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 21 | `apply_status` | 允许申请管理员 1允许 0不允许 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 允许申请管理员 1允许 0不允许 | 允许申请管理员 1允许 0不允许 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `status`：状态 1正常 0冻结。
- `del_flag`：删除状态(0-正常,1-已删除)。
- `apply_status`：允许申请管理员 1允许 0不允许。

### 业务说明

多租户信息表

## 172. 表：`sys_tenant_pack` 租户产品包

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_tenant_pack` |
| 中文名称 | 租户产品包 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 租户产品包 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `tenant_id` | 租户id | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx__stp_tenant_id_pack_code | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 3 | `pack_name` | 产品包名 | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产品包名 |
| 4 | `status` | 开启状态(0 未开启 1开启) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 开启状态(0 未开启 1开启) | 开启状态(0 未开启 1开启) |
| 5 | `remarks` | 备注 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 备注 |
| 6 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 7 | `create_time` | 创建时间 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 8 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 9 | `update_time` | 更新时间 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 10 | `pack_code` | 编码,默认添加的三个管理员需要设置编码 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | idx__stp_tenant_id_pack_code | 否 | 否 | — | 编码,默认添加的三个管理员需要设置编码 |
| 11 | `pack_type` | 产品包类型(default 默认产品包 custom 自定义产品包) | `varchar(10)` | 10 | 是 | `custom` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产品包类型(default 默认产品包 custom 自定义产品包) |
| 12 | `iz_sysn` | 自动分配给用户(0否 1是) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 自动分配给用户(0否 1是) | 自动分配给用户(0否 1是) |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx__stp_tenant_id_pack_code` | `tenant_id, pack_code` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_tenant_pack.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：开启状态(0 未开启 1开启)。
- `iz_sysn`：自动分配给用户(0否 1是)。

### 业务说明

租户产品包

## 173. 表：`sys_tenant_pack_perms` 租户产品包和菜单关系表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_tenant_pack_perms` |
| 中文名称 | 租户产品包和菜单关系表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 租户产品包和菜单关系表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 37 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键编号 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键编号 |
| 2 | `pack_id` | 租户产品包名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_stpp_pack_id | 否 | 否/待确认 | — | 租户产品包名称 |
| 3 | `permission_id` | 菜单id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 菜单id |
| 4 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 5 | `create_time` | 创建时间 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 6 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 7 | `update_time` | 更新时间 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_stpp_pack_id` | `pack_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

租户产品包和菜单关系表

## 174. 表：`sys_tenant_pack_user` 租户套餐人员表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_tenant_pack_user` |
| 中文名称 | 租户套餐人员表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 租户套餐人员表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 4 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `pack_id` | 租户产品包ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_tpu_pack_id | 否 | 否/待确认 | — | 租户产品包ID |
| 3 | `user_id` | 用户ID | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_tpu_user_id | 是 | 逻辑→sys_user.id | — | 用户ID |
| 4 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_tpu_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户ID |
| 5 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 6 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 7 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 8 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 9 | `status` | 状态 正常状态1 申请状态0 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_tpu_status | 是 | 否 | 状态 正常状态1 申请状态0 | 状态 正常状态1 申请状态0 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_tpu_pack_id` | `pack_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_tpu_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_tpu_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_tpu_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_tenant_pack_user.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。
- `sys_tenant_pack_user.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态 正常状态1 申请状态0。

### 业务说明

租户套餐人员表

## 175. 表：`sys_third_account` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_third_account` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 编号 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 编号 |
| 2 | `sys_user_id` | 第三方登录id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sta_sys_user_id_third_type | 否 | 否/待确认 | — | 第三方登录id |
| 3 | `avatar` | 头像 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 头像 |
| 4 | `status` | 状态(1-正常,2-冻结) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 状态(1-正常,2-冻结) | 状态(1-正常,2-冻结) |
| 5 | `del_flag` | 删除状态(0-正常,1-已删除) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | 删除状态(0-正常,1-已删除) | 删除状态(0-正常,1-已删除) |
| 6 | `realname` | 真实姓名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 真实姓名 |
| 7 | `tenant_id` | 租户id | `int` | 10,0 | 是 | `0` | 否 | 否 | 联合唯一:uniq_sta_third_user_id_third_type、联合唯一:uniq_sta_third_user_uuid_third_type | uniq_sta_third_user_id_third_type、uniq_sta_third_user_uuid_third_type | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 8 | `third_user_uuid` | 第三方账号 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uniq_sta_third_user_uuid_third_type | uniq_sta_third_user_uuid_third_type | 否 | 否 | — | 第三方账号 |
| 9 | `third_user_id` | 第三方app用户账号 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uniq_sta_third_user_id_third_type、联合唯一:uniq_stat_third_type_user_id | uniq_sta_third_user_id_third_type、uniq_stat_third_type_user_id | 否 | 否/待确认 | — | 第三方app用户账号 |
| 10 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 11 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 12 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 13 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 14 | `third_type` | 登录来源 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 联合唯一:uniq_sta_third_user_id_third_type、联合唯一:uniq_sta_third_user_uuid_third_type、联合唯一:uniq_stat_third_type_user_id | idx_sta_sys_user_id_third_type、uniq_sta_third_user_id_third_type、uniq_sta_third_user_uuid_third_type、uniq_stat_third_type_user_id | 否 | 否 | — | 登录来源 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sta_sys_user_id_third_type` | `sys_user_id, third_type` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_sta_third_user_id_third_type` | `third_user_id, third_type, tenant_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_sta_third_user_uuid_third_type` | `third_user_uuid, third_type, tenant_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_stat_third_type_user_id` | `third_type, third_user_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- `sys_third_account.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态(1-正常,2-冻结)。
- `del_flag`：删除状态(0-正常,1-已删除)。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 176. 表：`sys_third_app_config` 租户第三方配置表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_third_app_config` |
| 中文名称 | 租户第三方配置表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 系统 |
| 业务作用 | 租户第三方配置表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `tenant_id` | 租户id | `int` | 10,0 | 否 | `0` | 否 | 否 | 否 | idx_stac_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 3 | `agent_id` | 钉钉/企业微信应用id | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 钉钉/企业微信应用id |
| 4 | `client_id` | 钉钉/企业微信 应用id | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 钉钉/企业微信 应用id |
| 5 | `client_secret` | 钉钉/企业微信应用id对应的秘钥 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 钉钉/企业微信应用id对应的秘钥 |
| 6 | `corp_id` | 钉钉企业id | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 钉钉企业id |
| 7 | `third_type` | 第三方类别(dingtalk 钉钉 wechat_enterprise 企业微信) | `varchar(20)` | 20 | 是 | `无/NULL` | 否 | 否 | 否 | idx_stac_third_type | 否 | 否 | — | 第三方类别(dingtalk 钉钉 wechat_enterprise 企业微信) |
| 8 | `status` | 是否启用(0-否,1-是) | `int` | 10,0 | 是 | `1` | 否 | 否 | 否 | 否 | 是 | 否 | 是否启用(0-否,1-是) | 是否启用(0-否,1-是) |
| 9 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 10 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_stac_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_stac_third_type` | `third_type` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_third_app_config.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：是否启用(0-否,1-是)。

### 业务说明

租户第三方配置表

## 177. 表：`sys_ugroup` 用户组表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_ugroup` |
| 中文名称 | 用户组表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 用户组表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `group_name` | 用户组名称 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户组名称 |
| 3 | `description` | 描述 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 4 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 5 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 6 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 7 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 8 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_su_tenant_id | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_su_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_ugroup.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

用户组表

## 178. 表：`sys_ugroup_user` 用户组关系表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_ugroup_user` |
| 中文名称 | 用户组关系表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 用户组关系表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_suu_user_id、idx_suu_user_role_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `group_id` | 用户组id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_suu_group_id、idx_suu_user_role_id | 否 | 否/待确认 | — | 用户组id |
| 4 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_suu_group_id` | `group_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_suu_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_suu_user_role_id` | `user_id, group_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_ugroup_user.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。
- `sys_ugroup_user.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

用户组关系表

## 179. 表：`sys_user` 用户表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_user` |
| 中文名称 | 用户表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 用户表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 88 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（平台基础） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `username` | 登录账号 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uniq_sys_user_username | idx_su_del_username、uniq_sys_user_username | 否 | 否 | — | 登录账号 |
| 3 | `realname` | 真实姓名 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 真实姓名 |
| 4 | `password` | 密码 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 密码 |
| 5 | `salt` | md5密码盐 | `varchar(45)` | 45 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | md5密码盐 |
| 6 | `avatar` | 头像 | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 头像 |
| 7 | `birthday` | 生日 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 生日 |
| 8 | `sex` | 性别(0-默认未知,1-男,2-女) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 性别(0-默认未知,1-男,2-女) | 性别(0-默认未知,1-男,2-女) |
| 9 | `email` | 电子邮件 | `varchar(45)` | 45 | 是 | `无/NULL` | 否 | 否 | uniq_sys_user_email | uniq_sys_user_email | 否 | 否 | — | 电子邮件 |
| 10 | `phone` | 电话 | `varchar(45)` | 45 | 是 | `无/NULL` | 否 | 否 | uniq_sys_user_phone | uniq_sys_user_phone | 否 | 否 | — | 电话 |
| 11 | `org_code` | 登录会话的机构编码 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 登录会话的机构编码 |
| 12 | `status` | 性别(1-正常,2-冻结) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_su_status | 是 | 否 | 性别(1-正常,2-冻结) | 性别(1-正常,2-冻结) |
| 13 | `del_flag` | 删除状态(0-正常,1-已删除) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_su_del_flag、idx_su_del_username | 是 | 否 | 删除状态(0-正常,1-已删除) | 删除状态(0-正常,1-已删除) |
| 14 | `third_id` | 第三方登录的唯一标识 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 第三方登录的唯一标识 |
| 15 | `third_type` | 第三方类型 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 第三方类型 |
| 16 | `activiti_sync` | 同步工作流引擎(1-同步,0-不同步) | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 同步工作流引擎(1-同步,0-不同步) | 同步工作流引擎(1-同步,0-不同步) |
| 17 | `work_no` | 工号，唯一键 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | uniq_sys_user_work_no | uniq_sys_user_work_no | 否 | 否 | — | 工号，唯一键 |
| 18 | `telephone` | 座机号 | `varchar(45)` | 45 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 座机号 |
| 19 | `create_by` | 创建人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 20 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 21 | `update_by` | 更新人 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 22 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |
| 23 | `user_identity` | 身份（1普通成员 2上级） | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 身份（1普通成员 2上级） | 身份（1普通成员 2上级） |
| 24 | `depart_ids` | 负责部门 | `varchar(1000)` | 1000 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 负责部门 |
| 25 | `client_id` | 设备ID | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 设备ID |
| 26 | `login_tenant_id` | 上次登录选择租户ID | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 上次登录选择租户ID |
| 27 | `bpm_status` | 流程入职离职状态 | `varchar(2)` | 2 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 流程入职离职状态 |
| 28 | `sign_enable` | 是否启用个性签名（0 否 1是） | `tinyint(1)` | 3,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否启用个性签名（0 否 1是） | 是否启用个性签名（0 否 1是） |
| 29 | `sign` | 个性签名 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 个性签名 |
| 30 | `main_dep_post_id` | 主岗位（部门岗位id） | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_su_main_dep_post_id | 否 | 否/待确认 | — | 主岗位（部门岗位id） |
| 31 | `position_type` | 职务(字典) | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 职务(字典) |
| 32 | `last_pwd_update_time` | 上一次修改密码的时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 上一次修改密码的时间 |
| 33 | `sort` | 排序 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 排序 |
| 34 | `iz_hide_contact` | 是否隐藏联系方式（0 否 1是） | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | 是否隐藏联系方式（0 否 1是） | 是否隐藏联系方式（0 否 1是） |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_su_del_flag` | `del_flag` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_su_del_username` | `username, del_flag` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_su_main_dep_post_id` | `main_dep_post_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_su_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |
| `uniq_sys_user_email` | `email` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_sys_user_phone` | `phone` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_sys_user_username` | `username` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `uniq_sys_user_work_no` | `work_no` | 唯一索引 | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- `sex`：性别(0-默认未知,1-男,2-女)。
- `status`：性别(1-正常,2-冻结)。
- `del_flag`：删除状态(0-正常,1-已删除)。
- `activiti_sync`：同步工作流引擎(1-同步,0-不同步)。
- `user_identity`：身份（1普通成员 2上级）。
- `sign_enable`：是否启用个性签名（0 否 1是）。
- `iz_hide_contact`：是否隐藏联系方式（0 否 1是）。

### 业务说明

用户表

## 180. 表：`sys_user_dep_post` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_user_dep_post` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sudp_user_dep_id、idx_sudp_user_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `dep_id` | 部门岗位id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sudp_dep_id、idx_sudp_user_dep_id | 否 | 否/待确认 | — | 部门岗位id |
| 4 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 5 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 6 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 7 | `update_time` | 更新时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sudp_dep_id` | `dep_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sudp_user_dep_id` | `user_id, dep_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sudp_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_user_dep_post.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 181. 表：`sys_user_depart` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_user_depart` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `ID` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 39 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `ID` | id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 否 | 否 | — | id |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 联合唯一:idx_sud_user_dep_id | idx_sud_user_dep_id、idx_sud_user_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `dep_id` | 部门id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 联合唯一:idx_sud_user_dep_id | idx_sud_dep_id、idx_sud_user_dep_id | 否 | 否/待确认 | — | 部门id |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sud_dep_id` | `dep_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sud_user_dep_id` | `user_id, dep_id` | 唯一索引（联合） | 保证字段组合唯一，并支持按该组合进行幂等或业务键查询。 |
| `idx_sud_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `ID` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_user_depart.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 182. 表：`sys_user_position` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_user_position` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sup_user_id、idx_sup_user_position_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `position_id` | 职位id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sup_position_id、idx_sup_user_position_id | 否 | 否/待确认 | — | 职位id |
| 4 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 5 | `create_time` | 创建时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建时间 |
| 6 | `update_by` | 修改人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改人 |
| 7 | `update_time` | 修改时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 修改时间 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sup_position_id` | `position_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sup_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sup_user_position_id` | `user_id, position_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_user_position.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 183. 表：`sys_user_role` 用户角色表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_user_role` |
| 中文名称 | 用户角色表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 用户角色表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 101 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 是（平台基础） |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sur_user_id、idx_sur_user_role_id | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `role_id` | 角色id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sur_role_id、idx_sur_user_role_id | 否 | 否/待确认 | — | 角色id |
| 4 | `tenant_id` | 租户ID | `int` | 10,0 | 是 | `0` | 否 | 否 | 否 | 否 | 是 | 逻辑→sys_tenant.id | — | 租户ID |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sur_role_id` | `role_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sur_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sur_user_role_id` | `user_id, role_id` | 普通索引（联合） | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_user_role.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。
- `sys_user_role.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

用户角色表

## 184. 表：`sys_user_tenant` 用户租户关系表

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `sys_user_tenant` |
| 中文名称 | 用户租户关系表 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | Jeecg 用户权限 |
| 业务作用 | 用户租户关系表 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 78 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键id | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键id |
| 2 | `user_id` | 用户id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sut_user_id、idx_sut_user_rel_tenant、idx_sut_userid_status | 是 | 逻辑→sys_user.id | — | 用户id |
| 3 | `tenant_id` | 租户id | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sut_tenant_id、idx_sut_user_rel_tenant | 是 | 逻辑→sys_tenant.id | — | 租户id |
| 4 | `status` | 状态(1 正常 2 离职 3 待审核 4 拒绝 5 邀请加入) | `varchar(1)` | 1 | 是 | `无/NULL` | 否 | 否 | 否 | idx_sut_status、idx_sut_userid_status | 是 | 否 | 状态(1 正常 2 离职 3 待审核 4 拒绝 5 邀请加入) | 状态(1 正常 2 离职 3 待审核 4 拒绝 5 邀请加入) |
| 5 | `create_by` | 创建人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 6 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 7 | `update_by` | 更新人登录名称 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 8 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `idx_sut_status` | `status` | 普通索引 | 支持按状态和时间扫描任务、日志或业务记录。 |
| `idx_sut_tenant_id` | `tenant_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sut_user_id` | `user_id` | 普通索引 | 支持按索引字段组合查询；具体调用路径需结合 Mapper/Repository。 |
| `idx_sut_user_rel_tenant` | `user_id, tenant_id` | 普通索引（联合） | 支持租户与用户作用域查询。 |
| `idx_sut_userid_status` | `user_id, status` | 普通索引（联合） | 支持按状态和时间扫描任务、日志或业务记录。 |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- `sys_user_tenant.(user_id)` → `sys_user.(id)`：逻辑外键；认证用户逻辑外键；业务写入从 LoginUser 派生，不接受客户端指定所有者。
- `sys_user_tenant.(tenant_id)` → `sys_tenant.(id)`：逻辑外键；租户隔离逻辑外键。

### 枚举与约束

- `status`：状态(1 正常 2 离职 3 待审核 4 拒绝 5 邀请加入)。

### 业务说明

用户租户关系表

## 185. 表：`test_demo` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_demo` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人登录名称 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人登录名称 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人登录名称 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `name` | 用户名 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户名 |
| 7 | `sex` | 性别 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 性别 |
| 8 | `age` | 年龄 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 年龄 |
| 9 | `descc` | 描述 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 10 | `birthday` | 生日 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 生日 |
| 11 | `user_code` | 用户编码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户编码 |
| 12 | `file_kk` | 附件 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 附件 |
| 13 | `top_pic` | 头像 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 头像 |
| 14 | `chegnshi` | 城市 | `varchar(300)` | 300 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 城市 |
| 15 | `ceck` | checkbox | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | checkbox |
| 16 | `xiamuti` | 下拉多选 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下拉多选 |
| 17 | `search_sel` | 搜索下拉 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 搜索下拉 |
| 18 | `pop` | 弹窗 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 弹窗 |
| 19 | `sel_table` | 下拉字典表 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下拉字典表 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 186. 表：`test_enhance_select` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_enhance_select` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 7 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `province` | 省份 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 省份 |
| 3 | `city` | 市 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 市 |
| 4 | `area` | 区 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 区 |
| 5 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 6 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 187. 表：`test_note` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_note` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `name` | 用户名 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户名 |
| 8 | `age` | 年龄 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 年龄 |
| 9 | `sex` | 性别 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 性别 |
| 10 | `birthday` | 生日 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 生日 |
| 11 | `contents` | 请假原因 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假原因 |
| 12 | `year` | 年 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 年 |
| 13 | `sheng` | 地区 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 地区 |
| 14 | `month` | 月 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 月 |
| 15 | `begin_time` | 开始时间 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 开始时间 |
| 16 | `long_ids` | 长类型 | `bigint` | 19,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 长类型 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 188. 表：`test_online_link` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_online_link` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 11 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(32)` | 32 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `pid` | pid | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | pid |
| 3 | `name` | name | `varchar(255)` | 255 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | name |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 189. 表：`test_order_customer` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_order_customer` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 8 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `name` | 客户名字 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 客户名字 |
| 8 | `sex` | 性别 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 性别 |
| 9 | `age` | 年龄 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 年龄 |
| 10 | `birthday` | 生日 | `date` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 生日 |
| 11 | `order_id` | 订单id | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 订单id |
| 12 | `address` | 地址 | `varchar(300)` | 300 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 地址 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 190. 表：`test_order_main` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_order_main` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 9 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `order_code` | 订单编码 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 订单编码 |
| 7 | `order_date` | 下单时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下单时间 |
| 8 | `descc` | 描述 | `varchar(100)` | 100 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 9 | `xiala` | 下拉多选 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 下拉多选 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 191. 表：`test_order_product` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_order_product` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 22 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `product_name` | 产品名字 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产品名字 |
| 7 | `price` | 价格 | `double(32,0)` | 32,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 价格 |
| 8 | `num` | 数量 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 数量 |
| 9 | `descc` | 描述 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 描述 |
| 10 | `order_fk_id` | 订单外键ID | `varchar(32)` | 32 | 否 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否/待确认 | — | 订单外键ID |
| 11 | `pro_type` | 产品类型 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 产品类型 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 192. 表：`test_person` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_person` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 0 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 ID | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sex` | 性别 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 性别 |
| 7 | `name` | 用户名 | `varchar(200)` | 200 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 用户名 |
| 8 | `content` | 请假原因 | `longtext` | 4294967295 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假原因 |
| 9 | `be_date` | 请假时间 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假时间 |
| 10 | `qj_days` | 请假天数 | `int` | 10,0 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 请假天数 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。

## 193. 表：`test_shoptype_tree` 待确认

### 基本信息

| 项目 | 内容 |
| --- | --- |
| 表名 | `test_shoptype_tree` |
| 中文名称 | 待确认 |
| 所属数据库 | `rehealth_software` |
| 所属模块 | 演示/测试 |
| 业务作用 | 数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。 |
| 主键 | `id` |
| 存储引擎 | InnoDB |
| 数据量级 | 当前本地实例约 6 行（InnoDB 统计估算，非生产容量） |
| 是否核心表 | 否 |
| 结构依据 | 运行中 MySQL information_schema；表/字段 COMMENT；ReHealth SQL 与 Repository |

### 字段

| 序号 | 字段名 | 中文含义 | 类型 | 长度/精度 | 允许 NULL | 默认值 | 主键 | 自增 | 唯一 | 索引 | 公共字段 | 关联 | 枚举/约束 | 业务说明 |
| ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `id` | 主键 | `varchar(36)` | 36 | 否 | `无/NULL` | 是 | 否 | PRIMARY | PRIMARY | 是 | 否 | — | 主键 |
| 2 | `create_by` | 创建人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建人 |
| 3 | `create_time` | 创建日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 创建日期 |
| 4 | `update_by` | 更新人 | `varchar(50)` | 50 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新人 |
| 5 | `update_time` | 更新日期 | `datetime` | 不适用 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 更新日期 |
| 6 | `sys_org_code` | 所属部门 | `varchar(64)` | 64 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 是 | 否 | — | 所属部门 |
| 7 | `type_name` | 商品分类 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 商品分类 |
| 8 | `pic` | 分类图片 | `varchar(500)` | 500 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 分类图片 |
| 9 | `pid` | 父级节点 | `varchar(32)` | 32 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 父级节点 |
| 10 | `has_child` | 是否有子节点 | `varchar(3)` | 3 | 是 | `无/NULL` | 否 | 否 | 否 | 否 | 否 | 否 | — | 是否有子节点 |

### 索引

| 索引名称 | 字段 | 类型 | 作用 |
| --- | --- | --- | --- |
| `PRIMARY` | `id` | 主键 | 保证记录唯一并支持主键定位。 |

### 关联关系

- 未发现物理外键；本轮代码与命名证据也不足以确认其他逻辑关联，待确认。

### 枚举与约束

- 未发现数据库 CHECK 或可确认的代码枚举。

### 业务说明

数据库表/字段注释及本轮扫描到的业务证据不足，具体业务作用待确认。
