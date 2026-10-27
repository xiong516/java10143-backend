package com.java10143.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java10143.common.R;
import com.java10143.service.UserService;
import com.java10143.util.UserContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员拦截器：/admin/** 只有 role=admin 的用户能进。
 * 必须注册在 AuthInterceptor 之后（AuthInterceptor 已把 userId 写进 UserContext）。
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = UserContext.get();
        if (userId != null && userService.isAdmin(userId)) {
            return true;
        }
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(
                R.fail("403", "仅管理员可操作")));
        response.getWriter().flush();
        return false;
    }
}
