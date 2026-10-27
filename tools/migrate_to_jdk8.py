# -*- coding: utf-8 -*-
"""
把 Java10143 后端从 JDK 17 / Spring Boot 3 迁移到 JDK 8 / Spring Boot 2.7 的机械替换部分。
（结构性改动如文本块、HttpClient、Map.of、instanceof 模式匹配由手工完成）

做两件事：
  1. import jakarta.servlet.*  ->  import javax.servlet.*
  2. xxx.isBlank()  ->  StrUtil.isBlank(xxx)   （JDK 8 没有 String.isBlank）
"""
import os
import re

BASE = r"E:\xiazai\huawei\Java10143\src\main\java\com\java10143"
STRUTIL_IMPORT = "import cn.hutool.core.util.StrUtil;"


def migrate(text):
    changed = []
    t = text

    if "import jakarta.servlet" in t:
        t = t.replace("import jakarta.servlet", "import javax.servlet")
        changed.append("jakarta->javax")

    if ".isBlank()" in t:
        t = re.sub(r"\b([A-Za-z_][A-Za-z0-9_]*)\.isBlank\(\)", r"StrUtil.isBlank(\1)", t)
        changed.append("isBlank->StrUtil")
        if STRUTIL_IMPORT not in t:
            t = re.sub(r"(package [^\n]+;\n)", r"\1\n" + STRUTIL_IMPORT + "\n", t, count=1)

    return t, changed


def main():
    total = 0
    for root, _dirs, files in os.walk(BASE):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(root, name)
            with open(path, encoding="utf-8") as f:
                src = f.read()
            out, changed = migrate(src)
            if out != src:
                with open(path, "w", encoding="utf-8", newline="") as f:
                    f.write(out)
                rel = os.path.relpath(path, BASE)
                print("  [改] %-46s %s" % (rel, ", ".join(changed)))
                total += 1
    print("共修改 %d 个文件" % total)


if __name__ == "__main__":
    main()
