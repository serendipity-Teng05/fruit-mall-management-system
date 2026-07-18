# 水果商城生产部署与上线检查

## 1. 发布前准备

1. 准备独立的生产 MySQL 与 Redis，生产数据库运行账号不得使用 `root`。
2. 为 MySQL 开启 TLS，并建立两个账号：迁移账号负责 DDL，应用账号只拥有业务表所需 DML 权限。
3. 从 `deploy/production.env.example` 复制出服务器私有配置，真实文件不得进入代码仓库。
4. JWT 密钥使用密码管理器生成，至少 64 个随机字符。
5. 在维护窗口先执行备份，再运行升级和完整性检查：

```powershell
.\scripts\backup-database.ps1 -User fruit_mall_migrator
.\scripts\upgrade-database.ps1 -User fruit_mall_migrator
.\scripts\verify-database.ps1 -User fruit_mall_migrator
```

所有 `orphan_*`、`duplicate_*`、`negative_*` 计数必须为 0，最新版本应为 `20260716_v7_audit_reliability`。

## 2. 构建与启动

在 `deploy` 目录创建仅服务器可读的 `production.env`，然后运行：

```powershell
docker compose -f .\deploy\docker-compose.production.yml --env-file .\deploy\production.env build
docker compose -f .\deploy\docker-compose.production.yml --env-file .\deploy\production.env up -d
```

Compose 只把前端端口绑定到本机 `127.0.0.1:8088`。应再由云负载均衡或宿主机 Nginx/Caddy 提供公网 HTTPS，证书终止后转发至该端口。

## 3. 上线验证

- 前端首页、登录页和管理后台无控制台错误。
- 顾客注册、登录、购物车、地址、下单、取消订单、确认收货均通过。
- 管理员可管理商品和订单，顾客访问后台接口返回 403。
- 重复提交相同 `requestId` 不产生重复订单。
- 两个后端实例同时运行时，超时订单只恢复一次库存。
- `/actuator/health/readiness`、`/actuator/health/liveness` 正常。
- Prometheus 能采集 JVM、HTTP、数据库连接池指标。
- 日志能按 `X-Request-Id` 关联，且不包含密码、JWT、数据库密钥或完整支付通知原文。

## 4. 支付上线门槛

生产配置会强制关闭演示支付。开放微信或支付宝之前必须实现并测试：

- 服务端统一下单和平台证书/私钥管理。
- 异步通知验签、解密、金额与商户号核验。
- 使用 `sys_payment_notification` 按平台事件编号防重放和保证幂等。
- 支付结果主动查询、关单、退款、退款查询和每日对账。
- 回调异常告警与人工补单流程。

未完成以上项目时，应保持支付入口不可用，不得把模拟支付标记为真实支付。

## 5. 备份与回滚

- 每日完整备份，并持续保存 binlog；备份需加密并复制到另一存储位置。
- 每月至少执行一次恢复演练，不能只检查备份文件是否存在。
- 应用发布采用滚动或蓝绿方式；数据库迁移优先采用向前兼容的新增字段/索引。
- 发布失败时先回滚应用镜像。数据库优先使用向前修复；只有确认数据损失范围后才执行恢复脚本。
- `restore-database.ps1` 必须显式传入 `-ConfirmRestore`，用于防止误覆盖。

## 6. 监控告警建议

至少为以下指标设置告警：HTTP 5xx、P95 响应时间、Hikari 活跃连接、JVM 内存、Redis 连接失败、待支付订单积压、支付通知处理失败、重复支付通知、库存扣减冲突和定时任务连续未执行。
