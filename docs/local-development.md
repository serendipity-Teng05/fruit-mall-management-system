# 本地开发启动说明

## 推荐：一键启动

在项目根目录运行：

```powershell
.\start-dev.ps1
```

脚本会询问本机 MySQL 密码、升级 `fruit_mall` 数据库，并依次启动后端和前端。

## 从 IDEA 直接启动后端

IDEA 的运行按钮不会执行 `start-dev.ps1`，因此第一次使用前需要在项目根目录运行：

```powershell
.\scripts\configure-local.ps1
```

输入 Navicat 中 `root@localhost` 对应的 MySQL 密码。脚本会先验证账号和 `fruit_mall` 数据库，然后生成：

```text
config/application-local.properties
```

该文件已被 Git 忽略，不会提交数据库密码。配置成功后，停止 IDEA 中旧的后端进程，再重新运行 `FruitMallManagementSystemApplication`。

正常启动应在控制台看到：

```text
Database connection validation passed
```

然后访问 `http://127.0.0.1:8080/actuator/health`，应返回 `{"status":"UP"}`。此时刷新登录页或点击验证码区域即可重新加载验证码。

如果 MySQL 密码发生变化，重新运行一次 `configure-local.ps1` 即可。请勿把真实密码写入 `application-dev.properties` 或示例文件。
