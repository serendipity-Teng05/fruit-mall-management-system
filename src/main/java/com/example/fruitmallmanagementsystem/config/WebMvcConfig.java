package com.example.fruitmallmanagementsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AuthorizationInterceptor authorizationInterceptor;
    private final String[] allowedOrigins;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor,
                        AuthorizationInterceptor authorizationInterceptor,
                        @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
        this.allowedOrigins = allowedOrigins.trim().split("\\s*,\\s*");
    }

    /**
     * 1. 配置跨域请求 (CORS)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Request-Id", "Idempotency-Key")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 2. 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/refresh",
                        "/auth/captcha",
                        "/api/mall/products",
                        "/api/mall/products/**",
                        "/error",
                        "/actuator/health",
                        "/favicon.ico",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/**/*.png",
                        "/**/*.jpg"
                );
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/**")
                .order(1);
    }
}
