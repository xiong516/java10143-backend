#!/usr/bin/env bash
# ============================================================
# 一键切换隧道地址（Java10143 全端）
#
#   用法：bash set-tunnel.sh https://abcd1234.r8.cpolar.top
#         bash set-tunnel.sh            # 只检查当前各端域名，不改
#
# 改的是 4 个前端常量；数据库和后端不用动
# （后端 ImgUrl.abs() 按请求 Host 拼图片地址，换域名自动跟随）。
# ============================================================
set -u

ROOT_E="/e/xiazai/huawei"
MP_JS="E:/Users/lenovo/WeChatProjects/miniprogram-jd/utils/api.js"
HM_ETS="E:/xiazai/huawei/MyApplication10143/entry/src/main/ets/util/myreqest.ets"
UNI_JS="E:/xiazai/huawei/Java10143/uni-app/api/request.js"
WEB_JS="E:/xiazai/huawei/Java10143/src/main/resources/static/request.js"

RED=$'\033[31m'; GRN=$'\033[32m'; YEL=$'\033[33m'; DIM=$'\033[2m'; NC=$'\033[0m'

show_current() {
  echo "${YEL}当前各端隧道域名：${NC}"
  printf '  %-14s %s\n' "小程序"  "$(grep -oE "https?://[A-Za-z0-9.-]+cpolar[A-Za-z0-9.:-]*" "$MP_JS"  2>/dev/null | head -1)"
  printf '  %-14s %s\n' "鸿蒙App" "$(grep -oE "https?://[A-Za-z0-9.-]+cpolar[A-Za-z0-9.:-]*" "$HM_ETS" 2>/dev/null | head -1)"
  printf '  %-14s %s\n' "uni-app" "$(grep -oE "https?://[A-Za-z0-9.-]+cpolar[A-Za-z0-9.:-]*" "$UNI_JS" 2>/dev/null | head -1)"
  printf '  %-14s %s\n' "WEB"     "$(grep -oE "https?://[A-Za-z0-9.-]+cpolar[A-Za-z0-9.:-]*" "$WEB_JS" 2>/dev/null | head -1)"
  local n
  n=$(grep -hoE "https?://[A-Za-z0-9.-]+cpolar[A-Za-z0-9.:-]*" "$MP_JS" "$HM_ETS" "$UNI_JS" "$WEB_JS" 2>/dev/null | sed 's#/$##' | sort -u | wc -l)
  if [ "$n" -eq 1 ]; then echo "${GRN}  ✓ 四个端域名一致${NC}"; else echo "${RED}  ! 四端域名不一致（$n 种），建议统一${NC}"; fi
}

if [ $# -eq 0 ] || [ "${1:-}" = "--check" ]; then
  show_current
  echo
  echo "用法：bash set-tunnel.sh https://新域名"
  exit 0
fi

NEW="${1%/}"
if ! printf '%s' "$NEW" | grep -qE '^https?://[A-Za-z0-9.-]+(:[0-9]+)?$'; then
  echo "${RED}✗ 地址格式不对：$NEW${NC}"
  echo "  要形如 https://abcd1234.r8.cpolar.top（必须带 http:// 或 https://，结尾不要加 /）"
  exit 1
fi

echo "${DIM}目标地址：$NEW${NC}"
show_current
echo

STAMP=$(date +%Y%m%d-%H%M%S)
fail=0

# 替换某个文件里的隧道域名（只动 cpolar/ngrok 这类公网域名，不碰局域网 IP）
patch() {
  local file="$1" label="$2"
  if [ ! -f "$file" ]; then echo "${RED}✗ $label 文件不存在：$file${NC}"; fail=1; return; fi
  local old
  old=$(grep -oE "https?://[A-Za-z0-9.-]+(cpolar|ngrok|natapp|tunnel)[A-Za-z0-9.:-]*" "$file" | head -1)
  if [ -z "$old" ]; then echo "${YEL}! $label 里没找到隧道域名，跳过${NC}"; return; fi
  cp "$file" "$file.bak-$STAMP"
  # 用 | 作分隔符，避免 URL 里的 / 冲突
  sed -i "s|${old}|${NEW}|g" "$file"
  local hit
  hit=$(grep -c -F "$NEW" "$file")
  echo "${GRN}✓ $label${NC}  $old → $NEW  ${DIM}($hit 处，备份 .bak-$STAMP)${NC}"
}

patch "$MP_JS"  "微信小程序"
patch "$HM_ETS" "鸿蒙App"
patch "$UNI_JS" "uni-app"
patch "$WEB_JS" "WEB前端"

echo
show_current
echo
if [ $fail -ne 0 ]; then
  echo "${RED}有文件未改成功，请检查路径${NC}"; exit 1
fi

cat <<TIP
${YEL}接下来必须做的：${NC}
  WEB 端：static 目录会被打进 jar，改了 request.js 必须重新打包 + 重启后端
          双击 E:\xiazai\huawei\Java10143\desktop\build-java10143.bat
  H5 端 ：若通过隧道域名访问页面，重新编译 uni-app 的 H5（uni.js build -p h5）并拷到 static
  小程序：开发者工具重新编译；真机预览记得勾「不校验合法域名」
  鸿蒙App：DevEco 重新 Run，真机要卸载重装才会用新常量
  元服务 ：与隧道无关（10.0.2.2），不用管

${DIM}验证： curl ${NEW}/home/banner?distributionSite=1${NC}
TIP
