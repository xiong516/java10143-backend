package com.java10143.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Spu;
import com.java10143.entity.Video;
import com.java10143.mapper.SpuMapper;
import com.java10143.mapper.VideoMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 商品短视频（抖音式信息流） */
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final SpuMapper spuMapper;
    private final com.java10143.mapper.CommentMapper commentMapper;

    /** 前端信息流：GET /home/video?limit=10 */
    public List<Map<String, Object>> feed(String limit) {
        int size = 10;
        try {
            if (limit != null && !StrUtil.isBlank(limit)) size = Integer.parseInt(limit);
        } catch (Exception ignored) {
        }
        if (size <= 0) size = 10;
        if (size > 200) size = 200;   // 上限保护，避免前端传超大值把库拖死
        List<Video> list = videoMapper.selectList(Wrappers.<Video>lambdaQuery()
                .eq(Video::getStatus, 1)
                .orderByAsc(Video::getSort)
                .orderByDesc(Video::getId)
                .last("limit " + size));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Video v : list) {
            Spu goods = v.getSpuId() == null ? null : spuMapper.selectById(v.getSpuId());
            res.add(Vo.video(v, goods));
        }
        return res;
    }

    /** 点赞 +1：POST /home/video/{id}/like */
    public Map<String, Object> like(Long id) {
        return changeCount(id, "like", 1);
    }

    /** 取消点赞 -1：POST /home/video/{id}/like/cancel（不会减到负数） */
    public Map<String, Object> likeCancel(Long id) {
        return changeCount(id, "like", -1);
    }

    /** 收藏 +1：POST /home/video/{id}/collect */
    public Map<String, Object> collect(Long id) {
        return changeCount(id, "collect", 1);
    }

    /** 取消收藏 -1：POST /home/video/{id}/collect/cancel */
    public Map<String, Object> collectCancel(Long id) {
        return changeCount(id, "collect", -1);
    }

    /** 点赞/收藏/分享统一加减入口，delta 负数时最低减到 0 */
    private Map<String, Object> changeCount(Long id, String field, int delta) {
        Video v = videoMapper.selectById(id);
        if (v == null) throw new BizException("视频不存在");
        Integer cur;
        if ("like".equals(field)) cur = v.getLikeCount();
        else if ("collect".equals(field)) cur = v.getCollectCount();
        else cur = v.getShareCount();
        int now = (cur == null ? 0 : cur) + delta;
        if (now < 0) now = 0;
        if ("like".equals(field)) v.setLikeCount(now);
        else if ("collect".equals(field)) v.setCollectCount(now);
        else v.setShareCount(now);
        videoMapper.updateById(v);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", id);
        if ("like".equals(field)) res.put("likeCount", now);
        else if ("collect".equals(field)) res.put("collectCount", now);
        else res.put("shareCount", now);
        return res;
    }

    /** 分享 +1：POST /home/video/{id}/share */
    public Map<String, Object> share(Long id) {
        return changeCount(id, "share", 1);
    }

    /** 管理后台：全部视频（含下架） */
    public List<Map<String, Object>> adminList() {
        List<Video> list = videoMapper.selectList(Wrappers.<Video>lambdaQuery().orderByAsc(Video::getSort));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Video v : list) {
            Spu goods = v.getSpuId() == null ? null : spuMapper.selectById(v.getSpuId());
            res.add(Vo.video(v, goods));
        }
        return res;
    }

    public Video getById(Long id) {
        return videoMapper.selectById(id);
    }

    /** 管理后台：新增视频 */
    public Map<String, Object> create(Map<String, Object> body) {
        Video v = new Video();
        v.setSpuId(longOf(body.get("spuId")));
        v.setTitle(str(body.get("title")));
        v.setDescription(str(body.get("description")));
        v.setCover(str(body.get("cover")));
        v.setUrl(str(body.get("url")));
        v.setAuthorName(str(body.get("authorName")));
        v.setAuthorAvatar(str(body.get("authorAvatar")));
        v.setTags(str(body.get("tags")));
        v.setLikeCount(intOf(body.get("likeCount"), 0));
        v.setShareCount(intOf(body.get("shareCount"), 0));
        v.setCollectCount(intOf(body.get("collectCount"), 0));
        v.setDuration(intOf(body.get("duration"), 15));
        v.setSort(intOf(body.get("sort"), 99));
        v.setStatus(intOf(body.get("status"), 1));
        v.setCommentCount(0);
        if (StrUtil.isBlank(v.getTitle())) throw new BizException("视频标题不能为空");
        videoMapper.insert(v);
        return Vo.video(v, v.getSpuId() == null ? null : spuMapper.selectById(v.getSpuId()));
    }

    /** 管理后台：修改视频 */
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Video v = videoMapper.selectById(id);
        if (v == null) throw new BizException("视频不存在");
        if (body.containsKey("spuId")) v.setSpuId(longOf(body.get("spuId")));
        if (body.containsKey("title")) v.setTitle(str(body.get("title")));
        if (body.containsKey("description")) v.setDescription(str(body.get("description")));
        if (body.containsKey("cover")) v.setCover(str(body.get("cover")));
        if (body.containsKey("url")) v.setUrl(str(body.get("url")));
        if (body.containsKey("authorName")) v.setAuthorName(str(body.get("authorName")));
        if (body.containsKey("authorAvatar")) v.setAuthorAvatar(str(body.get("authorAvatar")));
        if (body.containsKey("tags")) v.setTags(str(body.get("tags")));
        if (body.containsKey("likeCount")) v.setLikeCount(intOf(body.get("likeCount"), 0));
        if (body.containsKey("shareCount")) v.setShareCount(intOf(body.get("shareCount"), 0));
        if (body.containsKey("collectCount")) v.setCollectCount(intOf(body.get("collectCount"), 0));
        if (body.containsKey("duration")) v.setDuration(intOf(body.get("duration"), 15));
        if (body.containsKey("sort")) v.setSort(intOf(body.get("sort"), 99));
        if (body.containsKey("status")) v.setStatus(intOf(body.get("status"), 1));
        videoMapper.updateById(v);
        return Vo.video(v, v.getSpuId() == null ? null : spuMapper.selectById(v.getSpuId()));
    }

    /** 管理后台：删除视频（连带评论） */
    public Boolean delete(Long id) {
        videoMapper.deleteById(id);
        // 删视频时把它的评论一起清掉（避免脏数据）
        try {
            commentMapper.delete(Wrappers.<com.java10143.entity.Comment>lambdaQuery()
                    .eq(com.java10143.entity.Comment::getTargetType, "video")
                    .eq(com.java10143.entity.Comment::getTargetId, id));
        } catch (Exception ignored) {
        }
        return true;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long longOf(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equals(s)) return null;
        return Long.valueOf(s);
    }

    private int intOf(Object o, int def) {
        try {
            return o == null ? def : (int) Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }
}
