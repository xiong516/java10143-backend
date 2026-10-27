package com.java10143.util;

/**
 * 当前登录用户（由 AuthInterceptor 写入，Controller/Service 直接读）
 */
public class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    /** 取不到就抛异常，用于必须登录的接口 */
    public static Long require() {
        Long id = HOLDER.get();
        if (id == null) throw new com.java10143.common.BizException("401", "请先登录");
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
