<div align="center">

<img src="img/screenshots/hero.jpg" alt="MintHive — AI 驱动的兴趣社交平台" width="100%" />

<br/>

<a href="https://github.com/JohnvenTom/Minthive/actions/workflows/ci.yml">
  <img src="https://github.com/JohnvenTom/Minthive/actions/workflows/ci.yml/badge.svg" alt="CI"/>
</a>
<img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/Vue-3.4-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3"/>
<img src="https://img.shields.io/badge/TypeScript-5.4-3178C6?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript"/>
<img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis"/>
<img src="https://img.shields.io/badge/MinIO-8.5-C72C48?style=flat-square&logo=minio&logoColor=white" alt="MinIO"/>
<img src="https://img.shields.io/badge/Capacitor-8-119EFF?style=flat-square&logo=capacitor&logoColor=white" alt="Capacitor"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License MIT"/>

<br/><br/>

**MintHive** = **Mint**（新鲜志趣）+ **Hive**（蜂巢聚合）

以兴趣聚合人群，以内容产生互动。

<sub>An AI-powered interest-based social platform · Circle community · Realtime messaging · AI assistant · Admin console</sub>

<br/>

[界面预览](#界面预览) ·
[功能特性](#功能特性) ·
[快速开始](#快速开始) ·
[系统架构](#系统架构) ·
[API 概览](#api-概览) ·
[部署](#部署)

</div>

---

## 这是什么

MintHive 是一个**可以直接跑起来**的兴趣社交平台，包含三个可独立部署的应用：

| 应用 | 技术栈 | 说明 |
|---|---|---|
| 用户端 | Vue 3 + Vite + Vant / Element Plus | Web + H5 + Capacitor Android |
| 管理后台 | Vue 3 + Element Plus + ECharts | 运营看板、内容审核、工单处理 |
| 服务端 | Spring Boot 3.3 + MyBatis-Plus | 143 个 REST 接口 + WebSocket + AI 网关 |

它不是一个只有 CRUD 的脚手架：圈子权限、内容审核、私信已读回执、消息撤回、操作审计、AI 双模式切换这些"真正麻烦的部分"都已经实现，并且有测试与 CI 兜底。

<details>
<summary><b>四个值得关注的设计点</b></summary>

<br/>

1. **AI 是可替换的基础设施**：`AiService` 接口 + 策略模式，公有云（OpenAI 兼容协议）与本地私有化（Ollama）两种实现零代码切换，失败自动降级为兜底话术，不阻塞业务主流程。
2. **内容安全是双层的**：DFA 敏感词命中后不直接拒绝，而是交给 AI 语义复审，识别谐音、变体与暗语，减少误杀；图片走本地检测，不外传。
3. **身份与会话是分离的**：JWT 只做凭证，Redis 维护该用户当前有效的 Token 集合——登出、封禁、改密会立刻让旧连接失效，WebSocket 也会被主动断开。
4. **所有写操作可追溯**：操作审计链路记录操作人、路径、结果码、耗时与数据变更，管理端可按 traceId 检索。

</details>

---

## 界面预览

> 以下均为项目**真实运行截图**（本地启动、真实数据），非设计稿。

<img src="img/screenshots/home.jpg" alt="首页信息流" width="100%" />

<div align="center"><sub>首页信息流 · 推荐 / 最新 / 最热三模式 · 瀑布流卡片 · 圈子公告与推荐轮播</sub></div>

<br/>

<table>
<tr>
<td width="50%">
<img src="img/screenshots/circle-plaza.jpg" alt="圈子广场"/>
<div align="center"><sub>圈子广场 · 分类筛选 · 推荐圈子 · 公开/私密标识</sub></div>
</td>
<td width="50%">
<img src="img/screenshots/circle-detail.jpg" alt="圈子详情"/>
<div align="center"><sub>圈子详情 · 圈主信息 · 公告 · 圈内动态</sub></div>
</td>
</tr>
<tr>
<td>
<img src="img/screenshots/post-detail.jpg" alt="帖子详情"/>
<div align="center"><sub>帖子详情 · 话题标签 · 点赞/评论/收藏 · 二级回复</sub></div>
</td>
<td>
<img src="img/screenshots/chat.jpg" alt="私信会话"/>
<div align="center"><sub>私信 · 实时收发 · 已读回执 · AI 代回复入口</sub></div>
</td>
</tr>
<tr>
<td>
<img src="img/screenshots/ai-draft.jpg" alt="AI 文案生成"/>
<div align="center"><sub>AI 发帖助手 · 关键词生成三种风格文案与话题</sub></div>
</td>
<td>
<img src="img/screenshots/ai-assistant.jpg" alt="AI 问答助手"/>
<div align="center"><sub>全局 AI 问答 · 流式输出 · 可跳转站内页面</sub></div>
</td>
</tr>
</table>

<br/>

<table>
<tr>
<td width="33%" align="center">
<img src="img/screenshots/messages.jpg" alt="消息中心"/>
<div align="center"><sub>消息中心 · 私信 / 通知 / 公告</sub></div>
</td>
<td width="33%" align="center">
<img src="img/screenshots/search.jpg" alt="搜索"/>
<div align="center"><sub>全站搜索 · 用户 / 帖子 / 圈子</sub></div>
</td>
<td width="33%" align="center">
<img src="img/screenshots/profile.jpg" alt="个人主页"/>
<div align="center"><sub>个人主页 · 动态 / 收藏 / 关注</sub></div>
</td>
</tr>
</table>

### 管理后台

<img src="img/screenshots/admin-dashboard.jpg" alt="数据大屏" width="100%" />

<div align="center"><sub>社区健康度监控中心 · 核心指标 · DAU/MAU 趋势 · AI 日报 · Excel 导出</sub></div>

<br/>

<table>
<tr>
<td width="50%">
<img src="img/screenshots/admin-user.jpg" alt="用户管理"/>
<div align="center"><sub>用户管理 · 封禁 / 解封 / 重置密码</sub></div>
</td>
<td width="50%">
<img src="img/screenshots/admin-content.jpg" alt="内容审核"/>
<div align="center"><sub>内容审核 · 待审 / 已发布 / 敏感词库</sub></div>
</td>
</tr>
<tr>
<td>
<img src="img/screenshots/admin-circle.jpg" alt="圈子管理"/>
<div align="center"><sub>圈子管理 · 圈主转让 · 推荐位 · 下架</sub></div>
</td>
<td>
<img src="img/screenshots/admin-report.jpg" alt="举报工单"/>
<div align="center"><sub>举报工单 · AI 风险分级 · 驳回 / 处罚</sub></div>
</td>
</tr>
</table>

<img src="img/screenshots/admin-audit.jpg" alt="操作审计" width="100%" />

<div align="center"><sub>操作审计 · 主体 / 路径 / 结果码 / 耗时 / traceId 全链路检索</sub></div>

### 跨端

<table>
<tr>
<td width="25%" align="center"><img src="img/screenshots/mobile-home.jpg" alt="移动端首页"/><br/><sub>移动端首页</sub></td>
<td width="25%" align="center"><img src="img/screenshots/mobile-circle.jpg" alt="移动端圈子"/><br/><sub>圈子广场</sub></td>
<td width="25%" align="center"><img src="img/screenshots/mobile-post.jpg" alt="移动端帖子"/><br/><sub>帖子详情</sub></td>
<td width="25%" align="center"><img src="img/screenshots/mobile-profile.jpg" alt="移动端个人页"/><br/><sub>个人主页</sub></td>
</tr>
</table>

---

## 功能特性

<table>
<tr>
<td width="50%" valign="top">

**圈子与社群**

- 圈子广场分类浏览、关键词搜索、推荐位轮播
- 公开圈一键加入 / 私密圈申请审核
- 圈内公告、置顶动态、成员管理
- 圈主能力：编辑资料、移出成员、转让圈主、解散圈子
- 圈子敏感字段（公告 / 帖子数）按访问权限过滤

</td>
<td width="50%" valign="top">

**内容与互动**

- 图文 / 视频动态、话题标签、可见范围
- 点赞、收藏、评论、二级回复、转发
- 收藏夹、关注 / 粉丝、个人主页
- 全站搜索：用户、帖子、圈子
- 举报工单：AI 风险分级 + 人工处置

</td>
</tr>
<tr>
<td valign="top">

**实时通信**

- WebSocket 长连接，12 类消息推送
- 评论、点赞、收藏、关注通知实时到达
- 一对一私信：已读回执、消息撤回
- 多设备同时在线，会话互不干扰
- 登出 / 封禁 / 改密后连接立即失效

</td>
<td valign="top">

**AI 全链路辅助**

- 发帖助手：关键词生成三种风格文案 + 话题抽取
- 文案润色、发布前内容预检
- 贴合上下文的评论生成
- 离线私信 AI 代回复（可撤回）
- 全局问答客服：SSE 流式输出，可跳转站内页面
- AI 兴趣推荐：用户行为日志驱动

</td>
</tr>
<tr>
<td valign="top">

**内容风控**

- DFA 敏感词库 + AI 语义复审双通道
- 识别谐音、变体与暗语
- 图片 AI 违规识别（本地检测，不外传）
- 审核结果与驳回原因推送作者
- 举报工单自动分级，高风险优先处理

</td>
<td valign="top">

**运营后台**

- 数据大屏：核心指标、趋势、圈子排行
- AI 日报：一键生成运营建议
- 用户 / 内容 / 圈子 / 工单 / 审计全模块
- AI 功能全局或单项开关
- Excel 导出（多 Sheet）

</td>
</tr>
</table>

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | 17+ | 后端运行环境 |
| Node.js | 20+ | 前端构建（CI 使用 Node 24） |
| MySQL | 8.0 | 业务数据库，Flyway 自动建表 |
| Redis | 7+ | 会话、缓存、限流、在线状态 |
| MinIO | 最新版 | 图片 / 视频对象存储 |
| Docker | 可选 | 一键拉起 MySQL / Redis / MinIO |

### 五步启动

```bash
# 1. 拉起基础依赖（也可使用本机已装的 MySQL / Redis / MinIO）
cd deploy
cp .env.example .env        # 按需修改密码与 JWT_SECRET
docker compose up -d

# 2. 配置后端环境变量
cd ../backend
cp .env.example .env        # 填数据库密码、MinIO 密钥、AI Key
```

```bash
# 3. 启动后端（Flyway 会自动建表并写入版本基线）
mvn spring-boot:run
# 服务地址     http://localhost:8080
# 健康检查     http://localhost:8080/actuator/health
# 接口文档     http://localhost:8080/doc.html
```

```bash
# 4. 启动用户端
cd frontend-user
npm install && npm run dev     # http://localhost:5173

# 5. 启动管理后台
cd ../frontend-admin
npm install && npm run dev     # http://localhost:5174
```

> Windows 用户也可以直接运行仓库根目录的 `start-dev.ps1`：脚本会检查依赖、按需打包 jar、启动后端与前端并轮询健康检查。
> 为避免 Windows 保留端口，脚本默认使用 **后端 8080 / 用户端 4400 / 管理端 4401**（加 `-WithAdmin` 同时启动管理端）。

### 首次使用

1. 打开 `http://localhost:5173`，在登录页切换到**注册**，用手机号注册（开发环境验证码打印在后端日志 `sms.log-code=true`）。
2. 管理员账号需要把数据库中的 `user.role` 改为 `2`，再访问 `http://localhost:5174` 登录。
3. 图片上传依赖 MinIO，请确认 `deploy/.env` 中的 `MINIO_PUBLIC_URL` 与后端 `MINIO_ENDPOINT` 指向同一地址。

<details>
<summary><b>环境变量清单</b></summary>

<br/>

**后端 `backend/.env`**

| 变量 | 必填 | 说明 |
|---|---|---|
| `MYSQL_DEV_PASSWORD` | ✅ | MySQL 密码 |
| `REDIS_DEV_PASSWORD` | — | Redis 密码，无密码留空 |
| `MINIO_DEV_ACCESS_KEY` / `MINIO_DEV_SECRET_KEY` | ✅ | MinIO 访问密钥 |
| `MINIO_ENDPOINT` / `MINIO_PUBLIC_URL` | ✅ | 服务端访问地址 / 浏览器访问地址 |
| `JWT_DEV_SECRET` | ✅ | 签名密钥，生产至少 32 字节 |
| `AI_CLOUD_DEV_BASE_URL` / `AI_CLOUD_DEV_API_KEY` / `AI_CLOUD_DEV_MODEL` | cloud 模式必填 | OpenAI 兼容服务 |
| `AI_LOCAL_BASE_URL` / `AI_LOCAL_MODEL` | local 模式必填 | Ollama 地址与模型 |

**部署 `deploy/.env`**

| 变量 | 说明 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | 容器内 MySQL root 密码 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` / `MINIO_PUBLIC_URL` / `MINIO_BUCKET` | 对象存储配置 |
| `JWT_SECRET` | 至少 32 字节随机串，缺失或过短会导致启动失败 |
| `AI_MODE` | `cloud` 或 `local` |
| `SMS_PROVIDER` / `SMS_HTTP_URL` / `SMS_HTTP_TOKEN` | 生产环境短信通道，未配置将无法启动 |

</details>

---

## 系统架构

```mermaid
flowchart TB
    subgraph CLIENT["客户端"]
        USER["用户端 · Vue 3 + Vite<br/>Web / H5 / Android"]
        ADMIN["管理后台 · Vue 3 + Element Plus<br/>PC"]
    end

    subgraph GATEWAY["接入层"]
        NGINX["Nginx<br/>静态资源 / 反向代理 / HTTPS"]
    end

    subgraph BACKEND["服务端 · Spring Boot 3.3"]
        API["REST API<br/>15 个用户端控制器<br/>7 个管理端控制器<br/>143 个接口"]
        WS["WebSocket<br/>12 类消息推送"]
        AI["AI 网关<br/>策略模式 · 双模式切换<br/>缓存 / 限流 / 降级"]
        SEC["安全层<br/>JWT + Redis 会话<br/>RBAC + 操作审计"]
    end

    subgraph STORAGE["数据层"]
        MYSQL[("MySQL 8.0<br/>17 张业务表")]
        REDIS[("Redis 7<br/>会话 / 缓存 / 在线状态 / 限流")]
        MINIO[("MinIO<br/>图片 / 视频")]
    end

    subgraph AI_MODE["AI 双模式"]
        CLOUD["公有云<br/>OpenAI 兼容协议"]
        LOCAL["本地私有化<br/>Ollama + Qwen3"]
    end

    USER --> NGINX
    ADMIN --> NGINX
    NGINX --> API
    NGINX --> WS
    API --> SEC
    API --> AI
    API --> MYSQL
    API --> REDIS
    API --> MINIO
    AI --> CLOUD
    AI --> LOCAL
    WS -. 实时推送 .-> USER

    style CLIENT fill:#0d1a18,stroke:#4ECDC4,color:#e6edf3
    style GATEWAY fill:#0d1a18,stroke:#7A86B8,color:#e6edf3
    style BACKEND fill:#0d1a18,stroke:#6DB33F,color:#e6edf3
    style STORAGE fill:#0d1a18,stroke:#4479A1,color:#e6edf3
    style AI_MODE fill:#0d1a18,stroke:#FFB627,color:#e6edf3
```

### 技术栈

| 层次 | 技术 |
|---|---|
| 服务端 | Java 17 · Spring Boot 3.3.5 · MyBatis-Plus 3.5.5 · Flyway 10 |
| 数据 | MySQL 8.0 · Redis 7（Jedis）· MinIO 8.5 |
| 安全 | Spring Security Crypto（BCrypt）· JJWT 0.12 · 自定义拦截器 |
| AI | Spring AI 1.0.0-M1 · OpenAI 兼容 HTTP 客户端 · Ollama |
| 接口文档 | Knife4j 4.5（生产环境自动关闭） |
| 用户端 | Vue 3.4 · Vite 5 · TypeScript 5.4 · Vant 4.8 · Pinia 2 · Swiper 12 |
| 管理端 | Vue 3.4 · Vite 5 · Element Plus 2.7 · ECharts 5.5 · xlsx |
| 跨端 | Capacitor 8（Android） |
| 测试 | JUnit 5 + Mockito · Vitest 2 · Testcontainers（集成测试） |
| CI | GitHub Actions：后端单测 / 集成测试 / 双端前端检查 |

<details>
<summary><b>项目结构</b></summary>

```text
Minthive/
├── backend/                      # Spring Boot 服务端
│   ├── src/main/java/com/minthive/
│   │   ├── ai/                   # AI 网关：策略、提示词、意图解析、工具调用
│   │   ├── audit/                # 操作审计：切面、拦截器、查询服务
│   │   ├── common/               # 统一响应、错误码、常量
│   │   ├── config/               # 跨域、WebMvc、Redis、MinIO、异步线程池
│   │   ├── controller/           # 用户端控制器（15）
│   │   │   └── admin/            # 管理端控制器（7）
│   │   ├── entity/ mapper/ service/
│   │   ├── media/                # 媒体定位、票据签发、访问控制
│   │   ├── security/             # JWT 拦截器、登录态、用户上下文
│   │   └── websocket/            # 连接管理、会话映射、消息类型
│   ├── src/main/resources/
│   │   ├── db/migration/         # Flyway 版本化迁移
│   │   └── mapper/               # MyBatis XML
│   └── src/test/                 # 单测 + 集成测试
├── frontend-user/                # 用户端（Web / H5 / Android）
│   └── src/
│       ├── api/ components/ stores/ styles/ utils/
│       └── views/                # 18 个页面
├── frontend-admin/               # 管理后台
│   └── src/views/                # 8 个页面
├── deploy/                       # docker-compose、Nginx、Dockerfile
├── img/screenshots/              # README 截图资源
├── docs/                         # 设计文档、需求说明
└── start-dev.ps1                 # Windows 一键启动脚本
```

</details>

---

## AI 设计

### 双模式

切换只需改一个配置项，业务代码零改动：

```yaml
ai:
  mode: cloud          # cloud | local
  cloud:
    base-url: ${AI_CLOUD_BASE_URL:https://api.openai.com/v1}   # 填到版本段，客户端追加 /chat/completions
    api-key: ${AI_CLOUD_API_KEY:}
    model: ${AI_CLOUD_MODEL:gpt-4o-mini}
  local:
    base-url: ${AI_LOCAL_BASE_URL:http://localhost:11434}
    model: ${AI_LOCAL_MODEL:qwen3:7b}
```

| 模式 | 适用场景 | 特点 |
|---|---|---|
| `cloud` | 线上部署 | 走 OpenAI 兼容协议，可对接任意兼容服务 |
| `local` | 数据不出内网 | 对接 Ollama，本地模型推理 |

### 九个 AI 能力

| 端点 | 能力 |
|---|---|
| `POST /api/ai/post-content` | 关键词生成三种风格文案 + 话题抽取（结构化返回） |
| `POST /api/ai/polish` | 文案润色与纠错 |
| `POST /api/ai/comment` | 贴合帖子内容的评论生成 |
| `POST /api/ai/message-reply` | 私信 AI 代回复 |
| `POST /api/ai/detect` | 发布前内容预检 |
| `POST /api/ai/qa` / `POST /api/ai/qa/stream` | 智能问答（同步 / 流式） |
| `POST /api/ai/query/stream` | 数据感知问答（管理员问答式取数） |
| `GET /api/ai/remaining` | 查询今日剩余调用次数 |
| `GET /api/ai/recommend` | 兴趣推荐 |

### 三个工程约束

- **限流**：每用户每日 50 次 AI 调用，Redis 计数，超限直接返回剩余次数提示。
- **缓存**：相同提示词命中缓存（默认 1 小时），降低重复消耗。
- **降级**：超时或异常时返回兜底话术，不抛错、不阻塞业务。

---

## 实时通信

基于原生 WebSocket（`/ws/{token}`），Redis 维护在线状态与会话映射。

| 类型 | 说明 |
|---|---|
| `comment` / `reply` | 评论、回复通知 |
| `like` / `collect` / `follow` | 点赞、收藏、关注提醒 |
| `chat` | 私信消息 |
| `system` / `circle_notice` | 系统公告、圈子公告 |
| `read_receipt` | 已读回执 |
| `revoke` | 消息撤回 |
| `online` / `offline` | 上下线状态 |

设计要点：

- 同一用户可多端同时在线，会话独立映射，互不覆盖。
- 登出、封禁、改密会撤销 Redis 中的 Token，并**主动关闭**对应连接。
- 前端带心跳与指数退避重连，断线期间的未读消息由消息中心兜底。

---

## 安全与风控

| 层面 | 措施 |
|---|---|
| 认证 | JWT 凭证 + Redis 有效 Token 集合双校验，登出即失效 |
| 授权 | 用户 / 圈主 / 管理员三级角色，接口级拦截器校验 |
| 密码 | BCrypt 自适应加盐，无 MD5 遗留 |
| 内容 | DFA 敏感词 + AI 语义复审，图片本地检测 |
| 防刷 | 发帖 50 / 评论 200 / 私信 100（每日每用户），Redis 原子计数 |
| 上传 | 扩展名白名单 + 服务端强制 Content-Type，杜绝存储型 XSS |
| 媒体 | 短时票据访问，私有桶校验，越权不可读 |
| 审计 | 写操作全量记录，`@Async` 任务显式抑制避免重复 |
| 配置 | 生产禁用接口文档与明文 SQL 日志；JWT 密钥缺失即启动失败 |

---

## API 概览

- 接口文档（开发环境）：`http://localhost:8080/doc.html`
- 统一响应结构：`{ code, msg, data, traceId }`，`code = 200` 表示成功
- 鉴权方式：请求头 `Authorization: Bearer <token>`

<details>
<summary><b>用户端接口分组（15 个控制器）</b></summary>

<br/>

| 分组 | 路径前缀 | 说明 |
|---|---|---|
| 认证 | `/api/auth` | 注册、密码/验证码登录、重置密码、登出 |
| 用户 | `/api/user` | 资料、头像、关注 / 粉丝、兴趣标签 |
| 帖子 | `/api/post` | 发布、详情、信息流、点赞、收藏、搜索 |
| 评论 | `/api/comment` | 评论、回复、删除 |
| 圈子 | `/api/circle` | 广场、详情、创建、加入、圈内动态 |
| 圈主管理 | `/api/circle-admin` | 成员管理、公告、转让、解散 |
| 消息 | `/api/message` | 私信、会话列表、已读、撤回 |
| 通知 | `/api/notification` | 通知列表、未读、已读 |
| 文件 | `/api/file` | 上传、媒体内容读取 |
| 举报 | `/api/report` | 提交举报、处理结果 |
| AI | `/api/ai` | 9 个 AI 能力 |
| 搜索 | `/api/search` | 用户 / 帖子 / 圈子聚合搜索 |
| 公告 | `/api/announcement` | 平台公告 |
| 轮播 | `/api/banner` | 首页轮播 |
| 反馈 | `/api/feedback` | 意见反馈 |

</details>

<details>
<summary><b>管理端接口分组（7 个控制器）</b></summary>

<br/>

| 分组 | 路径前缀 | 说明 |
|---|---|---|
| 数据 | `/api/admin/stats` | 核心指标、趋势、排行、AI 日报、导出 |
| 用户 | `/api/admin/user` | 列表、封禁、解封、重置密码、僵尸清理 |
| 内容 | `/api/admin/content` | 待审、已发布、审批、驳回、敏感词 |
| 圈子 | `/api/admin/circle` | 列表、推荐位、转让、下架 |
| 工单 | `/api/admin/report` | 工单列表、处置、处罚 |
| 审计 | `/api/admin/audit` | 操作日志检索与明细 |
| 配置 | `/api/admin/config` | 公告、轮播、管理员、AI 开关 |

</details>

---

## 数据库设计

17 张业务表，使用 Flyway 版本化迁移管理结构变更（`backend/src/main/resources/db/migration`）。

| 领域 | 表 |
|---|---|
| 用户 | `user`、`follow` |
| 内容 | `post`、`comment`、`like_collect` |
| 圈子 | `circle`、`circle_category`、`circle_user` |
| 消息 | `message`、`system_msg`、`notification_read` |
| 运营 | `banner`、`announcement`、`report` |
| AI | `ai_user_log` |
| 审计 | `audit_log`、`audit_log_change` |

关键约定：

- 所有业务表使用 `deleted` 软删除，列表查询统一过滤。
- 消息历史使用 `beforeId` 游标分页，避免深分页性能问题。
- 审计日志分主表 + 变更明细表，避免大字段影响检索。

---

## 测试与 CI

```bash
# 后端单元测试
cd backend && mvn test

# 后端集成测试（Testcontainers 拉起 MySQL / Redis）
cd backend && mvn verify

# 前端类型检查 + 单元测试
cd frontend-user && npm run typecheck && npm test
cd frontend-admin && npm run typecheck && npm test
```

| 项目 | 规模 |
|---|---|
| 后端单元测试 | 21 个测试类 / 108 个用例 |
| 前端单元测试 | 用户端 19 个 + 管理端 9 个 |
| 集成测试 | 媒体访问、审核、会话可见性、消息通知、可观测性 |
| CI 检查项 | 后端单测、后端集成测试、用户端、管理端（4 项均为必需检查） |

---

## 部署

<details>
<summary><b>Docker Compose 部署</b></summary>

<br/>

```bash
cd deploy
cp .env.example .env        # 填写数据库、MinIO、JWT、AI、短信配置
docker compose up -d
```

`deploy/` 目录包含：

- `docker-compose.yml`：MySQL、Redis、MinIO 与应用容器编排
- `web.Dockerfile`：前端构建镜像
- `nginx/user.conf`、`nginx/admin.conf`：用户端与管理端站点配置（含 SPA history 路由回退与接口代理）

生产注意事项：

- `JWT_SECRET` 至少 32 字节，缺失或过短会直接启动失败。
- `SMS_PROVIDER` 未配置时服务拒绝启动，避免验证码静默失效。
- 接口文档相关路径在生产环境返回 404。
- Android release 构建禁止明文 HTTP，仅 debug 允许本地调试地址。

</details>

<details>
<summary><b>Android 打包</b></summary>

<br/>

```bash
cd frontend-user
npm run build
npx cap sync android
npx cap open android      # 交给 Android Studio 构建签名包
```

Capacitor 使用 hash 路由，避免原生环境下刷新 404；已适配刘海屏安全区域与触控手势。

</details>

---

## 常见问题

<details>
<summary><b>注册时收不到验证码？</b></summary>

<br/>

开发环境默认 `sms.log-code=true`，验证码直接打印在后端日志中，搜索 `验证码` 即可。
生产环境必须配置 `SMS_PROVIDER` 与网关地址，否则服务不会启动。

</details>

<details>
<summary><b>AI 功能报错或提示"休息中"？</b></summary>

<br/>

按顺序检查：

1. `ai.mode` 与对应配置是否匹配（`cloud` 需要 API Key，`local` 需要 Ollama 已启动并拉取模型）。
2. `base-url` 只需填到版本段（如 `.../v1`），客户端会自行追加 `/chat/completions`。
3. 是否触达每日 50 次限额，可通过 `/api/ai/remaining` 查询。

AI 异常不会影响发帖、评论等主流程，只会返回兜底文案。

</details>

<details>
<summary><b>上传的图片显示 404？</b></summary>

<br/>

图片走 MinIO + 短时票据访问，需要确认：

- `MINIO_ENDPOINT` 与 `MINIO_PUBLIC_URL` 指向同一实例，且浏览器可直接访问后者。
- 存储桶 `minthive` 已创建，且为私有读。

</details>

<details>
<summary><b>前端启动端口被占用？</b></summary>

<br/>

Windows 的 5000–6000 段存在系统保留端口，可能出现"端口未占用却监听失败"。直接使用根目录脚本 `start-dev.ps1`，默认走 4400 / 4401 规避该问题。

</details>

---

## 贡献

1. Fork 本仓库并创建特性分支：`git checkout -b feature/your-feature`
2. 提交前请确保通过 `mvn test`、`npm run type-check` 与 `npm run test:unit`
3. 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

| 前缀 | 用途 |
|---|---|
| `feat:` | 新功能 |
| `fix:` | 缺陷修复 |
| `refactor:` | 重构 |
| `docs:` | 文档 |
| `test:` | 测试 |
| `chore:` | 构建 / 依赖 |

4. 提交 Pull Request，说明变更动机与验证方式。

---

## License

本项目基于 **MIT License** 开源，可自由用于学习、二次开发与商业项目，请保留版权声明。

## 致谢

感谢 Spring Boot、Vue.js、MyBatis-Plus、Vant、Element Plus、ECharts、MinIO、Redis 等开源项目。

<div align="center">
<br/>
<sub>如果这个项目对你有帮助，欢迎点一个 Star ⭐</sub>
</div>
