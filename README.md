<div align="center">

<svg xmlns="http://www.w3.org/2000/svg" width="260" height="72" viewBox="0 0 260 72">
  <defs>
    <linearGradient id="hiveGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#4ECDC4"/>
      <stop offset="100%" stop-color="#2CB5AC"/>
    </linearGradient>
    <linearGradient id="amberDot" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFB627"/>
      <stop offset="100%" stop-color="#F5A623"/>
    </linearGradient>
    <filter id="glow">
      <feGaussianBlur stdDeviation="2" result="blur"/>
      <feMerge>
        <feMergeNode in="blur"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>
  <polygon points="36,4 64,20 64,52 36,68 8,52 8,20"
           fill="url(#hiveGrad)" stroke="#3BB8B0" stroke-width="1.5" filter="url(#glow)"/>
  <polygon points="36,16 50,26 50,46 36,56 22,46 22,26"
           fill="#0D1117" stroke="none"/>
  <circle cx="36" cy="36" r="5" fill="url(#amberDot)" filter="url(#glow)"/>
  <polygon points="12,10 18,7 18,13 12,16 6,13 6,7"
           fill="#4ECDC4" opacity="0.3"/>
  <polygon points="60,58 66,55 66,61 60,64 54,61 54,55"
           fill="#4ECDC4" opacity="0.3"/>
  <text x="80" y="44" font-family="'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
        font-size="32" font-weight="700" fill="#4ECDC4" letter-spacing="1.5">
    Mint<tspan fill="#FFB627">Hive</tspan>
  </text>
  <text x="82" y="60" font-family="'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
        font-size="10" fill="#7A86B8" letter-spacing="3">
    INTEREST SOCIAL PLATFORM
  </text>
</svg>

<br/>

<a href="https://spring.io/projects/spring-boot" target="_blank">
  <img src="https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot"/>
</a>
<a href="https://vuejs.org/" target="_blank">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?style=flat-square&logo=vuedotjs&logoColor=white" alt="Vue 3"/>
</a>
<a href="https://www.typescriptlang.org/" target="_blank">
  <img src="https://img.shields.io/badge/TypeScript-5.4-3178C6?style=flat-square&logo=typescript&logoColor=white" alt="TypeScript"/>
</a>
<a href="https://www.mysql.com/" target="_blank">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL"/>
</a>
<a href="https://redis.io/" target="_blank">
  <img src="https://img.shields.io/badge/Redis-6.0+-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis"/>
</a>
<a href="https://min.io/" target="_blank">
  <img src="https://img.shields.io/badge/MinIO-8.5-C72C48?style=flat-square&logo=minio&logoColor=white" alt="MinIO"/>
</a>
<a href="https://spring.io/projects/spring-ai" target="_blank">
  <img src="https://img.shields.io/badge/Spring_AI-1.0_M1-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring AI"/>
</a>
<a href="https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API" target="_blank">
  <img src="https://img.shields.io/badge/WebSocket-Realtime-010101?style=flat-square&logo=websocket&logoColor=white" alt="WebSocket"/>
</a>
<a href="https://openjdk.org/" target="_blank">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17"/>
</a>
<a href="https://capacitorjs.com/" target="_blank">
  <img src="https://img.shields.io/badge/Capacitor-8-119EFF?style=flat-square&logo=capacitor&logoColor=white" alt="Capacitor"/>
</a>
<a href="https://github.com/your-org/Minthive/blob/main/LICENSE" target="_blank">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License: MIT"/>
</a>
<a href="https://github.com/your-org/Minthive/pulls" target="_blank">
  <img src="https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square" alt="PRs Welcome"/>
</a>

</div>

---

**MintHive** 是一个融合 AI 智能能力的垂直兴趣社交平台，名字取自 **Mint**（新鲜志趣）+ **Hive**（蜂巢聚合），寓意同兴趣爱好者相聚成团。平台主打「以兴趣聚合人群，以内容产生互动」，支持兴趣圈子社群、实时评论互动、AI 全链路辅助（发帖/评论/私信代聊/内容风控/千人千面推荐）、双重内容审核等核心能力，打造轻量化、高粘性、垂直化的兴趣交流社区。

---

## ▸ 核心功能

<table>
<tr>
<td width="50" align="center"><strong>◆</strong></td>
<td><img src="https://img.shields.io/badge/AI-智能辅助-4ECDC4?style=flat-square"/></td>
<td>
<b>AI 发帖助手</b> — 关键词一键生成 3 风格文案（简约/氛围感/干货）、文案润色纠错、敏感内容预检测<br/>
<b>AI 智能互动</b> — 贴合帖子内容生成自然评论、离线私信 AI 代回复（可撤回）<br/>
<b>AI 问答客服</b> — 全局悬浮入口，SSE 流式输出，7x24 小时自动应答
</td>
</tr>
<tr>
<td align="center"><strong>◇</strong></td>
<td><img src="https://img.shields.io/badge/CIRCLE-兴趣圈子-4ECDC4?style=flat-square"/></td>
<td>
圈子广场分类浏览与搜索、推荐圈子轮播 Banner、公开圈一键加入/私密圈审核制、圈子详情（公告/置顶/圈内动态）、圈主管理（成员管理/公告编辑/转让圈主/解散圈子）、创建/编辑圈子、AI 圈子智能推荐
</td>
</tr>
<tr>
<td align="center"><strong>▸</strong></td>
<td><img src="https://img.shields.io/badge/REALTIME-实时互动-FFB627?style=flat-square"/></td>
<td>
基于 WebSocket 长连接：评论/回复实时推送、点赞收藏即时同步、一对一私信实时收发与已读回执、系统/圈子公告全员推送
</td>
</tr>
<tr>
<td align="center"><strong>●</strong></td>
<td><img src="https://img.shields.io/badge/RECOMMEND-千人千面-FFB627?style=flat-square"/></td>
<td>
AI 兴趣向量动态更新、个性化信息流推荐（智能/最新/最热三模式切换）、AI 圈子与好友智能匹配、用户行为日志驱动推荐迭代
</td>
</tr>
<tr>
<td align="center"><strong>■</strong></td>
<td><img src="https://img.shields.io/badge/SAFETY-内容风控-FF5C6C?style=flat-square"/></td>
<td>
敏感词库 + AI 语义双重审核（识别谐音/变体/暗语）、AI 图片违规识别（本地检测不外传）、举报工单 AI 风险等级自动分拣、内容驳回原因推送
</td>
</tr>
<tr>
<td align="center"><strong>▹</strong></td>
<td><img src="https://img.shields.io/badge/ADMIN-管理后台-7A86B8?style=flat-square"/></td>
<td>
用户管理（封禁/解封/密码重置/僵尸清理）、内容审核（待审/已发布/敏感词库）、圈子管理、举报工单处理、系统配置、数据大屏（ECharts 可视化 + AI 日报 + Excel 导出）、AI 功能全局/单项开关
</td>
</tr>
<tr>
<td align="center"><strong>⬡</strong></td>
<td><img src="https://img.shields.io/badge/MOBILE-跨端部署-119EFF?style=flat-square"/></td>
<td>
Web + H5 响应式适配、Capacitor 原生 Android 打包、移动端安全区域（刘海屏）适配、触控手势优化
</td>
</tr>
</table>

---

## ▸ 系统架构

```mermaid
flowchart TB
    subgraph CLIENT["客户端"]
        USER["用户端<br/>Vue 3 + Vant 4 + Element Plus<br/>Web / H5 / Android"]
        ADMIN["管理后台<br/>Vue 3 + Element Plus + ECharts<br/>PC 端"]
    end

    subgraph GATEWAY["接入层"]
        NGINX["Nginx<br/>反向代理 / HTTPS"]
    end

    subgraph BACKEND["后端服务 · Spring Boot 3.3"]
        API["REST API<br/>/api/** · 14 个用户端控制器<br/>6 个管理端控制器"]
        WS["WebSocket<br/>/ws/{token} · 11 种消息类型"]
        AI["AI 模块<br/>策略模式 · 双模式切换<br/>限流 + 缓存 + 降级"]
        SECURITY["JWT 认证<br/>拦截器 + Redis Token"]
    end

    subgraph STORAGE["数据层"]
        MYSQL[("MySQL 8.0<br/>13 张核心表")]
        REDIS[("Redis<br/>缓存 / 向量存储<br/>在线状态 / 限流")]
        MINIO[("MinIO<br/>本地对象存储<br/>图片 / 视频")]
    end

    subgraph AI_MODE["AI 双模式"]
        CLOUD["公有云模式<br/>OpenAI 兼容 API"]
        LOCAL["本地私有化模式<br/>Ollama + Qwen3-7B"]
    end

    USER --> NGINX
    ADMIN --> NGINX
    NGINX --> API
    NGINX --> WS
    API --> SECURITY
    API --> AI
    API --> MYSQL
    API --> REDIS
    API --> MINIO
    AI --> CLOUD
    AI --> LOCAL
    WS -.->|实时推送| USER

    style CLIENT fill:#1a1e2e,stroke:#4ECDC4,color:#e6edf3
    style GATEWAY fill:#1a1e2e,stroke:#7A86B8,color:#e6edf3
    style BACKEND fill:#1a1e2e,stroke:#6DB33F,color:#e6edf3
    style STORAGE fill:#1a1e2e,stroke:#4479A1,color:#e6edf3
    style AI_MODE fill:#1a1e2e,stroke:#FFB627,color:#e6edf3
```

---

## ▸ 技术栈

### ◆ 后端

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.3.5 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM 增强（SpringBoot3 Starter） |
| MySQL | 8.0 | 主数据库 |
| Redis / Jedis | 6.0+ / 5.1.0 | 缓存 / 向量存储 / 在线状态 / 限流 |
| MinIO | 8.5.9 | 本地对象存储（图片/视频） |
| Spring AI | 1.0.0-M1 | AI 集成框架 |
| JJWT | 0.12.5 | JWT Token 管理 |
| Knife4j | 4.5.0 | Swagger3 / OpenAPI3 增强文档 |
| Apache POI | 5.2.5 | Excel 导出 |
| Hutool | 5.8.27 | 工具库 |
| dotenv-java | 3.0.1 | 环境变量加载 |
| Lombok | 1.18.30 | 样板代码简化 |
| Ollama + Qwen3-7B | 最新版（可选） | 本地大模型服务 |

### ◆ 用户端前端（Web + H5 + Android）

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.21 | UI 框架 |
| TypeScript | 5.4.3 | 类型安全 |
| Vite | 5.2.6 | 构建工具 |
| Vant | 4.8.7 | 移动端 H5 组件库 |
| Element Plus | 2.6.3 | 桌面端组件库 |
| Pinia | 2.1.7 | 状态管理（持久化） |
| Vue Router | 4.3.0 | 路由（18 条路由 + 认证守卫） |
| Axios | 1.6.8 | HTTP 客户端 |
| Swiper | 12.2.0 | 轮播组件 |
| vue-cropper | 1.1.4 | 图片裁剪 |
| dayjs | 1.11.10 | 日期格式化 |
| NProgress | 0.2.0 | 路由进度条 |
| Capacitor | 8.4.1 | 原生 Android 打包 |
| SCSS / Sass | 1.72.0 | CSS 预处理 |

### ◇ 管理端前端（PC）

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.31 | UI 框架 |
| TypeScript | 5.5.3 | 类型安全 |
| Vite | 5.3.3 | 构建工具 |
| Element Plus | 2.7.6 | 桌面端组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.4.0 | 路由 |
| Axios | 1.7.2 | HTTP 客户端 |
| ECharts | 5.5.0 | 数据可视化（自定义 minthive 主题） |
| xlsx + file-saver | 0.18.5 / 2.0.5 | Excel 导出 |
| SCSS / Sass | 1.77.6 | CSS 预处理（暮光玫瑰暗色主题） |

---

## ▸ 快速上手

### ◆ 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| Node.js | 18+ | 前端运行环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存 / 向量存储 / 在线状态 |
| MinIO | 最新稳定版 | 本地对象存储 |
| Ollama | 最新版（可选） | 本地大模型服务 |

### ◆ 五步启动

**01** 克隆仓库

```bash
git clone https://github.com/your-org/Minthive.git
cd Minthive
```

**02** 启动中间件

```bash
# MySQL — 创建数据库并初始化
mysql -uroot -p < backend/docs/sql/init.sql

# Redis
redis-server --port 6379

# MinIO
minio server /data/minio --console-address ":9001"
# 访问 http://localhost:9001 创建存储桶 minthive
```

**03** 配置后端

```bash
cd backend
cp .env.example .env
# 编辑 .env 填入 MySQL / Redis / MinIO / JWT / AI 实际配置
```

**04** 启动后端

```bash
cd backend
mvn spring-boot:run
# 服务启动于 http://localhost:8080
# 接口文档 http://localhost:8080/doc.html
```

**05** 启动前端

```bash
# 用户端
cd frontend-user
npm install
npm run dev
# 访问 http://localhost:5173

# 管理后台
cd frontend-admin
npm install
npm run dev
# 访问 http://localhost:5174
```

> 详细部署说明请参阅 [部署手册](backend/docs/部署手册.md)

---

## ▸ 项目结构

```
Minthive/
├── backend/                              # Spring Boot 3.3 后端
│   ├── src/main/java/com/minthive/
│   │   ├── MinthiveApplication.java      # 启动入口
│   │   ├── ai/                           # AI 双模式服务（7 个类）
│   │   │   ├── AiService.java            #   AI 接口（6 项能力）
│   │   │   ├── AiContext.java            #   策略选择器（cloud/local 路由）
│   │   │   ├── CloudAiServiceImpl.java   #   云端 API 实现
│   │   │   ├── LocalAiServiceImpl.java   #   本地 Ollama 实现
│   │   │   ├── AiRateLimiter.java        #   Redis 令牌桶限流（50次/日/用户）
│   │   │   ├── AiCacheManager.java       #   Redis 响应缓存
│   │   │   └── AiFallback.java           #   降级处理器
│   │   ├── common/                       # 通用模块（7 个类）
│   │   │   ├── Result.java               #   统一响应包装
│   │   │   ├── ResultCode.java           #   错误码枚举
│   │   │   ├── PageResult.java           #   分页结果
│   │   │   ├── Constants.java            #   全局常量
│   │   │   ├── RedisConstants.java       #   Redis Key 常量
│   │   │   ├── BusinessException.java    #   业务异常
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── config/                       # 配置类（10 个）
│   │   │   ├── AiConfig.java             #   AI 模式配置
│   │   │   ├── CorsConfig.java           #   跨域配置
│   │   │   ├── EnvLoader.java            #   .env 环境变量加载
│   │   │   ├── JwtConfig.java            #   JWT 设置
│   │   │   ├── Knife4jConfig.java        #   API 文档配置
│   │   │   ├── MinioConfig.java          #   MinIO 客户端
│   │   │   ├── MybatisPlusConfig.java    #   MyBatis-Plus 分页插件
│   │   │   ├── RedisConfig.java          #   Redis 序列化
│   │   │   ├── WebMvcConfig.java         #   拦截器注册
│   │   │   └── WebSocketConfig.java      #   WebSocket 端点
│   │   ├── controller/                   # 用户端 REST 控制器（14 个）
│   │   │   ├── AuthController.java       #   登录 / 注册 / 短信验证码 / 重置密码
│   │   │   ├── UserController.java       #   个人信息 / 修改资料 / 兴趣标签
│   │   │   ├── PostController.java       #   发布 / 信息流 / 点赞 / 收藏 / 转发
│   │   │   ├── CommentController.java    #   评论 / 回复 / 删除 / 点赞
│   │   │   ├── CircleController.java     #   圈子列表 / 详情 / 加入 / 离开 / 创建 / 推荐
│   │   │   ├── CircleAdminController.java #  圈主管理 / 成员 / 公告 / 转让
│   │   │   ├── MessageController.java    #   私信 / 聊天记录 / 已读 / AI 代回复
│   │   │   ├── FollowController.java     #   关注 / 取关 / AI 推荐好友
│   │   │   ├── SearchController.java     #   全局搜索 / 热词
│   │   │   ├── AiController.java         #   文案生成 / 润色 / 评论 / 问答 / SSE 流式
│   │   │   ├── FileController.java       #   文件上传（图片/视频 → MinIO）
│   │   │   ├── ReportController.java     #   提交举报
│   │   │   ├── ConfigController.java     #   公共配置 / Banner
│   │   │   ├── AnnouncementController.java # 系统公告
│   │   │   └── admin/                    # 管理端 REST 控制器（6 个）
│   │   │       ├── AdminUserController.java    # 用户管理 / 封禁 / 僵尸清理
│   │   │       ├── AdminStatsController.java   # 数据统计 / 趋势 / AI 日报
│   │   │       ├── AdminPostController.java    # 内容审核 / 驳回 / 敏感词
│   │   │       ├── AdminReportController.java  # 举报工单 / 处罚
│   │   │       ├── AdminCircleController.java  # 圈子管理 / 推荐 / 下架
│   │   │       └── AdminConfigController.java  # 系统配置 / AI 开关
│   │   ├── entity/                       # MyBatis-Plus 数据实体（13 张表映射）
│   │   │   ├── User.java                 #   user
│   │   │   ├── Post.java                 #   post
│   │   │   ├── Comment.java              #   comment
│   │   │   ├── Circle.java               #   circle
│   │   │   ├── CircleCategory.java       #   circle_category
│   │   │   ├── CircleUser.java           #   circle_user
│   │   │   ├── Follow.java               #   follow
│   │   │   ├── LikeCollect.java          #   like_collect
│   │   │   ├── Message.java              #   message
│   │   │   ├── Report.java               #   report
│   │   │   ├── SystemMsg.java            #   system_msg
│   │   │   ├── Announcement.java         #   announcement
│   │   │   └── AiUserLog.java            #   ai_user_log
│   │   ├── mapper/                       # Mapper 接口（19 个）+ XML（7 个自定义 SQL）
│   │   ├── security/                     # JWT 认证（4 个类）
│   │   │   ├── JwtInterceptor.java       #   请求拦截器
│   │   │   ├── JwtUtils.java             #   JWT 工具
│   │   │   ├── LoginUser.java            #   登录用户注解
│   │   │   └── UserContext.java          #   ThreadLocal 上下文
│   │   ├── service/                      # 业务服务接口（14 个）
│   │   ├── service/impl/                 # 业务服务实现（14 个）
│   │   ├── util/                         # 工具类（6 个）
│   │   │   ├── MinioUtil.java            #   MinIO 文件操作
│   │   │   ├── RedisUtil.java            #   Redis 缓存操作
│   │   │   ├── JwtUtil.java              #   JWT 令牌工具
│   │   │   ├── SensitiveWordUtil.java    #   DFA 敏感词算法
│   │   │   ├── BcryptUtil.java           #   BCrypt 密码加密
│   │   │   └── FileCompressUtil.java     #   图片压缩
│   │   └── websocket/                    # WebSocket 服务（3 个类）
│   │       ├── WebSocketServer.java      #   WS 端点 + 在线管理
│   │       ├── WsMessage.java            #   消息体
│   │       └── MessageType.java          #   11 种消息类型枚举
│   ├── src/main/resources/
│   │   ├── application.yml               # 基础配置
│   │   ├── application-dev.yml           # 开发环境配置
│   │   ├── application-prod.yml          # 生产环境配置
│   │   ├── sensitive-words.txt           # 敏感词库
│   │   └── mapper/                       # MyBatis XML（7 个管理端自定义 SQL）
│   ├── docs/
│   │   ├── sql/init.sql                  # 数据库初始化脚本
│   │   ├── sql/init_data.sql             # 测试数据
│   │   ├── 部署手册.md                    # 完整部署文档
│   │   └── 接口文档.md                    # 接口详细文档
│   ├── .env / .env.example               # 环境变量
│   └── pom.xml
│
├── frontend-user/                        # Vue 3 用户端（Web + H5 + Android）
│   └── src/
│       ├── api/                          # API 模块（14 个）
│       │   ├── request.ts                #   Axios 封装（拦截器 / 401 / 分页适配）
│       │   ├── auth.ts                   #   登录 / 注册 / 重置密码 / 登出
│       │   ├── user.ts                   #   个人信息 / 头像 / 兴趣标签 / 注销
│       │   ├── post.ts                   #   信息流 / CRUD / 点赞 / 收藏 / 转发 / 草稿
│       │   ├── comment.ts                #   评论 / 回复 / 删除 / 点赞
│       │   ├── circle.ts                 #   圈子 / 加入 / 离开 / 创建 / 分类 / 成员 / 公告
│       │   ├── message.ts                #   聊天 / 历史 / 发送 / 撤回 / 已读 / 通知
│       │   ├── follow.ts                 #   关注 / 取关 / AI 推荐好友
│       │   ├── search.ts                 #   全局搜索（用户/帖子/圈子/热词）
│       │   ├── file.ts                   #   图片/视频上传
│       │   ├── report.ts                 #   提交举报（4 种目标类型）
│       │   ├── config.ts                 #   公共配置 / Banner
│       │   ├── announcement.ts           #   系统公告
│       │   └── ai.ts                     #   文案生成 / 润色 / 评论 / 代回复 / 检测 / SSE 流式 / 推荐
│       ├── views/                        # 页面组件（18 个路由页面）
│       │   ├── home/                     #   首页信息流（瀑布流 / 智能推荐/最新/最热）
│       │   ├── post/                     #   发帖（AI 内联工具栏）+ 帖子详情
│       │   ├── circle/                   #   圈子广场 + 圈子详情 + 成员管理 + 创建/编辑
│       │   ├── message/                  #   消息中心 + 私信聊天（AI 代回复）
│       │   ├── profile/                  #   个人主页 + 编辑资料
│       │   ├── login/                    #   登录 + 重置密码
│       │   ├── interest/                 #   兴趣标签选择
│       │   ├── search/                   #   全局搜索
│       │   ├── settings/                 #   设置（AI 开关 / 主题 / 通知）
│       │   └── error/                    #   404
│       ├── stores/                       # Pinia 状态管理（3 个）
│       │   ├── user.ts                   #   Token / 用户信息 / 兴趣 / 登出
│       │   ├── chat.ts                   #   聊天列表 / 未读 / WebSocket
│       │   └── app.ts                    #   主题 / 移动端 / AI 启用 / AI 对话历史
│       ├── components/                   # 通用组件（18 个）
│       │   ├── AiAssistant.vue           #   全局 AI 问答悬浮窗（SSE 流式打字机效果）
│       │   ├── PostCard.vue              #   帖子卡片（AI 生成标记）
│       │   ├── CommentItem.vue           #   评论项（楼中楼 + AI 标记）
│       │   ├── CircleCard.vue            #   圈子卡片
│       │   ├── UserCard.vue              #   用户卡片
│       │   ├── NavBar.vue                #   导航栏
│       │   ├── TabBar.vue                #   底部标签栏
│       │   ├── BannerSwiper.vue          #   Banner 轮播
│       │   ├── ImageUploader.vue         #   图片上传器
│       │   ├── EditPostDialog.vue        #   编辑帖子弹窗
│       │   ├── ShareSheet.vue            #   分享面板
│       │   ├── ShareChainDialog.vue      #   分享链接弹窗
│       │   ├── ReportDialog.vue          #   举报弹窗
│       │   ├── LoadingSpinner.vue        #   加载动画
│       │   ├── EmptyState.vue            #   空状态
│       │   ├── AnimatedSelect.vue        #   动画选择器
│       │   └── AnimatedNumber.vue        #   动画数字
│       ├── utils/                        # 工具（4 个）
│       │   ├── token.ts                  #   Token 存储
│       │   ├── websocket.ts              #   WebSocket 客户端（心跳 + 自动重连）
│       │   ├── compress.ts               #   图片压缩
│       │   └── format.ts                 #   日期/文本格式化
│       ├── styles/                       # SCSS（4 个）
│       │   ├── variables.scss            #   设计变量（MintHive 薄荷绿主题）
│       │   ├── mixins.scss               #   SCSS 混入
│       │   ├── animations.scss           #   动画
│       │   └── global.scss               #   全局样式
│       ├── types/index.ts                #   TypeScript 类型定义
│       ├── router/index.ts               #   路由配置（18 条 + 认证守卫）
│       ├── App.vue                       #   根组件（NavBar + TabBar + AI FAB）
│       └── main.ts                       #   入口（Pinia + Router + Vant + Element Plus）
│
├── frontend-admin/                       # Vue 3 管理后台（PC 端 · 暮光玫瑰暗色主题）
│   └── src/
│       ├── views/                        # 管理页面（7 个）
│       │   ├── dashboard/                #   数据大屏（6 统计卡 + 6 ECharts 图 + AI 日报）
│       │   ├── user/                     #   用户管理（封禁/解封/密码重置/僵尸清理）
│       │   ├── content/                  #   内容审核（待审/已发布/敏感词库）
│       │   ├── circle/                   #   圈子管理（审核/下架/推荐/转让）
│       │   ├── report/                   #   举报工单（AI 风险等级排序）
│       │   ├── config/                   #   系统配置（公告/轮播/规则/AI 开关）
│       │   └── login/                    #   管理员登录
│       ├── components/                   # 通用组件（7 个）
│       │   ├── ChartCard.vue             #   ECharts 封装
│       │   ├── StatCard.vue              #   指标卡（数字滚动动画）
│       │   ├── AiReportCard.vue          #   AI 日报卡（SVG 环形健康分）
│       │   ├── RiskLevelTag.vue          #   风险等级标签（脉冲动画）
│       │   ├── DataTable.vue             #   数据表格封装
│       │   ├── StatusTag.vue             #   状态标签
│       │   └── HexagonLogo.vue           #   六边形 Logo
│       ├── layouts/                      # AdminLayout（侧边栏 260px + 顶栏 + 内容区）
│       ├── stores/                       # Pinia（2 个：admin 认证 + stats 数据）
│       ├── styles/                       # SCSS 暗色主题（4 个）
│       │   ├── variables.scss            #   暮光玫瑰设计变量（E879A9 主色）
│       │   ├── element-dark.scss         #   Element Plus 暗色覆写（648 行）
│       │   ├── echarts-theme.ts          #   自定义 minthive ECharts 主题
│       │   └── global.scss               #   全局样式/工具类
│       ├── api/                          # API 模块
│       ├── router/index.ts               #   路由（7 条 + 认证守卫）
│       └── main.ts                       #   入口（Pinia + Router + Element Plus 全局注册）
│
└── docs/                                 # 项目文档
    └── PulseSocial 兴趣社交平台开发软件需求规格说明书（SRS）.md
```

---

## ▸ API 概览

### ◆ 用户端 API（`/api/**`）

| 模块 | 路径前缀 | 核心端点 | 认证 |
|------|----------|----------|------|
| Auth | `/api/auth` | 登录 / 注册 / 短信验证码 / 重置密码 / 登出 | 公开 |
| User | `/api/user` | 个人信息 / 修改资料 / 头像 / 兴趣标签 / 注销 | Token |
| Post | `/api/post` | 发布 / 详情 / 信息流 / 点赞 / 收藏 / 转发 / 草稿 | Token |
| Comment | `/api/comment` | 发表评论 / 帖子评论列表 / 删除 / 点赞 | Token |
| Circle | `/api/circle` | 圈子列表 / 详情 / 加入 / 离开 / 创建 / 推荐 / 分类 | 部分 |
| Circle Admin | `/api/circle-admin` | 圈主管理 / 成员移出 / 公告发布 / 转让圈主 | 圈主 |
| Message | `/api/message` | 发送私信 / 聊天记录 / 已读 / AI 消息撤回 / 通知 | Token |
| Follow | `/api/follow` | 关注 / 取关 / 关注列表 / 粉丝列表 / AI 推荐好友 | Token |
| Search | `/api/search` | 全局搜索 / 用户 / 帖子 / 圈子 / 热词 | 公开 |
| AI | `/api/ai` | 文案生成 / 润色 / 评论 / 私信代回复 / 内容检测 / 问答流式 / 推荐 | Token |
| File | `/api/file` | 文件上传（图片 / 视频 → MinIO） | Token |
| Report | `/api/report` | 提交举报（4 种目标类型） | Token |
| Config | `/api/config` | 公共配置 / Banner | 公开 |
| Announcement | `/api/announcement` | 系统公告列表 | 公开 |

### ◇ 管理端 API（`/api/admin/**`）

| 模块 | 路径前缀 | 核心端点 |
|------|----------|----------|
| User | `/api/admin/user` | 用户列表 / 详情 / 封禁 / 解封 / 密码重置 / 僵尸清理 |
| Stats | `/api/admin/stats` | 核心指标 / 趋势 / 圈子排行 / AI 日报 / Excel 导出 |
| Content | `/api/admin/content` | 待审列表 / 已发布列表 / 审批 / 驳回 / 删除 / 敏感词管理 |
| Report | `/api/admin/report` | 工单列表 / 详情 / 驳回 / 删除内容 / 处罚用户 |
| Circle | `/api/admin/circle` | 圈子列表 / 申请审核 / 下架 / 编辑 / 推荐 / 权限转让 |
| Config | `/api/admin/config` | 公告 / 轮播 / 平台规则 / 操作员 / AI 功能开关 |

---

## ▸ 数据库设计

共 **13 张核心表**：

| 表名 | 实体 | 说明 |
|------|------|------|
| `user` | User | 用户表（BCrypt 加密 / 兴趣标签 JSON） |
| `post` | Post | 帖子表（审核状态 / 可见性 / AI 生成标记） |
| `comment` | Comment | 评论表（支持楼中楼嵌套） |
| `like_collect` | LikeCollect | 点赞/收藏联合表（类型区分） |
| `follow` | Follow | 关注关系表 |
| `circle` | Circle | 圈子表（公开/私密 / 推荐标记） |
| `circle_category` | CircleCategory | 圈子分类表 |
| `circle_user` | CircleUser | 圈子成员关系表 |
| `message` | Message | 私信消息表（AI 代回复标记） |
| `report` | Report | 举报工单表（5 种违规类型 / AI 风险等级） |
| `system_msg` | SystemMsg | 系统消息推送表 |
| `announcement` | Announcement | 系统公告表 |
| `ai_user_log` | AiUserLog | AI 使用日志表（DAU 统计 / 限流计量） |

---

## ▸ AI 双模式部署

MintHive 的 AI 模块采用 **策略模式** 设计，支持一键切换部署模式，无需改动代码：

| 模式 | 配置 | 适用场景 |
|------|------|----------|
| **Cloud** | `ai.mode=cloud` + API Key | 有外网环境，调用 OpenAI 兼容 API（DeepSeek / GPT 等） |
| **Local** | `ai.mode=local` + Ollama | 内网私有化部署，Ollama + Qwen3-7B，数据不出内网 |

```yaml
# application.yml — 切换仅需修改此配置
ai:
  mode: cloud          # cloud | local
  cloud:
    base-url: https://api.openai.com
    api-key: ${AI_CLOUD_API_KEY}
    model: gpt-4o-mini
  local:
    base-url: http://localhost:11434
    model: qwen3:7b
  rate-limit: 50       # 每用户每日调用上限
  cache:
    enabled: true
    ttl: 3600
  fallback:
    enabled: true
```

**AI 基础设施：**

| 组件 | 说明 |
|------|------|
| AiContext | 策略选择器，根据 `ai.mode` 路由至 Cloud/Local 实现 |
| AiRateLimiter | Redis 令牌桶，每用户每日 50 次，午夜自动重置 |
| AiCacheManager | Redis 缓存 AI 响应（参数哈希键，可配 TTL） |
| AiFallback | 降级处理器，AI 服务异常时返回兜底文案，不阻塞基础业务 |

---

## ▸ 限流策略

| 接口 | 限额 | 实现 |
|------|------|------|
| AI 调用 | 50 次/日/用户 | Redis 令牌桶 |
| 发帖 | 50 次/日/用户 | Redis 计数器 |
| 评论 | 200 次/日/用户 | Redis 计数器 |
| 私信 | 100 次/日/用户 | Redis 计数器 |
| 文件上传 | 图片 5MB / 视频 20MB / 时长 60s | Spring 配置 |

---

## ▸ 界面预览

<!-- TODO: 替换为实际截图路径 -->

<table>
<tr>
<td align="center" width="50%">
<b>首页信息流</b><br/>
<i>瀑布流布局 · 智能推荐/最新/最热三模式切换</i>
</td>
<td align="center" width="50%">
<b>圈子广场</b><br/>
<i>分类浏览 · 搜索 · 推荐轮播 · 加入/申请</i>
</td>
</tr>
<tr>
<td align="center">
<img src="" width="400" alt="首页信息流截图" onerror="this.style.display='none';this.parentElement.innerHTML='<div style=&quot;height:200px;display:flex;align-items:center;justify-content:center;background:#161b22;border-radius:8px;color:#4ECDC4;font-size:14px;&quot;>首页信息流截图占位</div>'"/>
</td>
<td align="center">
<img src="" width="400" alt="圈子广场截图" onerror="this.style.display='none';this.parentElement.innerHTML='<div style=&quot;height:200px;display:flex;align-items:center;justify-content:center;background:#161b22;border-radius:8px;color:#4ECDC4;font-size:14px;&quot;>圈子广场截图占位</div>'"/>
</td>
</tr>
<tr>
<td align="center">
<b>私信聊天</b><br/>
<i>WebSocket 实时消息 · AI 代回复 · 已读回执</i>
</td>
<td align="center">
<b>管理后台数据大屏</b><br/>
<i>ECharts 暮光玫瑰主题 · AI 日报 · Excel 导出</i>
</td>
</tr>
<tr>
<td align="center">
<img src="" width="400" alt="私信聊天截图" onerror="this.style.display='none';this.parentElement.innerHTML='<div style=&quot;height:200px;display:flex;align-items:center;justify-content:center;background:#161b22;border-radius:8px;color:#4ECDC4;font-size:14px;&quot;>私信聊天截图占位</div>'"/>
</td>
<td align="center">
<img src="" width="400" alt="管理后台截图" onerror="this.style.display='none';this.parentElement.innerHTML='<div style=&quot;height:200px;display:flex;align-items:center;justify-content:center;background:#161b22;border-radius:8px;color:#4ECDC4;font-size:14px;&quot;>管理后台截图占位</div>'"/>
</td>
</tr>
</table>

---

## ▸ 技术亮点

| 维度 | 实现 |
|------|------|
| **AI 解耦** | 策略模式双模式切换，Cloud/Local 零代码切换，降级不阻塞业务 |
| **实时通信** | 原生 WebSocket + Redis 在线追踪，11 种消息类型全覆盖 |
| **认证安全** | JWT + Redis Token 双校验，BCrypt 自适应加盐加密，无 MD5 遗留 |
| **风控体系** | DFA 敏感词算法 + AI 语义复审，识别谐音/变体/暗语，图片本地 AI 检测 |
| **限流防护** | Redis 令牌桶 + 计数器，AI/发帖/评论/私信分级限流 |
| **文件存储** | MinIO 本地私有化存储，图片自动压缩，无第三方云依赖 |
| **前端工程** | Vue 3 + Vite + TypeScript，Pinia 持久化状态，WebSocket 自动重连心跳 |
| **管理后台** | ECharts 自定义暗色主题，SVG 环形/滚动数字动画，XLSX 多 Sheet 导出 |
| **跨端部署** | Capacitor 原生 Android 打包，Web + H5 响应式适配，移动端安全区域适配 |

---

## ▸ 贡献指南

感谢你对 MintHive 的关注！欢迎通过以下方式参与贡献：

1. **Fork** 本仓库到你的 GitHub 账号
2. 创建特性分支：`git checkout -b feature/your-feature`
3. 提交变更：`git commit -m 'feat: add your feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 提交 **Pull Request**，描述变更内容与关联 Issue

提交规范建议遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

| 前缀 | 用途 |
|------|------|
| `feat:` | 新功能 |
| `fix:` | 修复 Bug |
| `docs:` | 文档更新 |
| `style:` | 代码格式调整 |
| `refactor:` | 重构 |
| `perf:` | 性能优化 |
| `test:` | 测试相关 |

---

## ▸ License

本项目基于 [MIT License](LICENSE) 开源。

---

## ▸ 致谢

<table>
<tr>
<td align="center">
<a href="https://spring.io/projects/spring-boot" target="_blank"><b>Spring Boot</b></a><br/>
<span style="font-size:12px;color:#7A86B8">后端框架</span>
</td>
<td align="center">
<a href="https://vuejs.org/" target="_blank"><b>Vue.js</b></a><br/>
<span style="font-size:12px;color:#7A86B8">前端框架</span>
</td>
<td align="center">
<a href="https://baomidou.com/" target="_blank"><b>MyBatis-Plus</b></a><br/>
<span style="font-size:12px;color:#7A86B8">ORM 增强</span>
</td>
<td align="center">
<a href="https://spring.io/projects/spring-ai" target="_blank"><b>Spring AI</b></a><br/>
<span style="font-size:12px;color:#7A86B8">AI 集成框架</span>
</td>
<td align="center">
<a href="https://ollama.com" target="_blank"><b>Ollama</b></a><br/>
<span style="font-size:12px;color:#7A86B8">本地大模型</span>
</td>
<td align="center">
<a href="https://github.com/QwenLM/Qwen3" target="_blank"><b>Qwen3</b></a><br/>
<span style="font-size:12px;color:#7A86B8">开源大语言模型</span>
</td>
</tr>
</table>

<br/>

<div align="center">
<sub>Built with </sub><b style="color:#4ECDC4">Mint</b><sub> fresh ideas · United in the </sub><b style="color:#FFB627">Hive</b>
</div>