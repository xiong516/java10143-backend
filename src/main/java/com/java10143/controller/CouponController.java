package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.CouponService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 优惠券：
 *   GET  /coupon/list               领券中心（未登录也能看，登录后带「我的」信息）
 *   POST /member/coupon/{id}/take   领取
 *   GET  /member/coupon?status=0    我的券（0未使用 1已用 2过期，不传查全部）
 *   GET  /member/coupon/preview     结算页试算：能减多少、有哪些券可选
 */
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 领券中心：公开接口，登录后带我的领券状态 */
    @GetMapping("/coupon/list")
    public R<Map<String, Object>> list() {
        Long uid = null;
        try {
            uid = UserContext.get();
        } catch (Exception ignore) {
            // 未登录 / token 失效都当游客处理
        }
        return R.ok(couponService.center(uid));
    }

    @PostMapping("/member/coupon/{id}/take")
    public R<Map<String, Object>> take(@PathVariable Long id) {
        return R.ok(couponService.take(UserContext.require(), id));
    }

    /** 兼容 GET，方便直接点链接领券（仍是登录接口） */
    @GetMapping("/member/coupon/{id}/take")
    public R<Map<String, Object>> takeGet(@PathVariable Long id) {
        return R.ok(couponService.take(UserContext.require(), id));
    }

    @GetMapping("/member/coupon")
    public R<List<Map<String, Object>>> mine(@RequestParam(required = false) Integer status) {
        return R.ok(couponService.mine(UserContext.require(), status));
    }

    /**
     * 结算页试算：/member/coupon/preview?amount=6999&spuIds=1001,1004
     * 返回最优券与全部可用券
     */
    @GetMapping("/member/coupon/preview")
    public R<Map<String, Object>> preview(@RequestParam(required = false) String amount,
                                          @RequestParam(required = false) String spuIds) {
        Long uid = UserContext.require();
        java.math.BigDecimal amt = java.math.BigDecimal.ZERO;
        try {
            if (amount != null && !amount.trim().isEmpty()) amt = new java.math.BigDecimal(amount.trim());
        } catch (Exception ignore) {
            amt = java.math.BigDecimal.ZERO;
        }
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (String s : com.java10143.util.Json.parseIds(spuIds)) {
            try {
                ids.add(Long.valueOf(s.trim()));
            } catch (Exception ignore) {
                // 忽略非法 id
            }
        }
        return R.ok(couponService.bestFor(uid, ids, amt));
    }
}
