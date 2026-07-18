package com.example.fruitmallmanagementsystem.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("databaseIntegrity")
public class DatabaseIntegrityHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseIntegrityHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer issues = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_schema_issue", Integer.class);
            if (issues != null && issues > 0) {
                return Health.down().withDetail("unresolvedSchemaIssues", issues)
                        .withDetail("action", "Run scripts/verify-database.ps1 and inspect sys_schema_issue").build();
            }
            return Health.up().withDetail("unresolvedSchemaIssues", 0).build();
        } catch (Exception exception) {
            return Health.down(exception).withDetail("action", "Apply database migrations through v6").build();
        }
    }
}
