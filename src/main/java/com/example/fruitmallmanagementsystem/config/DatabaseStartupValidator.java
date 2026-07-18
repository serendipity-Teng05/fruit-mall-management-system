package com.example.fruitmallmanagementsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseStartupValidator implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupValidator.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseStartupValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result == null || result != 1) {
                throw new IllegalStateException("Unexpected database validation result: " + result);
            }
            log.info("Database connection validation passed");
        } catch (DataAccessException exception) {
            throw new IllegalStateException(
                    "Database connection failed. When starting from IDEA, first run "
                            + ".\\scripts\\configure-local.ps1 in the project root, then restart the backend. "
                            + "When starting from a terminal, use .\\start-dev.ps1 or set DB_PASSWORD.",
                    exception
            );
        }
    }
}
