# 商机管理系统 · API 接口清单（V1.0）

> 范围：V1.0 = M01–M10、M12、M16、M18、M19(核心)。V1.1+ 接口不在此列。
> 权限码列引用《04-权限码表-V1.0》；阶段/客户唯一性规则引用《05-关键设计决策记录》D1/D2。
> **全局约定见第 1 节，逐接口见第 2 节，关键接口详情见第 3 节。**

---

## 1. 全局约定

| 约定 | 说明 |
|------|------|
| Base Path | `/api` |
| 鉴权 | 除 `/auth/login` 外，全部需 `Authorization: Bearer <token>` |
| 响应包装 | `{ "code": 0, "message": "ok", "data": ... }`，`code≠0` 为业务错误 |
| 分页 | 请求 `page`(从1)/`size`；响应 `{ total, page, size, list:[] }` |
| 租户隔离 | 租户级接口的 `tenant_id` **由后端从 token 解析**，前端禁止传入（PRD 4.2） |
| 权限校验 | 写接口标 `@RequirePerm("码")`；`view` 接口标是否走数据范围过滤 |
| 审计 | "审计=是"的接口必须写 `audit_log`（含前后值，PRD M18-01） |
| 通用异常 | `401` 未登录/Token失效；`403` 无权限/越权/租户冻结；`400` 参数/必填校验；`409` 冲突（重复/状态不允许）；`429` 限流 |
| ID 规范 | 主键雪花/UUID；业务编号（商机/客户）按 `config:number` 规则生成 |

**列含义**：`数据范围`=该查询是否经服务层数据范围过滤；`审计`=是否写审计日志。

---

## 2. 接口一览

### 2.1 认证 M01

| 方法 | 路径 | 权限码 | 数据范围 | 审计 | 说明 |
|------|------|--------|:--:|:--:|------|
| POST | `/auth/login` | 无 | - | 是(登录日志) | 租户识别+登录，返回 token+菜单+权限码集 |
| POST | `/auth/logout` | 登录态 | - | 是 | 退出 |
| POST | `/auth/refresh` | 登录态 | - | - | 刷新 token |
| GET | `/auth/me` | 登录态 | - | - | 当前用户+菜单+权限码+数据范围 |
| POST | `/auth/change-password` | 登录态 | - | 是 | 改密（首次/过期/主动） |

### 2.2 租户管理 M02（平台级，不带 tenant_id）

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/platform/tenants` | `tenant:view` | - | - |
| POST | `/platform/tenants` | `tenant:create` | - | 是 |
| PUT | `/platform/tenants/{id}` | `tenant:update` | - | 是 |
| PATCH | `/platform/tenants/{id}/status` | `tenant:status` | - | 是 |
| PUT | `/platform/tenants/{id}/package` | `tenant:package` | - | 是 |

### 2.3 组织与用户 M03

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/depts` | `org:dept:view` | - | - |
| POST | `/depts` | `org:dept:manage` | - | 是 |
| PUT | `/depts/{id}` | `org:dept:manage` | - | 是 |
| DELETE | `/depts/{id}` | `org:dept:manage` | - | 是 |
| PUT | `/depts/sort` | `org:dept:manage` | - | 是 |
| GET | `/users` | `org:user:view` | - | - |
| POST | `/users` | `org:user:create` | - | 是 |
| PUT | `/users/{id}` | `org:user:update` | - | 是 |
| PATCH | `/users/{id}/disable` | `org:user:disable` | - | 是 |
| POST | `/users/{id}/reset-password` | `org:user:reset_pwd` | - | 是 |
| POST | `/users/import` | `org:user:import` | - | 是 |
| PUT | `/users/{id}/roles` | `org:user:assign_role` | - | 是 |
| POST | `/users/{id}/handover` | `org:user:handover` | - | 是 |

### 2.4 角色权限 M04

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/roles` | `role:view` | - | - |
| POST | `/roles` | `role:create` | - | 是 |
| PUT | `/roles/{id}` | `role:update` | - | 是 |
| DELETE | `/roles/{id}` | `role:delete` | - | 是 |
| PUT | `/roles/{id}/permissions` | `role:assign_perm` | - | 是 |
| GET | `/permissions` | 登录态 | - | - | 权限码字典 |

### 2.5 商机 M05/M06/M07/M08/M09

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/opportunities` | `opp:view`（`view=sub/sub-coop` 加 `opp:view:sub`） | 是 | - |
| GET | `/opportunities/{id}` | `opp:view` | 是 | - |
| POST | `/opportunities` | `opp:create` | - | 是 |
| PUT | `/opportunities/{id}` | `opp:update` | 是 | 是 |
| DELETE | `/opportunities/{id}` | `opp:delete` | 是 | 是 |
| POST | `/opportunities/{id}/transfer` | `opp:transfer` | 是 | 是 |
| POST | `/opportunities/import` | `opp:import` | - | 是 |
| GET | `/opportunities/import/{taskId}` | `opp:import` | - | - | 异步进度/结果 |
| GET | `/opportunities/export` | `opp:export` | 是 | 是 |
| GET/POST | `/opportunities/{id}/follows` | `opp:view` / `opp:follow:create` | 是 | 是(POST) |
| POST | `/opportunities/{id}/stage` | `opp:stage:advance` | 是 | 是 |
| POST | `/opportunities/{id}/stage/rollback` | `opp:stage:rollback` | 是 | 是 |
| POST | `/opportunities/{id}/win` | `opp:win` | 是 | 是 |
| POST | `/opportunities/{id}/lose` | `opp:lose` | 是 | 是 |
| GET | `/opportunities/{id}/collaborators` | `opp:view` | 是 | - |
| POST | `/opportunities/{id}/collaborators` | `opp:collab:add` | 是 | 是 |
| DELETE | `/opportunities/{id}/collaborators/{cid}` | `opp:collab:remove` | 是 | 是 |

> 关联订单/报价/费用 Tab 为 V1.0 占位（决策见冲突 1 / 03 计划），**无对应 V1.0 接口**，列表返回空 + 前端"V1.1 上线"提示。

### 2.6 客户 M10

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/customers` | `customer:view`（`view=sub` 加 `customer:view:sub`） | 是 | - |
| GET | `/customers/{id}` | `customer:view` | 是 | - |
| POST | `/customers` | `customer:create` | - | 是 |
| PUT | `/customers/{id}` | `customer:update` | 是 | 是 |
| DELETE | `/customers/{id}` | `customer:delete` | 是 | 是 |
| POST | `/customers/{id}/transfer` | `customer:transfer` | 是 | 是 |
| POST | `/customers/import` | `customer:import` | - | 是 |
| GET | `/customers/export` | `customer:export` | 是 | 是 |
| GET/POST/PUT/DELETE | `/customers/{id}/contacts[/{cid}]` | `customer:view` / `customer:contact:manage` | 是 | 是(写) |
| GET/POST | `/customers/{id}/children` | `customer:view` / `customer:child:manage` | 是 | 是(POST) |
| POST | `/customers/{id}/follows` | `customer:follow:create` | 是 | 是 |

### 2.7 任务 M12

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/tasks` | `task:view`（`view=sub` 加 `task:view:sub`） | 是 | - |
| POST | `/tasks` | `task:create` | - | 是 |
| PUT | `/tasks/{id}` | `task:update` | 是 | 是 |
| POST | `/tasks/{id}/assign` | `task:assign` | 是 | 是 |
| POST | `/tasks/{id}/cancel` | `task:cancel` | 是 | 是 |

### 2.8 附件 M16

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| POST | `/files/sign-upload` | `file:upload` | 是 | - | 返回签名上传 URL + fileId |
| POST | `/files` | `file:upload` | 是 | 是 | 确认上传，绑定 object_type/object_id |
| GET | `/files/{id}/sign-download` | `file:download` | 是 | 是 | 返回限时签名 URL |
| DELETE | `/files/{id}` | `file:delete` | 是 | 是 |

### 2.9 审计 M18

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/audit-logs` | `audit:view` | 是(租户内) | - |
| GET | `/audit-logs/export` | `audit:export` | 是 | 是 |

### 2.10 系统配置 M19（核心）

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/config/stages` | 登录态 | - | - | 阶段字典（前端渲染） |
| PUT | `/config/stages` | `config:stage` | - | 是 |
| GET/PUT | `/config/number-rules` | 登录态 / `config:number` | - | 是(PUT) |
| GET/PUT | `/config/dicts` | 登录态 / `config:dict` | - | 是(PUT) |

### 2.11 工作台

| 方法 | 路径 | 权限码 | 数据范围 | 审计 |
|------|------|--------|:--:|:--:|
| GET | `/dashboard/summary` | `opp:view` | 是 | - | KPI/漏斗/待办/排行 |

---

## 3. 关键接口详情

### 3.1 登录

```
POST /api/auth/login        权限码：无    审计：是（登录日志，PRD M01-04）
请求：{ tenant, username, password, captcha }   # tenant=租户编码/域名/手机邮箱
返回：{ token, refreshToken, user:{id,name,role,deptId},
        menus:[...], perms:["opp:view",...], dataScope:"DEPT_AND_SUB" }
异常：401 账号或密码错误；423 连续失败锁定（M01-02）；403 租户冻结/停用（M02-02）；400 验证码错误
```

### 3.2 商机列表

```
GET /api/opportunities      权限码：opp:view（view=sub/sub-coop 需 opp:view:sub）
                            数据范围：是    审计：否
请求(query)：view=all|mine|sub|coop|sub-coop|won, stageId?, ownerId?, source?,
            keyword?, amountMin?, amountMax?, nextFollowStart?, nextFollowEnd?,
            overdue?, page, size, sort
返回：{ total, page, size, list:[ {id, code, title, customerName, stageId, stageName,
        amount, ownerName, collaborators:[], nextFollowAt, source, updatedAt} ] }
说明：服务层按角色 data_scope + view 复合过滤；view=sub* 校验 opp:view:sub，否则 403
异常：403 无 opp:view / 无 opp:view:sub
```

### 3.3 新增商机

```
POST /api/opportunities     权限码：opp:create    审计：是    租户隔离：是
请求：{ title, customerId, stageId?, source, amount?, ownerId?, contactIds?,
        demand?, expectedCloseAt?, collaborators?:[{userId,permissionJson}] }
返回：{ id, code, title, stageId, ownerId, createdAt }
规则：stageId 缺省=阶段配置首个阶段（D1：prospecting）；code 按 config:number 生成
异常：400 必填缺失；409 疑似重复客户（D2 软提示，可带 force=true 继续）；403 无权限
```

### 3.4 阶段推进 / 成交 / 输单（D1）

```
POST /api/opportunities/{id}/stage          权限码：opp:stage:advance   审计：是
请求：{ toStageId }
规则：按 config:stage 校验目标阶段必填项（PRD 10.1），不满足则 400 返回缺失字段清单
异常：400 必填校验失败；409 阶段流转不合法；403 无权限/越权

POST /api/opportunities/{id}/win            权限码：opp:win   审计：是
请求：{ amount, dealAt, orderRef?|dealNote }   # 成交必填（D1）
POST /api/opportunities/{id}/lose           权限码：opp:lose  审计：是
请求：{ reason, competitor, review }          # 输单必填（D1）

POST /api/opportunities/{id}/stage/rollback 权限码：opp:stage:rollback  审计：是
规则：仅 SALES_MANAGER / TENANT_ADMIN（D1），其他角色 403
```

### 3.5 添加协作人（对象级授权，D3）

```
POST /api/opportunities/{id}/collaborators  权限码：opp:collab:add   审计：是
请求：{ targets:[{type:"user|dept|role", id}], permissionJson:{
          view:true, follow:true, edit:false, upload:true, task:true } }
说明：permissionJson 存 opportunity_collaborator，按 4.6 交集语义生效；
     被授权人对该商机的动作仍受其角色权限码约束（冲突 4）
异常：403 非负责人/无 opp:collab:add；409 已是协作人
```

### 3.6 附件签名上传/下载（租户隔离，M16）

```
POST /api/files/sign-upload   权限码：file:upload
请求：{ objectType, objectId, fileName, size, contentType }
返回：{ fileId, uploadUrl, expiresIn }      # 对象存储直传，路径前缀 tenant_{id}/
POST /api/files               权限码：file:upload   审计：是   # 确认并落记录

GET /api/files/{id}/sign-download  权限码：file:download   审计：是
返回：{ downloadUrl, expiresIn }            # 限时签名；校验租户+数据权限
异常：403 跨租户/无 file:download；404 文件不存在
```

### 3.7 批量导入商机（异步，M06-03）

```
POST /api/opportunities/import   权限码：opp:import   审计：是
请求：multipart 文件 + templateVersion
返回：{ taskId }                            # 异步
GET /api/opportunities/import/{taskId}
返回：{ status:"running|done|failed", total, success, failed,
        errorFileUrl? }                     # 失败行可下载（M06-03）
规则：模板版本不匹配→400；校验失败行不入库，不产生脏数据（验收 TC-4xx）
```

### 3.8 操作日志查询（M18）

```
GET /api/audit-logs          权限码：audit:view   数据范围：租户内
请求(query)：objectType?, objectId?, userId?, action?, start?, end?, page, size
返回：{ total, list:[ {id, userName, objectType, objectId, action,
        beforeJson, afterJson, ip, createdAt} ] }
说明：仅本租户；普通角色不可删除日志（无 delete 接口）
```
