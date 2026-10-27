package com.java10143.controller;

import cn.hutool.core.util.StrUtil;

import com.java10143.common.BizException;
import com.java10143.common.R;
import com.java10143.service.OrderService;
import com.java10143.util.Json;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单
 *   GET  /member/order/pre?skuIds=1,2   结算预订单（鸿蒙端把 skuIds 放在 body 数组里）
 *   POST /member/order                  提交订单
 *   GET  /member/order/{id}             订单详情
 *   GET  /member/order                  我的订单列表
 */
@RestController
@RequestMapping("/member/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/pre")
    public R<Map<String, Object>> pre(@RequestParam(required = false) String skuIds,
                                     @RequestParam(required = false) String ids) {
        String raw = (skuIds != null && !StrUtil.isBlank(skuIds)) ? skuIds : ids;
        return R.ok(orderService.pre(UserContext.require(), Json.parseIds(raw)));
    }

    @PostMapping
    public R<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        return R.ok(orderService.submit(UserContext.require(), body));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable String id) {
        if ("pre".equals(id)) throw new BizException("订单不存在");
        return R.okBoth(orderService.detail(UserContext.require(), Long.valueOf(id)));
    }

    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) Integer orderState) {
        return R.ok(orderService.list(UserContext.require(), orderState));
    }
}
