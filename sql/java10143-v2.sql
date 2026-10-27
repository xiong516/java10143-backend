-- =====================================================================
--  Java10143 v2 升级脚本：短视频 + 评论 + 管理员角色
--  用法（已有 java10143 库的情况下）：
--    mysql -uroot -p < java10143-v2.sql
--  或 Navicat 里直接运行本文件
-- =====================================================================
USE `java10143`;

-- ---------- 1. user 表加角色字段（admin 账号 = 管理员） ----------
ALTER TABLE `user`
    ADD COLUMN `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色：user 普通用户 / admin 管理员';
UPDATE `user` SET `role` = 'admin' WHERE `account` = 'admin';
UPDATE `user` SET `role` = 'user'  WHERE `role` IS NULL OR `role` = '';

-- ---------- 2. 短视频表 ----------
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spu_id`         BIGINT       DEFAULT NULL COMMENT '关联商品 id',
    `title`          VARCHAR(100) DEFAULT NULL COMMENT '视频标题',
    `description`    VARCHAR(255) DEFAULT NULL COMMENT '视频描述/文案',
    `cover`          VARCHAR(255) DEFAULT NULL COMMENT '封面图（相对路径）',
    `url`            VARCHAR(255) DEFAULT NULL COMMENT '视频地址，空=前端用图片合成动态视频',
    `author_name`    VARCHAR(50)  DEFAULT NULL COMMENT '作者昵称',
    `author_avatar`  VARCHAR(255) DEFAULT NULL COMMENT '作者头像',
    `tags`           VARCHAR(255) DEFAULT NULL COMMENT '话题标签，用逗号分隔',
    `like_count`     INT          DEFAULT 0 COMMENT '点赞数',
    `comment_count`  INT          DEFAULT 0 COMMENT '评论数',
    `share_count`    INT          DEFAULT 0 COMMENT '分享数',
    `collect_count`  INT          DEFAULT 0 COMMENT '收藏数',
    `duration`       INT          DEFAULT 15 COMMENT '时长（秒）',
    `sort`           INT          DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`         INT          DEFAULT 1 COMMENT '1 上架 0 下架',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_spu` (`spu_id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品短视频（抖音式）';

-- ---------- 3. 评论表（商品 / 视频通用） ----------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `target_type`  VARCHAR(20)  NOT NULL COMMENT '评论对象类型：goods 商品 / video 视频',
    `target_id`    BIGINT       NOT NULL COMMENT '评论对象 id（商品 id 或视频 id）',
    `user_id`      BIGINT       DEFAULT NULL COMMENT '评论人 id',
    `nickname`     VARCHAR(50)  DEFAULT NULL COMMENT '评论人昵称',
    `avatar`       VARCHAR(255) DEFAULT NULL COMMENT '评论人头像',
    `score`        DECIMAL(2,1) DEFAULT 5.0 COMMENT '评分 1~5',
    `content`      VARCHAR(500) DEFAULT NULL COMMENT '评论内容',
    `pictures`     VARCHAR(1000) DEFAULT NULL COMMENT '评论图片，JSON 数组字符串',
    `like_count`   INT          DEFAULT 0 COMMENT '点赞数',
    `reply_count`  INT          DEFAULT 0 COMMENT '回复数',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论（商品/视频通用）';

-- ---------- 4. 短视频种子数据 ----------
-- 前 4 条是 AI 生成的真实 mp4（url 指向 /media/video/*.mp4），
-- 其余 url 留空 → 前端用商品图 + 运镜动效合成"轻量视频"，保证每个商品都有视频可刷。
INSERT INTO `video`
(`spu_id`, `title`, `description`, `cover`, `url`, `author_name`, `author_avatar`, `tags`, `like_count`, `share_count`, `collect_count`, `duration`, `sort`) VALUES
(1001, 'HUAWEI Mate 70 Pro 上手实拍', '昆仑玻璃 + 鸿蒙原生，夜景抓拍稳得离谱', '/img/p/p1001.svg?t=华为Mate70Pro', '/media/video/v1001.mp4', '科技小王', '/img/a/u1.svg?t=科技小王', '数码,华为,新机上手', 12800, 320, 4600, 15, 1),
(1002, '小米 15 Ultra 徕卡影像实拍', '一英寸大底，随手一拍都是大片',       '/img/p/p1002.svg?t=小米15Ultra',   '/media/video/v1002.mp4', '摄影老李', '/img/a/u2.svg?t=摄影老李', '数码,摄影,徕卡',   9600, 210, 3100, 15, 2),
(1004, '漫步者 NeoBuds Pro 3 降噪实测', '地铁里开降噪，世界瞬间安静',        '/img/p/p1004.svg?t=降噪耳机',      '/media/video/v1004.mp4', '耳机控',   '/img/a/u3.svg?t=耳机控',   '数码,耳机,降噪',   5300, 120, 1800, 15, 3),
(1013, '李宁䨻科技跑鞋上脚体验', '轻弹回弹，跑十公里不累脚',                '/img/p/p1013.svg?t=李宁跑鞋',      '/media/video/v1013.mp4', '跑步的猫', '/img/a/u1.svg?t=跑步的猫', '运动,跑鞋,国货',   7100, 160, 2400, 15, 4),
(1005, 'HUAWEI WATCH GT 5 Pro 开箱', '钛金属表壳，14 天续航',               '/img/p/p1005.svg?t=华为手表',      '', '腕上玩家', '/img/a/u2.svg?t=腕上玩家', '数码,手表,开箱', 4200, 90, 1300, 12, 5),
(1006, '小米电视 S Pro 75 观影体验', 'Mini LED 千级分区，客厅秒变影院',      '/img/p/p1006.svg?t=小米电视',      '', '客厅改造家', '/img/a/u3.svg?t=客厅改造家', '家电,电视,影音', 3800, 80, 1100, 12, 6),
(1010, '戴森 V12 吸尘器暗格实测', '地板缝里的猫毛都吸得干干净净',           '/img/p/p1010.svg?t=戴森吸尘器',    '', '家务小能手', '/img/a/u1.svg?t=家务小能手', '家电,清洁,戴森', 4600, 100, 1500, 12, 7),
(1009, '美的空气炸锅 5.5L 一锅三菜', '可视窗口，出餐不用开盖',                '/img/p/p1009.svg?t=空气炸锅',      '', '厨房日记', '/img/a/u2.svg?t=厨房日记', '厨房,美食,好物',  6100, 140, 2000, 12, 8),
(1015, '兰蔻小黑瓶 28 天肌肤打卡', '熬夜脸也能救回来的肌底液',               '/img/p/p1015.svg?t=兰蔻小黑瓶',    '', '护肤研究所', '/img/a/u3.svg?t=护肤研究所', '美妆,护肤,兰蔻', 8800, 190, 2900, 12, 9),
(1016, '圣罗兰小金条试色', '秋冬必入的显白王炸色号',                        '/img/p/p1016.svg?t=圣罗兰口红',    '', '美妆种草机', '/img/a/u1.svg?t=美妆种草机', '美妆,口红,试色', 12300, 260, 4100, 12, 10);

-- ---------- 5. 评论种子数据 ----------
INSERT INTO `comment` (`target_type`, `target_id`, `user_id`, `nickname`, `avatar`, `score`, `content`, `like_count`) VALUES
('goods', 1001, 1, '黑马小王', '/img/a/u1.svg?t=黑马小王', 5.0, '手感确实好，鸿蒙系统流畅，拍照夜景很惊喜。', 32),
('goods', 1001, 2, '测试用户', '/img/a/u2.svg?t=测试用户', 4.0, '屏幕素质不错，就是有点重，单手握着累。', 12),
('goods', 1001, 3, '管理员',   '/img/a/u3.svg?t=管理员',   5.0, '续航一天半没问题，充电也快，整体值这个价。', 21),
('goods', 1002, 1, '黑马小王', '/img/a/u1.svg?t=黑马小王', 5.0, '徕卡的色彩真的好看，随手拍都出片。', 18),
('goods', 1004, 2, '测试用户', '/img/a/u2.svg?t=测试用户', 5.0, '降噪很给力，通勤路上世界都安静了。', 9),
('video', 1, 1, '黑马小王', '/img/a/u1.svg?t=黑马小王', 5.0, '看完直接下单了，这代夜景提升太明显！', 45),
('video', 1, 2, '测试用户', '/img/a/u2.svg?t=测试用户', 4.0, '视频拍得挺好，就是价格能不能再香一点。', 16),
('video', 2, 3, '管理员',   '/img/a/u3.svg?t=管理员',   5.0, '这个镜头素质，出门旅行可以不带相机了。', 27),
('video', 3, 1, '黑马小王', '/img/a/u1.svg?t=黑马小王', 5.0, '降噪开启那一瞬间，太爽了。', 11),
('video', 4, 2, '测试用户', '/img/a/u2.svg?t=测试用户', 5.0, '国货跑鞋越来越能打了，脚感真不错。', 14);

-- 把评论条数回填到 video.comment_count
UPDATE `video` v SET v.`comment_count` = (
    SELECT COUNT(*) FROM `comment` c WHERE c.`target_type` = 'video' AND c.`target_id` = v.`id`
);

-- ---------- 6. 校验 ----------
SELECT (SELECT COUNT(*) FROM `video`)   AS `视频数`,
       (SELECT COUNT(*) FROM `comment`) AS `评论数`,
       (SELECT COUNT(*) FROM `user` WHERE `role` = 'admin') AS `管理员数`;
