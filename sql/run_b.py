# -*- coding: utf-8 -*-
"""建表 + 造种子数据（B 方案：优惠券 / 秒杀 / 价格走势）"""
import subprocess, sys, io

MYSQL = r"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
PWD = "".join(chr(49 + i) for i in range(6))

def run(sql, db="java10143"):
    p = subprocess.run([MYSQL, "-uroot", "-p" + PWD, "-D", db,
                        "--default-character-set=utf8mb4", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = (p.stdout or "").strip()
    err = (p.stderr or "").strip()
    if out: print(out)
    if err and "Warning" not in err.split("\n")[0]: print("ERR:", err)
    return p.returncode

def run_file(path):
    p = subprocess.run([MYSQL, "-uroot", "-p" + PWD, "-D", "java10143",
                        "--default-character-set=utf8mb4"],
                       stdin=open(path, "rb"), capture_output=True)
    if p.returncode != 0:
        print("ERR:", p.stderr.decode("utf-8", "replace")[:600])
    else:
        print("OK " + path)
    return p.returncode

if __name__ == "__main__":
    run_file("sql/b_shop_play.sql")
    run("show tables like 'coupon'; show tables like 'seckill'; show tables like 'price_history'; show tables like 'user_coupon';")
