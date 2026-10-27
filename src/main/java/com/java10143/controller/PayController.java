package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.OrderService;
import com.java10143.service.PayService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付
 *   GET /pay/aliPay?orderId=xxx   —— 鸿蒙端支付页调用，返回 { orderId, redirect }
 *   GET /pay/result?orderId=xxx   —— 模拟支付宝收银台页面（浏览器打开可直接看效果）
 */
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;
    private final OrderService orderService;

    @GetMapping("/aliPay")
    public R<Map<String, Object>> aliPay(@RequestParam String orderId) {
        // result 和 data 同时返回，兼容鸿蒙端两种解析写法
        return R.okBoth(payService.aliPay(UserContext.require(), Long.valueOf(orderId)));
    }

    @GetMapping(value = "/result", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String result(@RequestParam String orderId) {
        Map<String, Object> order = orderService.detailAny(Long.valueOf(orderId));
        // JDK 8：没有文本块（"""），改用字符串拼接 + String.format
        String html = "<!DOCTYPE html>\n"
                + "<html lang=\"zh-CN\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                + "  <title>支付宝 - 支付成功</title>\n"
                + "  <style>\n"
                + "    body{margin:0;font-family:\"Microsoft YaHei\",sans-serif;background:#f5f5f5;\n"
                + "         display:flex;align-items:center;justify-content:center;height:100vh}\n"
                + "    .card{background:#fff;border-radius:14px;padding:40px 56px;text-align:center;\n"
                + "          box-shadow:0 8px 30px rgba(0,0,0,.08)}\n"
                + "    .ok{font-size:60px;color:#52c41a;line-height:1}\n"
                + "    h1{font-size:22px;margin:18px 0 6px;color:#333}\n"
                + "    p{color:#888;font-size:14px;margin:6px 0}\n"
                + "    b{color:#e1251b;font-size:20px}\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <div class=\"card\">\n"
                + "    <div class=\"ok\">&#10004;</div>\n"
                + "    <h1>模拟支付宝支付成功</h1>\n"
                + "    <p>订单号：%s</p>\n"
                + "    <p>实付金额：<b>￥%s</b></p>\n"
                + "    <p style=\"margin-top:20px\">这是 Java10143 后端生成的模拟支付结果页</p>\n"
                + "  </div>\n"
                + "</body>\n"
                + "</html>\n";
        return String.format(html, orderId, order.get("payMoney"));
    }
}
