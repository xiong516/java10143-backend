package com.java10143.interceptor;

import com.java10143.common.Guard;
import com.java10143.common.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 接口限流（防刷）。
 *
 * <p>规则（按客户端 IP 统计，时间窗 1 分钟）：
 * <ul>
 *   <li>POST /login          10 次/分（登录最容易被撞库）</li>
 *   <li>/member/ai/chat      20 次/分（AI 对话最贵）</li>
 *   <li>/admin/**           120 次/分</li>
 *   <li>其它接口             300 次/分（兜底，防止整站被刷）</li>
 * </ul>
 *
 * <p>【为什么按 IP】token 可以伪造/共享，IP 是网络层天然标识；
 * 部署在 cpolar 隧道后面时真实 IP 在 X-Forwarded-For 里，优先取它。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String ip = clientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String scope;
        int max;
        if ("/login".equals(uri) && "POST".equalsIgnoreCase(method)) {
            scope = "login";
            max = 10;
        } else if (uri.startsWith("/member/ai/")) {
            scope = "ai";
            max = 20;
        } else if (uri.startsWith("/admin/")) {
            scope = "admin";
            max = 120;
        } else {
            scope = "global";
            max = 300;
        }

        boolean ok = Guard.allow("rl:" + scope + ":" + ip, max, 60_000L);
        if (ok) {
            return true;
        }

        log.warn("[限流] {} {} ip={} 超过 {} 次/分", method, uri, ip, max);
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                R.fail("0", "操作太频繁了，请 1 分钟后再试")));
        return false;
    }

    /** 取真实客户端 IP：隧道/反向代理场景优先 X-Forwarded-For 的第一段 */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && xff.length() > 0) {
            int comma = xff.indexOf(',');
            String first = comma > 0 ? xff.substring(0, comma) : xff;
            first = first.trim();
            if (first.length() > 0) {
                return first;
            }
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && real.trim().length() > 0) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
