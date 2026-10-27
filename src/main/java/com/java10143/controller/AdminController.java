package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理后台接口（仅 role=admin 可访问，由 AuthInterceptor + AdminInterceptor 双重把关）
 *
 *  看板  GET    /admin/stats
 *  商品  GET    /admin/goods              列表（keyword/page/size）
 *        GET    /admin/goods/{id}         详情
 *        POST   /admin/goods              新增
 *        PUT    /admin/goods/{id}         修改
 *        DELETE /admin/goods/{id}         删除
 *  分类  GET    /admin/categories
 *  视频  GET    /admin/video              列表
 *        POST   /admin/video              新增
 *        PUT    /admin/video/{id}         修改
 *        DELETE /admin/video/{id}         删除
 *  评论  GET    /admin/comment            列表（targetType/keyword）
 *        PUT    /admin/comment/{id}       修改
 *        DELETE /admin/comment/{id}       删除
 *  用户  GET    /admin/user               列表（keyword）
 *        POST   /admin/user               新增
 *        PUT    /admin/user/{id}          修改
 *        DELETE /admin/user/{id}          删除
 *  订单  GET    /admin/order              列表（只读）
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final com.java10143.service.AiService aiService;
    private final com.java10143.service.DashboardService dashboardService;

    /** 数据大屏：GET /admin/dashboard?days=14 —— 销售额趋势 / 类目占比 / 热销 Top10 / 订单状态环形 */
    @GetMapping("/admin/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(required = false) String days) {
        int d = 14;
        try {
            if (days != null && !days.trim().isEmpty()) d = Integer.parseInt(days.trim());
        } catch (NumberFormatException ignore) {
            d = 14;
        }
        return R.ok(dashboardService.overview(d));
    }

    /** AI 商品文案：POST /admin/ai/copy  body {name, category} → 标题/卖点/详情（Python 本地生成） */
    @PostMapping("/admin/ai/copy")
    public R<Map<String, Object>> aiCopy(@RequestBody Map<String, Object> body) {
        String name = body.get("name") == null ? "" : String.valueOf(body.get("name"));
        String category = body.get("category") == null ? "" : String.valueOf(body.get("category"));
        return R.ok(aiService.copy(name, category));
    }

    @GetMapping("/admin/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(adminService.stats());
    }

    // ---------------- 商品
    @GetMapping("/admin/goods")
    public R<Map<String, Object>> goodsList(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String page,
                                            @RequestParam(required = false) String size) {
        return R.ok(adminService.goodsList(keyword, page, size));
    }

    @GetMapping("/admin/goods/{id}")
    public R<Map<String, Object>> goodsDetail(@PathVariable String id) {
        return R.ok(adminService.goodsDetail(Long.valueOf(id)));
    }

    @PostMapping("/admin/goods")
    public R<Map<String, Object>> goodsCreate(@RequestBody Map<String, Object> body) {
        return R.ok(adminService.goodsCreate(body));
    }

    @PutMapping("/admin/goods/{id}")
    public R<Map<String, Object>> goodsUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(adminService.goodsUpdate(Long.valueOf(id), body));
    }

    @DeleteMapping("/admin/goods/{id}")
    public R<Boolean> goodsDelete(@PathVariable String id) {
        return R.ok(adminService.goodsDelete(Long.valueOf(id)));
    }

    @GetMapping("/admin/categories")
    public R<List<Map<String, Object>>> categories() {
        return R.ok(adminService.categories());
    }

    // ---------------- 视频
    @GetMapping("/admin/video")
    public R<List<Map<String, Object>>> videoList() {
        return R.ok(adminService.videoList());
    }

    @PostMapping("/admin/video")
    public R<Map<String, Object>> videoCreate(@RequestBody Map<String, Object> body) {
        return R.ok(adminService.videoCreate(body));
    }

    @PutMapping("/admin/video/{id}")
    public R<Map<String, Object>> videoUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(adminService.videoUpdate(Long.valueOf(id), body));
    }

    @DeleteMapping("/admin/video/{id}")
    public R<Boolean> videoDelete(@PathVariable String id) {
        return R.ok(adminService.videoDelete(Long.valueOf(id)));
    }

    // ---------------- 评论
    @GetMapping("/admin/comment")
    public R<List<Map<String, Object>>> commentList(@RequestParam(required = false) String targetType,
                                                    @RequestParam(required = false) String keyword) {
        return R.ok(adminService.commentList(targetType, keyword));
    }

    @PutMapping("/admin/comment/{id}")
    public R<Map<String, Object>> commentUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(adminService.commentUpdate(Long.valueOf(id), body));
    }

    @DeleteMapping("/admin/comment/{id}")
    public R<Boolean> commentDelete(@PathVariable String id) {
        return R.ok(adminService.commentDelete(Long.valueOf(id)));
    }

    // ---------------- 用户
    @GetMapping("/admin/user")
    public R<List<Map<String, Object>>> userList(@RequestParam(required = false) String keyword) {
        return R.ok(adminService.userList(keyword));
    }

    @PostMapping("/admin/user")
    public R<Map<String, Object>> userCreate(@RequestBody Map<String, Object> body) {
        return R.ok(adminService.userCreate(body));
    }

    @PutMapping("/admin/user/{id}")
    public R<Map<String, Object>> userUpdate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(adminService.userUpdate(Long.valueOf(id), body));
    }

    @DeleteMapping("/admin/user/{id}")
    public R<Boolean> userDelete(@PathVariable String id) {
        return R.ok(adminService.userDelete(Long.valueOf(id)));
    }

    // ---------------- 订单（只读）
    @GetMapping("/admin/order")
    public R<List<Map<String, Object>>> orderList(@RequestParam(required = false) String keyword) {
        return R.ok(adminService.orderList(keyword));
    }
}
