# BOMS 后端

商机管理系统后端，基于 Spring Boot 3.3 + MyBatis-Plus + MySQL 8（Java 21）。

## 技术栈

- Spring Boot 3.3 / Java 21
- MyBatis-Plus 3.5.9（租户行级隔离 / 逻辑删除 / 乐观锁 / 自动填充）
- Flyway（DDL 09 → `V1__schema.sql`，初始化 10 → `V2__seed.sql`）
- MySQL 8（隔离容器，端口 3307）
- JWT（jjwt）+ bcrypt

## 前置：基础设施

```bash
cd ../deploy
docker compose up -d            # MySQL 3307 + Redis 6380 + MinIO 9100/9101
```

## 运行

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # 本机 Maven 默认 JDK17，需指向 21
mvn -DskipTests spring-boot:run                     # 首次启动 Flyway 自动建表+初始化
```

启动后服务在 `http://127.0.0.1:8081`（8080 已被占用）。

## 演示账号（种子数据，口令均 123456）

| 账号 | 租户 | 角色 |
|------|------|------|
| platform_admin | 平台(0) | 平台超管 |
| tadmin | hd-sales | 租户管理员 |
| zhangwei | hd-sales | 销售主管（销售一部） |
| lina | hd-sales | 销售（销售一部） |
| wangqiang | hd-sales | 销售（销售二部） |
| auditor | hd-sales | 审计员（全租户只读） |

> 种子 `password_hash` 为占位，由 `boms.demo.allow-plain-password=true` 放行明文 123456。

## API 端点一览

### M01 认证 `/api/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录（返回 JWT + 用户信息） |
| GET | `/auth/me` | 当前用户（角色+权限+数据范围） |
| POST | `/auth/change-password` | 改密（bcrypt） |
| POST | `/auth/logout` | 登出 |

### M02 租户 `/api/platform/tenants`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/platform/tenants` | 租户列表（平台级） |
| POST | `/platform/tenants` | 创建租户（自动引导角色+管理员+阶段） |
| PUT | `/platform/tenants/{id}` | 更新租户 |
| PATCH | `/platform/tenants/{id}/status` | 启停租户 |
| PUT | `/platform/tenants/{id}/package` | 更换套餐 |

### M03 组织 `/api/users` `/api/depts`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/users` | 用户列表/创建（数据范围过滤） |
| PUT | `/users/{id}` | 更新用户 |
| PATCH | `/users/{id}/disable` | 停用 |
| POST | `/users/{id}/reset-password` | 重置密码 |
| PUT | `/users/{id}/roles` | 分配角色 |
| GET/POST | `/depts` | 部门树 |
| PUT/DELETE | `/depts/{id}` | 更新/删除部门 |

### M04 角色 `/api/roles` `/api/permissions`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/roles` | 角色列表/创建 |
| PUT/DELETE | `/roles/{id}` | 更新/删除 |
| PUT | `/roles/{id}/permissions` | 配置权限码+数据范围 |
| GET | `/permissions` | 权限码字典 |

### M10 客户 `/api/customers`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | `/customers` | 客户列表/创建（D2唯一性校验） |
| PUT/DELETE | `/customers/{id}` | 更新/删除 |
| GET/POST | `/customers/{id}/contacts` | 联系人列表/新增 |
| PUT/DELETE | `/customers/{id}/contacts/{cid}` | 更新/删除联系人 |

### M05-M07 商机 `/api/opportunities`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/opportunities` | 多视图列表（all/mine/sub/won/pool） |
| GET | `/opportunities/{id}` | 单个商机 |
| GET | `/opportunities/{id}/detail` | **富化详情**（含客户名/阶段名/负责人名） |
| POST | `/opportunities` | 创建商机 |
| PUT/DELETE | `/opportunities/{id}` | 更新/删除 |
| POST | `/opportunities/{id}/stage` | 阶段推进 |
| POST | `/opportunities/{id}/stage/rollback` | 阶段回退（仅主管/管理员） |
| POST | `/opportunities/{id}/win` | 赢单 |
| POST | `/opportunities/{id}/lose` | 输单 |
| POST | `/opportunities/{id}/transfer` | 转移负责人 |
| GET/POST | `/opportunities/{id}/follows` | 跟进记录 |
| GET | `/opportunities/{id}/contacts` | 商机关联客户联系人 |
| GET/POST | `/opportunities/{id}/collaborators` | 协作人列表/添加 |
| DELETE | `/opportunities/{id}/collaborators/{cid}` | 移除协作人 |

### M12 任务 `/api/tasks`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tasks` | 任务列表（视图+筛选+数据范围） |
| POST | `/tasks` | 创建任务 |
| PUT | `/tasks/{id}` | 更新任务 |
| POST | `/tasks/{id}/assign` | 指派 |
| POST | `/tasks/{id}/cancel` | 取消 |
| POST | `/tasks/{id}/status` | 状态变更（开始/完成） |

### M19 配置 `/api/config`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/config/stages` | 阶段列表 |
| PUT | `/config/stages/{id}` | 更新阶段配置 |
| GET | `/config/number-rules` | 编号规则列表 |
| PUT | `/config/number-rules/{bizType}` | 更新编号规则 |

### M16 审计日志 `/api/audit-logs`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/audit-logs` | 审计日志分页查询（支持对象类型/ID/用户/操作码/日期范围筛选） |

### D5 公海池 `/api/pool`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/pool/opportunities` | 商机公海列表 |
| POST | `/pool/opportunities/{id}/claim` | 认领商机 |
| POST | `/pool/opportunities/{id}/recycle` | 回收商机到公海 |
| GET | `/pool/customers` | 客户公海列表 |
| POST | `/pool/customers/{id}/claim` | 认领客户 |
| POST | `/pool/customers/{id}/recycle` | 回收客户到公海 |

## 模块结构

```
com.boms
├── common/           # 通用基础（响应、异常、租户、鉴权、审计AOP、数据范围）
└── modules/
    ├── auth/         # M01 认证
    ├── system/       # M02-M04 租户/部门/用户/角色/权限
    ├── customer/     # M10 客户+联系人
    ├── opportunity/  # M05-M07 商机+阶段+跟进+协作+配置
    ├── task/         # M12 任务中心
    ├── audit/        # M16 审计日志查询
    └── pool/         # D5 公海池
```

## 已完成进度

| Sprint | 范围 | 状态 |
|--------|------|------|
| 阶段 0 | 工程骨架 + 建库 + 租户/RBAC/审计基座 | ✅ |
| Sprint 1 | M01 认证 / M02 租户 / M03 组织用户 / M04 角色权限 | ✅ |
| Sprint 2-3 | M10 客户 / M05-M07 商机（列表/新增/阶段/跟进） | ✅ |
| Sprint 4 | M16 审计 / M19 配置 / M12 任务 / M08 详情Tab / M09 协作 / D5 公海 | ✅ |

## 待办

- M06 批量导入、M16 附件上传(MinIO)、公海自动回收定时任务
- 报价/订单占位、数据字典配置
- CI、后端多模块拆分
