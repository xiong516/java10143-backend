package com.java10143.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// 【注意】JDK 8 / Spring Boot 2.x 必须使用 javax.servlet，而不是 jakarta.servlet
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 【本项目最关键的一个适配器】
 *
 * 鸿蒙端 myreqest.ets 里，即使是 GET 请求，参数也是放在 body 里发出去的，例如：
 *      GET  /category          body: {"id":"1"}
 *      GET  /goods             body: {"id":"9"}
 *      GET  /home/banner       body: {"distributionSite":"2"}
 *      GET  /member/order/pre  body: ["1","2"]      （数组＝skuId 集合）
 *
 * 原生 Spring 只认 URL 上的 ?id=1，所以这个过滤器把 JSON body 里的键值
 * 变成 request parameter，这样控制器里 @RequestParam 就能正常拿到。
 */
@WebFilter(urlPatterns = "/*")
public class JsonBodyParamFilter implements Filter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 【修复】JDK 8 不支持 instanceof 模式匹配，必须先判断再强转
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;

        String contentType = req.getContentType();
        // 只处理 application/json 的请求，其它（表单、静态资源、OPTIONS 预检）直接放行
        if (contentType == null || !contentType.toLowerCase().contains("application/json")
                || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        byte[] body = StreamUtils.copyToByteArray(req.getInputStream());
        Map<String, String[]> extra = new LinkedHashMap<>();

        if (body.length > 0) {
            try {
                JsonNode node = MAPPER.readTree(body);
                if (node != null && node.isObject()) {
                    // 【修复】为了 JDK 8 兼容性，使用传统的 Iterator 遍历 fields，替代 forEachRemaining
                    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> e = fields.next();
                        JsonNode v = e.getValue();
                        // 对象/数组原样转成 JSON 字符串，控制器里自己再解析
                        extra.put(e.getKey(), new String[]{v.isValueNode() ? v.asText() : v.toString()});
                    }
                } else if (node != null && node.isArray()) {
                    // 数组默认当作 skuId 集合（/member/order/pre 就是这么传的）
                    List<String> list = new ArrayList<>();
                    for (JsonNode n : node) {
                        list.add(n.asText());
                    }
                    // String.join 在 JDK 8 中是支持的
                    extra.put("skuIds", new String[]{String.join(",", list)});
                    extra.put("ids", new String[]{String.join(",", list)});
                }
            } catch (Exception ignored) {
                // body 不是合法 JSON（比如 "ss"）就忽略
            }
        }

        chain.doFilter(new BodyCachedRequest(req, body, extra), response);
    }

    /** 既能把 body 缓存下来给 @RequestBody 用，又能把 body 里的键值变成 query 参数 */
    private static class BodyCachedRequest extends HttpServletRequestWrapper {

        private final byte[] body;
        private final Map<String, String[]> extra;

        BodyCachedRequest(HttpServletRequest request, byte[] body, Map<String, String[]> extra) {
            super(request);
            this.body = body;
            this.extra = extra;
        }

        @Override
        public ServletInputStream getInputStream() {
            // 【修复】JDK 8 匿名内部类访问局部变量必须是 final 或等效 final
            final ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                public boolean isFinished() {
                    return in.available() == 0;
                }

                public boolean isReady() {
                    return true;
                }

                public void setReadListener(ReadListener l) {
                }

                public int read() {
                    return in.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public String getParameter(String name) {
            // URL 上的参数优先
            String v = super.getParameter(name);
            if (v != null) return v;
            String[] arr = extra.get(name);
            return arr != null && arr.length > 0 ? arr[0] : null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            Set<String> names = new LinkedHashSet<>(extra.keySet());
            Enumeration<String> origin = super.getParameterNames();
            while (origin.hasMoreElements()) names.add(origin.nextElement());
            return Collections.enumeration(names);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] v = super.getParameterValues(name);
            if (v != null) return v;
            return extra.get(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> map = new LinkedHashMap<>(extra);
            map.putAll(super.getParameterMap());
            return map;
        }
    }
}