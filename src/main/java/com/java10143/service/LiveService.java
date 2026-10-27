package com.java10143.service;

import com.java10143.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 直播带货。
 *
 * 直播间生命周期：0 预告 → 1 直播中 → 2 已结束。
 * 演示环境不接真实推流，视频播放用封面/商品图做 Ken Burns 兜底，重点是
 * 「弹幕滚动 + 挂车商品 + 讲解中商品 + 在线人数」这条电商闭环。
 */
@Service
@RequiredArgsConstructor
public class LiveService {

    private final JdbcTemplate jdbc;

    private static final String[] SYSTEM_MSG = {
            "欢迎来到直播间，点关注不迷路～",
            "本场所有商品均支持 7 天无理由退换",
            "整点抽免单，记得蹲守直播间",
            "下单后 48 小时内发货，急件可备注"
    };

    /** GET /live/list —— 首页直播广场 */
    public Map<String, Object> list() {
        String sql = "SELECT r.*, s.name spu_name, s.price spu_price, s.main_pictures spu_pics "
                + "FROM live_room r LEFT JOIN spu s ON s.id = r.spu_id "
                + "ORDER BY r.status ASC, r.sort ASC, r.id ASC";
        List<Map<String, Object>> rows = jdbc.queryForList(sql);

        List<Map<String, Object>> living = new ArrayList<>();
        List<Map<String, Object>> upcoming = new ArrayList<>();
        List<Map<String, Object>> ended = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> o = card(r);
            int st = r.get("status") == null ? 0 : ((Number) r.get("status")).intValue();
            if (st == 1) living.add(o);
            else if (st == 0) upcoming.add(o);
            else ended.add(o);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("living", living);
        m.put("upcoming", upcoming);
        m.put("ended", ended);
        m.put("livingCount", living.size());
        m.put("totalViewer", living.stream()
                .mapToLong(x -> ((Number) x.get("viewerCount")).longValue()).sum());
        m.put("serverTime", System.currentTimeMillis());
        return m;
    }

    /** GET /live/{id} —— 直播间详情（含挂车商品、历史弹幕） */
    public Map<String, Object> detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.*, s.name spu_name, s.price spu_price, s.main_pictures spu_pics "
                        + "FROM live_room r LEFT JOIN spu s ON s.id = r.spu_id WHERE r.id = ?", id);
        if (rows.isEmpty()) throw new BizException("直播间不存在");
        Map<String, Object> room = card(rows.get(0));

        // 挂车商品
        List<Map<String, Object>> goods = jdbc.queryForList(
                "SELECT g.id, g.spu_id, g.sku_id, g.live_price, g.stock, g.sold, g.is_spot, "
                        + "s.name, s.price spu_price, s.old_price, s.main_pictures, s.sales_count, s.inventory "
                        + "FROM live_goods g JOIN spu s ON s.id = g.spu_id "
                        + "WHERE g.room_id = ? ORDER BY g.sort ASC", id);
        List<Map<String, Object>> gList = new ArrayList<>();
        for (Map<String, Object> g : goods) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", g.get("id"));
            o.put("spuId", g.get("spu_id"));
            o.put("skuId", g.get("sku_id"));
            o.put("name", g.get("name"));
            // 必须走 ImgUrl.abs()：① 按请求 Host 拼成绝对地址（手机拿到局域网 IP 才能显示）
            //                  ② 把 .svg 改写成 .png（鸿蒙 Image 按后缀选解码器，svg 后缀 + PNG 字节 = 白图）
            o.put("pic", com.java10143.util.ImgUrl.abs(firstPic(g.get("main_pictures"))));
            BigDecimal lp = (BigDecimal) g.get("live_price");
            BigDecimal sp = (BigDecimal) g.get("spu_price");
            o.put("livePrice", money(lp));
            o.put("originPrice", money(sp));
            if (lp != null && sp != null && sp.doubleValue() > 0) {
                double off = lp.doubleValue() / sp.doubleValue() * 10;
                o.put("off", String.format("%.1f 折", off));
                o.put("save", money(sp.subtract(lp)));
            }
            int stock = g.get("stock") == null ? 0 : ((Number) g.get("stock")).intValue();
            int sold = g.get("sold") == null ? 0 : ((Number) g.get("sold")).intValue();
            o.put("stock", stock);
            o.put("sold", sold);
            o.put("left", Math.max(stock - sold, 0));
            o.put("soldPercent", stock > 0 ? Math.min(100, (int) Math.round(sold * 100.0 / stock)) : 0);
            o.put("spot", g.get("is_spot") != null && ((Number) g.get("is_spot")).intValue() == 1);
            gList.add(o);
        }
        room.put("goods", gList);

        // 弹幕
        List<Map<String, Object>> msgs = jdbc.queryForList(
                "SELECT id, nickname, content, type, create_time FROM live_message "
                        + "WHERE room_id = ? ORDER BY id DESC LIMIT 40", id);
        List<Map<String, Object>> mList = new ArrayList<>();
        for (int i = msgs.size() - 1; i >= 0; i--) { // 反转让最早的在上
            Map<String, Object> r = msgs.get(i);
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", r.get("id"));
            o.put("nickname", r.get("nickname"));
            o.put("content", r.get("content"));
            o.put("type", r.get("type"));
            mList.add(o);
        }
        room.put("messages", mList);
        return room;
    }

    /** POST /member/live/{id}/join —— 进场 +1 在线 */
    public Map<String, Object> join(Long id) {
        jdbc.update("UPDATE live_room SET online_count = online_count + 1, viewer_count = viewer_count + 1 WHERE id = ?", id);
        insertMsg(id, null, "我", "来了来了", 1);
        return counter(id);
    }

    /** POST /member/live/{id}/like —— 点赞 */
    public Map<String, Object> like(Long id) {
        jdbc.update("UPDATE live_room SET like_count = like_count + 1 WHERE id = ?", id);
        return counter(id);
    }

    /** POST /member/live/{id}/message —— 发弹幕 */
    public Map<String, Object> send(Long id, Long userId, String nickname, String content) {
        if (content == null || content.trim().isEmpty()) throw new BizException("弹幕内容不能为空");
        String t = content.trim();
        if (t.length() > 60) t = t.substring(0, 60);
        insertMsg(id, userId, nickname, t, 0);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("ok", true);
        o.put("content", t);
        return o;
    }

    /** GET /live/{id}/messages?afterId= —— 增量拉弹幕（前端 2.5s 轮询） */
    public List<Map<String, Object>> messages(Long id, Long afterId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, nickname, content, type FROM live_message WHERE room_id = ? AND id > ? ORDER BY id ASC LIMIT 30",
                id, afterId == null ? 0L : afterId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", r.get("id"));
            o.put("nickname", r.get("nickname"));
            o.put("content", r.get("content"));
            o.put("type", r.get("type"));
            res.add(o);
        }
        return res;
    }

    /** 前端补充「虚拟观众弹幕」用：取一条随机话术 */
    public String randomDanmu() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return SYSTEM_MSG[r.nextInt(SYSTEM_MSG.length)];
    }

    /* ----------------- 内部工具 ----------------- */

    private void insertMsg(Long roomId, Long userId, String nick, String content, int type) {
        jdbc.update("INSERT INTO live_message(room_id,user_id,nickname,content,type,create_time) VALUES(?,?,?,?,?,NOW())",
                roomId, userId, nick, content, type);
    }

    private Map<String, Object> counter(Long id) {
        List<Map<String, Object>> r = jdbc.queryForList(
                "SELECT online_count, viewer_count, like_count, goods_count FROM live_room WHERE id = ?", id);
        Map<String, Object> o = new LinkedHashMap<>();
        if (!r.isEmpty()) {
            o.put("onlineCount", ((Number) r.get(0).get("online_count")).longValue());
            o.put("viewerCount", ((Number) r.get(0).get("viewer_count")).longValue());
            o.put("likeCount", ((Number) r.get(0).get("like_count")).longValue());
            o.put("goodsCount", ((Number) r.get(0).get("goods_count")).longValue());
        }
        return o;
    }

    private Map<String, Object> card(Map<String, Object> r) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", r.get("id"));
        o.put("title", r.get("title"));
        // 封面同样要走 ImgUrl.abs()：绝对地址 + .svg→.png（鸿蒙端才能显示）
        Object rawCover = r.get("cover") != null ? r.get("cover") : r.get("spu_pics");
        o.put("cover", com.java10143.util.ImgUrl.abs(firstPic(rawCover)));
        o.put("anchorName", r.get("anchor_name"));
        o.put("anchorAvatar", com.java10143.util.ImgUrl.abs(
                r.get("anchor_avatar") == null ? null : String.valueOf(r.get("anchor_avatar"))));
        o.put("intro", r.get("intro"));
        o.put("spuId", r.get("spu_id"));
        o.put("spuName", r.get("spu_name"));
        int st = r.get("status") == null ? 0 : ((Number) r.get("status")).intValue();
        o.put("status", st);
        o.put("statusText", st == 1 ? "直播中" : (st == 0 ? "预告" : "已结束"));
        o.put("viewerCount", r.get("viewer_count") == null ? 0 : ((Number) r.get("viewer_count")).longValue());
        o.put("onlineCount", r.get("online_count") == null ? 0 : ((Number) r.get("online_count")).longValue());
        o.put("likeCount", r.get("like_count") == null ? 0 : ((Number) r.get("like_count")).longValue());
        o.put("goodsCount", r.get("goods_count") == null ? 0 : ((Number) r.get("goods_count")).longValue());
        Date st1 = toDate(r.get("start_time"));
        Date et1 = toDate(r.get("end_time"));
        SimpleDateFormat f = new SimpleDateFormat("MM-dd HH:mm");
        o.put("startTime", st1 == null ? null : f.format(st1));
        o.put("endTime", et1 == null ? null : f.format(et1));
        o.put("startAt", st1 == null ? 0L : st1.getTime());
        o.put("endAt", et1 == null ? 0L : et1.getTime());
        o.put("serverTime", System.currentTimeMillis());
        if (st == 0 && st1 != null) {
            long ms = Math.max(st1.getTime() - System.currentTimeMillis(), 0);
            o.put("countdownMs", (double) ms);
        } else if (st == 1 && et1 != null) {
            long ms = Math.max(et1.getTime() - System.currentTimeMillis(), 0);
            o.put("countdownMs", (double) ms);
        } else {
            o.put("countdownMs", 0d);
        }
        return o;
    }

    /** JDBC 取 datetime 列可能是 java.sql.Timestamp / LocalDateTime，统一转 Date */
    static Date toDate(Object v) {
        if (v == null) return null;
        if (v instanceof Date) return (Date) v;
        if (v instanceof java.time.LocalDateTime) {
            return Date.from(((java.time.LocalDateTime) v).atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (v instanceof java.time.LocalDate) {
            return Date.from(((java.time.LocalDate) v).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }
        String s = String.valueOf(v).replace('T', ' ').trim();
        if (s.length() > 19) s = s.substring(0, 19);
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(s);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /** main_pictures 是逗号或 JSON 数组存的，取第一张 */
    static String firstPic(Object raw) {
        if (raw == null) return "";
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) return "";
        if (s.startsWith("[")) {
            s = s.replaceAll("[\\[\\]\"]", "");
            String[] arr = s.split(",");
            return arr.length > 0 ? arr[0].trim() : "";
        }
        String[] arr = s.split(",");
        return arr[0].trim();
    }

    private static double money(BigDecimal v) {
        if (v == null) return 0d;
        return v.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
