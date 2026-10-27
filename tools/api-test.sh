#!/bin/bash
# Java10143 接口自测脚本：把每个接口的返回都打到控制台
BASE="http://localhost:8080"
OUT=/tmp/api-test.txt
: > $OUT

echo "===== 1) 首页轮播 =====" | tee -a $OUT
curl -s "$BASE/home/banner?distributionSite=2" | head -c 400 >> $OUT; echo >> $OUT

echo "===== 2) 分类头部（分类页左侧） =====" | tee -a $OUT
curl -s "$BASE/home/category/head" | head -c 400 >> $OUT; echo >> $OUT

echo "===== 3) 分类下的商品 =====" | tee -a $OUT
curl -s -G "$BASE/category" --data-urlencode "id=101" | head -c 400 >> $OUT; echo >> $OUT

echo "===== 4) 商品详情 =====" | tee -a $OUT
curl -s -G "$BASE/goods" --data-urlencode "id=1001" | head -c 700 >> $OUT; echo >> $OUT

echo "===== 5) 登录 =====" | tee -a $OUT
LOGIN=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" \
  -d '{"account":"hm","password":"'"$1"'"}')
echo "$LOGIN" | head -c 500 >> $OUT; echo >> $OUT
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "token 长度: ${#TOKEN}" | tee -a $OUT

echo "===== 6) 购物车（带 token） =====" | tee -a $OUT
curl -s "$BASE/member/cart" -H "Authorization: Bearer $TOKEN" | head -c 600 >> $OUT; echo >> $OUT

echo "===== 7) 结算预订单（鸿蒙端那种 body 传参方式） =====" | tee -a $OUT
curl -s -X GET "$BASE/member/order/pre" -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" -d '["2001","2004"]' | head -c 800 >> $OUT; echo >> $OUT

echo "===== 8) 提交订单 =====" | tee -a $OUT
SUBMIT=$(curl -s -X POST "$BASE/member/order" -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"addressId":"1","buyerMessage":"","deliveryTimeType":1,"payChannel":1,"payType":1,"goods":[{"skuId":"2001","count":2}]}')
echo "$SUBMIT" | head -c 300 >> $OUT; echo >> $OUT
ORDER_ID=$(echo "$SUBMIT" | sed -n 's/.*"orderId":"\([0-9]*\)".*/\1/p')
echo "订单号: $ORDER_ID" | tee -a $OUT

echo "===== 9) 订单详情 =====" | tee -a $OUT
curl -s "$BASE/member/order/$ORDER_ID" -H "Authorization: Bearer $TOKEN" | head -c 800 >> $OUT; echo >> $OUT

echo "===== 10) 支付（模拟支付宝） =====" | tee -a $OUT
curl -s "$BASE/pay/aliPay?orderId=$ORDER_ID" -H "Authorization: Bearer $TOKEN" | head -c 400 >> $OUT; echo >> $OUT

echo "===== 11) 我的订单列表 =====" | tee -a $OUT
curl -s "$BASE/member/order" -H "Authorization: Bearer $TOKEN" | head -c 300 >> $OUT; echo >> $OUT

echo "===== 12) 生成的图片（SVG 前 200 字符） =====" | tee -a $OUT
curl -s "$BASE/img/p/p1001.svg" -H "Accept: image/svg+xml" | head -c 200 >> $OUT; echo >> $OUT

echo "===== 13) WEB 页面是否可访问 =====" | tee -a $OUT
curl -s -o /dev/null -w "index.html HTTP %{http_code}, 大小 %{size_download} 字节\n" "$BASE/" >> $OUT
curl -s -o /dev/null -w "app.js     HTTP %{http_code}, 大小 %{size_download} 字节\n" "$BASE/js/app.js" >> $OUT
