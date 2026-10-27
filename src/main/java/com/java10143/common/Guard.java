package com.java10143.common;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量防护工具（纯内存，不依赖 Redis/中间件，课程项目够用）。
 *
 * <p>提供两类能力：
 * <ol>
 *   <li><b>限流</b>：同一个 key（IP/接口）在时间窗内最多允许 N 次</li>
 *   <li><b>登录失败锁定</b>：同一账号连续失败 5 次，锁 5 分钟，挡住暴力破解</li>
 * </ol>
 *
 * 【注意】单机内存实现，服务重启即清零；多实例部署时要换成 Redis。
 */
public final class Guard {

    private Guard() {
    }

    /** 限流窗口：key -> {窗口起点毫秒, 已用次数} */
    private static final Map<String, long[]> WINDOW = new ConcurrentHashMap<>();
    /** 失败计数：key -> {首次失败毫秒, 连续失败次数} */
    private static final Map<String, long[]> FAILS = new ConcurrentHashMap<>();

    private static final int MAX_FAIL = 5;
    private static final long LOCK_MS = 5 * 60 * 1000L;
    /** 计数表超过这个量级就顺手清理过期项，防止长期运行内存膨胀 */
    private static final int CLEAN_THRESHOLD = 5000;

    /**
     * 限流判断。
     *
     * @param key      限流键（一般用 接口+IP）
     * @param max      窗口内最大次数
     * @param windowMs 窗口长度（毫秒）
     * @return true=放行，false=超限
     */
    public static boolean allow(String key, int max, long windowMs) {
        long now = System.currentTimeMillis();
        long[] w = WINDOW.get(key);
        if (w == null) {
            w = new long[]{now, 0};
            WINDOW.put(key, w);
            cleanup(now);
        }
        synchronized (w) {
            if (now - w[0] >= windowMs) {
                w[0] = now;
                w[1] = 0;
            }
            if (w[1] >= max) {
                return false;
            }
            w[1]++;
            return true;
        }
    }

    /** 还剩多少毫秒解锁，0 表示没锁 */
    public static long lockLeft(String key) {
        long[] f = FAILS.get(key);
        if (f == null) {
            return 0;
        }
        synchronized (f) {
            if (f[1] < MAX_FAIL) {
                return 0;
            }
            long left = LOCK_MS - (System.currentTimeMillis() - f[0]);
            if (left <= 0) {
                FAILS.remove(key);
                return 0;
            }
            return left;
        }
    }

    /** 记一次失败（超过阈值后开始计时锁定） */
    public static void fail(String key) {
        long now = System.currentTimeMillis();
        long[] f = FAILS.get(key);
        if (f == null) {
            f = new long[]{now, 0};
            FAILS.put(key, f);
            cleanup(now);
        }
        synchronized (f) {
            if (f[1] == 0) {
                f[0] = now;
            }
            f[1]++;
            // 锁定到期后重新计数
            if (f[1] > MAX_FAIL && now - f[0] > LOCK_MS) {
                f[0] = now;
                f[1] = 1;
            }
        }
    }

    /** 登录成功后清空失败计数 */
    public static void success(String key) {
        FAILS.remove(key);
    }

    /** 过期数据清理：只在表变大时触发，避免每次请求都遍历 */
    private static void cleanup(long now) {
        if (WINDOW.size() > CLEAN_THRESHOLD) {
            Iterator<Map.Entry<String, long[]>> it = WINDOW.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, long[]> e = it.next();
                long[] w = e.getValue();
                synchronized (w) {
                    if (now - w[0] > 10 * 60 * 1000L) {
                        it.remove();
                    }
                }
            }
        }
        if (FAILS.size() > CLEAN_THRESHOLD) {
            Iterator<Map.Entry<String, long[]>> it = FAILS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, long[]> e = it.next();
                long[] f = e.getValue();
                synchronized (f) {
                    if (f[1] < MAX_FAIL && now - f[0] > 30 * 60 * 1000L) {
                        it.remove();
                    } else if (f[1] >= MAX_FAIL && now - f[0] > LOCK_MS) {
                        it.remove();
                    }
                }
            }
        }
    }
}
