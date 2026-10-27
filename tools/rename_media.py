import os
import shutil

BASE = r"E:\xiazai\huawei\Java10143\media"

# 关键字 -> 目标文件名（商品）
GOODS_MAP = [
    ("华为旗舰智能手机", "g1001"),
    ("小米旗舰智能手机", "g1002"),
    ("安卓平板电脑", "g1003"),
    ("真无线蓝牙降噪耳机", "g1004"),
    ("智能手表圆形表盘", "g1005"),
    ("75_英寸大屏智能电视", "g1006"),
    ("法式多门冰箱", "g1007"),
    ("滚筒洗衣机", "g1008"),
    ("无线手持杆式吸尘器", "g1010"),
    ("男士运动外套", "g1011"),
    ("女士运动卫衣", "g1012"),
    ("专业跑步鞋", "g1013"),
    ("简约通勤双肩背包", "g1014"),
    ("精华液", "g1015"),
    ("女士速干运动短袖", "g1030"),
    ("中性笔", "g1025"),
    ("电子琴", "g1026"),
    ("智能音箱", "g1028"),
    ("洗发水", "g1017"),
    ("纯牛奶", "g1019"),
    ("每日坚果", "g1020"),
    ("车厘子", "g1021"),
    ("文学小说精装书", "g1023"),
    ("儿童科普绘本", "g1024"),
]

BANNER_MAP = [
    ("新人专享", "b1"),
    ("新款旗舰智能手机", "b2"),
    ("生鲜直营", "b3"),
    ("家电焕新", "b4"),
    ("数码好物", "b5"),
    ("美妆盛典", "b6"),
]


def run(src_dir, mapping, sub_dir):
    src_dir = os.path.join(BASE, src_dir)
    dst_dir = os.path.join(BASE, sub_dir)
    os.makedirs(dst_dir, exist_ok=True)
    files = [f for f in os.listdir(src_dir) if f.lower().endswith(".png")]
    done = []
    for key, target in mapping:
        hit = None
        for f in files:
            if key in f:
                hit = f
                break
        if not hit:
            print("MISS", key)
            continue
        dst = os.path.join(dst_dir, target + ".png")
        shutil.move(os.path.join(src_dir, hit), dst)
        done.append(target)
        print("OK", key, "->", target + ".png")
    left = [f for f in os.listdir(src_dir) if f.lower().endswith(".png")]
    if left:
        print("LEFTOVER", left)
    return done


print("=== goods ===")
run("goods_new", GOODS_MAP, "goods")
print("=== banners ===")
run("banners_new", BANNER_MAP, "banners")
