# 污染源自动监控运维系统 · 缺陷评估报告（复核版）

- 复核日期：2026-11-04
- 复核范围：`ken/backend`（Spring Boot 多模块）、`ken/web`（Vue3）、`ken/deploy`、工程化配置
- 复核方式：逐条核对前次评估的证据文件 + 补充扫描（SQL 注入、越权、CORS、XSS、依赖版本、缓存一致性、登录防爆破）
- 结论先行：**前次报告的 4 项 P0、5 项 P1 全部属实**；另发现 **6 项前次未覆盖的新缺陷**（其中 1 项 P1）；工程缺口部分有 1 处需要更正（deploy 目录已有 docker-compose，但仅为本地开发依赖编排）。

---

## 一、P0 高风险问题（全部复核属实）

### P0-1 多租户登录歧义 ✅ 属实
- 证据：`env-dao/.../mapper/SysUserMapper.java:19-21` — `selectByUsernameIgnoreTenant` 按 `username` 全表查 `LIMIT 1`，**无 ORDER BY、无租户过滤**；`AuthService.java:45` 中 `tenantCode` 是**可选参数**，不填则直接跳过租户校验。
- 后果：两个租户存在同名用户时，不填租户编码将登录到"恰好排在前面的那个"租户的用户，且无任何提示。
- 建议：登录必须三选一强制：① 前端强制传 `tenantCode`；② 用户名全局唯一（DDL 加唯一约束 + 注册校验）；③ 同名返回候选租户列表让用户选择。

### P0-2 dataScope 只存不用 ✅ 属实
- 证据：全库 grep `dataScope|DataScope|DATA_SCOPE`，业务侧仅 4 处（`AuthService.java:72-79,92,115` 写入会话/DTO；`TenantContextInterceptor.java:46-48` 装回上下文），**没有任何查询/SQL 消费它**。
- 后果：`ORG/REGION/SELF` 数据权限形同虚设，租户内任意用户可看全部站房数据（跨租户有插件兜底，租户内无隔离）。
- 建议：在 Service/Mapper 层引入统一的 scope 谓词（如查询构造器自动追加 `org_id =` / `user_id =` 条件），或至少在站房、任务、记录三个核心列表先落地。

### P0-3 HJ212 注释与实现相反 + DB 操作在 Netty 事件线程 ✅ 属实
- 证据：`Hj212ChannelHandler.java:15-19` 注释称"先应答后落库"，但 `channelRead0`（第 46-77 行）实际是 `safePersist` → `writeAck`，即**先同步落库后应答**（这个顺序本身是对的，注释错了）；`Hj212Server.java:61-64` pipeline 直接 `addLast(businessHandler)`，**无 EventExecutorGroup**，`frameMapper.insert` / `minuteDataService.accept` 全部跑在 event loop 线程上。
- 后果：DB 抖动会阻塞所有设备连接的 IO 事件循环（一个慢库拖垮整个接入端口）；注释误导后续维护者。
- 建议：`addLast(new DefaultEventExecutorGroup(16), businessHandler)` 把业务Handler 摘到业务线程池；修正注释为"先落库后应答"。

### P0-4 落库失败仍应答成功 ✅ 属实
- 证据：`Hj212ChannelHandler.java:79-85` — `safePersist` catch 后仅记日志，外层无条件 `writeAck(ctx, m, 1)`（成功回执）。
- 后果：数据库故障期间设备认为上报成功，平台实际丢数，且设备不会重发。
- 建议：persist 失败时 `writeAck(ctx, m, 2/3)`（拒绝/错误），让现场机走重发机制；原文留存逻辑保留。

---

## 二、P1 问题（全部复核属实）

### P1-1 生产配置不安全 ✅ 属实
`env-api/src/main/resources/application.yml`：
- 第 14 行 `useSSL=false`；第 15-16 行默认 `root/root123456`；第 29 行 Redis 默认空密码
- 第 39 行 `log-impl: StdOutImpl`（SQL 全量控制台输出）+ 第 126 行 `com.envmon: debug` —— 生产会泄露数据、拖慢性能
- 附带发现：第 52 行 `active-timeout: -1`（token 永不因闲置过期）、`is-concurrent: true`（允许多端并存）

### P1-2 定时任务无分布式锁 ✅ 属实
6 个 `@Scheduled`（5 个类）：`AlarmJob`、`DeviceParamNotifyJob`、`NotifyJob`、`OpsOverdueJob`、`TsAggregateJob`（小时+日 2 个）。全库无 Redisson/ShedLock/`setIfAbsent`。多实例部署必重复扫描、聚合、发消息。已有 Redis 依赖，接 ShedLock 或 Redisson 最省事。

### P1-3 参数校验缺失 ✅ 属实
9 个 Controller 中仅 `AuthController`（1 处）、`StationController`（2 处）用了 `@Valid`；`OpsController.java:48` 的 `PlanGenerate` 无 `@Valid`；`StationController.java:61` `changeStatus` 直接收 `Map<String, String>` 取 `bizStatus`，取值合法性依赖 Service 内部判断。

### P1-4 实体直出、API 与表结构耦合 ✅ 属实
`StationController` 返回 `MdStation` 实体、`OpsController` 返回 `OpsPlan/OpsPlanItem/OpsTask` 等，`tenantId`、`passwordHash` 类内部字段有随实体外泄风险（新增字段即自动暴露）。

### P1-5 审计"异步"名不副实 ✅ 属实
`AuditAspect.java:19` 注释写"异步写审计"，实际链路是切面（请求线程）→ `AuditWriterImpl.write` → `auditLogMapper.insert`，纯同步。业务高峰时审计 insert 会在请求线程串行排队。

---

## 三、新发现的缺陷（前次报告未覆盖）

### N-1（P1）MN 归属缓存永不失效，设备改绑后数据持续错归
`env-ingest/.../FrameService.java:48-80`：`mnCache`（MN→stationId/tenantId）只有 `evict/evictAll` 方法，但**全后端无任何调用方**；且 ingest 与 api 是两个独立进程，即使 api 侧设备管理想调也跨进程无效。
- 后果：设备改绑站房、删除重建 MN 后，接入进程继续按旧缓存归错站房，**直到重启 ingest**；错误数据还可能触发错站房报警。
- 建议：① ingest 侧加 TTL（如 10 分钟）+ 版本号失效；② 或 MN 归属查询改为读 Redis；③ 至少在设备管理保存后发 MQ/HTTP 通知 ingest 清缓存。

### N-2（P1）登录无防爆破
`AuthService.login` 无验证码、无失败次数锁定、无 IP 限速（grep 无 captcha/attempt/lock 逻辑）；配合 `active-timeout: -1`，被撞库成功的会话长期有效。建议：失败 N 次锁定 + Redis 计数限速 + 上线验证码。

### N-3（P2）ts_* 表游离于租户插件外，依赖"约定式"防线
`MybatisPlusConfig.java:34-43` 把 `ts_data_minute/hour/day` 列入 IGNORE_TABLES（接入进程无登录态，设计上合理），防线变成"`TsQueryService` 每个入口必须先 `requireStation(stationId)`"——这是纯人工约定，新增一个查询入口漏调即跨租户泄露。建议：把 requireStation 校验下沉到 Mapper 层公共入口或 AOP，不靠每个方法自觉。

### N-4（P2）Spring Boot 2.7.18 已 EOL + JDK 8
Spring Boot 2.7.x OSS 支持 2023-11 已终止，无安全补丁来源；JDK 8 亦属遗留版本。MVP 阶段可接受，生产上线前需规划升级列车（2.7 → 3.x 需 JDK 17 + jakarta 迁移，工程量大，建议单独立项）。

### N-5（P2）token 存 localStorage，XSS 可窃取
`web/src/api/http.ts:14-16`。配合无 CSP 配置，一旦有注入点即可长期持有 token。建议：短期加 CSP 响应头，中期评估 cookie+CSRF 方案。

### N-6（P2）工程杂项
- `mvn.sh` 硬编码本机 JDK 路径（`JAVA_HOME=C:/Program Files/...`）——本机变通可以理解，但应改为 `JAVA_HOME=${JAVA_HOME:-默认值}` 参数化，否则换机器即失效
- `deploy/docker-compose.yml` 注释残留 NestJS/TypeORM 的过时说明（"TypeORM synchronize 自动建表"），与实际 Flyway 体系矛盾，会误导新人
- 根目录无 `.gitignore`（仅 `backend/.gitignore`），且项目根本不是 git 仓库——**版本控制缺位仍是第一优先工程债**

---

## 四、对前次报告的更正

| 前次表述 | 复核结果 |
|---|---|
| "HJ212 代码声称先应答后落库，实际先同步查库落库再应答" | 属实，但补充：**先落库后应答本身是正确顺序**，问题在注释错误 + 事件线程阻塞 + 失败吞异常三件事 |
| "没有生产部署编排" | 更正：`deploy/docker-compose.yml` 已存在，但仅编排本地开发依赖（MySQL/Redis），无应用镜像、无 Dockerfile，结论实质成立 |
| "6 个定时任务" | 属实：5 个 Job 类、6 个 @Scheduled 方法 |
| 前端主包约 1.12 MB | 属实：`web/dist/assets/index-*.js` = 1.1MB，dist 总计 1.7MB |

## 五、复核通过项（无问题）

- Mapper 层无 `${}` 拼接，无 SQL 注入入口
- 前端无 `v-html`，无明显 XSS sink
- `ts_*` 查询侧统一走 `requireStation`，现有入口均已覆盖
- 接入进程写业务表（工作参数）前显式注入临时租户上下文（`DeviceParamService.java:32,54-67`），处理正确
- 前次报告"已做得较好的部分"与本次复核一致

## 六、修复路线建议（按序）

1. **第一批（P0，1-2 周）**：登录租户消歧 → HJ212 线程池 + 失败应答修正 → dataScope 落地查询
2. **第二批（P1 + 新发现，2-4 周）**：配置安全（密钥外置/SSL/日志降级）→ 分布式锁 → MN 缓存失效 → 登录防爆破 → DTO 化输出 + @Valid
3. **第三批（工程化，并行推进）**：`git init` + 根 .gitignore → CI（构建+测试+镜像）→ 集成测试（真实库 + Flyway + 租户隔离 + 鉴权）→ 前端分包降主包体积
4. **第四批（上线前）**：Spring Boot 升级立项、CSP/token 方案、审计异步化

---

*复核方式说明：本次为静态复核（代码逐条核对 + 定向扫描），未复跑 Maven/前端构建；测试通过情况沿用前次结论（Java 65 通过、Vue 类型检查/构建通过、NestJS 11 通过）。*

---

# 修复实施记录（2026-11-05，两批：a7a20b1 / 5a48089）

## 第一批（a7a20b1）：4 项 P0 + P1 快赢 + 新发现 N-1/N-2

| 编号 | 修复内容 | 关键改动 |
|---|---|---|
| P0-1 | 登录消歧 | `SysUserMapper` 新增 `countByUsernameIgnoreTenant`/`selectByUsernameAndTenant`；未传 tenantCode 且同名用户 >1 时拒绝并提示填写；传 tenantCode 时按 username+tenantId 精确取人 |
| P0-2 | dataScope 落地 | 新增 `DataScopeRules`（env-common，规则单源）+ `DataScopeService`（env-service，可见站房集合）；`StationService.page/detail/listByEnterprise`、`OpsTaskService.page`（SELF 按 assignee）、`AlarmEventService.page`、`TsQueryService.requireStation/realtime` 全部接入；`TenantContextInterceptor` 补恢复 orgId/regionId（**修复过程中发现原实现连 orgId 都没恢复**）；`V10__user_region.sql` 给 sys_user 加 region_id |
| P0-3 | HJ212 线程模型 | `Hj212Server` pipeline 业务 handler 挂 `DefaultEventExecutorGroup`（`hj212.business-threads:16`），event loop 不再跑同步 JDBC；注释改为"先落库后应答" |
| P0-4 | 失败应答 | `tryPersist` 返回布尔：落库失败回 `QnRtn=3`（已注册、未注册留档失败两条路径），设备按协议重发 |
| P1-1 | 生产配置 | 新增 `application-prod.yml`：SQL 日志 NoLogging、com.envmon 日志降 info、DB/Redis 凭据强制环境变量（无默认值）、useSSL 可配 |
| P1-2 | 分布式锁 | 新增 `RedisJobLock`（env-common，SET NX EX + Lua 比对释放，Redis 不可用 fail-open）；6 个 @Scheduled 全部接入（含 TsAggregateJob 启动补算），skip-fast 语义 |
| N-1 | MN 缓存 TTL | `FrameService.mnCache` 改为带时间戳条目，默认 600s 过期重查（`hj212.mn-cache-ttl-seconds`） |
| N-2 | 登录防爆破 | 同用户名 5 次失败锁 15 分钟（Redis 计数，fail-open） |
| 工程债 | 版本控制 | `git init`（main 分支）+ 根 `.gitignore` |

## 第二批（5a48089）：P1 剩余项 + 工程加固

| 编号 | 修复内容 | 关键改动 |
|---|---|---|
| P1-3 | 参数校验 | 全部 @RequestBody 补 `@Valid`；`OpsRequest`（action/taskId/result 等）、`AlarmHandleRequest` 加字段约束；`StationController.changeStatus` 与 `AlarmController.createTask` 的裸 Map 改类型化 DTO（`StationChangeStatusRequest`/`AlarmTaskRequest`） |
| P1-4 | 实体脱敏 | `SysUser.passwordHash` 加 `@JsonIgnore`——**SystemController 用户列表此前直出口令哈希给前端**；`TenantEntity.tenantId` 加 `@JsonIgnore`（已确认前端零消费） |
| P1-5 | 审计异步化 | `AuditWriterImpl` 改单线程后台执行器（有界队列 1000，满则丢弃并记日志，PreDestroy 优雅收尾），请求线程只构建 entry |
| N-5 | 安全响应头 | `SecurityHeadersFilter`：nosniff / X-Frame-Options: DENY / Referrer-Policy / Cache-Control: no-store；CSP 建议部署层（nginx）配置 |
| N-6 | 工程杂项 | `mvn.sh` JAVA_HOME 增加可执行性校验+自动回退（**环境变量指向已卸载的 jdk-8.0.412 会导致构建炸掉**）；docker-compose 过时 TypeORM 注释改为 Flyway 说明 |
| P0-2 补 | overview 作用域 | `MonitorService.overview` 站房/报警计数按可见站房集合过滤（报文计数保留平台级） |
| 前端 | 分包 | `vite.config.ts` manualChunks：**主包 1.1MB → 4.82KB**（vendor-element 815KB / vendor-vue 127KB / vendor-misc 175KB 独立缓存）；确认 echarts 为死依赖（源码零引用，建议网络良好时从 package.json 移除） |
| 测试 | 集成测试 | env-api 新增 `ApiSmokeTest`（`RUN_IT=true` 门控连真实 MySQL）：健康/登录成功/密码错误 40100/未登录 401，4/4 通过 |

## 验证结果（全部实跑，2026-11-05）

- 第一批：`mvn clean test` 6 模块 BUILD SUCCESS；真实 TCP 联调（QnRtn=1 / 坏 CRC QnRtn=3 / 实时值入库）；Flyway V10 自动应用
- 第二批：`mvn clean test` 6 模块 BUILD SUCCESS（60 单测）+ `RUN_IT=true` 集成 4/4 + `vue-tsc` 零错误 + `vite build` 通过（主包 4.82KB）
- git 提交链：`992d8b2` 初始 → `a7a20b1` P0/P1 批一 → `5a48089` P1 批二（main 分支）

## 修复中顺带发现并处理

- **Sa-Token 会话不能存 null**：`SaSession.set("orgId", null)` 内部 ConcurrentHashMap 抛 NPE（admin 的 orgId 为空时登录 500）→ 归属字段仅非空才写入
- 本机测试帧规范：长度字段 = 数据段字节数（不含 CRC）；CRC = CRC16/MODBUS（0xA001）
- 环境变量 JAVA_HOME 指向已卸载的 JDK 时，mvn.sh 必须做可执行性校验

## 未修复 / 遗留

- **CI**：git 仓库已就绪但无远端，CI 平台（GitHub Actions / Gitee Go）待定后补 workflow
- **N-4** Spring Boot 2.7 EOL 升级立项（单独立项，工程量大）
- N-3 ts_* 约定式防线的 Mapper 层根治；审计 before_json 字段在切面中未填充（仅 after_json）
- 部署层 nginx 的 CSP/反代配置（后端已给安全头，静态资源侧需部署时补）
- 前端 echarts 死依赖清理（需 npm install，本机网络受限时暂缓）
