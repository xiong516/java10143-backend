package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 首页轮播图 */
@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String imgUrl;

    private String hrefUrl;

    /** 跳转类型：1 页面 2 H5 3 小程序 */
    private String type;

    /** 投放位置：1 首页 2 分类页 */
    private String distributionSite;

    private Integer sort;
}
