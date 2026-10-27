package com.java10143.controller;

import com.java10143.common.BizException;
import com.java10143.common.R;
import com.java10143.dto.LoginReq;
import com.java10143.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * 登录：POST /login
 *
 * <p>防御点：
 * <ol>
 *   <li>参数校验：body 走 {@code @Valid @NotBlank}（JSR-303）；
 *       兼容鸿蒙端的 query 传参，query 场景在 service 里判空</li>
 *   <li>登录失败锁定：同账号连续错 5 次锁 5 分钟（见 UserService.login + Guard）</li>
 *   <li>限流：同 IP 每分钟最多 10 次登录（见 RateLimitInterceptor）</li>
 * </ol>
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoginController {

    private final UserService userService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody(required = false) LoginReq req,
                                        @RequestParam(required = false) String account,
                                        @RequestParam(required = false) String password) {
        // 鸿蒙端/WEB 有的走 body，有的走 query：body 优先，query 兜底
        String acc = req != null ? req.getAccount() : account;
        String pwd = req != null ? req.getPassword() : password;
        if (req == null && (acc == null || pwd == null)) {
            // query 场景没有 @Valid 兜底，手动校验一次
            if (acc == null || acc.trim().isEmpty()) throw new BizException("请输入账号");
            if (pwd == null || pwd.trim().isEmpty()) throw new BizException("请输入密码");
        }
        return R.ok(userService.login(acc, pwd));
    }
}
