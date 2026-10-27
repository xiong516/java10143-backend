# -*- coding: utf-8 -*-
"""
直播带货种子数据：6 个直播间（2 直播中 / 2 预告 / 2 已结束）+ 挂车商品 + 弹幕
商品全部取自现有 spu，不造假数据。
"""
import subprocess
import random
import datetime

MYSQL = r'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe'
PW = ''.join(chr(49 + i) for i in range(6))
DB = 'java10143'


def run(sql, fetch=False):
    args = [MYSQL, '-uroot', '-p' + PW, '--default-character-set=utf8mb4', DB, '-e', sql]
    r = subprocess.run(args, capture_output=True, encoding='utf-8', errors='replace')
    if r.returncode != 0:
        raise RuntimeError(r.stderr[:800])
    return r.stdout


def q(sql):
    out = run(sql.replace(';', '') + '\\G' if False else sql)
    return out


# ---------- 1. 取商品和 sku ----------
rows = run("SELECT id, name, price FROM spu ORDER BY id;")
spus = []
for line in rows.strip().split('\n')[1:]:
    parts = line.split('\t')
    if len(parts) >= 3:
        spus.append((int(parts[0]), parts[1], float(parts[2])))
print('spu 可用：', len(spus))

sku_rows = {}
for sid, _, _ in spus:
    out = run(f"SELECT id, price, inventory FROM sku WHERE spu_id={sid} ORDER BY price LIMIT 1;")
    lines = out.strip().split('\n')
    if len(lines) >= 2:
        p = lines[1].split('\t')
        sku_rows[sid] = (int(p[0]), float(p[1]), int(p[2]) if p[2] not in ('NULL', '') else 100)

# ---------- 2. 清空重建 ----------
run("DELETE FROM live_message; DELETE FROM live_goods; DELETE FROM live_room;")
run("ALTER TABLE live_room AUTO_INCREMENT=1; ALTER TABLE live_goods AUTO_INCREMENT=1; ALTER TABLE live_message AUTO_INCREMENT=1;")

now = datetime.datetime.now()

ROOMS = [
    # title, anchor, intro, status, viewer, online, like, duration_min(start offset), goods_n
    ('京东10143官方直播间 · 双11数码狂欢夜', '京东小鸽', '官方自营 正品保障 · 整点抽免单', 1, 128640, 3821, 96240, -92, 5),
    ('家电焕新专场 | 以旧换新最高补 800', '家电星推官', '空调冰箱洗衣机 · 送货上门包安装', 1, 86210, 2145, 53180, -48, 4),
    ('手机影像旗舰实测 · 夜景样张直出', '数码阿伟', '真机实测 不吹不黑', 0, 0, 0, 0, 35, 3),
    ('双11 预售 | 电脑办公一站式配齐', '办公研究所', '笔记本/显示器/外设 全场分期免息', 0, 0, 0, 0, 120, 3),
    ('（回放）国庆黄金周 · 美食家电专场', '家电星推官', '已结束 · 可看回放', 2, 45230, 0, 21870, -2880, 3),
    ('（回放）开学季 · 学生党平价款清单', '京东小鸽', '已结束 · 可看回放', 2, 31980, 0, 14620, -5760, 3),
]

DANMU = [
    '主播这个尺寸是多大的', '已下单，就等发货了', '能不能上个优惠券', '这个价格比平时便宜多少',
    '刚抢到一台，太香了', '求讲解一下售后政策', '有免息分期吗', '北京能次日达吗',
    '颜色还有货吗', '比隔壁便宜，冲了', '主播声音好好听', '蹲一个 1 号链接',
    '这个和上一代差在哪', '尺码建议拍大一码吗', '库存还有多少', '刚拍了两件，谢谢主播',
    '能不能再便宜点', '包邮吗', '支持以旧换新吗', '来晚了，还有优惠吗',
    '刚才那个商品下架了吗', '想看看实物细节', '发货快吗急用', '质量怎么样，用过的说说',
]
NICKS = ['小*鱼', 'jd***88', '阿*强', '一*一', '用*户', 'm***o', '大*白', '橙*子',
         '路*人', '飞*扬', '今*天', '暖*冬', '星*辰', '大*王', '青*柠']

gid = 0
mid = 0
for (title, anchor, intro, status, viewer, online, like, off, gn) in ROOMS:
    st = (now + datetime.timedelta(minutes=off)).strftime('%Y-%m-%d %H:%M:%S')
    et = None
    if status == 2:
        et = (now + datetime.timedelta(minutes=off) + datetime.timedelta(minutes=150)).strftime('%Y-%m-%d %H:%M:%S')
    elif status == 1:
        et = (now + datetime.timedelta(minutes=off) + datetime.timedelta(minutes=180)).strftime('%Y-%m-%d %H:%M:%S')
    else:
        et = (now + datetime.timedelta(minutes=off) + datetime.timedelta(minutes=120)).strftime('%Y-%m-%d %H:%M:%S')

    pick = [s for s in spus if s[0] in sku_rows][gid * 2 % max(1, len(spus) - gn):][:gn]
    if len(pick) < gn:
        pick = [s for s in spus if s[0] in sku_rows][:gn]
    gid += gn
    main_spu = pick[0][0] if pick else 'NULL'

    sql = (
        "INSERT INTO live_room(title,cover,anchor_name,anchor_avatar,intro,spu_id,status,"
        "viewer_count,online_count,like_count,goods_count,start_time,end_time,sort) VALUES("
        f"'{title}',NULL,'{anchor}',NULL,'{intro}',{main_spu},{status},{viewer},{online},{like},{len(pick)},"
        f"'{st}',{'NULL' if et is None else chr(39) + et + chr(39)},{ROOMS.index((title, anchor, intro, status, viewer, online, like, off, gn))});"
    )
    run(sql)
    out = run("SELECT LAST_INSERT_ID();").strip().split('\n')
    room_id = int(out[1]) if len(out) > 1 else 1
    if room_id == 0:
        room_id = int(run("SELECT MAX(id) FROM live_room;").strip().split('\n')[1])

    # 挂车
    for i, (sid, sname, sprice) in enumerate(pick):
        sk = sku_rows[sid]
        live_price = round(sk[1] * (0.72 if i == 0 else random.uniform(0.80, 0.93)), 2)
        stock = 60 + i * 20
        sold = 0 if status == 0 else random.randint(12, 58)
        run("INSERT INTO live_goods(room_id,spu_id,sku_id,live_price,stock,sold,sort,is_spot) VALUES("
            f"{room_id},{sid},{sk[0]},{live_price},{stock},{sold},{i},{1 if i == 0 else 0});")

    # 弹幕（直播中/已结束的才有）
    if status != 0:
        n = 14 if status == 1 else 8
        base = (now - datetime.timedelta(minutes=40 if status == 1 else 3000))
        for i in range(n):
            txt = DANMU[(room_id * 3 + i) % len(DANMU)]
            nick = NICKS[(room_id + i) % len(NICKS)]
            t = (base + datetime.timedelta(seconds=i * 47)).strftime('%Y-%m-%d %H:%M:%S')
            run("INSERT INTO live_message(room_id,user_id,nickname,content,type,create_time) VALUES("
                f"{room_id},NULL,'{nick}','{txt}',0,'{t}');")

print(run("SELECT id,title,status,anchor_name,goods_count,viewer_count FROM live_room;"))
print(run("SELECT COUNT(*) goods FROM live_goods;"))
print(run("SELECT COUNT(*) msgs FROM live_message;"))
