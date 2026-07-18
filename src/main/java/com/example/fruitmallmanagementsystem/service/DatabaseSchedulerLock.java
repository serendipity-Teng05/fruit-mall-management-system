package com.example.fruitmallmanagementsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class DatabaseSchedulerLock {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchedulerLock.class);
    private final JdbcTemplate jdbcTemplate;
    private final String instanceId;

    public DatabaseSchedulerLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.instanceId = (ManagementFactory.getRuntimeMXBean().getName() + "-" + UUID.randomUUID())
                .substring(0, Math.min(120, ManagementFactory.getRuntimeMXBean().getName().length() + 37));
    }

    public boolean tryAcquire(String name, Duration maximumDuration) {
        try {
            jdbcTemplate.update("INSERT IGNORE INTO sys_scheduler_lock "
                    + "(lock_name, lock_until, locked_at, locked_by) VALUES (?, NOW(), NOW(), ?)", name, instanceId);
            int updated = jdbcTemplate.update("UPDATE sys_scheduler_lock SET lock_until = ?, "
                            + "locked_at = NOW(), locked_by = ? WHERE lock_name = ? AND lock_until <= NOW()",
                    Timestamp.from(Instant.now().plus(maximumDuration)), instanceId, name);
            return updated == 1;
        } catch (Exception exception) {
            log.error("Unable to acquire scheduler lock {}. Has database v6 been applied?", name, exception);
            return false;
        }
    }

    public void release(String name) {
        try {
            jdbcTemplate.update("UPDATE sys_scheduler_lock SET lock_until = NOW() "
                    + "WHERE lock_name = ? AND locked_by = ?", name, instanceId);
        } catch (Exception exception) {
            log.warn("Unable to release scheduler lock {}", name, exception);
        }
    }
}
