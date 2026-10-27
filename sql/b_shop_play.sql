-- ============================================================
--  京东 10143 —— B 方案（电商业务玩法）
--  1. coupon        优惠券模板
--  2. user_coupon   用户领到的券
--  3. seckill       秒杀活动
--  4. price_history 商品价格走势快照
--  幂等：全部 IF NOT EXISTS
-- ============================================================

CREATE TABLE IF NOT EXISTS `coupon` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `name`          varchar(60)  NOT NULL COMMENT '券名，如 满300减50',
  `type`          tinyint      NOT NULL DEFAULT 1 COMMENT '1满减 2折扣 3无门槛',
  `threshold`     decimal(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额，0=无门槛',
  `amount`        decimal(10,2) NOT NULL DEFAULT 0 COMMENT '减免金额（type=1/3 用）',
  `discount`      decimal(4,2)  NOT NULL DEFAULT 10 COMMENT '折扣（type=2 用，8.5=打85折）',
  `max_discount`  decimal(10,2) NOT NULL DEFAULT 0 COMMENT '折扣券最高抵扣，0=不限',
  `scope`         varchar(20)  NOT NULL DEFAULT 'all' COMMENT 'all全品类 category品类',
  `category_id`   bigint       NULL COMMENT 'scope=category 时限定品类',
  `total_count`   int          NOT NULL DEFAULT 100 COMMENT '发行总量',
  `taken_count`   int          NOT NULL DEFAULT 0 COMMENT '已领取',
  `used_count`    int          NOT NULL DEFAULT 0 COMMENT '已使用',
  `per_limit`     int          NOT NULL DEFAULT 1 COMMENT '每人限领',
  `start_time`    datetime     NULL,
  `end_time`      datetime     NULL,
  `status`        tinyint      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id`         bigint   NOT NULL AUTO_INCREMENT,
  `user_id`    bigint   NOT NULL,
  `coupon_id`  bigint   NOT NULL,
  `status`     tinyint  NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  `order_id`   bigint   NULL COMMENT '使用的订单',
  `take_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `use_time`   datetime NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`, `status`),
  KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户领券记录';

CREATE TABLE IF NOT EXISTS `seckill` (
  `id`            bigint        NOT NULL AUTO_INCREMENT,
  `spu_id`        bigint        NOT NULL,
  `sku_id`        bigint        NOT NULL,
  `title`         varchar(80)   NULL COMMENT '活动口号',
  `price`         decimal(10,2) NOT NULL COMMENT '秒杀价',
  `origin_price`  decimal(10,2) NULL COMMENT '原价',
  `stock`         int           NOT NULL DEFAULT 50 COMMENT '秒杀库存',
  `sold`          int           NOT NULL DEFAULT 0 COMMENT '已售',
  `limit_per`     int           NOT NULL DEFAULT 1 COMMENT '每人限购',
  `start_time`    datetime      NOT NULL,
  `end_time`      datetime      NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

CREATE TABLE IF NOT EXISTS `price_history` (
  `id`          bigint        NOT NULL AUTO_INCREMENT,
  `spu_id`      bigint        NOT NULL,
  `price`       decimal(10,2) NOT NULL COMMENT '当日最低价',
  `stat_date`   date          NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spu_date` (`spu_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格走势（按日快照）';
