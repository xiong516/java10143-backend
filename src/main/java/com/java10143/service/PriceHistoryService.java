package com.java10143.service;

import com.java10143.common.BizException;
import com.java10143.entity.Spu;
import com.java10143.mapper.SpuMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 商品价格走势：
 *   GET /goods/{spuId}/price-history?days=90
 * 返回：日期序列 / 价格序列 / 历史最低最高 / 当前价在历史中的位置 / 建议
 */
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final JdbcTemplate jdbc;
    private final SpuMapper spuMapper;

    public Map<String, Object> history(Long spuId, int days) {
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) throw new BizException("商品不存在");
        if (days <= 0 || days > 365) days = 90;

        LocalDate from = LocalDate.now().minusDays(days - 1L);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT stat_date, price FROM price_history WHERE spu_id = ? AND stat_date >= ? ORDER BY stat_date ASC",
                spuId, java.sql.Date.valueOf(from));

        List<String> labels = new ArrayList<>();
        List<String> prices = new ArrayList<>();
        BigDecimal min = null, max = null, sum = BigDecimal.ZERO;
        String minDate = null, maxDate = null;

        for (Map<String, Object> r : rows) {
            labels.add(String.valueOf(r.get("stat_date")));
            BigDecimal p = (BigDecimal) r.get("price");
            prices.add(Vo.money(p));
            sum = sum.add(p);
            if (min == null || p.compareTo(min) < 0) { min = p; minDate = String.valueOf(r.get("stat_date")); }
            if (max == null || p.compareTo(max) > 0) { max = p; maxDate = String.valueOf(r.get("stat_date")); }
        }

        BigDecimal current = spu.getPrice() == null ? BigDecimal.ZERO : spu.getPrice();
        BigDecimal avg = prices.isEmpty() ? current
                : sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

        // 当前价在 [min, max] 区间的百分位（0=史上最低）
        int percentile = 50;
        if (min != null && max != null && max.compareTo(min) > 0) {
            percentile = current.subtract(min)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(max.subtract(min), 0, RoundingMode.HALF_UP).intValue();
            percentile = Math.max(0, Math.min(100, percentile));
        }

        String advice;
        String adviceLevel; // low 值得买 / mid 一般 / high 偏贵
        if (percentile <= 15) {
            advice = "当前是近 " + days + " 天最低价区间，值得入手";
            adviceLevel = "low";
        } else if (percentile >= 80) {
            advice = "当前价格处于近期高位，建议再等等或关注秒杀";
            adviceLevel = "high";
        } else {
            advice = "价格处于正常区间，比均价" + (current.compareTo(avg) <= 0 ? "更低" : "略高");
            adviceLevel = "mid";
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("spuId", spuId);
        res.put("name", spu.getName());
        res.put("days", days);
        res.put("labels", labels);
        res.put("prices", prices);
        res.put("current", Vo.money(current));
        res.put("min", min == null ? Vo.money(current) : Vo.money(min));
        res.put("minDate", minDate);
        res.put("max", max == null ? Vo.money(current) : Vo.money(max));
        res.put("maxDate", maxDate);
        res.put("avg", Vo.money(avg));
        res.put("percentile", percentile);
        res.put("advice", prices.isEmpty() ? "暂无足够历史数据" : advice);
        res.put("adviceLevel", prices.isEmpty() ? "mid" : adviceLevel);
        // 相比最高价省了多少
        res.put("saveVsMax", max == null ? "0.00" : Vo.money(max.subtract(current).max(BigDecimal.ZERO)));
        return res;
    }

    /**
     * 每日快照任务：把每个 spu 的当前最低 SKU 价写进 price_history（当天已有则更新）。
     * 项目启动时由 PriceSnapshotTask 调用一次，也可以手动 POST /admin/price/snapshot 触发。
     */
    public int snapshot() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT p.id spuId, MIN(s.price) minPrice FROM spu p " +
                "JOIN sku s ON s.spu_id = p.id GROUP BY p.id");
        LocalDate today = LocalDate.now();
        int n = 0;
        for (Map<String, Object> r : rows) {
            Long spuId = ((Number) r.get("spuId")).longValue();
            BigDecimal price = (BigDecimal) r.get("minPrice");
            if (price == null) continue;
            n += jdbc.update(
                    "INSERT INTO price_history (spu_id, price, stat_date) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE price = VALUES(price)",
                    spuId, price, java.sql.Date.valueOf(today));
        }
        return n;
    }
}
