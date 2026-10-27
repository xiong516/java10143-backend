package com.java10143.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理。
 *
 * <p>【防御原则】给前端的永远是"人话 + 不泄露内部信息"，
 * 真实异常堆栈只写服务端日志。否则 SQL 片段、文件路径、类名
 * 这类信息会被攻击者拿去做下一步探测（信息泄露是入侵的第一步）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：文案是代码里精心写的，可以原样给用户 */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数格式错误（比如 id 传了字母） */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public R<Void> handleBadRequest(Exception e) {
        log.warn("参数异常：{}", e.getMessage());
        return R.fail("0", "请求参数格式不对，请检查后重试");
    }

    /** JSR-303 校验失败（@Valid / @NotBlank 等）：把第一条中文提示返回给用户 */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public R<Void> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .orElse("请求参数不完整");
        log.warn("参数校验失败：{}", msg);
        return R.fail("0", msg);
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持：{}", e.getMessage());
        return R.fail("0", "该接口不支持这种请求方式");
    }

    /** 其它所有异常：日志留全量，返回统一话术（★ 不再拼接 e.getMessage()） */
    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return R.fail("0", "服务器开小差了，请稍后再试");
    }
}
