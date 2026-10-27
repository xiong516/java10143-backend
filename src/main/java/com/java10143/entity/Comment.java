package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 评论（商品 / 视频通用） */
@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** goods 商品 / video 视频 */
    private String targetType;

    private Long targetId;

    private Long userId;

    private String nickname;

    private String avatar;

    /** 评分 1~5 */
    private BigDecimal score;

    private String content;

    /** 评论图片，JSON 数组字符串 */
    private String pictures;

    private Integer likeCount;

    private Integer replyCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
