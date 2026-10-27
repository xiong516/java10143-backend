package com.java10143.interceptor;

import cn.hutool.core.util.StrUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java10143.common.R;
import com.java10143.util.JwtUtil;
import com.java10143.util.UserContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器：/member/** 和 /pay/** 必须带 token。
 * 兼容鸿蒙端几种不同的传法：
 *    Authorization: Bearer xxx
 *    Authorization: xxx
 *    token: xxx
 *    ?token=xxx
 */
@Component
// @RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    public AuthInterceptor(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
}
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || StrUtil.isBlank(token)) token = request.getHeader("token");
        if (token == null || StrUtil.isBlank(token)) token = request.getParameter("token");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7).trim();
        if (token != null) token = token.replace("\"", "").trim();

        Long userId = jwtUtil.parse(token);
        if (userId == null) {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(MAPPER.writeValueAsString(
                    R.fail("401", "登录已失效，请重新登录")));
            response.getWriter().flush();
            return false;
        }
        UserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
