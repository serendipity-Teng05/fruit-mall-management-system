-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: fruit_mall
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `fruit_mall`
--

/*!40000 DROP DATABASE IF EXISTS `fruit_mall`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `fruit_mall` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `fruit_mall`;

--
-- Table structure for table `sys_log`
--

DROP TABLE IF EXISTS `sys_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID；未认证请求允许为空',
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作动作（如：新增订单、修改库存）',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作模块（订单/商品/用户）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `fk_sys_log_user` (`user_id`) USING BTREE,
  CONSTRAINT `fk_sys_log_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_log`
--

LOCK TABLES `sys_log` WRITE;
/*!40000 ALTER TABLE `sys_log` DISABLE KEYS */;
INSERT INTO `sys_log` VALUES (3,1,'登录系统','认证模块','管理员登录成功','2026-03-12 08:35:12'),(4,1,'查看数据概览','数据概览','进入首页查看统计数据','2026-03-12 08:36:01'),(5,1,'查询商品列表','商品管理','查看全部商品信息','2026-03-12 08:40:15'),(6,1,'新增商品','商品管理','新增商品：云南阳光玫瑰','2026-03-12 08:45:22'),(7,1,'修改商品','商品管理','修改商品库存：广西沃柑','2026-03-12 08:52:10'),(8,1,'查看订单列表','订单管理','查看近7天订单数据','2026-03-12 09:03:18'),(9,1,'查看订单详情','订单管理','查看订单：ORD20260312004','2026-03-12 09:05:33'),(10,1,'发货操作','订单管理','订单发货：ORD20260311002','2026-03-12 09:18:44'),(11,1,'查看角色列表','角色管理','查看系统角色数据','2026-03-12 09:28:19'),(12,1,'新增角色','角色管理','新增角色：采购员','2026-03-12 09:33:26'),(13,1,'分配权限','角色权限','为角色“管理员”分配全部权限','2026-03-12 09:42:05'),(14,1,'查看用户角色','用户角色分配','查看用户角色关联信息','2026-03-12 09:45:47'),(15,1,'分配用户角色','用户角色分配','为用户 staff1 分配员工角色','2026-03-12 09:48:03'),(16,1,'查看订单日志','订单日志','查看订单日志列表','2026-03-12 10:02:16'),(17,1,'查看系统日志','系统日志','查看系统日志列表','2026-03-12 10:05:55'),(18,3,'登录系统','认证模块','334478121@qq.com 登录成功','2026-03-11 14:12:44'),(19,3,'查看商品列表','商品管理','员工查看商品列表','2026-03-11 14:16:09'),(20,3,'查看订单列表','订单管理','员工查看订单管理页面','2026-03-11 14:21:38'),(21,3,'查看订单详情','订单管理','查看订单：ORD20260311004','2026-03-11 14:24:10'),(22,3,'退出登录','认证模块','用户主动退出系统','2026-03-11 14:30:22');
/*!40000 ALTER TABLE `sys_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_order`
--

DROP TABLE IF EXISTS `sys_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_order` (
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint DEFAULT NULL COMMENT '操作员ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` int DEFAULT '0' COMMENT '0待支付,1待发货,2待收货,3已完成,4已取消',
  `customer_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户姓名',
  `customer_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户电话',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '收货地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `payment_time` datetime DEFAULT NULL COMMENT '支付时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `receive_time` datetime DEFAULT NULL COMMENT '收货时间',
  PRIMARY KEY (`order_no`) USING BTREE,
  KEY `idx_order_user_time` (`user_id`,`create_time`),
  KEY `idx_order_status_time` (`status`,`create_time`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_order_amount` CHECK ((`total_amount` >= 0)),
  CONSTRAINT `chk_order_status` CHECK ((`status` in (0,1,2,3,4)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_order`
--

LOCK TABLES `sys_order` WRITE;
/*!40000 ALTER TABLE `sys_order` DISABLE KEYS */;
INSERT INTO `sys_order` VALUES ('ORD20260306001',1,49.18,1,'宋景川','13811110020','济南市历下区泉城路100号','2026-04-09 16:32:57',NULL,NULL,NULL),('ORD20260307001',1,57.00,2,'许嘉悦','13811110019','福州市鼓楼区五四路66号','2026-04-10 10:08:44',NULL,NULL,NULL),('ORD20260308001',1,74.37,3,'胡依诺','13811110018','郑州市金水区花园路88号','2026-04-11 12:15:26',NULL,NULL,NULL),('ORD20260308002',12,45.00,2,'迪丽热巴','13222222222','乌鲁木齐市天山区','2026-04-12 13:49:01',NULL,'2026-03-12 12:41:03',NULL),('ORD20260309001',1,57.45,1,'郭婉晴','13811110014','长沙市岳麓区麓谷大道77号','2026-04-12 08:36:45',NULL,NULL,NULL),('ORD20260309002',1,134.00,1,'邓浩然','13811110015','青岛市市南区香港中路56号','2026-04-12 11:27:18',NULL,NULL,NULL),('ORD20260309003',1,68.00,2,'罗欣怡','13811110016','厦门市思明区莲前西路118号','2026-04-12 14:58:39',NULL,NULL,NULL),('ORD20260309004',1,167.80,3,'高子辰','13811110017','合肥市蜀山区长江西路300号','2026-04-12 17:21:04',NULL,NULL,NULL),('ORD20260310001',1,50.00,0,'郑书涵','13811110010','苏州市工业园区星湖街218号','2026-04-13 09:15:33',NULL,NULL,NULL),('ORD20260310002',1,42.96,1,'谢俊凯','13811110011','西安市雁塔区科技路33号','2026-04-13 10:42:11',NULL,NULL,NULL),('ORD20260310003',1,323.00,2,'何梦瑶','13811110012','重庆市渝中区解放碑民权路66号','2026-04-13 15:18:52',NULL,NULL,NULL),('ORD20260310004',1,79.70,3,'彭宇航','13811110013','天津市南开区长江道120号','2026-04-13 18:06:27',NULL,NULL,NULL),('ORD20260311001',1,45.00,0,'陈若曦','13811110005','深圳市南山区科技园科苑路9号','2026-04-14 08:52:16',NULL,NULL,NULL),('ORD20260311002',1,209.94,1,'周子轩','13811110006','杭州市西湖区文三路199号','2026-04-14 10:24:55',NULL,NULL,NULL),('ORD20260311003',1,69.90,2,'刘佳宁','13811110007','成都市武侯区人民南路四段36号','2026-04-14 13:09:41',NULL,NULL,NULL),('ORD20260311004',1,256.00,3,'黄子墨','13811110008','武汉市洪山区珞喻路726号','2026-04-14 16:47:29',NULL,NULL,NULL),('ORD20260311005',1,59.90,3,'孙可欣','13811110009','南京市鼓楼区中山北路88号','2026-04-14 19:18:22',NULL,NULL,NULL),('ORD20260312001',1,74.75,1,'张雨涵','13811110001','上海市浦东新区张江路88号','2026-04-15 09:12:15','2026-07-13 19:52:35',NULL,NULL),('ORD20260312002',1,150.00,2,'李嘉豪','13811110002','上海市闵行区虹桥路166号','2026-04-15 10:05:22',NULL,'2026-04-15 14:17:41',NULL),('ORD20260312003',1,154.70,2,'王思琪','13811110003','北京市海淀区中关村南大街15号','2026-04-15 11:36:48',NULL,NULL,NULL),('ORD20260312004',1,117.66,3,'赵一鸣','13811110004','广州市天河区体育西路21号','2026-04-15 14:18:03',NULL,NULL,NULL);
/*!40000 ALTER TABLE `sys_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_order_item`
--

DROP TABLE IF EXISTS `sys_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联订单号',
  `product_id` bigint DEFAULT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '商品名称快照',
  `price` decimal(10,2) DEFAULT NULL COMMENT '购买单价',
  `count` int DEFAULT NULL COMMENT '购买数量',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_item_order_no` (`order_no`),
  KEY `idx_order_item_product_id` (`product_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_no`) REFERENCES `sys_order` (`order_no`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `sys_product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_order_item_count` CHECK ((`count` > 0)),
  CONSTRAINT `chk_order_item_price` CHECK ((`price` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单详情表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_order_item`
--

LOCK TABLES `sys_order_item` WRITE;
/*!40000 ALTER TABLE `sys_order_item` DISABLE KEYS */;
INSERT INTO `sys_order_item` VALUES (1,'ORD20260206001',15,'秘鲁进口蓝莓',19.90,1),(2,'ORD20260206001',17,'泰国5A山竹',29.90,1),(3,'ORD20260206002',27,'晴王葡萄',58.00,2),(4,'ORD20260206002',14,'广西沃柑',6.99,10),(5,'ORD20260205001',22,'墨西哥牛油果',8.90,10),(6,'ORD20260205002',26,'阳山水蜜桃',128.00,2),(7,'ORD20260204005',28,'人参果',12.50,5),(8,'ORD20260204006',25,'山东沾化冬枣',15.00,3),(9,'ORD20260312001',15,'秘鲁进口蓝莓',19.90,2),(10,'ORD20260312001',14,'广西沃柑',6.99,5),(11,'ORD20260312002',27,'晴王葡萄',58.00,2),(12,'ORD20260312002',18,'陕西洛川苹果',8.50,4),(13,'ORD20260312003',26,'阳山水蜜桃',128.00,1),(14,'ORD20260312003',22,'墨西哥牛油果',8.90,3),(15,'ORD20260312004',17,'泰国5A山竹',29.90,3),(16,'ORD20260312004',14,'广西沃柑',6.99,4),(17,'ORD20260311001',25,'山东沾化冬枣',15.00,3),(18,'ORD20260311002',7,'泰国金枕榴莲',168.00,1),(19,'ORD20260311002',14,'广西沃柑',6.99,6),(20,'ORD20260311003',30,'红肉菠萝蜜',25.00,2),(21,'ORD20260311003',15,'秘鲁进口蓝莓',19.90,1),(22,'ORD20260311004',26,'阳山水蜜桃',128.00,2),(23,'ORD20260311005',12,'精选混合果切',15.00,2),(24,'ORD20260311005',2,'香甜大香蕉',6.99,10),(25,'ORD20260310001',28,'人参果',12.50,4),(26,'ORD20260310002',4,'海南西瓜',1.50,10),(27,'ORD20260310002',14,'广西沃柑',6.99,4),(28,'ORD20260310003',8,'智利车厘子JJJ',299.00,1),(29,'ORD20260310003',24,'福建琯溪蜜柚',12.00,2),(30,'ORD20260310004',17,'泰国5A山竹',29.90,2),(31,'ORD20260310004',15,'秘鲁进口蓝莓',19.90,1),(32,'ORD20260309001',14,'广西沃柑',6.99,5),(33,'ORD20260309001',31,'甘肃花牛苹果',7.50,3),(34,'ORD20260309002',27,'晴王葡萄',58.00,2),(35,'ORD20260309002',11,'云南阳光玫瑰',18.00,1),(36,'ORD20260309003',23,'菲律宾凤梨',28.00,2),(37,'ORD20260309003',24,'福建琯溪蜜柚',12.00,1),(38,'ORD20260309004',26,'阳山水蜜桃',128.00,1),(39,'ORD20260309004',15,'秘鲁进口蓝莓',19.90,2),(40,'ORD20260308001',22,'墨西哥牛油果',8.90,6),(41,'ORD20260308001',14,'广西沃柑',6.99,3),(42,'ORD20260307001',25,'山东沾化冬枣',15.00,3),(43,'ORD20260307001',29,'羊角蜜瓜',6.00,2),(44,'ORD20260306001',3,'巨峰葡萄',8.80,4),(45,'ORD20260306001',1,'红富士苹果',6.99,2);
/*!40000 ALTER TABLE `sys_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_order_log`
--

DROP TABLE IF EXISTS `sys_order_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_order_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `status_before` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '变更前状态',
  `status_after` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '变更后状态',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `change_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `fk_order_log_order` (`order_no`) USING BTREE,
  CONSTRAINT `fk_order_log_order` FOREIGN KEY (`order_no`) REFERENCES `sys_order` (`order_no`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='订单状态日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_order_log`
--

LOCK TABLES `sys_order_log` WRITE;
/*!40000 ALTER TABLE `sys_order_log` DISABLE KEYS */;
INSERT INTO `sys_order_log` VALUES (2,'ORD20260308002','待处理','已支付','订单支付成功','2026-03-09 13:51:01'),(3,'ORD20260312001','新建','待支付','用户提交订单，等待支付','2026-03-12 09:12:15'),(4,'ORD20260312002','新建','待支付','用户提交订单，等待支付','2026-03-12 10:05:22'),(5,'ORD20260312002','待支付','待发货','支付成功，等待商家发货','2026-03-12 10:08:10'),(6,'ORD20260312003','新建','待支付','用户提交订单，等待支付','2026-03-12 11:36:48'),(7,'ORD20260312003','待支付','待发货','支付成功，等待商家发货','2026-03-12 11:40:02'),(8,'ORD20260312003','待发货','待收货','商家已发货，等待用户收货','2026-03-12 13:15:30'),(9,'ORD20260312004','新建','待支付','用户提交订单，等待支付','2026-03-12 14:18:03'),(10,'ORD20260312004','待支付','待发货','支付成功，等待商家发货','2026-03-12 14:21:18'),(11,'ORD20260312004','待发货','待收货','商家已发货，等待用户收货','2026-03-12 16:02:00'),(12,'ORD20260312004','待收货','已完成','用户确认收货，订单完成','2026-03-12 20:36:14'),(13,'ORD20260311001','新建','待支付','用户提交订单，等待支付','2026-03-11 08:52:16'),(14,'ORD20260311002','新建','待支付','用户提交订单，等待支付','2026-03-11 10:24:55'),(15,'ORD20260311002','待支付','待发货','支付成功，等待商家发货','2026-03-11 10:29:40'),(16,'ORD20260311003','新建','待支付','用户提交订单，等待支付','2026-03-11 13:09:41'),(17,'ORD20260311003','待支付','待发货','支付成功，等待商家发货','2026-03-11 13:15:09'),(18,'ORD20260311003','待发货','待收货','商家已发货，等待用户收货','2026-03-11 17:42:30'),(19,'ORD20260311004','新建','待支付','用户提交订单，等待支付','2026-03-11 16:47:29'),(20,'ORD20260311004','待支付','待发货','支付成功，等待商家发货','2026-03-11 16:50:00'),(21,'ORD20260311004','待发货','待收货','商家已发货，等待用户收货','2026-03-11 18:20:00'),(22,'ORD20260311004','待收货','已完成','用户确认收货，订单完成','2026-03-11 21:10:45'),(23,'ORD20260311005','新建','待支付','用户提交订单，等待支付','2026-03-11 19:18:22'),(24,'ORD20260311005','待支付','待发货','支付成功，等待商家发货','2026-03-11 19:22:10'),(25,'ORD20260311005','待发货','待收货','商家已发货，等待用户收货','2026-03-11 20:30:00'),(26,'ORD20260311005','待收货','已完成','用户确认收货，订单完成','2026-03-11 22:45:06'),(27,'ORD20260310001','新建','待支付','用户提交订单，等待支付','2026-03-10 09:15:33'),(28,'ORD20260310002','新建','待支付','用户提交订单，等待支付','2026-03-10 10:42:11'),(29,'ORD20260310002','待支付','待发货','支付成功，等待商家发货','2026-03-10 10:48:26'),(30,'ORD20260310003','新建','待支付','用户提交订单，等待支付','2026-03-10 15:18:52'),(31,'ORD20260310003','待支付','待发货','支付成功，等待商家发货','2026-03-10 15:22:11'),(32,'ORD20260310003','待发货','待收货','商家已发货，等待用户收货','2026-03-10 18:40:00'),(33,'ORD20260310004','新建','待支付','用户提交订单，等待支付','2026-03-10 18:06:27'),(34,'ORD20260310004','待支付','待发货','支付成功，等待商家发货','2026-03-10 18:09:00'),(35,'ORD20260310004','待发货','待收货','商家已发货，等待用户收货','2026-03-10 20:00:00'),(36,'ORD20260310004','待收货','已完成','用户确认收货，订单完成','2026-03-10 22:31:40'),(37,'ORD20260309001','新建','待支付','用户提交订单，等待支付','2026-03-09 08:36:45'),(38,'ORD20260309001','待支付','待发货','支付成功，等待商家发货','2026-03-09 08:41:15'),(39,'ORD20260309002','新建','待支付','用户提交订单，等待支付','2026-03-09 11:27:18'),(40,'ORD20260309002','待支付','待发货','支付成功，等待商家发货','2026-03-09 11:32:20'),(41,'ORD20260309003','新建','待支付','用户提交订单，等待支付','2026-03-09 14:58:39'),(42,'ORD20260309003','待支付','待发货','支付成功，等待商家发货','2026-03-09 15:05:08'),(43,'ORD20260309003','待发货','待收货','商家已发货，等待用户收货','2026-03-09 18:15:00'),(44,'ORD20260309004','新建','待支付','用户提交订单，等待支付','2026-03-09 17:21:04'),(45,'ORD20260309004','待支付','待发货','支付成功，等待商家发货','2026-03-09 17:26:10'),(46,'ORD20260309004','待发货','待收货','商家已发货，等待用户收货','2026-03-09 19:40:00'),(47,'ORD20260309004','待收货','已完成','用户确认收货，订单完成','2026-03-09 22:18:35'),(48,'ORD20260308001','新建','待支付','用户提交订单，等待支付','2026-03-08 12:15:26'),(49,'ORD20260308001','待支付','待发货','支付成功，等待商家发货','2026-03-08 12:20:14'),(50,'ORD20260308001','待发货','待收货','商家已发货，等待用户收货','2026-03-08 15:03:20'),(51,'ORD20260308001','待收货','已完成','用户确认收货，订单完成','2026-03-08 19:05:00'),(52,'ORD20260307001','新建','待支付','用户提交订单，等待支付','2026-03-07 10:08:44'),(53,'ORD20260307001','待支付','待发货','支付成功，等待商家发货','2026-03-07 10:12:06'),(54,'ORD20260307001','待发货','待收货','商家已发货，等待用户收货','2026-03-07 13:30:00'),(55,'ORD20260306001','新建','待支付','用户提交订单，等待支付','2026-03-06 16:32:57'),(56,'ORD20260306001','待支付','待发货','支付成功，等待商家发货','2026-03-06 16:38:09');
/*!40000 ALTER TABLE `sys_order_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_payment`
--

DROP TABLE IF EXISTS `sys_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_no` varchar(40) NOT NULL COMMENT '系统支付单号',
  `order_no` varchar(50) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '支付用户',
  `channel` varchar(20) NOT NULL DEFAULT 'DEMO' COMMENT '支付渠道',
  `amount` decimal(12,2) NOT NULL COMMENT '支付金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待支付 1已支付 2已关闭 3失败',
  `provider_trade_no` varchar(80) DEFAULT NULL COMMENT '第三方交易号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paid_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_payment_order` (`order_no`),
  KEY `idx_payment_user_time` (`user_id`,`create_time`),
  KEY `idx_payment_status_time` (`status`,`create_time`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_no`) REFERENCES `sys_order` (`order_no`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_payment_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `chk_payment_status` CHECK ((`status` in (0,1,2,3)))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_payment`
--

LOCK TABLES `sys_payment` WRITE;
/*!40000 ALTER TABLE `sys_payment` DISABLE KEYS */;
INSERT INTO `sys_payment` VALUES (1,'PAY202607131952358272DA1C','ORD20260312001',1,'DEMO',74.75,1,'DEMO-B5E5D4DD7923420E82E212B08FB19BE7','2026-07-13 19:52:35','2026-07-13 19:52:35');
/*!40000 ALTER TABLE `sys_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `permission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `permission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码（接口/菜单）',
  `permission_type` enum('MENU','API') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'API' COMMENT '权限类型',
  `parent_id` bigint DEFAULT NULL COMMENT '父级权限ID，用于菜单层级',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`permission_id`) USING BTREE,
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,'商品管理','PRODUCT_MANAGE','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(2,'订单管理','ORDER_MANAGE','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(3,'人员管理','USER_MANAGE','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(4,'角色管理','ROLE_MANAGE','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(5,'角色权限','ROLE_PERMISSION','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(6,'用户角色分配','USER_ROLE_ASSIGN','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(7,'订单日志','ORDER_LOG','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(8,'系统日志','SYS_LOG','MENU',NULL,'2026-03-12 16:35:30','2026-03-12 16:35:30'),(9,'支付流水','PAYMENT_MANAGE','MENU',NULL,'2026-07-13 19:33:50','2026-07-13 19:33:50');
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_product`
--

DROP TABLE IF EXISTS `sys_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分类',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `stock` int DEFAULT '0' COMMENT '库存',
  `unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '斤' COMMENT '单位',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图片URL',
  `status` int DEFAULT '1' COMMENT '状态: 1上架 0下架',
  `version` int DEFAULT '1' COMMENT '乐观锁版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `stock_threshold` int DEFAULT '10' COMMENT '库存预警阈值',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_product_status_category` (`status`,`category`),
  KEY `idx_product_stock` (`stock`),
  CONSTRAINT `chk_product_price` CHECK ((`price` >= 0)),
  CONSTRAINT `chk_product_status` CHECK ((`status` in (0,1))),
  CONSTRAINT `chk_product_stock` CHECK ((`stock` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='商品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_product`
--

LOCK TABLES `sys_product` WRITE;
/*!40000 ALTER TABLE `sys_product` DISABLE KEYS */;
INSERT INTO `sys_product` VALUES (1,'红富士苹果','水果',6.98,101,'斤','/images/apple.jpg',1,1,'2026-02-04 22:07:25',10),(2,'香甜大香蕉','水果',2.99,200,'斤','https://images.unsplash.com/photo-1528825871115-3581a5387919?auto=format&fit=crop&w=300&h=300',1,1,'2026-02-04 22:07:25',10),(3,'巨峰葡萄','水果',8.80,50,'斤','/images/grape.jpg',1,1,'2026-02-04 22:07:25',10),(4,'海南西瓜','水果',1.50,300,'个','/images/watermelon.jpg',1,1,'2026-02-04 22:07:25',10),(5,'红心火龙果','热带水果',8.00,4,'斤','/images/pitaya.jpg',1,1,'2026-02-04 22:07:25',10),(6,'丹东奶油草莓','热销水果',35.00,5,'盒','https://images.unsplash.com/photo-1518635017498-87f514b751ba?auto=format&fit=crop&w=300&h=300',1,1,'2026-02-04 22:07:25',10),(7,'泰国金枕榴莲','进口水果',168.00,0,'个','/images/durian.jpg',1,1,'2026-02-04 22:07:25',10),(8,'智利车厘子JJJ','进口水果',299.00,50,'箱','/images/cherry.jpg',1,1,'2026-02-04 22:07:25',10),(9,'新疆阿克苏苹果','时令水果',6.50,120,'斤','/images/apple.jpg',1,1,'2026-02-04 22:07:25',10),(10,'海南贵妃芒','热销水果',12.80,8,'斤','https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=300&h=300',1,1,'2026-02-04 22:07:25',10),(11,'云南阳光玫瑰','时令水果',18.00,200,'斤','/images/rose.jpg',1,1,'2026-02-04 22:07:25',10),(12,'精选混合果切','鲜果切',15.00,3,'盒','https://images.unsplash.com/photo-1490474418585-ba9bad8fd0ea?auto=format&fit=crop&w=300&h=300',1,1,'2026-02-04 22:07:25',10),(13,'黄金百香果','热带水果',15.80,50,'斤','/images/passion fruit.jpg',1,1,'2026-02-05 12:09:42',10),(14,'广西沃柑','时令水果',6.99,200,'斤','/images/tangerine.jpg',1,1,'2026-02-05 12:09:42',10),(15,'秘鲁进口蓝莓','进口水果',19.90,100,'盒','/images/blueberry.jpg',1,1,'2026-02-05 12:09:42',10),(16,'绿宝石甜瓜','水果',25.00,30,'个','/images/melon.jpg',1,1,'2026-02-05 12:09:42',10),(17,'泰国5A山竹','进口水果',29.90,60,'斤','/images/Mangosteen.jpg',1,1,'2026-02-05 12:09:42',10),(18,'陕西洛川苹果','水果',8.50,150,'斤','/images/apple.jpg',1,1,'2026-02-05 12:09:42',10),(19,'库尔勒香梨','水果',9.90,120,'斤','/images/pear.jpg',1,1,'2026-02-05 12:09:42',10),(20,'突尼斯软籽石榴','时令水果',18.00,40,'斤','/images/Pomegranate.jpg',1,1,'2026-02-05 12:09:42',10),(21,'越南白肉火龙果','进口水果',6.50,80,'斤','/images/dragon.jpg',1,1,'2026-02-05 12:09:42',10),(22,'墨西哥牛油果','进口水果',8.90,50,'个','/images/avocado.jpg',1,1,'2026-02-05 12:09:42',10),(23,'菲律宾凤梨','进口水果',28.00,40,'个','/images/pineapple.jpg',1,1,'2026-02-05 12:09:42',10),(24,'福建琯溪蜜柚','时令水果',12.00,30,'个','/images/pomelo.jpg',1,1,'2026-02-05 12:09:42',10),(25,'山东沾化冬枣','时令水果',15.00,60,'斤','/images/jujube.jpg',1,1,'2026-02-05 12:09:42',10),(26,'阳山水蜜桃','热销水果',128.00,0,'盒','/images/peach.jpg',1,1,'2026-02-05 12:09:42',10),(27,'晴王葡萄','热销水果',58.00,20,'斤','/images/grape.jpg',1,1,'2026-02-05 12:09:42',10),(28,'人参果','水果',12.50,50,'斤','/images/Ginseng fruit.jpg',1,1,'2026-02-05 12:09:42',10),(29,'羊角蜜瓜','水果',6.00,80,'斤','/images/Honeydew melon.jpg',1,1,'2026-02-05 12:09:42',10),(30,'红肉菠萝蜜','热带水果',25.00,15,'盒','/images/jackfruit.jpg',1,1,'2026-02-05 12:09:42',10),(31,'甘肃花牛苹果','水果',7.50,100,'斤','/images/apple.jpg',1,1,'2026-02-05 12:09:42',10),(32,'攀枝花凯特芒','热销水果',9.90,70,'斤','https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=300&h=300',1,1,'2026-02-05 12:09:42',10);
/*!40000 ALTER TABLE `sys_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` varchar(50) NOT NULL COMMENT '稳定角色编码',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE KEY `uk_role_code` (`role_code`),
  UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'ADMIN','管理员','系统管理员','2026-03-08 16:54:32','2026-07-13 20:21:23'),(2,'CUSTOMER','普通用户','普通用户','2026-03-08 16:54:32','2026-07-13 20:21:23'),(6,'STAFF','普通员工','普通员工','2026-03-30 21:48:06','2026-07-13 20:21:23');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_role_permission` (`role_id`,`permission_id`) USING BTREE,
  KEY `fk_role_permission_permission` (`permission_id`) USING BTREE,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`permission_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (1,1,1),(2,1,2),(3,1,3),(4,1,4),(5,1,5),(6,1,6),(7,1,7),(8,1,8),(9,1,9),(10,2,2),(11,2,7),(12,6,1),(13,6,2),(14,6,7);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_schema_version`
--

DROP TABLE IF EXISTS `sys_schema_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_schema_version` (
  `version` varchar(100) NOT NULL,
  `description` varchar(255) NOT NULL,
  `installed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据库升级记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_schema_version`
--

LOCK TABLES `sys_schema_version` WRITE;
/*!40000 ALTER TABLE `sys_schema_version` DISABLE KEYS */;
INSERT INTO `sys_schema_version` VALUES ('20260713_v3_user_role_normalization','人员编号、角色编码、单一角色来源、关联ID与完整性约束规范化','2026-07-13 20:21:25');
/*!40000 ALTER TABLE `sys_schema_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_sequence`
--

DROP TABLE IF EXISTS `sys_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_sequence` (
  `sequence_key` varchar(50) NOT NULL,
  `next_value` bigint NOT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sequence_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务编号序列表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_sequence`
--

LOCK TABLES `sys_sequence` WRITE;
/*!40000 ALTER TABLE `sys_sequence` DISABLE KEYS */;
INSERT INTO `sys_sequence` VALUES ('USER_NO',7,'2026-07-13 20:21:25');
/*!40000 ALTER TABLE `sys_sequence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_no` varchar(20) NOT NULL COMMENT '人员编号',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用，0停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_status` (`status`,`user_no`),
  CONSTRAINT `chk_user_status` CHECK ((`status` in (0,1)))
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'EMP000001','admin','$2a$10$XiaDCOJu4gUV/QP.CwftWuIvyC1gtX0sKDhpz1tu2nI439QBarx7i','超级管理员','13800138000',1,'2026-01-28 17:44:58'),(3,'EMP000002','334478121@qq.com','123456','小邓','19318255405',1,'2026-01-30 17:26:37'),(6,'EMP000003','3516932719@qq.com','123456','小刘','18965985621',1,'2026-02-25 18:52:42'),(7,'EMP000006','1234964655','123456','小蒋','4968446655',1,'2026-03-30 21:54:28'),(11,'EMP000004','staff1','123456','员工一','13800138001',1,'2026-03-08 17:04:44'),(12,'EMP000005','staff2','123456','员工二','13800138002',1,'2026-03-08 17:04:44');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_user_role` (`user_id`,`role_id`) USING BTREE,
  KEY `fk_user_role_role` (`role_id`) USING BTREE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`role_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1,1,'2026-03-12 15:27:24'),(2,3,1,'2026-03-12 15:27:28'),(3,6,2,'2026-03-30 21:54:15'),(4,7,6,'2026-03-30 21:54:28'),(6,12,6,'2026-03-30 21:54:15'),(9,11,6,'2026-07-13 20:43:36');
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'fruit_mall'
--

-- v4 customer mall integration (kept at the end so legacy dump rows remain import-compatible)
ALTER TABLE `sys_order`
  ADD COLUMN `expire_time` datetime DEFAULT NULL COMMENT '待支付失效时间' AFTER `receive_time`,
  ADD COLUMN `cancel_time` datetime DEFAULT NULL COMMENT '取消时间' AFTER `expire_time`;

ALTER TABLE `sys_product`
  ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT '商城商品介绍' AFTER `image`,
  ADD COLUMN `origin` varchar(100) DEFAULT NULL COMMENT '产地' AFTER `description`,
  ADD COLUMN `sales_count` int NOT NULL DEFAULT 0 COMMENT '累计销量' AFTER `origin`,
  ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`;

ALTER TABLE `sys_order_item`
  ADD COLUMN `product_image` varchar(255) DEFAULT NULL COMMENT '商品图片快照' AFTER `product_name`,
  ADD COLUMN `product_unit` varchar(20) DEFAULT NULL COMMENT '商品单位快照' AFTER `product_image`;

UPDATE `sys_order_item` oi
JOIN `sys_product` p ON p.id = oi.product_id
SET oi.product_image = p.image, oi.product_unit = p.unit;

UPDATE `sys_product` p
SET p.sales_count = COALESCE((
  SELECT SUM(oi.count)
  FROM `sys_order_item` oi
  JOIN `sys_order` o ON o.order_no = oi.order_no
  WHERE oi.product_id = p.id AND o.status IN (1,2,3)
), 0),
p.origin = CASE
  WHEN p.name LIKE '%海南%' THEN '海南'
  WHEN p.name LIKE '%广西%' THEN '广西'
  WHEN p.name LIKE '%云南%' THEN '云南'
  WHEN p.name LIKE '%陕西%' OR p.name LIKE '%洛川%' THEN '陕西'
  WHEN p.name LIKE '%新疆%' OR p.name LIKE '%库尔勒%' THEN '新疆'
  WHEN p.name LIKE '%福建%' THEN '福建'
  WHEN p.name LIKE '%泰国%' THEN '泰国'
  WHEN p.name LIKE '%秘鲁%' THEN '秘鲁'
  WHEN p.name LIKE '%智利%' THEN '智利'
  ELSE '源头产区' END,
p.description = CONCAT('当季严选', p.name, '，产地直采，新鲜发货。');

ALTER TABLE `sys_payment`
  DROP INDEX `uk_payment_order`,
  ADD INDEX `idx_payment_order_time` (`order_no`,`create_time`),
  ADD UNIQUE INDEX `uk_payment_channel_trade` (`channel`,`provider_trade_no`);

CREATE TABLE `sys_user_address` (
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
  KEY `idx_address_user_default` (`user_id`,`is_default`,`update_time`),
  CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_address_default` CHECK (`is_default` IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址';

INSERT INTO `sys_user_address`
  (`user_id`,`recipient_name`,`phone`,`province`,`city`,`district`,`detail_address`,`is_default`)
VALUES (6,'小刘','18965985621','福建省','泉州市','丰泽区','东海大街 88 号',1);

DELETE rp FROM `sys_role_permission` rp
JOIN `sys_role` r ON r.role_id = rp.role_id
WHERE r.role_code = 'CUSTOMER';

INSERT INTO `sys_schema_version` (`version`,`description`)
VALUES ('20260713_v4_mall_integration','顾客商城、收货地址、订单归属、支付多次尝试与接口权限');

--
-- Dumping routines for database 'fruit_mall'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-13 20:47:45
