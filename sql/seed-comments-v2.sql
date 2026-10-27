-- 商品评价种子数据 v2（2026-10-24）
-- 目标：30 个商品每个至少 3 条评价；本次补 16 个空白商品（各 3 条）+ 6 个只有 1 条的（各补 2 条）
-- 只动 target_type='goods'，不影响短视频/直播间评论
USE java10143;

-- ===================== 16 个没有评价的商品（各补 3 条） =====================

-- 1007 美的 508L 法式多门冰箱
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1007,1,'大熊猫爱吃竹子','/img/a/u1.svg?t=大熊猫爱吃竹子',5.0,'508L 容量真不是盖的，法式四门分区合理，变温室放母乳和海鲜都合适，风冷无霜再也不用除冰了。','["/img/p/p1007.svg"]',43,'2026-10-14 20:15:00'),
('goods',1007,2,'爱吃橘子的猫','/img/a/u2.svg?t=爱吃橘子的猫',4.5,'静音效果不错，放厨房夜里基本听不到声音。就是送货师傅说电梯进不去爬了六楼，辛苦了。',NULL,21,'2026-10-13 11:32:00'),
('goods',1007,3,'开源集中营','/img/a/u3.svg?t=开源集中营',5.0,'一级能效月底看电表确实省，面板玻璃好擦，美的一直口碑稳，推荐新家装修入手。','["/img/p/p1007.svg"]',18,'2026-10-12 09:48:00');

-- 1008 小天鹅 10KG 滚筒洗衣机
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1008,2,'晚风扑扑','/img/a/u2.svg?t=晚风扑扑',5.0,'10 公斤全家桶够用，洗烘一体梅雨天救了大命，被套床单塞进去一次搞定，脱水也安静。',NULL,57,'2026-10-13 21:20:00'),
('goods',1008,1,'早睡冠军候选人','/img/a/u1.svg?t=早睡冠军候选人',4.5,'羊毛洗程序真的温柔，毛衣洗完不缩水。不过烘干容量比洗涤小，大件得分开烘。',NULL,26,'2026-10-11 14:05:00'),
('goods',1008,3,'数码老炮儿','/img/a/u3.svg?t=数码老炮儿',5.0,'dd 直驱电机就是稳，噪音比家里旧的波轮小太多，晚上洗衣服不扰民，一级能效也省电。','["/img/p/p1008.svg"]',33,'2026-10-10 19:44:00');

-- 1011 李宁 男款运动外套 春秋款
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1011,1,'跑者阿伟','/img/a/u1.svg?t=跑者阿伟',5.0,'版型正，175/84A 穿着刚好留一件卫衣的余量，面料软而不塌，晨跑挡风正好。','["/img/p/p1011.svg"]',39,'2026-10-14 08:22:00'),
('goods',1011,2,'溜达的锦鲤','/img/a/u2.svg?t=溜达的锦鲤',4.5,'做工没得挑，走线整齐，口袋深。颜色比图片稍微深一点点，可以接受。',NULL,12,'2026-10-12 17:36:00'),
('goods',1011,4,'test1','/img/a/u4.svg?t=test1',5.0,'春秋穿厚度刚好，透气不闷，国货之光，第二件李宁了。',NULL,15,'2026-10-11 12:10:00');

-- 1012 中国李宁 女子卫衣 国潮款
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1012,2,'桃子汽水','/img/a/u2.svg?t=桃子汽水',5.0,'印花太好看了，情侣款和男朋友一人一件，加绒的手感软软的，洗了一次没掉色没变形。','["/img/p/p1012.svg"]',66,'2026-10-13 22:41:00'),
('goods',1012,1,'奶茶三分甜','/img/a/u1.svg?t=奶茶三分甜',4.5,'宽松版型显瘦，160 穿 S 码到屁股，质量对得起价格，就是物流慢了一天。',NULL,24,'2026-10-12 10:18:00'),
('goods',1012,3,'骑手大姐大','/img/a/u3.svg?t=骑手大姐大',5.0,'面料厚实不起球，中国李宁的刺绣细节拉满，回头率很高，闺蜜看了也下单了。',NULL,31,'2026-10-10 16:55:00');

-- 1014 简约通勤双肩包 15.6 英寸
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1014,1,'格子衫常驻嘉宾','/img/a/u1.svg?t=格子衫常驻嘉宾',5.0,'电脑隔层带缓冲，15.6 寸游戏本塞进去还有富余，背部透气设计夏天背也不捂汗。','["/img/p/p1014.svg"]',48,'2026-10-14 09:02:00'),
('goods',1014,2,'打工人不打烊','/img/a/u2.svg?t=打工人不打烊',4.5,'能装是真能装，雨天淋了一路里面没湿。拉链再顺滑点就完美了。',NULL,19,'2026-10-12 20:33:00'),
('goods',1014,3,'上岸中','/img/a/u3.svg?t=上岸中',5.0,'考研自习室通勤神器，书 laptop 水杯雨伞全装得下，肩带减压设计背一天也不酸。',NULL,22,'2026-10-11 08:47:00');

-- 1017 海飞丝 去屑洗发水 750ml
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1017,2,'头秃少女自救中','/img/a/u2.svg?t=头秃少女自救中',5.0,'去屑真的有用，用了两周肩膀上再没下过雪，750ml 大瓶全家能用俩月。','["/img/p/p1017.svg"]',52,'2026-10-13 19:26:00'),
('goods',1017,1,'油头星人','/img/a/u1.svg?t=油头星人',4.5,'控油效果不错，以前一天一洗现在两天一洗，味道是经典柠檬味不冲。',NULL,27,'2026-10-11 22:08:00'),
('goods',1017,3,'阿三的日常','/img/a/u3.svg?t=阿三的日常',5.0,'正品无疑，扫码能查，泡沫细腻好冲，价格比超市便宜不少，回购第三次了。',NULL,16,'2026-10-10 12:30:00');

-- 1018 云南白药 牙膏 双支装
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1018,1,'牙龈敏感户','/img/a/u1.svg?t=牙龈敏感户',5.0,'刷牙出血用了一周明显好转，膏体温和不辣嘴，双支装划算，囤货必备。','["/img/p/p1018.svg"]',36,'2026-10-14 07:55:00'),
('goods',1018,2,'脆皮大学生','/img/a/u2.svg?t=脆皮大学生',4.5,'薄荷味挺清爽，但不是那种辣嘴的凉，早上刷完口气清新时间挺长。',NULL,14,'2026-10-12 21:17:00'),
('goods',1018,4,'test1','/img/a/u4.svg?t=test1',5.0,'国货牙膏里的排面，全家都在用，物流快日期新鲜，离生产日期才一个月。',NULL,11,'2026-10-11 09:36:00');

-- 1019 伊利 金典有机纯牛奶 250ml*12
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1019,3,'早八人','/img/a/u3.svg?t=早八人',5.0,'3.8g 乳蛋白口感确实浓，有机认证喝着放心，孩子早上热一盒配面包，一个月喝了四箱。','["/img/p/p1019.svg"]',58,'2026-10-13 08:12:00'),
('goods',1019,1,'健身餐日记','/img/a/u1.svg?t=健身餐日记',4.5,'无蔗糖不腻，练完后来一盒补蛋白，包装完好一箱没坏盒，日期也新鲜。',NULL,23,'2026-10-11 18:44:00'),
('goods',1019,2,'囤货小能手','/img/a/u2.svg?t=囤货小能手',5.0,'比超市每盒便宜一块多，12 盒装冰箱正好放得下，万单店回购不解释。',NULL,17,'2026-10-10 15:22:00');

-- 1021 智利车厘子 2J 5斤装
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1021,1,'车厘子自由者','/img/a/u1.svg?t=车厘子自由者',5.0,'2J 果径真的大，一口一个停不下来，冰袋到货还是硬的，坏果就挑出来两颗还理赔了。','["/img/p/p1021.svg"]',89,'2026-10-13 21:58:00'),
('goods',1021,2,'水果雷达','/img/a/u2.svg?t=水果雷达',4.5,'甜度在线，梗是绿的说明新鲜，就是大小略有参差，总体对得起价格。',NULL,34,'2026-10-12 13:07:00'),
('goods',1021,3,'夜宵指挥官','/img/a/u3.svg?t=夜宵指挥官',5.0,'泡沫箱+冰袋包装很到位，5 斤吃了一周，比楼下水果店便宜三分之一，已推荐同事。',NULL,41,'2026-10-11 20:31:00');

-- 1022 三顿半 精品速溶咖啡 24颗
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1022,2,'美式续命选手','/img/a/u2.svg?t=美式续命选手',5.0,'冷水冰水都能秒溶，数字罐口味全试了一遍，5 号做冰美式绝了，小罐子还留着种多肉。','["/img/p/p1022.svg"]',61,'2026-10-14 10:24:00'),
('goods',1022,1,'咖啡因驱动','/img/a/u1.svg?t=咖啡因驱动',4.5,'风味确实比普通速溶强，中深烘不酸，就是量少一泡刚够，办公室常备。',NULL,28,'2026-10-12 14:52:00'),
('goods',1022,3,'猫窝蹲守员','/img/a/u3.svg?t=猫窝蹲守员',5.0,'颜值和口味都在线，早餐配牛奶做拿铁很顺，比连锁店一杯便宜太多。',NULL,19,'2026-10-10 19:09:00');

-- 1023 人民文学出版社 红楼梦 全二册
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1023,1,'纸质书守门人','/img/a/u1.svg?t=纸质书守门人',5.0,'人文社经典版本，注释详实，排版疏朗不费眼，脂批参考价值高，收藏阅读两相宜。','["/img/p/p1023.svg"]',47,'2026-10-13 16:38:00'),
('goods',1023,2,'备考中的咸鱼','/img/a/u2.svg?t=备考中的咸鱼',4.5,'纸张厚实没有异味，字体大小适中，晚上读半小时眼睛不难受，就是书有点重。',NULL,13,'2026-10-11 23:15:00'),
('goods',1023,3,'书虫本虫','/img/a/u3.svg?t=书虫本虫',5.0,'给女儿买的课外必读，塑料膜全新未拆，印刷清晰无错页，这个价位很值。',NULL,21,'2026-10-10 11:26:00');

-- 1024 少儿科普绘本 十万个为什么 全8册
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1024,2,'两个男孩的妈','/img/a/u2.svg?t=两个男孩的妈',5.0,'印刷精美配色舒服，问答模式孩子特别买账，五岁娃每天睡前点名要听两篇。','["/img/p/p1024.svg"]',54,'2026-10-13 20:47:00'),
('goods',1024,1,'绘本书单收割机','/img/a/u1.svg?t=绘本书单收割机',4.5,'内容比老版更新，加了航天和 AI 的章节，铜版纸质量好，就是 8 册看完还想续。',NULL,22,'2026-10-12 09:15:00'),
('goods',1024,3,'小学鸡家长','/img/a/u3.svg?t=小学鸡家长',5.0,'送人自留都合适，注音版一年级自己也能读，快递小哥还帮我送上楼了，好评。',NULL,16,'2026-10-10 17:53:00');

-- 1026 雅马哈 电子琴 61键 初学款
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1026,1,'琴房新学员','/img/a/u1.svg?t=琴房新学员',5.0,'琴键力度反馈比同价位好，音色是雅马哈一贯的干净，送的教学 APP 跟着练一周会弹小星星了。','["/img/p/p1026.svg"]',45,'2026-10-14 12:31:00'),
('goods',1026,2,'给孩子买的爸','/img/a/u2.svg?t=给孩子买的爸',4.5,'八岁女儿学琴入门够用，耳机接口半夜练琴不打扰邻居，琴架安装十分钟搞定。',NULL,26,'2026-10-12 18:09:00'),
('goods',1026,3,'宿舍音乐人','/img/a/u3.svg?t=宿舍音乐人',5.0,'500 种音色玩起来上头，接电脑做 MIDI 也行，初学和娱乐兼顾，性价比高。',NULL,29,'2026-10-11 21:42:00');

-- 1028 小米 智能音箱 Pro 语音助手
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1028,2,'全屋智能初步玩家','/img/a/u2.svg?t=全屋智能初步玩家',5.0,'小爱同学识别率很高，隔两米喊照样响应，接了家里十几个智能设备，一句话全屋开关灯。','["/img/p/p1028.svg"]',63,'2026-10-13 22:05:00'),
('goods',1028,1,'床边闹钟替代品','/img/a/u1.svg?t=床边闹钟替代品',4.5,'音质对得起价格，低音有点惊喜，睡眠模式会自动降低音量，就是米家外生态接入略麻烦。',NULL,24,'2026-10-11 15:38:00'),
('goods',1028,3,'科技早知道','/img/a/u3.svg?t=科技早知道',5.0,'给孩子讲故事、查天气、定闹钟三件套全齐，蓝牙 5.0 连手机放歌也稳，这个价位无敌。',NULL,30,'2026-10-10 13:24:00');

-- 1029 小天鹅 冰箱 452L 十字四门
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1029,1,'厨房收纳控','/img/a/u1.svg?t=厨房收纳控',5.0,'十字门分区科学，452L 对三口之家绰绰有余，风冷无霜 + 双循环不串味，颜值也在线。','["/img/p/p1029.svg"]',38,'2026-10-14 11:18:00'),
('goods',1029,2,'搬新家的人','/img/a/u2.svg?t=搬新家的人',4.5,'嵌入预留位刚好放下，运行声音很小，变温区调到 0 度存肉类特别方便。',NULL,15,'2026-10-12 16:41:00'),
('goods',1029,3,'电费观察员','/img/a/u3.svg?t=电费观察员',5.0,'一级能效一天不到一度电，冷静灰面板大气好擦，以旧换新补贴用下来真香。',NULL,20,'2026-10-11 10:57:00');

-- 1030 李宁 男子速干短袖 T恤
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1030,1,'夜跑十公里','/img/a/u1.svg?t=夜跑十公里',5.0,'速干是真速干，跑完十公里后背只是微潮，十来分钟就干，重量几乎无感。','["/img/p/p1030.svg"]',42,'2026-10-13 21:12:00'),
('goods',1030,2,'球场常客','/img/a/u2.svg?t=球场常客',4.5,'透气孔设计合理，打球出汗不粘身，洗了几次没变形没起球，这个价可以冲。',NULL,18,'2026-10-11 19:26:00'),
('goods',1030,4,'test1','/img/a/u4.svg?t=test1',5.0,'版型不紧不松，175/92A 合身，比优衣库速干系列更凉快，国货厚道。',NULL,13,'2026-10-10 14:39:00');

-- ===================== 6 个只有 1 条评价的商品（各补 2 条） =====================

-- 1006 小米电视 S Pro 75 英寸（原 1 条）
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1006,2,'客厅观影党','/img/a/u2.svg?t=客厅观影党',5.0,'Mini LED 分区控光名不虚传，暗场细节比旧电视多出一个档次，144Hz 打 PS5 顺滑无拖影。','["/img/p/p1006.svg"]',71,'2026-10-13 20:55:00'),
('goods',1006,3,'装机猿','/img/a/u3.svg?t=装机猿',4.5,'音质出乎意料，不用回音壁也够用，系统确实没开机广告，装第三方软件稍微折腾。',NULL,29,'2026-10-11 17:29:00');

-- 1010 戴森 V12 无线吸尘器（原 1 条）
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1010,2,'吸猫爱好者','/img/a/u2.svg?t=吸猫爱好者',5.0,'激光探测让猫毛无处遁形，吸完床底那个集尘筒看着就解压，主机比想象中轻，举着吸天花板不累。','["/img/p/p1010.svg"]',66,'2026-10-13 14:33:00'),
('goods',1010,3,'家务分工失败者','/img/a/u3.svg?t=家务分工失败者',4.5,'续航标准模式够全屋一遍，尘杯单手倾倒很方便，配件收纳架最好提前找地方挂。',NULL,25,'2026-10-11 20:14:00');

-- 1015 兰蔻 小黑瓶精华肌底液（原 1 条）
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1015,1,'成分党小刘','/img/a/u1.svg?t=成分党小刘',5.0,'质地轻薄上脸秒吸收，换季敏感期稳定皮肤全靠它，第二瓶空瓶打卡，官网渠道码验证过。','["/img/p/p1015.svg"]',58,'2026-10-14 08:46:00'),
('goods',1015,3,'熬夜冠军','/img/a/u3.svg?t=熬夜冠军',4.5,'用一个月毛孔视觉上细腻了些，后续叠加面霜不搓泥，就是价格让我只敢省着按。',NULL,21,'2026-10-12 22:36:00');

-- 1016 圣罗兰 口红 小金条（原 1 条）
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1016,2,'口红收集癖','/img/a/u2.svg?t=口红收集癖',5.0,'21 号复古红绝了，哑光不拔干，薄涂日常厚涂气场全开，包装送人也拿得出手。','["/img/p/p1016.svg"]',73,'2026-10-13 18:52:00'),
('goods',1016,1,'黄皮试色机','/img/a/u1.svg?t=黄皮试色机',4.5,'膏体顺滑显色度高，黄皮友好，持妆中上，吃饭后需要补，整体满意。',NULL,27,'2026-10-11 12:58:00');

-- 1025 晨光 中性笔 考试专用（原 1 条）
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1025,2,'刷题机器人','/img/a/u2.svg?t=刷题机器人',5.0,'0.5 考试标配，出墨均匀不断墨，一盒 12 支两个月刷完三本卷子，手不累。',NULL,33,'2026-10-12 19:47:00'),
('goods',1025,3,'监考老师','/img/a/u3.svg?t=监考老师',4.5,'给学生备用的，速干程度不错，左撇子写完基本不糊手，握胶舒适。','["/img/p/p1025.svg"]',17,'2026-10-10 21:03:00');

-- 1027 HUAWEI MatePad 11.5 平板（原 1 条）
INSERT INTO comment (target_type,target_id,user_id,nickname,avatar,score,content,pictures,like_count,create_time) VALUES
('goods',1027,1,'网课搭子','/img/a/u1.svg?t=网课搭子',5.0,'120Hz 纸感护眼屏看网课舒服，配上手写笔记很跟手，分屏做笔记一屏两用，学习平板天花板。','["/img/p/p1027.svg"]',56,'2026-10-13 13:29:00'),
('goods',1027,2,'差旅办公族','/img/a/u2.svg?t=差旅办公族',4.5,'轻薄好带，WPS 配键盘轻度办公够用，续航两天一充，就是扬声器外放一般。',NULL,23,'2026-10-11 16:14:00');
