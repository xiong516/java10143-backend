# -*- coding: utf-8 -*-
"""
给 30 个商品生成真实详情图（详情 tab 用的 dSPU_N 图片）。

原理：数据库 detail_pictures 存的是 /img/p/d1001_1.svg，
后端 ImgController 提取数字的方式是「去掉所有非数字」→ d1001_1 -> 10011，
然后按 g<数字>.jpg 去 media/goods/ 找真实图。
所以这里把主图 g1001.jpg 裁出 3 张不同视角的细节图，
存成 g10011.jpg / g10012.jpg / g10013.jpg（= "1001"+"1" 拼接），
后端不用改代码、不用重启，下次请求就出真图。
"""
import os
import re
from PIL import Image, ImageEnhance

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # Java10143/
GOODS_DIR = os.path.join(BASE, "media", "goods")
TSV = r"E:\xiazai2\2026-10-24-22-41-23\detail_pics.tsv"
OUT_W = 790  # 统一输出宽度


def detail_seeds(tsv_path):
    """从 mysql 导出的 TSV 里解析每个商品的 dSPU_N seed 列表"""
    result = {}
    pat = re.compile(r"/img/p/d(\d+)_(\d+)\.(?:svg|png)")
    with open(tsv_path, "r", encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\n").split("\t", 1)
            if len(parts) < 2:
                continue
            spu = parts[0].strip()
            seeds = [(m.group(1), m.group(2)) for m in pat.finditer(parts[1])]
            if seeds:
                result[spu] = [s[1] for s in seeds]  # 只要 idx
    return result


def crop_variant(src, idx, total):
    """按序号裁出不同视角：1 整图、2 中部放大、3 局部细节、其余整图"""
    w, h = src.size
    if idx == 1 or total == 1:
        box = (0, 0, w, h)                      # 整图
    elif idx == 2:
        cw, ch = int(w * 0.62), int(h * 0.62)   # 中心放大
        box = ((w - cw) // 2, (h - ch) // 2, (w + cw) // 2, (h + ch) // 2)
    elif idx == 3:
        cw, ch = int(w * 0.66), int(h * 0.5)    # 上半部分细节
        box = (0, int(h * 0.06), cw, int(h * 0.06) + ch)
    else:
        cw, ch = int(w * 0.66), int(h * 0.5)    # 下半部分细节
        box = (w - cw, h - ch - int(h * 0.06), w, h - int(h * 0.06))

    img = src.crop(box)
    ratio = OUT_W / img.width
    img = img.resize((OUT_W, max(1, int(img.height * ratio))), Image.LANCZOS)
    # 轻微增强，让详情图比主图更「商品图」一点
    img = ImageEnhance.Sharpness(img).enhance(1.25)
    img = ImageEnhance.Color(img).enhance(1.08)
    return img


def main():
    seeds = detail_seeds(TSV)
    ok, miss = 0, []
    for spu, idxs in sorted(seeds.items()):
        src_path = os.path.join(GOODS_DIR, "g%s.jpg" % spu)
        if not os.path.exists(src_path):
            miss.append(spu)
            continue
        src = Image.open(src_path).convert("RGB")
        for idx in idxs:
            # d1001_1 -> 数字 10011 = spu + idx 直接拼接（和后端提取规则一致）
            out_name = "g%s%s.jpg" % (spu, idx)
            out_path = os.path.join(GOODS_DIR, out_name)
            crop_variant(src, int(idx), len(idxs)).save(
                out_path, "JPEG", quality=88)
            ok += 1
    print("生成详情图 %d 张" % ok)
    if miss:
        print("缺主图的商品: %s" % ",".join(miss))


if __name__ == "__main__":
    main()
