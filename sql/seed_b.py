# -*- coding: utf-8 -*-
"""B 方案种子数据：优惠券模板 / 秒杀场次 / 90 天价格走势"""

import subprocess
from datetime import datetime, timedelta, date
import random

MYSQL = r"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
PWD = "".join(chr(49 + i) for i in range(6))
DB = "java10143"


def run(sql):
    p = subprocess.run([MYSQL, "-uroot", "-p" + PWD, "-D", DB,
                        "--default-character-set=utf8mb4", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = (p.stdout or "").strip()
    err = (p.stderr or "").strip()
    if out:
        print(out)
    if err and "Using a password" not in err:
        print("ERR:", err[:800])


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "\\'")


now = datetime.now()


def f(dt):
    return dt.strftime("%Y-%m-%d %H:%M:%S")


# ---------------- 1. 优惠券 ----------------
COUPONS = [
    # name, type, threshold, amount, discount, max_discount, scope, category_id, total, per_limit, 有效天数
    ("新人首单礼 · 无门槛 10 元", 3, 0, 10, 10, 0, "all", None, 9999, 1, 30),
    ("满 199 减 20", 1, 199, 20, 10, 0, "all", None, 5000, 2, 15),
    ("满 499 减 50", 1, 499, 50, 10, 0, "all", None, 3000, 2, 15),
    ("满 999 减 120", 1, 999, 120, 10, 0, "all", None, 2000, 1, 20),
    ("满 2999 减 400（手机专享）", 1, 2999, 400, 10, 0, "category", 101, 800, 1, 20),
    ("电脑办公 · 满 2000 打 9 折", 2, 2000, 0, 9, 500, "category", 106, 600, 1, 20),
    ("家电家居 · 满 500 减 80", 1, 500, 80, 10, 0, "category", 102, 1200, 2, 20),
    ("全场 95 折（最高减 200）", 2, 100, 0, 9.5, 200, "all", None, 4000, 1, 10),
    ("大额神券 · 满 5999 减 800", 1, 5999, 800, 10, 0, "all", None, 300, 1, 25),
]

vals = []
for name, typ, th, amt, disc, maxd, scope, cid, total, per, days in COUPONS:
    vals.append(
        "('{}',{}, {:.2f}, {:.2f}, {:.2f}, {:.2f}, '{}', {}, {}, 0, 0, {}, '{}', '{}', 1)".format(
            esc(name), typ, th, amt, disc, maxd, scope,
            "NULL" if cid is None else cid, total, per,
            f(now - timedelta(days=1)), f(now + timedelta(days=days))))

run("DELETE FROM coupon;")
run("INSERT INTO coupon (name,type,threshold,amount,discount,max_discount,scope,category_id,"
    "total_count,taken_count,used_count,per_limit,start_time,end_time,status) VALUES "
    + ",".join(vals) + ";")

# ---------------- 2. 秒杀场次 ----------------
# 8 场：2 场进行中、3 场即将开始、3 场已结束（历史场次用来撑数据）
SEC = [
    # spu, sku_offset, price, origin, stock, sold, title, 开始偏移(小时), 结束偏移(小时)
    (1001, -0.6, 5499.00, 6499.00, 60, 41, "整点秒杀 · 百亿补贴", 0, 3),
    (1004, -0.8, 699.00, 899.00, 120, 96, "爆款直降", 0, 2),
    (1009, -0.5, 199.00, 299.00, 200, 87, "上半场 · 手慢无", 0, 4),
    (1005, 0.0, 1999.00, 2688.00, 80, 0, "10 点场 · 冰点价", 1, 5),
    (1015, 0.0, 799.00, 1080.00, 100, 0, "限量 100 台", 1, 4),
    (1027, 0.0, 1499.00, 1999.00, 50, 0, "午间闪购", 2, 6),
    (1013, 0.0, 599.00, 799.00, 300, 218, "昨日爆款返场", -6, -2),
    (1016, 0.0, 269.00, 369.00, 250, 250, "已抢光", -8, -4),
    (1011, 0.0, 349.00, 499.00, 150, 132, "昨日 20 点场", -10, -6),
]

rows = []
for spu_id, off, price, origin, stock, sold, title, sh, eh in SEC:
    # 找该 spu 的第一个 sku
    p = subprocess.run([MYSQL, "-uroot", "-p" + PWD, "-D", DB, "--default-character-set=utf8mb4",
                        "-N", "-e", "select id,price from sku where spu_id=%d order by id limit 1;" % spu_id],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    line = (p.stdout or "").strip()
    if not line:
        continue
    sku_id = line.split("\t")[0]
    rows.append("({},{},'{}',{:.2f},{:.2f},{},{},{},'{}','{}')".format(
        spu_id, sku_id, esc(title), price, origin, stock, sold, 1,
        f(now + timedelta(hours=sh)), f(now + timedelta(hours=eh))))

run("DELETE FROM seckill;")
run("INSERT INTO seckill (spu_id,sku_id,title,price,origin_price,stock,sold,limit_per,start_time,end_time) VALUES "
    + ",".join(rows) + ";")

# ---------------- 3. 价格走势（近 90 天） ----------------
random.seed(10143)
hp = subprocess.run([MYSQL, "-uroot", "-p" + PWD, "-D", DB, "--default-character-set=utf8mb4",
                     "-N", "-e", "select id, price from spu order by id;"],
                    capture_output=True, text=True, encoding="utf-8", errors="replace")

ph_rows = []
today = date.today()
for line in (hp.stdout or "").strip().split("\n"):
    parts = line.strip().split("\t")
    if len(parts) < 2:
        continue
    spu_id = int(parts[0])
    base = float(parts[1])
    cur = base
    for i in range(89, -1, -1):
        d = today - timedelta(days=i)
        # 大促日（每月 15、28 号）降价，其余日子小幅波动
        if d.day in (15, 28):
            price = base * random.uniform(0.86, 0.93)
        else:
            drift = random.uniform(-0.012, 0.012)
            cur = max(base * 0.88, min(base * 1.06, cur * (1 + drift)))
            price = cur
        ph_rows.append("({}, {:.2f}, '{}')".format(spu_id, round(price, 2), d.isoformat()))

run("DELETE FROM price_history;")
# 分批插入，避免单条 SQL 过长
BATCH = 400
for i in range(0, len(ph_rows), BATCH):
    run("INSERT INTO price_history (spu_id,price,stat_date) VALUES "
        + ",".join(ph_rows[i:i + BATCH]) + ";")

run("select count(*) coupons from coupon; select count(*) seckills from seckill; select count(*) prices from price_history;")
run("select id,spu_id,price,origin_price,stock,sold,start_time,end_time from seckill;")
