# 水果商城管理系统

本科毕业设计一体化项目，包含顾客商城和后台管理端。后端使用 Spring Boot 3、MyBatis-Plus、MySQL、JWT，前端使用 Vue 3、Vite、Pinia、Element Plus。

## 已实现功能

顾客端形成了完整购物闭环：

- 商品浏览、分类、搜索与商品详情
- 本地持久化购物车和库存校验
- 注册、登录、收货地址管理
- 提交订单、服务端价格重算与原子扣减库存
- 微信、支付宝、演示支付三种界面选择
- 我的订单、订单详情、支付状态、取消订单、确认收货
- 待支付订单 30 分钟自动关闭并恢复库存

后台管理端包含：

- 数据概览、商品、订单、支付流水
- 人员、角色、权限和用户角色分配
- 订单日志、系统日志
- 接口级角色/权限校验，顾客不能访问管理接口

## 环境要求

- JDK 17
- Node.js 18 或更高版本
- MySQL 8
- `mysql.exe` 已加入 PATH

## 最快启动方式

首次使用或已有数据库，在项目根目录直接运行：

```powershell
.\start-dev.ps1
```

如果 8080 或 5173 被上一次启动遗留的本项目进程占用，启动脚本会先安全停止该进程再重新启动；若端口属于其他软件，脚本会显示占用程序和 PID，不会误杀。也可以随时手动运行 `.\stop-dev.ps1` 后再启动，或用 `.\start-dev.ps1 -PreflightOnly` 只检查并清理端口而不启动服务。

脚本会安全询问本机 MySQL 密码，不会把密码写入代码；如果 `fruit_mall` 不存在会自动初始化，已存在则按照数据库版本表只执行尚未安装的升级。随后等待后端健康检查通过，再启动前端。

停止本项目启动的前后端服务：

```powershell
.\stop-dev.ps1
```

停止脚本会核对端口进程的工作区命令行，不会关闭其他项目占用的进程。

也可以按需单独执行数据库脚本：

```powershell
.\scripts\init-database.ps1    # 仅用于重建全新数据库
.\scripts\upgrade-database.ps1
```

访问地址：

- 顾客商城：`http://127.0.0.1:5173/mall`
- 顾客订单：`http://127.0.0.1:5173/mall/orders`
- 收货地址：`http://127.0.0.1:5173/mall/addresses`
- 后台登录：`http://127.0.0.1:5173/login`
- 后端健康检查：`http://127.0.0.1:8080/actuator/health`

演示账号：

- 管理员：`admin / 123456`
- 顾客：`3516932719@qq.com / 123456`
- 也可以在顾客端注册新账号

旧数据中的明文演示密码在首次正确登录后会自动升级为 BCrypt。

## 数据库与 Navicat

- 完整初始化文件：`database/fruit_mall.sql`
- 现有数据库最新升级：`database/upgrade_v7_audit_reliability.sql`
- 一键升级脚本：`scripts/upgrade-database.ps1`（检查 `sys_schema_version` 后按 v2 → v7 执行未安装版本）
- 备份脚本：`scripts/backup-database.ps1`
- 完整性检查：`scripts/verify-database.ps1`

升级增加商品详情与销量、收货地址、订单超时字段、商品快照、多支付尝试索引，并清除普通顾客角色误继承的后台权限。刷新 Navicat 的 `fruit_mall@localhost` 连接后即可看到更新。

人员和角色数据遵循以下规则：

- `sys_user.id`、`sys_role.role_id` 是内部自增主键，不要求连续，也不向用户表达业务含义
- 页面显示稳定业务编号 `sys_user.user_no`，员工格式为 `EMP000001`，顾客格式为 `CUS000001`
- `sys_user_role` 是用户与角色关系的唯一数据源，联合唯一索引防止重复分配
- 删除人员采用停用方式，保留订单、支付和日志历史

## 手动启动

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/fruit_mall?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的本机数据库密码"
$env:JWT_SECRET="至少 64 位的随机生产密钥"

.\mvnw.cmd spring-boot:run
```

另开终端：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

验证构建：

```powershell
.\mvnw.cmd -DskipTests package
cd frontend
npm.cmd run build
```

## 支付说明

开发环境中的微信和支付宝选项使用本地演示支付适配流程，适合毕业答辩完整展示，不会产生真实扣款。订单金额只由后端根据数据库商品价格计算，支付成功后支付流水、订单状态、销量和日志会在事务中协调更新。生产环境默认关闭所有演示渠道；没有真实平台适配器时后端会拒绝创建支付单，避免出现无法完成的伪支付。

接入真实支付前需要申请微信商户号或支付宝应用，并补充：服务端 SDK 下单、异步通知验签、金额与商户号核验、通知防重放、主动查单和退款状态机。没有商户密钥时不应伪装成真实支付，也不要让前端直接决定支付成功。

## 主要接口

- `GET /api/mall/products`：公开商品列表与搜索
- `GET /api/mall/products/{id}`：公开商品详情
- `POST /auth/register`：顾客注册
- `GET/POST/PUT/DELETE /api/mall/addresses`：本人收货地址
- `POST /api/mall/orders`：本人提交订单
- `GET /api/mall/orders`：本人订单
- `POST /api/mall/orders/{orderNo}/cancel`：取消待支付订单
- `POST /api/mall/orders/{orderNo}/confirm-receipt`：确认收货
- `POST /api/payment/create`：按渠道创建支付尝试
- `GET /api/payment/status`：查询本人订单的最新支付状态

除登录、注册、验证码、健康检查和公开商品接口外，其余业务请求都需要携带：

```text
Authorization: Bearer <登录返回的 token>
```

## 生产运行基线

- 本地默认使用 `dev` Profile；公网必须显式设置 `SPRING_PROFILES_ACTIVE=prod`
- `prod` Profile 会检查 JWT 密钥、HTTPS CORS 域名、MySQL TLS、非 root 数据库账号、Redis 和演示支付开关，不合格时拒绝启动
- Redis 统一保存生产验证码 Session 和登录失败次数，支持多实例运行
- 订单超时任务使用 `sys_scheduler_lock`，多个后端实例不会重复执行同一轮任务
- `/actuator/prometheus` 提供监控指标，生产管理端口默认只监听 `127.0.0.1:9090`
- `deploy/` 提供后端容器、前端 Nginx、安全响应头和生产 Compose 模板
- `.github/workflows/ci.yml` 会验证真实 MySQL 迁移、后端集成测试和前端生产构建

完整上线步骤与回滚检查见 `docs/production-deployment.md`，项目内外工作边界见 `docs/go-live-checklist.md`。真实微信/支付宝仍需商户资质、官方 SDK、回调验签、主动查单、退款和每日对账，不能仅靠配置开启。
