# BOMS 测试说明

## 1. 测试策略

当前仓库已有后端 JUnit 5 测试，前端暂未引入单元测试框架。交付前建议采用以下分层验证：

1. 后端单元测试：统一响应、业务异常、JWT、导入文件校验等纯逻辑。
2. 后端集成/冒烟：启动 Spring Boot + MySQL/MinIO 后访问健康检查和核心接口。
3. 前端静态验证：`vue-tsc` 类型检查、Vite 生产构建。
4. 手工业务冒烟：登录、客户、商机、跟进、任务、公海、附件、导入、平台管理。

## 2. 后端测试命令

```bash
cd backend
mvn test
```

若只验证 Web 启动模块测试：

```bash
cd backend
mvn test -pl boms-web -am
```

## 3. 前端检查命令

```bash
cd frontend
npm ci
npm run typecheck
npm run build
```

## 4. 构建验证

```bash
cd backend
mvn package -DskipTests
```

```bash
cd frontend
npm run build
```

## 5. 冒烟测试清单

| 场景 | 验证点 |
|------|--------|
| 登录 | 演示账号可登录；错误密码返回统一错误；生产禁用明文密码登录 |
| 权限 | 不同角色菜单和按钮可见性符合权限码；越权接口返回 403 |
| 客户 | 列表、新建、编辑、联系人维护、重复客户提示 |
| 商机 | 新建、阶段推进/回退、赢单/输单、转移、协作人 |
| 跟进/任务 | 新增跟进、任务指派、状态流转、数据范围过滤 |
| 批量导入 | 模板下载带 Authorization；仅允许 xls/xlsx；超大文件被拒绝；进度可轮询 |
| 附件 | 签名上传、确认、下载、删除；跨租户附件不可访问 |
| 平台对接 | appKey/appSecret 签名校验、开通、SSO、心跳 |

## 6. 覆盖范围说明

本次补充的测试覆盖批量导入入口文件校验：

- 支持 `.xlsx` / `.xls` 且扩展名大小写不敏感。
- 拒绝非 Excel 文件。
- 拒绝空文件和超过配置上限的文件。

前端目前没有 Vitest/Playwright 等测试框架。若后续进入持续迭代，建议先引入 Vitest 覆盖 `services` 和权限守卫，再引入 Playwright 覆盖登录与核心业务冒烟。
