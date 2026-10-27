package com.java10143.controller;

import com.java10143.common.R;
import com.java10143.service.CommentService;
import com.java10143.service.UserService;
import com.java10143.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 评论（商品 / 视频通用）
 *   GET    /comment/list?targetType=goods&targetId=1001    评论列表（公开）
 *   POST   /comment/{id}/like                              评论点赞
 *   GET    /member/comment/mine                            我的评论（登录）
 *   POST   /member/comment                                 发表评论（登录） body {targetType,targetId,score,content,pictures[]}
 *   DELETE /member/comment/{id}                            删除评论（本人或管理员）
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/comment/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String targetType,
                                             @RequestParam(required = false) String targetId,
                                             @RequestParam(required = false) String limit) {
        return R.ok(commentService.list(targetType, targetId, limit));
    }

    @PostMapping("/comment/{id}/like")
    public R<Map<String, Object>> like(@PathVariable String id) {
        return R.ok(commentService.like(Long.valueOf(id)));
    }

    @GetMapping("/member/comment/mine")
    public R<List<Map<String, Object>>> mine() {
        return R.ok(commentService.mine(UserContext.require()));
    }

    @PostMapping("/member/comment")
    public R<Map<String, Object>> add(@RequestBody Map<String, Object> body) {
        return R.ok(commentService.add(UserContext.require(), body));
    }

    @DeleteMapping("/member/comment/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        Long userId = UserContext.require();
        return R.ok(commentService.delete(userId, userService.isAdmin(userId), Long.valueOf(id)));
    }
}
