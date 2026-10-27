package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 商品 SKU（具体规格：颜色/尺码 …） */
@Data
@TableName("sku")
public class Sku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spuId;

    private String skuCode;

    private BigDecimal price;

    private BigDecimal oldPrice;

    private Integer inventory;

    /** 规格，JSON 数组字符串 [{"name":"颜色","valueName":"白"}] */
    private String specs;
}
