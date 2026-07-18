-- Fruit Mall v7: make authentication audit records reliable.
-- Anonymous and failed authentication requests do not have a user id yet, so the audit column must be nullable.
USE `fruit_mall`;

CREATE TABLE IF NOT EXISTS `sys_schema_version` (
  `version` varchar(100) NOT NULL,
  `description` varchar(255) NOT NULL,
  `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DELIMITER $$
DROP PROCEDURE IF EXISTS `upgrade_v7_audit_reliability`$$
CREATE PROCEDURE `upgrade_v7_audit_reliability`()
BEGIN
  DECLARE v_nullable varchar(3) DEFAULT NULL;
  DECLARE v_count INT DEFAULT 0;

  SELECT IS_NULLABLE INTO v_nullable
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'user_id'
    LIMIT 1;
  IF v_nullable = 'NO' THEN
    ALTER TABLE `sys_log`
      MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT '操作用户ID；未认证请求允许为空';
  END IF;

  SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND INDEX_NAME = 'idx_log_user_time';
  IF v_count = 0 THEN
    ALTER TABLE `sys_log` ADD INDEX `idx_log_user_time` (`user_id`, `create_time`);
  END IF;
END$$

CALL `upgrade_v7_audit_reliability`()$$
DROP PROCEDURE IF EXISTS `upgrade_v7_audit_reliability`$$
DELIMITER ;

INSERT INTO `sys_schema_version` (`version`, `description`)
VALUES ('20260716_v7_audit_reliability', '允许未认证请求写入审计日志，并补充用户时间索引')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);
