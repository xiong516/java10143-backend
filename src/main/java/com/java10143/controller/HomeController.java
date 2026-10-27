package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 首页接口
 *   GET /home/banner?distributionSite=1
 *   GET /home/new?limit=4
 *   GET /home/hot?limit=4
 *   GET /home/category/head
 */
@CrossOrigin(origins = "*") // 这一行是你加的，保留
@RestController // 这一行保留
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final com.java10143.service.RecommendService recommendService;

    /** AI 智能推荐：登录=按订单类目个性化，访客=热销榜兜底（/home/recommend 走可选登录拦截） */
    @GetMapping("/recommend")
    public R<List<Map<String, Object>>> recommend(@RequestParam(required = false) String limit) {
        return R.ok(recommendService.recommend(limit));
    }

    @GetMapping("/banner")
    public R<List<Map<String, Object>>> banner(@RequestParam(required = false) String distributionSite) {
        return R.ok(homeService.banner(distributionSite));
    }

    @GetMapping("/new")
    public R<List<Map<String, Object>>> newGoods(@RequestParam(required = false) String limit) {
        return R.ok(homeService.newProducts(limit));
    }

    @GetMapping("/hot")
    public R<List<Map<String, Object>>> hot(@RequestParam(required = false) String limit) {
        return R.ok(homeService.hotProducts(limit));
    }

    @GetMapping("/category/head")
    public R<List<Map<String, Object>>> categoryHead() {
        return R.ok(homeService.categoryHead());
    }

    /** GET /home/brand?limit=6 —— 热门品牌（对应接口文档首页页面-获取热门品牌） */
    @GetMapping("/brand")
    public R<List<Map<String, Object>>> brand(@RequestParam(required = false) String limit) {
        return R.ok(homeService.hotBrands(limit));
    }
}