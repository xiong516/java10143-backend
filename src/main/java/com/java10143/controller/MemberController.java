package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.UserService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 当前登录用户信息
 *   GET /member/profile   返回 id/account/nickname/avatar/role/admin
 * 端上用它判断「是不是管理员」，从而决定要不要显示管理入口。
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final UserService userService;

    @GetMapping("/member/profile")
    public R<Map<String, Object>> profile() {
        Long userId = UserContext.require();
        return R.ok(userService.profile(userService.getById(userId)));
    }
}
