package com.example.fruitmallmanagementsystem.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile("prod")
public class ProductionReadinessValidator implements ApplicationRunner {
    private final Environment environment;

    public ProductionReadinessValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();
        String jwtSecret = value("app.jwt.secret");
        String secretLower = jwtSecret.toLowerCase(Locale.ROOT);
        if (jwtSecret.length() < 64 || secretLower.contains("development")
                || secretLower.contains("local") || secretLower.contains("replace")) {
            errors.add("JWT_SECRET 必须是至少 64 位的随机生产密钥");
        }
        if (environment.getProperty("app.payment.demo-enabled", Boolean.class, true)) {
            errors.add("生产环境必须设置 PAYMENT_DEMO_ENABLED=false");
        }
        String origins = value("app.cors.allowed-origins").toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(origins) || origins.contains("localhost")
                || origins.contains("127.0.0.1") || origins.contains("*")) {
            errors.add("CORS_ALLOWED_ORIGINS 必须只包含正式 HTTPS 域名");
        }
        String databaseUser = value("spring.datasource.username");
        if (!StringUtils.hasText(databaseUser) || "root".equalsIgnoreCase(databaseUser)) {
            errors.add("生产数据库必须使用独立的低权限账号，不能使用 root");
        }
        String databaseUrl = value("spring.datasource.url").toLowerCase(Locale.ROOT);
        boolean tlsEnabled = databaseUrl.contains("sslmode=required")
                || databaseUrl.contains("sslmode=verify") || databaseUrl.contains("usessl=true");
        if (!tlsEnabled) {
            errors.add("生产 DB_URL 必须启用 MySQL TLS（sslMode=REQUIRED/VERIFY_CA/VERIFY_IDENTITY）");
        }
        if (!environment.getProperty("app.security.distributed-state-enabled", Boolean.class, false)) {
            errors.add("生产环境必须启用 DISTRIBUTED_STATE_ENABLED=true");
        }
        if (!StringUtils.hasText(value("spring.data.redis.host"))) {
            errors.add("生产环境必须配置 REDIS_HOST");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("生产环境安全检查未通过：\n- " + String.join("\n- ", errors));
        }
    }

    private String value(String key) {
        return environment.getProperty(key, "").trim();
    }
}
