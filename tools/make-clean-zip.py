# -*- coding: utf-8 -*-
"""
生成一份可以放心上传的干净压缩包（交作业 / 传 GitHub / 发同事）。

做三件事：
  1. 排除隐私文件（数据库配置、浏览器 profile、手机截图、崩溃日志、备份源码）
  2. 排除体积过大的目录（target / node_modules / 视频 / 误放的 Python 库）
  3. 脱敏：把 yml 里的数据库密码、JWT 密钥，sql 里的密码哈希，bat 里的明文密码
     全部替换成占位符（正则匹配，脚本本身不含任何真实密码）

用法：
    python tools/make-clean-zip.py              # 默认输出到 E:\\xiazai\\huawei\\Java10143-clean-日期.zip
    python tools/make-clean-zip.py --out xxx.zip
    python tools/make-clean-zip.py --dry-run    # 只打印将打包/排除的内容，不生成 zip

隐私说明：脚本只读取项目文件，不联网、不上传任何东西。
"""
import os
import re
import sys
import zipfile
import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ---------- 排除规则 ----------
# 目录名前缀匹配（相对 ROOT，用 / 分隔）
EXCLUDE_DIRS = [
    'target',                        # Maven 编译产物（里面也有 application.yml）
    'backup-springboot3-src',        # 上一版源码备份，含明文密码
    'docs/shots/profile',            # Chrome 浏览器配置：cookie / 保存的密码 / 历史记录
    'docs/京东商品图片/.pylibs',      # 误放进来的 Python 库，158MB
    'uni-app/node_modules',
    'uni-app/unpackage',
    'uni-app/dist',
    'ai-service/__pycache__',
    'tools/__pycache__',
    '.github/modernize',             # IDE 升级插件留下的日志和 hooks，不属于项目
    '.git',
    '.idea',
    '.vscode',
    'media',                         # 141MB 视频素材，单独拷贝即可
]

# 文件名规则（正则，匹配 basename）
EXCLUDE_FILE_PATTERNS = [
    r'^phone_screen\d*\.png$',       # 手机截图（可能含状态栏/通知）
    r'^验证-.*\.png$',
    r'.*\.log$',                     # 运行日志 / JVM 崩溃转储
    r'^replay_pid\d+\.log$',
    r'^hs_err_pid\d+\.log$',
    r'^docs/京东商品图片\.zip$',
    r'^sql/java10143\.sql$',         # 含 user 表 md5 哈希，脱敏后另存
    r'\.jar$', r'\.class$', r'\.war$',
]

# ---------- 脱敏规则 ----------
# (文件匹配正则, 替换规则列表[(正则, 替换)])  —— 不对真实密码做硬编码
SANITIZE = {
    r'application\.ya?ml$': [
        # 数据库密码：${JAVA10143_DB_PASSWORD:真实密码}  ->  ${JAVA10143_DB_PASSWORD:请填写本机MySQL密码}
        (r'(\$\{JAVA10143_DB_PASSWORD:)[^}]*(\})', r'\1请填写本机MySQL密码\2'),
        # 裸写的 password: xxx
        (r'(\n\s+password:\s*)(?!\$\{)[^\s#\n]+', r'\1请填写本机MySQL密码'),
        # JWT 密钥
        (r'(\n\s+secret:\s*)(?!\$\{)[^\s#\n]+', r'\1${JAVA10143_JWT_SECRET:请改成随机长字符串}'),
    ],
    r'\.bat$': [
        # 启动横幅里打印的演示密码
        (r'(password:\s*)[0-9A-Za-z_!@#$%^&*]+', r'\1见 docs/隐私与安全说明.md'),
    ],
    r'\.sql$': [
        # 32 位 MD5 密码哈希
        (r"'[a-f0-9]{32}'", r"'<密码MD5哈希>'"),
    ],
    r'README\.md$': [
        # mysql 命令行里直接带的 root 密码：-p123456  ->  -p
        (r'mysql\s+-u\S+\s+-p[0-9A-Za-z_!@#$%^&*]+', r'mysql -uroot -p'),
    ],
}


def excluded(path_rel):
    """判断相对路径是否要排除"""
    p = path_rel.replace('\\', '/')
    for d in EXCLUDE_DIRS:
        if p == d or p.startswith(d + '/'):
            return '目录排除: ' + d
    for pat in EXCLUDE_FILE_PATTERNS:
        if re.search(pat, p, re.I):
            return '文件排除: ' + pat
    return None


def sanitize(rel, data):
    """按需脱敏，返回 (bytes, 是否改动过)"""
    for pat, rules in SANITIZE.items():
        if re.search(pat, rel, re.I):
            try:
                txt = data.decode('utf-8')
            except UnicodeDecodeError:
                return data, False
            old = txt
            for r, rep in rules:
                txt = re.sub(r, rep, txt)
            if txt != old:
                return txt.encode('utf-8'), True
            return data, False
    return data, False


def main():
    out = None
    dry = '--dry-run' in sys.argv
    for i, a in enumerate(sys.argv):
        if a == '--out' and i + 1 < len(sys.argv):
            out = sys.argv[i + 1]
    if out is None:
        stamp = datetime.datetime.now().strftime('%Y%m%d')
        out = os.path.join(os.path.dirname(ROOT), 'Java10143-clean-%s.zip' % stamp)

    keep, skip = [], []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        rel_dir = os.path.relpath(dirpath, ROOT).replace('\\', '/')
        if rel_dir == '.':
            rel_dir = ''
        # 整体排除的目录直接剪枝，避免 walk 进去浪费时间
        dropped = [d for d in dirnames
                   if excluded((rel_dir + '/' + d) if rel_dir else d)]
        for d in dropped:
            full = (rel_dir + '/' + d) if rel_dir else d
            skip.append((full + '/**', '目录排除: ' + d))
        dirnames[:] = [d for d in dirnames if d not in dropped]
        for fn in filenames:
            rel = (rel_dir + '/' + fn) if rel_dir else fn
            why = excluded(rel)
            if why:
                skip.append((rel, why))
            else:
                keep.append(rel)

    total_src = 0
    for rel in keep:
        try:
            total_src += os.path.getsize(os.path.join(ROOT, rel))
        except OSError:
            pass

    print('=' * 62)
    print('项目根目录 :', ROOT)
    print('将打包     : %d 个文件, %.1f MB' % (len(keep), total_src / 1048576.0))
    print('已排除     : %d 个条目' % len(skip))
    print('=' * 62)

    # 分组展示排除项，避免刷屏
    from collections import Counter
    cnt = Counter(w for _, w in skip)
    for w, n in cnt.most_common():
        print('  %-42s %5d 项' % (w, n))

    if dry:
        print('\n[dry-run] 未生成 zip。去掉 --dry-run 即可打包。')
        return 0

    changed = []
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for rel in keep:
            full = os.path.join(ROOT, rel)
            try:
                with open(full, 'rb') as f:
                    data = f.read()
            except OSError as e:
                print('  [skip] %s (%s)' % (rel, e))
                continue
            data, did = sanitize(rel, data)
            if did:
                changed.append(rel)
            z.writestr(rel, data)

    print('\n生成 :', out)
    print('大小 : %.1f MB' % (os.path.getsize(out) / 1048576.0))
    if changed:
        print('\n已脱敏 %d 个文件（密码/密钥已替换为占位符）：' % len(changed))
        for c in changed:
            print('   -', c)
    print('\n提示：解压后先看 docs/隐私与安全说明.md，按里面填自己的数据库密码。')
    return 0


if __name__ == '__main__':
    sys.exit(main())
