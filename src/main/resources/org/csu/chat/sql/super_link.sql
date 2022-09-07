/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 50735
 Source Host           : localhost:3306
 Source Schema         : super_link

 Target Server Type    : MySQL
 Target Server Version : 50735
 File Encoding         : 65001

 Date: 01/09/2022 19:57:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for game_history
-- ----------------------------
DROP TABLE IF EXISTS `game_history`;
CREATE TABLE `game_history`  (
  `end_time` datetime NULL DEFAULT NULL COMMENT 'x年x月x日\r\n玩家xx击败了玩家xx\r\n游戏难度为 eazy\r\n用时为 1：20',
  `winner` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `loser` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `game_level` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of game_history
-- ----------------------------

-- ----------------------------
-- Table structure for game_level
-- ----------------------------
DROP TABLE IF EXISTS `game_level`;
CREATE TABLE `game_level`  (
  `level` int(255) NOT NULL,
  `unitSize` int(255) NULL DEFAULT NULL,
  `sceenWidth` int(255) NULL DEFAULT NULL,
  `sceenLength` int(255) NULL DEFAULT NULL,
  `rowCount` int(255) NULL DEFAULT NULL,
  `colCount` int(255) NULL DEFAULT NULL,
  PRIMARY KEY (`level`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of game_level
-- ----------------------------
INSERT INTO `game_level` VALUES (0, 50, 1280, 980, 12, 12);
INSERT INTO `game_level` VALUES (1, 100, 1800, 1200, 10, 10);

-- ----------------------------
-- Table structure for log
-- ----------------------------
DROP TABLE IF EXISTS `log`;
CREATE TABLE `log`  (
  `log_info` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '日志内容',
  `log_time` datetime NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of log
-- ----------------------------

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `username` varchar(25) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '登录账户、用户名',
  `password` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码',
  `nickname` varchar(25) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `is_admin` int(20) NOT NULL COMMENT '是否为管理员',
  `user_rank` varchar(25) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '玩家排名',
  `user_goal` varchar(25) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '玩家积分',
  `user_port` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '玩家端口号',
  `user_friend` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '玩家好友列表',
  PRIMARY KEY (`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('123', '4297f44b13955235245b2497399d7a93', '', 0, '1', '10', '8888', '');
INSERT INTO `user` VALUES ('a1', '4297f44b13955235245b2497399d7a93', NULL, 0, NULL, '0', '8888', NULL);
INSERT INTO `user` VALUES ('ACID', '4297f44b13955235245b2497399d7a93', '', 0, '2', '20', '8888', '');
INSERT INTO `user` VALUES ('asdasd', '5fa72358f0b4fb4f2c5d7de8c9a41846', '', 0, '3', '30', '8888', '');
INSERT INTO `user` VALUES ('asdasdasd', '5fa72358f0b4fb4f2c5d7de8c9a41846', '', 0, '4', '40', '8888', '');
INSERT INTO `user` VALUES ('cz', '4297f44b13955235245b2497399d7a93', ' ', 0, '999', '0', '8888', ' ');
INSERT INTO `user` VALUES ('czdsaasddas', '4297f44b13955235245b2497399d7a93', ' ', 0, '999', '0', '8888', ' ');
INSERT INTO `user` VALUES ('czdsaassaddas', '4297f44b13955235245b2497399d7a93', ' ', 0, '999', '0', '8888', ' ');
INSERT INTO `user` VALUES ('da', '196b0f14eba66e10fba74dbf9e99c22f', NULL, 0, NULL, '0', '8888', NULL);
INSERT INTO `user` VALUES ('j2ee', '4297f44b13955235245b2497399d7a93', 'iTie', 1, '5', '50', '8888', '');
INSERT INTO `user` VALUES ('wuyiheng', '4297f44b13955235245b2497399d7a93', '', 0, '6', '60', '8888', '');
INSERT INTO `user` VALUES ('zxc', '5fa72358f0b4fb4f2c5d7de8c9a41846', '', 0, '7', '70', '8888', '');
INSERT INTO `user` VALUES ('zyj', '4297f44b13955235245b2497399d7a93', ' ', 0, '999', '0', '8888', ' ');
INSERT INTO `user` VALUES ('zz1', '5fa72358f0b4fb4f2c5d7de8c9a41846', NULL, 0, NULL, '0', '8888', NULL);

SET FOREIGN_KEY_CHECKS = 1;
