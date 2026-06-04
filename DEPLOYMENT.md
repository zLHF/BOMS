# BOMS 部署说明

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

1. 复制根目录 `.env.example` 为 `.env`，或在 CI/CD/容器平台中配置等价变量。
2. 生产环境必须修改：
   - `SPRING_DATASOURCE_PASSWORD`
   - `BOMS_JWT_SECRET`（至少 32 字节随机字符串）
   - `BOMS_DEMO_ALLOW_PLAIN_PASSWORD=false`
   - `BOMS_MINIO_ACCESS_KEY` / `BOMS_MINIO_SECRET_KEY`
   - `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` / `MINIO_ROOT_PASSWORD`
3. 不要把真实 `.env`、数据库备份、对象存储密钥提交到代码仓库。

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
