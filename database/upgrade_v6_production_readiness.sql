-- Fruit Mall v6: production scheduler locking, payment callback idempotency and restored integrity constraints.
-- Safe to run repeatedly on MySQL 8.
USE `fruit_mall`;

CREATE TABLE IF NOT EXISTS `sys_schema_version` (
  `version` varchar(100) NOT NULL,
  `description` varchar(255) NOT NULL,
  `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sys_scheduler_lock` (
  `lock_name` varchar(100) NOT NULL,
  `lock_until` datetime(3) NOT NULL,
  `locked_at` datetime(3) NOT NULL,
  `locked_by` varchar(128) NOT NULL,
  PRIMARY KEY (`lock_name`),
  KEY `idx_scheduler_lock_until` (`lock_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多实例定时任务数据库锁';

CREATE TABLE IF NOT EXISTS `sys_payment_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_event_id` varchar(128) NOT NULL,
  `payment_no` varchar(64) DEFAULT NULL,
  `order_no` varchar(64) DEFAULT NULL,
  `channel` varchar(20) NOT NULL,
  `payload_hash` char(64) NOT NULL,
  `signature_valid` tinyint NOT NULL DEFAULT 0,
  `processed` tinyint NOT NULL DEFAULT 0,
  `failure_reason` varchar(255) DEFAULT NULL,
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_notification_event` (`channel`, `provider_event_id`),
  KEY `idx_payment_notification_order` (`order_no`, `received_at`),
  KEY `idx_payment_notification_pending` (`processed`, `received_at`),
  CONSTRAINT `chk_payment_notification_flags` CHECK (`signature_valid` IN (0,1) AND `processed` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付平台通知幂等记录（不保存敏感原文）';

CREATE TABLE IF NOT EXISTS `sys_refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_hash` char(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `token_version` int NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `replaced_by_hash` char(64) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used_at` datetime DEFAULT NULL,
  `ip_hash` char(64) DEFAULT NULL,
  `user_agent_hash` char(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_user_expiry` (`user_id`, `expires_at`),
  KEY `idx_refresh_expiry_revoked` (`expires_at`, `revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷新令牌（仅保存不可逆哈希）';

CREATE TABLE IF NOT EXISTS `sys_schema_issue` (
  `constraint_name` varchar(100) NOT NULL,
  `issue_detail` varchar(500) NOT NULL,
  `detected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`constraint_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库完整性约束恢复问题';

DELIMITER $$
DROP PROCEDURE IF EXISTS `upgrade_v6_production_readiness`$$
CREATE PROCEDURE `upgrade_v6_production_readiness`()
BEGIN
  DECLARE v_count INT DEFAULT 0;

  SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND COLUMN_NAME = 'refund_status';
  IF v_count = 0 THEN
    ALTER TABLE `sys_payment`
      ADD COLUMN `refund_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未退款 1退款中 2已退款 3退款失败' AFTER `paid_time`,
      ADD COLUMN `refund_amount` decimal(10,2) DEFAULT NULL AFTER `refund_status`,
      ADD COLUMN `refund_time` datetime DEFAULT NULL AFTER `refund_amount`;
  END IF;

  SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND INDEX_NAME = 'idx_payment_status_time';
  IF v_count = 0 THEN
    ALTER TABLE `sys_payment` ADD INDEX `idx_payment_status_time` (`status`, `create_time`);
  END IF;

  SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND INDEX_NAME = 'idx_order_user_status_time';
  IF v_count = 0 THEN
    ALTER TABLE `sys_order` ADD INDEX `idx_order_user_status_time` (`user_id`, `status`, `create_time`);
  END IF;

  -- Remove only relation-table orphans. Historical orders, payments and audit logs are preserved.
  DELETE ur FROM `sys_user_role` ur LEFT JOIN `sys_user` u ON u.id = ur.user_id WHERE u.id IS NULL;
  DELETE ur FROM `sys_user_role` ur LEFT JOIN `sys_role` r ON r.role_id = ur.role_id WHERE r.role_id IS NULL;
  DELETE rp FROM `sys_role_permission` rp LEFT JOIN `sys_role` r ON r.role_id = rp.role_id WHERE r.role_id IS NULL;
  DELETE rp FROM `sys_role_permission` rp LEFT JOIN `sys_permission` p ON p.permission_id = rp.permission_id WHERE p.permission_id IS NULL;
  DELETE a FROM `sys_user_address` a LEFT JOIN `sys_user` u ON u.id = a.user_id WHERE u.id IS NULL;

END$$

CALL `upgrade_v6_production_readiness`()$$
DROP PROCEDURE IF EXISTS `upgrade_v6_production_readiness`$$

DROP PROCEDURE IF EXISTS `restore_v6_foreign_key`$$
CREATE PROCEDURE `restore_v6_foreign_key`(
  IN p_table varchar(64), IN p_constraint varchar(100), IN p_column varchar(64),
  IN p_reference_table varchar(64), IN p_reference_column varchar(64),
  IN p_delete_action varchar(20), IN p_update_action varchar(20)
)
restore_block: BEGIN
  DECLARE v_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_count FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND BINARY CONSTRAINT_NAME = BINARY p_constraint;
  IF v_count > 0 THEN
    DELETE FROM `sys_schema_issue` WHERE BINARY `constraint_name` = BINARY p_constraint;
    LEAVE restore_block;
  END IF;

  BEGIN
    DECLARE v_message varchar(500) DEFAULT 'unknown error';
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
      GET DIAGNOSTICS CONDITION 1 v_message = MESSAGE_TEXT;
      INSERT INTO `sys_schema_issue` (`constraint_name`, `issue_detail`)
      VALUES (p_constraint, LEFT(v_message, 500))
      ON DUPLICATE KEY UPDATE `issue_detail` = VALUES(`issue_detail`), `detected_at` = CURRENT_TIMESTAMP;
    END;

    SET @migration_sql = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_constraint,
      '` FOREIGN KEY (`', p_column, '`) REFERENCES `', p_reference_table, '` (`', p_reference_column,
      '`) ON DELETE ', p_delete_action, ' ON UPDATE ', p_update_action);
    PREPARE migration_statement FROM @migration_sql;
    EXECUTE migration_statement;
    DEALLOCATE PREPARE migration_statement;
    DELETE FROM `sys_schema_issue` WHERE BINARY `constraint_name` = BINARY p_constraint;
  END;
END$$

CALL `restore_v6_foreign_key`('sys_order', 'fk_order_user', 'user_id', 'sys_user', 'id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_order_item', 'fk_order_item_order', 'order_no', 'sys_order', 'order_no', 'CASCADE', 'CASCADE')$$
CALL `restore_v6_foreign_key`('sys_order_item', 'fk_order_item_product', 'product_id', 'sys_product', 'id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_order_log', 'fk_order_log_order', 'order_no', 'sys_order', 'order_no', 'CASCADE', 'CASCADE')$$
CALL `restore_v6_foreign_key`('sys_payment', 'fk_payment_order', 'order_no', 'sys_order', 'order_no', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_payment', 'fk_payment_user', 'user_id', 'sys_user', 'id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_role_permission', 'fk_role_permission_role', 'role_id', 'sys_role', 'role_id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_role_permission', 'fk_role_permission_permission', 'permission_id', 'sys_permission', 'permission_id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_user_role', 'fk_user_role_user', 'user_id', 'sys_user', 'id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_user_role', 'fk_user_role_role', 'role_id', 'sys_role', 'role_id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_user_address', 'fk_address_user', 'user_id', 'sys_user', 'id', 'RESTRICT', 'RESTRICT')$$
CALL `restore_v6_foreign_key`('sys_refresh_token', 'fk_refresh_token_user', 'user_id', 'sys_user', 'id', 'CASCADE', 'RESTRICT')$$

DROP PROCEDURE IF EXISTS `restore_v6_foreign_key`$$
DELIMITER ;

INSERT INTO `sys_schema_version` (`version`, `description`)
VALUES ('20260716_v6_production_readiness', '生产定时锁、支付通知幂等、退款字段、关键索引、关联清理与外键恢复')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);
