package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 商品短视频（抖音式信息流） */
@Data
@TableName("video")
public class Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品 id */
    private Long spuId;

    private String title;

    /** 文案/描述 */
    private String description;

    /** 封面图（相对路径） */
    private String cover;

    /** 视频地址；为空表示前端用商品图合成动态视频 */
    private String url;

    private String authorName;

    private String authorAvatar;

    /** 话题标签，逗号分隔 */
    private String tags;

    private Integer likeCount;

    private Integer commentCount;

    private Integer shareCount;

    private Integer collectCount;

    /** 时长（秒） */
    private Integer duration;

    private Integer sort;

    /** 1 上架 0 下架 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
