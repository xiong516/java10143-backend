package com.java10143.service;

import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.entity.OrderItem;
import com.java10143.entity.Orders;
import com.java10143.entity.User;
import com.java10143.mapper.OrderItemMapper;
import com.java10143.mapper.OrdersMapper;
import com.java10143.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AI 智能客服（Java 侧网关）
 *
 * 职责：
 *   1. 从数据库查出这个用户最近的订单（订单号 / 状态 / 商品 / 金额）
 *   2. 把订单上下文 + 用户昵称 + 聊天内容，转发给 Python 的 AI 服务（ai-service/server.py）
 *   3. Python 服务没启动时，自动降级成本地兜底应答，保证客服入口永远不报错
 *
 * 为什么要绕一层？因为「订单上下文」只有 Java 侧连数据库才拿得到，
 * 有了它 AI 才能回答“我这单到哪了”这种带具体订单信息的问题。
 */
@Service
@RequiredArgsConstructor
public class AiService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    @Value("${java10143.ai.url:http://localhost:8001}")
    private String aiUrl;

    /** 访问 Python AI 服务的超时（毫秒）；JDK 8 用 HttpURLConnection，不依赖任何第三方 HTTP 库 */
    private static final int AI_CONNECT_TIMEOUT_MS = 3000;
    private static final int AI_READ_TIMEOUT_MS = 30000;

    /** 订单状态码 -> 中文（JDK 8 没有 Map.of，用静态块初始化） */
    private static final Map<Integer, String> STATE_TEXT = new LinkedHashMap<>();

    static {
        STATE_TEXT.put(1, "待付款");
        STATE_TEXT.put(2, "待发货");
        STATE_TEXT.put(3, "待收货");
        STATE_TEXT.put(4, "待评价");
        STATE_TEXT.put(5, "已完成");
        STATE_TEXT.put(6, "已取消");
    }

    /** 极简 HTTP POST（返回响应体；非 200 返回 null） */
    private static String httpPost(String url, String json, int readTimeoutMs) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(AI_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(readTimeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            if (conn.getResponseCode() != 200) return null;
            return readBody(conn.getInputStream());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 极简 HTTP GET（返回响应体；非 200 返回 null） */
    private static String httpGet(String url, int readTimeoutMs) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(AI_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(readTimeoutMs);
            if (conn.getResponseCode() != 200) return null;
            return readBody(conn.getInputStream());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    /** 聊天主入口（userId 为 null 表示访客，未登录） */
    public Map<String, Object> chat(Long userId, String message, String orderId, String sessionId) {
        boolean loggedIn = userId != null;
        // 访客没有订单上下文，buildOrderContext 会直接返回空 map
        Map<String, Object> order = loggedIn ? buildOrderContext(userId, orderId) : new LinkedHashMap<String, Object>();
        String nickname = loggedIn ? nickname(userId) : null;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", StrUtil.isBlank(sessionId)
                ? (loggedIn ? ("u" + userId) : "guest")
                : sessionId);
        payload.put("userId", loggedIn ? String.valueOf(userId) : "");
        payload.put("nickname", nickname == null ? "" : nickname);
        payload.put("message", message);
        payload.put("order", order);
        // 让 Python 那边知道这是访客，回答时别编造订单信息
        payload.put("guest", !loggedIn);

        Map<String, Object> remote = callPython(payload);
        if (remote != null) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("reply", remote.get("reply"));
            res.put("intent", remote.get("intent"));
            res.put("source", remote.get("source"));
            res.put("sessionId", payload.get("sessionId"));
            res.put("aiOnline", true);
            res.put("loggedIn", loggedIn);
            res.put("orderBrief", brief(order));
            return res;
        }

        // Python 服务没开 —— 本地兜底，保证还能聊
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("reply", fallback(message, order, nickname));
        res.put("intent", "fallback");
        res.put("source", "java");
        res.put("sessionId", payload.get("sessionId"));
        res.put("aiOnline", false);
        res.put("loggedIn", loggedIn);
        res.put("orderBrief", brief(order));
        return res;
    }

    /** AI 服务状态，前端可以用来显示「AI在线/离线」 */
    public Map<String, Object> status() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("url", aiUrl);
        try {
            String body = httpGet(aiUrl + "/health", AI_CONNECT_TIMEOUT_MS);
            res.put("online", body != null);
            res.put("detail", body == null ? "AI 服务未启动" : body);
        } catch (Exception e) {
            res.put("online", false);
            res.put("detail", "AI 服务未启动：" + e.getClass().getSimpleName());
        }
        return res;
    }

    /**
     * AI 商品文案生成：转发给 Python 的 /ai/copy（本地模板洗牌；挂了大模型 Key 会自动升级）。
     * Python 不在线时返回降级提示，管理后台不报错。
     */
    public Map<String, Object> copy(String name, String category) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name == null ? "" : name);
        payload.put("category", category == null ? "" : category);
        try {
            String body = httpPost(aiUrl + "/ai/copy", com.java10143.util.Json.to(payload), 10000);
            if (body != null) {
                Map<String, Object> parsed = com.java10143.util.Json.toMap(body);
                if (parsed != null && "1".equals(String.valueOf(parsed.get("code")))) {
                    Object result = parsed.get("result");
                    if (result instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) result;
                        m.put("source", "python-ai");
                        return m;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // 降级：Python 不在线，给一份最简可用文案，管理后台依旧能用
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("category", "默认");
        java.util.List<String> titles = new ArrayList<>();
        titles.add("【新品上架】" + name);
        titles.add(name + "｜限时特惠，入手正当时");
        fallback.put("titles", titles);
        fallback.put("sellingPoints", new String[]{"品质保障", "官方正品", "极速发货"});
        fallback.put("detail", name + " 京东10143 自营，7 天无理由退换，现在下单享新人优惠。");
        fallback.put("source", "java-fallback");
        return fallback;
    }

    // ---------------------------------------------------------------- 内部

    private Map<String, Object> callPython(Map<String, Object> payload) {
        try {
            String json = com.java10143.util.Json.to(payload);
            String body = httpPost(aiUrl + "/ai/chat", json, AI_READ_TIMEOUT_MS);
            if (body == null) return null;
            Map<String, Object> parsed = com.java10143.util.Json.toMap(body);
            if (parsed == null || parsed.isEmpty()) return null;
            Object result = parsed.get("result");
            // JDK 8：不支持 instanceof 模式匹配，先判断再强转
            if (result instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) result;
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("reply", m.get("reply"));
                out.put("intent", m.get("intent"));
                out.put("source", m.get("source"));
                return out;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> buildOrderContext(Long userId, String orderId) {
        Orders order = null;
        if (orderId != null && !StrUtil.isBlank(orderId)) {
            try {
                Orders o = ordersMapper.selectById(Long.valueOf(orderId));
                if (o != null && o.getUserId().equals(userId)) order = o;
            } catch (Exception ignored) {
                // 订单号不是数字就忽略
            }
        }
        if (order == null) {
            // 没有指定订单号，就取这个用户最近的一单（刚下单的用户大概率问这一单）
            List<Orders> list = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getUserId, userId)
                    .orderByDesc(Orders::getId)
                    .last("limit 1"));
            if (!list.isEmpty()) order = list.get(0);
        }

        Map<String, Object> ctx = new LinkedHashMap<>();
        if (order == null) return ctx;

        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));

        StringBuilder goods = new StringBuilder();
        for (int i = 0; i < items.size() && i < 3; i++) {
            if (i > 0) goods.append("、");
            goods.append(items.get(i).getName());
        }

        ctx.put("orderNo", String.valueOf(order.getId()));
        ctx.put("status", STATE_TEXT.getOrDefault(order.getOrderState(), "处理中"));
        ctx.put("goods", goods.length() == 0 ? "商品" : goods.toString());
        ctx.put("money", order.getPayMoney() == null ? "" : order.getPayMoney().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        ctx.put("createTime", order.getCreateTime() == null ? "" : order.getCreateTime().toString());
        return ctx;
    }

    private Map<String, Object> brief(Map<String, Object> order) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("orderNo", order.get("orderNo"));
        b.put("status", order.get("status"));
        b.put("goods", order.get("goods"));
        b.put("money", order.get("money"));
        return b;
    }

    private String nickname(Long userId) {
        if (userId == null) return null;
        try {
            User u = userMapper.selectById(userId);
            return u == null ? null : u.getNickname();
        } catch (Exception e) {
            return null;
        }
    }

    /** Python 服务不在时的兜底应答，带随机变化，别让用户看到“服务不可用” */
    private String fallback(String message, Map<String, Object> order, String nickname) {
        String who = (nickname == null || StrUtil.isBlank(nickname)) ? "" : (" " + nickname);
        String t = message == null ? "" : message;
        String no = String.valueOf(order.getOrDefault("orderNo", ""));
        String status = String.valueOf(order.getOrDefault("status", ""));
        boolean hasOrder = !"null".equals(no) && !StrUtil.isBlank(no);
        Random r = new Random();

        String[] answers;
        if (t.contains("退") || t.contains("换")) {
            answers = new String[]{
                    "收到" + who + "，退换货 7 天内都可以办。您在订单页点「申请售后」，我这边帮您盯着流程。",
                    who + "退换没问题～我先把工单记下来，人工客服一会儿跟你确认退货方式。"};
        } else if (t.contains("快递") || t.contains("物流") || t.contains("发货") || t.contains("多久")) {
            answers = new String[]{
                    who + "别急，" + (hasOrder ? ("订单 " + no + " 现在是「" + status + "」，") : "")
                            + "仓库一般 24 小时内出库，我帮您盯物流，有更新通知您。",
                    "我帮您催一下仓库" + who + "，" + (hasOrder ? ("订单 " + no + "「" + status + "」，") : "")
                            + "出库后 1~3 天到，超时我主动找您。"};
        } else if (t.contains("发票")) {
            answers = new String[]{
                    who + "发票在订单详情里点「申请开票」就能开，电子票一般当天出。",
                    "开票没问题" + who + "，填好抬头和税号，出票后发您邮箱。"};
        } else if (t.contains("退款") || t.contains("钱")) {
            answers = new String[]{
                    who + "退款走原路返回，审核通过 1~3 个工作日到账，我帮您催审核。",
                    "钱不会少的" + who + "，退款 1~3 个工作日到账，超时您直接找我。"};
        } else {
            answers = new String[]{
                    who + "收到！这个问题我帮您记下了，您可以再说具体一点（比如订单、物流还是售后），我好精准处理。",
                    "我在的" + who + "～您是想问发货、退换还是别的？说个关键词我就懂。"};
        }
        String answer = answers[r.nextInt(answers.length)];
        // 访客问「订单/物流」这类需要查单的问题：提示一下登录
        if (hasOrder == false && (nickname == null || StrUtil.isBlank(nickname))
                && (t.contains("订单") || t.contains("物流") || t.contains("快递") || t.contains("我的"))) {
            answer = answer + "（小提示：登录后我能直接看到您的订单，回答会更准哦～）";
        }
        return answer;
    }
}
