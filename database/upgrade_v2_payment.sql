-- 水果商城管理系统 v2：支付模块增量脚本（MySQL 8）
-- 执行前请备份 fruit_mall 数据库。

-- 旧数据未记录操作用户时归属到 admin，保证支付所有权校验可用。
UPDATE sys_order
SET user_id = (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1)
WHERE user_id IS NULL;

CREATE TABLE IF NOT EXISTS sys_payment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    payment_no VARCHAR(40) NOT NULL COMMENT '系统支付单号',
    order_no VARCHAR(40) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '支付用户',
    channel VARCHAR(20) NOT NULL COMMENT '支付渠道：DEMO/ALIPAY/WECHAT',
    amount DECIMAL(12, 2) NOT NULL COMMENT '支付金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已关闭 3失败',
    provider_trade_no VARCHAR(80) NULL COMMENT '第三方交易号',
    create_time DATETIME NOT NULL,
    paid_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_payment_order (order_no),
    KEY idx_payment_user_time (user_id, create_time),
    KEY idx_payment_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- 补充管理端“支付流水”菜单权限，并默认授予管理员角色（role_id=1）。
INSERT INTO sys_permission
    (permission_name, permission_code, permission_type, parent_id, create_time, update_time)
SELECT '支付流水', 'PAYMENT_MANAGE', 'MENU', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE permission_code = 'PAYMENT_MANAGE'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, permission_id
FROM sys_permission p
WHERE p.permission_code = 'PAYMENT_MANAGE'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 1 AND rp.permission_id = p.permission_id
  );

CREATE TABLE IF NOT EXISTS `sys_schema_version` (
  `version` varchar(100) NOT NULL,
  `description` varchar(255) NOT NULL,
  `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `sys_schema_version` (`version`, `description`)
VALUES ('20260712_v2_payment', '支付流水、支付权限与基础索引')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- 建议确认现有数据没有重复后，为订单号添加唯一索引：
-- ALTER TABLE sys_order ADD UNIQUE KEY uk_order_no (order_no);
-- 建议为订单明细查询添加索引：
-- ALTER TABLE sys_order_item ADD KEY idx_order_item_order_no (order_no);
