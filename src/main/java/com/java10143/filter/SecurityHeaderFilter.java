package com.java10143.filter;

import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头（防 MIME 嗅探、防点击劫持、防外链泄露来源）。
 *
 * <p>不处理业务逻辑，只给每个响应加几个头，属于"零风险加固"。
 */
@Component
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse resp = (HttpServletResponse) response;
            // 禁止浏览器猜类型：避免把用户上传/评论里的文本当成脚本执行
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // 不允许被别的网站用 iframe 套住（防点击劫持）
            resp.setHeader("X-Frame-Options", "SAMEORIGIN");
            // 跳转到外站时不带完整来源地址（防信息外泄）
            resp.setHeader("Referrer-Policy", "no-referrer-when-downgrade");
            // 关掉旧版浏览器的 XSS 过滤器（它会误伤正常内容，现代浏览器已内建 CSP 思路）
            resp.setHeader("X-XSS-Protection", "0");
        }
        chain.doFilter(request, response);
    }
}
