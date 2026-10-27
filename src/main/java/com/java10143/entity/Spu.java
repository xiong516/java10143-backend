package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品 SPU */
@Data
@TableName("spu")
public class Spu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String spuCode;

    private Long categoryId;

    private Long brandId;

    /** 商品描述 */
    private String description;

    /** 现价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal oldPrice;

    /** 总库存 */
    private Integer inventory;

    /** 销量 */
    private Integer salesCount;

    /** 评论数 */
    private Integer commentCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 折扣（为空表示无折扣） */
    private BigDecimal discount;

    /** 是否预售 0/1 */
    private Integer isPreSale;

    /** 视频比例 1=1:1/16:9  2=3:4 */
    private Integer videoScale;

    /** 主图集合，JSON 数组字符串 ["/img/p/a.svg"] */
    private String mainPictures;

    /** 主图视频，JSON 数组字符串 */
    private String mainVideos;

    /** 详情图集合，JSON 数组字符串 */
    private String detailPictures;

    /** 商品属性，JSON 数组字符串 [{"name":"品牌","value":"xx"}] */
    private String detailProperties;

    /** 规格集合，JSON 数组字符串 [{"id":1,"name":"颜色","values":[{"name":"白","desc":"","picture":null}]}] */
    private String specs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
