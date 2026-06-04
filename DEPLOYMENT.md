# BOMS 部署与上线文档

> 本文件是 BOMS **唯一的部署/运维权威文档**，覆盖：环境准备、数据库初始化、启动方式、生产注意事项、常见问题、上线前检查清单。
> 环境变量的**完整清单以根目录 `.env.example` 为唯一来源**，本文不再逐项罗列，新增变量只需改 `.env.example`。

## 1. 服务器要求

| 项目 | 建议版本/资源 |
|------|---------------|
| OS | Linux x86_64，建议 Ubuntu 22.04+/CentOS Stream 9+ |
| CPU/内存 | 演示 2C/4G；生产建议 4C/8G 起，根据导入量和并发扩容 |
| Java | JDK/JRE 21 |
| Node.js | 20.x（仅构建前端时需要） |
| Maven | 3.9+（仅构建后端时需要） |
| Docker | 24+，Compose v2 |
| 数据库 | MySQL 8.0，字符集 utf8mb4，时区 Asia/Shanghai |
| 对象存储 | MinIO 或兼容 S3 的对象存储 |

## 2. 环境变量准备

1. 复制根目录 `.env.example` 为 `.env`，或在 CI/CD/容器平台中配置等价变量。**完整变量清单与默认值见 `.env.example`**，本文只强调下面的生产必改/可选项。
2. 生产环境**必须修改**：
   - `SPRING_DATASOURCE_PASSWORD`
   - `BOMS_JWT_SECRET`（至少 32 字节随机字符串）
   - `BOMS_DEMO_ALLOW_PLAIN_PASSWORD=false`
   - `BOMS_MINIO_ACCESS_KEY` / `BOMS_MINIO_SECRET_KEY`
   - `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` / `MINIO_ROOT_PASSWORD`
3. **可选：算力平台对接（CPN，SSO + 心跳）**。默认关闭，不配置不影响其余功能；需要对接时设置：
   - `BOMS_CPN_ENABLED=true`
   - `BOMS_CPN_GATEWAY`（算力平台网关地址）
   - `BOMS_CPN_APP_KEY` / `BOMS_CPN_APP_SECRET`（平台分配，secret 走密钥管理系统注入）
   - `BOMS_CPN_HEARTBEAT_CRON`（可选，默认每 5 分钟）

   > 启用后，后端的心跳定时任务 `CpnHeartbeatScheduler` 才会注册（受 `@ConditionalOnProperty(boms.cpn.enabled=true)` 控制）。
4. 不要把真实 `.env`、数据库备份、对象存储密钥、算力平台 secret 提交到代码仓库。

## 3. 数据库初始化

后端启动时 Flyway 会自动执行以下迁移：

| 脚本 | 作用 |
|------|------|
| `backend/boms-service/src/main/resources/db/migration/V1__schema.sql` | 基础表结构 |
| `backend/boms-service/src/main/resources/db/migration/V2__seed.sql` | 权限、演示租户、角色、用户、阶段、字典种子数据 |
| `backend/boms-service/src/main/resources/db/migration/V3__sprint5.sql` | 公海、附件状态、字典补充 |
| `backend/boms-service/src/main/resources/db/migration/V4__sprint6.sql` | 算力平台映射表 |

部署前请确认数据库用户具备建表、建索引、变更表结构权限。若生产库已存在历史数据，先在备份库验证 Flyway 迁移。

## 4. 本地/演示环境启动

```bash
cd deploy
docker compose --env-file ../.env up -d
```

```bash
cd backend
mvn -DskipTests spring-boot:run -pl boms-web
```

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5174`。

## 5. Docker 镜像构建

```bash
cd backend
docker build -t boms-backend:latest .
```

```bash
cd frontend
docker build -t boms-frontend:latest .
```

生产建议使用固定镜像 tag（如 Git commit SHA），避免 `latest` 无法追溯。

## 6. 生产部署注意事项

- 前端 nginx 默认把 `/api/` 和 `/open-api/` 代理到 `http://boms-backend:8081`，容器服务名需要与部署编排保持一致。
- 后端默认端口为 `8081`，可通过 `SERVER_PORT` 调整。
- 附件上传依赖 MinIO bucket，后端启动时会尝试自动创建 `BOMS_MINIO_BUCKET`；生产环境也可预先创建 bucket 并设置生命周期/备份策略。
- 批量导入最大文件大小由 `BOMS_IMPORT_MAX_FILE_SIZE_BYTES` 控制，需与网关上传限制保持一致。
- 算力平台对接默认关闭；如启用，需确保 `BOMS_CPN_GATEWAY` 可从后端容器访问，且 `BOMS_CPN_APP_SECRET` 由密钥管理系统注入而非写入仓库。
- 生产环境请关闭演示明文密码登录：`BOMS_DEMO_ALLOW_PLAIN_PASSWORD=false`。
- 建议在网关层启用 HTTPS、请求体大小限制、访问日志、限流和 WAF 基础规则。

## 7. 部署后健康检查

```bash
curl -i http://127.0.0.1:8081/api/health
```

```bash
curl -I http://127.0.0.1/
```

登录后重点验证：客户列表、商机列表、商机详情、批量导入模板下载、附件签名上传、公海配置、平台租户列表。

## 8. 常见问题

### Maven 下载依赖失败

检查服务器是否能访问 Maven Central，或配置公司 Nexus/Artifactory 镜像仓库。

### 后端启动时 Flyway 报错

确认 MySQL 版本为 8.0、字符集为 `utf8mb4`，并检查数据库是否已经存在未被 Flyway 管理的同名表。

### 前端接口 404 或 502

确认 nginx `proxy_pass` 后端服务名和端口正确，后端 `/api/health` 可访问。

### 附件上传失败

确认 `BOMS_MINIO_ENDPOINT` 可从后端容器访问，bucket 存在，access key/secret key 有读写权限。

---

## 9. 上线前检查清单

> 正式上线前逐项核对；本清单原为独立的 `PRE_LAUNCH_CHECKLIST.md`，现并入本文统一维护。

### 9.1 代码检查

- [ ] 当前分支已完成 Code Review。
- [ ] 无未处理的高风险 TODO/FIXME、临时代码、`debugger`、调试输出。
- [ ] 后端接口均使用统一响应和统一异常处理。
- [ ] 新增/修改接口具备必要参数校验。
- [ ] 前端无硬编码生产接口地址，生产 API 由 nginx/网关代理。
- [ ] Git 工作区干净，构建产物、真实 `.env`、日志、数据库备份未提交。

### 9.2 测试验证

- [ ] `cd backend && mvn test` 通过。
- [ ] `cd backend && mvn package -DskipTests` 通过。
- [ ] `cd frontend && npm ci` 通过。
- [ ] `cd frontend && npm run typecheck` 通过。
- [ ] `cd frontend && npm run build` 通过。
- [ ] 登录、客户、商机、跟进、任务、公海、附件、导入、平台管理已完成冒烟。

### 9.3 数据库检查

- [ ] 生产数据库已备份并验证可恢复。
- [ ] MySQL 版本为 8.0，字符集/排序规则符合迁移脚本要求。
- [ ] Flyway 迁移在预发布环境验证通过。
- [ ] 数据库账号具备必要 DDL/DML 权限，且不是 root 账号。
- [ ] 关键表容量、索引、慢查询已评估。

### 9.4 环境变量检查

- [ ] 已基于 `.env.example` 配置生产变量。
- [ ] `BOMS_JWT_SECRET` 已替换为强随机密钥。
- [ ] `BOMS_DEMO_ALLOW_PLAIN_PASSWORD=false`。
- [ ] 数据库、MinIO、算力平台对接密钥均来自密钥管理系统或部署平台变量。
- [ ] `BOMS_IMPORT_MAX_FILE_SIZE_BYTES` 与网关上传大小限制一致。
- [ ] 如启用算力对接：`BOMS_CPN_ENABLED=true` 且 `BOMS_CPN_GATEWAY`/`APP_KEY`/`APP_SECRET` 已正确配置。

### 9.5 安全检查

- [ ] HTTPS 已启用，HTTP 已重定向或受控。
- [ ] JWT 密钥、数据库密码、MinIO 密钥、算力平台 secret 未入库。
- [ ] RBAC 权限码和 ABAC 数据范围已按角色抽查。
- [ ] 跨租户访问客户/商机/附件/导入任务返回 403 或无数据。
- [ ] 文件上传仅允许业务白名单类型，且对象存储 bucket 权限最小化。
- [ ] `/open-api/` 签名、时间戳、防重放策略已在网关或服务侧验证。

### 9.6 性能风险

- [ ] 批量导入大小、行数和并发已压测或限流。
- [ ] MySQL 连接池、慢查询阈值、磁盘空间监控已配置。
- [ ] MinIO 容量、生命周期、备份策略已确认。
- [ ] 前端静态资源开启缓存，HTML 不做强缓存。
- [ ] 定时任务执行窗口与业务高峰不冲突。

### 9.7 回滚方案

- [ ] 已记录当前生产镜像 tag、数据库版本和配置版本。
- [ ] 新版本镜像 tag 可追溯到 Git commit。
- [ ] 回滚命令和负责人已明确。
- [ ] 数据库迁移涉及不可逆 DDL 时已准备回滚脚本或恢复方案。
- [ ] 对象存储新增数据不影响旧版本读取。

### 9.8 上线后验证项

- [ ] `/api/health` 返回成功。
- [ ] 前端首页可访问，刷新二级路由不 404。
- [ ] 关键账号可登录，权限菜单正确。
- [ ] 客户/商机列表查询正常，新增/编辑/删除链路正常。
- [ ] 附件上传、下载、删除正常。
- [ ] 批量导入模板下载、上传、进度轮询正常。
- [ ] 后端日志无持续异常，数据库/CPU/内存/磁盘指标正常。
