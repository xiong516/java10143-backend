package com.java10143.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.entity.OrderItem;
import com.java10143.entity.Orders;
import com.java10143.entity.Spu;
import com.java10143.mapper.OrderItemMapper;
import com.java10143.mapper.OrdersMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.util.UserContext;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能推荐（千人千面）
 *
 * 思路（本地算法，零外部依赖）：
 *   1. 登录用户：从 TA 的历史订单里统计买过哪些分类（order_item → spu → categoryId），
 *      挑出买得最多的 1~2 个类目，从这些类目里按销量挑「没买过的」商品
 *   2. 访客 / 没有订单：回退到全站热销榜
 *   每个商品带一个 reason 字段（推荐理由），前端直接展示
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final SpuMapper spuMapper;

    /** GET /home/recommend?limit=6 */
    public List<Map<String, Object>> recommend(String limit) {
        int size = 6;
        try {
            if (limit != null && !StrUtil.isBlank(limit)) size = Math.max(1, Math.min(20, Integer.parseInt(limit)));
        } catch (Exception ignored) {
        }

        Long userId = UserContext.get();
        if (userId != null) {
            List<Map<String, Object>> personalized = personalized(userId, size);
            if (!personalized.isEmpty()) return personalized;
        }
        return hotFallback(size);
    }

    /** 登录用户：按历史订单类目推荐没买过的热销品 */
    private List<Map<String, Object>> personalized(Long userId, int size) {
        // 1. 用户所有订单
        List<Orders> orders = ordersMapper.selectList(
                Wrappers.<Orders>lambdaQuery().eq(Orders::getUserId, userId));
        if (orders.isEmpty()) return new ArrayList<>();

        // 2. 订单明细 → 买过的 spuId、买过商品的名称与类目计数
        List<Long> orderIds = new ArrayList<>();
        for (Orders o : orders) orderIds.add(o.getId());
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().in(OrderItem::getOrderId, orderIds));
        if (items.isEmpty()) return new ArrayList<>();

        Map<Long, Integer> catCount = new HashMap<>();
        Map<Long, String> catName = new LinkedHashMap<>();
        List<Long> boughtSpuIds = new ArrayList<>();
        String lastBoughtName = null;
        for (OrderItem it : items) {
            if (it.getSpuId() == null) continue;
            boughtSpuIds.add(it.getSpuId());
            lastBoughtName = it.getName();
            Spu spu = spuMapper.selectById(it.getSpuId());
            if (spu == null || spu.getCategoryId() == null) continue;
            catCount.merge(spu.getCategoryId(), 1, Integer::sum);
        }
        if (catCount.isEmpty()) return new ArrayList<>();

        // 3. 按出现次数取前 2 个类目
        List<Long> topCats = new ArrayList<>(catCount.keySet());
        topCats.sort((a, b) -> catCount.get(b) - catCount.get(a));
        while (topCats.size() > 2) topCats.remove(topCats.size() - 1);

        // 4. 这些类目里的热销商品，排除买过的
        List<Spu> candidates = spuMapper.selectList(
                Wrappers.<Spu>lambdaQuery()
                        .in(Spu::getCategoryId, topCats)
                        .orderByDesc(Spu::getSalesCount)
                        .last("limit " + (size + boughtSpuIds.size())));
        String reasonTail = lastBoughtName == null ? "" : "，因为你买过「" + lastBoughtName + "」";
        List<Map<String, Object>> res = new ArrayList<>();
        for (Spu spu : candidates) {
            if (boughtSpuIds.contains(spu.getId())) continue;
            Map<String, Object> m = Vo.product(spu);
            m.put("reason", "猜你喜欢" + reasonTail);
            res.add(m);
            if (res.size() >= size) break;
        }
        return res;
    }

    /** 访客 / 无历史：全站热销榜兜底 */
    private List<Map<String, Object>> hotFallback(int size) {
        List<Spu> list = spuMapper.selectList(
                Wrappers.<Spu>lambdaQuery().orderByDesc(Spu::getSalesCount).last("limit " + size));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Spu spu : list) {
            Map<String, Object> m = Vo.product(spu);
            m.put("reason", "全站热销榜 · 大家都在买");
            res.add(m);
        }
        return res;
    }
}
