package com.example.fruitmallmanagementsystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionReadinessValidatorTests {
    @Test
    void rejectsUnsafeProductionDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", "development-secret")
                .withProperty("app.payment.demo-enabled", "true")
                .withProperty("app.cors.allowed-origins", "http://localhost:5173")
                .withProperty("spring.datasource.username", "root")
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost/fruit_mall?useSSL=false");

        assertThatThrownBy(() -> new ProductionReadinessValidator(environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("生产环境安全检查未通过");
    }

    @Test
    void acceptsExplicitSecureProductionConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", "3L!fX9qP2vN8sR4mK7dC1zA6wE5yT0uI9oB3gH8jQ2pV7nM4xS6cD1kZ5rF0aW8Y7")
                .withProperty("app.payment.demo-enabled", "false")
                .withProperty("app.cors.allowed-origins", "https://mall.example.com")
                .withProperty("spring.datasource.username", "fruit_mall_app")
                .withProperty("spring.datasource.url", "jdbc:mysql://db.example.com/fruit_mall?sslMode=VERIFY_IDENTITY")
                .withProperty("app.security.distributed-state-enabled", "true")
                .withProperty("spring.data.redis.host", "redis.example.com");

        assertThatCode(() -> new ProductionReadinessValidator(environment).run(null)).doesNotThrowAnyException();
    }
}
