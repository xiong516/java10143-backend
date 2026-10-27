-- -*- coding: utf-8 -*-
-- 直播带货（方案 D 扩展）：直播间 + 直播商品 + 直播弹幕
-- 全部 IF NOT EXISTS，可重复执行

CREATE TABLE IF NOT EXISTS live_room (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  title         VARCHAR(120) NOT NULL COMMENT '直播间标题',
  cover         VARCHAR(255) DEFAULT NULL COMMENT '封面',
  anchor_name   VARCHAR(50)  DEFAULT NULL COMMENT '主播名',
  anchor_avatar VARCHAR(255) DEFAULT NULL COMMENT '主播头像',
  anchor_id     BIGINT       DEFAULT NULL COMMENT '主播用户 id',
  intro         VARCHAR(255) DEFAULT NULL COMMENT '简介',
  spu_id        BIGINT       DEFAULT NULL COMMENT '主推商品',
  status        INT          DEFAULT 0 COMMENT '0 预告 1 直播中 2 已结束',
  viewer_count  INT          DEFAULT 0 COMMENT '累计观看',
  online_count  INT          DEFAULT 0 COMMENT '当前在线',
  like_count    INT          DEFAULT 0 COMMENT '点赞',
  goods_count   INT          DEFAULT 0 COMMENT '挂车商品数',
  start_time    DATETIME     DEFAULT NULL,
  end_time      DATETIME     DEFAULT NULL,
  sort          INT          DEFAULT 0,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间';

CREATE TABLE IF NOT EXISTS live_goods (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  room_id     BIGINT NOT NULL,
  spu_id      BIGINT NOT NULL,
  sku_id      BIGINT DEFAULT NULL,
  live_price  DECIMAL(10,2) DEFAULT NULL COMMENT '直播专享价',
  stock       INT  DEFAULT 0,
  sold        INT  DEFAULT 0,
  sort        INT  DEFAULT 0,
  is_spot     INT  DEFAULT 0 COMMENT '1 讲解中',
  PRIMARY KEY (id),
  KEY idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间挂车商品';

CREATE TABLE IF NOT EXISTS live_message (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  room_id    BIGINT NOT NULL,
  user_id    BIGINT DEFAULT NULL,
  nickname   VARCHAR(50) DEFAULT NULL,
  content    VARCHAR(255) DEFAULT NULL,
  type       INT DEFAULT 0 COMMENT '0 弹幕 1 进场 2 系统',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_room_time (room_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播弹幕';
