/*
Navicat MySQL Data Transfer

Source Server         : 链接
Source Server Version : 50716
Source Host           : localhost:3306
Source Database       : kettle-master

Target Server Type    : MYSQL
Target Server Version : 50716
File Encoding         : 65001

Date: 2017-06-09 13:34:19
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for k_job
-- ----------------------------
DROP TABLE IF EXISTS `k_job`;
CREATE TABLE `k_job` (
  `job_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '作业ID',
  `job_name` varchar(50) DEFAULT NULL COMMENT '作业名称',
  `job_description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `job_type` int(11) DEFAULT NULL COMMENT '1:数据库资源库；2:上传的文件',
  `job_path` varchar(200) DEFAULT NULL COMMENT '作业保存路径（可以是资源库中的路径也可以是服务器中保存作业文件的路径）',
  `job_repository_id` int(11) DEFAULT NULL COMMENT '作业的资源库ID',
  `job_quartz` int(11) DEFAULT '1' COMMENT '定时策略（外键ID）',
  `job_record` int(11) DEFAULT NULL COMMENT '作业执行记录（外键ID）',
  `job_log_level` varchar(10) DEFAULT NULL COMMENT '日志级别(basic，detail，error，debug，minimal，rowlevel）',
  `job_status` int(11) DEFAULT NULL COMMENT '状态（1：正在运行；2：已停止）',
  `add_time` datetime DEFAULT NULL COMMENT '添加时间',
  `add_user` int(11) DEFAULT NULL COMMENT '添加者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_user` int(11) DEFAULT NULL COMMENT '编辑者',
  `del_flag` int(11) DEFAULT NULL COMMENT '是否删除（1：存在；0：删除）',
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_job
-- ----------------------------
--INSERT INTO `k_job` VALUES ('1', '字符串长度是指字符串所含的字符始还是从开始', '字符串长度是指字符串所含的字符始还是从开始字符串长度是指字符串所含的字符始还是从开始字符串长度是指字符串所含的字符始还是从开始', '1', null, null, null, null, null, '2', '2017-05-26 11:14:03', '1', '2017-05-26 11:14:07', '1', '1');
--INSERT INTO `k_job` VALUES ('4', '测试测试测试', '任务描述任务描述', '1', '/job/mysql-mysql', '1', '1', null, null, '2', '2017-05-27 19:44:15', '1', '2017-05-27 19:44:15', '1', '1');

-- ----------------------------
-- Table structure for k_job_monitor
-- ----------------------------
DROP TABLE IF EXISTS `k_job_monitor`;
CREATE TABLE `k_job_monitor` (
  `monitor_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '监控作业ID',
  `monitor_job` int(11) DEFAULT NULL COMMENT '监控的作业ID',
  `monitor_success` int(11) DEFAULT NULL COMMENT '成功次数',
  `monitor_fail` int(11) DEFAULT NULL COMMENT '失败次数',
  `add_user` int(11) DEFAULT NULL COMMENT '添加人',
  `monitor_status` int(11) DEFAULT NULL COMMENT '监控状态（是否启动，1:启动；2:停止）',
  `run_status` text COMMENT '运行状态（起始时间-结束时间,起始时间-结束时间……）',
  PRIMARY KEY (`monitor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_job_monitor
-- ----------------------------

-- ----------------------------
-- Table structure for k_job_record
-- ----------------------------
DROP TABLE IF EXISTS `k_job_record`;
CREATE TABLE `k_job_record` (
  `record_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '作业记录ID',
  `record_job` int(11) DEFAULT NULL COMMENT '作业ID',
  `start_time` datetime DEFAULT NULL COMMENT '启动时间',
  `stop_time` datetime DEFAULT NULL COMMENT '停止时间',
  `record_status` int(11) DEFAULT NULL COMMENT '任务执行结果（1：成功；2：失败）',
  `log_file_path` varchar(100) DEFAULT NULL COMMENT '作业日志记录文件保存位置',
  `add_user` int(11) DEFAULT NULL COMMENT '添加人',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_job_record
-- ----------------------------

-- ----------------------------
-- Table structure for k_quartz
-- ----------------------------
DROP TABLE IF EXISTS `k_quartz`;
CREATE TABLE `k_quartz` (
  `quartz_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `quartz_description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `quartz_cron` varchar(100) DEFAULT NULL COMMENT '定时策略',
  `add_time` datetime DEFAULT NULL COMMENT '添加时间',
  `add_user` int(11) DEFAULT NULL COMMENT '添加者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_user` int(11) DEFAULT NULL COMMENT '编辑者',
  `del_flag` int(11) DEFAULT NULL COMMENT '是否删除（1：存在；0：删除）',
  PRIMARY KEY (`quartz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_quartz
-- ----------------------------
--INSERT INTO `k_quartz` VALUES ('1', '立即执行一次', null, '2017-05-27 14:44:13', null, '2017-05-27 14:44:13', null, '1');
--INSERT INTO `k_quartz` VALUES ('2', '每周一0点执行一次', '0 0 0 ? * 2', '2017-05-27 14:56:38', null, '2017-05-27 14:56:38', null, '1');
--INSERT INTO `k_quartz` VALUES ('3', '每月1日0点执行一次', '0 0 0 1 * ?', '2017-05-27 14:56:38', null, '2017-05-27 14:56:38', null, '1');
--INSERT INTO `k_quartz` VALUES ('4', '每日0点执行一次', '0 0 0 * * ?', '2017-05-27 14:44:13', null, '2017-05-27 14:44:15', null, '1');
--INSERT INTO `k_quartz` VALUES ('8', '也不知道是啥的定时策略', '* * * * 1 ?', '2017-05-27 19:15:17', '1', '2017-05-27 19:15:17', '1', '1');
--INSERT INTO `k_quartz` VALUES ('12', 'mysql-mysql的定时策略', '* 0/2 * * * ?', '2017-06-02 13:07:42', '1', '2017-06-02 13:07:42', '1', '1');
--INSERT INTO `k_quartz` VALUES ('13', '6-2测试转换的定时策略', '0/20 * * * * ?', '2017-06-02 17:24:47', '1', '2017-06-02 17:24:47', '1', '1');

-- ----------------------------
-- Table structure for k_repository
-- ----------------------------
DROP TABLE IF EXISTS `k_repository`;
CREATE TABLE `k_repository` (
  `repository_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `repository_name` varchar(50) DEFAULT NULL COMMENT '资源库名称',
  `repository_username` varchar(50) DEFAULT NULL COMMENT '登录用户名',
  `repository_password` varchar(50) DEFAULT NULL COMMENT '登录密码',
  `repository_type` varchar(10) DEFAULT NULL COMMENT '资源库数据库类型（MYSQL、ORACLE）',
  `database_access` varchar(10) DEFAULT NULL COMMENT '资源库数据库访问模式（"Native", "ODBC", "OCI", "Plugin", "JNDI")',
  `database_host` varchar(50) DEFAULT NULL COMMENT '资源库数据库主机名或者IP地址',
  `database_port` varchar(10) DEFAULT NULL COMMENT '资源库数据库端口号',
  `database_name` varchar(20) DEFAULT NULL COMMENT '资源库数据库名称',
  `database_username` varchar(50) DEFAULT NULL COMMENT '数据库登录账号',
  `database_password` varchar(50) DEFAULT NULL COMMENT '数据库登录密码',
  `add_time` datetime DEFAULT NULL COMMENT '添加时间',
  `add_user` int(11) DEFAULT NULL COMMENT '添加者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_user` int(11) DEFAULT NULL COMMENT '编辑者',
  `del_flag` int(11) DEFAULT NULL COMMENT '是否删除（1：存在；0：删除）',
  PRIMARY KEY (`repository_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_repository
-- ----------------------------

-- ----------------------------
-- Table structure for k_repository_type
-- ----------------------------
DROP TABLE IF EXISTS `k_repository_type`;
CREATE TABLE `k_repository_type` (
  `repository_type_id` int(11) NOT NULL,
  `repository_type_code` varchar(30) DEFAULT NULL,
  `repository_type_des` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`repository_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_repository_type
-- ----------------------------
INSERT INTO `k_repository_type` VALUES ('1', 'INGRES', 'Ingres');
INSERT INTO `k_repository_type` VALUES ('2', 'INTERBASE', 'Borland Interbase');
INSERT INTO `k_repository_type` VALUES ('3', 'INFOBRIGHT', 'Infobright');
INSERT INTO `k_repository_type` VALUES ('4', 'ORACLE', 'Oracle');
INSERT INTO `k_repository_type` VALUES ('5', 'EXTENDB', 'ExtenDB');
INSERT INTO `k_repository_type` VALUES ('6', 'MSACCESS', 'MS Access');
INSERT INTO `k_repository_type` VALUES ('7', 'SYBASE', 'Sybase');
INSERT INTO `k_repository_type` VALUES ('8', 'PALO', 'Palo MOLAP Server');
INSERT INTO `k_repository_type` VALUES ('9', 'INFORMIX', 'Informix');
INSERT INTO `k_repository_type` VALUES ('10', 'LucidDB', 'LucidDB');
INSERT INTO `k_repository_type` VALUES ('11', 'TERADATA', 'Teradata');
INSERT INTO `k_repository_type` VALUES ('12', 'UNIVERSE', 'UniVerse database');
INSERT INTO `k_repository_type` VALUES ('13', 'MONETDB', 'MonetDB');
INSERT INTO `k_repository_type` VALUES ('14', 'CACHE', 'Intersystems Cache');
INSERT INTO `k_repository_type` VALUES ('15', 'MSSQL', 'MS SQL Server');
INSERT INTO `k_repository_type` VALUES ('16', 'KettleThin', 'Pentaho Data Services');
INSERT INTO `k_repository_type` VALUES ('17', 'GREENPLUM', 'Greenplum');
INSERT INTO `k_repository_type` VALUES ('18', 'GENERIC', 'Generic database');
INSERT INTO `k_repository_type` VALUES ('19', 'IMPALA', 'Impala');
INSERT INTO `k_repository_type` VALUES ('20', 'SQLITE', 'SQLite');
INSERT INTO `k_repository_type` VALUES ('21', 'REMEDY-AR-SYSTEM', 'Remedy Action Request System');
INSERT INTO `k_repository_type` VALUES ('22', 'MONDRIAN', 'Native Mondrian');
INSERT INTO `k_repository_type` VALUES ('23', 'HIVE2', 'Hadoop Hive 2');
INSERT INTO `k_repository_type` VALUES ('24', 'NETEZZA', 'Netezza');
INSERT INTO `k_repository_type` VALUES ('25', 'VERTICA5', 'Vertica 5+');
INSERT INTO `k_repository_type` VALUES ('26', 'POSTGRESQL', 'PostgreSQL');
INSERT INTO `k_repository_type` VALUES ('27', 'EXASOL4', 'Exasol 4');
INSERT INTO `k_repository_type` VALUES ('28', 'HYPERSONIC', 'Hypersonic');
INSERT INTO `k_repository_type` VALUES ('29', 'AS/400', 'AS/400');
INSERT INTO `k_repository_type` VALUES ('30', 'ORACLERDB', 'Oracle RDB');
INSERT INTO `k_repository_type` VALUES ('31', 'DBASE', 'dBase III, IV or 5');
INSERT INTO `k_repository_type` VALUES ('32', 'IMPALASIMBA', 'Cloudera Impala');
INSERT INTO `k_repository_type` VALUES ('33', 'KINGBASEES', 'KingbaseES');
INSERT INTO `k_repository_type` VALUES ('34', 'SAPR3', 'SAP ERP System');
INSERT INTO `k_repository_type` VALUES ('35', 'SQLBASE', 'Gupta SQL Base');
INSERT INTO `k_repository_type` VALUES ('36', 'DERBY', 'Apache Derby');
INSERT INTO `k_repository_type` VALUES ('37', 'VERTICA', 'Vertica');
INSERT INTO `k_repository_type` VALUES ('38', 'INFINIDB', 'Calpont InfiniDB');
INSERT INTO `k_repository_type` VALUES ('39', 'HIVE', 'Hadoop Hive');
INSERT INTO `k_repository_type` VALUES ('40', 'MYSQL', 'MySQL');
INSERT INTO `k_repository_type` VALUES ('41', 'MSSQLNATIVE', 'MS SQL Server (Native)');
INSERT INTO `k_repository_type` VALUES ('42', 'H2', 'H2');
INSERT INTO `k_repository_type` VALUES ('43', 'SAPDB', 'MaxDB (SAP DB)');
INSERT INTO `k_repository_type` VALUES ('44', 'SPARKSIMBA', 'SparkSQL');
INSERT INTO `k_repository_type` VALUES ('45', 'VECTORWISE', 'Ingres VectorWise');
INSERT INTO `k_repository_type` VALUES ('46', 'DB2', 'IBM DB2');
INSERT INTO `k_repository_type` VALUES ('47', 'NEOVIEW', 'Neoview');
INSERT INTO `k_repository_type` VALUES ('48', 'SYBASEIQ', 'SybaseIQ');
INSERT INTO `k_repository_type` VALUES ('49', 'REDSHIFT', 'Redshift');
INSERT INTO `k_repository_type` VALUES ('50', 'FIREBIRD', 'Firebird SQL');
INSERT INTO `k_repository_type` VALUES ('51', 'OpenERPDatabaseMeta', 'OpenERP Server');

-- ----------------------------
-- Table structure for k_trans
-- ----------------------------
DROP TABLE IF EXISTS `k_trans`;
CREATE TABLE `k_trans` (
  `trans_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '转换ID',
  `trans_name` varchar(50) DEFAULT NULL COMMENT '转换名称',
  `trans_description` varchar(500) DEFAULT NULL COMMENT '转换描述',
  `trans_type` int(11) DEFAULT NULL COMMENT '1:数据库资源库；2:上传的文件',
  `trans_path` varchar(200) DEFAULT NULL COMMENT '转换保存路径（可以是资源库中的路径也可以是服务器中保存作业文件的路径）',
  `trans_repository_id` int(11) DEFAULT NULL COMMENT '转换的资源库ID',
  `trans_quartz` int(11) DEFAULT '1' COMMENT '定时策略（外键ID）',
  `trans_record` int(11) DEFAULT NULL COMMENT '转换执行记录（外键ID）',
  `trans_log_level` varchar(10) DEFAULT NULL COMMENT '日志级别(basic，detail，error，debug，minimal，rowlevel）',
  `trans_status` int(11) DEFAULT NULL COMMENT '状态（1：正在运行；2：已停止）',
  `add_time` datetime DEFAULT NULL COMMENT '添加时间',
  `add_user` int(11) DEFAULT NULL COMMENT '添加者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_user` int(11) DEFAULT NULL COMMENT '编辑者',
  `del_flag` int(11) DEFAULT NULL COMMENT '是否删除（1：存在；0：删除）',
  PRIMARY KEY (`trans_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_trans
-- ----------------------------
INSERT INTO `k_trans` VALUES ('5', '测试转换6-2', '如果测试成功我就回家', '1', '/trans/kettle-test-6.2', '2', '13', null, 'debug', '2', '2017-06-02 19:53:19', '1', '2017-06-02 19:53:19', '1', '1');

-- ----------------------------
-- Table structure for k_trans_monitor
-- ----------------------------
DROP TABLE IF EXISTS `k_trans_monitor`;
CREATE TABLE `k_trans_monitor` (
  `monitor_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '监控转换ID',
  `monitor_trans` int(11) DEFAULT NULL COMMENT '监控的转换的ID',
  `monitor_success` int(11) DEFAULT NULL COMMENT '成功次数',
  `monitor_fail` int(11) DEFAULT NULL COMMENT '失败次数',
  `add_user` int(11) DEFAULT NULL COMMENT '添加人',
  `monitor_status` int(11) DEFAULT NULL COMMENT '监控状态（是否启动，1:启动；2:停止）',
  `run_status` text COMMENT '运行状态（起始时间-结束时间,起始时间-结束时间……）',
  PRIMARY KEY (`monitor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_trans_monitor
-- ----------------------------
INSERT INTO `k_trans_monitor` VALUES ('2', '5', '10', '0', '1', '2', '1496882010562-1496882116205,1496882152403-1496882203088,1496984875507-1496984902176');

-- ----------------------------
-- Table structure for k_trans_record
-- ----------------------------
DROP TABLE IF EXISTS `k_trans_record`;
CREATE TABLE `k_trans_record` (
  `record_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '转换记录ID',
  `record_trans` int(11) DEFAULT NULL COMMENT '转换ID',
  `start_time` datetime DEFAULT NULL COMMENT '启动时间',
  `stop_time` datetime DEFAULT NULL COMMENT '停止时间',
  `record_status` int(11) DEFAULT NULL COMMENT '任务执行结果（1：成功；2：失败）',
  `log_file_path` varchar(100) DEFAULT NULL COMMENT '转换日志记录文件保存位置',
  `add_user` int(11) DEFAULT NULL COMMENT '添加人',
  PRIMARY KEY (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_trans_record
-- ----------------------------
---INSERT INTO `k_trans_record` VALUES ('54', '5', '2017-06-08 08:33:53', '2017-06-08 08:33:56', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882035925.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('55', '5', '2017-06-08 08:34:09', '2017-06-08 08:34:11', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882050981.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('56', '5', '2017-06-08 08:34:25', '2017-06-08 08:34:27', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882066788.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('57', '5', '2017-06-08 08:34:45', '2017-06-08 08:34:45', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882085275.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('58', '5', '2017-06-08 08:35:02', '2017-06-08 08:35:02', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882102211.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('59', '5', '2017-06-08 08:36:02', '2017-06-08 08:36:02', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882161791.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('60', '5', '2017-06-08 08:36:21', '2017-06-08 08:36:22', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882181505.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('61', '5', '2017-06-08 08:36:41', '2017-06-08 08:36:42', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496882201608.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('62', '5', '2017-06-09 13:08:03', '2017-06-09 13:08:04', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496984883542.txt', '1');
--INSERT INTO `k_trans_record` VALUES ('63', '5', '2017-06-09 13:08:21', '2017-06-09 13:08:22', '1', 'F:/tmp/kettle-master//1/trans@kettle-test-6.2-log/1496984901542.txt', '1');

-- ----------------------------
-- Table structure for k_user
-- ----------------------------
DROP TABLE IF EXISTS `k_user`;
CREATE TABLE `k_user` (
  `u_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `u_nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `u_email` varchar(30) DEFAULT NULL COMMENT '用户邮箱',
  `u_phone` varchar(50) DEFAULT NULL COMMENT '用于电话',
  `u_account` varchar(50) DEFAULT NULL COMMENT '用户账号',
  `u_password` varchar(50) DEFAULT NULL COMMENT '用户密码',
  `add_time` datetime DEFAULT NULL COMMENT '添加时间',
  `add_user` int(11) DEFAULT NULL COMMENT '添加者',
  `edit_time` datetime DEFAULT NULL COMMENT '编辑时间',
  `edit_user` int(11) DEFAULT NULL COMMENT '编辑者',
  `del_flag` int(11) DEFAULT NULL COMMENT '是否删除（1：存在；0：删除）',
  PRIMARY KEY (`u_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of k_user
-- ----------------------------
INSERT INTO `k_user` VALUES ('1', 'admin', null, null, 'admin', 'cc9e6ea0462b98fe1d3cb09c2b46a838', null, null, null, null, '1');
