-- =====================================================================
-- Sprint 6 数据库迁移
-- 内容：算力平台对接映射表
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 算力平台租户映射（算力平台企业 ↔ BOMS租户）
CREATE TABLE IF NOT EXISTS `cpn_tenant_mapping` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `boms_tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT 'BOMS租户ID',
  `cpn_enterprise_name` VARCHAR(200)    NOT NULL COMMENT '算力平台企业名称',
  `cpn_tenant_id`       VARCHAR(100)    NULL     COMMENT '算力平台租户ID（getUserInfoByToken 返回的 tenantID）',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cpn_tenant_boms` (`boms_tenant_id`),
  KEY `idx_cpn_enterprise` (`cpn_enterprise_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算力平台租户映射';

-- 2. 算力平台用户映射（算力平台用户 ↔ BOMS用户）
CREATE TABLE IF NOT EXISTS `cpn_user_mapping` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `boms_user_id`    BIGINT UNSIGNED NOT NULL COMMENT 'BOMS用户ID',
  `cpn_login_name`  VARCHAR(100)    NOT NULL COMMENT '算力平台登录账号（pltAccountLogin）',
  `cpn_user_cn`     VARCHAR(100)    NULL     COMMENT '算力平台用户姓名（pltUserCn）',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cpn_user_boms` (`boms_user_id`),
  KEY `idx_cpn_login` (`cpn_login_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算力平台用户映射';
