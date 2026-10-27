package com.java10143.service;

import com.java10143.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 优惠券：领券中心 / 我的券 / 下单试算 / 下单核销。
 * 全部用 JdbcTemplate 手写 SQL，避免为 4 张新表再引入 Mapper 样板代码。
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final JdbcTemplate jdbc;

    // ==================== 领券中心 ====================

    /**
     * GET /coupon/list
     * 返回全部上架券；登录后额外带 mine（我领了几张、是否还能领）。
     */
    public Map<String, Object> center(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM coupon WHERE status = 1 ORDER BY threshold ASC, id ASC");

        // 每个用户已领数量
        Map<Long, Integer> taken = new HashMap<>();
        if (userId != null) {
            for (Map<String, Object> r : jdbc.queryForList(
                    "SELECT coupon_id, COUNT(*) c FROM user_coupon WHERE user_id = ? GROUP BY coupon_id", userId)) {
                taken.put(num(r.get("coupon_id")).longValue(), num(r.get("c")).intValue());
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        int available = 0;
        for (Map<String, Object> r : rows) {
            long id = num(r.get("id")).longValue();
            int total = num(r.get("total_count")).intValue();
            int got = num(r.get("taken_count")).intValue();
            int perLimit = num(r.get("per_limit")).intValue();
            int mine = taken.getOrDefault(id, 0);

            LocalDateTime start = toTime(r.get("start_time"));
            LocalDateTime end = toTime(r.get("end_time"));
            boolean expired = end != null && end.isBefore(now);
            boolean notStart = start != null && start.isAfter(now);
            boolean soldOut = got >= total;
            boolean canTake = !expired && !notStart && !soldOut && mine < perLimit;

            Map<String, Object> m = card(r);
            m.put("mineCount", mine);
            m.put("soldOut", soldOut);
            m.put("expired", expired);
            m.put("notStart", notStart);
            m.put("canTake", canTake);
            // 进度条：已抢百分比
            m.put("percent", total <= 0 ? 100 : Math.min(100, got * 100 / total));
            // 倒计时毫秒（前端算秒）
            if (!expired && end != null) {
                m.put("remainMs", (double) java.time.Duration.between(now, end).toMillis());
            } else {
                m.put("remainMs", 0d);
            }
            if (canTake) available++;
            list.add(m);
        }

        List<Map<String, Object>> mineList = userId == null ? new ArrayList<>() : mine(userId, 0);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", list);
        res.put("availableCount", available);
        res.put("mineCount", mineList.size());
        res.put("mine", mineList);
        res.put("login", userId != null);
        return res;
    }

    /** POST /member/coupon/{id}/take */
    @Transactional
    public Map<String, Object> take(Long userId, Long couponId) {
        if (userId == null) throw new BizException("请先登录再领券");

        Map<String, Object> c;
        try {
            c = jdbc.queryForMap("SELECT * FROM coupon WHERE id = ? AND status = 1", couponId);
        } catch (Exception e) {
            throw new BizException("优惠券不存在或已下架");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = toTime(c.get("end_time"));
        LocalDateTime start = toTime(c.get("start_time"));
        if (start != null && start.isAfter(now)) throw new BizException("活动还没开始，先收藏吧");
        if (end != null && end.isBefore(now)) throw new BizException("该券已过期");

        int total = num(c.get("total_count")).intValue();
        int got = num(c.get("taken_count")).intValue();
        if (got >= total) throw new BizException("手慢了，券已抢光");

        int perLimit = num(c.get("per_limit")).intValue();
        Integer mineCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_coupon WHERE user_id = ? AND coupon_id = ?",
                Integer.class, userId, couponId);
        if (mineCount != null && mineCount >= perLimit) throw new BizException("每人限领 " + perLimit + " 张，你已领满");

        // 用 UPDATE 带条件扣减，天然防并发超发
        int updated = jdbc.update(
                "UPDATE coupon SET taken_count = taken_count + 1 WHERE id = ? AND taken_count < total_count", couponId);
        if (updated == 0) throw new BizException("手慢了，券已抢光");

        jdbc.update("INSERT INTO user_coupon (user_id, coupon_id, status, take_time) VALUES (?, ?, 0, ?)",
                userId, couponId, Timestamp.valueOf(now));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("couponId", couponId);
        res.put("msg", "领取成功，已放入「我的优惠券」");
        res.put("mineCount", mineCount == null ? 1 : mineCount + 1);
        return res;
    }

    /** GET /member/coupon?status=0  —— 我的券；不传 status 查全部 */
    public List<Map<String, Object>> mine(Long userId, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder sql = new StringBuilder(
                "SELECT uc.id ucId, uc.status ucStatus, uc.take_time takeTime, uc.use_time useTime, " +
                "c.* FROM user_coupon uc JOIN coupon c ON c.id = uc.coupon_id WHERE uc.user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (status != null) {
            sql.append(" AND uc.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY uc.status ASC, c.threshold ASC, uc.id DESC");

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(sql.toString(), args.toArray())) {
            Map<String, Object> m = card(r);
            m.put("ucId", r.get("ucId"));
            m.put("ucStatus", num(r.get("ucStatus")).intValue());
            m.put("takeTime", String.valueOf(r.get("takeTime")));
            m.put("useTime", r.get("useTime") == null ? null : String.valueOf(r.get("useTime")));
            LocalDateTime end = toTime(r.get("end_time"));
            boolean expired = end != null && end.isBefore(now);
            m.put("expired", expired);
            m.put("usable", num(r.get("ucStatus")).intValue() == 0 && !expired);
            if (end != null) {
                m.put("remainMs", (double) Math.max(0, java.time.Duration.between(now, end).toMillis()));
            } else {
                m.put("remainMs", 0d);
            }
            m.put("tips", tipFor(r));
            list.add(m);
        }
        return list;
    }

    private String tipFor(Map<String, Object> r) {
        int type = num(r.get("type")).intValue();
        BigDecimal th = dec(r.get("threshold"));
        if (type == 3) return "无门槛，下单立减";
        if (type == 2) return "满 " + th.setScale(0, RoundingMode.HALF_UP) + " 元可用";
        return "满 " + th.setScale(0, RoundingMode.HALF_UP) + " 元可用";
    }

    // ==================== 下单试算 ====================

    /** 券品是否匹配：全品类 or 品类命中 */
    private boolean scopeMatch(Map<String, Object> c, Set<Long> categoryIds) {
        String scope = Objects.toString(c.get("scope"), "all");
        if (!"category".equals(scope)) return true;
        Long cid = c.get("category_id") == null ? null : num(c.get("category_id")).longValue();
        return cid != null && categoryIds.contains(cid);
    }

    /**
     * 给一次结算算「最优券」。
     *
     * @param userId      用户
     * @param spuIds      本单包含的 spu（判品类用）
     * @param totalAmount 本单商品总额
     * @return {discount: BigDecimal, coupon: Map|null, best: Map|null, options: List}
     *         discount=0 表示没券可用
     */
    public Map<String, Object> bestFor(Long userId, List<Long> spuIds, BigDecimal totalAmount) {
        Set<Long> catIds = new HashSet<>();
        if (spuIds != null && !spuIds.isEmpty()) {
            List<Object> args = new ArrayList<>(spuIds);
            StringBuilder in = new StringBuilder();
            for (int i = 0; i < spuIds.size(); i++) in.append(i == 0 ? "?" : ",?");
            for (Map<String, Object> r : jdbc.queryForList(
                    "SELECT DISTINCT category_id FROM spu WHERE id IN (" + in + ")", args.toArray())) {
                if (r.get("category_id") != null) catIds.add(num(r.get("category_id")).longValue());
            }
        }

        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, Object> best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (Map<String, Object> c : candidateCoupons(userId)) {
            BigDecimal th = dec(c.get("threshold"));
            if (totalAmount.compareTo(th) < 0) continue;
            if (!scopeMatch(c, catIds)) continue;
            BigDecimal d = discountOf(c, totalAmount);
            if (d.compareTo(BigDecimal.ZERO) <= 0) continue;

            Map<String, Object> o = card(c);
            o.put("ucId", c.get("ucId"));
            o.put("discount", d.setScale(2, RoundingMode.HALF_UP).toPlainString());
            o.put("enabled", true);
            options.add(o);

            if (d.compareTo(bestDiscount) > 0) {
                bestDiscount = d;
                best = o;
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("discount", bestDiscount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        res.put("best", best);
        res.put("options", options);
        return res;
    }

    /** 该用户所有「未使用且在有效期内」的券 */
    private List<Map<String, Object>> candidateCoupons(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return jdbc.queryForList(
                "SELECT uc.id ucId, c.* FROM user_coupon uc JOIN coupon c ON c.id = uc.coupon_id " +
                "WHERE uc.user_id = ? AND uc.status = 0 AND c.status = 1 " +
                "AND (c.start_time IS NULL OR c.start_time <= ?) " +
                "AND (c.end_time IS NULL OR c.end_time >= ?)",
                userId, Timestamp.valueOf(now), Timestamp.valueOf(now));
    }

    /** 一张券对某金额能减多少 */
    private BigDecimal discountOf(Map<String, Object> c, BigDecimal totalAmount) {
        int type = num(c.get("type")).intValue();
        if (type == 2) {
            BigDecimal disc = dec(c.get("discount"));
            if (disc.compareTo(BigDecimal.TEN) >= 0 || disc.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
            // 8.5 折 => 减 total * (1 - 0.85)
            BigDecimal off = totalAmount.multiply(BigDecimal.ONE.subtract(disc.movePointLeft(1)));
            BigDecimal max = dec(c.get("max_discount"));
            if (max.compareTo(BigDecimal.ZERO) > 0 && off.compareTo(max) > 0) off = max;
            return off;
        }
        BigDecimal amt = dec(c.get("amount"));
        // 不能让订单金额变成负数
        return amt.compareTo(totalAmount) > 0 ? totalAmount : amt;
    }

    /** 核销：把 ucId 标记已用，写入订单号 */
    public BigDecimal redeem(Long userId, Long ucId, Long orderId, BigDecimal totalAmount) {
        if (ucId == null) return BigDecimal.ZERO;
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT uc.status, c.* FROM user_coupon uc JOIN coupon c ON c.id = uc.coupon_id " +
                "WHERE uc.id = ? AND uc.user_id = ?", ucId, userId);
        if (rows.isEmpty()) throw new BizException("优惠券不存在");
        Map<String, Object> c = rows.get(0);
        if (num(c.get("status")).intValue() != 0) throw new BizException("该券已使用或已失效");
        if (totalAmount.compareTo(dec(c.get("threshold"))) < 0) {
            throw new BizException("订单金额未达到优惠券使用门槛");
        }
        BigDecimal d = discountOf(c, totalAmount);
        int n = jdbc.update("UPDATE user_coupon SET status = 1, order_id = ?, use_time = ? " +
                        "WHERE id = ? AND user_id = ? AND status = 0",
                orderId, Timestamp.valueOf(LocalDateTime.now()), ucId, userId);
        if (n == 0) throw new BizException("该券已被使用");
        jdbc.update("UPDATE coupon SET used_count = used_count + 1 WHERE id = ?", num(c.get("id")));
        return d.setScale(2, RoundingMode.HALF_UP);
    }

    /** 订单取消/超时关闭时退券 */
    public void refund(Long orderId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, coupon_id FROM user_coupon WHERE order_id = ? AND status = 1", orderId);
        for (Map<String, Object> r : rows) {
            jdbc.update("UPDATE user_coupon SET status = 0, order_id = NULL, use_time = NULL WHERE id = ?",
                    r.get("id"));
            jdbc.update("UPDATE coupon SET used_count = GREATEST(used_count - 1, 0) WHERE id = ?",
                    r.get("coupon_id"));
        }
    }

    // ==================== 工具 ====================

    /** 统一券卡片结构（前端直接渲染） */
    private Map<String, Object> card(Map<String, Object> r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", num(r.get("id")).longValue());
        m.put("name", r.get("name"));
        m.put("type", num(r.get("type")).intValue());
        m.put("threshold", dec(r.get("threshold")).setScale(2, RoundingMode.HALF_UP).toPlainString());
        m.put("amount", dec(r.get("amount")).setScale(2, RoundingMode.HALF_UP).toPlainString());
        m.put("discount", dec(r.get("discount")).stripTrailingZeros().toPlainString());
        m.put("maxDiscount", dec(r.get("max_discount")).setScale(2, RoundingMode.HALF_UP).toPlainString());
        m.put("scope", r.get("scope"));
        m.put("categoryId", r.get("category_id") == null ? null : String.valueOf(r.get("category_id")));
        m.put("totalCount", num(r.get("total_count")).intValue());
        m.put("takenCount", num(r.get("taken_count")).intValue());
        m.put("perLimit", num(r.get("per_limit")).intValue());
        m.put("startTime", r.get("start_time") == null ? null : String.valueOf(r.get("start_time")));
        m.put("endTime", r.get("end_time") == null ? null : String.valueOf(r.get("end_time")));
        // 大字号展示：满减券显示 ¥50，折扣券显示 8.5
        int type = num(r.get("type")).intValue();
        String face;
        if (type == 2) {
            face = dec(r.get("discount")).stripTrailingZeros().toPlainString();
        } else {
            face = dec(r.get("amount")).setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        m.put("face", face);
        m.put("faceUnit", type == 2 ? "折" : "元");
        return m;
    }

    private static BigDecimal dec(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static Number num(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return (Number) o;
        try {
            return new BigDecimal(String.valueOf(o));
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
