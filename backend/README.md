# env-monitor · 服务端（Java）

污染源自动监控设施第三方运维信息管理系统 —— Spring Boot 2.7.18 / JDK 8。

> 选型依据见 `docs/00-架构决策记录.html` 与 `~/.workbuddy/plans/` 中的迁移方案。
> 同仓库的 `server/` 是早先用 NestJS 写的参考实现，逻辑一致，可对照阅读。

## 模块

| 模块 | 职责 | 关键类 |
|---|---|---|
| `env-common` | 统一响应、异常、租户上下文、审计切面、traceId | `TenantContextHolder`、`AuditAspect`、`ResponseAdvice`、`GlobalExceptionHandler` |
| `env-dao` | 实体、Mapper、多租户插件、Flyway 脚本 | `MybatisPlusConfig`、`TsDataMinuteMapper`、`V4__ts_data.sql` |
| `env-ts` | 时序数据：CP 解析、分钟落库、附录 G 聚合 | `Hj212DataParser`、`MinuteDataService`、`TsAggregateService`、`TsAggregateJob` |
| `env-service` | 业务服务 | `AuthService`、`StationService`、`MonitorService`、`AuditWriterImpl` |
| `env-api` | HTTP 接口与启动类 | `EnvApiApplication`、`SaTokenConfigure`、`TenantContextInterceptor` |
| `env-ingest` | HJ212 Netty 接入（独立进程） | `Hj212Codec`、`Hj212FrameDecoder`、`Hj212Server` |

## 关键设计

1. **多租户**：`MyBatis-Plus TenantLineInnerInterceptor` 自动补 `tenant_id`，业务 SQL 不用手写租户条件。
   登录等无上下文场景用 `@InterceptorIgnore(tenantLine = "true")` 显式放行。
2. **上下文传递**：`TransmittableThreadLocal` 而非普通 `ThreadLocal`，线程池/异步任务不会丢租户。
3. **审计**：`@Audit(module, action)` + AOP，写库不阻塞业务；密码类字段不落审计。
4. **统一响应**：`{ code, message, data, traceId }`，与前端既有契约完全一致，`web/` 无需改动。
5. **接入层独立**：Netty 单独进程，先应答后落库，避免设备重发打爆平台。
6. **MN 归属**（直连模式的关键）：报文头 `MN` → `md_device.mn` → `station_id`。
   已注册设备正常入库并回 `QnRtn=1`；未注册 MN 回 `QnRtn=2` 拒收，**但原文仍落库留档**，
   用于排查"谁在往平台发数据"。映射有本地缓存，设备变更后调 `FrameService.evict(mn)`。
7. **时序数据**：接入层解析 CP 后写 `ts_data_minute`（纵向，一站一因子一分钟），
   小时/日由 `env-api` 的定时任务按附录 G 聚合（≥45 有效分钟 / ≥75% 有效小时）。
   分钟/小时/日均用唯一键 + upsert，补传乱序晚到只重算、不重复。
   接入进程只写分钟表，聚合任务用 `ts.job.enabled=false` 关掉，避免两个进程重复计算。
8. **附录 G 计算前置处理**：写分钟表前做超量程夹取（`md_station_factor.range_max`，
   补 `T` 标记）与负值修约（`ts.calc.negative-fix`，按因子配置）；停运区间存
   `md_station_stop`，由站房状态切换自动开闭，小时聚合用 `NOT EXISTS` 剔除、
   日均分母扣减停运小时，恢复运行时自动重算该站房的历史小时值。
9. **报警引擎**：`alm_rule` 存规则级别/持续时长/抑制窗口/参数，每 5 分钟按租户扫描
   三条数据类规则（超标 / 恒值 / 缺数）；命中后按"活动事件 + 抑制窗口"去重，
   条件恢复自动关单并写 `alm_action`。停运站房整站跳过，停运时段数据不参与判定。
   新租户缺内置规则时引擎自动补齐，阈值改 `params_json` 即可，不用改代码。
10. **消息中心**：报警产生/恢复/人工处置都触发通知；模板存 `msg_template`，
    接收人按"责任工程师 → 值班人 → 角色兜底"解析，渠道按级别 × 时段 × 升级阶段决策。
    外部通道失败重试 3 次后降级站内消息，每次发送（含 SKIPPED）都写 `msg_send_log`；
    T+15 / T+30 未认领自动升级，阶段记在 `alm_event.escalate_stage` 防重复推送。
11. **运维证据链**：月度计划按"应运维站房"生成，发布后明细实例化为任务；任务状态机
    `DISPATCHED → ACCEPTED → ARRIVED → SUBMITTED → CLOSED` 每步写 `ops_task_log`，
    到场打卡按 300 米电子围栏校验（BR-03）。记录含检查项、照片水印与标记区间，审核
    人不得为执行人（BR-04），通过后自动关闭任务及其关联报警。标记 ⇄ 工单双向核验
    输出"疑似虚假标记 / 漏标"清单（BR-05 / BR-06）。
12. **工作参数监控**：`hj212.work-param-cn` 指定哪些 CN 是工作参数报文；按参数名
    白名单解析（带因子前缀时保留为 `因子:参数`），写只追加的 `dev_param_snapshot`，
    与上一次值不同则记 `dev_param_change` 并产生 P1 事件（1 小时抑制）。接入层不碰
    外部通道，首报由 `DeviceParamNotifyJob` 用"事件无发送日志"判据补发，天然幂等。

## 数据源

甲方已确认 **数采仪直连**（非对接上级平台），因此：

- `env-ingest` 属于生产链路，必须部署并放通 TCP 20000；
- 原始报文、双维度标记、工作参数、远程反控全部可用；
- 上级平台对接（Upstream）本期不做，仅保留配置位。

## 本地启动

### 1. 数据库

```bash
cd ../deploy
docker compose up -d mysql redis
```

首次启动 Flyway 会自动建表并写入演示数据（`V1__init.sql` / `V2__seed.sql` / `V3__device_mn.sql`）。
V3 会给 `md_device` 加 `mn` 列并写入 3 台演示数采仪（MN 与站房一一对应），同时给 `hj212_frame` 加 `station_id`。

### 2. 接口服务

```bash
cd backend
./mvn.sh clean package -DskipTests
./mvn.sh -pl env-api spring-boot:run
# 或：java -jar env-api/target/env-api-0.1.0.jar
```

> 本机系统 Maven 安装缺 `conf/m2.conf`，`mvn` 无法启动，所以用 `mvn.sh`
> （内置 `backend/.tools/apache-maven-3.9.9`，并显式设置 JDK 8 的 `JAVA_HOME`）。
> 在正常环境里直接用 `mvn` 即可。

### 3. 接入服务（可选）

```bash
./mvn.sh -pl env-ingest spring-boot:run
# 监听 20000，数采仪直连上报
```

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查（免登录） |
| POST | `/api/auth/login` | 登录，`{"username":"admin","password":"Admin@123"}` |
| GET | `/api/auth/profile` | 当前用户 |
| GET | `/api/station?page=1&pageSize=20` | 站房分页 |
| POST | `/api/station` | 新建站房（ORG/MGR） |
| PATCH | `/api/station/{id}/status` | 变更状态（停运必填原因） |
| GET | `/api/monitor/overview` | 工作台概览（含今日分钟数据量与有效率） |
| GET | `/api/monitor/realtime?limit=200` | 站房实时值（跨站房，各因子最新一条） |
| GET | `/api/monitor/station/{id}/realtime` | 单站房实时值（含未上报因子） |
| GET | `/api/monitor/station/{id}/valid-rate?date=2026-11-04` | 单站房有效率（附录 G） |
| GET | `/api/monitor/station/{id}/day?date=2026-11-04` | 单站房日数据 |
| POST | `/api/monitor/ts/rebuild?from=2026-11-01&to=2026-11-04` | 历史报文重算（SUP/REG/MGR） |
| GET | `/api/alarm/events?status=OPEN&page=1&pageSize=20` | 报警事件分页（级别/站房可筛） |
| PATCH | `/api/alarm/events/{id}/handle` | 认领/关闭/误报，`{"action":"ACK","remark":"..."}` |
| POST | `/api/alarm/events/{id}/task` | 报警一键转工单，回写 `alm_event.related_task_id` |
| POST | `/api/alarm/scan` | 手工触发全租户扫描（SUP/REG/MGR） |
| POST | `/api/ops/plan/generate` | 按月生成运维计划，已排期站房跳过 |
| GET | `/api/ops/plan` / `/plan/coverage` | 计划列表、BR-01 覆盖率与未排期站房 |
| POST | `/api/ops/plan/{id}/publish` | 计划明细实例化为任务 |
| GET | `/api/ops/task` / `PATCH /task/{id}/action` | 任务列表与状态流转（接单/到场/提交/改派/退回） |
| POST | `/api/ops/record` / `/record/{id}/submit` | 填报现场记录（明细/照片/标记区间）、提交审核 |
| GET | `/api/ops/record/{id}` / `/record/pending` | 记录详情（含审核轨迹）、待审核列表 |
| POST | `/api/ops/audit/{recordId}` | 通过/驳回，通过后闭环任务与关联报警 |
| GET | `/api/ops/trace/check?from=&to=` | 标记 ⇄ 工单双向核验（BR-05 / BR-06） |
| GET | `/api/device/params/latest?stationId=` | 站房各设备工作参数最新值 |
| GET | `/api/device/params?stationId=&from=&to=` | 参数历史快照 |
| GET | `/api/device/params/changes?from=&to=` | 参数变更留痕（含关联报警事件） |
| GET | `/api/message/logs?eventId=&status=&channel=` | 消息发送日志（SENT/FAILED/SKIPPED） |
| GET | `/api/message/channels` | 通道可用性及未配置原因 |
| POST | `/api/message/test?level=P1` | 发送测试卡片（SUP/REG/MGR） |
| POST | `/api/message/escalate` | 手工触发升级扫描（SUP/REG/MGR） |
| GET | `/api/station/{id}/factors` | 站房因子档案（量程/限值） |
| GET | `/api/station/{id}/stops?from=&to=` | 停运区间（默认最近 30 天） |
| GET | `/api/system/user` | 用户列表（ORG/REG） |

鉴权：`Authorization: Bearer <token>`（Sa-Token，`token-name` + `token-prefix` 已在 `application.yml` 配好）。

## 测试

```bash
./mvn.sh test
```

共 65 个用例：

- `env-ingest` 10 个：HJ212 CRC 标准值、编解码回环、长度字段、CRC 篡改、非法结构、应答、粘包、半包、垃圾前缀
- `env-ts` 30 个：CP 解析（多 DataTime 组 / QN 兜底 / 标记优先级）、工作参数白名单与因子前缀、附录 G 的 45-44 分钟与 18-17 小时边界、停运分母扣除、超量程夹取、负值修约、MyBatis `<script>` SQL 解析
- `env-service` 25 个：报警判定与渠道矩阵/升级阈值、模板渲染、任务状态机（跨状态拒绝、改派、退回）、300 米电子围栏、BR-04 审核不相容

## 已知限制

- Spring Boot 2.7 的 OSS 支持已结束（2023-11），生产需内网部署 + 依赖扫描；详见 ADR。
- 仅实现 11 张核心表，其余 44 张按模块增量迁移（完整 DDL 见 `docs/schema.sql`）。
- 时序数据暂用 MySQL 承载（`V4__ts_data.sql`），TDengine 服务端就绪后替换 `TsData*Mapper` 的 SQL 即可；Excel/PDF 报表、工作流尚未接入。
- 附录 G 已覆盖分钟/小时/日有效性、降级规则、超量程夹取、负值修约、停运区间剔除；停运区间由站房状态切换维护，历史数据可用 `/api/monitor/ts/rebuild` 重算。地方考核口径若与 45 分钟 / 75% 不同，改 `ts.calc.*` 配置即可。
- 报警引擎目前只有三条数据类规则，`alm_rule.expression` 字段保留但还不是通用表达式求值。
- 消息中心：飞书群机器人 Webhook 可用（交互卡片只带"查看详情"按钮，**认领按钮回调**需要企业自建应用 + 公网 HTTPS 回调）；短信/小程序为网关适配器，未配置 `SMS_GATEWAY_URL` / `WXA_GATEWAY_URL` 时记 SKIPPED 并降级到站内消息；公众号订阅通知与 openid 绑定、P3 每日 09:00 汇总、电话呼叫兜底尚未实现。
- 通道密钥走环境变量：`FEISHU_WEBHOOK_URL`、`FEISHU_ENABLED`、`SMS_GATEWAY_URL`、`WXA_GATEWAY_URL`、`NOTIFY_DETAIL_URL`，未配置不会影响报警事件落库。
- 运维侧：作业模板可视化配置、多媒体对象存储与水印生成、物资出库联动、电子签名/PDF 归档、人脸比对、计划审批流与拖拽改期尚未接入；当前到场打卡校验坐标距离，管理端演示用站房坐标，现场应由小程序上报真实 GPS。
- 超标按实测值与 `md_station_factor.std_limit` 比较，折算值（`-ZsRtd`）暂未落库；缺数只对近 2 天有数据的因子判定，长期离线应走设备类规则。
- HJ212 仅支持明文报文解析；SM4 解密工具已就绪（`Sm4Util`），待拿到设备密钥后启用。
