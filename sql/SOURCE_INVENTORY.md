# SQL 来源清单

> 扫描日期：2026-08-19。统一资产不删除、不改写历史 Flyway；业务 Mapper/JDBC SQL 继续由代码维护。

| 类型 | 原始位置 | 处理方式 |
|---|---|---|
| 业务或工具 SQL | `backend/deploy/rehealth/.local-runtime/jeecgboot-rehealth-software.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/cleanup-medical-workspace-hardware-test-data.sql` | 种子脚本已按适用范围汇总，清理脚本保留原位 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/cleanup-medical-workspace-test-data.sql` | 种子脚本已按适用范围汇总，清理脚本保留原位 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/seed-insurance-workflow-test-data.sql` | 按依赖顺序汇总至 `mysql/04_test_data.sql`；原脚本保留 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/seed-medical-workspace-hardware-test-data.sql` | 汇总至 `timescaledb/04_test_data.sql`；原脚本保留 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/seed-medical-workspace-test-data.sql` | 按依赖顺序汇总至 `mysql/04_test_data.sql`；原脚本保留 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-hardware-test-data.sql` | 汇总至 `timescaledb/04_test_data.sql`；原脚本保留 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-test-data.sql` | 按依赖顺序汇总至 `mysql/04_test_data.sql`；原脚本保留 |
| 测试/清理数据 | `backend/deploy/rehealth/scripts/seed-multi-insurer-tenant-test-data.sql` | 按依赖顺序汇总至 `mysql/04_test_data.sql`；原脚本保留 |
| 基础结构/厂商脚本 | `backend/deploy/rehealth/timescale/init.sql` | 保留原位并在 README 说明依赖 |
| 版本迁移 | `backend/device-service/src/main/resources/db/migration/timescale/V1__verify_timescale_prerequisites.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/device-service/src/main/resources/db/migration/timescale/V2__create_hardware_schema.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/device-service/src/main/resources/db/migration/timescale/V3__create_hypertables_and_lifecycle_policies.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/device-service/src/main/resources/db/migration/timescale/V4__create_diet_behavior_records.sql` | 保留原位；统一目录只提供当前快照 |
| 业务或工具 SQL | `backend/device-service/src/test/resources/mysql-hardware.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 构建产物 | `backend/device-service/target/classes/db/migration/timescale/V1__verify_timescale_prerequisites.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/device-service/target/classes/db/migration/timescale/V2__create_hardware_schema.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/device-service/target/classes/db/migration/timescale/V3__create_hypertables_and_lifecycle_policies.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/device-service/target/classes/db/migration/timescale/V4__create_diet_behavior_records.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/device-service/target/test-classes/mysql-hardware.sql` | 不纳入；由源文件生成 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/db/jeecgboot-mysql-5.7.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/db/tables_nacos.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/db/tables_xxl_job.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/hardware/mysql/V1__create_hardware_telemetry_tables.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V1__create_rehealth_software_tables.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260723_2__upgrade_legacy_software_schema.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260723_3__add_telemetry_kafka_projection.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260724_1__harden_model_request_audit.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260729_1__normalize_business_records.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260730_1__add_health_agent_conversations.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260731_1__add_behavior_records.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260731_2__add_factor16_contributions.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260805_1__add_rhi_manual_health_input.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260811_1__seed_insurance_risk_permission.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260812_1__add_website_records.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260812_2__create_insurance_business_schema.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260812_3__seed_insurer_roles.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_1__extend_insurance_workflow.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_2__seed_insurer_workflow_permissions.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_3__grant_insurance_workflow_to_admin.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_4__add_insurance_subject_manager_scope.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_5__rename_insurer_roles_cn.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_6__create_insurance_settings.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_7__grant_insurance_settings_to_admin.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_8__isolate_department_codes_by_tenant.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_1__grant_insurance_settings_view.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_2__create_insurance_intervention_actions.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_3__create_rhi_daily_snapshot.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_4__create_rdi_daily_snapshot.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260817_1__add_insurance_adherence_events.sql` | 保留原位并在 README 说明依赖 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260819_1__create_versioned_care_plans.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260819_2__create_care_plan_execution_facts.sql` | 保留原位并在 README 说明依赖 |
| 测试/清理数据 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/testdata/software/mysql/seed-versioned-care-plan-test-data.sql` | 按依赖顺序汇总至 `mysql/04_test_data.sql`；原脚本保留 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/hardware/mysql/V1__create_hardware_telemetry_tables.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V1__create_rehealth_software_tables.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260723_2__upgrade_legacy_software_schema.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260723_3__add_telemetry_kafka_projection.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260724_1__harden_model_request_audit.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260729_1__normalize_business_records.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260730_1__add_health_agent_conversations.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260731_1__add_behavior_records.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260731_2__add_factor16_contributions.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260805_1__add_rhi_manual_health_input.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260811_1__seed_insurance_risk_permission.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260812_1__add_website_records.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260812_2__create_insurance_business_schema.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260812_3__seed_insurer_roles.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_1__extend_insurance_workflow.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_2__seed_insurer_workflow_permissions.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_3__grant_insurance_workflow_to_admin.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_4__add_insurance_subject_manager_scope.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_5__rename_insurer_roles_cn.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_6__create_insurance_settings.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_7__grant_insurance_settings_to_admin.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260813_8__isolate_department_codes_by_tenant.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260814_1__grant_insurance_settings_view.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260814_2__create_insurance_intervention_actions.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260814_3__create_rhi_daily_snapshot.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260814_4__create_rdi_daily_snapshot.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260817_1__add_insurance_adherence_events.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260819_1__create_versioned_care_plans.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/software/mysql/V20260819_2__create_care_plan_execution_facts.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/target/classes/db/testdata/software/mysql/seed-versioned-care-plan-test-data.sql` | 不纳入；由源文件生成 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/src/main/resources/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-biz/target/classes/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.0_1__airag_add_menu.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.0_2__airag_init_db.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.1_1__all_upgrade.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.1_2__openapi.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.2_1__all_upgrade.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.3_0__all_upgrade.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.8.3_1__upgrade_jimubi.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.0_0__all_upgrade.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.0_1__mcp_demo.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.0_2__upd_dep_category.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.0_3__add_aiflow_permission.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.0_4__add_onlineuser_perms.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.1_0__all_upgrade.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.1_1__add_aiapp_img_gen.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.1_2__add_aiwriteblog.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.2_0__all_upgrade.sql` | 保留原位；统一目录只提供当前快照 |
| 版本迁移 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.2_1__rehealth_admin_patient_permission.sql` | 保留原位；统一目录只提供当前快照 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.0_1__airag_add_menu.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.0_2__airag_init_db.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.1_1__all_upgrade.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.1_2__openapi.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.2_1__all_upgrade.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.3_0__all_upgrade.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.8.3_1__upgrade_jimubi.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.0_0__all_upgrade.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.0_1__mcp_demo.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.0_2__upd_dep_category.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.0_3__add_aiflow_permission.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.0_4__add_onlineuser_perms.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.1_0__all_upgrade.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.1_1__add_aiapp_img_gen.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.1_2__add_aiwriteblog.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.2_0__all_upgrade.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/classes/flyway/sql/mysql/V3.9.2_1__rehealth_admin_patient_permission.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-cloud-nacos/docs/db/nacos_dm.sql` | 保留原位并在 README 说明依赖 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 版本迁移 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/src/main/resources/flyway/sql/mysql/V3.9.2_1__rehealth_admin_patient_permission.sql` | 生产基础数据抽取至 `mysql/03_init_data.sql`；原迁移保留 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/classes/flyway/sql/mysql/V3.9.2_1__rehealth_admin_patient_permission.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 构建产物 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/target/config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 不纳入；由源文件生成 |
| 基础结构/厂商脚本 | `backend/jeecg-boot/jeecg-server-cloud/jeecg-visual/jeecg-cloud-xxljob/doc/db/tables_xxl_job.sql` | 保留原位并在 README 说明依赖 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/default/tree/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/erp/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/inner-table/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/jvxe/onetomany/java/${bussiPackage}/${entityPackage}/vue3Native/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |
| 业务或工具 SQL | `config/jeecg/code-template-online/tab/onetomany/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql` | 保留原位；运行时 SQL 不复制为建表脚本 |

## 非 `.sql` SQL 来源

- Android `AppDatabase.kt`、`RiskHistoryMigrationSql.kt` 与各 `@Entity`：结构以 Room 19.json 导出为准，统一到 `sqlite/01_room_schema.sql`。
- Device Service Java Repository：只包含运行时 DML，结构由 Timescale Flyway 管理，不复制。
- JeecgBoot Java Repository、MyBatis Mapper/XML：只包含运行时查询/DML，结构由软件库迁移和平台基线管理，不复制。
- `seed-admin-rhi-test-data.ps1`、`seed-admin-intervention-test-data.ps1`：包含按运行环境动态解析 ID 的 SQL，保留为运行时测试工具，未静态拼接。
- Nacos、XXL-Job 与 JeecgBoot 官方大脚本属于第三方平台基线，未混入 ReHealth 业务初始化数据。
