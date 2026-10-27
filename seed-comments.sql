-- 商品评价种子数据（用于鸿蒙端「评价」tab 展示）
-- 只动 target_type='goods'，不影响短视频评论
USE java10143;

-- 清掉之前手测留下的无意义评论
DELETE FROM comment WHERE target_type='goods' AND content IN ('2','3',',','234','11','1');

-- 荣耀平板 1003
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1003,1,'购物小达人',5.0,'屏幕素质很惊喜，144Hz 滑动跟手，看文档和追剧都舒服，重量也控制得不错。',32,'2026-10-10 20:12:00'),
('goods',1003,2,'普通用户',4.0,'性能够用，后悔没上 12G 版本。散热一般，长时间玩游戏背面会有点温热。',15,'2026-10-09 15:40:00'),
('goods',1003,3,'管理员',5.0,'平板电脑 12.1 英寸，边框很窄，官方笔延迟低，记笔记很顺手。',48,'2026-10-08 09:20:00');

-- 华为 Mate 70 Pro 1001
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1001,2,'普通用户',5.0,'昆仑玻璃确实抗摔，用了两周没划痕，拍照长焦也很稳，夜景进步明显。',86,'2026-10-11 22:54:28'),
('goods',1001,1,'购物小达人',5.0,'信号是真的强，电梯里也能刷视频，鸿蒙系统流畅度没得说。',64,'2026-10-10 18:03:00'),
('goods',1001,3,'管理员',4.5,'续航一天重度使用没问题，就是机身略重，单手握久了会觉得有点压手。',40,'2026-10-09 11:26:00');

-- 小米 15 Ultra 1002
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1002,1,'购物小达人',5.0,'徕卡色彩很讨喜，人像直出就能发朋友圈，大底进光量足，夜拍几乎没噪点。',73,'2026-10-12 10:15:00'),
('goods',1002,2,'普通用户',4.5,'拍照没得挑，就是重了一点，镜头模组突出比较明显，建议配个壳。',29,'2026-10-11 16:48:00');

-- 漫步者 NeoBuds Pro 3 1004
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1004,2,'普通用户',5.0,'降噪很给力，地铁里基本听不到报站声，通勤路上神器。',55,'2026-10-12 08:30:00'),
('goods',1004,1,'购物小达人',4.5,'音质在这个价位很能打，低频有弹性，戴久了耳朵也不闷。',37,'2026-10-11 19:05:00');

-- 华为手表 1005
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1005,3,'管理员',5.0,'健康监测很全面，睡眠数据挺准，续航能撑十来天，日常够用。',44,'2026-10-10 21:10:00'),
('goods',1005,1,'购物小达人',4.0,'表盘好看、运动模式多，就是第三方应用生态还偏少。',18,'2026-10-09 13:55:00');

-- 空气炸锅 1009
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1009,2,'普通用户',5.0,'可视窗太实用了，不用反复拉出来看，鸡翅 200 度 15 分钟就外焦里嫩。',61,'2026-10-08 20:40:00'),
('goods',1009,3,'管理员',5.0,'容量够一家三口用，清洗方便，一键解冻也好用，性价比高。',33,'2026-10-07 12:22:00');

-- 李宁跑鞋 1013
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1013,1,'购物小达人',5.0,'回弹很好，跑十公里膝盖压力小多了，鞋面透气不闷脚。',52,'2026-10-11 07:40:00'),
('goods',1013,2,'普通用户',4.5,'码数偏小半码建议买大一码，鞋底抓地不错，雨天也不打滑。',26,'2026-10-10 18:12:00');

-- 其他首页商品简单补几条
INSERT INTO comment (target_type,target_id,user_id,nickname,score,content,like_count,create_time) VALUES
('goods',1025,3,'管理员',5.0,'考试用笔，出水顺滑不洇纸，一盒能用一学期，囤货首选。',21,'2026-10-09 09:00:00'),
('goods',1016,1,'购物小达人',5.0,'小金条显色度很高，哑光不拔干，黄皮涂也好看。',45,'2026-10-10 22:18:00'),
('goods',1015,2,'普通用户',5.0,'质地清爽好吸收，用了两周皮肤状态稳定了不少。',38,'2026-10-11 21:05:00'),
('goods',1006,1,'购物小达人',4.5,'75 英寸放客厅正合适，画质通透，系统没有开机广告很加分。',57,'2026-10-12 19:30:00'),
('goods',1010,3,'管理员',5.0,'吸力强，地毯深处的灰都能吸出来，配件齐全，就是有点重。',42,'2026-10-11 14:20:00');
