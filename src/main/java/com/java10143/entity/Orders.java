package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表（表名用 orders，避免和 MySQL 关键字 order 冲突）
 * orderState: 1 待付款 / 2 待发货 / 3 待收货 / 4 待评价 / 5 已完成 / 6 已取消
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer orderState;

    /** 支付状态：0 未支付，1 已支付 */
    private Integer payState;

    /** 1 在线支付，2 货到付款 */
    private Integer payType;

    /** 1 支付宝，2 微信 */
    private Integer payChannel;

    /** 商品总金额 */
    private BigDecimal totalMoney;

    /** 运费 */
    private BigDecimal postFee;

    /** 实付金额 */
    private BigDecimal payMoney;

    /** 商品总件数 */
    private Integer totalNum;

    private String buyerMessage;

    private Integer deliveryTimeType;

    private String receiver;

    private String receiverContact;

    private String provinceCode;

    private String cityCode;

    private String countyCode;

    private String receiverAddress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 支付截止时间（字符串，方便前端直接展示倒计时） */
    private String payLatestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consignTime;

    /** 交易关闭时间（未支付超时 -> 就是它） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
