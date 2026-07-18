-- Fruit Mall v4: customer storefront, addresses, safer order/payment model.
-- Idempotent for MySQL 8; run against the existing fruit_mall database.
USE `fruit_mall`;

DELIMITER $$
DROP PROCEDURE IF EXISTS `upgrade_v4_mall_integration`$$
CREATE PROCEDURE `upgrade_v4_mall_integration`()
BEGIN
    DECLARE v_count INT DEFAULT 0;

    CREATE TABLE IF NOT EXISTS `sys_schema_version` (
      `version` varchar(100) NOT NULL,
      `description` varchar(255) NOT NULL,
      `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`version`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

    CREATE TABLE IF NOT EXISTS `sys_user_address` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `user_id` bigint NOT NULL,
      `recipient_name` varchar(50) NOT NULL,
      `phone` varchar(20) NOT NULL,
      `province` varchar(50) NOT NULL,
      `city` varchar(50) NOT NULL,
      `district` varchar(50) DEFAULT NULL,
      `detail_address` varchar(255) NOT NULL,
      `is_default` tinyint NOT NULL DEFAULT 0,
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      KEY `idx_address_user_default` (`user_id`, `is_default`, `update_time`),
      CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
      CONSTRAINT `chk_address_default` CHECK (`is_default` IN (0, 1))
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址';

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_product' AND COLUMN_NAME = 'description';
    IF v_count = 0 THEN
      ALTER TABLE `sys_product` ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT '商城商品介绍' AFTER `image`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_product' AND COLUMN_NAME = 'origin';
    IF v_count = 0 THEN
      ALTER TABLE `sys_product` ADD COLUMN `origin` varchar(100) DEFAULT NULL COMMENT '产地' AFTER `description`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_product' AND COLUMN_NAME = 'sales_count';
    IF v_count = 0 THEN
      ALTER TABLE `sys_product` ADD COLUMN `sales_count` int NOT NULL DEFAULT 0 COMMENT '累计销量' AFTER `origin`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_product' AND COLUMN_NAME = 'update_time';
    IF v_count = 0 THEN
      ALTER TABLE `sys_product` ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND COLUMN_NAME = 'expire_time';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order` ADD COLUMN `expire_time` datetime DEFAULT NULL COMMENT '待支付失效时间' AFTER `receive_time`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order' AND COLUMN_NAME = 'cancel_time';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order` ADD COLUMN `cancel_time` datetime DEFAULT NULL COMMENT '取消时间' AFTER `expire_time`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order_item' AND COLUMN_NAME = 'product_image';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order_item` ADD COLUMN `product_image` varchar(255) DEFAULT NULL COMMENT '商品图片快照' AFTER `product_name`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_order_item' AND COLUMN_NAME = 'product_unit';
    IF v_count = 0 THEN
      ALTER TABLE `sys_order_item` ADD COLUMN `product_unit` varchar(20) DEFAULT NULL COMMENT '商品单位快照' AFTER `product_image`;
    END IF;

    UPDATE `sys_order_item` oi
    JOIN `sys_product` p ON p.id = oi.product_id
    SET oi.product_image = COALESCE(oi.product_image, p.image),
        oi.product_unit = COALESCE(oi.product_unit, p.unit);

    UPDATE `sys_product` p
    SET p.sales_count = COALESCE((
      SELECT SUM(oi.count)
      FROM `sys_order_item` oi
      JOIN `sys_order` o ON o.order_no = oi.order_no
      WHERE oi.product_id = p.id AND o.status IN (1, 2, 3)
    ), 0);

    UPDATE `sys_product`
    SET `origin` = CASE
      WHEN name LIKE '%烟台%' OR name LIKE '%山东%' THEN '山东'
      WHEN name LIKE '%海南%' THEN '海南'
      WHEN name LIKE '%广西%' THEN '广西'
      WHEN name LIKE '%云南%' THEN '云南'
      WHEN name LIKE '%陕西%' OR name LIKE '%洛川%' THEN '陕西'
      WHEN name LIKE '%新疆%' OR name LIKE '%库尔勒%' THEN '新疆'
      WHEN name LIKE '%福建%' THEN '福建'
      WHEN name LIKE '%泰国%' THEN '泰国'
      WHEN name LIKE '%秘鲁%' THEN '秘鲁'
      WHEN name LIKE '%智利%' THEN '智利'
      ELSE COALESCE(origin, '源头产区') END,
        `description` = COALESCE(description, CONCAT('当季严选', name, '，产地直采，新鲜发货。'));

    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND INDEX_NAME = 'uk_payment_order';
    IF v_count > 0 THEN
      ALTER TABLE `sys_payment` DROP INDEX `uk_payment_order`;
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND INDEX_NAME = 'idx_payment_order_time';
    IF v_count = 0 THEN
      ALTER TABLE `sys_payment` ADD INDEX `idx_payment_order_time` (`order_no`, `create_time`);
    END IF;

    SELECT COUNT(*) INTO v_count FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_payment' AND INDEX_NAME = 'uk_payment_channel_trade';
    IF v_count = 0 THEN
      ALTER TABLE `sys_payment` ADD UNIQUE INDEX `uk_payment_channel_trade` (`channel`, `provider_trade_no`);
    END IF;

    -- Customer accounts must never inherit admin order/log permissions.
    DELETE rp FROM `sys_role_permission` rp
    JOIN `sys_role` r ON r.role_id = rp.role_id
    JOIN `sys_permission` p ON p.permission_id = rp.permission_id
    WHERE r.role_code = 'CUSTOMER'
      AND p.permission_code IN ('PRODUCT_MANAGE', 'ORDER_MANAGE', 'PAYMENT_MANAGE',
                                'USER_MANAGE', 'ROLE_MANAGE', 'ROLE_PERMISSION',
                                'USER_ROLE_ASSIGN', 'ORDER_LOG', 'SYS_LOG');

    INSERT INTO `sys_user_address`
      (`user_id`, `recipient_name`, `phone`, `province`, `city`, `district`, `detail_address`, `is_default`)
    SELECT u.id, COALESCE(u.real_name, '顾客'), u.phone, '福建省', '泉州市', '丰泽区', '东海大街 88 号', 1
    FROM `sys_user` u
    JOIN `sys_user_role` ur ON ur.user_id = u.id
    JOIN `sys_role` r ON r.role_id = ur.role_id AND r.role_code = 'CUSTOMER'
    WHERE u.phone IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM `sys_user_address` a WHERE a.user_id = u.id)
    LIMIT 1;

    INSERT INTO `sys_schema_version` (`version`, `description`)
    VALUES ('20260713_v4_mall_integration', '顾客商城、收货地址、订单归属、支付多次尝试与接口权限')
    ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);
END$$

CALL `upgrade_v4_mall_integration`()$$
DROP PROCEDURE IF EXISTS `upgrade_v4_mall_integration`$$
DELIMITER ;
