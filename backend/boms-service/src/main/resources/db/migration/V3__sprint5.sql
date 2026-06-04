-- =====================================================================
-- Sprint 5 数据库迁移
-- 内容：公海配置表、附件字段补充、权限码补充、字典种子
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 公海池配置表（每租户一行）
CREATE TABLE IF NOT EXISTS `pool_config` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id`           BIGINT UNSIGNED NOT NULL,
  `auto_recycle_enabled` TINYINT        NOT NULL DEFAULT 1 COMMENT '是否启用自动回收',
  `no_follow_days`      INT             NOT NULL DEFAULT 30 COMMENT '无有效跟进天数阈值',
  `protection_days`     INT             NOT NULL DEFAULT 7  COMMENT '领取保护期天数',
  `personal_limit`      INT             NOT NULL DEFAULT 50 COMMENT '个人领取上限',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pool_config_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公海池配置';

-- 初始化每个租户的默认配置
INSERT INTO `pool_config` (`tenant_id`, `auto_recycle_enabled`, `no_follow_days`, `protection_days`, `personal_limit`)
SELECT id, 1, 30, 7, 50 FROM `tenant`;

-- 2. 附件表增加 status 字段（PENDING=待确认上传, CONFIRMED=已确认）
ALTER TABLE `attachment` ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' COMMENT 'PENDING/CONFIRMED' AFTER `content_type`;

-- 3. 新增权限码（公海配置菜单 + 操作）
INSERT INTO `permission` (`code`,`type`,`name`,`module`) VALUES
('config:pool','OP','公海池配置','M05'),
('menu:settings:pool','MENU','公海配置','M05'),
('menu:settings:dict','MENU','数据字典','M19');

-- 4. 为租户管理员追加新权限（租户管理员拥有所有非平台码，新码自动覆盖，无需额外操作）
-- 为销售主管追加公海配置查看权限
SET @tid := (SELECT id FROM tenant WHERE code='hd-sales');
SET @r_sm := (SELECT id FROM role WHERE tenant_id=@tid AND code='SALES_MANAGER');
INSERT IGNORE INTO `role_permission` (`tenant_id`,`role_id`,`permission_id`)
SELECT @tid, @r_sm, id FROM `permission` WHERE `code` IN ('config:pool','menu:settings:pool');

-- 5. 补充字典种子数据（跟进方式、客户行业）
INSERT INTO `sys_dict` (`tenant_id`,`dict_type`,`item_code`,`item_label`,`sort`) VALUES
(@tid,'follow_type','电话','电话',1),
(@tid,'follow_type','拜访','拜访',2),
(@tid,'follow_type','会议','会议',3),
(@tid,'follow_type','微信','微信',4),
(@tid,'follow_type','邮件','邮件',5),
(@tid,'follow_type','方案','方案沟通',6),
(@tid,'customer_industry','IT','IT/互联网',1),
(@tid,'customer_industry','manufacturing','制造业',2),
(@tid,'customer_industry','finance','金融',3),
(@tid,'customer_industry','education','教育',4),
(@tid,'customer_industry','healthcare','医疗健康',5),
(@tid,'customer_industry','retail','零售/消费',6);
