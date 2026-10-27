"""从已生成的真实商品图派生分类小图标（media/category/c*.jpg）。

不额外消耗 AI 生成额度：把对应商品图居中裁成 300x300 方图即可。
每个分类挑最贴近它的一张商品图。
"""
import os
import subprocess

FFMPEG = r"C:/Users/lenovo/AppData/Roaming/bilibili/ffmpeg/ffmpeg.exe"
BASE = r"E:/xiazai/huawei/Java10143/media"

# 分类 id -> 源商品图（media/goods/gXXXX.jpg）
MAP = {
    1: "g1001",    # 手机数码
    2: "g1007",    # 家用电器
    3: "g1011",    # 服饰鞋包
    4: "g1015",    # 美妆个护
    5: "g1021",    # 食品生鲜
    6: "g1023",    # 图书文娱
    101: "g1001",  # 手机
    102: "g1003",  # 平板电脑
    103: "g1004",  # 耳机音响
    104: "g1005",  # 智能穿戴
    105: "g1006",  # 电视影音
    106: "g1008",  # 冰箱洗衣机
    107: "g1009",  # 厨房小电
    108: "g1010",  # 生活电器
    109: "g1011",  # 男装
    110: "g1012",  # 女装
    111: "g1013",  # 运动鞋
    112: "g1014",  # 箱包
    113: "g1015",  # 面部护肤
    114: "g1016",  # 彩妆香水
    115: "g1017",  # 洗护发
    116: "g1018",  # 口腔护理
    117: "g1019",  # 牛奶乳品
    118: "g1020",  # 休闲零食
    119: "g1021",  # 新鲜水果
    120: "g1022",  # 咖啡冲饮
    121: "g1023",  # 文学小说
    122: "g1024",  # 少儿读物
    123: "g1025",  # 办公文具
    124: "g1026",  # 乐器
}

dst_dir = os.path.join(BASE, "category")
os.makedirs(dst_dir, exist_ok=True)

ok = 0
for cid, gid in MAP.items():
    src = os.path.join(BASE, "goods", gid + ".jpg")
    dst = os.path.join(dst_dir, "c%d.jpg" % cid)
    if not os.path.exists(src):
        print("MISS", gid)
        continue
    vf = "scale=300:300:force_original_aspect_ratio=increase,crop=300:300"
    cmd = [FFMPEG, "-y", "-loglevel", "error", "-i", src, "-vf", vf, "-q:v", "4", dst]
    subprocess.run(cmd, check=True)
    ok += 1
print("generated", ok, "category icons ->", dst_dir)
