package com.java10143.controller;

import com.java10143.common.BizException;
import com.java10143.common.R;
import com.java10143.service.SeckillService;
import com.java10143.util.Json;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀：
 *   GET  /seckill/list         场次列表（公开）
 *   GET  /seckill/{id}         活动详情（公开）
 *   POST /member/seckill/buy   抢购下单 {seckillId, count, addressId}
 */
@RestController
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @GetMapping("/seckill/list")
    public R<Map<String, Object>> list() {
        return R.ok(seckillService.list());
    }

    @GetMapping("/seckill/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(seckillService.detail(id));
    }

    /**
     * 抢购：先锁库存，再交给 OrderService 生成正式订单。
     * 任何一步失败都要把锁的库存还回去，否则会出现「卖不出去但库存没了」。
     */
    @PostMapping("/member/seckill/buy")
    public R<Map<String, Object>> buy(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.require();
        Long seckillId = toLong(body.get("seckillId"));
        int count = toInt(body.get("count"), 1);
        if (seckillId == null) throw new BizException("请选择秒杀活动");

        Map<String, Object> grabbed = seckillService.grab(uid, seckillId, count);
        Long skuId = toLong(grabbed.get("skuId"));
        try {
            Map<String, Object> order = new LinkedHashMap<>();
            order.put("addressId", body.get("addressId"));
            order.put("buyerMessage", body.get("buyerMessage"));
            order.put("deliveryTimeType", body.get("deliveryTimeType"));
            order.put("payType", toInt(body.get("payType"), 1));
            order.put("payChannel", toInt(body.get("payChannel"), 1));
            order.put("seckillId", seckillId);
            List<Map<String, Object>> goods = new ArrayList<>();
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("skuId", skuId);
            g.put("count", count);
            goods.add(g);
            order.put("goods", goods);
            return R.ok(orderService.submit(uid, order));
        } catch (RuntimeException e) {
            seckillService.restore(seckillId, count);
            throw e;
        }
    }

    private final com.java10143.service.OrderService orderService;

    private static Long toLong(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        try {
            return (int) Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }
}
