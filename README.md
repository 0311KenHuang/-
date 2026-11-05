# 污染源自动监控设施第三方运维信息管理系统

面向生态环境监管方的一体化平台：**HJ 212—2025 数据接入 + 第三方运维闭环 + 监管取证 + 多端触达**。

> 需求与设计文档在 [`docs/`](docs/index.html)，本仓库是可运行的工程骨架。

---

## 一、目录结构

```
项目1104/
├─ docs/                     需求规格、库表设计、接入方案、报警推送、页面清单 + ADR + 2 张架构图
├─ backend/                  **服务端正式实现（Java / Spring Boot 2.7.18 / JDK 8）**
│  ├─ env-common/            统一响应、异常、租户上下文、审计 AOP、traceId
│  ├─ env-dao/               实体、Mapper、多租户插件、Flyway 脚本
│  ├─ env-ts/                时序数据：CP 解析、分钟落库、附录 G 小时/日聚合
│  ├─ env-service/           业务服务（auth / station / monitor / system）
│  ├─ env-api/               HTTP 接口、Sa-Token 配置、启动类
│  ├─ env-ingest/            HJ212 Netty 接入（独立进程）
│  └─ mvn.sh                 本机 Maven 启动器（系统 Maven 安装不完整）
├─ server/                   NestJS 参考实现（逻辑一致，可对照阅读，不再演进）
├─ web/                      管理端（Vue3 + Vite + Element Plus）
├─ deploy/docker-compose.yml MySQL / Redis（可选 TDengine）
└─ README.md
```

> 服务端语言决策见 `docs/00-架构决策记录.html`：甲方指定 Java，团队以 Java 为主，且部署环境锁定 JDK 8，因此选 Spring Boot 2.7.18（3.x 需 JDK 17）。

---

## 二、技术栈与选型说明

| 层 | 选型 | 说明 |
|---|---|---|
| 服务端 | **Spring Boot 2.7.18 + JDK 8 + MyBatis-Plus 3.5.5 + Sa-Token** | 甲方指定 Java，部署环境锁 JDK 8；详见 `backend/README.md` |
| 业务库 | MySQL 8（Flyway 管理迁移） | 档案、单据、审计 |
| 时序库 | TDengine 3（待接入） | 1000 站房约 1.44 亿条/天，关系库扛不住 |
| 缓存 | Redis（待接入） | 幂等去重、会话、限流 |
| 管理端 | Vue3 + Vite + Element Plus + Pinia | 与 `docs/05` 的页面清单一致 |
| 移动端 | uni-app（待建） | 一套代码出小程序 + 公众号 H5 |
| 参考实现 | NestJS（`server/`） | 先期验证过的逻辑，Java 版按它平移 |

---

## 三、快速开始

### 1. 起依赖

```bash
cd deploy
docker compose up -d          # MySQL 3306 / Redis 6379
```

### 2. 起服务端（Java）

```bash
cd backend
./mvn.sh clean package -DskipTests
# 本地开发必须显式开 dev profile（SQL 日志、debug 级别都在这里）
./mvn.sh -pl env-api spring-boot:run -Dspring-boot.run.profiles=dev
# 或：java -jar env-api/target/env-api-0.1.0.jar --server.port=8080 --spring.profiles.active=dev
```

> 不带 profile 启动是"安全默认值"：不打印 SQL、日志级别 info。
> 生产用 `--spring.profiles.active=prod`，数据库/Redis 凭据必须由环境变量注入（无默认值）。

首次启动 Flyway 自动建表并写入演示数据。验证：

```bash
curl http://127.0.0.1:8080/api/health
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

> 本机 `mvn` 安装缺 `conf/m2.conf` 无法启动，所以用 `backend/mvn.sh`（内置独立 Maven + 显式 JDK 8）。
> 正常环境直接用 `mvn` 即可。

### 2.1 起参考实现（NestJS，可选）

```bash
cd server
npm install --registry=https://registry.npmmirror.com
npm run seed && npm run dev
```

### 3. 起管理端

```bash
cd web
npm install --registry=https://registry.npmmirror.com
npm run dev                   # http://127.0.0.1:5173
npm run build                 # 交付构建：vue-tsc 类型检查 + vite 生产构建，需 @types/node
```

### 4. 跑单测与打包

```bash
cd backend
./mvn.sh test                 # 65 个用例（接入 / 时序 / 报警 / 消息 / 运维 / 参数）
./mvn.sh clean package -DskipTests
```

---

## 四、已实现 / 待实现

### 已实现（骨架级）

- **统一响应与异常**：`{ code, message, data, traceId }`，业务异常 HTTP 200 + 业务码
- **多租户上下文**：`AsyncLocalStorage` 贯穿请求，`Ctx.tenantId()` 取不到直接抛错
- **认证鉴权**：JWT + `@Public()` / `@Roles()`，超管默认放行
- **审计**：`@Audit(module, action)` 注解式写 `sys_audit_log`，密码类字段自动脱敏
- **站房主数据**：分页查询（租户隔离 + 软删除）、详情、新建、改状态
- **HJ212 接入**：
  - CRC16/MODBUS + 报文编解码（`##` + 4 位长度 + 数据段 + CRC）
  - 流式拆包器，处理粘包 / 半包 / 垃圾前缀 / 假帧头
  - TCP 服务（先应答后落库、空闲 300s 断开）+ HTTP 上报入口
  - 原始报文落库 `hj212_frame`（含 CP 原文，重算不丢多 DataTime 组）
- **时序数据（MySQL 承载，TDengine 待切）**：
  - CP 解析为 `(station_id, factor_code, ts, value, dev_flag)` 纵向分钟记录，唯一键幂等覆盖，重发/补传不重复
  - 附录 G：小时 ≥45 有效分钟、日 ≥75% 有效小时；整点滚动重算 + 次日 00:30 出日值
  - 计算前置处理：超量程按量程上限夹取并标 T、负值修约、停运区间剔除（日均分母扣减停运小时）
  - 工作台接口：站房实时值、单站有效率/日数据、停运区间、历史报文重算
- **报警引擎（三条核心规则）**：超标（连续 ≥3 分钟，排除 D/C/M/T 与停运）、恒值（连续 10 条完全相同）、缺数（连续 >30 分钟）；规则级别/抑制窗口/阈值存 `alm_rule`，每 5 分钟按租户扫描，事件按对象去重、条件恢复自动关单，处置写 `alm_action`
- **消息中心**：飞书交互卡片 + 短信/小程序网关适配 + 站内消息兜底；模板、接收人路由、渠道矩阵（级别 × 昼夜 × 升级阶段）、T+15/T+30 升级、发送日志与重试降级闭环
- **运维证据链**：月度计划（覆盖率/未排期看板）→ 任务下发与状态流转（接单/到场 300 米围栏/提交/改派/退回）→ 现场记录（检查项/照片/标记区间）→ 审核（审核人≠执行人，通过后闭环任务与关联报警）；报警可一键转工单，标记 ⇄ 工单双向核验输出疑似虚假标记与漏标
- **工作参数监控**：解析 2025 新增上传的量程/斜率/截距/校准浓度/消解温度与 SN/CPUID/MAC/固件，快照只追加，变更留痕并产生 P1 报警；CN 可按现场厂商约定配置，通知由 Web 进程补发，不拖慢设备应答
- **工作台概览**：站房数、状态分布、报警数（含未结束数）、今日报文、今日分钟数据量与有效率
- **管理端**：登录、主框架（侧栏/面包屑/角色标签）、工作台（实时值 + 报警待办 + 消息触达）、站房列表、运维证据链页面

### 待实现（按优先级）

| 优先级 | 内容 |
|---|---|
| P0 | TDengine 切换（分钟/小时/日已用 MySQL 落地；需先部署 TDengine 服务端） |
| P0 | 作业模板可视化、物资出库联动、电子签名/PDF 归档；证据链主干已上线 |
| P0 | 飞书交互卡片回调（认领按钮）、P3 日报汇总、电话呼叫兜底；消息中心其余链路已上线 |
| P1 | 物资管理（流向追溯）、考勤与轨迹、执法检查与整改 |
| P1 | 小程序（扫码巡检、离线填报、打卡） |
| P2 | 远程反控（校时 1012/1013、读参）、数据分析与考核 |

### 交付状态（2026-11-05 真实联调后）

| 检查项 | 状态 |
|---|---|
| 后端编译 / 65 个单测 / 打包 | 通过（`./mvn.sh test`、`-o package`） |
| 前端类型检查 + 生产构建 | 通过（`npm run build`，已补 `@types/node`） |
| Flyway V1~V9 增量迁移 | **通过**：真实 MySQL 上 V4~V9 依次执行成功，新表 12 张 |
| 真实联调（报文 → 数据 → 报警 → 通知 → 工单 → 审核） | **通过**，发现并修复 1 个真实缺陷（默认查询窗口漏昨天） |
| 外部通道（飞书/短信/小程序）真实触达 | 未做：缺 `FEISHU_WEBHOOK_URL` 等凭据；SKIPPED 降级链路已验证 |
| TDengine / 设备 5000 并发压测 / 小程序端 | 未做，需对应环境 |

联调已验证的关键行为：未注册 MN 拒收且原文留档；附录 G 的 1/60、45 分钟边界；缺数报警自动产生并升级到 T+30；连续 3 分钟超标触发 P1；工作参数 500→650 变更产生 P1 且留痕；未配置通道记 SKIPPED 不假装送达；任务跨状态拒绝、300 米围栏拒绝异地打卡、BR-04 审核不相容、审核通过闭环任务；标记 C 无运维记录触发「疑似虚假标记」。

---

## 五、开发约定

1. **所有业务表带 `tenant_id`**，查询一律 `Ctx.tenantId()` 注入，禁止手写常量。
2. **写操作必须标 `@Audit`**，监管系统里"没留痕"等于"没发生"。
3. **业务异常用 `BizException`**，不要直接抛 `Error`；错误码集中在 `common/errors`。
4. **实体不返回给前端**：列表接口显式剔除 `passwordHash` 等字段。
5. **DDL 变更走 migration**，`DB_SYNCHRONIZE` 只在本地开。
6. **HJ212 只做接入，不做计算**：解析 → 落库 → 投递，计算交给下游，避免接入层被慢逻辑拖死。

---

## 六、HJ212 接入说明

| 项 | 说明 |
|---|---|
| TCP 端口 | `HJ212_TCP_PORT`，默认 20000，现场机主动连接 |
| HTTP 入口 | `POST /api/ingest/hj212`，body 为原始报文字符串或 `{ "raw": "..." }` |
| HTTP 鉴权 | Header `X-Ingest-Token`，值取 `INGEST_TOKEN`（未配置则不校验，生产必须配） |
| 应答格式 | `HJ212_ACK_MODE=qnrtn`（默认，CP 回 `QnRtn`）或 `exerntn`（回 `ExeRtn`）——**以现场设备厂商约定为准** |
| 数据源模式 | `INGEST_MODE=direct` 走自建 TCP；`upstream` 时跳过 TCP，改为对接上级平台 |

> 命令编码（2011/2051/3020/1012/1013/1014 等）以 HJ 212—2025 正式文本为准，`docs/03` 里有完整表格与测试用例清单。

---

## 七、下一步建议

1. 确认数据源模式（自建接入 vs 对接国发/省平台），决定 `ingest` 模块是否需要写 upstream 适配器。
2. 把 `ts_data_minute/hour/day` 从 MySQL 切到 TDengine（写入/聚合 SQL 集中在 `env-dao` 的 `TsData*Mapper`，接口不变）。
3. 打通运维证据链：计划 → 任务 → 记录 → 审核，这是甲方最容易验收的部分。
4. 报警引擎先做 8 条核心规则（超标、离线、缺数、恒值、密钥到期、SN 不一致、参数变更、虚假标记），跑通飞书卡片闭环。
