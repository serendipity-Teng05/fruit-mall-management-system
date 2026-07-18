-- v3: 人员、角色与用户角色关系规范化（MySQL 8）
-- 执行前请先备份。脚本带版本标记，可重复执行。

USE `fruit_mall`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_schema_version` (
  `version` varchar(100) NOT NULL,
  `description` varchar(255) NOT NULL,
  `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库升级记录';

DELIMITER $$

DROP PROCEDURE IF EXISTS `migrate_v3_user_role_normalization`$$
CREATE PROCEDURE `migrate_v3_user_role_normalization`()
BEGIN
  DECLARE v_count INT DEFAULT 0;
  DECLARE v_next_user_no BIGINT DEFAULT 1;

  SELECT COUNT(*) INTO v_count
  FROM `sys_schema_version`
  WHERE `version` = '20260713_v3_user_role_normalization';

  IF v_count = 0 THEN
    -- 角色增加稳定业务编码；主键 role_id 继续只作为内部关联键。
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'role_code';
    IF v_count = 0 THEN
      ALTER TABLE `sys_role` ADD COLUMN `role_code` varchar(50) NULL COMMENT '稳定角色编码' AFTER `role_id`;
    END IF;

    UPDATE `sys_role` SET `role_code` = 'ADMIN' WHERE `role_id` = 1 AND (`role_code` IS NULL OR `role_code` = '');
    UPDATE `sys_role` SET `role_code` = 'CUSTOMER' WHERE `role_id` = 2 AND (`role_code` IS NULL OR `role_code` = '');
    UPDATE `sys_role` SET `role_code` = 'STAFF' WHERE `role_id` = 6 AND (`role_code` IS NULL OR `role_code` = '');
    UPDATE `sys_role` SET `role_code` = CONCAT('ROLE_', `role_id`) WHERE `role_code` IS NULL OR `role_code` = '';
    ALTER TABLE `sys_role` MODIFY COLUMN `role_code` varchar(50) NOT NULL COMMENT '稳定角色编码';

    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND INDEX_NAME = 'uk_role_code';
    IF v_count = 0 THEN
      ALTER TABLE `sys_role` ADD UNIQUE INDEX `uk_role_code` (`role_code`);
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND INDEX_NAME = 'uk_role_name';
    IF v_count = 0 THEN
      ALTER TABLE `sys_role` ADD UNIQUE INDEX `uk_role_name` (`role_name`);
    END IF;

    -- 人员增加业务编号和启停状态。
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'user_no';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD COLUMN `user_no` varchar(20) NULL COMMENT '人员编号' AFTER `id`;
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'status';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用' AFTER `phone`;
    END IF;

    DROP TEMPORARY TABLE IF EXISTS `tmp_user_number`;
    CREATE TEMPORARY TABLE `tmp_user_number` AS
      SELECT `id`, ROW_NUMBER() OVER (ORDER BY `create_time`, `id`) AS `seq`
      FROM `sys_user`
      WHERE `user_no` IS NULL OR `user_no` = '';
    UPDATE `sys_user` u
    JOIN `tmp_user_number` n ON n.`id` = u.`id`
    SET u.`user_no` = CONCAT('EMP', LPAD(n.`seq`, 6, '0'));
    DROP TEMPORARY TABLE `tmp_user_number`;

    ALTER TABLE `sys_user` MODIFY COLUMN `user_no` varchar(20) NOT NULL COMMENT '人员编号';
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_user_no';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_user_no` (`user_no`);
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_user_phone';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD UNIQUE INDEX `uk_user_phone` (`phone`);
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'idx_user_status';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD INDEX `idx_user_status` (`status`, `user_no`);
    END IF;

    -- 迁移旧 role 字段中尚未进入关联表的数据，再删除冗余字段。
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'role';
    IF v_count > 0 THEN
      INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`)
      SELECT u.`id`, r.`role_id`, CURRENT_TIMESTAMP
      FROM `sys_user` u
      JOIN `sys_role` r ON r.`role_code` = CASE UPPER(COALESCE(u.`role`, 'STAFF'))
        WHEN 'ADMIN' THEN 'ADMIN'
        WHEN 'USER' THEN 'CUSTOMER'
        ELSE 'STAFF'
      END
      WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` ur WHERE ur.`user_id` = u.`id`);
      ALTER TABLE `sys_user` DROP COLUMN `role`;
    END IF;

    -- 关联表主键不使用雪花 ID，重建为本地自增小整数；业务关联键保持不变。
    DROP TEMPORARY TABLE IF EXISTS `tmp_user_role`;
    CREATE TEMPORARY TABLE `tmp_user_role` AS
      SELECT `user_id`, `role_id`, MIN(`create_time`) AS `create_time`
      FROM `sys_user_role` GROUP BY `user_id`, `role_id`;
    DELETE FROM `sys_user_role`;
    ALTER TABLE `sys_user_role` AUTO_INCREMENT = 1;
    INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`)
      SELECT `user_id`, `role_id`, `create_time` FROM `tmp_user_role` ORDER BY `user_id`, `role_id`;
    DROP TEMPORARY TABLE `tmp_user_role`;

    DROP TEMPORARY TABLE IF EXISTS `tmp_role_permission`;
    CREATE TEMPORARY TABLE `tmp_role_permission` AS
      SELECT `role_id`, `permission_id` FROM `sys_role_permission` GROUP BY `role_id`, `permission_id`;
    DELETE FROM `sys_role_permission`;
    ALTER TABLE `sys_role_permission` AUTO_INCREMENT = 1;
    INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
      SELECT `role_id`, `permission_id` FROM `tmp_role_permission` ORDER BY `role_id`, `permission_id`;
    DROP TEMPORARY TABLE `tmp_role_permission`;

    -- 数据库级连续人员编号序列，避免业务编号依赖有空洞的自增主键。
    CREATE TABLE IF NOT EXISTS `sys_sequence` (
      `sequence_key` varchar(50) NOT NULL,
      `next_value` bigint NOT NULL,
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`sequence_key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务编号序列表';
    SELECT COALESCE(MAX(CAST(SUBSTRING(`user_no`, 4) AS UNSIGNED)), 0) + 1 INTO v_next_user_no FROM `sys_user`;
    INSERT INTO `sys_sequence` (`sequence_key`, `next_value`) VALUES ('USER_NO', v_next_user_no)
      ON DUPLICATE KEY UPDATE `next_value` = GREATEST(`next_value`, VALUES(`next_value`));

    -- 补齐其他明显的数据完整性约束。
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_permission' AND INDEX_NAME = 'uk_permission_code';
    IF v_count = 0 THEN
      ALTER TABLE `sys_permission` ADD UNIQUE INDEX `uk_permission_code` (`permission_code`);
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = 'fk_order_user';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order`
        ADD CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON DELETE RESTRICT ON UPDATE RESTRICT;
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND CONSTRAINT_NAME = 'chk_user_status';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD CONSTRAINT `chk_user_status` CHECK (`status` IN (0, 1));
    END IF;

    INSERT INTO `sys_schema_version` (`version`, `description`)
    VALUES ('20260713_v3_user_role_normalization', '人员编号、角色编码、单一角色来源、关联ID与完整性约束规范化');
  END IF;
END$$

CALL `migrate_v3_user_role_normalization`()$$
DROP PROCEDURE `migrate_v3_user_role_normalization`$$

DELIMITER ;
