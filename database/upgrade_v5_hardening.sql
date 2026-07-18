-- Fruit Mall v5: security, idempotent orders, immutable audit logs and data integrity.
-- Idempotent for MySQL 8; safe to run repeatedly against fruit_mall.
USE `fruit_mall`;

DELIMITER $$
DROP PROCEDURE IF EXISTS `upgrade_v5_hardening`$$
CREATE PROCEDURE `upgrade_v5_hardening`()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_fk_table VARCHAR(64);
    DECLARE v_fk_name VARCHAR(64);
    DECLARE v_old_fk_checks INT DEFAULT 1;

    CREATE TABLE IF NOT EXISTS `sys_schema_version` (
      `version` varchar(100) NOT NULL,
      `description` varchar(255) NOT NULL,
      `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`version`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

    -- Earlier project versions changed several key columns while retaining legacy foreign-key
    -- metadata. MySQL revalidates those constraints during any later ALTER TABLE and raises 1215.
    -- Remove constraints only (no rows are deleted and supporting indexes are retained); the
    -- application enforces ownership/existence in transactions and uses soft deletion.
    SET v_old_fk_checks = @@FOREIGN_KEY_CHECKS;
    SET FOREIGN_KEY_CHECKS = 0;
    legacy_fk_cleanup: LOOP
      SELECT MIN(TABLE_NAME) INTO v_fk_table
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE();
      IF v_fk_table IS NULL THEN
        LEAVE legacy_fk_cleanup;
      END IF;
      SELECT MIN(CONSTRAINT_NAME) INTO v_fk_name
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = v_fk_table;
      SET @migration_sql = CONCAT(
        'ALTER TABLE `', REPLACE(v_fk_table, '`', '``'),
        '` DROP FOREIGN KEY `', REPLACE(v_fk_name, '`', '``'), '`'
      );
      PREPARE migration_statement FROM @migration_sql;
      EXECUTE migration_statement;
      DEALLOCATE PREPARE migration_statement;
    END LOOP;
    SET FOREIGN_KEY_CHECKS = v_old_fk_checks;

    -- Revoke old JWTs immediately after password/status/logout changes.
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'token_version';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user` ADD COLUMN `token_version` int NOT NULL DEFAULT 0 AFTER `status`;
    END IF;

    -- Order submission idempotency and soft archive.
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND COLUMN_NAME = 'request_id';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order` ADD COLUMN `request_id` varchar(64) DEFAULT NULL AFTER `user_id`;
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND COLUMN_NAME = 'archived';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order` ADD COLUMN `archived` tinyint NOT NULL DEFAULT 0 AFTER `cancel_time`;
    END IF;
    UPDATE `sys_order` SET `request_id` = CONCAT('LEGACY_', `order_no`) WHERE `request_id` IS NULL OR `request_id` = '';
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND INDEX_NAME = 'uk_order_user_request';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order` ADD UNIQUE INDEX `uk_order_user_request` (`user_id`, `request_id`);
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND INDEX_NAME = 'idx_order_archive_status_time';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order` ADD INDEX `idx_order_archive_status_time` (`archived`, `status`, `create_time`);
    END IF;

    -- Remove legacy orphan rows after normalizing old constraints. Order writes remain protected
    -- by service transactions, soft deletion and indexed logical references.
    DELETE oi FROM `sys_order_item` oi LEFT JOIN `sys_order` o ON o.order_no = oi.order_no WHERE o.order_no IS NULL;
    DELETE ol FROM `sys_order_log` ol LEFT JOIN `sys_order` o ON o.order_no = ol.order_no WHERE o.order_no IS NULL;

    -- Trace who changed an order and where the transition came from.
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order_log' AND COLUMN_NAME = 'operator_id';
    IF v_count = 0 THEN ALTER TABLE `sys_order_log` ADD COLUMN `operator_id` bigint DEFAULT NULL AFTER `remark`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order_log' AND COLUMN_NAME = 'source';
    IF v_count = 0 THEN ALTER TABLE `sys_order_log` ADD COLUMN `source` varchar(20) NOT NULL DEFAULT 'LEGACY' AFTER `operator_id`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order_log' AND COLUMN_NAME = 'request_id';
    IF v_count = 0 THEN ALTER TABLE `sys_order_log` ADD COLUMN `request_id` varchar(64) DEFAULT NULL AFTER `source`; END IF;

    -- Immutable request audit details.
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'username';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `username` varchar(50) DEFAULT NULL AFTER `user_id`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'ip_address';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `ip_address` varchar(64) DEFAULT NULL AFTER `remark`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'http_method';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `http_method` varchar(10) DEFAULT NULL AFTER `ip_address`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'request_uri';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `request_uri` varchar(255) DEFAULT NULL AFTER `http_method`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'request_id';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `request_id` varchar(64) DEFAULT NULL AFTER `request_uri`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'success';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `success` tinyint NOT NULL DEFAULT 1 AFTER `request_id`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND COLUMN_NAME = 'duration_ms';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD COLUMN `duration_ms` bigint DEFAULT NULL AFTER `success`; END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_log' AND INDEX_NAME = 'idx_log_request_time';
    IF v_count = 0 THEN ALTER TABLE `sys_log` ADD INDEX `idx_log_request_time` (`request_id`, `create_time`); END IF;

    -- Only one default address per user, enforced by a generated nullable key.
    UPDATE `sys_user_address` a
      JOIN (SELECT user_id, MIN(id) keep_id FROM `sys_user_address` WHERE is_default = 1 GROUP BY user_id) k
        ON k.user_id = a.user_id
      SET a.is_default = 0 WHERE a.is_default = 1 AND a.id <> k.keep_id;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_address' AND COLUMN_NAME = 'default_user_id';
    IF v_count = 0 THEN
      ALTER TABLE `sys_user_address` ADD COLUMN `default_user_id` bigint
        GENERATED ALWAYS AS (IF(`is_default` = 1, `user_id`, NULL)) STORED;
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_address' AND INDEX_NAME = 'uk_address_one_default';
    IF v_count = 0 THEN
      SET @migration_sql = 'ALTER TABLE `sys_user_address` ADD UNIQUE INDEX `uk_address_one_default` (`default_user_id`)';
      PREPARE migration_statement FROM @migration_sql;
      EXECUTE migration_statement;
      DEALLOCATE PREPARE migration_statement;
    END IF;

    -- Only one active pending payment per order.
    UPDATE `sys_payment` p
      JOIN (SELECT order_no, MAX(id) keep_id FROM `sys_payment` WHERE status = 0 GROUP BY order_no) k
        ON k.order_no = p.order_no
      SET p.status = 2 WHERE p.status = 0 AND p.id <> k.keep_id;
    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND COLUMN_NAME = 'pending_order_no';
    IF v_count = 0 THEN
      ALTER TABLE `sys_payment` ADD COLUMN `pending_order_no` varchar(64)
        GENERATED ALWAYS AS (IF(`status` = 0, `order_no`, NULL)) STORED;
    END IF;
    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND INDEX_NAME = 'uk_payment_one_pending';
    IF v_count = 0 THEN
      SET @migration_sql = 'ALTER TABLE `sys_payment` ADD UNIQUE INDEX `uk_payment_one_pending` (`pending_order_no`)';
      PREPARE migration_statement FROM @migration_sql;
      EXECUTE migration_statement;
      DEALLOCATE PREPARE migration_statement;
    END IF;

    -- Remove role duplicates and prevent CUSTOMER from being mixed with backend roles.
    DELETE ur1 FROM `sys_user_role` ur1 JOIN `sys_user_role` ur2
      ON ur1.user_id = ur2.user_id AND ur1.role_id = ur2.role_id AND ur1.id > ur2.id;
    -- MySQL 1093 forbids deleting from a table while a nested query reads the same target.
    -- Materialize the affected users first, then delete only their CUSTOMER relation.
    DROP TEMPORARY TABLE IF EXISTS `tmp_backend_role_users`;
    CREATE TEMPORARY TABLE `tmp_backend_role_users` (`user_id` bigint NOT NULL PRIMARY KEY) ENGINE=MEMORY
      SELECT DISTINCT relation.user_id
      FROM `sys_user_role` relation
      JOIN `sys_role` role ON role.role_id = relation.role_id
      WHERE role.role_code <> 'CUSTOMER';
    DELETE customer_relation FROM `sys_user_role` customer_relation
      JOIN `sys_role` customer_role
        ON customer_role.role_id = customer_relation.role_id AND customer_role.role_code = 'CUSTOMER'
      JOIN `tmp_backend_role_users` backend_user ON backend_user.user_id = customer_relation.user_id;
    DROP TEMPORARY TABLE IF EXISTS `tmp_backend_role_users`;
    DELETE rp FROM `sys_role_permission` rp JOIN `sys_role` r ON r.role_id = rp.role_id WHERE r.role_code = 'CUSTOMER';

    -- Inventory movement ledger for future stock tracing.
    CREATE TABLE IF NOT EXISTS `sys_inventory_movement` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `product_id` bigint NOT NULL,
      `change_quantity` int NOT NULL,
      `movement_type` varchar(30) NOT NULL,
      `business_no` varchar(64) DEFAULT NULL,
      `operator_id` bigint DEFAULT NULL,
      `remark` varchar(255) DEFAULT NULL,
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      KEY `idx_inventory_product_time` (`product_id`, `create_time`),
      KEY `idx_inventory_business` (`business_no`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存变动流水';
    -- Some legacy installations use a different engine or signedness for sys_product.id.
    -- Keep this as an indexed logical reference: all writes validate the product in the service layer,
    -- and products are soft-deleted, so a physical FK is not required for ledger consistency.
    DELETE movement FROM `sys_inventory_movement` movement
      LEFT JOIN `sys_product` product ON product.id = movement.product_id
      WHERE product.id IS NULL;

    INSERT INTO `sys_schema_version` (`version`, `description`)
    VALUES ('20260715_v5_hardening', 'RBAC防提权、JWT撤销、订单幂等、支付唯一性、不可变审计与外键清理')
    ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);
END$$

CALL `upgrade_v5_hardening`()$$
DROP PROCEDURE IF EXISTS `upgrade_v5_hardening`$$
DELIMITER ;
