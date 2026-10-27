package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.CartService;
import com.java10143.util.Json;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车（需要登录，token 由 AuthInterceptor 校验）
 *   GET    /member/cart
 *   POST   /member/cart              body {skuId, count}
 *   PUT    /member/cart/{skuId}      body {count, selected}
 *   DELETE /member/cart              body {ids:[skuId...]}
 */
@RestController
@RequestMapping("/member/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        return R.ok(cartService.list(UserContext.require()));
    }

    @PostMapping
    public R<Map<String, Object>> add(@RequestParam(required = false) String skuId,
                                      @RequestParam(required = false) Integer count) {
        return R.ok(cartService.add(UserContext.require(), Long.valueOf(skuId), count));
    }

    @PutMapping("/{skuId}")
    public R<Map<String, Object>> update(@PathVariable String skuId,
                                         @RequestParam(required = false) Integer count,
                                         @RequestParam(required = false) Boolean selected) {
        return R.ok(cartService.update(UserContext.require(), Long.valueOf(skuId), count, selected));
    }

    @DeleteMapping
    public R<Boolean> delete(@RequestParam(required = false) String ids) {
        List<String> idList = Json.parseIds(ids);
        return R.ok(cartService.delete(UserContext.require(), idList));
    }
}
