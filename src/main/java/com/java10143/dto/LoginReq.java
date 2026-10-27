package com.java10143.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求体（JSR-303 参数校验示例）。
 *
 * <p>Controller 里用 {@code @Valid @RequestBody LoginReq} 接收，
 * 校验不通过会抛 MethodArgumentNotValidException，
 * 由 GlobalExceptionHandler 统一转成友好提示。
 *
 * <p>为什么 message 用中文写死：这是直接展示给用户看的文案。
 */
@Data
public class LoginReq {

    /** 账号：用户名或手机号 */
    @NotBlank(message = "请输入账号")
    @Size(max = 20, message = "账号长度不能超过 20 位")
    private String account;

    /** 密码 */
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String password;
}
