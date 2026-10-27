package com.java10143.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Comment;
import com.java10143.entity.User;
import com.java10143.entity.Video;
import com.java10143.mapper.CommentMapper;
import com.java10143.mapper.UserMapper;
import com.java10143.mapper.VideoMapper;
import com.java10143.util.Json;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 评论：商品和视频通用，支持 查看 / 发表 / 删除 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;

    /** 评论列表：GET /comment/list?targetType=goods&targetId=1001 */
    public List<Map<String, Object>> list(String targetType, String targetId, String limit) {
        int size = 50;
        try {
            if (limit != null && !StrUtil.isBlank(limit)) size = Integer.parseInt(limit);
        } catch (Exception ignored) {
        }
        String type = normalizeType(targetType);
        List<Comment> list = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getTargetType, type)
                .eq(Comment::getTargetId, Long.valueOf(targetId))
                .orderByDesc(Comment::getId)
                .last("limit " + size));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Comment c : list) res.add(Vo.comment(c));
        return res;
    }

    /** 我的评论：GET /member/comment/mine */
    public List<Map<String, Object>> mine(Long userId) {
        List<Comment> list = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getId)
                .last("limit 100"));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Comment c : list) res.add(Vo.comment(c));
        return res;
    }

    /** 发表评论：POST /member/comment */
    @Transactional
    public Map<String, Object> add(Long userId, Map<String, Object> body) {
        String type = normalizeType(str(body.get("targetType")));
        String targetId = str(body.get("targetId"));
        String content = str(body.get("content"));
        if (StrUtil.isBlank(targetId)) throw new BizException("缺少评论对象 id");
        if (StrUtil.isBlank(content)) throw new BizException("评论内容不能为空");
        if (content.length() > 500) content = content.substring(0, 500);

        User u = userMapper.selectById(userId);
        Comment c = new Comment();
        c.setTargetType(type);
        c.setTargetId(Long.valueOf(targetId));
        c.setUserId(userId);
        c.setNickname(u == null || StrUtil.isBlank(u.getNickname()) ? "京东用户" : u.getNickname());
        c.setAvatar(u == null ? null : u.getAvatar());
        c.setScore(decimalOf(body.get("score")));
        c.setContent(content);
        c.setPictures(picturesJson(body.get("pictures")));
        c.setLikeCount(0);
        c.setReplyCount(0);
        commentMapper.insert(c);

        refreshVideoCommentCount(type, c.getTargetId());
        return Vo.comment(commentMapper.selectById(c.getId()));
    }

    /** 删除评论：本人可删自己的，管理员可删任意 */
    @Transactional
    public Boolean delete(Long userId, boolean admin, Long commentId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) throw new BizException("评论不存在");
        if (!admin && (c.getUserId() == null || !c.getUserId().equals(userId))) {
            throw new BizException("只能删除自己发的评论");
        }
        commentMapper.deleteById(commentId);
        refreshVideoCommentCount(c.getTargetType(), c.getTargetId());
        return true;
    }

    /** 评论点赞：POST /comment/{id}/like */
    public Map<String, Object> like(Long commentId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) throw new BizException("评论不存在");
        int now = (c.getLikeCount() == null ? 0 : c.getLikeCount()) + 1;
        c.setLikeCount(now);
        commentMapper.updateById(c);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", commentId);
        res.put("likeCount", now);
        return res;
    }

    // ------------------------------------------------ 管理后台

    /** 后台评论列表：可按类型/关键词过滤 */
    public List<Map<String, Object>> adminList(String targetType, String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Comment> q =
                Wrappers.<Comment>lambdaQuery().orderByDesc(Comment::getId).last("limit 200");
        if (!StrUtil.isBlank(targetType)) q.eq(Comment::getTargetType, targetType);
        if (!StrUtil.isBlank(keyword)) q.like(Comment::getContent, keyword);
        List<Comment> list = commentMapper.selectList(q);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Comment c : list) res.add(Vo.comment(c));
        return res;
    }

    /** 后台改评论（内容/评分） */
    public Map<String, Object> adminUpdate(Long id, Map<String, Object> body) {
        Comment c = commentMapper.selectById(id);
        if (c == null) throw new BizException("评论不存在");
        if (body.containsKey("content")) c.setContent(str(body.get("content")));
        if (body.containsKey("score")) c.setScore(decimalOf(body.get("score")));
        commentMapper.updateById(c);
        return Vo.comment(c);
    }

    /** 后台删评论（管理员，等价于 delete(userId, true, id)） */
    @Transactional
    public Boolean adminDelete(Long id) {
        Comment c = commentMapper.selectById(id);
        if (c == null) return true;
        commentMapper.deleteById(id);
        refreshVideoCommentCount(c.getTargetType(), c.getTargetId());
        return true;
    }

    // ------------------------------------------------ 内部

    private void refreshVideoCommentCount(String type, Long targetId) {
        if (!"video".equals(type) || targetId == null) return;
        Video v = videoMapper.selectById(targetId);
        if (v == null) return;
        Long cnt = commentMapper.selectCount(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getTargetType, "video")
                .eq(Comment::getTargetId, targetId));
        v.setCommentCount(cnt == null ? 0 : cnt.intValue());
        videoMapper.updateById(v);
    }

    private String normalizeType(String type) {
        return "video".equalsIgnoreCase(type) ? "video" : "goods";
    }

    private String picturesJson(Object o) {
        if (o == null) return null;
        List<String> pics = Json.strList(Json.to(o));
        if (pics == null || pics.isEmpty()) return null;
        return Json.to(pics);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private BigDecimal decimalOf(Object o) {
        if (o == null) return new BigDecimal("5.0");
        try {
            BigDecimal d = new BigDecimal(String.valueOf(o));
            if (d.compareTo(BigDecimal.ZERO) < 0) d = BigDecimal.ZERO;
            if (d.compareTo(new BigDecimal("5")) > 0) d = new BigDecimal("5");
            return d;
        } catch (Exception e) {
            return new BigDecimal("5.0");
        }
    }
}
