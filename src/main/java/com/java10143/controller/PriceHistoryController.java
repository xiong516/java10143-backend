package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品价格走势：
 *   GET  /goods/{spuId}/price-history?days=90   走势数据 + 历史最低/最高 + 购买建议
 *   POST /admin/price/snapshot                  手动触发当天快照（管理员）
 */
@RestController
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping("/goods/{spuId}/price-history")
    public R<Map<String, Object>> history(@PathVariable Long spuId,
                                          @RequestParam(required = false) Integer days) {
        return R.ok(priceHistoryService.history(spuId, days == null ? 90 : days));
    }

    @PostMapping("/admin/price/snapshot")
    public R<Map<String, Object>> snapshot() {
        int n = priceHistoryService.snapshot();
        Map<String, Object> res = new java.util.LinkedHashMap<>();
        res.put("count", n);
        res.put("msg", "已写入 " + n + " 条当日价格快照");
        return R.ok(res);
    }
}
