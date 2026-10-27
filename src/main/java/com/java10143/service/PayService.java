package com.java10143.service;

import com.java10143.common.BizException;
import com.java10143.entity.Orders;
import com.java10143.mapper.OrdersMapper;
import com.java10143.util.ImgUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付。
 * 这里做的是「模拟支付宝」：调一次就把订单标记为已支付，并返回一个跳转地址。
 * 页面上的“立即支付宝支付”按钮其实只展示这个链接，然后跳订单详情页。
 */
@Service
@RequiredArgsConstructor
public class PayService {

    private final OrdersMapper ordersMapper;

    /** GET /pay/aliPay?orderId=xxx */
    public Map<String, Object> aliPay(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BizException("无权支付该订单");

        if (order.getPayState() == null || order.getPayState() == 0) {
            order.setPayState(1);
            order.setOrderState(2);      // 已付款 -> 待发货
            order.setPayTime(LocalDateTime.now());
            ordersMapper.updateById(order);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderId", order.getId());
        // 用「当前请求的 Host」拼跳转地址：电脑上访问是 localhost，
        // 手机用 192.168.x.x 访问时会自动变成 192.168.x.x，真机也能打开
        res.put("redirect", ImgUrl.abs("/pay/result?orderId=" + order.getId()));
        return res;
    }
}
