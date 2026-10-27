# -*- coding: utf-8 -*-
"""把 src/main/resources/static 里已修补的文件更新进 target/Java10143.jar。
只替换内容，保留 JAR 内所有其他条目与元数据不变。"""
import zipfile, os, hashlib, sys

JAR = 'target/Java10143.jar'
SRC = 'src/main/resources/static'
FILES = [
    'assets/detail-Cqm8zf61.css',
    'assets/jd-nav-BHPNEp-P.css',
    'assets/jd-nav.suvkfQh-.js',
    'assets/pages-detail-detail.VIkVJXtK.js',
    'assets/webview-Cuc651CQ.css',
    'coupon.html',
    'live.html',
    'live-room.html',
    'seckill.html',
    'video.html',
    'css/live.css',
    'index.html',
    'request.js',
]
PREFIX = 'BOOT-INF/classes/static/'

# 读取替换内容
repl = {}
for rel in FILES:
    with open(os.path.join(SRC, rel), 'rb') as f:
        repl[PREFIX + rel] = f.read()
    print('read', rel, len(repl[PREFIX + rel]), 'bytes')

zin = zipfile.ZipFile(JAR, 'r')
names = set(zin.namelist())
missing = [k for k in repl if k not in names]
if missing:
    print('ERROR: not in jar:', missing)
    sys.exit(1)

tmp = JAR + '.tmp'
zout = zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED)
replaced = 0
for item in zin.infolist():
    data = repl.get(item.filename)
    if data is not None:
        zi = zipfile.ZipInfo(item.filename, date_time=item.date_time)
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = item.external_attr
        zi.internal_attr = item.internal_attr
        zi.create_system = item.create_system
        zi.extra = item.extra
        zi.comment = item.comment
        zout.writestr(zi, data)
        replaced += 1
    else:
        zi = zipfile.ZipInfo(item.filename, date_time=item.date_time)
        zi.compress_type = item.compress_type
        zi.external_attr = item.external_attr
        zi.internal_attr = item.internal_attr
        zi.create_system = item.create_system
        zi.extra = item.extra
        zi.comment = item.comment
        zout.writestr(zi, zin.read(item.filename))
zout.close()
zin.close()

OUT = JAR
STAGED = 'target/Java10143.patched.jar'
try:
    os.replace(tmp, JAR)
    print('jar replaced in place:', JAR)
except PermissionError:
    os.replace(tmp, STAGED)
    OUT = STAGED
    print('jar is LOCKED by running backend - staged patched jar at:', STAGED)

# 校验：读回与磁盘逐字节对比
z = zipfile.ZipFile(OUT)
bad = z.testzip()
print('zip integrity:', 'OK' if bad is None else ('BAD: ' + bad))
ok = True
for rel in FILES:
    key = PREFIX + rel
    a = hashlib.md5(z.read(key)).hexdigest()
    b = hashlib.md5(open(os.path.join(SRC, rel), 'rb').read()).hexdigest()
    if a != b:
        ok = False
        print('MISMATCH', key)
print('content verify:', 'ALL OK' if ok else 'FAILED')
print('new jar size:', os.path.getsize(JAR))
