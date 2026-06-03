# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目说明

**商机管理系统（BOMS）**，多租户、多用户、多组织的 B2B 销售协同平台，基于 `商机管理系统_PRD_多用户多租户版.md` 与 `docs/` 设计文档实现。

**当前状态：V1.0 Sprint 4 已完成**。平台底座（M01–M04）+ 客户/商机主体（M05/M06/M07/M10）+ 任务中心/详情多Tab/协作/公海/审计/配置（M08/M09/M12/D5/M16/M19）均已落地。

## 目录结构

| 目录 | 说明 |
|------|------|
| `backend/` | **后端**：Spring Boot 3.3 + MyBatis-Plus + Flyway + MySQL 8（Java 21）。见 `backend/README.md` |
| `frontend/` | **前端**：Vue3 + TS + Vite + Element Plus + Pinia + Vue Router。见 `frontend/README.md` |
| `deploy/` | `docker-compose.yml`：隔离的 MySQL(3307) + Redis(6380) + MinIO(9100/9101) |
| `docs/` | 设计文档 01–10（含 09 DDL / 10 初始化 SQL，已纳入 Flyway） |
| `business-opportunity-highfi-vue3/` | 高保真原型（Element Plus 视觉基座，前端 token 来源，仅参考） |
| `business-opportunity-prototype/` | 早期纯 Vue3 结构原型（仅参考，**非开发目标**） |

> 真实开发在 `backend/` 与 `frontend/`，两个 `business-opportunity-*` 仅作视觉/结构参考。

## 常用命令

```bash
# 1) 基础设施（与本机其他项目隔离，勿碰 3306 上的 yunqian-mysql-1）
cd deploy && docker compose up -d

# 2) 后端（本机 Maven 默认 JDK17，必须指向 21，否则报 "release version 21 not supported"）
cd backend && export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -DskipTests spring-boot:run          # 首次启动 Flyway 自动建表+初始化；服务在 :8081

# 3) 前端（/api 经 vite 代理到 8081）
cd frontend && npm install
npm run dev                               # http://localhost:5174
npm run typecheck                         # vue-tsc（提交前必过）
npm run build
```

## 架构概览

### 后端（`com.boms`，单模块清晰分包）

- **统一响应/异常**：`common/result/R`、`common/exception/GlobalExceptionHandler`（401/403 映射 HTTP 状态）。
- **多租户隔离**：`JwtAuthFilter` 解析 token → 经 `PrincipalLoader` 加载权限/角色/数据范围/部门 → 写入 `TenantContext`(ThreadLocal)，请求结束清理。`BomsTenantHandler` + MyBatis-Plus `TenantLineInnerInterceptor` 对业务表自动追加 `tenant_id`（平台表与 tenant_id=0 跳过）。**tenant_id 永远以 token 为准，前端不可信传入**。
- **鉴权**：`@RequirePerm("码")` AOP（缺登录态→401，缺权限码→403）。`@AuditLog` AOP 落 `audit_log`。
- **数据范围(ABAC)**：`DataScope`(SELF/DEPT/DEPT_AND_SUB/TENANT/PLATFORM) + `ScopeService.apply(wrapper, ownerCol, deptCol)`，多角色取最宽，对 view 查询追加过滤。
- **迁移**：`docs/09`→`backend/.../db/migration/V1__schema.sql`，`docs/10`→`V2__seed.sql`，Flyway 启动时建 21 表 + 种子。
- **模块**：`modules/auth`(登录/me/改密)、`modules/system`(租户/部门/用户/角色/权限)、`modules/customer`、`modules/opportunity`(商机+阶段+跟进+协作+配置)、`modules/task`(任务中心)、`modules/audit`(审计日志查询)、`modules/pool`(公海池)。

### 前端（`frontend/src`，按 02 架构 §8）

- `main.ts` 装配 Element Plus + Pinia + Router + `v-perm` 指令；`App.vue` 仅渲染 `<RouterView>`。
- `router/`（路由表带 `perm`/`group` meta + 登录/权限守卫）、`stores/auth.ts`、`services/`（axios 拦截器注入 token、解包 `R<T>`、401 跳登录）、`directives/perm.ts`（无权限从 DOM 移除）。
- `layouts/DefaultLayout.vue`：侧边栏菜单按用户 `menu:*` 权限码过滤分组渲染。
- `styles/tokens.css`（设计 token，主色 `#2563eb`）+ `base.css`（来自高保真）。
- `views/`：登录、工作台、客户（含公海）、商机（含详情多Tab 7标签页+公海）、任务中心、设置(组织用户/角色权限/阶段编号配置/操作日志)、平台(租户管理)。

### 数据库

- 21 张表（见 `docs/09`）。公共字段 `tenant_id/created_by/created_at/updated_by/updated_at/deleted`，逻辑删除 `@TableLogic`，乐观锁 `@Version`，金额 `DECIMAL(15,2)`。
- 独立 docker 实例 `boms-mysql`（端口 **3307**，库 `boms`，账密 `boms/boms_pw`，root `boms_root_pw`）。

## 已完成进度

| 阶段 | 范围 | 里程碑 |
|------|------|--------|
| 阶段 0 | 前后端工程骨架 + 建库初始化 + 租户/RBAC/审计基座 | M0：登录→工作台、租户上下文贯穿、审计落库 |
| V1.0 Sprint 1 | M01 认证 / M02 租户(创建即引导角色矩阵+管理员+阶段) / M03 组织用户 / M04 角色权限 | M1：两租户、角色受限、跨租户拒绝+审计 |
| V1.0 Sprint 2–3 | M10 客户(+联系人/D2唯一性) / M05 商机多视图 / M06 新增编辑 / M07 阶段流转(D1)+跟进 | M2：客户商机可建可查、多视图、数据按角色受限、跨租户隔离 |
| V1.0 Sprint 4 | M16 审计日志查看 / M19 阶段编号配置 / M12 任务中心 / M08 商机详情多Tab(7标签) / M09 协作 / D5 公海池 | M3：任务CRUD+数据范围、商机详情7Tab、协作人管理、手动公海回收/认领、审计日志查询、阶段/编号配置 |

**待办**：M06 批量导入、M16 附件中心(MinIO文件上传)、自动回收定时任务(公海)、报价/订单占位、跟进记录独立页、数据字典配置、CI、后端多模块拆分。

## 关键约定与易踩坑

- **JDK**：跑后端前必须 `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`。
- **端口**：后端 8081（8080 被本机 yunqian-backend 占）、前端 5174、MySQL 3307。**不要动 3306 上的 `yunqian-mysql-1`（无关项目）**。
- **登录态接口**：当前用户在 `GET /api/auth/me`（不是 `/api/me`）。
- **MyBatis-Plus 3.5.9**：租户/分页等 SQL 解析拦截器在独立 `mybatis-plus-jsqlparser` 依赖。
- **演示账号**（口令均 `123456`，种子 `password_hash` 占位，`boms.demo.allow-plain-password=true` 放行明文）：
  - `platform_admin`（平台超管，租户码留空）
  - `tadmin`(TENANT_ADMIN) / `zhangwei`(**SALES_MANAGER**) / `lina`·`wangqiang`(SALES) / `auditor`(只读)，租户码 `hd-sales`
- **新增页面需五处对齐**（06 §4）：页面(06) ↔ 接口(07) ↔ 权限码(04) ↔ 测试(08) ↔ 表(09)。权限码源头是 `docs/04`，改权限码先改 04。
- 业务决策回指 `docs/05` ADR D1–D8（阶段链、客户唯一性、协作、审计员、公海、报价、短信、容量）。
- **任务表无 dept_id**：task 表通过 assigneeId 关联用户，"sub"视图先查部门用户ID列表再过滤。
- **公海字段**：Opportunity 和 Customer 实体都有 `isPool`、`poolRecycledAt`、`poolReason` 三个字段。
- **商机详情路由**：`/opportunity/:id`（无 group，不显示在侧边栏菜单）。

## API 端点一览

### 认证 `/api/auth`
`POST /login` `GET /me` `POST /change-password` `POST /logout`

### 系统管理
- 租户：`GET/POST /api/platform/tenants` `PUT/PATCH /api/platform/tenants/{id}`
- 用户：`GET/POST /api/users` `PUT /api/users/{id}` `PATCH /api/users/{id}/disable` `POST /api/users/{id}/reset-password` `PUT /api/users/{id}/roles`
- 角色：`GET/POST /api/roles` `PUT/DELETE /api/roles/{id}` `PUT /api/roles/{id}/permissions`
- 部门：`GET/POST /api/depts` `PUT/DELETE /api/depts/{id}`

### 业务
- 客户：`GET/POST /api/customers` `PUT/DELETE /api/customers/{id}` `GET/POST /api/customers/{id}/contacts`
- 商机：`GET/POST /api/opportunities` `GET /api/opportunities/{id}` `GET /api/opportunities/{id}/detail`(富化) `PUT/DELETE /api/opportunities/{id}` `POST /{id}/stage` `POST /{id}/stage/rollback` `POST /{id}/win` `POST /{id}/lose` `POST /{id}/transfer` `GET/POST /{id}/follows` `GET /{id}/contacts` `GET/POST /{id}/collaborators` `DELETE /{id}/collaborators/{cid}`
- 任务：`GET/POST /api/tasks` `PUT /api/tasks/{id}` `POST /api/tasks/{id}/assign` `POST /api/tasks/{id}/cancel` `POST /api/tasks/{id}/status`
- 配置：`GET /api/config/stages` `PUT /api/config/stages/{id}` `GET/PUT /api/config/number-rules`
- 审计：`GET /api/audit-logs`
- 公海：`GET /api/pool/opportunities` `GET /api/pool/customers` `POST /api/pool/opportunities/{id}/claim` `POST /api/pool/opportunities/{id}/recycle` `POST /api/pool/customers/{id}/claim` `POST /api/pool/customers/{id}/recycle`

## 文档导航

设计文档 `docs/01`–`10`（README 有索引与阅读顺序）。后端/前端各有 `README.md` 记录已实现接口、运行方式与演示账号。
