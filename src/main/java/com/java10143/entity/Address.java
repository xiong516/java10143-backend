package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 收货地址 */
@Data
@TableName("address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 收货人姓名 */
    private String receiver;

    /** 联系方式 */
    private String contact;

    private String provinceCode;

    private String cityCode;

    private String countyCode;

    /** 详细地址 */
    private String address;

    /** 完整地址（省市区 + 详细） */
    private String fullLocation;

    private String postalCode;

    /** 标签，多个用英文逗号分隔 */
    private String addressTags;

    /** 0 = 默认地址，1 = 非默认 */
    private Integer isDefault;
}
