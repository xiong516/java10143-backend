package com.java10143.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户表 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录用） */
    private String account;

    /** 密码（数据库里存 MD5） */
    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    private String mobile;

    /** 性别：男 / 女 / 保密 */
    private String gender;

    private String birthday;

    private String cityCode;

    private String provinceCode;

    private String profession;

    /** 角色：user 普通用户 / admin 管理员 */
    private String role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
