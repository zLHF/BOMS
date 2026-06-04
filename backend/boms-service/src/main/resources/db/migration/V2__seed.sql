-- =====================================================================
-- 商机管理系统 · 数据初始化（V1.0）
-- 依赖：先执行 09-数据库DDL-V1.0.sql
-- 内容：①权限码字典（对齐 04-权限码表）②套餐 ③演示租户/部门 ④默认角色+权限矩阵（04 §6）
--       ⑤演示用户+用户角色 ⑥默认商机阶段（决策 D1）⑦编号规则
-- 约定：平台超管 tenant_id=0（逻辑平台，无 tenant 表记录）；演示租户 code='hd-sales'
-- 口令：演示账号明文均为 123456；password_hash 为占位，正式由后端用 bcrypt 重新生成
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- ① 权限码字典（71 条：10 MENU + 61 OP），对齐《04-权限码表-V1.0》
-- ---------------------------------------------------------------------
INSERT INTO `permission` (`code`,`type`,`name`,`module`) VALUES
-- 菜单
('menu:dashboard','MENU','工作台','M-Dashboard'),
('menu:opportunity','MENU','商机管理','M05'),
('menu:customer','MENU','客户管理','M10'),
('menu:task','MENU','任务中心','M12'),
('menu:follow','MENU','跟进记录','M07'),
('menu:settings:org','MENU','组织用户','M03'),
('menu:settings:role','MENU','角色权限','M04'),
('menu:settings:stage','MENU','阶段/编号配置','M19'),
('menu:settings:audit','MENU','操作日志','M18'),
('menu:platform:tenant','MENU','租户管理','M02'),
-- 平台/租户
('platform:audit:view','OP','平台全局审计查看','M18'),
('tenant:view','OP','租户查看','M02'),
('tenant:create','OP','租户开通','M02'),
('tenant:update','OP','租户编辑','M02'),
('tenant:status','OP','租户启停冻结','M02'),
('tenant:package','OP','套餐/容量配置','M02'),
-- 组织与用户
('org:dept:view','OP','部门查看','M03'),
('org:dept:manage','OP','部门增删改/排序','M03'),
('org:user:view','OP','用户查看','M03'),
('org:user:create','OP','用户新增','M03'),
('org:user:update','OP','用户编辑','M03'),
('org:user:disable','OP','用户禁用','M03'),
('org:user:reset_pwd','OP','重置密码','M03'),
('org:user:import','OP','用户批量导入','M03'),
('org:user:assign_role','OP','分配角色','M03'),
('org:user:handover','OP','离职交接','M03'),
-- 角色权限
('role:view','OP','角色查看','M04'),
('role:create','OP','角色新增','M04'),
('role:update','OP','角色编辑','M04'),
('role:delete','OP','角色删除','M04'),
('role:assign_perm','OP','配置权限/数据范围','M04'),
-- 商机
('opp:view','OP','商机查看','M05'),
('opp:view:sub','OP','查看下属商机/下属协作','M05'),
('opp:create','OP','新增商机','M06'),
('opp:update','OP','编辑商机','M06'),
('opp:delete','OP','删除商机','M06'),
('opp:transfer','OP','转移商机','M06'),
('opp:import','OP','批量导入商机','M06'),
('opp:export','OP','导出商机','M05'),
('opp:follow:create','OP','新增跟进','M07'),
('opp:stage:advance','OP','阶段推进','M07'),
('opp:stage:rollback','OP','阶段回退','M07'),
('opp:win','OP','标记成交','M07'),
('opp:lose','OP','标记输单/作废','M07'),
('opp:collab:add','OP','添加协作人','M09'),
('opp:collab:remove','OP','取消协作','M09'),
-- 客户
('customer:view','OP','客户查看','M10'),
('customer:view:sub','OP','查看下属客户','M10'),
('customer:create','OP','新增客户','M10'),
('customer:update','OP','编辑客户','M10'),
('customer:delete','OP','删除客户','M10'),
('customer:transfer','OP','转移客户/归属','M10'),
('customer:import','OP','客户导入','M10'),
('customer:export','OP','客户导出','M10'),
('customer:contact:manage','OP','联系人增删改','M10'),
('customer:child:manage','OP','下级客户维护','M10'),
('customer:follow:create','OP','客户跟进','M10'),
-- 任务
('task:view','OP','任务查看','M12'),
('task:view:sub','OP','查看下属/部门任务','M12'),
('task:create','OP','新建任务','M12'),
('task:update','OP','任务编辑/状态','M12'),
('task:assign','OP','分派/转派任务','M12'),
('task:cancel','OP','取消任务','M12'),
-- 附件
('file:upload','OP','上传附件','M16'),
('file:download','OP','下载附件','M16'),
('file:delete','OP','删除附件','M16'),
-- 审计
('audit:view','OP','操作日志查询','M18'),
('audit:export','OP','审计日志导出','M18'),
-- 配置
('config:stage','OP','商机阶段配置','M19'),
('config:number','OP','编号规则配置','M19'),
('config:dict','OP','数据字典配置','M19');

-- ---------------------------------------------------------------------
-- ② 套餐（决策 D8 三档；V1.1 计费用，V1.0 用作容量上限）
-- ---------------------------------------------------------------------
INSERT INTO `sys_package`
 (`name`,`max_users`,`max_opportunities`,`max_customers`,`max_attachment_gb`,`max_file_mb`,`max_sms`,`max_api_daily`,`max_import_rows`) VALUES
('基础版',  20,  20000,  10000,   5,  30, 0,  5000,  5000),
('专业版', 100, 100000,  50000,  50,  50, 0, 10000, 10000),
('企业版', 500, 500000, 200000, 200, 100, 0, 50000, 50000);
SET @pkg := (SELECT id FROM sys_package WHERE name='专业版');  -- 演示租户用专业版

-- ---------------------------------------------------------------------
-- ③ 演示租户 + 部门
-- ---------------------------------------------------------------------
INSERT INTO `tenant` (`name`,`code`,`domain`,`status`,`package_id`,`expire_at`)
VALUES ('华东销售中心','hd-sales','hd.example.com','NORMAL',@pkg,'2027-12-31 23:59:59');
SET @tid := (SELECT id FROM tenant WHERE code='hd-sales');

INSERT INTO `department` (`tenant_id`,`parent_id`,`name`,`sort`) VALUES (@tid, 0, '华东销售中心', 1);
SET @dept_root := (SELECT id FROM department WHERE tenant_id=@tid AND name='华东销售中心');
INSERT INTO `department` (`tenant_id`,`parent_id`,`name`,`sort`) VALUES
(@tid, @dept_root, '销售一部', 1),
(@tid, @dept_root, '销售二部', 2);
SET @dept1 := (SELECT id FROM department WHERE tenant_id=@tid AND name='销售一部');
SET @dept2 := (SELECT id FROM department WHERE tenant_id=@tid AND name='销售二部');

-- ---------------------------------------------------------------------
-- ④ 角色（平台角色 tenant_id=0；租户角色 tenant_id=@tid）数据范围见 D3/D4
-- ---------------------------------------------------------------------
INSERT INTO `role` (`tenant_id`,`name`,`code`,`data_scope`,`remark`) VALUES
(0,    '平台超级管理员','PLATFORM_ADMIN','PLATFORM','平台运营，不直接看租户业务数据'),
(@tid, '租户管理员',   'TENANT_ADMIN', 'TENANT',  '本租户全部配置'),
(@tid, '销售主管',     'SALES_MANAGER','DEPT_AND_SUB','本部门及下级'),
(@tid, '销售人员',     'SALES',        'SELF',    '本人数据 + 协作授权'),
(@tid, '只读审计员',   'AUDITOR',      'TENANT',  '全租户只读（D4）');

SET @r_pa := (SELECT id FROM role WHERE tenant_id=0    AND code='PLATFORM_ADMIN');
SET @r_ta := (SELECT id FROM role WHERE tenant_id=@tid AND code='TENANT_ADMIN');
SET @r_sm := (SELECT id FROM role WHERE tenant_id=@tid AND code='SALES_MANAGER');
SET @r_sa := (SELECT id FROM role WHERE tenant_id=@tid AND code='SALES');
SET @r_au := (SELECT id FROM role WHERE tenant_id=@tid AND code='AUDITOR');

-- ---------------------------------------------------------------------
-- ⑤ 角色-权限矩阵（对齐 04 §6；○/— 不授予）
-- ---------------------------------------------------------------------
-- 平台超管：平台码 + 工作台
INSERT INTO `role_permission` (`tenant_id`,`role_id`,`permission_id`)
SELECT 0, @r_pa, id FROM `permission`
WHERE `code` IN ('menu:dashboard','menu:platform:tenant','platform:audit:view',
                 'tenant:view','tenant:create','tenant:update','tenant:status','tenant:package');

-- 租户管理员：本租户全部码（排除平台码）
INSERT INTO `role_permission` (`tenant_id`,`role_id`,`permission_id`)
SELECT @tid, @r_ta, id FROM `permission`
WHERE `code` NOT LIKE 'tenant:%' AND `code` NOT LIKE 'platform:%' AND `code` <> 'menu:platform:tenant';

-- 销售主管
INSERT INTO `role_permission` (`tenant_id`,`role_id`,`permission_id`)
SELECT @tid, @r_sm, id FROM `permission` WHERE `code` IN (
 'menu:dashboard','menu:opportunity','menu:customer','menu:task','menu:follow',
 'opp:view','opp:view:sub','opp:create','opp:update','opp:transfer','opp:import','opp:export',
 'opp:follow:create','opp:stage:advance','opp:stage:rollback','opp:win','opp:lose','opp:collab:add','opp:collab:remove',
 'customer:view','customer:view:sub','customer:create','customer:update','customer:transfer',
 'customer:contact:manage','customer:child:manage','customer:import','customer:export','customer:follow:create',
 'task:view','task:view:sub','task:create','task:update','task:assign','task:cancel',
 'file:upload','file:download','file:delete');

-- 销售人员
INSERT INTO `role_permission` (`tenant_id`,`role_id`,`permission_id`)
SELECT @tid, @r_sa, id FROM `permission` WHERE `code` IN (
 'menu:dashboard','menu:opportunity','menu:customer','menu:task','menu:follow',
 'opp:view','opp:create','opp:update','opp:follow:create','opp:stage:advance','opp:win','opp:lose',
 'opp:collab:add','opp:collab:remove',
 'customer:view','customer:create','customer:update','customer:contact:manage','customer:child:manage','customer:follow:create',
 'task:view','task:create','task:update','task:assign','task:cancel',
 'file:upload','file:download');

-- 只读审计员（D4：全租户只读）
INSERT INTO `role_permission` (`tenant_id`,`role_id`,`permission_id`)
SELECT @tid, @r_au, id FROM `permission` WHERE `code` IN (
 'menu:dashboard','menu:settings:audit','opp:view','customer:view','audit:view','audit:export');

-- ---------------------------------------------------------------------
-- ⑥ 演示用户（口令 123456，hash 占位需后端重置）+ 用户角色
-- ---------------------------------------------------------------------
SET @pwd := '$2a$10$7EqJtq98hPqEX7fNZaFWoO0aQ7Qh0WzN3xUW9oFqf3pQ1Qh0WzN3x'; -- 占位
INSERT INTO `sys_user` (`tenant_id`,`username`,`mobile`,`email`,`password_hash`,`real_name`,`dept_id`,`status`) VALUES
(0,    'platform_admin','13900000000','admin@platform.com',@pwd,'平台管理员',NULL,'ENABLED'),
(@tid, 'tadmin',        '13900000001','admin@hd.com',     @pwd,'租户管理员',@dept_root,'ENABLED'),
(@tid, 'zhangwei',      '13800000001','zhangwei@hd.com',  @pwd,'张伟',      @dept1,'ENABLED'),
(@tid, 'lina',          '13800000002','lina@hd.com',      @pwd,'李娜',      @dept1,'ENABLED'),
(@tid, 'wangqiang',     '13800000003','wangqiang@hd.com', @pwd,'王强',      @dept2,'ENABLED'),
(@tid, 'auditor',       '13800000009','auditor@hd.com',   @pwd,'审计员',    @dept_root,'ENABLED');

SET @u_pa := (SELECT id FROM sys_user WHERE tenant_id=0    AND username='platform_admin');
SET @u_ta := (SELECT id FROM sys_user WHERE tenant_id=@tid AND username='tadmin');
SET @u_zw := (SELECT id FROM sys_user WHERE tenant_id=@tid AND username='zhangwei');
SET @u_ln := (SELECT id FROM sys_user WHERE tenant_id=@tid AND username='lina');
SET @u_wq := (SELECT id FROM sys_user WHERE tenant_id=@tid AND username='wangqiang');
SET @u_au := (SELECT id FROM sys_user WHERE tenant_id=@tid AND username='auditor');

-- 上下级 + 部门负责人：张伟为销售一部负责人，李娜直属张伟
UPDATE `department` SET `leader_id`=@u_zw WHERE id=@dept1;
UPDATE `department` SET `leader_id`=@u_wq WHERE id=@dept2;
UPDATE `sys_user` SET `direct_leader_id`=@u_zw WHERE id=@u_ln;

INSERT INTO `user_role` (`tenant_id`,`user_id`,`role_id`) VALUES
(0,    @u_pa, @r_pa),
(@tid, @u_ta, @r_ta),
(@tid, @u_zw, @r_sm),
(@tid, @u_ln, @r_sa),
(@tid, @u_wq, @r_sa),
(@tid, @u_au, @r_au);

-- ---------------------------------------------------------------------
-- ⑦ 默认商机阶段（决策 D1：高保真原型阶段链 + 赢率梯度 + 配色）
-- ---------------------------------------------------------------------
INSERT INTO `opportunity_stage` (`tenant_id`,`code`,`name`,`sort`,`win_rate`,`color`,`stage_type`,`required_fields`) VALUES
(@tid,'prospecting','初步接触',1,10,'gray','IN_PROGRESS',NULL),
(@tid,'qualifying', '需求确认',2,30,'cyan','IN_PROGRESS',NULL),
(@tid,'proposal',   '方案报价',3,50,'blue','IN_PROGRESS', JSON_ARRAY('contact')),
(@tid,'negotiation','商务谈判',4,70,'orange','IN_PROGRESS',NULL),
(@tid,'won',        '赢单',    5,100,'green','WON', JSON_ARRAY('deal_amount','deal_at')),
(@tid,'lost',       '输单',    6,0,'red','LOST', JSON_ARRAY('reason','competitor','review')),
(@tid,'void',       '作废',    7,0,'gray','VOID', JSON_ARRAY('reason'));

-- ---------------------------------------------------------------------
-- ⑧ 编号规则（M19-03）
-- ---------------------------------------------------------------------
INSERT INTO `number_rule` (`tenant_id`,`biz_type`,`prefix`,`date_format`,`seq_length`) VALUES
(@tid,'opportunity','OPP','yyyyMMdd',4),
(@tid,'customer',   'CUST','yyyyMMdd',4);

-- ---------------------------------------------------------------------
-- ⑨ 字典样例（M19，可选）
-- ---------------------------------------------------------------------
INSERT INTO `sys_dict` (`tenant_id`,`dict_type`,`item_code`,`item_label`,`sort`) VALUES
(@tid,'opp_source','website','官网咨询',1),
(@tid,'opp_source','campaign','市场活动',2),
(@tid,'opp_source','referral','客户转介绍',3),
(@tid,'opp_source','bidding','投标',4),
(@tid,'customer_level','A','A级',1),
(@tid,'customer_level','B','B级',2),
(@tid,'customer_level','C','C级',3);

-- =====================================================================
-- 初始化结束。
-- 验证建议：
--   SELECT r.code, COUNT(*) FROM role r JOIN role_permission rp ON rp.role_id=r.id GROUP BY r.code;
--   登录演示：租户 hd-sales / 账号 zhangwei / 口令 123456（hash 需后端重置）
-- =====================================================================
