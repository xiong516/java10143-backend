package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.LiveService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 直播带货
 *   GET  /live/list                      直播广场（公开，分 直播中/预告/已结束）
 *   GET  /live/{id}                      直播间详情：信息 + 挂车商品 + 历史弹幕
 *   GET  /live/{id}/messages?afterId=    增量拉弹幕
 *   POST /member/live/{id}/join          进场
 *   POST /member/live/{id}/like          点赞
 *   POST /member/live/{id}/message       发弹幕 {content}
 */
@RestController
@RequiredArgsConstructor
public class LiveController {

    private final LiveService liveService;

    @GetMapping("/live/list")
    public R<Map<String, Object>> list() {
        return R.ok(liveService.list());
    }

    @GetMapping("/live/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(liveService.detail(id));
    }

    @GetMapping("/live/{id}/messages")
    public R<List<Map<String, Object>>> messages(@PathVariable Long id,
                                                 @RequestParam(required = false) Long afterId) {
        return R.ok(liveService.messages(id, afterId));
    }

    @PostMapping("/member/live/{id}/join")
    public R<Map<String, Object>> join(@PathVariable Long id) {
        return R.ok(liveService.join(id));
    }

    @PostMapping("/member/live/{id}/like")
    public R<Map<String, Object>> like(@PathVariable Long id) {
        return R.ok(liveService.like(id));
    }

    @PostMapping("/member/live/{id}/message")
    public R<Map<String, Object>> message(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long uid = null;
        try {
            uid = UserContext.get();
        } catch (Exception ignored) {
        }
        Object c = body == null ? null : body.get("content");
        return R.ok(liveService.send(id, uid, "我", c == null ? "" : String.valueOf(c)));
    }
}
