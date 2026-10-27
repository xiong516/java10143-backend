package com.java10143.interceptor;

import cn.hutool.core.util.StrUtil;

import com.java10143.util.JwtUtil;
import com.java10143.util.UserContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 「可选登录」拦截器 —— 给 AI 客服用的。
 *
 * 和 AuthInterceptor 的区别：
 *   - AuthInterceptor：没 token 直接 401，必须登录
 *   - 本拦截器：有 token 就解析出用户（顺带带上订单上下文），
 *              没 token / token 过期 也放行，以「访客」身份继续，
 *              这样未登录的访客也能跟客服小京聊两句。
 *
 * 支持的传法和 AuthInterceptor 完全一致：
 *   Authorization: Bearer xxx / Authorization: xxx / token: xxx / ?token=xxx
 */
@Component
@RequiredArgsConstructor
public class OptionalAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) token = request.getHeader("token");
        if (StrUtil.isBlank(token)) token = request.getParameter("token");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7).trim();
        if (token != null) token = token.replace("\"", "").trim();

        Long userId = jwtUtil.parse(token);
        if (userId != null) UserContext.set(userId);
        // 无论有没有登录，都放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
