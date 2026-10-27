package com.java10143.service;

import com.java10143.common.BizException;
import com.java10143.entity.Sku;
import com.java10143.entity.Spu;
import com.java10143.mapper.SkuMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 秒杀：
 *   GET  /seckill/list        —— 按场次分组（进行中 / 即将开始 / 已结束）
 *   GET  /seckill/{id}        —— 单个活动详情（下单前校验价格和库存）
 *   POST /member/seckill/{id}/buy —— 直接下单（内部走 OrderService）
 *
 * 防超卖：UPDATE seckill SET sold = sold + n WHERE id = ? AND sold + n <= stock
 * 影响行数为 0 就说明抢光了 —— 这一条 SQL 就是全部并发控制。
 */
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final JdbcTemplate jdbc;
    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;

    /** GET /seckill/list */
    public Map<String, Object> list() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> all = jdbc.queryForList(
                "SELECT * FROM seckill ORDER BY start_time ASC, id ASC");

        List<Map<String, Object>> running = new ArrayList<>();
        List<Map<String, Object>> upcoming = new ArrayList<>();
        List<Map<String, Object>> ended = new ArrayList<>();

        for (Map<String, Object> r : all) {
            Map<String, Object> m = item(r, now);
            if (m == null) continue;
            int st = ((Number) m.get("stateCode")).intValue();
            if (st == 1) running.add(m);
            else if (st == 0) upcoming.add(m);
            else ended.add(m);
        }
        // 进行中按「剩余百分比」升序，快抢光的排前面
        running.sort(Comparator.comparingDouble(a -> -toDouble(a.get("soldPercent"))));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("running", running);
        res.put("upcoming", upcoming);
        res.put("ended", ended.size() > 6 ? ended.subList(ended.size() - 6, ended.size()) : ended);
        res.put("serverTime", System.currentTimeMillis());
        res.put("runningCount", running.size());
        return res;
    }

    /** GET /seckill/{id} */
    public Map<String, Object> detail(Long id) {
        Map<String, Object> r;
        try {
            r = jdbc.queryForMap("SELECT * FROM seckill WHERE id = ?", id);
        } catch (Exception e) {
            throw new BizException("秒杀活动不存在");
        }
        Map<String, Object> m = item(r, LocalDateTime.now());
        if (m == null) throw new BizException("秒杀活动不存在");
        return m;
    }

    /**
     * 下单前锁库存 + 拿秒杀价。
     * 返回 {price, skuId, spuId, title}，由 OrderService 生成订单。
     */
    @Transactional
    public Map<String, Object> grab(Long userId, Long seckillId, int count) {
        Map<String, Object> r;
        try {
            r = jdbc.queryForMap("SELECT * FROM seckill WHERE id = ?", seckillId);
        } catch (Exception e) {
            throw new BizException("秒杀活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = toTime(r.get("start_time"));
        LocalDateTime end = toTime(r.get("end_time"));
        if (start != null && now.isBefore(start)) throw new BizException("秒杀还没开始，先加个提醒吧");
        if (end != null && now.isAfter(end)) throw new BizException("本轮秒杀已结束");

        int limit = ((Number) r.get("limit_per")).intValue();
        if (count > limit) throw new BizException("每人限购 " + limit + " 件");
        if (count <= 0) throw new BizException("购买数量有误");

        // ★ 核心：一条 UPDATE 完成「判断 + 扣减」，天然原子
        int n = jdbc.update(
                "UPDATE seckill SET sold = sold + ? WHERE id = ? AND sold + ? <= stock",
                count, seckillId, count);
        if (n == 0) throw new BizException("手慢了，秒杀库存已抢光");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("seckillId", seckillId);
        res.put("spuId", r.get("spu_id"));
        res.put("skuId", r.get("sku_id"));
        res.put("price", r.get("price"));
        res.put("count", count);
        res.put("title", r.get("title"));
        return res;
    }

    /** 下单失败时把库存还回去 */
    public void restore(Long seckillId, int count) {
        if (seckillId == null) return;
        jdbc.update("UPDATE seckill SET sold = GREATEST(sold - ?, 0) WHERE id = ?", count, seckillId);
    }

    // ==================== 内部 ====================

    private Map<String, Object> item(Map<String, Object> r, LocalDateTime now) {
        long spuId = ((Number) r.get("spu_id")).longValue();
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null) return null;
        Sku sku = skuMapper.selectById(((Number) r.get("sku_id")).longValue());

        LocalDateTime start = toTime(r.get("start_time"));
        LocalDateTime end = toTime(r.get("end_time"));
        int stock = ((Number) r.get("stock")).intValue();
        int sold = ((Number) r.get("sold")).intValue();

        boolean notStart = start != null && now.isBefore(start);
        boolean isEnd = end != null && now.isAfter(end);
        int state;      // 0 即将开始 1 进行中 2 已结束
        long remainMs;
        if (isEnd) {
            state = 2;
            remainMs = 0;
        } else if (notStart) {
            state = 0;
            remainMs = Duration.between(now, start).toMillis();
        } else {
            state = 1;
            remainMs = end == null ? 0 : Duration.between(now, end).toMillis();
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ((Number) r.get("id")).longValue());
        m.put("title", r.get("title"));
        m.put("spuId", spuId);
        m.put("skuId", r.get("sku_id"));
        m.put("name", spu.getName());
        m.put("picture", Vo.firstPicture(spu));
        m.put("price", Vo.money((BigDecimal) r.get("price")));
        m.put("originPrice", Vo.money((BigDecimal) r.get("origin_price")));
        m.put("stock", stock);
        m.put("sold", sold);
        m.put("limitPer", r.get("limit_per"));
        m.put("startTime", String.valueOf(r.get("start_time")));
        m.put("endTime", String.valueOf(r.get("end_time")));
        m.put("state", state == 1 ? "进行中" : (state == 0 ? "即将开始" : "已结束"));
        m.put("stateCode", state);
        m.put("remainMs", (double) remainMs);
        m.put("soldPercent", stock == 0 ? 100 : Math.min(100, sold * 100 / stock));
        m.put("left", Math.max(0, stock - sold));
        m.put("soldOut", sold >= stock);
        // 省多少
        BigDecimal op = (BigDecimal) r.get("origin_price");
        BigDecimal sp = (BigDecimal) r.get("price");
        if (op != null && sp != null && op.compareTo(sp) > 0) {
            m.put("save", Vo.money(op.subtract(sp)));
            m.put("off", sp.multiply(BigDecimal.TEN)
                    .divide(op, 1, java.math.RoundingMode.HALF_UP).toPlainString() + " 折");
        } else {
            m.put("save", "0.00");
            m.put("off", "");
        }
        if (sku != null) {
            m.put("attrsText", Vo.attrsText(sku.getSpecs()));
            m.put("skuPrice", Vo.money(sku.getPrice()));
        }
        return m;
    }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }

    private static LocalDateTime toTime(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp) return ((Timestamp) o).toLocalDateTime();
        if (o instanceof LocalDateTime) return (LocalDateTime) o;
        return null;
    }
}
