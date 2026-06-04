# BOMS 前端

商机管理系统前端，基于 Vue 3 + TypeScript + Element Plus + Pinia + Vue Router。

**当前状态：V1.0 全部完成**。功能开发 + 算力平台 SSO + 批量导入 + 构建优化均已落地。

## 技术栈

- Vue 3 + TypeScript + Vite 6
- Element Plus（主色 `#2563eb`）+ @element-plus/icons-vue
- Pinia（auth store）+ Vue Router（路由表 + 登录/权限守卫）
- axios（请求拦截注入 token、响应解包 `R<T>`、401 跳登录）

## 运行（需后端先起）

```bash
# 1) 基础设施 + 后端
cd ../deploy && docker compose up -d
cd ../backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -DskipTests spring-boot:run -pl boms-web

# 2) 前端（/api 经 vite 代理到 8081）
cd ../frontend
npm install
npm run dev          # http://localhost:5174
npm run typecheck    # vue-tsc 类型检查
npm run build        # 生产构建
```

## 页面清单

| 路由 | 页面 | 模块 | 状态 |
|------|------|------|------|
| `/login` | 登录页 | M01 | ✅ |
| `/dashboard` | 工作台 | — | ✅ |
| `/opportunity` | 商机管理（多视图列表+批量导入） | M05/M06/M07 | ✅ |
| `/opportunity/:id` | 商机详情（9 Tab） | M08/M09/M16 | ✅ |
| `/customer` | 客户管理（含公海切换） | M10/D5 | ✅ |
| `/task` | 任务中心 | M12 | ✅ |
| `/follow` | 跟进记录（全局时间线/列表） | M07 | ✅ |
| `/settings/org` | 组织用户 | M03 | ✅ |
| `/settings/role` | 角色权限 | M04 | ✅ |
| `/settings/stage` | 阶段/编号配置 | M19 | ✅ |
| `/settings/audit` | 操作日志 | M16 | ✅ |
| `/settings/pool` | 公海配置（回收天数等） | D5 | ✅ |
| `/settings/dict` | 数据字典配置 | — | ✅ |
| `/platform/tenant` | 租户管理 | M02 | ✅ |

## 商机详情 9 个标签页（M08）

| Tab | 内容 | 数据来源 |
|-----|------|---------|
| 概况 | 阶段条 + 信息网格 + 推进/赢/输操作 | `oppApi.detail()` |
| 联系人 | 客户联系人表格 | `oppApi.contacts()` |
| 跟进 | 时间线 + 新增跟进表单 | `oppApi.follows()` |
| 任务 | 关联任务列表 + 新建 | `taskApi.list()` |
| 协作 | 协作人列表 + 添加/移除 | `oppApi.collaborators()` |
| 附件 | MinIO 签名上传/下载/删除 | `fileApi.*` |
| 操作日志 | 商机相关审计记录 | `auditApi.list()` |
| 报价 | 占位页 | — |
| 订单 | 占位页 | — |

## 目录结构

```
src/
├── main.ts              # 入口：Element Plus + Pinia + Router + v-perm
├── App.vue              # 根组件（仅 RouterView）
├── router/index.ts      # 路由表（perm/group meta + 守卫）
├── stores/auth.ts       # 认证状态（token/用户/权限码）
├── services/
│   ├── request.ts       # axios 封装（token注入、R<T>解包、401跳登录）
│   ├── auth.ts          # 认证 API（login/me/change-password/logout）
│   ├── business.ts      # 业务 API（customer/opp/task/config/audit/pool/follow）
│   ├── system.ts        # 系统 API（tenant/user/dept/role/dict）
│   └── file.ts          # 文件 API（sign-upload/confirm/sign-download/delete/list）
├── directives/perm.ts   # v-perm 权限指令（无权限移除DOM）
├── components/
│   ├── AttachmentPanel.vue   # 附件上传/下载面板（MinIO签名）
│   └── ImportDialog.vue      # 批量导入对话框（Excel上传+进度轮询）
├── layouts/DefaultLayout.vue  # 侧边栏+顶栏布局
├── styles/
│   ├── tokens.css       # 设计token（主色#2563eb）
│   └── base.css         # 全局样式
├── types/index.ts       # TypeScript 接口定义
└── views/
    ├── LoginView.vue
    ├── DashboardView.vue
    ├── OpportunityView.vue       # 商机列表（含公海视图+回收/认领+批量导入）
    ├── OpportunityDetailView.vue # 商机详情9Tab
    ├── CustomerView.vue          # 客户列表（含公海切换）
    ├── TaskView.vue              # 任务中心
    ├── FollowView.vue            # 跟进记录（全局时间线/列表+筛选）
    ├── PlaceholderView.vue       # 占位页
    ├── settings/
    │   ├── OrgUserView.vue       # 组织用户
    │   ├── RoleView.vue          # 角色权限
    │   ├── StageConfigView.vue   # 阶段/编号配置
    │   ├── AuditView.vue         # 操作日志
    │   ├── PoolConfigView.vue    # 公海配置
    │   └── DictConfigView.vue    # 数据字典配置
    └── platform/
        └── TenantView.vue        # 租户管理
```

## 演示账号

| 账号 | 租户码 | 角色 | 可见菜单 |
|------|--------|------|---------|
| platform_admin | （留空） | 平台超管 | 工作台 + 平台/租户管理 |
| zhangwei | hd-sales | 销售主管 | 业务全部 + 设置全部 |
| lina | hd-sales | 销售 | 业务（无下属视图） |
| auditor | hd-sales | 审计员 | 全部（只读） |

口令统一 `123456`。

## 构建部署

- `npm run build` 输出到 `dist/`
- `Dockerfile` + `nginx.conf`：多阶段构建，nginx 托管静态文件 + API 代理
- CI 流水线自动构建并推送 Docker 镜像

## 已完成进度

| Sprint | 范围 | 状态 |
|--------|------|------|
| 阶段 0 | 工程骨架 + 路由/状态/API/权限基座 | ✅ |
| Sprint 1 | 登录 + 工作台 + 组织用户 + 角色权限 + 租户管理 | ✅ |
| Sprint 2-3 | 客户管理 + 商机管理（列表/新建/阶段/跟进） | ✅ |
| Sprint 4 | 任务中心 + 商机详情7Tab + 协作 + 公海 + 审计日志 + 配置页 | ✅ |
| Sprint 5 | 附件中心(MinIO) + 自动回收 + 跟进独立页 + 字典配置 + 报价订单占位(9Tab) | ✅ |
| Sprint 6 | 算力平台 SSO + 批量导入对话框 | ✅ |
| Sprint 7 | Dockerfile + nginx.conf 构建 | ✅ |

**待办**：无。V1.0 全部完成。
