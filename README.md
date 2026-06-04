# BOMS - 商机管理系统

多租户、多用户、多组织的 B2B 销售协同平台。涵盖商机全生命周期管理（线索 → 跟进 → 赢单/输单）、客户管理、任务中心、公海池、附件中心、批量导入、算力平台对接等完整功能。

**当前版本：V1.0（已全部完成）**

## 技术栈

| 层 | 技术 |
|----|------|
| **后端** | Spring Boot 3.3 · Java 21 · MyBatis-Plus 3.5.9 · Flyway · MySQL 8 |
| **前端** | Vue 3 · TypeScript · Vite 6 · Element Plus · Pinia · Vue Router |
| **存储** | MySQL 8（业务数据）· MinIO（附件）· Redis（缓存） |
| **部署** | Docker · Docker Compose · GitHub Actions CI |
| **安全** | JWT + bcrypt · 多租户行级隔离 · RBAC + ABAC 数据范围 · HMAC-SHA256 签名 |

## 目录结构

```
BOMS/
├── backend/                    # 后端（三层 Maven 多模块）
│   ├── boms-common/            #   通用基础层（响应/异常/租户/鉴权/审计AOP/数据范围）
│   ├── boms-service/           #   业务服务层（10 个业务模块）
│   ├── boms-web/               #   Web 启动层（main/配置/Flyway/测试）
│   ├── Dockerfile              #   多阶段构建
│   └── README.md
├── frontend/                   # 前端（Vue 3 + TS + Element Plus）
│   ├── src/
│   │   ├── views/              #   14 个页面（登录/工作台/客户/商机/任务/跟进/设置/平台）
│   │   ├── components/         #   AttachmentPanel · ImportDialog
│   │   ├── services/           #   API 封装（auth · business · system · file）
│   │   ├── stores/             #   Pinia store
│   │   ├── router/             #   路由表 + 权限守卫
│   │   └── directives/         #   v-perm 权限指令
│   ├── Dockerfile              #   多阶段构建（node → nginx）
│   ├── nginx.conf              #   SPA 路由 + API 代理
│   └── README.md
├── deploy/
│   └── docker-compose.yml      # 基础设施（MySQL 3307 · Redis 6380 · MinIO 9100/9101）
├── docs/                       # 设计文档 01–10
├── .github/workflows/ci.yml   # GitHub Actions CI 流水线
├── CLAUDE.md                   # Claude Code 开发指引
└── README.md                   # 本文件
```

## 快速开始（本地开发）

### 1. 准备环境变量并启动基础设施

```bash
cp .env.example .env   # 本地开发可使用默认示例；生产必须替换所有 change-me 值
cd deploy
docker compose --env-file ../.env up -d
```

将启动三个服务：

| 服务 | 端口 | 说明 |
|------|------|------|
| `boms-mysql` | **3307** | MySQL 8.0（库 `boms`，账密 `boms`/`boms_pw`） |
| `boms-redis` | **6380** | Redis 7（持久化） |
| `boms-minio` | **9100**（API）/ **9101**（控制台） | MinIO 对象存储（账密 `boms`/`boms_minio_pw`） |

### 2. 启动后端

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS，必须 JDK 21

mvn compile                                        # 编译全部子模块
mvn -DskipTests spring-boot:run -pl boms-web       # 启动（首次启动 Flyway 自动建表+种子数据）
```

- 服务地址：`http://127.0.0.1:8081`
- Flyway 自动执行 `V1__schema.sql`（21 张表）+ `V2__seed.sql`（种子数据）+ `V3__sprint5.sql` + `V4__sprint6.sql`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev          # http://localhost:5174（/api 自动代理到 8081）
```

### 4. 访问系统

打开 `http://localhost:5174`，使用下方演示账号登录。

## Docker 部署（生产/演示环境）

### 方式一：Docker Compose 一键部署

在项目根目录创建 `docker-compose.prod.yml`：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-boms_root_pw}
      MYSQL_DATABASE: boms
      MYSQL_USER: boms
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-boms_pw}
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      - --default-time-zone=+08:00
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1"]
      interval: 5s
      timeout: 5s
      retries: 20

  redis:
    image: redis:7-alpine
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis_data:/data

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_USER:-boms}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD:-boms_minio_pw}
    volumes:
      - minio_data:/data

  backend:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/boms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
      SPRING_DATASOURCE_USERNAME: boms
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD:-boms_pw}
      BOMS_JWT_SECRET: ${JWT_SECRET:-change-me-in-production}
      BOMS_DEMO_ALLOW_PLAIN_PASSWORD: "false"
    depends_on:
      mysql:
        condition: service_healthy
    ports:
      - "8081:8081"

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
  redis_data:
  minio_data:
```

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

启动后访问 `http://localhost`（前端 nginx 80 端口，自动代理 `/api/` 到后端）。

### 方式二：单独构建镜像

```bash
# 构建后端镜像
cd backend
docker build -t boms-backend .

# 构建前端镜像
cd ../frontend
docker build -t boms-frontend .
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | MySQL 连接串 | `jdbc:mysql://127.0.0.1:3307/boms?...` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户 | `boms` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `boms_pw` |
| `BOMS_JWT_SECRET` | JWT 签名密钥 | 演示密钥（**生产必须更换**） |
| `BOMS_JWT_EXPIRE_MINUTES` | Token 有效期（分钟） | `720` |
| `BOMS_DEMO_ALLOW_PLAIN_PASSWORD` | 是否允许明文密码登录 | `true`（**生产必须设为 false**） |
| `BOMS_MINIO_ENDPOINT` | MinIO/S3 API 地址 | `http://127.0.0.1:9100` |
| `BOMS_MINIO_ACCESS_KEY` | 后端访问 MinIO 的 Access Key | `boms` |
| `BOMS_MINIO_SECRET_KEY` | 后端访问 MinIO 的 Secret Key | `boms_minio_pw` |
| `BOMS_MINIO_BUCKET` | 附件 bucket | `boms-files` |
| `BOMS_IMPORT_MAX_FILE_SIZE_BYTES` | 批量导入文件大小上限 | `10485760` |
| `LOGGING_LEVEL_COM_BOMS` | 后端业务日志级别 | `INFO` |
| `MINIO_ROOT_USER` | MinIO 管理员用户 | `boms` |
| `MINIO_ROOT_PASSWORD` | MinIO 管理员密码 | `boms_minio_pw` |

完整变量清单见 `.env.example`。

> **⚠️ 安全提醒**：生产部署时务必修改 JWT 密钥、数据库密码、MinIO 密码，并关闭 `BOMS_DEMO_ALLOW_PLAIN_PASSWORD`。演示账号依赖明文口令放行，仅允许在本地或演示环境使用。

### MinIO 初始化

首次部署后需在 MinIO 控制台（`http://localhost:9101`）创建 bucket `boms`（或通过后端附件上传自动创建，取决于 MinIO 配置）。

## 演示账号

口令统一 `123456`（种子 `password_hash` 为占位，由 `boms.demo.allow-plain-password=true` 放行明文）。

| 账号 | 租户码 | 角色 | 可见范围 |
|------|--------|------|---------|
| `platform_admin` | （留空） | 平台超管 | 工作台 + 平台/租户管理 |
| `tadmin` | `hd-sales` | 租户管理员 | 全部（含设置） |
| `zhangwei` | `hd-sales` | 销售主管（销售一部） | 业务全部 + 设置全部 |
| `lina` | `hd-sales` | 销售（销售一部） | 业务（无下属视图） |
| `wangqiang` | `hd-sales` | 销售（销售二部） | 业务（无下属视图） |
| `auditor` | `hd-sales` | 审计员 | 全部（只读） |

## 功能概览

### 核心业务

- **商机管理**：多视图列表（全部/我的/下属/赢单/公海）→ 新建 → 阶段推进/回退 → 赢单/输单 → 转移
- **商机详情**：9 个标签页（概况 · 联系人 · 跟进 · 任务 · 协作 · 附件 · 操作日志 · 报价 · 订单）
- **客户管理**：客户 CRUD + 联系人管理 + 同租户唯一性校验
- **跟进记录**：商机内跟进 + 全局时间线/列表页（支持筛选）
- **任务中心**：任务 CRUD + 指派 + 状态流转 + 数据范围过滤
- **公海池**：商机/客户手动回收与认领 + 30 天无跟进自动回收 + 公海参数配置

### 系统管理

- **多租户隔离**：创建租户自动引导角色矩阵 + 管理员 + 阶段配置
- **组织架构**：部门树 + 用户管理（含启停/重置密码/角色分配）
- **角色权限**：RBAC 权限码 + ABAC 数据范围（SELF/DEPT/DEPT_AND_SUB/TENANT/PLATFORM）
- **阶段/编号配置**：自定义销售阶段 + 商机编号规则
- **数据字典**：字典类型 + 字典项 CRUD
- **审计日志**：全操作审计 + 多条件查询
- **附件中心**：MinIO 签名上传/下载/删除

### 扩展功能

- **批量导入**：Excel 导入商机（EasyExcel 解析 + 异步任务 + 进度轮询）
- **算力平台对接**：用户开通（appKey/appSecret 鉴权）· SSO 登录 · 5 分钟心跳上报（HMAC-SHA256 签名）

### CI/CD

- **GitHub Actions**：编译 → 测试 → 前端 typecheck → 构建 → Docker 镜像
- **Dockerfile**：后端多阶段构建（Maven → JDK 21 JRE）、前端多阶段构建（Node → Nginx）
- **上线前检查**：发布前按 `PRE_LAUNCH_CHECKLIST.md` 完成代码、测试、数据库、环境变量、安全、性能和回滚确认。

## 数据库

- 21+ 张表，Flyway 自动迁移
- 公共字段：`tenant_id / created_by / created_at / updated_by / updated_at / deleted`
- 逻辑删除（`@TableLogic`）、乐观锁（`@Version`）、金额 `DECIMAL(15,2)`

## 交付与运维文档

| 文档 | 说明 |
|------|------|
| `.env.example` | 环境变量样例，不包含真实密钥 |
| `DEPLOYMENT.md` | 部署步骤、服务器要求、数据库初始化、常见问题 |
| `TESTING.md` | 测试方法、测试命令、覆盖范围和冒烟建议 |
| `PRE_LAUNCH_CHECKLIST.md` | 上线前检查清单 |
| `CHANGELOG.md` | 本次交付前检查与补强记录 |

## 设计文档

| 文档 | 说明 |
|------|------|
| `docs/01` | PRD 需求规格 |
| `docs/02` | 架构设计 |
| `docs/03` | API 接口设计 |
| `docs/04` | 权限码定义 |
| `docs/05` | 技术决策记录（ADR） |
| `docs/06` | 前端页面设计 |
| `docs/07` | 接口详细设计 |
| `docs/08` | 测试用例 |
| `docs/09` | DDL（已纳入 Flyway V1） |
| `docs/10` | 初始化 SQL（已纳入 Flyway V2） |

## 开发进度

| 阶段 | 范围 | 状态 |
|------|------|------|
| 阶段 0 | 前后端工程骨架 + 建库 + 租户/RBAC/审计基座 | ✅ |
| Sprint 1 | M01 认证 / M02 租户 / M03 组织用户 / M04 角色权限 | ✅ |
| Sprint 2-3 | M10 客户 / M05-M07 商机（列表/新增/阶段/跟进） | ✅ |
| Sprint 4 | M08 详情 Tab / M09 协作 / M12 任务 / M16 审计 / M19 配置 / D5 公海 | ✅ |
| Sprint 5 | M16 附件中心(MinIO) / D5 自动回收 / 跟进独立页 / 数据字典 / 报价订单占位 | ✅ |
| Sprint 6 | 算力平台对接(开通/SSO/心跳) / M06 批量导入 | ✅ |
| Sprint 7 | 后端多模块拆分 / GitHub Actions CI / Dockerfile / 单元测试 | ✅ |

## License

Private — 内部项目，未经授权不得分发。
