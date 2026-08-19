-- ReHealth software_db 当前结构快照（由本地 MySQL catalog 生成）
-- 目标版本：MySQL 8.4；字符集 utf8mb4；时区 UTC
-- 索引与物理外键保留在各 CREATE TABLE 中，避免重复创建。
SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 表：QRTZ_BLOB_TRIGGERS
-- 中文名称：QRTZ_BLOB_TRIGGERS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_BLOB_TRIGGERS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `BLOB_DATA` blob COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_BLOB_TRIGGERS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_CALENDARS
-- 中文名称：QRTZ_CALENDARS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_CALENDARS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `CALENDAR` blob NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_CALENDARS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_CRON_TRIGGERS
-- 中文名称：QRTZ_CRON_TRIGGERS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_CRON_TRIGGERS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `CRON_EXPRESSION` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TIME_ZONE_ID` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_CRON_TRIGGERS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_FIRED_TRIGGERS
-- 中文名称：QRTZ_FIRED_TRIGGERS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_FIRED_TRIGGERS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `ENTRY_ID` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `FIRED_TIME` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `SCHED_TIME` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `PRIORITY` int NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `STATE` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_FIRED_TRIGGERS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_JOB_DETAILS
-- 中文名称：QRTZ_JOB_DETAILS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_JOB_DETAILS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `DESCRIPTION` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_CLASS_NAME` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `IS_DURABLE` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `IS_UPDATE_DATA` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_DATA` blob COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_JOB_DETAILS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_LOCKS
-- 中文名称：QRTZ_LOCKS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_LOCKS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `LOCK_NAME` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_LOCKS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_PAUSED_TRIGGER_GRPS
-- 中文名称：QRTZ_PAUSED_TRIGGER_GRPS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_PAUSED_TRIGGER_GRPS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_PAUSED_TRIGGER_GRPS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_SCHEDULER_STATE
-- 中文名称：QRTZ_SCHEDULER_STATE 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_SCHEDULER_STATE` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `INSTANCE_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `LAST_CHECKIN_TIME` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `CHECKIN_INTERVAL` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_SCHEDULER_STATE 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_SIMPLE_TRIGGERS
-- 中文名称：QRTZ_SIMPLE_TRIGGERS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `REPEAT_COUNT` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `REPEAT_INTERVAL` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TIMES_TRIGGERED` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_SIMPLE_TRIGGERS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_SIMPROP_TRIGGERS
-- 中文名称：QRTZ_SIMPROP_TRIGGERS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_SIMPROP_TRIGGERS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `STR_PROP_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `STR_PROP_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `STR_PROP_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `INT_PROP_1` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `INT_PROP_2` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `LONG_PROP_1` bigint DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `LONG_PROP_2` bigint DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `BOOL_PROP_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `BOOL_PROP_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_SIMPROP_TRIGGERS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：QRTZ_TRIGGERS
-- 中文名称：QRTZ_TRIGGERS 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `QRTZ_TRIGGERS` (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_GROUP` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `DESCRIPTION` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `NEXT_FIRE_TIME` bigint DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `PREV_FIRE_TIME` bigint DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `PRIORITY` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_STATE` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `TRIGGER_TYPE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `START_TIME` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `END_TIME` bigint DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `CALENDAR_NAME` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `MISFIRE_INSTR` smallint DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `JOB_DATA` blob COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) USING BTREE,
  KEY `SCHED_NAME` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='QRTZ_TRIGGERS 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：aigc_word_template
-- 中文名称：Word模版
-- 业务用途：Word模版
-- ============================================================================
CREATE TABLE IF NOT EXISTS `aigc_word_template` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '模版名称',
  `code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '模版编码',
  `header` text CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci COMMENT '页眉',
  `footer` text CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci COMMENT '页脚',
  `main` text CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci COMMENT '主体内容',
  `margins` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '页边距',
  `width` int DEFAULT NULL COMMENT '宽度',
  `height` int DEFAULT NULL COMMENT '高度',
  `paper_direction` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '纸张方向 vertical纵向 horizontal横向',
  `watermark` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL COMMENT '水印',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='Word模版';

-- ============================================================================
-- 表：airag_app
-- 中文名称：airag_app 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_app` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用图标',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用类型',
  `prologue` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '开场白',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '提示词',
  `model_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型id',
  `knowledge_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '知识库',
  `flow_id` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程id（多个以逗号分隔）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态（enable=启用、disable=禁用、release=发布）',
  `msg_num` int DEFAULT NULL COMMENT '历史消息数',
  `metadata` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元数据',
  `preset_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '预设问题',
  `quick_command` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '快捷指令',
  `plugins` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '插件',
  `memory_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '记忆库(知识库的id)',
  `variables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '存放变量的配置',
  `iz_open_memory` int DEFAULT NULL COMMENT '是否开启记忆(0 不开启，1开启)',
  `memory_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '记忆和变量提示词',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='airag_app 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：airag_ext_data
-- 中文名称：通用扩展数据表
-- 业务用途：通用扩展数据表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_ext_data` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `biz_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型标识（ evaluator:评估器；track:测试追踪 ）',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `descr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述信息',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签，多个用逗号分隔',
  `data_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '实际存储内容，json',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态（run:进行中 completed：已完成）',
  `dataset_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '评测集数据',
  `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '元数据，用于存储补充业务数据信息',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  `version` int DEFAULT NULL COMMENT '版本1开始',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_biz` (`biz_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='通用扩展数据表';

-- ============================================================================
-- 表：airag_flow
-- 中文名称：airag_flow 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_flow` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  `application_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `descr` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用图标',
  `chain` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '编排规则',
  `design` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '编排设计',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态（enable=启用、disable=禁用、release=发布）',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '元数据',
  `trigger_cron` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'cron定时任务触发器配置JSON',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='airag_flow 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：airag_knowledge
-- 中文名称：airag_knowledge 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_knowledge` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '知识库名称',
  `descr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `embed_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '向量模型id',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型(knowledge知识 memory 记忆)',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '元数据',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='airag_knowledge 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：airag_knowledge_doc
-- 中文名称：airag_knowledge_doc 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_knowledge_doc` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  `knowledge_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '知识库id',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '内容',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '元数据',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='airag_knowledge_doc 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：airag_mcp
-- 中文名称：airag_mcp 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_mcp` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `descr` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '描述',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'mcp' COMMENT '类型（plugin=插件，mcp=MCP）',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'mcp类型（sse：sse类型；stdio：标准类型）',
  `endpoint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '服务端点（SSE类型为URL，stdio类型为命令）',
  `headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '请求头（sse类型）、环境变量（stdio类型）',
  `tools` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具列表',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '状态（enable=启用、disable=禁用）',
  `synced` int DEFAULT NULL COMMENT '是否同步',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '元数据',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='airag_mcp 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：airag_model
-- 中文名称：airag_model 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_model` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '供应者',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型名称',
  `credential` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '凭证信息',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API域名',
  `model_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型类型',
  `model_params` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模型参数',
  `activate_flag` int DEFAULT NULL COMMENT '是否激活（1=是，0=否）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='airag_model 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：airag_prompts
-- 中文名称：AI提示词表
-- 业务用途：AI提示词表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `airag_prompts` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键ID',
  `name` varchar(125) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提示词名称',
  `prompt_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提示词key',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提示词功能描述',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '提示词模板内容，支持变量占位符如 {{variable}}',
  `category` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提示词分类',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签，多个逗号分割',
  `model_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适配的大模型ID',
  `model_param` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '大模型的参数配置',
  `status` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0:未发布 1:已发布）',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本号(格式 0.0.1)',
  `del_flag` int DEFAULT NULL COMMENT '删除状态（0未删除 1已删除）',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  UNIQUE KEY `uni_key` (`prompt_key`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='AI提示词表';

-- ============================================================================
-- 表：ccc
-- 中文名称：ccc 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `ccc` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `has_child` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否有子节点',
  `pid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级节点',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原注释：name；名称；当前业务对象的名称；是否属于直接身份信息取决于所在表。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属部门',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='ccc 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：demo
-- 中文名称：demo 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `demo` (
  `id` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `name` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '姓名',
  `key_word` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关键词',
  `punch_time` datetime DEFAULT NULL COMMENT '打卡时间',
  `salary_money` decimal(10,3) DEFAULT NULL COMMENT '工资',
  `bonus_money` double(10,2) DEFAULT NULL COMMENT '奖金',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '性别 {男:1,女:2}',
  `age` int DEFAULT NULL COMMENT '年龄',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `email` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮箱',
  `content` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '个人简介',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '所属部门编码',
  `tenant_id` int DEFAULT '0' COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `update_count` int DEFAULT NULL COMMENT '乐观锁测试',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='demo 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：flyway_schema_history
-- 中文名称：Flyway 迁移历史表
-- 业务用途：记录 Flyway 数据库迁移执行历史；不是业务数据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `flyway_schema_history` (
  `installed_rank` int NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版本；记录或配置版本；是否为乐观锁需结合实体 @Version 判断。',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '描述；当前记录的业务内容描述。',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型；当前记录的分类类型；具体枚举值需以所在模块代码或字典为准。',
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `checksum` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'TODO：字段中文业务含义待确认',
  `execution_time` int NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `success` tinyint(1) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`installed_rank`) USING BTREE,
  KEY `flyway_schema_history_s_idx` (`success`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='Flyway 迁移历史表；记录 Flyway 数据库迁移执行历史；不是业务数据。';

-- ============================================================================
-- 表：hardware_activity
-- 中文名称：硬件活动表
-- 业务用途：保存规范化活动、步数、距离、热量、时长和心率。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hardware_activity` (
  `id` varchar(36) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `upload_batch_id` varchar(36) NOT NULL COMMENT '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。',
  `client_record_id` varchar(128) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `started_at` datetime(3) NOT NULL COMMENT '开始时间；会话、活动或信号时间窗开始时间。',
  `ended_at` datetime(3) DEFAULT NULL COMMENT '结束时间；会话、活动或信号时间窗结束时间。',
  `activity_type` varchar(64) NOT NULL COMMENT '活动类型；标识活动记录的类型；具体允许值由设备 Provider 映射定义。',
  `steps` int NOT NULL DEFAULT '0' COMMENT '步数；活动时间窗或自然日内的设备步数。',
  `distance_meters` decimal(20,3) NOT NULL DEFAULT '0.000' COMMENT '距离；活动距离，单位米。',
  `calories_kcal` decimal(20,3) NOT NULL DEFAULT '0.000' COMMENT '热量；餐食或活动能量，单位千卡。',
  `duration_minutes` int NOT NULL DEFAULT '0' COMMENT '持续时长；活动持续分钟数。',
  `average_heart_rate` decimal(10,3) DEFAULT NULL COMMENT '平均心率；活动或 ECG 测量期间的平均心率。',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `fk_hardware_activity_batch` (`upload_batch_id`),
  KEY `idx_hardware_activity_user_time` (`user_id`,`started_at`),
  KEY `idx_hardware_activity_device_time` (`device_id`,`started_at`),
  CONSTRAINT `fk_hardware_activity_batch` FOREIGN KEY (`upload_batch_id`) REFERENCES `hardware_upload_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='硬件活动表；保存规范化活动、步数、距离、热量、时长和心率。';

-- ============================================================================
-- 表：hardware_data_quality_event
-- 中文名称：硬件数据质量事件表
-- 业务用途：保存遥测质量事件、严重程度和详情码。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hardware_data_quality_event` (
  `id` varchar(36) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `upload_batch_id` varchar(36) DEFAULT NULL COMMENT '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型；标识质量、Outbox、归因或审计事件的业务类型。',
  `severity` varchar(32) NOT NULL COMMENT '严重程度；质量事件严重程度，受数据库 CHECK 约束。',
  `message` varchar(512) NOT NULL COMMENT '消息文本；保存当前事件、错误或业务消息文本。',
  `occurred_at` datetime(3) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `fk_hardware_quality_batch` (`upload_batch_id`),
  KEY `idx_hardware_quality_user_time` (`user_id`,`occurred_at`),
  KEY `idx_hardware_quality_device_time` (`device_id`,`occurred_at`),
  CONSTRAINT `fk_hardware_quality_batch` FOREIGN KEY (`upload_batch_id`) REFERENCES `hardware_upload_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='硬件数据质量事件表；保存遥测质量事件、严重程度和详情码。';

-- ============================================================================
-- 表：hardware_measurement
-- 中文名称：硬件标量测量表
-- 业务用途：保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hardware_measurement` (
  `id` varchar(36) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `upload_batch_id` varchar(36) NOT NULL COMMENT '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。',
  `client_record_id` varchar(128) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `metric_type` varchar(64) NOT NULL COMMENT '指标类型；标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。',
  `measured_at` datetime(3) NOT NULL COMMENT '测量时间；健康指标实际测量时间。',
  `primary_value` decimal(20,6) NOT NULL COMMENT '主测量值；规范化测量的主要数值，例如单值指标或血压收缩压分量。',
  `secondary_value` decimal(20,6) DEFAULT NULL COMMENT '次测量值；规范化测量的可选第二数值，例如成对测量的第二分量。',
  `unit` varchar(32) NOT NULL COMMENT '计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。',
  `quality_code` varchar(64) DEFAULT NULL COMMENT '质量代码；规范化的设备或遥测质量代码。',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `fk_hardware_measurement_batch` (`upload_batch_id`),
  KEY `idx_hardware_measurement_user_time` (`user_id`,`measured_at`),
  KEY `idx_hardware_measurement_device_time` (`device_id`,`measured_at`),
  KEY `idx_hardware_measurement_metric_time` (`metric_type`,`measured_at`),
  CONSTRAINT `fk_hardware_measurement_batch` FOREIGN KEY (`upload_batch_id`) REFERENCES `hardware_upload_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='硬件标量测量表；保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。';

-- ============================================================================
-- 表：hardware_signal_chunk_metadata
-- 中文名称：硬件信号元数据表
-- 业务用途：只保存信号时间窗、采样率和质量元数据，不保存原始波形。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hardware_signal_chunk_metadata` (
  `id` varchar(36) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `upload_batch_id` varchar(36) NOT NULL COMMENT '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `signal_type` varchar(32) NOT NULL COMMENT '信号类型；标识信号/ECG 分块或元数据的信号类别。',
  `started_at` datetime(3) NOT NULL COMMENT '开始时间；会话、活动或信号时间窗开始时间。',
  `sample_rate_hz` decimal(10,3) DEFAULT NULL COMMENT '采样率；信号采样频率，单位 Hz。',
  `sample_count` int DEFAULT NULL COMMENT '采样点数；当前信号块包含的样本数量。',
  `payload_ref` varchar(512) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `retention_expires_at` datetime(3) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `fk_hardware_signal_batch` (`upload_batch_id`),
  KEY `idx_hardware_signal_retention` (`retention_expires_at`),
  CONSTRAINT `fk_hardware_signal_batch` FOREIGN KEY (`upload_batch_id`) REFERENCES `hardware_upload_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='硬件信号元数据表；只保存信号时间窗、采样率和质量元数据，不保存原始波形。';

-- ============================================================================
-- 表：hardware_sleep_session
-- 中文名称：硬件睡眠会话表
-- 业务用途：保存规范化睡眠会话和阶段分钟数。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hardware_sleep_session` (
  `id` varchar(36) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `upload_batch_id` varchar(36) NOT NULL COMMENT '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。',
  `client_record_id` varchar(128) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `started_at` datetime(3) NOT NULL COMMENT '开始时间；会话、活动或信号时间窗开始时间。',
  `ended_at` datetime(3) NOT NULL COMMENT '结束时间；会话、活动或信号时间窗结束时间。',
  `deep_minutes` int NOT NULL DEFAULT '0' COMMENT '深睡时长；深睡阶段分钟数。',
  `light_minutes` int NOT NULL DEFAULT '0' COMMENT '浅睡时长；浅睡阶段分钟数。',
  `awake_minutes` int NOT NULL DEFAULT '0' COMMENT '清醒时长；睡眠会话内清醒分钟数。',
  `rem_minutes` int NOT NULL DEFAULT '0' COMMENT 'REM 时长；快速眼动睡眠阶段分钟数。',
  `interruption_minutes` int NOT NULL DEFAULT '0' COMMENT '中断时长；睡眠中断分钟数。',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `fk_hardware_sleep_batch` (`upload_batch_id`),
  KEY `idx_hardware_sleep_user_time` (`user_id`,`started_at`),
  KEY `idx_hardware_sleep_device_time` (`device_id`,`started_at`),
  CONSTRAINT `fk_hardware_sleep_batch` FOREIGN KEY (`upload_batch_id`) REFERENCES `hardware_upload_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='硬件睡眠会话表；保存规范化睡眠会话和阶段分钟数。';

-- ============================================================================
-- 表：hardware_upload_batch
-- 中文名称：硬件上传批次表
-- 业务用途：保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `hardware_upload_batch` (
  `id` varchar(36) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `receipt_id` varchar(36) NOT NULL COMMENT '持久化收据 ID；服务端为已接收批次生成的唯一收据标识。',
  `batch_id` varchar(128) NOT NULL COMMENT '客户端批次 ID；客户端生成的稳定遥测批次业务键，重试时保持不变。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `source` varchar(64) DEFAULT NULL COMMENT '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。',
  `collected_from` datetime(3) DEFAULT NULL COMMENT '采集窗口起点；上传批次覆盖的最早采集时间。',
  `collected_to` datetime(3) DEFAULT NULL COMMENT '采集窗口终点；上传批次覆盖的最晚采集时间。',
  `received_at` datetime(3) NOT NULL COMMENT '接收时间；服务端收到上传批次的时间。',
  `committed_at` datetime(3) NOT NULL COMMENT '持久化完成时间；批次完成约定 durable write 的时间。',
  `status` varchar(32) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `record_count` int NOT NULL COMMENT '记录总数；批次中全部规范化记录数量。',
  `measurement_count` int NOT NULL COMMENT '测量记录数；批次中的标量测量条数。',
  `sleep_session_count` int NOT NULL COMMENT '睡眠会话数；批次中的睡眠会话条数。',
  `activity_count` int NOT NULL COMMENT '活动记录数；批次中的活动记录条数。',
  `signal_chunk_count` int NOT NULL DEFAULT '0' COMMENT '信号分块数；旧 MySQL 批次记录的信号分块计数。',
  `quality_json` text COMMENT '特征质量 JSON；保存特征缺失、质量和来源等版本化元数据。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hardware_batch_owner_device` (`user_id`,`device_id`,`batch_id`),
  UNIQUE KEY `uk_hardware_batch_receipt` (`receipt_id`),
  KEY `idx_hardware_batch_user_time` (`user_id`,`collected_from`),
  KEY `idx_hardware_batch_device_time` (`device_id`,`collected_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='硬件上传批次表；保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。';

-- ============================================================================
-- 表：jeecg_order_customer
-- 中文名称：jeecg_order_customer 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jeecg_order_customer` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '客户名',
  `sex` varchar(4) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '性别',
  `idcard` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '身份证号码',
  `idcard_pic` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '身份证扫描件',
  `telphone` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '电话1',
  `order_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '外键',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jeecg_order_customer 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jeecg_order_main
-- 中文名称：jeecg_order_main 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jeecg_order_main` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `order_code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '订单号',
  `ctype` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '订单类型',
  `order_date` datetime DEFAULT NULL COMMENT '订单日期',
  `order_money` double(10,3) DEFAULT NULL COMMENT '订单金额',
  `content` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '订单备注',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `bpm_status` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '流程状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jeecg_order_main 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jeecg_order_ticket
-- 中文名称：jeecg_order_ticket 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jeecg_order_ticket` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `ticket_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '航班号',
  `tickect_date` datetime DEFAULT NULL COMMENT '航班时间',
  `order_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '外键',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jeecg_order_ticket 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_dict
-- 中文名称：jimu_dict 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_dict` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `dict_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典名称',
  `dict_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典编码',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `del_flag` int DEFAULT NULL COMMENT '删除状态',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `type` int(1) unsigned zerofill DEFAULT '0' COMMENT '字典类型0为string,1为number',
  `tenant_id` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多租户标识',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sd_dict_code` (`dict_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jimu_dict 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_dict_item
-- 中文名称：jimu_dict_item 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_dict_item` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `dict_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典id',
  `item_text` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典项文本',
  `item_value` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典项值',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT NULL COMMENT '排序',
  `status` int DEFAULT NULL COMMENT '状态（1启用 0不启用）',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间；Jeecg 公共创建时间字段。',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人；Jeecg 公共审计字段，记录最后更新用户。',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间；Jeecg 公共更新时间字段。',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sdi_role_dict_id` (`dict_id`) USING BTREE,
  KEY `idx_sdi_role_sort_order` (`sort_order`) USING BTREE,
  KEY `idx_sdi_status` (`status`) USING BTREE,
  KEY `idx_sdi_dict_val` (`dict_id`,`item_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jimu_dict_item 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_report
-- 中文名称：在线excel设计器
-- 业务用途：在线excel设计器
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '编码',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '名称',
  `note` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '说明',
  `status` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '状态',
  `type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `json_str` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'json字符串',
  `api_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求地址',
  `thumb` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '缩略图',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识0-正常,1-已删除',
  `api_method` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求方法0-get,1-post',
  `api_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求编码',
  `template` tinyint(1) DEFAULT NULL COMMENT '是否是模板 0不是,1是',
  `view_count` bigint DEFAULT '0' COMMENT '浏览次数',
  `css_str` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'css增强',
  `js_str` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'js增强',
  `py_str` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'py增强',
  `tenant_id` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多租户标识',
  `update_count` int DEFAULT '0' COMMENT '乐观锁版本',
  `submit_form` tinyint(1) DEFAULT NULL COMMENT '是否填报报表 0不是,1是',
  `is_multi_sheet` tinyint DEFAULT NULL COMMENT '是否多sheet报表 1是 0否',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_jmreport_code` (`code`) USING BTREE,
  KEY `uniq_jmreport_createby` (`create_by`) USING BTREE,
  KEY `uniq_jmreport_delflag` (`del_flag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='在线excel设计器';

-- ============================================================================
-- 表：jimu_report_category
-- 中文名称：分类
-- 业务用途：分类
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_category` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级id',
  `iz_leaf` int DEFAULT NULL COMMENT '是否为叶子节点(0 否 1是)',
  `source_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源类型( report 积木报表 screen 大屏  drag 仪表盘)',
  `del_flag` int DEFAULT NULL COMMENT '删除标识(0 正常 1 已删除)',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户id',
  `sort_no` int DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='分类';

-- ============================================================================
-- 表：jimu_report_data_source
-- 中文名称：jimu_report_data_source 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_data_source` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据源名称',
  `report_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '报表_id',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编码',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `db_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据库类型',
  `db_driver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '驱动类',
  `db_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据源地址',
  `db_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `db_password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `connect_times` int DEFAULT '0' COMMENT '连接失败次数',
  `tenant_id` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多租户标识',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型(report:报表;drag:仪表盘)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_jmdatasource_report_id` (`report_id`) USING BTREE,
  KEY `idx_jmdatasource_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='jimu_report_data_source 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_report_db
-- 中文名称：jimu_report_db 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_db` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `jimu_report_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主键字段',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `db_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据集编码',
  `db_ch_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据集名字',
  `db_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据源类型',
  `db_table_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据库表名',
  `db_dyn_sql` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '动态查询SQL',
  `db_key` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据源KEY',
  `tb_db_key` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '填报数据源',
  `tb_db_table_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '填报数据表',
  `java_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'java类数据集  类型（spring:springkey,class:java类名）',
  `java_value` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'java类数据源  数值（bean key/java类名）',
  `api_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求地址',
  `api_method` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求方法0-get,1-post',
  `is_list` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '是否是列表0否1是 默认0',
  `is_page` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否作为分页,0:不分页，1:分页',
  `db_source` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据源',
  `db_source_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据库类型 MYSQL ORACLE SQLSERVER',
  `json_data` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'json数据，直接解析json内容',
  `api_convert` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'api转换器',
  `iz_shared_source` int DEFAULT NULL COMMENT '是否为共享数据源(0 否 1 是)',
  `jimu_shared_source_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '指向共享数据集的id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_jmreportdb_db_key` (`db_key`) USING BTREE,
  KEY `idx_jimu_report_id` (`jimu_report_id`) USING BTREE,
  KEY `idx_db_source_id` (`db_source`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jimu_report_db 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_report_db_field
-- 中文名称：jimu_report_db_field 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_db_field` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `jimu_report_db_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据源ID',
  `field_name` varchar(80) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段名',
  `field_name_physics` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '物理字段名（文件数据集使用，存的是excel的字段标题）',
  `field_text` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段文本',
  `widget_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '控件类型',
  `widget_width` int DEFAULT NULL COMMENT '控件宽度',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `search_flag` int DEFAULT '0' COMMENT '查询标识0否1是 默认0',
  `search_mode` int DEFAULT NULL COMMENT '查询模式1简单2范围',
  `dict_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典编码支持从表中取数据',
  `search_value` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询默认值',
  `search_format` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询时间格式化表达式',
  `ext_json` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '参数配置',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_jrdf_jimu_report_db_id` (`jimu_report_db_id`) USING BTREE,
  KEY `idx_dbfield_order_num` (`order_num`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jimu_report_db_field 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_report_db_param
-- 中文名称：jimu_report_db_param 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_db_param` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `jimu_report_head_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '动态报表ID',
  `param_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '参数字段',
  `param_txt` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数文本',
  `param_value` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数默认值',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `search_flag` int DEFAULT NULL COMMENT '查询标识0否1是 默认0',
  `widget_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询控件类型',
  `search_mode` int DEFAULT NULL COMMENT '查询模式1简单2范围',
  `dict_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典',
  `search_format` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询时间格式化表达式',
  `ext_json` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '参数配置',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_jrdp_jimu_report_head_id` (`jimu_report_head_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='jimu_report_db_param 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：jimu_report_export_job
-- 中文名称：积木报表导出计划表
-- 业务用途：积木报表导出计划表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_export_job` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务名称',
  `begin_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `exec_interval` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '执行频率',
  `report_conf` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '导出报表配置',
  `last_run_time` datetime DEFAULT NULL COMMENT '最后执行时间',
  `receiver_email` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '接收通知的邮件',
  `file_sync_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件同步路径',
  `status` int DEFAULT NULL COMMENT '状态(0:停止;1:启动)',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `tenant_id` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多租户标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='积木报表导出计划表';

-- ============================================================================
-- 表：jimu_report_export_log
-- 中文名称：积木报表自动导出记录表
-- 业务用途：积木报表自动导出记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_export_log` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次编号',
  `export_channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '导出渠道',
  `export_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '导出类型',
  `report_id` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '报表id',
  `download_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '下载路径',
  `status` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '多租户标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='积木报表自动导出记录表';

-- ============================================================================
-- 表：jimu_report_ext_data
-- 中文名称：通用扩展数据表
-- 业务用途：通用扩展数据表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_ext_data` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `biz_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型标识，如 report_share、temp_config 等',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称，展示用',
  `descr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述信息',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签，多个用逗号分隔',
  `data_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '实际存储内容',
  `metadata` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '元数据，用于存储补充信息',
  `status` tinyint DEFAULT '1' COMMENT '状态标识：1正常 0禁用',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_biz` (`biz_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='通用扩展数据表';

-- ============================================================================
-- 表：jimu_report_icon_lib
-- 中文名称：积木图库表
-- 业务用途：积木图库表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_icon_lib` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片名称',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片类型',
  `image_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图片地址',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='积木图库表';

-- ============================================================================
-- 表：jimu_report_link
-- 中文名称：超链接配置表
-- 业务用途：超链接配置表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_link` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `report_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '积木设计器id',
  `parameter` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '参数',
  `eject_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '弹出方式（0 当前页面 1 新窗口）',
  `link_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链接名称',
  `api_method` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请求方法0-get,1-post',
  `link_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链接方式(0 网络报表 1 网络连接 2 图表联动)',
  `api_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '外网api',
  `link_chart_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联动图表的ID',
  `expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表达式',
  `requirement` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '条件',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `uniq_link_reportid` (`report_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='超链接配置表';

-- ============================================================================
-- 表：jimu_report_map
-- 中文名称：地图配置表
-- 业务用途：地图配置表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_map` (
  `id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `label` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '地图名称',
  `name` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '地图编码',
  `data` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '地图数据',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '0表示未删除,1表示删除',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '所属部门',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_jmreport_map_name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='地图配置表';

-- ============================================================================
-- 表：jimu_report_share
-- 中文名称：积木报表预览权限表
-- 业务用途：积木报表预览权限表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_share` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `report_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '在线excel设计器id',
  `preview_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '预览地址',
  `preview_lock` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码锁',
  `last_update_time` datetime DEFAULT NULL COMMENT '最后更新时间',
  `term_of_validity` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '有效期(0:永久有效，1:1天，2:7天)',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否过期(0未过期，1已过期)',
  `preview_lock_status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否为密码锁(0 否,1是)',
  `share_token` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分享token',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_report_id` (`report_id`) USING BTREE,
  UNIQUE KEY `uniq_jrs_report_id` (`report_id`) USING BTREE COMMENT '报表id唯一索引',
  KEY `idx_jrs_share_token` (`share_token`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='积木报表预览权限表';

-- ============================================================================
-- 表：jimu_report_sheet
-- 中文名称：报表Sheet表
-- 业务用途：报表Sheet表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jimu_report_sheet` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键（Sheet ID）',
  `report_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '报表ID',
  `sheet_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Sheet名称',
  `sheet_order` int NOT NULL COMMENT '排序（可以为负数，负数表示在默认sheet前面）',
  `json_str` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '该sheet的完整jsonStr',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_report_id` (`report_id`) USING BTREE,
  KEY `idx_sheet_order` (`report_id`,`sheet_order`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='报表Sheet表';

-- ============================================================================
-- 表：joa_demo
-- 中文名称：流程测试
-- 业务用途：流程测试
-- ============================================================================
CREATE TABLE IF NOT EXISTS `joa_demo` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原注释：ID；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请假人',
  `days` int DEFAULT NULL COMMENT '请假天数',
  `begin_date` datetime DEFAULT NULL COMMENT '开始时间',
  `end_date` datetime DEFAULT NULL COMMENT '请假结束时间',
  `reason` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请假原因',
  `bpm_status` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '1' COMMENT '流程状态',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='流程测试';

-- ============================================================================
-- 表：oauth2_registered_client
-- 中文名称：oauth2_registered_client 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `oauth2_registered_client` (
  `id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `client_id_issued_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'TODO：字段中文业务含义待确认',
  `client_secret` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `client_secret_expires_at` timestamp NULL DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `client_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `client_authentication_methods` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `authorization_grant_types` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `redirect_uris` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `post_logout_redirect_uris` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `scopes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `client_settings` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `token_settings` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='oauth2_registered_client 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_auth_data
-- 中文名称：onl_auth_data 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_auth_data` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `cgform_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'online表ID',
  `rule_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名',
  `rule_column` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则列',
  `rule_operator` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则条件 大于小于like',
  `rule_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则值',
  `status` int DEFAULT NULL COMMENT '1有效 0无效',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='onl_auth_data 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_auth_page
-- 中文名称：onl_auth_page 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_auth_page` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT ' 主键',
  `cgform_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'online表id',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段名/按钮编码',
  `type` int DEFAULT NULL COMMENT '1字段 2按钮',
  `control` int DEFAULT NULL COMMENT '3可编辑 5可见(仅支持两种状态值3,5)',
  `page` int DEFAULT NULL COMMENT '3列表 5表单(仅支持两种状态值3,5)',
  `status` int DEFAULT NULL COMMENT '1有效 0无效',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_onl_auth_page_code` (`code`) USING BTREE,
  KEY `idx_onl_auth_page_cgform_id` (`cgform_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='onl_auth_page 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_auth_relation
-- 中文名称：onl_auth_relation 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_auth_relation` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `role_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色id',
  `auth_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限id',
  `type` int DEFAULT NULL COMMENT '1字段 2按钮 3数据权限',
  `cgform_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'online表单ID',
  `auth_mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '授权方式role角色，depart部门，user人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='onl_auth_relation 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgform_button
-- 中文名称：Online表单自定义按钮
-- 业务用途：Online表单自定义按钮
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_button` (
  `ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `BUTTON_CODE` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮编码',
  `BUTTON_ICON` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮图标',
  `BUTTON_NAME` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮名称',
  `BUTTON_STATUS` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮状态',
  `BUTTON_STYLE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮样式',
  `EXP` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表达式',
  `CGFORM_HEAD_ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表单ID',
  `OPT_TYPE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮类型',
  `ORDER_NUM` int DEFAULT NULL COMMENT '排序',
  `OPT_POSITION` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮位置1侧面 2底部',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `idx_ocb_CGFORM_HEAD_ID` (`CGFORM_HEAD_ID`) USING BTREE,
  KEY `idx_ocb_BUTTON_CODE` (`BUTTON_CODE`) USING BTREE,
  KEY `idx_ocb_BUTTON_STATUS` (`BUTTON_STATUS`) USING BTREE,
  KEY `idx_ocb_ORDER_NUM` (`ORDER_NUM`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='Online表单自定义按钮';

-- ============================================================================
-- 表：onl_cgform_enhance_java
-- 中文名称：onl_cgform_enhance_java 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_enhance_java` (
  `ID` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `BUTTON_CODE` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮编码',
  `CG_JAVA_TYPE` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '类型',
  `CG_JAVA_VALUE` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数值',
  `CGFORM_HEAD_ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '表单ID',
  `ACTIVE_STATUS` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '1' COMMENT '生效状态',
  `EVENT` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'end' COMMENT '事件状态(end:结束，start:开始)',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `idx_ejava_cgform_head_id` (`CGFORM_HEAD_ID`) USING BTREE,
  KEY `idx_ocej_BUTTON_CODE` (`BUTTON_CODE`) USING BTREE,
  KEY `idx_ocej_ACTIVE_STATUS` (`ACTIVE_STATUS`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgform_enhance_java 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgform_enhance_js
-- 中文名称：onl_cgform_enhance_js 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_enhance_js` (
  `ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `CG_JS` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'JS增强内容',
  `CG_JS_TYPE` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型',
  `CONTENT` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `CGFORM_HEAD_ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表单ID',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `idx_ejs_cgform_head_id` (`CGFORM_HEAD_ID`) USING BTREE,
  KEY `idx_ejs_cg_js_type` (`CG_JS_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgform_enhance_js 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgform_enhance_sql
-- 中文名称：onl_cgform_enhance_sql 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_enhance_sql` (
  `ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `BUTTON_CODE` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮编码',
  `CGB_SQL` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT 'SQL内容',
  `CGB_SQL_NAME` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'Sql名称',
  `CONTENT` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `CGFORM_HEAD_ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表单ID',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `idx_oces_CGFORM_HEAD_ID` (`CGFORM_HEAD_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgform_enhance_sql 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgform_field
-- 中文名称：onl_cgform_field 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_field` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `cgform_head_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '表ID',
  `db_field_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字段名字',
  `db_field_txt` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段备注',
  `db_field_name_old` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原字段名',
  `db_is_key` tinyint(1) DEFAULT NULL COMMENT '是否主键 0否 1是',
  `db_is_null` tinyint(1) DEFAULT NULL COMMENT '是否允许为空0否 1是',
  `db_is_persist` tinyint(1) DEFAULT NULL COMMENT '是否需要同步数据库字段， 1是0否',
  `db_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数据库字段类型',
  `db_length` int NOT NULL COMMENT '数据库字段长度',
  `db_point_length` int DEFAULT NULL COMMENT '小数点',
  `db_default_val` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表字段默认值',
  `dict_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典code',
  `dict_table` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典表',
  `dict_text` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典Text',
  `field_show_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表单控件类型',
  `field_href` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '跳转URL',
  `field_length` int DEFAULT NULL COMMENT '表单控件长度',
  `field_valid_type` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表单字段校验规则',
  `field_must_input` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段是否必填',
  `field_extend_json` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '扩展参数JSON',
  `field_default_value` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '控件默认值，不同的表达式展示不同的结果。\r\n1. 纯字符串直接赋给默认值；\r\n2. #{普通变量}；\r\n3. {{ 动态JS表达式 }}；\r\n4. ${填值规则编码}；\r\n填值规则表达式只允许存在一个，且不能和其他规则混用。',
  `is_query` tinyint(1) DEFAULT NULL COMMENT '是否查询条件0否 1是',
  `is_show_form` tinyint(1) DEFAULT NULL COMMENT '表单是否显示0否 1是',
  `is_show_list` tinyint(1) DEFAULT NULL COMMENT '列表是否显示0否 1是',
  `is_read_only` tinyint(1) DEFAULT '0' COMMENT '是否是只读（1是 0否）',
  `query_mode` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询模式',
  `main_table` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '外键主表名',
  `main_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '外键主键字段',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `converter` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '自定义值转换器',
  `query_def_val` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询默认值',
  `query_dict_text` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询配置字典text',
  `query_dict_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询配置字典code',
  `query_dict_table` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询配置字典table',
  `query_show_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询显示控件',
  `query_config_flag` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否启用查询配置1是0否',
  `query_valid_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询字段校验类型',
  `query_must_input` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询字段是否必填1是0否',
  `sort_flag` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否支持排序1是0否',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ocf_cgform_head_id` (`cgform_head_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgform_field 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgform_head
-- 中文名称：onl_cgform_head 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_head` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `table_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '表名',
  `table_type` int NOT NULL COMMENT '表类型: 0单表、1主表、2附表',
  `table_version` int DEFAULT '1' COMMENT '表版本',
  `table_txt` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '表说明',
  `is_checkbox` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '是否带checkbox',
  `is_db_synch` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'N' COMMENT '同步数据库状态',
  `is_page` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '是否分页',
  `is_tree` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '是否是树',
  `id_sequence` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主键生成序列',
  `id_type` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主键类型',
  `query_mode` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询模式',
  `relation_type` int DEFAULT NULL COMMENT '映射关系 0一对多  1一对一',
  `sub_table_str` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '子表',
  `tab_order_num` int DEFAULT NULL COMMENT '附表排序序号',
  `tree_parent_id_field` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '树形表单父id',
  `tree_id_field` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '树表主键字段',
  `tree_fieldname` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '树开表单列字段',
  `form_category` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT 'bdfl_ptbd' COMMENT '表单分类',
  `form_template` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'PC表单模板',
  `form_template_mobile` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表单模板样式(移动端)',
  `scroll` int DEFAULT '0' COMMENT '是否有横向滚动条',
  `copy_version` int DEFAULT NULL COMMENT '复制版本号',
  `copy_type` int DEFAULT '0' COMMENT '复制表类型1为复制表 0为原始表',
  `physic_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原始表ID',
  `ext_config_json` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '扩展JSON',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `theme_template` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主题模板',
  `is_des_form` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否用设计器表单',
  `des_form_code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '设计器表单编码',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  `low_app_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关联的应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_och_cgform_head_id` (`table_name`) USING BTREE,
  KEY `idx_och_table_name` (`form_template`) USING BTREE,
  KEY `idx_och_form_template_mobile` (`form_template_mobile`) USING BTREE,
  KEY `idx_och_table_version` (`table_version`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgform_head 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgform_index
-- 中文名称：onl_cgform_index 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgform_index` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `cgform_head_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主表id',
  `index_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '索引名称',
  `index_name_old` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原索引名称',
  `index_field` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '索引栏位',
  `index_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '索引类型',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `is_db_synch` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'N' COMMENT '是否同步数据库 N未同步 Y已同步',
  `del_flag` int DEFAULT '0' COMMENT '是否删除 0未删除 1删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_oci_cgform_head_id` (`cgform_head_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgform_index 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgreport_head
-- 中文名称：onl_cgreport_head 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgreport_head` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '报表编码',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '报表名字',
  `cgr_sql` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '报表SQL',
  `return_val_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '返回值字段',
  `return_txt_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '返回文本字段',
  `return_type` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '1' COMMENT '返回类型，单选或多选',
  `db_source` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '动态数据源',
  `content` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  `low_app_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关联的应用ID',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `index_onlinereport_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgreport_head 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgreport_item
-- 中文名称：onl_cgreport_item 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgreport_item` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `cgrhead_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '报表ID',
  `field_name` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字段名字',
  `field_txt` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段文本',
  `field_width` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `field_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段类型',
  `search_mode` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询模式',
  `is_order` int DEFAULT '0' COMMENT '是否排序  0否,1是',
  `is_search` int DEFAULT '0' COMMENT '是否查询  0否,1是',
  `dict_code` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典CODE',
  `field_href` varchar(120) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段跳转URL',
  `is_show` int DEFAULT '1' COMMENT '是否显示  0否,1显示',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `replace_val` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '取值表达式',
  `is_total` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否合计 0否,1是（仅对数值有效）',
  `group_title` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '分组标题',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_oci_cgrhead_id` (`cgrhead_id`) USING BTREE,
  KEY `idx_oci_is_show` (`is_show`) USING BTREE,
  KEY `idx_oci_order_num` (`order_num`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgreport_item 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_cgreport_param
-- 中文名称：onl_cgreport_param 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_cgreport_param` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `cgrhead_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '动态报表ID',
  `param_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '参数字段',
  `param_txt` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数文本',
  `param_value` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数默认值',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ocp_cgrhead_id` (`cgrhead_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_cgreport_param 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_drag_comp
-- 中文名称：组件库
-- 业务用途：组件库
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_comp` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `comp_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件名称',
  `comp_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `type_id` int DEFAULT NULL COMMENT '组件类型',
  `comp_config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组件配置',
  `status` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态0:无效 1:有效',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='组件库';

-- ============================================================================
-- 表：onl_drag_dataset_head
-- 中文名称：onl_drag_dataset_head 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_dataset_head` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '名称',
  `code` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '编码',
  `parent_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父id',
  `db_source` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '动态数据源',
  `query_sql` varchar(5000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '查询数据SQL',
  `content` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `iz_agent` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '原注释：iz_agent；TODO：字段中文业务含义待确认',
  `data_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据类型',
  `api_method` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'api方法：get/post',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间；Jeecg 公共创建时间字段。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间；Jeecg 公共更新时间字段。',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人；Jeecg 公共审计字段，记录最后更新用户。',
  `low_app_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '应用ID',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_drag_dataset_head 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_drag_dataset_item
-- 中文名称：onl_drag_dataset_item 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_dataset_item` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `head_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主表ID',
  `field_name` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段名',
  `field_txt` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段文本',
  `field_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段类型',
  `widget_type` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '控件类型',
  `dict_code` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典Code',
  `dict_table` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `dict_text` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `iz_show` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否列表显示',
  `iz_search` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否查询',
  `iz_total` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否计算总计（仅对数值有效）',
  `search_mode` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询模式',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_oddi_head_id` (`head_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_drag_dataset_item 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_drag_dataset_param
-- 中文名称：onl_drag_dataset_param 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_dataset_param` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `head_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '动态报表ID',
  `param_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '参数字段',
  `param_txt` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数文本',
  `param_value` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数默认值',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `iz_search` int DEFAULT NULL COMMENT '查询标识0否1是 默认0',
  `widget_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询控件类型',
  `search_mode` int DEFAULT NULL COMMENT '查询模式1简单2范围',
  `dict_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_oddp_head_id` (`head_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_drag_dataset_param 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_drag_page
-- 中文名称：可视化拖拽界面
-- 业务用途：可视化拖拽界面
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_page` (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '界面名称',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '访问路径',
  `background_color` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '背景色',
  `background_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '背景图',
  `design_type` int DEFAULT NULL COMMENT '设计模式(1:pc,2:手机,3:平板)',
  `theme` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主题色',
  `style` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '面板主题',
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '封面图',
  `des_json` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '仪表盘主配置JSON',
  `template` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '布局json',
  `protection_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '保护码',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属分类',
  `iz_template` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '是否模板(1:是；0不是)',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `low_app_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用ID',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  `update_count` int DEFAULT '1' COMMENT 'TODO：字段中文业务含义待确认',
  `visits_num` int DEFAULT NULL COMMENT '访问次数',
  `del_flag` int DEFAULT NULL COMMENT '删除状态( 0未删除 1已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='可视化拖拽界面';

-- ============================================================================
-- 表：onl_drag_page_comp
-- 中文名称：可视化拖拽页面组件
-- 业务用途：可视化拖拽页面组件
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_page_comp` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父组件ID',
  `page_Id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '界面ID',
  `comp_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件库ID',
  `component` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件名称',
  `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '组件配置',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='可视化拖拽页面组件';

-- ============================================================================
-- 表：onl_drag_share
-- 中文名称：仪表盘预览分享表
-- 业务用途：仪表盘预览分享表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_share` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `drag_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '在线仪表盘设计器id',
  `preview_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '预览地址',
  `preview_lock` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码锁',
  `last_update_time` datetime DEFAULT NULL COMMENT '最后更新时间',
  `term_of_validity` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '有效期(0:永久有效，1:1天，7:7天)',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否过期(0未过期，1已过期)',
  `preview_lock_status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否为密码锁(0 否,1是)',
  `share_token` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分享token',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_ods_drag_id` (`drag_id`) USING BTREE COMMENT '仪表盘id唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='仪表盘预览分享表';

-- ============================================================================
-- 表：onl_drag_table_relation
-- 中文名称：仪表盘聚合表
-- 业务用途：仪表盘聚合表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_drag_table_relation` (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `aggregation_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '聚合表名称',
  `aggregation_desc` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '聚合表描述',
  `relation_forms` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '关联表单',
  `filter_condition` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '过滤条件',
  `header_fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '表头字段',
  `calculate_fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '公式字段',
  `validate_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '校验信息',
  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除状态(0-正常,1-已删除)',
  `low_app_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '应用ID',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_aggregation_name` (`aggregation_name`) USING BTREE,
  KEY `idx_del_flag` (`del_flag`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_create_by` (`create_by`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='仪表盘聚合表';

-- ============================================================================
-- 表：onl_graphreport_head
-- 中文名称：onl_graphreport_head 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_graphreport_head` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '图表名称',
  `code` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '图表编码',
  `cgr_sql` varchar(5000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '查询数据SQL',
  `xaxis_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT 'X轴数据字段',
  `yaxis_field` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT 'Y轴数据字段',
  `yaxis_text` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT 'y轴文字描述',
  `content` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `extend_js` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '扩展JS',
  `graph_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图表类型',
  `is_combination` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'combination' COMMENT '是否组合',
  `display_template` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '展示模板',
  `data_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据类型',
  `db_source` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '动态数据源',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  `low_app_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关联的应用ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间；Jeecg 公共创建时间字段。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间；Jeecg 公共更新时间字段。',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人；Jeecg 公共审计字段，记录最后更新用户。',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_gpreport_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_graphreport_head 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_graphreport_item
-- 中文名称：onl_graphreport_item 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_graphreport_item` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `graphreport_head_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主表ID',
  `field_name` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段名',
  `field_txt` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段文本',
  `is_show` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否列表显示',
  `is_total` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否计算总计（仅对数值有效）',
  `search_flag` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否查询',
  `search_mode` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '查询模式',
  `dict_code` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典Code',
  `field_href` varchar(120) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段href',
  `field_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段类型',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `replace_val` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '取值表达式',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ogi_graphreport_head_id` (`graphreport_head_id`) USING BTREE,
  KEY `idx_ogi_is_show` (`is_show`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_graphreport_item 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_graphreport_params
-- 中文名称：Online图表：参数表
-- 业务用途：Online图表：参数表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_graphreport_params` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `head_id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'Online图表ID',
  `param_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '参数字段',
  `param_txt` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数文本',
  `param_value` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数默认值',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `onl_graphreport_param_head_id` (`head_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='Online图表：参数表';

-- ============================================================================
-- 表：onl_graphreport_templet
-- 中文名称：onl_graphreport_templet 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_graphreport_templet` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `templet_code` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `templet_name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '报表名称',
  `templet_style` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '报表风格模板（单排、双排、Tab模式、分组模式-根据配置动态展示、可自定义...）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间；Jeecg 公共创建时间字段。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间；Jeecg 公共更新时间字段。',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人；Jeecg 公共审计字段，记录最后更新用户。',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_graphreport_templet 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：onl_graphreport_templet_item
-- 中文名称：onl_graphreport_templet_item 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `onl_graphreport_templet_item` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `graphreport_templet_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `graphreport_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图表编码',
  `graphreport_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '图表类型（饼状图、曲线图、柱状图、数据列表等）',
  `group_num` int DEFAULT NULL COMMENT '组合数字，默认值0 非必填',
  `group_style` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组合展示风格（1 卡片，2 tab）非必填',
  `group_txt` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '分组描述',
  `order_num` int DEFAULT NULL COMMENT '排序',
  `is_show` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否显示 1显示 0不显示，默认1',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间；Jeecg 公共创建时间字段。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间；Jeecg 公共更新时间字段。',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人；Jeecg 公共审计字段，记录最后更新用户。',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ogti_grreport_tempid` (`graphreport_templet_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='onl_graphreport_templet_item 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：open_api
-- 中文名称：接口表
-- 业务用途：接口表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `open_api` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接口名称',
  `request_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接口地址',
  `white_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'IP白名单，支持IP、CIDR、通配符，逗号或换行分隔',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '白名单备注说明',
  `body` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求体内容',
  `origin_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始地址',
  `status` int DEFAULT NULL COMMENT '状态',
  `del_flag` int DEFAULT NULL COMMENT '删除标识',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `headers_json` json DEFAULT NULL COMMENT '请求头json',
  `params_json` json DEFAULT NULL COMMENT '请求参数json',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='接口表';

-- ============================================================================
-- 表：open_api_auth
-- 中文名称：权限表
-- 业务用途：权限表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `open_api_auth` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授权名称',
  `ak` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原注释：AK；TODO：字段中文业务含义待确认',
  `sk` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原注释：SK；TODO：字段中文业务含义待确认',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `system_user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联系统用户名',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='权限表';

-- ============================================================================
-- 表：open_api_log
-- 中文名称：调用记录表
-- 业务用途：调用记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `open_api_log` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `api_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接口ID',
  `call_auth_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调用ID',
  `call_time` datetime DEFAULT NULL COMMENT '调用时间',
  `used_time` bigint DEFAULT NULL COMMENT '耗时',
  `response_time` datetime DEFAULT NULL COMMENT '响应时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='调用记录表';

-- ============================================================================
-- 表：open_api_permission
-- 中文名称：openapi授权
-- 业务用途：openapi授权
-- ============================================================================
CREATE TABLE IF NOT EXISTS `open_api_permission` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `api_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '接口ID',
  `api_auth_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认证ID',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='openapi授权';

-- ============================================================================
-- 表：oss_file
-- 中文名称：oss_file 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `oss_file` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `file_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件名称',
  `url` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件地址',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='oss_file 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_ai_conversation
-- 中文名称：服务端健康问答会话表
-- 业务用途：保存按租户和用户隔离的权威健康问答会话。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_ai_conversation` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `title` varchar(128) NOT NULL COMMENT '标题；当前会话、研究、报告或业务对象的展示标题。',
  `status` varchar(32) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `summary_text` longtext COMMENT '摘要文本；保存当前结果的人类可读摘要。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  KEY `idx_rehealth_ai_conversation_owner_updated` (`tenant_id`,`user_id`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务端健康问答会话表；保存按租户和用户隔离的权威健康问答会话。';

-- ============================================================================
-- 表：rehealth_ai_message
-- 中文名称：服务端健康问答消息表
-- 业务用途：保存健康问答完整消息历史、请求幂等键、Provider 和模型版本。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_ai_message` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `conversation_id` varchar(64) NOT NULL COMMENT '会话 ID；标识健康问答会话；服务端物理关联 rehealth_ai_conversation.id。',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `request_id` varchar(128) NOT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `role` varchar(16) NOT NULL COMMENT '消息角色；标识健康问答消息发送方角色；服务端和本地会话代码据此组装上下文。',
  `content` text NOT NULL COMMENT '消息内容；保存当前健康问答消息正文。',
  `status` varchar(32) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `provider` varchar(128) DEFAULT NULL COMMENT '服务提供方；标识产生消息、模型结果或设备数据的 Provider。',
  `model_version` varchar(128) DEFAULT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `retryable` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_ai_message_request_role` (`conversation_id`,`request_id`,`role`),
  KEY `idx_rehealth_ai_message_owner_created` (`tenant_id`,`user_id`,`created_at`),
  KEY `idx_rehealth_ai_message_conversation_created` (`conversation_id`,`created_at`),
  CONSTRAINT `fk_rehealth_ai_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `rehealth_ai_conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务端健康问答消息表；保存健康问答完整消息历史、请求幂等键、Provider 和模型版本。';

-- ============================================================================
-- 表：rehealth_attribution_event
-- 中文名称：归因请求事件表
-- 业务用途：保存提交给 PIAS 的个体归因请求元数据和版本化输入快照。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_attribution_event` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `attribution_request_id` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `event_date` varchar(32) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `risk_score` double NOT NULL COMMENT '风险分数；模型返回的风险数值；解释范围和概率语义必须以模型契约为准。',
  `intervention_id` varchar(128) DEFAULT NULL COMMENT '干预行动 ID；标识用户反馈所针对的具体干预行动。',
  `adherence` double DEFAULT NULL COMMENT '依从性；用户反馈中记录的干预执行或依从情况。',
  `baseline_risk_score` double DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `idx_rehealth_attribution_user_date` (`user_id`,`event_date`),
  KEY `idx_rehealth_attribution_request` (`attribution_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='归因请求事件表；保存提交给 PIAS 的个体归因请求元数据和版本化输入快照。';

-- ============================================================================
-- 表：rehealth_attribution_result
-- 中文名称：个体归因结果表
-- 业务用途：保存 PIAS 个体归因结果及模型证据快照。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_attribution_result` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `status` varchar(64) DEFAULT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `model_version` varchar(128) DEFAULT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `request_id` varchar(128) DEFAULT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `attribution_mode` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `is_mock` tinyint(1) DEFAULT NULL COMMENT '是否模拟数据；明确标识结果是否来自 Mock/合成路径；生产结果不得为真。',
  `provider` varchar(128) DEFAULT NULL COMMENT '服务提供方；标识产生消息、模型结果或设备数据的 Provider。',
  `history_days` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `min_history_days` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `intervention_days` int DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `intervention_data_sufficient` tinyint(1) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `current_risk_score` double DEFAULT NULL COMMENT '当前风险分数；保险风险查询中读取的最新已确认 CVD 风险分数。',
  `current_risk_level` varchar(64) DEFAULT NULL COMMENT '当前风险等级；保险风险查询中读取的最新已确认风险等级。',
  `current_trend` varchar(64) DEFAULT NULL COMMENT '当前趋势；当前业务对象的描述性趋势；不表示因果或诊断。',
  `individual_att` double DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `trend_delta` double DEFAULT NULL COMMENT '趋势变化值；当前风险或指标相对既定历史参考的变化量。',
  `adherence_average` double DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `interpretation` longtext COMMENT 'TODO：字段中文业务含义待确认',
  `error_code` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `retryable` tinyint(1) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `request_json` longtext NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `response_json` longtext NOT NULL COMMENT '响应证据 JSON；保存模型或 Provider 的版本化结构化响应快照。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `idx_attribution_result_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='个体归因结果表；保存 PIAS 个体归因结果及模型证据快照。';

-- ============================================================================
-- 表：rehealth_behavior_record
-- 中文名称：结构化行为记录表
-- 业务用途：保存拍照食物/OCR 的已验证结构化结果；不保存原始图片。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_behavior_record` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `request_id` varchar(128) NOT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `category` varchar(32) NOT NULL COMMENT '分类；当前行为、干预或业务记录的分类；具体枚举待对应代码确认。',
  `title` varchar(255) NOT NULL COMMENT '标题；当前会话、研究、报告或业务对象的展示标题。',
  `summary` varchar(2000) DEFAULT NULL COMMENT '摘要；保存当前结果的结构化或可展示摘要。',
  `items_json` longtext COMMENT '干预行动列表 JSON；保存有序结构化干预行动和证据引用。',
  `calories_kcal` decimal(10,2) DEFAULT NULL COMMENT '热量；餐食或活动能量，单位千卡。',
  `protein_grams` decimal(10,2) DEFAULT NULL COMMENT '蛋白质；餐食蛋白质估计值，单位克。',
  `carbohydrate_grams` decimal(10,2) DEFAULT NULL COMMENT '碳水化合物；餐食碳水化合物估计值，单位克。',
  `fat_grams` decimal(10,2) DEFAULT NULL COMMENT '脂肪；餐食脂肪估计值，单位克。',
  `ocr_text` longtext COMMENT 'TODO：字段中文业务含义待确认',
  `confidence` double DEFAULT NULL COMMENT '置信度；当前特征、因素、识别结果或计划的可信程度。',
  `model_version` varchar(128) NOT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `occurred_at` datetime(3) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_behavior_owner_request` (`tenant_id`,`user_id`,`request_id`),
  KEY `idx_rehealth_behavior_owner_occurred` (`tenant_id`,`user_id`,`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结构化行为记录表；保存拍照食物/OCR 的已验证结构化结果；不保存原始图片。';

-- ============================================================================
-- 表：rehealth_care_plan
-- 中文名称：机构干预计划主表
-- 业务用途：保存按租户、机构类型和服务对象隔离的计划聚合、当前/草稿版本指针及乐观锁。
-- 逻辑关联：current_revision_id -> rehealth_care_plan_revision.id（当前最新已发布版本逻辑外键）
-- 逻辑关联：draft_revision_id -> rehealth_care_plan_revision.id（单一可变草稿版本逻辑外键）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_care_plan` (
  `id` varchar(64) NOT NULL COMMENT '关怀计划主键',
  `tenant_id` int NOT NULL COMMENT '所属 Jeecg 租户 ID',
  `owner_type` varchar(32) NOT NULL COMMENT '计划所属机构类型：保险、医疗或个人',
  `owner_org_ref` varchar(64) NOT NULL COMMENT '所属机构引用；保险机构当前使用租户 ID',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '租户范围内的服务对象引用',
  `rehealth_user_id` varchar(64) NOT NULL COMMENT '由可信服务关系解析的 ReHealth APP 用户 ID',
  `source_plan_id` varchar(128) DEFAULT NULL COMMENT '可选的历史计划或外部计划标识',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '计划生命周期状态：草稿、生效或已撤回',
  `current_revision_id` varchar(64) DEFAULT NULL COMMENT '最新发布版本 ID；该版本可在未来时间生效',
  `draft_revision_id` varchar(64) DEFAULT NULL COMMENT '当前唯一可编辑的草稿版本 ID',
  `lock_version` bigint NOT NULL DEFAULT '0' COMMENT '计划全部变更使用的乐观锁版本号',
  `created_by` varchar(64) NOT NULL COMMENT '创建计划的认证用户 ID',
  `created_at` datetime(3) NOT NULL COMMENT '计划创建时间',
  `updated_by` varchar(64) NOT NULL COMMENT '最后更新计划的认证用户 ID',
  `updated_at` datetime(3) NOT NULL COMMENT '计划最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_care_plan_subject` (`tenant_id`,`owner_type`,`subject_ref`,`status`,`updated_at`),
  KEY `idx_care_plan_user` (`tenant_id`,`rehealth_user_id`,`status`,`updated_at`),
  KEY `idx_care_plan_current_revision` (`tenant_id`,`current_revision_id`),
  KEY `idx_care_plan_draft_revision` (`tenant_id`,`draft_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='按租户隔离并支持乐观锁的机构关怀计划主表';

-- ============================================================================
-- 表：rehealth_care_plan_audit_event
-- 中文名称：机构干预计划审计表
-- 业务用途：保存不含计划正文的版本生命周期操作、内容哈希和变更原因。
-- 逻辑关联：plan_id -> rehealth_care_plan.id（计划版本审计所属聚合逻辑外键）
-- 逻辑关联：revision_id -> rehealth_care_plan_revision.id（计划版本审计目标版本逻辑外键）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_care_plan_audit_event` (
  `id` varchar(64) NOT NULL COMMENT '计划审计事件主键',
  `tenant_id` int NOT NULL COMMENT '所属 Jeecg 租户 ID',
  `owner_type` varchar(32) NOT NULL COMMENT '用于审计筛选的计划所属机构类型',
  `actor_user_id` varchar(64) NOT NULL COMMENT '执行操作的认证用户 ID',
  `action` varchar(64) NOT NULL COMMENT '版本操作，例如创建草稿、更新草稿、克隆版本、发布或撤回',
  `plan_id` varchar(64) NOT NULL COMMENT '受影响的关怀计划 ID',
  `revision_id` varchar(64) DEFAULT NULL COMMENT '受影响的计划版本 ID',
  `before_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '操作前的计划内容摘要',
  `after_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '操作后的计划内容摘要',
  `reason` varchar(1000) DEFAULT NULL COMMENT '长度受限的机构变更或撤回原因',
  `created_at` datetime(3) NOT NULL COMMENT '审计事件创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_care_plan_audit_plan` (`tenant_id`,`plan_id`,`created_at`),
  KEY `idx_care_plan_audit_actor` (`tenant_id`,`actor_user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仅追加写入的关怀计划版本生命周期审计表';

-- ============================================================================
-- 表：rehealth_care_plan_execution
-- 中文名称：机构干预执行事实表
-- 业务用途：保存对具体任务实例的完成、部分完成、跳过或不适用评分，作为滚动 28 日依从性分子。
-- 逻辑关联：occurrence_id -> rehealth_care_plan_occurrence.id（执行评分所属任务实例逻辑外键）
-- 逻辑关联：plan_id -> rehealth_care_plan.id（执行评分所属计划逻辑外键）
-- 逻辑关联：revision_id -> rehealth_care_plan_revision.id（执行评分所属版本逻辑外键）
-- 逻辑关联：plan_item_id -> rehealth_care_plan_item.id（执行评分所属版本项目逻辑外键）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_care_plan_execution` (
  `id` varchar(64) NOT NULL COMMENT '计划任务执行事实主键',
  `tenant_id` int NOT NULL COMMENT '从任务实例复制的所属 Jeecg 租户 ID',
  `occurrence_id` varchar(64) NOT NULL COMMENT '被评分的计划任务实例 ID',
  `plan_id` varchar(64) NOT NULL COMMENT '执行时所属关怀计划 ID',
  `revision_id` varchar(64) NOT NULL COMMENT '执行时所属已发布版本 ID',
  `plan_item_id` varchar(64) NOT NULL COMMENT '执行时所属版本内计划项目 ID',
  `logical_item_id` varchar(64) NOT NULL COMMENT '跨版本保持稳定的逻辑项目 ID',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '租户范围内的服务对象引用',
  `feedback_type` varchar(32) NOT NULL COMMENT '执行评分：completed、partially_completed、skipped 或 not_applicable',
  `score_value` decimal(5,4) DEFAULT NULL COMMENT '依从性计分值：1、0.5、0；不适用为 NULL',
  `verification_type` varchar(32) NOT NULL DEFAULT 'self_report' COMMENT '执行事实核验方式：用户自报、设备核验或人员确认',
  `note` varchar(1000) DEFAULT NULL COMMENT '用户可选的有界执行备注，不保存原始健康遥测',
  `occurred_at` datetime(3) NOT NULL COMMENT '用户执行或提交评分的业务时间',
  `source_system` varchar(64) NOT NULL DEFAULT 'rehealth_app' COMMENT '执行事实来源系统',
  `source_record_id` varchar(128) NOT NULL COMMENT '来源系统内的幂等记录 ID',
  `created_at` datetime(3) NOT NULL COMMENT '执行事实写入服务端的时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_care_plan_execution_source` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_care_plan_execution_occurrence` (`tenant_id`,`occurrence_id`,`occurred_at`),
  KEY `idx_care_plan_execution_subject_time` (`tenant_id`,`subject_ref`,`occurred_at`),
  KEY `idx_care_plan_execution_plan_time` (`tenant_id`,`plan_id`,`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='独立于计划版本内容的任务执行评分事实表，用于滚动二十八日依从性';

-- ============================================================================
-- 表：rehealth_care_plan_item
-- 中文名称：机构干预计划项目表
-- 业务用途：保存绑定到具体版本的患者可见计划项目快照及稳定逻辑项目标识。
-- 逻辑关联：plan_id -> rehealth_care_plan.id（计划项目所属聚合逻辑外键）
-- 逻辑关联：revision_id -> rehealth_care_plan_revision.id（计划项目所属不可变版本逻辑外键）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_care_plan_item` (
  `id` varchar(64) NOT NULL COMMENT '版本内计划项目主键',
  `tenant_id` int NOT NULL COMMENT '从计划主表复制的所属 Jeecg 租户 ID',
  `plan_id` varchar(64) NOT NULL COMMENT '所属关怀计划 ID',
  `revision_id` varchar(64) NOT NULL COMMENT '包含该不可变项目快照的计划版本 ID',
  `logical_item_id` varchar(64) NOT NULL COMMENT '克隆新版本时保持不变的逻辑项目 ID',
  `category` varchar(32) NOT NULL COMMENT '保守干预分类，例如运动、营养、睡眠或随访',
  `title` varchar(255) NOT NULL COMMENT '用户可见的计划项目标题',
  `instructions` varchar(4000) DEFAULT NULL COMMENT '长度受限的用户可见执行说明',
  `schedule_json` longtext COMMENT '结构化计划规则，由独立任务实例生成器展开',
  `scoring_weight` decimal(10,3) NOT NULL DEFAULT '1.000' COMMENT '每个已生成任务实例的依从性计分权重',
  `allow_not_applicable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '用户是否可以将任务标记为不适用',
  `display_order` int NOT NULL COMMENT '当前版本内稳定的展示顺序',
  `created_at` datetime(3) NOT NULL COMMENT '计划项目快照创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_care_plan_item_logical` (`tenant_id`,`revision_id`,`logical_item_id`),
  UNIQUE KEY `uk_care_plan_item_order` (`tenant_id`,`revision_id`,`display_order`),
  KEY `idx_care_plan_item_plan` (`tenant_id`,`plan_id`,`revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定具体版本的用户可见关怀计划项目快照表';

-- ============================================================================
-- 表：rehealth_care_plan_occurrence
-- 中文名称：机构干预任务实例表
-- 业务用途：保存绑定计划版本和项目的到期任务实例，为后续真实依从性分母提供稳定标识。
-- 逻辑关联：plan_id -> rehealth_care_plan.id（任务实例所属计划逻辑外键）
-- 逻辑关联：revision_id -> rehealth_care_plan_revision.id（任务实例生成版本逻辑外键）
-- 逻辑关联：plan_item_id -> rehealth_care_plan_item.id（任务实例生成项目逻辑外键）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_care_plan_occurrence` (
  `id` varchar(64) NOT NULL COMMENT '用于反馈幂等的计划任务实例主键',
  `tenant_id` int NOT NULL COMMENT '从计划主表复制的所属 Jeecg 租户 ID',
  `plan_id` varchar(64) NOT NULL COMMENT '所属关怀计划 ID',
  `revision_id` varchar(64) NOT NULL COMMENT '生成该任务实例的已发布版本 ID',
  `plan_item_id` varchar(64) NOT NULL COMMENT '生成该任务实例的版本内计划项目 ID',
  `logical_item_id` varchar(64) NOT NULL COMMENT '跨计划版本保持稳定的逻辑项目 ID',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '租户范围内的服务对象引用',
  `scheduled_at` datetime(3) NOT NULL COMMENT '按统一服务端时间记录的计划执行时间',
  `due_at` datetime(3) NOT NULL COMMENT '用于计算依从性时间窗口的截止时间',
  `status` varchar(32) NOT NULL DEFAULT 'scheduled' COMMENT '任务实例状态：待执行或已取消；执行事实单独存储',
  `exclusion_reason` varchar(128) DEFAULT NULL COMMENT '已取消任务不计入依从性的原因',
  `created_at` datetime(3) NOT NULL COMMENT '任务实例创建时间',
  `updated_at` datetime(3) NOT NULL COMMENT '任务实例最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_care_plan_occurrence_due` (`tenant_id`,`plan_item_id`,`scheduled_at`),
  KEY `idx_care_plan_occurrence_subject_due` (`tenant_id`,`subject_ref`,`status`,`due_at`),
  KEY `idx_care_plan_occurrence_revision` (`tenant_id`,`revision_id`,`status`,`scheduled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绑定计划版本并构成未来依从性分母的到期任务实例表';

-- ============================================================================
-- 表：rehealth_care_plan_revision
-- 中文名称：机构干预计划版本表
-- 业务用途：保存草稿、已发布和已撤回的计划版本；已发布内容不可原地覆盖。
-- 逻辑关联：plan_id -> rehealth_care_plan.id（计划版本所属聚合逻辑外键）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_care_plan_revision` (
  `id` varchar(64) NOT NULL COMMENT '计划版本主键',
  `tenant_id` int NOT NULL COMMENT '从计划主表复制的所属 Jeecg 租户 ID',
  `plan_id` varchar(64) NOT NULL COMMENT '所属关怀计划 ID',
  `revision_no` int NOT NULL COMMENT '计划内单调递增的版本序号',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '版本状态：草稿、已发布或已撤回',
  `title` varchar(255) NOT NULL COMMENT '用户可见的计划标题',
  `summary` varchar(2000) DEFAULT NULL COMMENT '长度受限的用户可见计划摘要',
  `change_reason` varchar(1000) DEFAULT NULL COMMENT '机构填写的本次版本变更原因',
  `content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '版本元数据及有序计划项目的 SHA-256 摘要',
  `effective_from` datetime(3) DEFAULT NULL COMMENT '发布时设置的版本生效时间，包含该时间点',
  `effective_to` datetime(3) DEFAULT NULL COMMENT '由新版本或撤回设置的失效时间，不包含该时间点',
  `published_by` varchar(64) DEFAULT NULL COMMENT '发布版本的认证用户 ID',
  `published_at` datetime(3) DEFAULT NULL COMMENT '版本发布时间',
  `withdrawn_by` varchar(64) DEFAULT NULL COMMENT '撤回版本的认证用户 ID',
  `withdrawn_at` datetime(3) DEFAULT NULL COMMENT '版本撤回时间',
  `created_by` varchar(64) NOT NULL COMMENT '创建版本的认证用户 ID',
  `created_at` datetime(3) NOT NULL COMMENT '版本创建时间',
  `updated_by` varchar(64) NOT NULL COMMENT '最后编辑草稿的认证用户 ID',
  `updated_at` datetime(3) NOT NULL COMMENT '版本最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_care_plan_revision_no` (`tenant_id`,`plan_id`,`revision_no`),
  KEY `idx_care_plan_revision_effective` (`tenant_id`,`plan_id`,`status`,`effective_from`,`effective_to`),
  KEY `idx_care_plan_revision_hash` (`tenant_id`,`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发布后内容不可变的机构关怀计划版本表';

-- ============================================================================
-- 表：rehealth_cvd_feature_vector
-- 中文名称：CVD 特征向量表
-- 业务用途：保存一次 CVD-16 评估使用的版本化特征向量和质量证据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_cvd_feature_vector` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `request_id` varchar(128) NOT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `feature_schema_version` varchar(64) NOT NULL COMMENT '特征协议版本；标识特征向量遵循的字段协议版本。',
  `feature_json` longtext NOT NULL COMMENT '特征向量 JSON；保存一次模型评估实际使用的版本化特征向量。',
  `quality_json` longtext COMMENT '特征质量 JSON；保存特征缺失、质量和来源等版本化元数据。',
  `payload_json` longtext NOT NULL COMMENT '载荷 JSON；保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_feature_user_request` (`user_id`,`request_id`),
  KEY `idx_feature_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CVD 特征向量表；保存一次 CVD-16 评估使用的版本化特征向量和质量证据。';

-- ============================================================================
-- 表：rehealth_cvd_risk_result
-- 中文名称：CVD 风险结果表
-- 业务用途：保存模型风险分数、等级、模型贡献、Factor16 贡献、警告和模型版本。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_cvd_risk_result` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `feature_vector_id` varchar(64) NOT NULL COMMENT '特征向量记录 ID；物理关联 rehealth_cvd_feature_vector.id。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `request_id` varchar(128) NOT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `feature_schema_version` varchar(64) NOT NULL COMMENT '特征协议版本；标识特征向量遵循的字段协议版本。',
  `model_version` varchar(128) NOT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `scorer_mode` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `is_mock` tinyint(1) DEFAULT NULL COMMENT '是否模拟数据；明确标识结果是否来自 Mock/合成路径；生产结果不得为真。',
  `artifact_name` varchar(255) DEFAULT NULL COMMENT '模型制品名称；标识产生结果时使用的已加载模型制品。',
  `fallback_reason` varchar(512) DEFAULT NULL COMMENT '回退原因；记录模型为何使用回退路径；生产不得静默伪装 Mock。',
  `contribution_method` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `factor_contribution_version` varchar(64) DEFAULT NULL COMMENT 'Factor16 规则版本；标识产生 Factor16 贡献的规则版本。',
  `risk_score` double NOT NULL COMMENT '风险分数；模型返回的风险数值；解释范围和概率语义必须以模型契约为准。',
  `risk_level` varchar(64) NOT NULL COMMENT '风险等级；模型基于风险分数返回的离散等级；完整枚举待模型契约确认。',
  `contribution_json` longtext COMMENT '模型贡献 JSON；保存模型原始特征贡献，用于模型审计。',
  `factor_contribution_json` longtext COMMENT 'Factor16 贡献 JSON；保存独立 Factor16 规则的逐字段贡献。',
  `factor_measured_component_json` longtext COMMENT 'Factor16 实测分量 JSON；保存 Factor16 中经确认实测部分的贡献分量。',
  `factor_control_support_json` longtext COMMENT 'Factor16 控制支持分量 JSON；保存 Factor16 中有证据的控制支持趋势分量。',
  `missing_fields_json` longtext COMMENT '缺失字段 JSON；保存本次模型评估缺少的输入字段列表。',
  `quality_warnings_json` longtext COMMENT '质量警告 JSON；保存本次模型评估产生的数据质量警告。',
  `summary` longtext COMMENT '摘要；保存当前结果的结构化或可展示摘要。',
  `response_json` longtext NOT NULL COMMENT '响应证据 JSON；保存模型或 Provider 的版本化结构化响应快照。',
  `evaluated_at` datetime(3) NOT NULL COMMENT '评估时间；模型或规则完成评估的时间。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_risk_user_request` (`user_id`,`request_id`),
  KEY `fk_rehealth_risk_feature` (`feature_vector_id`),
  KEY `idx_risk_user_created` (`user_id`,`evaluated_at`),
  CONSTRAINT `fk_rehealth_risk_feature` FOREIGN KEY (`feature_vector_id`) REFERENCES `rehealth_cvd_feature_vector` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CVD 风险结果表；保存模型风险分数、等级、模型贡献、Factor16 贡献、警告和模型版本。';

-- ============================================================================
-- 表：rehealth_device_binding
-- 中文名称：用户设备绑定表
-- 业务用途：保存认证用户与产品、稳定设备身份及状态的绑定关系。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_device_binding` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `device_id` varchar(128) NOT NULL COMMENT '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。',
  `device_name` varchar(255) DEFAULT NULL COMMENT '设备名称；设备绑定或上报中的可展示设备名称。',
  `manufacturer` varchar(128) DEFAULT NULL COMMENT '设备制造商；绑定设备的制造商标识。',
  `device_model` varchar(128) DEFAULT NULL COMMENT '设备型号；设备上报或绑定时记录的具体型号。',
  `model` varchar(128) DEFAULT NULL COMMENT '设备型号；绑定设备的型号标识。',
  `firmware_version` varchar(128) DEFAULT NULL COMMENT '固件版本；采集当前数据时设备固件的版本。',
  `hardware_address_hash` varchar(255) DEFAULT NULL COMMENT '硬件地址摘要；设备硬件地址的不可逆摘要；不保存原始 BLE MAC。',
  `status` varchar(32) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `bound_at` datetime(3) NOT NULL COMMENT '绑定时间；用户与设备绑定建立的时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_device_user_device` (`user_id`,`device_id`),
  KEY `idx_rehealth_device_user_updated` (`user_id`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户设备绑定表；保存认证用户与产品、稳定设备身份及状态的绑定关系。';

-- ============================================================================
-- 表：rehealth_health_interview
-- 中文名称：健康访谈主表
-- 业务用途：保存认证用户每次结构化健康访谈的主记录和兼容 JSON 快照。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_health_interview` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `generated_at` datetime(3) NOT NULL COMMENT '生成时间；计划或结果完成生成的时间。',
  `answers_json` longtext COMMENT '访谈回答兼容快照；保存完整访谈回答的版本化 JSON；类型化回答表是主要查询结构。',
  `baseline_json` longtext COMMENT '基线证据 JSON；保存风险、归因或研究计算使用的版本化基线快照。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `idx_health_interview_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康访谈主表；保存认证用户每次结构化健康访谈的主记录和兼容 JSON 快照。';

-- ============================================================================
-- 表：rehealth_health_interview_answer
-- 中文名称：健康访谈回答表
-- 业务用途：保存访谈下的有序问答明细。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_health_interview_answer` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `interview_id` varchar(64) NOT NULL COMMENT '健康访谈记录 ID；物理关联 rehealth_health_interview.id。',
  `question_id` varchar(128) DEFAULT NULL COMMENT '问题 ID；标识访谈回答对应的稳定问题。',
  `topic` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `content` text NOT NULL COMMENT '消息内容；保存当前健康问答消息正文。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_interview_answer_order` (`interview_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_interview_answer` FOREIGN KEY (`interview_id`) REFERENCES `rehealth_health_interview` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康访谈回答表；保存访谈下的有序问答明细。';

-- ============================================================================
-- 表：rehealth_health_interview_baseline
-- 中文名称：健康访谈基线表
-- 业务用途：保存访谈提取的有序健康基线指标。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_health_interview_baseline` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `interview_id` varchar(64) NOT NULL COMMENT '健康访谈记录 ID；物理关联 rehealth_health_interview.id。',
  `label` varchar(255) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `item_value` varchar(1000) DEFAULT NULL COMMENT '回答/基线值；访谈明细中的类型化或文本值。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_interview_baseline_order` (`interview_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_interview_baseline` FOREIGN KEY (`interview_id`) REFERENCES `rehealth_health_interview` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康访谈基线表；保存访谈提取的有序健康基线指标。';

-- ============================================================================
-- 表：rehealth_health_interview_focus
-- 中文名称：健康访谈关注项表
-- 业务用途：保存访谈识别出的重点健康关注项。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_health_interview_focus` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `interview_id` varchar(64) NOT NULL COMMENT '健康访谈记录 ID；物理关联 rehealth_health_interview.id。',
  `focus_area` varchar(255) NOT NULL COMMENT '健康关注领域；访谈识别出的重点健康关注领域。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_interview_focus_order` (`interview_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_interview_focus` FOREIGN KEY (`interview_id`) REFERENCES `rehealth_health_interview` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康访谈关注项表；保存访谈识别出的重点健康关注项。';

-- ============================================================================
-- 表：rehealth_insurance_audit_event
-- 中文名称：保险操作审计表
-- 业务用途：保存租户内保险资源操作的不可变审计事件和前后哈希。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_audit_event` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `actor_user_id` varchar(64) NOT NULL COMMENT '操作用户 ID；执行审批或审计动作的内部用户。',
  `action` varchar(64) NOT NULL COMMENT '操作动作；保存审批或审计动作；具体枚举由对应业务服务定义。',
  `resource_type` varchar(64) NOT NULL COMMENT '资源类型；保险审计事件所操作资源的类型。',
  `resource_id` varchar(64) NOT NULL COMMENT '资源 ID；保险审计事件所操作资源的记录标识。',
  `request_id` varchar(128) DEFAULT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `before_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '变更前哈希；资源变更前内容的完整性摘要。',
  `after_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '变更后哈希；资源变更后内容的完整性摘要。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `idx_insurance_audit_tenant_resource` (`tenant_id`,`resource_type`,`resource_id`,`created_at`),
  KEY `idx_insurance_audit_tenant_actor` (`tenant_id`,`actor_user_id`,`created_at`),
  KEY `idx_insurance_audit_request` (`tenant_id`,`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险操作审计表；保存租户内保险资源操作的不可变审计事件和前后哈希。';

-- ============================================================================
-- 表：rehealth_insurance_claim
-- 中文名称：保险理赔表
-- 业务用途：保存理赔事件、金额、状态和保障代码。
-- 逻辑关联：policy_id -> rehealth_insurance_policy.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_claim` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `claim_no` varchar(128) NOT NULL COMMENT '理赔号；租户内唯一的理赔业务编号。',
  `policy_id` varchar(64) DEFAULT NULL COMMENT '保单记录 ID；逻辑关联 rehealth_insurance_policy.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `claim_type` varchar(64) NOT NULL COMMENT '理赔类型；理赔事件分类；完整枚举待保险业务确认。',
  `event_on` date DEFAULT NULL COMMENT '出险日期；理赔对应保险事件的发生日期。',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '提交时间；理赔、报告或审批流程提交时间。',
  `decided_at` datetime(3) DEFAULT NULL COMMENT '理赔决定时间；保险理赔完成审核决定的时间。',
  `status` varchar(32) NOT NULL DEFAULT 'submitted' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `billed_amount` decimal(18,2) DEFAULT NULL COMMENT '申请金额；理赔申请或医疗账单金额。',
  `approved_amount` decimal(18,2) DEFAULT NULL COMMENT '批准金额；审核或结算批准的金额。',
  `paid_amount` decimal(18,2) DEFAULT NULL COMMENT '已支付金额；理赔实际支付金额。',
  `currency` char(3) NOT NULL DEFAULT 'CNY' COMMENT '币种；金额字段采用的三字符货币代码，默认 CNY。',
  `coverage_code` varchar(64) DEFAULT NULL COMMENT '保障责任编码；保险产品保障责任的稳定代码。',
  `outcome_code` varchar(64) DEFAULT NULL COMMENT '理赔结局代码；理赔审核或支付结局的稳定代码。',
  `source_system` varchar(64) NOT NULL COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_claim_tenant_no` (`tenant_id`,`claim_no`),
  UNIQUE KEY `uk_insurance_claim_source_record` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_claim_subject_status` (`tenant_id`,`subject_ref`,`status`),
  KEY `idx_insurance_claim_policy` (`tenant_id`,`policy_id`,`event_on`),
  KEY `idx_insurance_claim_period` (`tenant_id`,`event_on`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险理赔表；保存理赔事件、金额、状态和保障代码。';

-- ============================================================================
-- 表：rehealth_insurance_consent
-- 中文名称：保险授权同意表
-- 业务用途：保存主体按类型和版本授予或撤销的授权及证据哈希。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_consent` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `consent_type` varchar(64) NOT NULL COMMENT '授权类型；主体授权覆盖的数据或用途类型。',
  `consent_version` varchar(64) NOT NULL COMMENT '授权版本；主体同意的授权文本或协议版本。',
  `status` varchar(32) NOT NULL DEFAULT 'granted' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `granted_at` datetime(3) DEFAULT NULL COMMENT '授权授予时间；授权状态变为 granted 的时间。',
  `revoked_at` datetime(3) DEFAULT NULL COMMENT '授权撤销时间；主体撤销授权的时间。',
  `evidence_ref` varchar(128) DEFAULT NULL COMMENT '授权证据引用；指向受控授权证据的引用，不直接保存证据正文。',
  `evidence_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '证据哈希；授权、报告或结算证据内容的完整性摘要。',
  `source_system` varchar(64) NOT NULL DEFAULT 'rehealth_app' COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_consent_version` (`tenant_id`,`subject_ref`,`consent_type`,`consent_version`),
  KEY `idx_insurance_consent_current` (`tenant_id`,`subject_ref`,`consent_type`,`status`),
  KEY `idx_insurance_consent_updated` (`tenant_id`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险授权同意表；保存主体按类型和版本授予或撤销的授权及证据哈希。';

-- ============================================================================
-- 表：rehealth_insurance_coverage
-- 中文名称：保险保障责任表
-- 业务用途：保存保单下的保障代码、限额、免赔额和有效期。
-- 逻辑关联：policy_id -> rehealth_insurance_policy.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_coverage` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `policy_id` varchar(64) NOT NULL COMMENT '保单记录 ID；逻辑关联 rehealth_insurance_policy.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `coverage_code` varchar(64) NOT NULL COMMENT '保障责任编码；保险产品保障责任的稳定代码。',
  `coverage_name` varchar(255) DEFAULT NULL COMMENT '保障责任名称；保险保障责任的可展示名称。',
  `limit_amount` decimal(18,2) DEFAULT NULL COMMENT '保障限额；当前保障责任的最高限额。',
  `deductible_amount` decimal(18,2) DEFAULT NULL COMMENT '免赔额；保单或保障责任的免赔金额。',
  `effective_on` date DEFAULT NULL COMMENT '生效日期；保单或保障责任开始生效日期。',
  `expires_on` date DEFAULT NULL COMMENT '到期日期；保单或保障责任到期日期。',
  `status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `source_system` varchar(64) NOT NULL COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_coverage_source_record` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_coverage_policy` (`tenant_id`,`policy_id`,`status`),
  KEY `idx_insurance_coverage_subject` (`tenant_id`,`subject_ref`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险保障责任表；保存保单下的保障代码、限额、免赔额和有效期。';

-- ============================================================================
-- 表：rehealth_insurance_import_batch
-- 中文名称：rehealth_insurance_import_batch 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_import_batch` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `import_type` varchar(32) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `source_system` varchar(64) NOT NULL COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `idempotency_key` varchar(128) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '内容哈希；结算包或业务内容的完整性摘要。',
  `status` varchar(32) NOT NULL DEFAULT 'processing' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `total_count` int NOT NULL DEFAULT '0' COMMENT 'TODO：字段中文业务含义待确认',
  `success_count` int NOT NULL DEFAULT '0' COMMENT 'TODO：字段中文业务含义待确认',
  `failure_count` int NOT NULL DEFAULT '0' COMMENT 'TODO：字段中文业务含义待确认',
  `error_json` longtext COMMENT 'TODO：字段中文业务含义待确认',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `completed_at` datetime(3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_import_idempotency` (`tenant_id`,`import_type`,`idempotency_key`),
  KEY `idx_insurance_import_status` (`tenant_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_insurance_import_batch 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_insurance_intervention
-- 中文名称：保险干预参与表
-- 业务用途：保存主体加入健康干预计划的状态与反馈时间。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_intervention` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `plan_id` varchar(128) NOT NULL COMMENT '干预计划业务 ID；保险干预参与记录中的稳定计划业务标识。',
  `source_plan_id` varchar(64) DEFAULT NULL COMMENT '来源干预计划 ID；逻辑引用 ReHealth 原始干预计划。',
  `consent_id` varchar(64) DEFAULT NULL COMMENT '授权记录 ID；逻辑关联允许当前保险干预使用数据的授权记录。',
  `status` varchar(32) NOT NULL DEFAULT 'enrolled' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `enrolled_at` datetime(3) DEFAULT NULL COMMENT '加入干预时间；主体加入保险健康干预计划的时间。',
  `ended_at` datetime(3) DEFAULT NULL COMMENT '结束时间；会话、活动或信号时间窗结束时间。',
  `last_feedback_at` datetime(3) DEFAULT NULL COMMENT '最近反馈时间；主体最近一次干预反馈时间。',
  `source_system` varchar(64) NOT NULL DEFAULT 'rehealth_app' COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_intervention_plan` (`tenant_id`,`subject_ref`,`plan_id`),
  UNIQUE KEY `uk_insurance_intervention_source_record` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_intervention_status` (`tenant_id`,`status`,`enrolled_at`),
  KEY `idx_insurance_intervention_subject` (`tenant_id`,`subject_ref`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险干预参与表；保存主体加入健康干预计划的状态与反馈时间。';

-- ============================================================================
-- 表：rehealth_insurance_intervention_action
-- 中文名称：保险人工干预行动表
-- 业务用途：保存租户和负责人范围内的随访、任务与人工复核行动及完成结果。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_intervention_action` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `plan_id` varchar(128) DEFAULT NULL COMMENT '干预计划业务 ID；保险干预参与记录中的稳定计划业务标识。',
  `action_type` varchar(32) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `title` varchar(255) NOT NULL COMMENT '标题；当前会话、研究、报告或业务对象的展示标题。',
  `content` varchar(2000) DEFAULT NULL COMMENT '消息内容；保存当前健康问答消息正文。',
  `assignee_user_id` varchar(32) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `due_at` datetime(3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `completed_at` datetime(3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `result_json` longtext COMMENT '研究结果 JSON；保存完整版本化研究结果。',
  `created_by` varchar(32) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `request_id` varchar(128) DEFAULT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_action_request` (`tenant_id`,`request_id`),
  KEY `idx_insurance_action_subject` (`tenant_id`,`subject_ref`,`updated_at`),
  KEY `idx_insurance_action_assignee` (`tenant_id`,`assignee_user_id`,`status`,`due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险人工干预行动表；保存租户和负责人范围内的随访、任务与人工复核行动及完成结果。';

-- ============================================================================
-- 表：rehealth_insurance_intervention_feedback
-- 中文名称：rehealth_insurance_intervention_feedback 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_intervention_feedback` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `binding_id` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `intervention_id` varchar(128) DEFAULT NULL COMMENT '干预行动 ID；标识用户反馈所针对的具体干预行动。',
  `plan_item_id` varchar(128) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `feedback_type` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `occurred_at` datetime(3) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `completion_rate` decimal(8,6) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `adherence_score` decimal(8,6) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `expected_count` decimal(10,3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `completed_count` decimal(10,3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `verification_type` varchar(32) NOT NULL DEFAULT 'self_report' COMMENT 'TODO：字段中文业务含义待确认',
  `calculation_version` varchar(64) NOT NULL DEFAULT 'legacy-client-score' COMMENT 'TODO：字段中文业务含义待确认',
  `outcome_summary_json` longtext COMMENT 'TODO：字段中文业务含义待确认',
  `source_system` varchar(64) NOT NULL DEFAULT 'rehealth_app' COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) NOT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_feedback_source` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_feedback_binding` (`tenant_id`,`binding_id`,`occurred_at`),
  KEY `idx_insurance_feedback_subject` (`tenant_id`,`subject_ref`,`occurred_at`),
  KEY `idx_insurance_feedback_item_period` (`tenant_id`,`binding_id`,`plan_item_id`,`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_insurance_intervention_feedback 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_insurance_plan_binding
-- 中文名称：rehealth_insurance_plan_binding 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_plan_binding` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `policy_id` varchar(64) NOT NULL COMMENT '保单记录 ID；逻辑关联 rehealth_insurance_policy.id。',
  `plan_id` varchar(128) NOT NULL COMMENT '干预计划业务 ID；保险干预参与记录中的稳定计划业务标识。',
  `consent_id` varchar(64) NOT NULL COMMENT '授权记录 ID；逻辑关联允许当前保险干预使用数据的授权记录。',
  `status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `bound_at` datetime(3) NOT NULL COMMENT '绑定时间；用户与设备绑定建立的时间。',
  `unbound_at` datetime(3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `source_system` varchar(64) NOT NULL DEFAULT 'rehealth_app' COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_plan_binding` (`tenant_id`,`subject_ref`,`policy_id`,`plan_id`),
  UNIQUE KEY `uk_insurance_plan_binding_source` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_plan_binding_status` (`tenant_id`,`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_insurance_plan_binding 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_insurance_policy
-- 中文名称：保险保单表
-- 业务用途：保存租户内保单、产品、金额、期限和被保主体引用。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_policy` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `policy_no` varchar(128) NOT NULL COMMENT '保单号；租户内唯一的保单业务编号。',
  `product_code` varchar(64) DEFAULT NULL COMMENT '产品编码；选择设备 Provider 和能力目录的稳定产品编码。',
  `product_name` varchar(255) DEFAULT NULL COMMENT '产品名称；保险或设备产品的可展示名称。',
  `policy_type` varchar(64) NOT NULL COMMENT '保单类型；保险产品的保单类型；完整枚举待保险业务确认。',
  `policyholder_subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '投保主体引用；投保人的去标识主体引用。',
  `insured_subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被保主体引用；被保险人的去标识主体引用。',
  `coverage_amount` decimal(18,2) DEFAULT NULL COMMENT '保额；保单总保障金额。',
  `premium_amount` decimal(18,2) DEFAULT NULL COMMENT '保费；保单保费金额。',
  `deductible_amount` decimal(18,2) DEFAULT NULL COMMENT '免赔额；保单或保障责任的免赔金额。',
  `waiting_period_days` int DEFAULT NULL COMMENT '等待期天数；保单责任生效前的等待期天数。',
  `effective_on` date DEFAULT NULL COMMENT '生效日期；保单或保障责任开始生效日期。',
  `expires_on` date DEFAULT NULL COMMENT '到期日期；保单或保障责任到期日期。',
  `status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `source_system` varchar(64) NOT NULL COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_policy_tenant_no` (`tenant_id`,`policy_no`),
  UNIQUE KEY `uk_insurance_policy_source_record` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_policy_subject_status` (`tenant_id`,`insured_subject_ref`,`status`),
  KEY `idx_insurance_policy_effective` (`tenant_id`,`effective_on`,`expires_on`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险保单表；保存租户内保单、产品、金额、期限和被保主体引用。';

-- ============================================================================
-- 表：rehealth_insurance_rwe_report
-- 中文名称：真实世界证据报告表
-- 业务用途：保存版本化 RWE 报告及审批证据。
-- 逻辑关联：study_id -> rehealth_insurance_study.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_rwe_report` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `report_no` varchar(128) NOT NULL COMMENT '报告编号；租户内唯一的 RWE 报告编号。',
  `study_id` varchar(64) NOT NULL COMMENT '研究记录 ID；逻辑关联 rehealth_insurance_study.id。',
  `report_type` varchar(64) NOT NULL DEFAULT 'rwe' COMMENT '报告类型；报告业务类型，当前默认 rwe。',
  `report_version` int NOT NULL DEFAULT '1' COMMENT '报告版本；同一研究下报告的递增版本。',
  `title` varchar(255) NOT NULL COMMENT '标题；当前会话、研究、报告或业务对象的展示标题。',
  `period_start` date DEFAULT NULL COMMENT '研究/报告起始日期；研究或报告纳入数据的开始日期。',
  `period_end` date DEFAULT NULL COMMENT '研究/报告结束日期；研究或报告纳入数据的结束日期。',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `evidence_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '证据哈希；授权、报告或结算证据内容的完整性摘要。',
  `report_json` longtext NOT NULL COMMENT '报告内容 JSON；保存版本化结构化 RWE 报告。',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `submitted_at` datetime(3) DEFAULT NULL COMMENT '提交时间；理赔、报告或审批流程提交时间。',
  `approved_by` varchar(64) DEFAULT NULL COMMENT '审批用户 ID；批准当前研究、报告或结算包的内部用户。',
  `approved_at` datetime(3) DEFAULT NULL COMMENT '审批时间；审批完成时间。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_rwe_report_no` (`tenant_id`,`report_no`),
  UNIQUE KEY `uk_insurance_rwe_report_version` (`tenant_id`,`study_id`,`report_version`),
  KEY `idx_insurance_rwe_report_status` (`tenant_id`,`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='真实世界证据报告表；保存版本化 RWE 报告及审批证据。';

-- ============================================================================
-- 表：rehealth_insurance_settlement_approval
-- 中文名称：保险结算审批记录表
-- 业务用途：保存结算包的审批动作、意见和请求幂等键。
-- 逻辑关联：package_id -> rehealth_insurance_settlement_package.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_settlement_approval` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `package_id` varchar(64) NOT NULL COMMENT '结算包 ID；逻辑关联 rehealth_insurance_settlement_package.id。',
  `action` varchar(32) NOT NULL COMMENT '操作动作；保存审批或审计动作；具体枚举由对应业务服务定义。',
  `comment` varchar(2000) DEFAULT NULL COMMENT '审批/操作意见；保存审批或操作人员提交的说明文本。',
  `actor_user_id` varchar(64) NOT NULL COMMENT '操作用户 ID；执行审批或审计动作的内部用户。',
  `request_id` varchar(128) NOT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_settlement_approval_request` (`tenant_id`,`package_id`,`request_id`),
  KEY `idx_insurance_settlement_approval_package` (`tenant_id`,`package_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险结算审批记录表；保存结算包的审批动作、意见和请求幂等键。';

-- ============================================================================
-- 表：rehealth_insurance_settlement_package
-- 中文名称：保险结算包表
-- 业务用途：保存由研究和报告形成的版本化结算证据包。
-- 逻辑关联：study_id -> rehealth_insurance_study.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_settlement_package` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `package_no` varchar(128) NOT NULL COMMENT '结算包编号；租户内唯一的结算证据包编号。',
  `study_id` varchar(64) NOT NULL COMMENT '研究记录 ID；逻辑关联 rehealth_insurance_study.id。',
  `report_id` varchar(64) DEFAULT NULL COMMENT '报告 ID；逻辑关联形成结算包的 RWE 报告。',
  `package_version` int NOT NULL DEFAULT '1' COMMENT '结算包版本；同一研究下结算证据包的递增版本。',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `currency` char(3) NOT NULL DEFAULT 'CNY' COMMENT '币种；金额字段采用的三字符货币代码，默认 CNY。',
  `estimated_savings` decimal(18,2) DEFAULT NULL COMMENT '预计节省金额；基于已批准研究口径估算的节省金额。',
  `approved_amount` decimal(18,2) DEFAULT NULL COMMENT '批准金额；审核或结算批准的金额。',
  `snapshot_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '快照哈希；研究或结算证据快照的内容完整性摘要。',
  `evidence_manifest_json` longtext NOT NULL COMMENT '证据清单 JSON；保存结算包包含的证据引用和哈希清单。',
  `package_json` longtext NOT NULL COMMENT '结算包内容 JSON；保存完整版本化结算内容。',
  `content_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '内容哈希；结算包或业务内容的完整性摘要。',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `approved_by` varchar(64) DEFAULT NULL COMMENT '审批用户 ID；批准当前研究、报告或结算包的内部用户。',
  `approved_at` datetime(3) DEFAULT NULL COMMENT '审批时间；审批完成时间。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_settlement_package_no` (`tenant_id`,`package_no`),
  UNIQUE KEY `uk_insurance_settlement_package_version` (`tenant_id`,`study_id`,`package_version`),
  KEY `idx_insurance_settlement_status` (`tenant_id`,`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险结算包表；保存由研究和报告形成的版本化结算证据包。';

-- ============================================================================
-- 表：rehealth_insurance_study
-- 中文名称：保险研究定义表
-- 业务用途：保存真实世界研究人群、干预、结局规则和审批状态。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_study` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `study_no` varchar(128) NOT NULL COMMENT '研究编号；租户内唯一的保险真实世界研究编号。',
  `title` varchar(255) NOT NULL COMMENT '标题；当前会话、研究、报告或业务对象的展示标题。',
  `period_start` date DEFAULT NULL COMMENT '研究/报告起始日期；研究或报告纳入数据的开始日期。',
  `period_end` date DEFAULT NULL COMMENT '研究/报告结束日期；研究或报告纳入数据的结束日期。',
  `population_rule_json` longtext NOT NULL COMMENT '研究人群规则；定义研究人群纳入排除条件的版本化 JSON。',
  `intervention_rule_json` longtext NOT NULL COMMENT '研究干预规则；定义研究处理/干预暴露的版本化 JSON。',
  `outcome_rule_json` longtext NOT NULL COMMENT '研究结局规则；定义研究结局计算口径的版本化 JSON。',
  `methodology` varchar(64) NOT NULL DEFAULT 'psm' COMMENT '研究方法；真实世界研究使用的方法，当前默认 psm。',
  `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `model_version` varchar(128) DEFAULT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `approved_by` varchar(64) DEFAULT NULL COMMENT '审批用户 ID；批准当前研究、报告或结算包的内部用户。',
  `approved_at` datetime(3) DEFAULT NULL COMMENT '审批时间；审批完成时间。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_study_tenant_no` (`tenant_id`,`study_no`),
  KEY `idx_insurance_study_status` (`tenant_id`,`status`,`updated_at`),
  KEY `idx_insurance_study_period` (`tenant_id`,`period_start`,`period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险研究定义表；保存真实世界研究人群、干预、结局规则和审批状态。';

-- ============================================================================
-- 表：rehealth_insurance_study_job
-- 中文名称：rehealth_insurance_study_job 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_study_job` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `study_id` varchar(64) NOT NULL COMMENT '研究记录 ID；逻辑关联 rehealth_insurance_study.id。',
  `snapshot_id` varchar(64) NOT NULL COMMENT '快照记录 ID；逻辑关联本业务域的快照主记录。',
  `job_type` varchar(32) NOT NULL DEFAULT 'psm' COMMENT 'TODO：字段中文业务含义待确认',
  `status` varchar(32) NOT NULL DEFAULT 'queued' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `request_id` varchar(128) NOT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `attempt` int NOT NULL DEFAULT '0' COMMENT 'TODO：字段中文业务含义待确认',
  `error_message` varchar(1000) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `result_id` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `started_at` datetime(3) DEFAULT NULL COMMENT '开始时间；会话、活动或信号时间窗开始时间。',
  `finished_at` datetime(3) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_study_job_request` (`tenant_id`,`study_id`,`request_id`),
  KEY `idx_insurance_study_job_status` (`tenant_id`,`status`,`created_at`),
  KEY `idx_insurance_study_job_snapshot` (`tenant_id`,`snapshot_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_insurance_study_job 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_insurance_study_member
-- 中文名称：保险研究成员表
-- 业务用途：保存研究快照中的去标识主体、队列分组和结局值。
-- 逻辑关联：snapshot_id -> rehealth_insurance_study_snapshot.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_study_member` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `snapshot_id` varchar(64) NOT NULL COMMENT '快照记录 ID；逻辑关联本业务域的快照主记录。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `cohort_group` varchar(32) NOT NULL COMMENT '队列分组；研究成员所属处理组或对照组。',
  `baseline_risk` decimal(10,6) DEFAULT NULL COMMENT '基线风险；研究成员在干预前的基线风险值。',
  `outcome_value` decimal(18,6) DEFAULT NULL COMMENT '结局值；保险研究成员在既定结局定义下的观测结果值。',
  `intervention_status` varchar(32) DEFAULT NULL COMMENT '干预状态；研究成员在结局窗口内的干预状态。',
  `covariate_json` longtext COMMENT 'TODO：字段中文业务含义待确认',
  `source_row_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_snapshot_member` (`tenant_id`,`snapshot_id`,`subject_ref`),
  KEY `idx_insurance_snapshot_member_group` (`tenant_id`,`snapshot_id`,`cohort_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险研究成员表；保存研究快照中的去标识主体、队列分组和结局值。';

-- ============================================================================
-- 表：rehealth_insurance_study_result
-- 中文名称：保险研究结果表
-- 业务用途：保存 PSM/真实世界研究估计、区间、平衡和成本结果。
-- 逻辑关联：study_id -> rehealth_insurance_study.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- 逻辑关联：snapshot_id -> rehealth_insurance_study_snapshot.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_study_result` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `study_id` varchar(64) NOT NULL COMMENT '研究记录 ID；逻辑关联 rehealth_insurance_study.id。',
  `snapshot_id` varchar(64) NOT NULL COMMENT '快照记录 ID；逻辑关联本业务域的快照主记录。',
  `result_version` int NOT NULL COMMENT '结果版本；同一研究结果的递增版本。',
  `status` varchar(32) NOT NULL DEFAULT 'calculated' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `att_estimate` decimal(18,8) DEFAULT NULL COMMENT 'ATT 估计值；对已处理者平均处理效应的估计值。',
  `ci_lower` decimal(18,8) DEFAULT NULL COMMENT '区间下界；研究估计区间的下界。',
  `ci_upper` decimal(18,8) DEFAULT NULL COMMENT '区间上界；研究估计区间的上界。',
  `matched_pairs` int DEFAULT NULL COMMENT '匹配对数；PSM 等匹配方法最终形成的匹配样本对数。',
  `balance_json` longtext COMMENT '协变量平衡 JSON；保存匹配前后协变量平衡诊断。',
  `cost_basis_json` longtext COMMENT '成本口径 JSON；保存经济性或结算计算使用的成本口径。',
  `model_version` varchar(128) DEFAULT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `result_json` longtext NOT NULL COMMENT '研究结果 JSON；保存完整版本化研究结果。',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_study_result_version` (`tenant_id`,`study_id`,`result_version`),
  KEY `idx_insurance_study_result_status` (`tenant_id`,`study_id`,`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险研究结果表；保存 PSM/真实世界研究估计、区间、平衡和成本结果。';

-- ============================================================================
-- 表：rehealth_insurance_study_snapshot
-- 中文名称：保险研究快照表
-- 业务用途：保存研究人群不可变快照、来源水位和内容哈希。
-- 逻辑关联：study_id -> rehealth_insurance_study.id（保险域逻辑外键，数据库未声明 FOREIGN KEY）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_study_snapshot` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `study_id` varchar(64) NOT NULL COMMENT '研究记录 ID；逻辑关联 rehealth_insurance_study.id。',
  `snapshot_version` int NOT NULL COMMENT '快照版本；同一研究下不可变人群快照的递增版本。',
  `snapshot_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '快照哈希；研究或结算证据快照的内容完整性摘要。',
  `source_watermark` varchar(128) DEFAULT NULL COMMENT '来源水位；生成研究快照时上游数据的版本或时间水位。',
  `cohort_total` int NOT NULL DEFAULT '0' COMMENT '队列总人数；研究快照中的去标识主体总数。',
  `treated_total` int NOT NULL DEFAULT '0' COMMENT '处理组人数；研究快照中处理/干预组主体数。',
  `control_total` int NOT NULL DEFAULT '0' COMMENT '对照组人数；研究快照中对照组主体数。',
  `source_summary_json` longtext NOT NULL COMMENT '来源摘要 JSON；保存研究快照来源和覆盖情况的结构化摘要。',
  `immutable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否不可变；标识研究快照生成后是否禁止修改，默认 true。',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_snapshot_version` (`tenant_id`,`study_id`,`snapshot_version`),
  UNIQUE KEY `uk_insurance_snapshot_hash` (`tenant_id`,`study_id`,`snapshot_hash`),
  KEY `idx_insurance_snapshot_study` (`tenant_id`,`study_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险研究快照表；保存研究人群不可变快照、来源水位和内容哈希。';

-- ============================================================================
-- 表：rehealth_insurance_subject
-- 中文名称：保险业务主体表
-- 业务用途：保存租户隔离、去标识化的保险主体与 ReHealth 用户映射。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_subject` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `rehealth_user_id` varchar(64) NOT NULL COMMENT 'ReHealth 用户 ID；保险主体映射到的内部认证用户 ID，逻辑关联 sys_user.id。',
  `external_subject_ref_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '外部主体引用摘要；外部系统主体标识的不可逆摘要。',
  `enrollment_status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '纳入状态；保险主体在当前租户业务中的纳入状态。',
  `consent_status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT '授权状态；保险主体当前授权状态；完整枚举由保险服务定义。',
  `consent_version` varchar(64) DEFAULT NULL COMMENT '授权版本；主体同意的授权文本或协议版本。',
  `consented_at` datetime(3) DEFAULT NULL COMMENT '授权时间；主体完成当前授权的时间。',
  `source_system` varchar(64) NOT NULL DEFAULT 'rehealth' COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `source_record_id` varchar(128) DEFAULT NULL COMMENT '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。',
  `metadata_json` longtext COMMENT '扩展元数据 JSON；保存版本化扩展信息；不是核心字段的唯一权威表示。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_subject_tenant_ref` (`tenant_id`,`subject_ref`),
  UNIQUE KEY `uk_insurance_subject_tenant_user` (`tenant_id`,`rehealth_user_id`),
  UNIQUE KEY `uk_insurance_subject_source_record` (`tenant_id`,`source_system`,`source_record_id`),
  KEY `idx_insurance_subject_tenant_status` (`tenant_id`,`enrollment_status`,`consent_status`),
  KEY `idx_insurance_subject_tenant_updated` (`tenant_id`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保险业务主体表；保存租户隔离、去标识化的保险主体与 ReHealth 用户映射。';

-- ============================================================================
-- 表：rehealth_insurance_subject_manager
-- 中文名称：rehealth_insurance_subject_manager 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_subject_manager` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `manager_user_id` varchar(32) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `department_id` varchar(32) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `subject_ref` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '去标识保险主体引用；租户内稳定的去标识主体引用，不保存直接患者标识。',
  `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `source_system` varchar(64) NOT NULL DEFAULT 'LOCAL_INSURANCE_QA' COMMENT '来源系统；标识记录来自哪个受信业务系统。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_subject_manager` (`tenant_id`,`manager_user_id`,`subject_ref`),
  KEY `idx_insurance_manager_subject` (`tenant_id`,`manager_user_id`,`status`),
  KEY `idx_insurance_subject_manager_department` (`tenant_id`,`department_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_insurance_subject_manager 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_insurance_tenant_profile
-- 中文名称：rehealth_insurance_tenant_profile 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_insurance_tenant_profile` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `organization_name` varchar(200) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `license_no` varchar(100) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `insurance_type` varchar(32) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `compliance_email` varchar(120) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `regulatory_email` varchar(120) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `data_retention_years` int NOT NULL DEFAULT '7' COMMENT 'TODO：字段中文业务含义待确认',
  `mask_sensitive_data` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'TODO：字段中文业务含义待确认',
  `access_log_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'TODO：字段中文业务含义待确认',
  `notification_config_json` json DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本；记录或配置版本；是否为乐观锁需结合实体 @Version 判断。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insurance_tenant_profile_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_insurance_tenant_profile 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_intervention_contraindication
-- 中文名称：干预禁忌表
-- 业务用途：保存某次干预计划包含的有序禁忌与安全限制。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_intervention_contraindication` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `plan_record_id` varchar(64) NOT NULL COMMENT '干预计划记录 ID；物理关联 rehealth_intervention_plan.id。',
  `item_value` varchar(1000) NOT NULL COMMENT '回答/基线值；访谈明细中的类型化或文本值。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_contraindication_order` (`plan_record_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_contraindication_plan` FOREIGN KEY (`plan_record_id`) REFERENCES `rehealth_intervention_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='干预禁忌表；保存某次干预计划包含的有序禁忌与安全限制。';

-- ============================================================================
-- 表：rehealth_intervention_feedback
-- 中文名称：干预反馈表
-- 业务用途：保存用户对具体干预计划/行动的完成、跳过或不适用反馈。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_intervention_feedback` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `plan_record_id` varchar(64) NOT NULL COMMENT '干预计划记录 ID；物理关联 rehealth_intervention_plan.id。',
  `plan_id` varchar(128) NOT NULL COMMENT '干预计划业务 ID；保险干预参与记录中的稳定计划业务标识。',
  `intervention_id` varchar(128) NOT NULL COMMENT '干预行动 ID；标识用户反馈所针对的具体干预行动。',
  `idempotency_key` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `status` varchar(64) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `adherence` double DEFAULT NULL COMMENT '依从性；用户反馈中记录的干预执行或依从情况。',
  `note` varchar(2000) DEFAULT NULL COMMENT '备注；保存用户或业务操作的可选补充说明。',
  `checked_at` datetime(3) DEFAULT NULL COMMENT '反馈打卡时间；用户对干预行动提交反馈的时间。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_feedback_user_key` (`user_id`,`idempotency_key`),
  KEY `fk_rehealth_feedback_plan` (`plan_record_id`),
  KEY `idx_feedback_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_rehealth_feedback_plan` FOREIGN KEY (`plan_record_id`) REFERENCES `rehealth_intervention_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='干预反馈表；保存用户对具体干预计划/行动的完成、跳过或不适用反馈。';

-- ============================================================================
-- 表：rehealth_intervention_plan
-- 中文名称：健康干预计划表
-- 业务用途：保存基于权威画像、风险和设备行为上下文生成的结构化保守干预计划。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_intervention_plan` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `plan_id` varchar(128) NOT NULL COMMENT '干预计划业务 ID；保险干预参与记录中的稳定计划业务标识。',
  `source_request_id` varchar(128) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `feature_schema_version` varchar(64) DEFAULT NULL COMMENT '特征协议版本；标识特征向量遵循的字段协议版本。',
  `model_version` varchar(128) NOT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `scorer_mode` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `is_mock` tinyint(1) DEFAULT NULL COMMENT '是否模拟数据；明确标识结果是否来自 Mock/合成路径；生产结果不得为真。',
  `artifact_name` varchar(255) DEFAULT NULL COMMENT '模型制品名称；标识产生结果时使用的已加载模型制品。',
  `priority_intervention` varchar(1000) DEFAULT NULL COMMENT '优先干预摘要；结构化干预计划中优先级最高行动的摘要。',
  `rationale` longtext COMMENT '干预依据；解释干预行动与权威画像、风险或行为上下文之间的依据。',
  `expected_impact` varchar(1000) DEFAULT NULL COMMENT '预期影响；保守描述执行干预可能带来的健康行为影响，不构成疗效保证。',
  `confidence` double DEFAULT NULL COMMENT '置信度；当前特征、因素、识别结果或计划的可信程度。',
  `medical_disclaimer` varchar(2000) DEFAULT NULL COMMENT '医疗免责声明；声明建议仅供健康参考、不能替代医疗诊断或医生。',
  `generated_at` datetime(3) NOT NULL COMMENT '生成时间；计划或结果完成生成的时间。',
  `response_json` longtext NOT NULL COMMENT '响应证据 JSON；保存模型或 Provider 的版本化结构化响应快照。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_plan_user_plan` (`user_id`,`plan_id`),
  KEY `idx_plan_user_generated` (`user_id`,`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康干预计划表；保存基于权威画像、风险和设备行为上下文生成的结构化保守干预计划。';

-- ============================================================================
-- 表：rehealth_model_request_log
-- 中文名称：模型请求审计表
-- 业务用途：保存不含原始 PII/遥测的模型调用元数据、状态、耗时和错误码。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_model_request_log` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `request_id` varchar(128) DEFAULT NULL COMMENT '请求幂等 ID；用于请求追踪与幂等控制，不能作为用户身份来源。',
  `operation` varchar(64) NOT NULL COMMENT '操作名称；保存模型请求或业务审计的操作名称。',
  `model_version` varchar(128) DEFAULT NULL COMMENT '模型版本；产生当前模型输出的版本标识。',
  `outcome` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `error_code` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `latency_ms` bigint NOT NULL DEFAULT '0' COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  KEY `idx_model_request_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型请求审计表；保存不含原始 PII/遥测的模型调用元数据、状态、耗时和错误码。';

-- ============================================================================
-- 表：rehealth_patient_allergy
-- 中文名称：患者过敏史表
-- 业务用途：保存健康档案下的有序过敏条目。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_patient_allergy` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `profile_id` varchar(64) NOT NULL COMMENT '健康档案记录 ID；物理关联 rehealth_patient_profile.id。',
  `item_value` varchar(512) NOT NULL COMMENT '回答/基线值；访谈明细中的类型化或文本值。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_allergy_order` (`profile_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_allergy_profile` FOREIGN KEY (`profile_id`) REFERENCES `rehealth_patient_profile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者过敏史表；保存健康档案下的有序过敏条目。';

-- ============================================================================
-- 表：rehealth_patient_diagnosis
-- 中文名称：患者诊断史表
-- 业务用途：保存健康档案下的有序诊断史条目。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_patient_diagnosis` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `profile_id` varchar(64) NOT NULL COMMENT '健康档案记录 ID；物理关联 rehealth_patient_profile.id。',
  `item_value` varchar(512) NOT NULL COMMENT '回答/基线值；访谈明细中的类型化或文本值。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_diagnosis_order` (`profile_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_diagnosis_profile` FOREIGN KEY (`profile_id`) REFERENCES `rehealth_patient_profile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者诊断史表；保存健康档案下的有序诊断史条目。';

-- ============================================================================
-- 表：rehealth_patient_medication
-- 中文名称：患者用药史表
-- 业务用途：保存健康档案下的有序用药条目。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_patient_medication` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `profile_id` varchar(64) NOT NULL COMMENT '健康档案记录 ID；物理关联 rehealth_patient_profile.id。',
  `item_value` varchar(512) NOT NULL COMMENT '回答/基线值；访谈明细中的类型化或文本值。',
  `sort_order` int NOT NULL COMMENT '排序序号；控制同一主记录下明细的稳定展示和处理顺序。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_medication_order` (`profile_id`,`sort_order`),
  CONSTRAINT `fk_rehealth_medication_profile` FOREIGN KEY (`profile_id`) REFERENCES `rehealth_patient_profile` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者用药史表；保存健康档案下的有序用药条目。';

-- ============================================================================
-- 表：rehealth_patient_profile
-- 中文名称：患者健康档案表
-- 业务用途：保存认证用户的类型化健康档案、BMI 和乐观锁版本。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_patient_profile` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `name` varchar(128) DEFAULT NULL COMMENT '名称；当前业务对象的名称；是否属于直接身份信息取决于所在表。',
  `gender` varchar(32) DEFAULT NULL COMMENT '性别；健康档案中用户明确提供或经访谈确认的性别；完整枚举待产品契约确认。',
  `age` smallint DEFAULT NULL COMMENT '年龄；健康档案中用户明确提供或经访谈确认的年龄。',
  `height_cm` decimal(6,2) DEFAULT NULL COMMENT '身高；健康档案中的身高，单位厘米。',
  `weight_kg` decimal(6,2) DEFAULT NULL COMMENT '体重；健康档案中的体重，单位千克。',
  `bmi` decimal(5,2) DEFAULT NULL COMMENT '体质指数 BMI；服务端根据档案身高和体重计算的 BMI。',
  `family_history` tinyint(1) DEFAULT NULL COMMENT '家族史标志；标识健康档案是否记录相关家族病史；空值表示未确认。',
  `smoking` tinyint(1) DEFAULT NULL COMMENT '吸烟标志；标识健康档案中的吸烟情况；空值表示未确认。',
  `drinking` tinyint(1) DEFAULT NULL COMMENT '饮酒标志；标识健康档案中的饮酒情况；空值表示未确认。',
  `diabetes_history` tinyint(1) DEFAULT NULL COMMENT '糖尿病史标志；标识健康档案中是否有糖尿病史；空值表示未确认。',
  `hypertension_history` tinyint(1) DEFAULT NULL COMMENT '高血压史标志；标识健康档案中是否有高血压史；空值表示未确认。',
  `profile_version` bigint NOT NULL DEFAULT '1' COMMENT '档案版本号；由 Repository 显式维护的乐观锁版本，更新档案时用于冲突检测。',
  `profile_json` longtext COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rehealth_profile_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='患者健康档案表；保存认证用户的类型化健康档案、BMI 和乐观锁版本。';

-- ============================================================================
-- 表：rehealth_rdi_contribution
-- 中文名称：rehealth_rdi_contribution 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_rdi_contribution` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `snapshot_id` varchar(64) NOT NULL COMMENT '快照记录 ID；逻辑关联本业务域的快照主记录。',
  `factor_code` varchar(64) NOT NULL COMMENT '因素编码；RDI 因素的稳定代码。',
  `domain_code` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `source_code` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `current_value` decimal(16,6) NOT NULL COMMENT '当前值；当前因素或指标参与计算时使用的实际值。',
  `baseline_value` decimal(16,6) DEFAULT NULL COMMENT '基线值；用于与当前值比较的个人或研究基线值。',
  `unit` varchar(32) NOT NULL COMMENT '计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。',
  `raw_points` decimal(10,6) NOT NULL COMMENT '原始贡献分；乘入置信度等修正前的因素贡献分。',
  `confidence` decimal(8,6) NOT NULL COMMENT '置信度；当前特征、因素、识别结果或计划的可信程度。',
  `final_points` decimal(10,6) NOT NULL COMMENT '最终贡献分；考虑置信度和规则修正后实际使用的贡献分。',
  `source_factor_id` varchar(255) NOT NULL COMMENT '来源因素 ID；关联产生当前贡献的稳定来源因素。',
  `algorithm_version` varchar(128) NOT NULL COMMENT '算法版本；产生当前规则或算法结果的版本标识。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rdi_contribution_snapshot_factor` (`snapshot_id`,`factor_code`),
  KEY `idx_rdi_contribution_snapshot_points` (`snapshot_id`,`final_points`),
  CONSTRAINT `fk_rdi_contribution_snapshot` FOREIGN KEY (`snapshot_id`) REFERENCES `rehealth_rdi_daily_snapshot` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_rdi_contribution 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_rdi_daily_snapshot
-- 中文名称：rehealth_rdi_daily_snapshot 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_rdi_daily_snapshot` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `scored_on` date NOT NULL COMMENT '评分日期；评分所属本地自然日，使用 ISO-8601 日期。',
  `raw_score` decimal(8,4) NOT NULL COMMENT '原始分数；平滑或展示转换前的当日算法分数。',
  `display_score` decimal(8,4) NOT NULL COMMENT '展示分数；经过规定平滑后用于产品展示的分数。',
  `data_confidence` decimal(8,6) NOT NULL COMMENT '数据可信度；算法对当前输入覆盖和质量的综合可信度。',
  `status` varchar(32) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `is_mock` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否模拟数据；明确标识结果是否来自 Mock/合成路径；生产结果不得为真。',
  `algorithm_version` varchar(128) NOT NULL COMMENT '算法版本；产生当前规则或算法结果的版本标识。',
  `calculation_source` varchar(64) NOT NULL COMMENT '计算来源；标识当前 RHI 快照由哪个受控计算路径产生。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rdi_daily_user_date` (`user_id`,`scored_on`),
  KEY `idx_rdi_daily_user_updated` (`user_id`,`scored_on`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='rehealth_rdi_daily_snapshot 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rehealth_rhi_daily_snapshot
-- 中文名称：云端 RHI 每日聚合快照表
-- 业务用途：保存认证用户从 App 上传的日级 RHI 分数、领域、特征与质量聚合快照；不保存原始遥测。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_rhi_daily_snapshot` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `scored_on` date NOT NULL COMMENT '评分日期；评分所属本地自然日，使用 ISO-8601 日期。',
  `raw_score` decimal(8,4) NOT NULL COMMENT '原始分数；平滑或展示转换前的当日算法分数。',
  `display_score` decimal(8,4) NOT NULL COMMENT '展示分数；经过规定平滑后用于产品展示的分数。',
  `data_confidence` decimal(8,6) NOT NULL COMMENT '数据可信度；算法对当前输入覆盖和质量的综合可信度。',
  `status` varchar(32) NOT NULL COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `product_tier` varchar(32) NOT NULL COMMENT '产品数据层级；RHI 根据当前可用证据确定的 LITE/STANDARD/CLINICAL 数据层级。',
  `available_days` int NOT NULL COMMENT '有效天数；评分回看窗口内具有可用证据的天数。',
  `available_feature_count` int NOT NULL COMMENT '可用特征数；本次评分实际提取到的有效特征数量。',
  `smoothing_alpha` decimal(8,6) NOT NULL COMMENT '平滑系数；原始分与历史展示分合并时使用的平滑参数。',
  `algorithm_version` varchar(128) NOT NULL COMMENT '算法版本；产生当前规则或算法结果的版本标识。',
  `calculation_source` varchar(64) NOT NULL COMMENT '计算来源；标识当前 RHI 快照由哪个受控计算路径产生。',
  `domains_json` longtext NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `features_json` longtext NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `quality_json` longtext COMMENT '特征质量 JSON；保存特征缺失、质量和来源等版本化元数据。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rhi_daily_user_date` (`user_id`,`scored_on`),
  KEY `idx_rhi_daily_user_updated` (`user_id`,`scored_on`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='云端 RHI 每日聚合快照表；保存认证用户从 App 上传的日级 RHI 分数、领域、特征与质量聚合快照；不保存原始遥测。';

-- ============================================================================
-- 表：rehealth_rhi_manual_health_input
-- 中文名称：云端 RHI 手工输入表
-- 业务用途：保存认证用户 Room-first 手工健康输入的云端副本，并按 updated_at 合并。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_rhi_manual_health_input` (
  `user_id` varchar(64) NOT NULL COMMENT '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。',
  `sedentary_hours_per_day` decimal(6,2) DEFAULT NULL COMMENT '日均久坐时长；用户确认的日均久坐小时数。',
  `waist_circumference_cm` decimal(6,2) DEFAULT NULL COMMENT '腰围；用户确认的腰围，单位厘米。',
  `vo2_max_ml_kg_min` decimal(6,2) DEFAULT NULL COMMENT '最大摄氧量；正式 VO2max，单位 ml/kg/min。',
  `hba1c_percent` decimal(6,2) DEFAULT NULL COMMENT '糖化血红蛋白；用户确认的 HbA1c 百分比。',
  `egfr_ml_min_1_73m2` decimal(7,2) DEFAULT NULL COMMENT '估算肾小球滤过率；用户确认的 eGFR，单位 ml/min/1.73m²。',
  `cuff_sbp_7d_mean` decimal(6,2) DEFAULT NULL COMMENT '7 日袖带收缩压均值；经确认上臂袖带测量的 3–7 日收缩压均值。',
  `cuff_dbp_7d_mean` decimal(6,2) DEFAULT NULL COMMENT '7 日袖带舒张压均值；经确认上臂袖带测量的 3–7 日舒张压均值。',
  `cuff_valid_days` int DEFAULT NULL COMMENT '袖带有效天数；计算袖带血压均值时包含的有效自然日数。',
  `cuff_confirmed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '袖带血压是否确认；只有用户确认且满足规则的上臂袖带血压才进入正式特征。',
  `fasting_glucose_mmol_l` decimal(7,3) DEFAULT NULL COMMENT '空腹血糖；用户确认的空腹血糖，单位 mmol/L。',
  `total_cholesterol_mmol_l` decimal(7,3) DEFAULT NULL COMMENT '总胆固醇；用户确认的总胆固醇，单位 mmol/L。',
  `ldl_mmol_l` decimal(7,3) DEFAULT NULL COMMENT '低密度脂蛋白胆固醇；用户确认的 LDL-C，单位 mmol/L。',
  `hdl_mmol_l` decimal(7,3) DEFAULT NULL COMMENT '高密度脂蛋白胆固醇；用户确认的 HDL-C，单位 mmol/L。',
  `triglycerides_mmol_l` decimal(7,3) DEFAULT NULL COMMENT '甘油三酯；用户确认的甘油三酯，单位 mmol/L。',
  `lab_confirmed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '化验是否确认；只有用户确认且带日期的医院化验值才进入正式特征。',
  `lab_recorded_at` bigint DEFAULT NULL COMMENT '化验日期时间；经确认医院化验报告的记录时间。',
  `client_updated_at` bigint NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`user_id`),
  KEY `idx_rhi_manual_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='云端 RHI 手工输入表；保存认证用户 Room-first 手工健康输入的云端副本，并按 updated_at 合并。';

-- ============================================================================
-- 表：rehealth_schema_migration
-- 中文名称：ReHealth 迁移版本表
-- 业务用途：记录 ReHealth 自定义软件库迁移版本；不是业务数据。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_schema_migration` (
  `version` varchar(64) NOT NULL COMMENT '版本；记录或配置版本；是否为乐观锁需结合实体 @Version 判断。',
  `applied_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ReHealth 迁移版本表；记录 ReHealth 自定义软件库迁移版本；不是业务数据。';

-- ============================================================================
-- 表：rehealth_telemetry_event_projection
-- 中文名称：遥测事件运营投影表
-- 业务用途：保存 Kafka 遥测生命周期事件的隐私安全运营投影。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_telemetry_event_projection` (
  `event_id` varchar(128) NOT NULL COMMENT '事件 ID；标识遥测或业务事件；具体关联以物理外键或事件契约为准。',
  `event_type` varchar(128) NOT NULL COMMENT '事件类型；标识质量、Outbox、归因或审计事件的业务类型。',
  `schema_id` varchar(128) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `batch_id` varchar(128) NOT NULL COMMENT '客户端批次 ID；客户端生成的稳定遥测批次业务键，重试时保持不变。',
  `tenant_ref` varchar(160) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `user_ref` varchar(160) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `device_ref` varchar(160) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `record_count` int NOT NULL COMMENT '记录总数；批次中全部规范化记录数量。',
  `persistence_status` varchar(32) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `quality_status` varchar(64) DEFAULT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `occurred_at` datetime(3) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`event_id`),
  KEY `idx_rehealth_telemetry_projection_tenant_time` (`tenant_ref`,`occurred_at`),
  KEY `idx_rehealth_telemetry_projection_device_time` (`tenant_ref`,`device_ref`,`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='遥测事件运营投影表；保存 Kafka 遥测生命周期事件的隐私安全运营投影。';

-- ============================================================================
-- 表：rehealth_telemetry_quality_case
-- 中文名称：遥测质量工单表
-- 业务用途：保存由遥测质量事件派生的运营质量工单。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_telemetry_quality_case` (
  `event_id` varchar(128) NOT NULL COMMENT '事件 ID；标识遥测或业务事件；具体关联以物理外键或事件契约为准。',
  `batch_id` varchar(128) NOT NULL COMMENT '客户端批次 ID；客户端生成的稳定遥测批次业务键，重试时保持不变。',
  `tenant_ref` varchar(160) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `device_ref` varchar(160) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `accepted_count` int NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `rejected_count` int NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `quality_status` varchar(64) NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  PRIMARY KEY (`event_id`),
  KEY `idx_rehealth_quality_case_tenant_time` (`tenant_ref`,`created_at`),
  CONSTRAINT `fk_rehealth_quality_projection` FOREIGN KEY (`event_id`) REFERENCES `rehealth_telemetry_event_projection` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='遥测质量工单表；保存由遥测质量事件派生的运营质量工单。';

-- ============================================================================
-- 表：rehealth_website_record
-- 中文名称：官网业务记录表
-- 业务用途：保存官网侧按租户隔离的结构化业务记录；具体记录类型由业务代码定义。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rehealth_website_record` (
  `id` varchar(64) NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` varchar(64) NOT NULL COMMENT '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型；保险审计事件所操作资源的类型。',
  `status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。',
  `payload_json` longtext NOT NULL COMMENT '载荷 JSON；保存可重放或版本化载荷；需结合表用途判断是否包含健康特征。',
  `created_by` varchar(64) NOT NULL COMMENT '创建用户 ID；创建当前研究、报告、结算包或快照的内部用户。',
  `created_at` datetime(3) NOT NULL COMMENT '创建时间；记录首次创建时间。',
  `updated_at` datetime(3) NOT NULL COMMENT '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。',
  PRIMARY KEY (`id`),
  KEY `idx_website_record_tenant_resource_created` (`tenant_id`,`resource_type`,`created_at`),
  KEY `idx_website_record_tenant_status` (`tenant_id`,`resource_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='官网业务记录表；保存官网侧按租户隔离的结构化业务记录；具体记录类型由业务代码定义。';

-- ============================================================================
-- 表：rep_demo_dxtj
-- 中文名称：rep_demo_dxtj 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rep_demo_dxtj` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '姓名',
  `gtime` datetime DEFAULT NULL COMMENT '雇佣日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职务',
  `jphone` varchar(125) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '家庭电话',
  `birth` datetime DEFAULT NULL COMMENT '出生日期',
  `hukou` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '户口所在地',
  `laddress` varchar(125) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系地址',
  `jperson` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '紧急联系人',
  `sex` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原注释：xingbie；TODO：字段中文业务含义待确认',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='rep_demo_dxtj 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rep_demo_employee
-- 中文名称：rep_demo_employee 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rep_demo_employee` (
  `id` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `num` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '编号',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '姓名',
  `sex` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '性别',
  `birthday` datetime DEFAULT NULL COMMENT '出生日期',
  `nation` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '民族',
  `political` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '政治面貌',
  `native_place` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '籍贯',
  `height` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '身高',
  `weight` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '体重',
  `health` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '健康状况',
  `id_card` varchar(80) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '身份证号',
  `education` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '学历',
  `school` varchar(80) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '毕业学校',
  `major` varchar(80) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '专业',
  `address` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '联系地址',
  `zip_code` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮编',
  `email` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原注释：Email；TODO：字段中文业务含义待确认',
  `phone` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `foreign_language` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '外语语种',
  `foreign_language_level` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '外语水平',
  `computer_level` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '计算机水平',
  `graduation_time` datetime DEFAULT NULL COMMENT '毕业时间',
  `arrival_time` datetime DEFAULT NULL COMMENT '到职时间',
  `positional_titles` varchar(30) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职称',
  `education_experience` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '教育经历',
  `work_experience` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '工作经历',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识0-正常,1-已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='rep_demo_employee 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rep_demo_gongsi
-- 中文名称：rep_demo_gongsi 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rep_demo_gongsi` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `gname` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '货品名称',
  `gdata` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '返利',
  `tdata` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '备注',
  `didian` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `zhaiyao` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `num` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='rep_demo_gongsi 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rep_demo_jianpiao
-- 中文名称：rep_demo_jianpiao 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rep_demo_jianpiao` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `bnum` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `ftime` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `sfkong` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `kaishi` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `jieshu` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `hezairen` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `jpnum` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `shihelv` varchar(125) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  `s_id` int NOT NULL COMMENT 'TODO：字段中文业务含义待确认',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='rep_demo_jianpiao 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rep_demo_order_main
-- 中文名称：rep_demo_order_main 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rep_demo_order_main` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `order_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '订单编码',
  `order_date` datetime DEFAULT NULL COMMENT '下单时间',
  `descc` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `xiala` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '下拉多选',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='rep_demo_order_main 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：rep_demo_order_product
-- 中文名称：rep_demo_order_product 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `rep_demo_order_product` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `product_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '产品名字',
  `price` double(32,0) DEFAULT NULL COMMENT '价格',
  `num` int DEFAULT NULL COMMENT '数量',
  `descc` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `order_fk_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '订单外键ID',
  `pro_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '产品类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='rep_demo_order_product 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_announcement
-- 中文名称：系统通告表
-- 业务用途：系统通告表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_announcement` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `titile` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '标题',
  `msg_content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '内容',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `sender` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '发布人',
  `priority` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '优先级（L低，M中，H高）',
  `msg_category` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '2' COMMENT '消息类型1:通知公告2:系统消息',
  `msg_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '通告对象类型（USER:指定用户，ALL:全体用户）',
  `send_status` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '发布状态（0未发布，1已发布，2已撤销）',
  `send_time` datetime DEFAULT NULL COMMENT '发布时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '撤销时间',
  `del_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '删除状态（0，正常，1已删除）',
  `bus_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '业务类型(email:邮件 bpm:流程 tenant_invite:租户邀请)',
  `bus_id` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '业务id',
  `open_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '打开方式(组件：component 路由：url)',
  `open_page` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组件/路由 地址',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `user_ids` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '指定用户',
  `msg_abstract` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '摘要/扩展业务参数',
  `dt_task_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '钉钉task_id，用于撤回消息',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  `files` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '附件',
  `visits_num` int DEFAULT NULL COMMENT '访问次数',
  `iz_top` int DEFAULT '0' COMMENT '是否置顶（0:否;  1:是）',
  `iz_approval` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否审批（0否 1是）',
  `bpm_status` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '流程状态',
  `msg_classify` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '消息归类',
  `notice_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '通知类型(system:系统消息、file:知识库、flow:流程、plan:日程计划、meeting:会议)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sanno_endtime` (`end_time`) USING BTREE,
  KEY `idx_sanno_start_time` (`start_time`) USING BTREE,
  KEY `idx_sanno_msg_type` (`msg_type`) USING BTREE,
  KEY `idx_sanno_send_status` (`send_status`) USING BTREE,
  KEY `idx_sanno_del_flag` (`del_flag`) USING BTREE,
  KEY `idx_sanno_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_sanno_sender` (`sender`) USING BTREE,
  KEY `idx_sanno_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统通告表';

-- ============================================================================
-- 表：sys_announcement_send
-- 中文名称：用户通告阅读标记表
-- 业务用途：用户通告阅读标记表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_announcement_send` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `annt_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '通告ID',
  `user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户id',
  `read_flag` int DEFAULT NULL COMMENT '阅读状态（0未读，1已读）',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `star_flag` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '标星状态( 1为标星 空/0没有标星)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sacm_annt_id` (`annt_id`) USING BTREE,
  KEY `idx_sacm_user_id` (`user_id`) USING BTREE,
  KEY `idx_sacm_read_flag` (`read_flag`) USING BTREE,
  KEY `idx_sacm_star_flag` (`star_flag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户通告阅读标记表';

-- ============================================================================
-- 表：sys_category
-- 中文名称：sys_category 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_category` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `pid` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父级节点',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型名称',
  `code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '类型编码',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '所属部门',
  `has_child` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否有子节点',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `index_scg_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_category 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_check_rule
-- 中文名称：sys_check_rule 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_check_rule` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名称',
  `rule_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则Code',
  `rule_json` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则JSON',
  `rule_description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则描述',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_scr_rule_code` (`rule_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='sys_check_rule 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_comment
-- 中文名称：系统评论回复表
-- 业务用途：系统评论回复表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_comment` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `table_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '表名',
  `table_data_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数据id',
  `from_user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '来源用户id',
  `to_user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '发送给用户id(允许为空)',
  `comment_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '评论id(允许为空，不为空时，则为回复)',
  `comment_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '回复内容',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_table_data_id` (`table_name`,`table_data_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统评论回复表';

-- ============================================================================
-- 表：sys_data_log
-- 中文名称：sys_data_log 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_data_log` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人真实名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `data_table` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '表名',
  `data_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据ID',
  `data_content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '数据内容',
  `data_version` int DEFAULT NULL COMMENT '版本号',
  `type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT 'json' COMMENT '类型',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sdl_data_table_id` (`data_table`,`data_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_data_log 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_data_source
-- 中文名称：sys_data_source 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_data_source` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据源编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据源名称',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `db_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据库类型',
  `db_driver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '驱动类',
  `db_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据源地址',
  `db_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '数据库名称',
  `db_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `db_password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属部门',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sdc_rule_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='sys_data_source 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_depart
-- 中文名称：组织机构表
-- 业务用途：组织机构表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_depart` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：ID；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `parent_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父机构ID',
  `depart_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '机构/部门名称',
  `depart_name_en` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '英文名',
  `depart_name_abbr` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '缩写',
  `depart_order` int DEFAULT '0' COMMENT '排序',
  `description` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `org_category` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '1' COMMENT '机构类别 1公司，2部门，3岗位，4子公司',
  `org_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '树深度层级level',
  `org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '机构编码',
  `mobile` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `fax` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '传真',
  `address` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '地址',
  `memo` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '状态（1启用，0不启用）',
  `del_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '删除状态（0，正常，1已删除）',
  `qywx_identifier` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '对接企业微信的ID',
  `ding_identifier` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '对接钉钉部门的ID',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  `iz_leaf` tinyint(1) DEFAULT '0' COMMENT '是否有叶子节点: 1是0否',
  `position_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职级id',
  `dep_post_parent_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '上级岗位id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_depart_tenant_org_code` (`tenant_id`,`org_code`),
  KEY `idx_sd_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_sd_depart_order` (`depart_order`) USING BTREE,
  KEY `idx_sd_position_id` (`position_id`) USING BTREE,
  KEY `idx_sd_dep_post_parent_id` (`dep_post_parent_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='组织机构表';

-- ============================================================================
-- 表：sys_depart_permission
-- 中文名称：部门权限表
-- 业务用途：部门权限表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_depart_permission` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `depart_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '部门id',
  `permission_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '权限id',
  `data_rule_ids` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据规则id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='部门权限表';

-- ============================================================================
-- 表：sys_depart_role
-- 中文名称：部门角色表
-- 业务用途：部门角色表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_depart_role` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `depart_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '部门id',
  `role_name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '部门角色名称',
  `role_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '部门角色编码',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='部门角色表';

-- ============================================================================
-- 表：sys_depart_role_permission
-- 中文名称：部门角色权限表
-- 业务用途：部门角色权限表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_depart_role_permission` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `depart_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '部门id',
  `role_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '角色id',
  `permission_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '权限id',
  `data_rule_ids` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据权限ids',
  `operate_date` datetime DEFAULT NULL COMMENT '操作时间',
  `operate_ip` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '操作ip',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sdrp_role_per_id` (`role_id`,`permission_id`) USING BTREE,
  KEY `idx_sdrp_role_id` (`role_id`) USING BTREE,
  KEY `idx_sdrp_per_id` (`permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='部门角色权限表';

-- ============================================================================
-- 表：sys_depart_role_user
-- 中文名称：部门角色用户表
-- 业务用途：部门角色用户表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_depart_role_user` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户id',
  `drole_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sdr_user_id` (`user_id`) USING BTREE,
  KEY `idx_sdr_role_id` (`drole_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='部门角色用户表';

-- ============================================================================
-- 表：sys_dict
-- 中文名称：sys_dict 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `dict_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典名称',
  `dict_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典编码',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `del_flag` int DEFAULT NULL COMMENT '删除状态',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `type` int(1) unsigned zerofill DEFAULT '0' COMMENT '字典类型0为string,1为number',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  `low_app_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '低代码应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sd_dict_code` (`dict_code`) USING BTREE,
  KEY `uk_sd_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_dict 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_dict_item
-- 中文名称：sys_dict_item 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_dict_item` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `dict_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典id',
  `item_text` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典项文本',
  `item_value` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '字典项值',
  `item_color` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字典项颜色',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `sort_order` int DEFAULT NULL COMMENT '排序',
  `status` int DEFAULT NULL COMMENT '状态（1启用 0不启用）',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间；Jeecg 公共创建时间字段。',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人；Jeecg 公共审计字段，记录最后更新用户。',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间；Jeecg 公共更新时间字段。',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sditem_role_dict_id` (`dict_id`) USING BTREE,
  KEY `idx_sditem_role_sort_order` (`sort_order`) USING BTREE,
  KEY `idx_sditem_status` (`status`) USING BTREE,
  KEY `idx_sditem_dict_val` (`dict_id`,`item_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_dict_item 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_files
-- 中文名称：知识库-文档管理
-- 业务用途：知识库-文档管理
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_files` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `file_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件名称',
  `url` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件地址',
  `file_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）',
  `store_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件上传类型(temp/本地上传(临时文件) manage/知识库)',
  `parent_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父级id',
  `tenant_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '租户id',
  `file_size` double(13,2) DEFAULT NULL COMMENT '文件大小（kb）',
  `iz_folder` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否文件夹(1：是  0：否)',
  `iz_root_folder` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否为1级文件夹，允许为空 (1：是 )',
  `iz_star` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否标星(1：是  0：否)',
  `down_count` int DEFAULT NULL COMMENT '下载次数',
  `read_count` int DEFAULT NULL COMMENT '阅读次数',
  `share_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '分享链接',
  `share_perms` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看)',
  `enable_down` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否允许下载(1：是  0：否)',
  `enable_updat` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否允许修改(1：是  0：否)',
  `del_flag` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '删除状态(0-正常,1-删除至回收站)',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `index_tenant_id` (`tenant_id`) USING BTREE,
  KEY `index_del_flag` (`del_flag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='知识库-文档管理';

-- ============================================================================
-- 表：sys_fill_rule
-- 中文名称：sys_fill_rule 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_fill_rule` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则名称',
  `rule_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则Code',
  `rule_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则实现类',
  `rule_params` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规则参数',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sfr_rule_code` (`rule_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='sys_fill_rule 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_form_file
-- 中文名称：sys_form_file 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_form_file` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `table_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '表名',
  `table_data_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '数据id',
  `file_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '关联文件id',
  `file_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '文件类型(text:文本, excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）)',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_table_form` (`table_name`,`table_data_id`) USING BTREE,
  KEY `index_file_id` (`file_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_form_file 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_gateway_route
-- 中文名称：sys_gateway_route 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_gateway_route` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `router_id` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '路由ID',
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '服务名',
  `uri` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '服务地址',
  `predicates` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '断言',
  `filters` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '过滤器',
  `retryable` int DEFAULT NULL COMMENT '是否重试:0-否 1-是',
  `strip_prefix` int DEFAULT NULL COMMENT '是否忽略前缀0-否 1-是',
  `persistable` int DEFAULT NULL COMMENT '是否为保留数据:0-否 1-是',
  `show_api` int DEFAULT NULL COMMENT '是否在接口文档中展示:0-否 1-是',
  `status` int DEFAULT NULL COMMENT '状态:0-无效 1-有效',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '所属部门',
  `del_flag` int DEFAULT NULL COMMENT '删除状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_gateway_route 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_log
-- 中文名称：系统日志表
-- 业务用途：系统日志表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `log_type` int DEFAULT NULL COMMENT '日志类型（1登录日志，2操作日志, 3.租户操作日志）',
  `log_content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '日志内容',
  `operate_type` int DEFAULT NULL COMMENT '操作类型',
  `userid` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '操作用户账号',
  `username` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '操作用户名称',
  `ip` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原注释：IP；TODO：字段中文业务含义待确认',
  `method` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求java方法',
  `request_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求路径',
  `request_param` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '请求参数',
  `request_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '请求类型',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  `client_type` varchar(5) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '客户端类型 pc:电脑端 app:手机端 h5:移动网页端',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sl_userid` (`userid`) USING BTREE,
  KEY `idx_sl_log_type` (`log_type`) USING BTREE,
  KEY `idx_sl_operate_type` (`operate_type`) USING BTREE,
  KEY `idx_sl_create_time` (`create_time`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='系统日志表';

-- ============================================================================
-- 表：sys_permission
-- 中文名称：菜单权限表
-- 业务用途：菜单权限表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `parent_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '父id',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '菜单标题',
  `url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '路径',
  `component` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组件',
  `is_route` tinyint(1) DEFAULT '1' COMMENT '是否路由菜单: 0:不是  1:是（默认值1）',
  `component_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组件名字',
  `redirect` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '一级菜单跳转地址',
  `menu_type` int DEFAULT NULL COMMENT '菜单类型(0:一级菜单; 1:子菜单:2:按钮权限)',
  `perms` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '菜单权限编码',
  `perms_type` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '权限策略1显示2禁用',
  `sort_no` double(8,2) DEFAULT NULL COMMENT '菜单排序',
  `always_show` tinyint(1) DEFAULT NULL COMMENT '聚合子路由: 1是0否',
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '菜单图标',
  `is_leaf` tinyint(1) DEFAULT NULL COMMENT '是否叶子节点:    1是0否',
  `keep_alive` tinyint(1) DEFAULT NULL COMMENT '是否缓存该页面:    1:是   0:不是',
  `hidden` tinyint DEFAULT '0' COMMENT '是否隐藏路由: 0否,1是',
  `hide_tab` tinyint DEFAULT NULL COMMENT '是否隐藏tab: 0否,1是',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `create_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` int DEFAULT '0' COMMENT '删除状态 0正常 1已删除',
  `rule_flag` int DEFAULT '0' COMMENT '是否添加数据权限1是0否',
  `status` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '按钮权限状态(0无效1有效)',
  `internal_or_external` tinyint(1) DEFAULT NULL COMMENT '外链菜单打开方式 0/内部打开 1/外部打开',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `index_menu_type` (`menu_type`) USING BTREE,
  KEY `index_menu_hidden` (`hidden`) USING BTREE,
  KEY `index_menu_status` (`status`) USING BTREE,
  KEY `index_menu_del_flag` (`del_flag`) USING BTREE,
  KEY `index_menu_url` (`url`) USING BTREE,
  KEY `index_menu_sort_no` (`sort_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='菜单权限表';

-- ============================================================================
-- 表：sys_permission_data_rule
-- 中文名称：sys_permission_data_rule 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_permission_data_rule` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：ID；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `permission_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '菜单ID',
  `rule_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '规则名称',
  `rule_column` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '字段',
  `rule_conditions` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '条件',
  `rule_value` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '规则值',
  `status` varchar(3) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '权限有效状态1有0否',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人；Jeecg 公共审计字段，记录创建用户。',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_spdr_permission_id` (`permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_permission_data_rule 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_position
-- 中文名称：职务级别
-- 业务用途：职务级别
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_position` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职务编码',
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职务级别名称',
  `post_level` int DEFAULT NULL COMMENT '职务等级',
  `company_id` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '公司id',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `sys_org_code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组织机构编码',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='职务级别';

-- ============================================================================
-- 表：sys_quartz_job
-- 中文名称：sys_quartz_job 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_quartz_job` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `del_flag` int DEFAULT NULL COMMENT '删除状态',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `job_class_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '任务类名',
  `cron_expression` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'cron表达式',
  `parameter` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '参数',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `status` int DEFAULT NULL COMMENT '状态 0正常 -1停止',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_quartz_job 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_role
-- 中文名称：角色表
-- 业务用途：角色表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `role_name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '角色名称',
  `role_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '角色编码',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_sys_role_role_code` (`role_code`) USING BTREE,
  KEY `idx_sysrole_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='角色表';

-- ============================================================================
-- 表：sys_role_index
-- 中文名称：角色首页表
-- 业务用途：角色首页表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_role_index` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色编码',
  `url` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '组件',
  `is_route` tinyint(1) DEFAULT '1' COMMENT '是否路由菜单: 0:不是  1:是（默认值1）',
  `priority` int DEFAULT '0' COMMENT '优先级',
  `status` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态0:无效 1:有效',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '所属部门',
  `relation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联关系(ROLE:角色 USER:用户)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sri_role_code` (`role_code`) USING BTREE,
  KEY `idx_sri_status` (`status`) USING BTREE,
  KEY `idx_sri_priority` (`priority`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色首页表';

-- ============================================================================
-- 表：sys_role_permission
-- 中文名称：角色权限表
-- 业务用途：角色权限表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `role_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '角色id',
  `permission_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '权限id',
  `data_rule_ids` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '数据权限ids',
  `operate_date` datetime DEFAULT NULL COMMENT '操作时间',
  `operate_ip` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '操作ip',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_srp_role_per_id` (`role_id`,`permission_id`) USING BTREE,
  KEY `idx_srp_role_id` (`role_id`) USING BTREE,
  KEY `idx_srp_permission_id` (`permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='角色权限表';

-- ============================================================================
-- 表：sys_sms
-- 中文名称：sys_sms 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_sms` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：ID；主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `es_title` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '消息标题',
  `es_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '发送方式：参考枚举MessageTypeEnum',
  `es_receiver` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '接收人',
  `es_param` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '发送所需参数Json格式',
  `es_content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '推送内容',
  `es_send_time` datetime DEFAULT NULL COMMENT '推送时间',
  `es_send_status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '推送状态 0未推送 1推送成功 2推送失败 -1失败不再发送',
  `es_send_num` int DEFAULT NULL COMMENT '发送次数 超过5次不再发送',
  `es_result` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '推送失败原因',
  `remark` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_ss_es_type` (`es_type`) USING BTREE,
  KEY `idx_ss_es_receiver` (`es_receiver`) USING BTREE,
  KEY `idx_ss_es_send_time` (`es_send_time`) USING BTREE,
  KEY `idx_ss_es_send_status` (`es_send_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_sms 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_sms_template
-- 中文名称：sys_sms_template 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_sms_template` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `template_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '模板标题',
  `template_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '模板CODE',
  `template_type` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '模板类型：1短信 2邮件 3微信',
  `template_category` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '模版分类：notice通知公告 other其他',
  `template_content` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '模板内容',
  `template_test_json` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '模板测试json',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `use_status` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否使用中 1是0否',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_sst_template_code` (`template_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_sms_template 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_table_white_list
-- 中文名称：系统表白名单
-- 业务用途：系统表白名单
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_table_white_list` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '允许的表名',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '允许的字段名，多个用逗号分割',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '状态，1=启用，0=禁用',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_sys_table_white_list_table_name` (`table_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='系统表白名单';

-- ============================================================================
-- 表：sys_tenant
-- 中文名称：多租户信息表
-- 业务用途：多租户信息表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_tenant` (
  `id` int NOT NULL COMMENT '租户编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `begin_date` datetime DEFAULT NULL COMMENT '开始时间',
  `end_date` datetime DEFAULT NULL COMMENT '结束时间',
  `status` int DEFAULT NULL COMMENT '状态 1正常 0冻结',
  `trade` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属行业',
  `company_size` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '公司规模',
  `company_address` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '公司地址',
  `company_logo` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '公司logo',
  `house_number` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '门牌号',
  `work_place` varchar(100) CHARACTER SET utf32 COLLATE utf32_general_ci DEFAULT NULL COMMENT '工作地点',
  `secondary_domain` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '二级域名',
  `login_bkgd_img` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录背景图片',
  `position` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职级',
  `department` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除状态(0-正常,1-已删除)',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `apply_status` int DEFAULT NULL COMMENT '允许申请管理员 1允许 0不允许',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='多租户信息表';

-- ============================================================================
-- 表：sys_tenant_pack
-- 中文名称：租户产品包
-- 业务用途：租户产品包
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_tenant_pack` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `tenant_id` int DEFAULT NULL COMMENT '租户id',
  `pack_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '产品包名',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '开启状态(0 未开启 1开启)',
  `remarks` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` date DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` date DEFAULT NULL COMMENT '更新时间',
  `pack_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编码,默认添加的三个管理员需要设置编码',
  `pack_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'custom' COMMENT '产品包类型(default 默认产品包 custom 自定义产品包)',
  `iz_sysn` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '自动分配给用户(0否 1是)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx__stp_tenant_id_pack_code` (`tenant_id`,`pack_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='租户产品包';

-- ============================================================================
-- 表：sys_tenant_pack_perms
-- 中文名称：租户产品包和菜单关系表
-- 业务用途：租户产品包和菜单关系表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_tenant_pack_perms` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键编号',
  `pack_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户产品包名称',
  `permission_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单id',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` date DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` date DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stpp_pack_id` (`pack_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='租户产品包和菜单关系表';

-- ============================================================================
-- 表：sys_tenant_pack_user
-- 中文名称：租户套餐人员表
-- 业务用途：租户套餐人员表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_tenant_pack_user` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `pack_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '租户产品包ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户ID',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int DEFAULT NULL COMMENT '状态 正常状态1 申请状态0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tpu_pack_id` (`pack_id`) USING BTREE,
  KEY `idx_tpu_user_id` (`user_id`) USING BTREE,
  KEY `idx_tpu_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_tpu_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='租户套餐人员表';

-- ============================================================================
-- 表：sys_third_account
-- 中文名称：sys_third_account 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_third_account` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '编号',
  `sys_user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方登录id',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态(1-正常,2-冻结)',
  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除状态(0-正常,1-已删除)',
  `realname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '真实姓名',
  `tenant_id` int DEFAULT '0' COMMENT '租户id',
  `third_user_uuid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方账号',
  `third_user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方app用户账号',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `third_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录来源',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_stat_third_type_user_id` (`third_type`,`third_user_id`) USING BTREE,
  UNIQUE KEY `uniq_sta_third_user_id_third_type` (`third_user_id`,`third_type`,`tenant_id`) USING BTREE,
  UNIQUE KEY `uniq_sta_third_user_uuid_third_type` (`third_user_uuid`,`third_type`,`tenant_id`) USING BTREE,
  KEY `idx_sta_sys_user_id_third_type` (`sys_user_id`,`third_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='sys_third_account 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_third_app_config
-- 中文名称：租户第三方配置表
-- 业务用途：租户第三方配置表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_third_app_config` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `tenant_id` int NOT NULL DEFAULT '0' COMMENT '租户id',
  `agent_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '钉钉/企业微信应用id',
  `client_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '钉钉/企业微信 应用id',
  `client_secret` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '钉钉/企业微信应用id对应的秘钥',
  `corp_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '钉钉企业id',
  `third_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '第三方类别(dingtalk 钉钉 wechat_enterprise 企业微信)',
  `status` int DEFAULT '1' COMMENT '是否启用(0-否,1-是)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_stac_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_stac_third_type` (`third_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='租户第三方配置表';

-- ============================================================================
-- 表：sys_ugroup
-- 中文名称：用户组表
-- 业务用途：用户组表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_ugroup` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `group_name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户组名称',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_su_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户组表';

-- ============================================================================
-- 表：sys_ugroup_user
-- 中文名称：用户组关系表
-- 业务用途：用户组关系表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_ugroup_user` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户id',
  `group_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户组id',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_suu_user_id` (`user_id`) USING BTREE,
  KEY `idx_suu_group_id` (`group_id`) USING BTREE,
  KEY `idx_suu_user_role_id` (`user_id`,`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户组关系表';

-- ============================================================================
-- 表：sys_user
-- 中文名称：用户表
-- 业务用途：用户表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `username` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '登录账号',
  `realname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '密码',
  `salt` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT 'md5密码盐',
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '头像',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `sex` tinyint(1) DEFAULT NULL COMMENT '性别(0-默认未知,1-男,2-女)',
  `email` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '电子邮件',
  `phone` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '电话',
  `org_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '登录会话的机构编码',
  `status` tinyint(1) DEFAULT NULL COMMENT '性别(1-正常,2-冻结)',
  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除状态(0-正常,1-已删除)',
  `third_id` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '第三方登录的唯一标识',
  `third_type` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '第三方类型',
  `activiti_sync` tinyint(1) DEFAULT NULL COMMENT '同步工作流引擎(1-同步,0-不同步)',
  `work_no` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '工号，唯一键',
  `telephone` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '座机号',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `user_identity` tinyint(1) DEFAULT NULL COMMENT '身份（1普通成员 2上级）',
  `depart_ids` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '负责部门',
  `client_id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '设备ID',
  `login_tenant_id` int DEFAULT NULL COMMENT '上次登录选择租户ID',
  `bpm_status` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '流程入职离职状态',
  `sign_enable` tinyint(1) DEFAULT NULL COMMENT '是否启用个性签名（0 否 1是）',
  `sign` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '个性签名',
  `main_dep_post_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '主岗位（部门岗位id）',
  `position_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '职务(字典)',
  `last_pwd_update_time` datetime DEFAULT NULL COMMENT '上一次修改密码的时间',
  `sort` int DEFAULT NULL COMMENT '排序',
  `iz_hide_contact` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '是否隐藏联系方式（0 否 1是）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_sys_user_work_no` (`work_no`) USING BTREE,
  UNIQUE KEY `uniq_sys_user_username` (`username`) USING BTREE,
  UNIQUE KEY `uniq_sys_user_phone` (`phone`) USING BTREE,
  UNIQUE KEY `uniq_sys_user_email` (`email`) USING BTREE,
  KEY `idx_su_status` (`status`) USING BTREE,
  KEY `idx_su_del_flag` (`del_flag`) USING BTREE,
  KEY `idx_su_del_username` (`username`,`del_flag`) USING BTREE,
  KEY `idx_su_main_dep_post_id` (`main_dep_post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户表';

-- ============================================================================
-- 表：sys_user_dep_post
-- 中文名称：sys_user_dep_post 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_user_dep_post` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户id',
  `dep_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门岗位id',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sudp_user_id` (`user_id`) USING BTREE,
  KEY `idx_sudp_dep_id` (`dep_id`) USING BTREE,
  KEY `idx_sudp_user_dep_id` (`user_id`,`dep_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='sys_user_dep_post 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_user_depart
-- 中文名称：sys_user_depart 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_user_depart` (
  `ID` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '原注释：id；TODO：字段中文业务含义待确认',
  `user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户id',
  `dep_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '部门id',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `idx_sud_user_dep_id` (`user_id`,`dep_id`) USING BTREE,
  KEY `idx_sud_user_id` (`user_id`) USING BTREE,
  KEY `idx_sud_dep_id` (`dep_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='sys_user_depart 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_user_position
-- 中文名称：sys_user_position 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_user_position` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户id',
  `position_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职位id',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sup_user_id` (`user_id`) USING BTREE,
  KEY `idx_sup_position_id` (`position_id`) USING BTREE,
  KEY `idx_sup_user_position_id` (`user_id`,`position_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='sys_user_position 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：sys_user_role
-- 中文名称：用户角色表
-- 业务用途：用户角色表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户id',
  `role_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '角色id',
  `tenant_id` int DEFAULT '0' COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sur_user_id` (`user_id`) USING BTREE,
  KEY `idx_sur_role_id` (`role_id`) USING BTREE,
  KEY `idx_sur_user_role_id` (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='用户角色表';

-- ============================================================================
-- 表：sys_user_tenant
-- 中文名称：用户租户关系表
-- 业务用途：用户租户关系表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_user_tenant` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户id',
  `tenant_id` int DEFAULT NULL COMMENT '租户id',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态(1 正常 2 离职 3 待审核 4 拒绝 5 邀请加入)',
  `create_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_sut_user_id` (`user_id`) USING BTREE,
  KEY `idx_sut_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_sut_user_rel_tenant` (`user_id`,`tenant_id`) USING BTREE,
  KEY `idx_sut_status` (`status`) USING BTREE,
  KEY `idx_sut_userid_status` (`user_id`,`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户租户关系表';

-- ============================================================================
-- 表：test_demo
-- 中文名称：test_demo 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_demo` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户名',
  `sex` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '性别',
  `age` int DEFAULT NULL COMMENT '年龄',
  `descc` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `birthday` datetime DEFAULT NULL COMMENT '生日',
  `user_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户编码',
  `file_kk` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '附件',
  `top_pic` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '头像',
  `chegnshi` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '城市',
  `ceck` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原注释：checkbox；TODO：字段中文业务含义待确认',
  `xiamuti` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '下拉多选',
  `search_sel` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '搜索下拉',
  `pop` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '弹窗',
  `sel_table` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '下拉字典表',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='test_demo 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_enhance_select
-- 中文名称：test_enhance_select 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_enhance_select` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市',
  `area` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='test_enhance_select 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_note
-- 中文名称：test_note 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_note` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属部门',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名',
  `age` int DEFAULT NULL COMMENT '年龄',
  `sex` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别',
  `birthday` datetime DEFAULT NULL COMMENT '生日',
  `contents` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '请假原因',
  `year` date DEFAULT NULL COMMENT '年',
  `sheng` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地区',
  `month` date DEFAULT NULL COMMENT '月',
  `begin_time` date DEFAULT NULL COMMENT '开始时间',
  `long_ids` bigint DEFAULT NULL COMMENT '长类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='test_note 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_online_link
-- 中文名称：test_online_link 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_online_link` (
  `id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `pid` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原注释：pid；TODO：字段中文业务含义待确认',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '原注释：name；名称；当前业务对象的名称；是否属于直接身份信息取决于所在表。',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='test_online_link 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_order_customer
-- 中文名称：test_order_customer 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_order_customer` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属部门',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户名字',
  `sex` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别',
  `age` int DEFAULT NULL COMMENT '年龄',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单id',
  `address` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='test_order_customer 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_order_main
-- 中文名称：test_order_main 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_order_main` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `order_code` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '订单编码',
  `order_date` datetime DEFAULT NULL COMMENT '下单时间',
  `descc` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `xiala` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '下拉多选',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='test_order_main 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_order_product
-- 中文名称：test_order_product 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_order_product` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `product_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '产品名字',
  `price` double(32,0) DEFAULT NULL COMMENT '价格',
  `num` int DEFAULT NULL COMMENT '数量',
  `descc` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '描述',
  `order_fk_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '订单外键ID',
  `pro_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '产品类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='test_order_product 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_person
-- 中文名称：test_person 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_person` (
  `id` varchar(36) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。',
  `create_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sex` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '性别',
  `name` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '用户名',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '请假原因',
  `be_date` datetime DEFAULT NULL COMMENT '请假时间',
  `qj_days` int DEFAULT NULL COMMENT '请假天数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='test_person 表；TODO：表的中文业务用途待对应模块负责人确认。';

-- ============================================================================
-- 表：test_shoptype_tree
-- 中文名称：test_shoptype_tree 表
-- 业务用途：TODO：表的中文业务用途待对应模块负责人确认。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `test_shoptype_tree` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属部门',
  `type_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品分类',
  `pic` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类图片',
  `pid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '父级节点',
  `has_child` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否有子节点',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='test_shoptype_tree 表；TODO：表的中文业务用途待对应模块负责人确认。';

SET FOREIGN_KEY_CHECKS = 1;
