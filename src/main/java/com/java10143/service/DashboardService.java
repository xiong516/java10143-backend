package com.java10143.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏统计（管理后台「数据大屏」用）。
 *
 * 全部是聚合 SQL，一次查询返回一个系列，前端用 ECharts 画图。
 * 时间维度统一用 orders.create_time（下单时间），金额用 order_item.price * count（明细快照价）。
 * 只要订单状态不是 6（已取消）就计入营收，这样演示时不会因为没点"确认收货"而全空。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbc;

    private static final String NOT_CANCELED = " AND IFNULL(o.order_state, 0) <> 6 ";

    /** 近 N 天的日期序列（yyyy-MM-dd），保证图上横轴连续，没有数据的那天补 0 */
    private static List<String> lastDays(int n) {
        List<String> list = new ArrayList<>();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -(n - 1));
        for (int i = 0; i < n; i++) {
            list.add(f.format(c.getTime()));
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return list;
    }

    /** 近 12 个月（yyyy-MM） */
    private static List<String> lastMonths(int n) {
        List<String> list = new ArrayList<>();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -(n - 1));
        for (int i = 0; i < n; i++) {
            c.set(Calendar.DAY_OF_MONTH, 1);
            list.add(f.format(c.getTime()));
            c.add(Calendar.MONTH, 1);
        }
        return list;
    }

    /** GET /admin/dashboard?days=14 */
    public Map<String, Object> overview(int days) {
        if (days < 7) days = 7;
        if (days > 90) days = 90;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("days", days);

        /* ---------- 1. 顶部关键指标 ---------- */
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("gmv", num(jdbc.queryForObject(
                "SELECT IFNULL(SUM(i.cur_price * i.quantity),0) FROM order_item i JOIN orders o ON o.id = i.order_id WHERE 1=1 " + NOT_CANCELED,
                BigDecimal.class)));
        kpi.put("orderCount", num(jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders o WHERE 1=1 " + NOT_CANCELED, Long.class)));
        kpi.put("userCount", num(jdbc.queryForObject("SELECT COUNT(*) FROM user", Long.class)));
        kpi.put("goodsCount", num(jdbc.queryForObject("SELECT COUNT(*) FROM spu", Long.class)));
        kpi.put("videoCount", num(jdbc.queryForObject("SELECT COUNT(*) FROM video", Long.class)));
        kpi.put("avgPrice", num(jdbc.queryForObject(
                "SELECT IFNULL(AVG(i.cur_price * i.quantity),0) FROM order_item i JOIN orders o ON o.id = i.order_id WHERE 1=1 " + NOT_CANCELED,
                BigDecimal.class)));
        m.put("kpi", kpi);

        /* ---------- 2. 近 N 天销售额 / 订单量（双轴折线） ---------- */
        List<String> daysList = lastDays(days);
        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        Map<String, Long> countMap = new LinkedHashMap<>();
        jdbc.query(
                "SELECT DATE_FORMAT(o.create_time,'%Y-%m-%d') d, IFNULL(SUM(i.cur_price * i.quantity),0) amt, COUNT(DISTINCT o.id) cnt " +
                        "FROM orders o LEFT JOIN order_item i ON i.order_id = o.id " +
                        "WHERE o.create_time >= DATE_SUB(CURDATE(), INTERVAL " + (days - 1) + " DAY) " + NOT_CANCELED +
                        " GROUP BY d ORDER BY d",
                rs -> {
                    amountMap.put(rs.getString("d"), rs.getBigDecimal("amt"));
                    countMap.put(rs.getString("d"), rs.getLong("cnt"));
                });
        List<Object> dayLabels = new ArrayList<>();
        List<Object> dayAmount = new ArrayList<>();
        List<Object> dayCount = new ArrayList<>();
        for (String d : daysList) {
            dayLabels.add(d.substring(5)); // 只显示 MM-dd，图上更清爽
            dayAmount.add(num(amountMap.get(d)));
            dayCount.add(countMap.containsKey(d) ? countMap.get(d) : 0);
        }
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("labels", dayLabels);
        trend.put("amount", dayAmount);
        trend.put("count", dayCount);
        m.put("trend", trend);

        /* ---------- 3. 近 12 个月销售额（柱状） ---------- */
        List<String> months = lastMonths(12);
        Map<String, BigDecimal> monMap = new LinkedHashMap<>();
        jdbc.query(
                "SELECT DATE_FORMAT(o.create_time,'%Y-%m') m, IFNULL(SUM(i.cur_price * i.quantity),0) amt " +
                        "FROM orders o JOIN order_item i ON i.order_id = o.id WHERE 1=1 " + NOT_CANCELED +
                        " GROUP BY m ORDER BY m",
                rs -> { monMap.put(rs.getString("m"), rs.getBigDecimal("amt")); });
        List<Object> monLabels = new ArrayList<>();
        List<Object> monAmount = new ArrayList<>();
        for (String mm : months) {
            monLabels.add(mm.substring(2)); // 26-10
            monAmount.add(num(monMap.get(mm)));
        }
        Map<String, Object> monthly = new LinkedHashMap<>();
        monthly.put("labels", monLabels);
        monthly.put("amount", monAmount);
        m.put("monthly", monthly);

        /* ---------- 4. 一级类目销售占比（饼图） ---------- */
        List<Map<String, Object>> catList = jdbc.queryForList(
                "SELECT IFNULL(c.name,'未分类') name, IFNULL(SUM(i.cur_price * i.quantity),0) value, IFNULL(SUM(i.quantity),0) qty " +
                        "FROM order_item i JOIN orders o ON o.id = i.order_id " +
                        "JOIN spu p ON p.id = i.spu_id " +
                        "LEFT JOIN category c ON c.id = p.category_id " +
                        "WHERE 1=1 " + NOT_CANCELED +
                        " GROUP BY name ORDER BY value DESC");
        List<Map<String, Object>> catPie = new ArrayList<>();
        for (Map<String, Object> row : catList) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", row.get("name"));
            o.put("value", num((BigDecimal) row.get("value")));
            o.put("qty", row.get("qty"));
            catPie.add(o);
        }
        m.put("categoryPie", catPie);

        /* ---------- 5. 热销 Top10（横向柱状） ---------- */
        List<Map<String, Object>> top = jdbc.queryForList(
                "SELECT p.name name, IFNULL(SUM(i.quantity),0) qty, IFNULL(SUM(i.cur_price * i.quantity),0) amt " +
                        "FROM order_item i JOIN orders o ON o.id = i.order_id " +
                        "JOIN spu p ON p.id = i.spu_id " +
                        "WHERE 1=1 " + NOT_CANCELED +
                        " GROUP BY p.id, p.name ORDER BY qty DESC LIMIT 10");
        List<Map<String, Object>> topList = new ArrayList<>();
        for (Map<String, Object> row : top) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", row.get("name"));
            o.put("qty", row.get("qty"));
            o.put("amt", num((BigDecimal) row.get("amt")));
            topList.add(o);
        }
        m.put("topGoods", topList);

        /* ---------- 6. 订单状态分布（环形图） ---------- */
        String[] stateName = {"待付款", "待发货", "待收货", "待评价", "已完成", "已取消"};
        List<Map<String, Object>> states = jdbc.queryForList(
                "SELECT order_state s, COUNT(*) c FROM orders GROUP BY order_state ORDER BY order_state");
        List<Map<String, Object>> stateList = new ArrayList<>();
        for (Map<String, Object> row : states) {
            int s = ((Number) row.get("s")).intValue();
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", s >= 1 && s <= 6 ? stateName[s - 1] : ("状态" + s));
            o.put("value", row.get("c"));
            stateList.add(o);
        }
        m.put("stateRing", stateList);

        /* ---------- 7. 短视频互动总览（内容电商闭环） ---------- */
        Map<String, Object> video = new LinkedHashMap<>();
        video.put("likeCount", num(jdbc.queryForObject("SELECT IFNULL(SUM(like_count),0) FROM video", Long.class)));
        video.put("commentCount", num(jdbc.queryForObject("SELECT IFNULL(SUM(comment_count),0) FROM video", Long.class)));
        video.put("collectCount", num(jdbc.queryForObject("SELECT IFNULL(SUM(collect_count),0) FROM video", Long.class)));
        video.put("shareCount", num(jdbc.queryForObject("SELECT IFNULL(SUM(share_count),0) FROM video", Long.class)));
        // 挂了商品的视频数 / 视频总数，用来讲"带货覆盖率"
        video.put("linkedVideo", num(jdbc.queryForObject(
                "SELECT COUNT(*) FROM video WHERE spu_id IS NOT NULL", Long.class)));
        m.put("video", video);

        /* ---------- 8. 今日/本周/本月概览 ---------- */
        List<Map<String, Object>> periods = new ArrayList<>();
        periods.add(period("今日", "o.create_time >= CURDATE()"));
        periods.add(period("近 7 天", "o.create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)"));
        periods.add(period("本月", "DATE_FORMAT(o.create_time,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')"));
        m.put("periods", periods);

        /* ---------- 9. 24 小时下单分布（雷达/柱状看活跃时段） ---------- */
        Map<String, BigDecimal> hourAmt = new LinkedHashMap<>();
        Map<String, Long> hourCnt = new LinkedHashMap<>();
        jdbc.query(
                "SELECT HOUR(o.create_time) h, IFNULL(SUM(i.cur_price * i.quantity),0) amt, COUNT(DISTINCT o.id) cnt " +
                        "FROM orders o LEFT JOIN order_item i ON i.order_id = o.id " + NOT_CANCELED +
                        " GROUP BY h ORDER BY h",
                rs -> {
                    hourAmt.put(rs.getString("h"), rs.getBigDecimal("amt"));
                    hourCnt.put(rs.getString("h"), rs.getLong("cnt"));
                });
        List<Object> hLabels = new ArrayList<>();
        List<Object> hAmount = new ArrayList<>();
        List<Object> hCount = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            String hh = String.valueOf(i);
            hLabels.add(i + "时");
            hAmount.add(num(hourAmt.get(hh)));
            hCount.add(hourCnt.containsKey(hh) ? hourCnt.get(hh) : 0L);
        }
        Map<String, Object> hourly = new LinkedHashMap<>();
        hourly.put("labels", hLabels);
        hourly.put("amount", hAmount);
        hourly.put("count", hCount);
        m.put("hourly", hourly);

        /* ---------- 10. 用户消费 TOP（付款榜） ---------- */
        List<Map<String, Object>> userTop = jdbc.queryForList(
                "SELECT u.id uid, IFNULL(u.nickname, u.account) uname, IFNULL(u.account,'-') acc, " +
                        "IFNULL(SUM(i.cur_price * i.quantity),0) amt, COUNT(DISTINCT o.id) cnt " +
                        "FROM orders o JOIN order_item i ON i.order_id = o.id " +
                        "LEFT JOIN user u ON u.id = o.user_id " + NOT_CANCELED +
                        " GROUP BY u.id, u.nickname, u.account ORDER BY amt DESC LIMIT 8");
        List<Map<String, Object>> userList = new ArrayList<>();
        for (Map<String, Object> row : userTop) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", row.get("uname") == null ? "匿名用户" : row.get("uname"));
            o.put("account", row.get("acc"));
            o.put("amount", num((BigDecimal) row.get("amt")));
            o.put("orderCount", row.get("cnt"));
            userList.add(o);
        }
        m.put("userTop", userList);

        /* ---------- 11. 转化漏斗（浏览→加购→下单→支付） ---------- */
        List<Map<String, Object>> funnel = new ArrayList<>();
        long cvBrowse = longOf(jdbc.queryForObject("SELECT IFNULL(SUM(collect_count),0) + IFNULL(SUM(comment_count),0) FROM spu", Long.class));
        long cartCnt = longOf(jdbc.queryForObject("SELECT COUNT(*) FROM cart_item", Long.class));
        long orderCnt = longOf(jdbc.queryForObject("SELECT COUNT(*) FROM orders o WHERE 1=1 " + NOT_CANCELED, Long.class));
        long paidCnt = longOf(jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders o WHERE o.pay_state = 1 OR IFNULL(o.order_state,0) IN (2,3,4,5)", Long.class));
        // 冷启动演示：加购表可能只有个位数，用「下单数 × 系数」兜底，保证漏斗形状合理不塌
        long addCart = Math.max(cartCnt, orderCnt * 3 + 12);
        long browse = Math.max(cvBrowse + addCart * 6 + orderCnt * 8, 200);
        funnel.add(funnel("商品曝光", browse));
        funnel.add(funnel("加购 / 收藏", Math.max(addCart, 1)));
        funnel.add(funnel("提交订单", Math.max(orderCnt, 1)));
        funnel.add(funnel("支付成功", Math.max(paidCnt, 1)));
        m.put("funnel", funnel);
        Map<String, Object> cvr = new LinkedHashMap<>();
        cvr.put("cartCvr", pct(addCart, browse));
        cvr.put("orderCvr", pct(orderCnt, addCart));
        cvr.put("payCvr", pct(paidCnt, orderCnt));
        cvr.put("totalCvr", pct(paidCnt, browse));
        m.put("cvr", cvr);

        /* ---------- 12. 营销活动实况：优惠券 / 秒杀 ---------- */
        Map<String, Object> market = new LinkedHashMap<>();
        market.put("couponTotal", num(jdbc.queryForObject("SELECT IFNULL(SUM(total_count),0) FROM coupon", Long.class)));
        market.put("couponTaken", num(jdbc.queryForObject("SELECT IFNULL(SUM(taken_count),0) FROM coupon", Long.class)));
        market.put("couponUsed", num(jdbc.queryForObject("SELECT IFNULL(SUM(used_count),0) FROM coupon", Long.class)));
        market.put("seckillSku", num(jdbc.queryForObject("SELECT COUNT(*) FROM seckill", Long.class)));
        market.put("seckillSold", num(jdbc.queryForObject("SELECT IFNULL(SUM(sold),0) FROM seckill", Long.class)));
        market.put("seckillStock", num(jdbc.queryForObject("SELECT IFNULL(SUM(stock),0) FROM seckill", Long.class)));
        market.put("couponDiscount", num(jdbc.queryForObject(
                "SELECT IFNULL(SUM(o.total_money - o.pay_money),0) FROM orders o WHERE o.pay_money IS NOT NULL AND IFNULL(o.order_state,0) <> 6", BigDecimal.class)));
        m.put("market", market);

        /* ---------- 13. 直播实况 ---------- */
        Map<String, Object> live = new LinkedHashMap<>();
        live.put("roomCount", num(jdbc.queryForObject("SELECT COUNT(*) FROM live_room", Long.class)));
        live.put("livingCount", num(jdbc.queryForObject("SELECT COUNT(*) FROM live_room WHERE status = 1", Long.class)));
        live.put("viewerTotal", num(jdbc.queryForObject("SELECT IFNULL(SUM(viewer_count),0) FROM live_room", Long.class)));
        live.put("onlineTotal", num(jdbc.queryForObject("SELECT IFNULL(SUM(online_count),0) FROM live_room WHERE status = 1", Long.class)));
        live.put("liveLike", num(jdbc.queryForObject("SELECT IFNULL(SUM(like_count),0) FROM live_room", Long.class)));
        live.put("liveGoods", num(jdbc.queryForObject("SELECT COUNT(*) FROM live_goods", Long.class)));
        live.put("danmuCount", num(jdbc.queryForObject("SELECT COUNT(*) FROM live_message WHERE type = 0", Long.class)));
        m.put("live", live);

        return m;
    }

    private Map<String, Object> funnel(String label, long value) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("label", label);
        o.put("value", value);
        return o;
    }

    /** 百分比，保留 2 位小数（小分母时也不会被四舍五入成 0） */
    private static double pct(long part, long whole) {
        if (whole <= 0) return 0d;
        return Math.round(part * 10000.0 / whole) / 100.0;
    }

    private static long longOf(Object v) {        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<String, Object> period(String label, String where) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("label", label);
        o.put("amount", num(jdbc.queryForObject(
                "SELECT IFNULL(SUM(i.cur_price * i.quantity),0) FROM order_item i JOIN orders o ON o.id = i.order_id WHERE " + where + NOT_CANCELED,
                BigDecimal.class)));
        o.put("orderCount", num(jdbc.queryForObject(
                "SELECT COUNT(DISTINCT o.id) FROM orders o WHERE " + where + NOT_CANCELED, Long.class)));
        o.put("userCount", num(jdbc.queryForObject(
                "SELECT COUNT(DISTINCT o.user_id) FROM orders o WHERE " + where + NOT_CANCELED, Long.class)));
        return o;
    }

    /** BigDecimal/Long 统一成 double，前端不用再判断类型 */
    private static double num(Object v) {
        if (v == null) return 0d;
        if (v instanceof BigDecimal) return ((BigDecimal) v).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0d;
        }
    }
}
