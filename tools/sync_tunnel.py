#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
sync_tunnel.py —— cpolar 隧道域名变了以后，一键同步到所有前端源码

背景：
  cpolar 免费版的公网域名是和"隧道会话"绑定的，cpolar 进程一重启
  （电脑重启 / 手动关掉 / 服务崩溃重连 / 免费版会话到期）就会重新分配一个
  随机域名，例如 103a525a.r8.cpolar.top -> 4c308390.r8.cpolar.top。
  域名一变，所有写死了旧域名的地方都会连不上（腾讯/微信端表现为 502、超时）。

  本脚本从 cpolar 自己的日志里读出"最近一次分配到的域名"，然后改写三个源码文件：
    1. 微信小程序      WeChatProjects/miniprogram-jd/utils/api.js
    2. 鸿蒙 App        MyApplication10143/entry/src/main/ets/util/myreqest.ets
    3. uni-app         Java10143/uni-app/api/request.js
  （uni-app 和小程序里都有"局域网优先 / 自动优选"逻辑，隧道只是其中一条线路，
    所以只改域名常量即可，不需要动逻辑。）

用法：
  python sync_tunnel.py            # 读取日志最新域名并同步
  python sync_tunnel.py --check    # 只看当前域名，不改文件
  python sync_tunnel.py --domain xxx.r8.cpolar.top   # 手动指定域名
"""
import glob
import io
import os
import re
import socket
import sys
import time
import urllib.request

LOG_DIR = r"C:\Users\lenovo\.cpolar\logs"
DOMAIN_RE = re.compile(r"([a-z0-9]{6,12}\.r\d+\.cpolar\.top)")

TARGETS = [
    (r"E:\Users\lenovo\WeChatProjects\miniprogram-jd\utils\api.js", "微信小程序"),
    (r"E:\xiazai\huawei\MyApplication10143\entry\src\main\ets\util\myreqest.ets", "鸿蒙 App"),
    (r"E:\xiazai\huawei\Java10143\uni-app\api\request.js", "uni-app"),
]


def latest_domain():
    """从日志里取最后一次出现的隧道域名（日志是追加写，最后一行最新）"""
    files = glob.glob(os.path.join(LOG_DIR, "cpolar_service.log.*"))
    files = [f for f in files if os.path.isfile(f)]
    if not files:
        return None
    files.sort(key=lambda p: os.path.getmtime(p))
    found = None
    for path in files[-2:]:  # 看最近两个（跨天日志滚动时也能拿到）
        try:
            with io.open(path, "r", encoding="utf-8", errors="replace") as f:
                for line in f:
                    m = DOMAIN_RE.search(line)
                    if m:
                        found = m.group(1)
        except Exception:
            pass
    return found


def port_open(host, port, timeout=1.2):
    """本机端口是否在监听"""
    try:
        with socket.create_connection((host, port), timeout):
            return True
    except Exception:
        return False


def alive(domain, timeout=15, tries=2):
    """隧道冷启动比较慢（常 8~12 秒），给两次机会"""
    url = "https://%s/home/banner?distributionSite=1" % domain
    last = ""
    for i in range(tries):
        try:
            with urllib.request.urlopen(url, timeout=timeout) as r:
                body = r.read(200).decode("utf-8", "replace")
                return ('"code":"1"' in body), "HTTP %s" % r.status
        except Exception as e:
            last = str(e)[:80]
            if i + 1 < tries:
                time.sleep(1)
    return False, last


def patch(path, new_domain):
    """返回 (命中处数, 是否真的改了)"""
    if not os.path.isfile(path):
        return None
    with io.open(path, "r", encoding="utf-8", errors="replace", newline="") as f:
        text = f.read()
    hits = re.findall(r"[a-z0-9]{6,12}\.r\d+\.cpolar\.top", text)
    if not hits:
        return (0, False)
    new_text = re.sub(r"[a-z0-9]{6,12}\.r\d+\.cpolar\.top", new_domain, text)
    changed = new_text != text
    if changed:
        with io.open(path, "w", encoding="utf-8", newline="") as f:
            f.write(new_text)
    return (len(hits), changed)


def main():
    args = sys.argv[1:]
    check_only = "--check" in args
    manual = None
    if "--domain" in args:
        i = args.index("--domain")
        if i + 1 < len(args):
            manual = args[i + 1].strip().replace("https://", "").replace("http://", "").strip("/")

    domain = manual or latest_domain()
    if not domain:
        print("[X] 没能从日志里读到隧道域名，请到 cpolar 面板复制当前公网地址后：")
        print("    python sync_tunnel.py --domain xxxx.r8.cpolar.top")
        return 2

    print("[i] 当前隧道域名：%s" % domain)

    # 先看后端和 cpolar 到底在不在，比"连通性不通"这种笼统提示有用
    back_up = port_open("127.0.0.1", 8080)
    cpolar_up = port_open("127.0.0.1", 9200)
    print("[i] 后端 8080   ：%s" % ("已启动" if back_up else "没启动 —— 先双击桌面「京东8080bat.bat」"))
    print("[i] cpolar 服务：%s" % ("运行中" if cpolar_up else "没运行 —— 先启动 cpolar，否则这个域名已经失效"))

    ok, info = alive(domain)
    print("[i] 隧道连通自检：%s（%s）" % ("可用" if ok else "不通", info))
    if not ok:
        if not back_up:
            print("    * 原因很明确：本机 8080 没监听，隧道转发进来只能回 502。")
        elif not cpolar_up:
            print("    * cpolar 没跑，隧道地址肯定连不上。")
        else:
            print("    * 两者都在但隧道不通：域名可能已经过期，先用 --domain 手动填 cpolar 面板上的新地址。")

    if check_only:
        return 0

    total = 0
    for path, name in TARGETS:
        res = patch(path, domain)
        if res is None:
            print("[!] %-10s 文件不存在：%s" % (name, path))
            continue
        n, changed = res
        if n == 0:
            print("[-] %-10s 没有写死的隧道域名，跳过" % name)
        elif changed:
            total += n
            print("[OK] %-10s 已更新 %d 处" % (name, n))
        else:
            print("[=] %-10s 已经是最新（%d 处）" % (name, n))
    print("\n[完成] 本次改动 %d 处。接下来：" % total)
    print("  1) 微信开发者工具里点「编译」重新加载小程序")
    print("  2) 鸿蒙端重新 Run 一次（DevEco）")
    print("  3) uni-app / H5 重新构建")
    return 0


if __name__ == "__main__":
    sys.exit(main())
