-- =====================================================================
-- 商机管理系统 · 数据库 DDL（V1.0）
-- 依据：02-系统架构设计 §5（数据模型/索引）、04-权限码表、05-决策记录 D1/D2/D4
-- 范围：V1.0 = M01–M10、M12、M16、M18、M19(核心)。V1.1+ 表（报价/订单/发票/退款/费用/产品/短信）不建。
--
-- 工程约定（评审 4.3 要求的 DDL 级约束）：
--   引擎/字符集 : InnoDB / utf8mb4 / utf8mb4_0900_ai_ci（MySQL 8）
--   主键        : BIGINT UNSIGNED（DDL 用 AUTO_INCREMENT；生产分库改雪花 ID，应用层填充）
--   外键        : 不建物理外键，关系由应用层维护（多租户共享库 + 分库友好，MyBatis-Plus 实践）
--   多租户      : 所有业务表带 tenant_id（0=平台）；唯一索引一律以 tenant_id 打头；服务层强制注入
--   公共字段    : tenant_id, created_by, created_at, updated_by, updated_at, deleted
--   逻辑删除    : deleted TINYINT 0/1（MyBatis-Plus @TableLogic）
--   乐观锁      : 关键业务表加 version INT（@Version）
--   金额        : DECIMAL(15,2)
--   枚举        : VARCHAR + COMMENT 列出取值（配合 sys_dict）
-- 执行顺序：本文件（建表）→ 10-数据初始化-V1.0.sql（种子）
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 一、平台级（不带 tenant_id 或 tenant_id=0）
-- =====================================================================

-- 1. 租户
CREATE TABLE `tenant` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `name`        VARCHAR(100)    NOT NULL COMMENT '租户名称',
  `code`        VARCHAR(50)     NOT NULL COMMENT '租户编码（登录识别）',
  `domain`      VARCHAR(100)    NULL COMMENT '独立域名（登录识别）',
  `status`      VARCHAR(20)     NOT NULL DEFAULT 'TRIAL' COMMENT '状态：TRIAL试用/NORMAL正常/EXPIRING到期提醒/FROZEN冻结/DISABLED停用',
  `package_id`  BIGINT UNSIGNED NULL COMMENT '套餐ID',
  `expire_at`   DATETIME        NULL COMMENT '到期时间',
  `config_json` JSON            NULL COMMENT '租户级配置（容量上限/编号/短信签名等）',
  `created_by`  BIGINT UNSIGNED NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`  BIGINT UNSIGNED NULL,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`code`),
  UNIQUE KEY `uk_tenant_domain` (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户';

-- 2. 套餐（容量，决策 D8）
CREATE TABLE `sys_package` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name`             VARCHAR(50)     NOT NULL COMMENT '套餐名称',
  `max_users`        INT             NOT NULL DEFAULT 0 COMMENT '最大用户数，0=不限',
  `max_opportunities` INT            NOT NULL DEFAULT 0 COMMENT '最大商机数',
  `max_customers`    INT             NOT NULL DEFAULT 0 COMMENT '最大客户数',
  `max_attachment_gb` INT            NOT NULL DEFAULT 0 COMMENT '附件空间(GB)',
  `max_sms`          INT             NOT NULL DEFAULT 0 COMMENT '短信条数（V1.1）',
  `max_api_daily`    INT             NOT NULL DEFAULT 0 COMMENT 'API 日调用量',
  `max_file_mb`      INT             NOT NULL DEFAULT 50 COMMENT '单文件大小上限(MB)（D8）',
  `max_import_rows`  INT             NOT NULL DEFAULT 10000 COMMENT '单次导入行数上限（D8）',
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐/容量';

-- 3. 权限码字典（全局，对应 04-权限码表）
CREATE TABLE `permission` (
  `id`     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `code`   VARCHAR(64)     NOT NULL COMMENT '权限码，如 opp:create',
  `type`   VARCHAR(10)     NOT NULL COMMENT '类型：MENU菜单/OP操作',
  `name`   VARCHAR(100)    NOT NULL COMMENT '权限名称',
  `module` VARCHAR(50)     NULL COMMENT '所属模块 M01~M19',
  `remark` VARCHAR(200)    NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限码字典';

-- =====================================================================
-- 二、组织与权限（租户级）
-- =====================================================================

-- 4. 部门
CREATE TABLE `department` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父部门，0=根',
  `name`       VARCHAR(100)    NOT NULL COMMENT '部门名称',
  `leader_id`  BIGINT UNSIGNED NULL COMMENT '部门负责人',
  `sort`       INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `status`     VARCHAR(20)     NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `created_by` BIGINT UNSIGNED NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT UNSIGNED NULL,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dept_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';

-- 5. 用户（user 为保留字，用 sys_user）
CREATE TABLE `sys_user` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID，0=平台超管',
  `username`        VARCHAR(50)     NOT NULL COMMENT '登录账号',
  `mobile`          VARCHAR(20)     NULL COMMENT '手机号',
  `email`           VARCHAR(100)    NULL COMMENT '邮箱',
  `password_hash`   VARCHAR(100)    NOT NULL COMMENT '密码哈希(bcrypt)',
  `real_name`       VARCHAR(50)     NOT NULL COMMENT '姓名',
  `dept_id`         BIGINT UNSIGNED NULL COMMENT '主部门',
  `direct_leader_id` BIGINT UNSIGNED NULL COMMENT '直属上级',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED/RESIGNED离职',
  `mfa_enabled`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否开启MFA',
  `last_login_at`   DATETIME        NULL,
  `created_by`      BIGINT UNSIGNED NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`      BIGINT UNSIGNED NULL,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT         NOT NULL DEFAULT 0,
  `version`         INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant_username` (`tenant_id`, `username`),
  UNIQUE KEY `uk_user_tenant_mobile` (`tenant_id`, `mobile`),
  KEY `idx_user_tenant_dept` (`tenant_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 6. 角色
CREATE TABLE `role` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID，0=平台',
  `name`       VARCHAR(50)     NOT NULL COMMENT '角色名称',
  `code`       VARCHAR(50)     NOT NULL COMMENT '角色码：PLATFORM_ADMIN/TENANT_ADMIN/SALES_MANAGER/SALES/AUDITOR',
  `data_scope` VARCHAR(20)     NOT NULL DEFAULT 'SELF' COMMENT '数据范围：SELF/DEPT/DEPT_AND_SUB/TENANT/PLATFORM（D3/D4）',
  `status`     VARCHAR(20)     NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  `remark`     VARCHAR(200)    NULL,
  `created_by` BIGINT UNSIGNED NULL,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` BIGINT UNSIGNED NULL,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- 7. 角色-权限
CREATE TABLE `role_permission` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`     BIGINT UNSIGNED NOT NULL,
  `role_id`       BIGINT UNSIGNED NOT NULL,
  `permission_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rp` (`role_id`, `permission_id`),
  KEY `idx_rp_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

-- 8. 用户-角色
CREATE TABLE `user_role` (
  `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT UNSIGNED NOT NULL,
  `user_id`   BIGINT UNSIGNED NOT NULL,
  `role_id`   BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ur` (`user_id`, `role_id`),
  KEY `idx_ur_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- =====================================================================
-- 三、客户域（租户级）
-- =====================================================================

-- 9. 客户（唯一性规则见决策 D2）
CREATE TABLE `customer` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`   BIGINT UNSIGNED NOT NULL,
  `code`        VARCHAR(50)     NULL COMMENT '客户编号（编号规则生成）',
  `name`        VARCHAR(200)    NOT NULL COMMENT '客户名称',
  `credit_code` VARCHAR(50)     NULL COMMENT '统一社会信用代码（填写则租户内唯一，D2）',
  `type`        VARCHAR(30)     NULL COMMENT '客户类型',
  `industry`    VARCHAR(50)     NULL COMMENT '行业',
  `region`      VARCHAR(100)    NULL COMMENT '地区',
  `level`       VARCHAR(10)     NULL COMMENT '客户等级 A/B/C',
  `owner_id`    BIGINT UNSIGNED NULL COMMENT '负责人',
  `dept_id`     BIGINT UNSIGNED NULL COMMENT '所属部门',
  `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级客户，0=无（下级客户 M10-04）',
  `status`      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `is_pool`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否在公海（D5预留，V1.1启用）',
  `pool_recycled_at` DATETIME   NULL COMMENT '入池时间（D5预留）',
  `pool_reason` VARCHAR(200)    NULL COMMENT '入池原因（D5预留）',
  `created_by`  BIGINT UNSIGNED NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`  BIGINT UNSIGNED NULL,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT         NOT NULL DEFAULT 0,
  `version`     INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_credit` (`tenant_id`, `credit_code`),  -- 允许多 NULL：填了才唯一（D2）
  KEY `idx_customer_tenant_name` (`tenant_id`, `name`),
  KEY `idx_customer_tenant_owner` (`tenant_id`, `owner_id`),
  KEY `idx_customer_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户';

-- 10. 联系人
CREATE TABLE `contact` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`     BIGINT UNSIGNED NOT NULL,
  `customer_id`   BIGINT UNSIGNED NOT NULL COMMENT '所属客户',
  `name`          VARCHAR(50)     NOT NULL COMMENT '姓名',
  `title`         VARCHAR(50)     NULL COMMENT '职务/角色',
  `mobile`        VARCHAR(20)     NULL COMMENT '手机号（不强制唯一，D2）',
  `email`         VARCHAR(100)    NULL,
  `is_key_person` TINYINT         NOT NULL DEFAULT 0 COMMENT '是否关键联系人',
  `created_by`    BIGINT UNSIGNED NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`    BIGINT UNSIGNED NULL,
  `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_contact_tenant_customer` (`tenant_id`, `customer_id`),
  KEY `idx_contact_tenant_mobile` (`tenant_id`, `mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人';

-- =====================================================================
-- 四、商机域（租户级）
-- =====================================================================

-- 11. 商机阶段配置（租户可配，决策 D1 / M19-01）
CREATE TABLE `opportunity_stage` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`       BIGINT UNSIGNED NOT NULL,
  `code`            VARCHAR(30)     NOT NULL COMMENT '阶段码：prospecting/qualifying/proposal/negotiation/won/lost/void',
  `name`            VARCHAR(30)     NOT NULL COMMENT '阶段名称',
  `sort`            INT             NOT NULL DEFAULT 0,
  `win_rate`        INT             NOT NULL DEFAULT 0 COMMENT '默认赢率%',
  `color`           VARCHAR(10)     NULL COMMENT '配色：gray/cyan/blue/orange/green/red（设计系统3.2）',
  `stage_type`      VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/WON/LOST/VOID',
  `required_fields` JSON            NULL COMMENT '进入该阶段必填项（PRD 10.1）',
  `is_active`       TINYINT         NOT NULL DEFAULT 1,
  `created_by`      BIGINT UNSIGNED NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`      BIGINT UNSIGNED NULL,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机阶段配置';

-- 12. 商机
CREATE TABLE `opportunity` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`        BIGINT UNSIGNED NOT NULL,
  `code`             VARCHAR(50)     NULL COMMENT '商机编号（编号规则生成）',
  `title`            VARCHAR(200)    NOT NULL COMMENT '商机标题',
  `customer_id`      BIGINT UNSIGNED NOT NULL COMMENT '关联客户（必填）',
  `stage_id`         BIGINT UNSIGNED NOT NULL COMMENT '当前阶段',
  `source`           VARCHAR(50)     NULL COMMENT '来源渠道',
  `amount`           DECIMAL(15,2)   NOT NULL DEFAULT 0 COMMENT '预计金额',
  `win_rate`         INT             NOT NULL DEFAULT 0 COMMENT '赢率%（随阶段或手填）',
  `owner_id`         BIGINT UNSIGNED NULL COMMENT '负责人',
  `dept_id`          BIGINT UNSIGNED NULL COMMENT '负责人部门（冗余，便于数据范围）',
  `status`           VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/WON/LOST/VOID（随阶段type冗余）',
  `demand`           TEXT            NULL COMMENT '客户需求',
  `expected_close_at` DATETIME       NULL COMMENT '预计成交日期',
  `last_follow_at`   DATETIME        NULL COMMENT '最后跟进时间',
  `next_follow_at`   DATETIME        NULL COMMENT '下次跟进时间',
  `deal_amount`      DECIMAL(15,2)   NULL COMMENT '成交金额（赢单时）',
  `deal_at`          DATETIME        NULL COMMENT '成交时间',
  `is_pool`          TINYINT         NOT NULL DEFAULT 0 COMMENT '是否在公海（D5预留）',
  `pool_recycled_at` DATETIME        NULL COMMENT '入池时间（D5预留）',
  `pool_reason`      VARCHAR(200)    NULL COMMENT '入池原因（D5预留）',
  `created_by`       BIGINT UNSIGNED NULL,
  `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`       BIGINT UNSIGNED NULL,
  `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT         NOT NULL DEFAULT 0,
  `version`          INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_opp_tenant_code` (`tenant_id`, `code`),
  KEY `idx_opp_list` (`tenant_id`, `owner_id`, `stage_id`, `updated_at`),
  KEY `idx_opp_customer` (`tenant_id`, `customer_id`),
  KEY `idx_opp_status` (`tenant_id`, `status`),
  KEY `idx_opp_next_follow` (`tenant_id`, `next_follow_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机';

-- 13. 商机跟进
CREATE TABLE `opportunity_follow` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`      BIGINT UNSIGNED NOT NULL,
  `opportunity_id` BIGINT UNSIGNED NOT NULL,
  `follow_type`    VARCHAR(20)     NULL COMMENT '跟进方式：电话/拜访/会议/邮件',
  `content`        TEXT            NULL COMMENT '跟进内容',
  `result`         VARCHAR(200)    NULL COMMENT '客户反馈/结果',
  `next_time`      DATETIME        NULL COMMENT '下次跟进时间',
  `creator_id`     BIGINT UNSIGNED NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_follow_opp` (`tenant_id`, `opportunity_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机跟进记录';

-- 14. 商机协作（对象级授权，决策 D3）
CREATE TABLE `opportunity_collaborator` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`       BIGINT UNSIGNED NOT NULL,
  `opportunity_id`  BIGINT UNSIGNED NOT NULL,
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '协作人',
  `permission_json` JSON            NULL COMMENT '对象级授权：{view,follow,edit,upload,task}（M09-02）',
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REMOVED',
  `created_by`      BIGINT UNSIGNED NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collab` (`tenant_id`, `opportunity_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商机协作';

-- =====================================================================
-- 五、协同与通用（租户级，多态关联）
-- =====================================================================

-- 15. 任务（多态：object_type=opportunity/customer）
CREATE TABLE `task` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`   BIGINT UNSIGNED NOT NULL,
  `object_type` VARCHAR(20)     NULL COMMENT '关联对象类型：opportunity/customer',
  `object_id`   BIGINT UNSIGNED NULL COMMENT '关联对象ID',
  `title`       VARCHAR(200)    NOT NULL COMMENT '任务标题',
  `content`     TEXT            NULL,
  `assignee_id` BIGINT UNSIGNED NULL COMMENT '负责人',
  `creator_id`  BIGINT UNSIGNED NULL,
  `due_at`      DATETIME        NULL COMMENT '截止时间',
  `priority`    VARCHAR(10)     NOT NULL DEFAULT 'NORMAL' COMMENT 'HIGH/NORMAL/LOW',
  `status`      VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DOING/DONE/OVERDUE/CANCELLED',
  `created_by`  BIGINT UNSIGNED NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by`  BIGINT UNSIGNED NULL,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_task_object` (`tenant_id`, `object_type`, `object_id`),
  KEY `idx_task_assignee` (`tenant_id`, `assignee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务';

-- 16. 附件（多态）
CREATE TABLE `attachment` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`    BIGINT UNSIGNED NOT NULL,
  `object_type`  VARCHAR(20)     NOT NULL COMMENT 'opportunity/customer',
  `object_id`    BIGINT UNSIGNED NOT NULL,
  `file_name`    VARCHAR(255)    NOT NULL,
  `file_path`    VARCHAR(500)    NOT NULL COMMENT '对象存储路径，前缀 tenant_{id}/',
  `file_size`    BIGINT          NOT NULL DEFAULT 0 COMMENT '字节',
  `content_type` VARCHAR(100)    NULL,
  `version`      INT             NOT NULL DEFAULT 1 COMMENT '附件版本号（M16-03）',
  `uploader_id`  BIGINT UNSIGNED NULL,
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_attach_object` (`tenant_id`, `object_type`, `object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件';

-- =====================================================================
-- 六、审计与配置（租户级）
-- =====================================================================

-- 17. 操作审计日志（不可改不可删，无 deleted/updated）
CREATE TABLE `audit_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`   BIGINT UNSIGNED NOT NULL,
  `user_id`     BIGINT UNSIGNED NULL,
  `user_name`   VARCHAR(50)     NULL,
  `object_type` VARCHAR(30)     NULL COMMENT '对象类型',
  `object_id`   BIGINT UNSIGNED NULL,
  `action`      VARCHAR(50)     NOT NULL COMMENT '动作（权限码或语义动作）',
  `before_json` JSON            NULL COMMENT '变更前',
  `after_json`  JSON            NULL COMMENT '变更后',
  `result`      VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/DENIED（越权）',
  `ip`          VARCHAR(45)     NULL,
  `user_agent`  VARCHAR(255)    NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_object` (`tenant_id`, `object_type`, `object_id`),
  KEY `idx_audit_user` (`tenant_id`, `user_id`, `created_at`),
  KEY `idx_audit_action` (`tenant_id`, `action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

-- 18. 登录日志（M01-04）
CREATE TABLE `login_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `user_id`     BIGINT UNSIGNED NULL,
  `username`    VARCHAR(50)     NULL,
  `ip`          VARCHAR(45)     NULL,
  `location`    VARCHAR(100)    NULL,
  `device`      VARCHAR(200)    NULL,
  `result`      VARCHAR(20)     NOT NULL COMMENT 'SUCCESS/FAIL/LOCKED',
  `fail_reason` VARCHAR(100)    NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_login_user` (`tenant_id`, `user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

-- 19. 编号规则（M19-03）
CREATE TABLE `number_rule` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`   BIGINT UNSIGNED NOT NULL,
  `biz_type`    VARCHAR(30)     NOT NULL COMMENT '业务类型：opportunity/customer',
  `prefix`      VARCHAR(20)     NULL COMMENT '前缀，如 OPP',
  `date_format` VARCHAR(20)     NULL COMMENT '日期格式，如 yyyyMMdd',
  `seq_length`  INT             NOT NULL DEFAULT 4 COMMENT '流水位数',
  `current_seq` BIGINT          NOT NULL DEFAULT 0 COMMENT '当前流水',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_number_tenant_biz` (`tenant_id`, `biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='编号规则';

-- 20. 数据字典（M19，config:dict）
CREATE TABLE `sys_dict` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`  BIGINT UNSIGNED NOT NULL,
  `dict_type`  VARCHAR(50)     NOT NULL COMMENT '字典类型：customer_industry/opp_source/...',
  `item_code`  VARCHAR(50)     NOT NULL,
  `item_label` VARCHAR(100)    NOT NULL,
  `sort`       INT             NOT NULL DEFAULT 0,
  `is_active`  TINYINT         NOT NULL DEFAULT 1,
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT         NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`tenant_id`, `dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

-- 21. 导入任务（M06-03 异步导入）
CREATE TABLE `import_task` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`       BIGINT UNSIGNED NOT NULL,
  `biz_type`        VARCHAR(30)     NOT NULL COMMENT 'opportunity/customer/user',
  `file_name`       VARCHAR(255)    NULL,
  `status`          VARCHAR(20)     NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/DONE/FAILED',
  `total`           INT             NOT NULL DEFAULT 0,
  `success`         INT             NOT NULL DEFAULT 0,
  `failed`          INT             NOT NULL DEFAULT 0,
  `error_file_path` VARCHAR(500)    NULL COMMENT '失败行清单路径',
  `operator_id`     BIGINT UNSIGNED NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_import_operator` (`tenant_id`, `operator_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入任务';

SET FOREIGN_KEY_CHECKS = 1;
-- =====================================================================
-- DDL 结束。共 21 张表，覆盖 V1.0 全模块。
-- 接下来执行 10-数据初始化-V1.0.sql 写入权限码、默认角色、阶段、演示租户。
-- =====================================================================
