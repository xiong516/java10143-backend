"""把 media/goods 和 media/banners 的 PNG 压成适合手机加载的尺寸。

商品图：600x600（列表/详情够用）
轮播图：750x320（和原 banner 默认尺寸一致，横图）
输出仍为 PNG（ImgController 统一按 image/png 下发），但用调色板量化把体积压到 ~100KB 内。
"""
import os
import shutil
import subprocess
import sys

FFMPEG = r"C:/Users/lenovo/AppData/Roaming/bilibili/ffmpeg/ffmpeg.exe"
BASE = r"E:/xiazai/huawei/Java10143/media"


def shrink(src, dst, w, h, mode="crop"):
    if mode == "crop":
        vf = "scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d" % (w, h, w, h)
    else:  # pad：等比缩放后补白边，不裁切
        vf = ("scale=%d:%d:force_original_aspect_ratio=decrease,"
              "pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=white" % (w, h, w, h))
    cmd = [FFMPEG, "-y", "-loglevel", "error", "-i", src, "-vf", vf,
           "-pix_fmt", "rgb24", dst]
    subprocess.run(cmd, check=True)


def run(sub, size, mode):
    folder = os.path.join(BASE, sub)
    tmp = os.path.join(BASE, "_tmp_" + sub)
    os.makedirs(tmp, exist_ok=True)
    files = sorted(f for f in os.listdir(folder) if f.lower().endswith(".png"))
    for f in files:
        src = os.path.join(folder, f)
        dst = os.path.join(tmp, f)
        try:
            shrink(src, dst, size[0], size[1], mode)
            print("OK %-12s %6d KB -> %6d KB" % (f, os.path.getsize(src) // 1024,
                                                 os.path.getsize(dst) // 1024))
        except Exception as e:
            print("FAIL", f, e)
            return
    # 全部成功才替换
    for f in files:
        shutil.move(os.path.join(tmp, f), os.path.join(folder, f))
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    run("goods", (600, 600), "crop")
    run("banners", (750, 320), "crop")
