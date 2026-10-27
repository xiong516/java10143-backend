package com.java10143.util;

import cn.hutool.core.util.StrUtil;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的 JWT 工具：登录时签发，之后所有需要登录的接口用它解析出 userId
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${java10143.jwt.secret}")
    private String secret;

    @Value("${java10143.jwt.expire-hours:72}")
    private int expireHours;

    /** 生成 token */
    public String create(Long userId, String account) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("account", account);
        payload.put("exp",
                DateUtil.offsetHour(new Date(), expireHours).getTime() / 1000);
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 解析 token，失败返回 null */
    public Long parse(String token) {
        if (token == null || StrUtil.isBlank(token)) return null;
        try {
            JWT jwt = JWTUtil.parseToken(token).setKey(secret.getBytes(StandardCharsets.UTF_8));
            if (!jwt.verify()) return null;
            Object userId = jwt.getPayload("userId");
            return userId == null ? null : Long.valueOf(String.valueOf(userId));
        } catch (Exception e) {
            log.debug("token 解析失败：{}", e.getMessage());
            return null;
        }
    }
}
