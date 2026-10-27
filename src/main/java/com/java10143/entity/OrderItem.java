package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 订单商品明细 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long spuId;

    private Long skuId;

    private String name;

    private String image;

    /** 规格文字，如“颜色：白 尺码：40” */
    private String attrsText;

    /** 成交单价 */
    private BigDecimal curPrice;

    private Integer quantity;

    /** 该行实付小计 */
    private BigDecimal realPay;
}
