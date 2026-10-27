package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 商品分类（支持多级：layer 1/2/3） */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 父级分类 id，0 表示一级分类 */
    private Long parentId;

    /** 层级：1 一级，2 二级，3 三级 */
    private Integer layer;

    private String picture;

    private Integer sort;
}
