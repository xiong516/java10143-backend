# -*- coding: utf-8 -*-
"""把静态图片合成的种子短视频重做成多镜头 Ken Burns 动态片。

每个商品取 主图 + 3 张详情图，各生成 3.2s 运镜段（推近/拉远/横移），
淡化转场拼接 + 字幕轮播，覆盖 media/video/vXXXX.mp4。
用法: python make_seed_videos.py [spuId ...]   缺省处理 1005 1006 1009 1010 1015 1016
"""
import json
import os
import shutil
import subprocess
import sys
import urllib.request

BASE = "http://127.0.0.1:8080"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MEDIA = os.path.join(ROOT, "media", "video")
TMP = os.path.join(MEDIA, "_rebuild_tmp")
FF = r"C:\Users\lenovo\AppData\Roaming\bilibili\ffmpeg\ffmpeg.exe"
FONT = r"C:\Windows\Fonts\simhei.ttf"   # 单字面 ttf，老 ffmpeg drawtext 才认；中文可用
FONT_ESC = "C\\:/Windows/Fonts/simhei.ttf"

SPUS = [int(x) for x in sys.argv[1:]] or [1005, 1006, 1009, 1010, 1015, 1016]
D = 96            # 每段帧数 (3.2s @30fps)
SEG = 3.2
XFD = 0.4
SIZE = "720x1280"


def sh(args):
    env = dict(os.environ)
    env["FONTCONFIG_FILE"] = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts.conf")
    p = subprocess.run(args, capture_output=True, text=True, env=env)
    if p.returncode != 0:
        raise RuntimeError(args[0] + " failed:\n" + (p.stderr or "")[-1200:])
    return p


def fetch(path_no_query, dst):
    url = BASE + path_no_query.rsplit("?", 1)[0]
    if url.lower().endswith(".svg"):
        url = url[:-4] + ".png"          # ImgController 一律输出 PNG
    req = urllib.request.Request(url, headers={"User-Agent": "seed-rebuild"})
    with urllib.request.urlopen(req, timeout=20) as r, open(dst, "wb") as f:
        f.write(r.read())
    if os.path.getsize(dst) < 500:
        raise RuntimeError("image too small: " + url)


def pics_of(spu):
    out = subprocess.run(
        [r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
         "--default-character-set=utf8mb4", "-N", "-uroot",
         "-p" + "".join(chr(c) for c in [49, 50, 51, 52, 53, 54]),
         "-e", "use java10143; select main_pictures, detail_pictures from spu where id=%d;" % spu],
        capture_output=True, text=True).stdout.replace("\r", "")
    parts = [x for x in out.strip().split("\t") if x.strip()]
    main, detail = parts[0], parts[1] if len(parts) > 1 else "[]"
    arr = json.loads(main)          # 详情图是占位图（文件名渐变图），不进片子里
    return [a for a in arr if a][:4]


def motion(i):
    """返回 zoompan 的 z/x/y 表达式（同一张主图的四种运镜+取景位置）。"""
    cx = "(iw-iw/zoom)/2"
    if i % 4 == 0:      # 居中推近
        return "min(1+0.0035*on,1.4)", cx, "(ih-ih/zoom)/2"
    if i % 4 == 1:      # 居中拉远
        return "max(1.4-0.0035*on,1.0)", cx, "(ih-ih/zoom)/2"
    if i % 4 == 2:      # 偏下取景 横移
        return "1.3", "(iw-iw/zoom)*on/%d" % D, "(ih-ih/zoom)*0.72"
    return "min(1.15+0.002*on,1.35)", "(iw-iw/zoom)*(1-on/%d)" % D, "(ih-ih/zoom)*0.2"


def cover_bg_chain(idx):
    """模糊背景铺满 + 前景完整contain + 放大到 2880x5120 供 zoompan 平滑采样。"""
    return (
        "[0:v]scale=720:1280:force_original_aspect_ratio=increase,"
        "crop=720:1280,boxblur=24:6[bg%d];"
        "[0:v]scale=720:1280:force_original_aspect_ratio=decrease[fg%d];"
        "[bg%d][fg%d]overlay=(W-w)/2:(H-h)/2,scale=2880:5120,setsar=1[up%d]"
    ) % (idx, idx, idx, idx, idx)


def build_video(spu, title):
    imgs = []
    for i, p in enumerate(pics_of(spu)):
        dst = os.path.join(TMP, "%d_%d.png" % (spu, i))
        fetch(p, dst)
        imgs.append(dst)
    if len(imgs) == 1:              # 只有一张主图 → 复制 4 份做四段运镜
        imgs = imgs * 4
    if len(imgs) < 4:
        imgs = (imgs * 4)[:4]
    print("  images:", len(imgs))

    tones = ["eq=saturation=1.06", "eq=saturation=0.92:brightness=0.02",
             "eq=saturation=1.12:contrast=1.05", "eq=saturation=1.0:brightness=-0.02"]
    segs = []
    for i, img in enumerate(imgs):
        z, x, y = motion(i)
        seg = os.path.join(TMP, "%d_seg%d.mp4" % (spu, i))
        fade = "fade=t=in:st=0:d=0.3,fade=t=out:st=%.2f:d=0.3," % (SEG - 0.3)
        sh([FF, "-y", "-i", img,
            "-filter_complex", cover_bg_chain(i)
                  + ";[up%d]zoompan=z='%s':x='%s':y='%s':d=%d:s=%s:fps=30,"
                    % (i, z, x, y, D, SIZE)
                  + tones[i % 4] + "," + fade + "format=yuv420p[v]",
            "-map", "[v]", "-frames:v", str(D),
            "-c:v", "libx264", "-preset", "fast", "-crf", "20", seg])
        segs.append(seg)

    out = os.path.join(MEDIA, "v%d.mp4" % spu)
    n = len(segs)

    # 1) 无损拼接（各段编码参数一致）→ 2) 重新编码叠字幕
    merged = os.path.join(TMP, "%d_merged.mp4" % spu)
    lst = os.path.join(TMP, "%d_list.txt" % spu)
    with open(lst, "w", encoding="utf-8") as f:
        for s in segs:
            f.write("file '" + s.replace("\\", "/") + "'\n")
    sh([FF, "-y", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy", merged])

    total = round(n * SEG, 2)
    slot = total / 4.0
    slogans = ["京东10143 · 官方正品", "直播间专属优惠价", "限时抢购 立即下单", "7天无理由退换"]
    dts = ["drawtext=fontfile='%s':text='%s':fontcolor=white:fontsize=40:"
           "box=1:boxcolor=black@0.38:boxborderw=16:x=(w-text_w)/2:y=96" % (FONT_ESC, title)]
    for i, s in enumerate(slogans):
        a = round(i * slot + 0.4, 2)
        b = round((i + 1) * slot - 0.4, 2)
        dts.append("drawtext=fontfile='%s':text='%s':fontcolor=white:fontsize=46:"
                   "box=1:boxcolor=black@0.42:boxborderw=18:x=(w-text_w)/2:y=1040:"
                   "enable='between(t,%s,%s)'" % (FONT_ESC, s, a, b))
    fc = ",".join(dts) + ",format=yuv420p"

    sh([FF, "-y", "-i", merged, "-vf", fc,
        "-c:v", "libx264", "-preset", "medium", "-crf", "23",
        "-maxrate", "3M", "-bufsize", "6M",
        "-movflags", "+faststart", "-r", "30", out])
    return out, total


def main():
    os.makedirs(TMP, exist_ok=True)
    for spu in SPUS:
        title = {1005: "HUAWEI WATCH GT 5 Pro", 1006: "小米电视 S Pro 75",
                 1009: "美的空气炸锅 5.5L", 1010: "戴森 V12 无线吸尘器",
                 1015: "兰蔻小黑瓶精华", 1016: "圣罗兰小金条"}.get(spu, "京东10143")
        print("spu %d (%s) ..." % (spu, title))
        out, total = build_video(spu, title)
        print("  OK -> %s  %.1fs  %.0fKB" % (out, total, os.path.getsize(out) / 1024))
    shutil.rmtree(TMP, ignore_errors=True)
    print("ALL DONE")


if __name__ == "__main__":
    main()
