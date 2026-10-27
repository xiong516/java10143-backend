package com.java10143.common;

/**
 * 业务异常：抛出后由 GlobalExceptionHandler 统一转成 { code:"0", msg:"xxx" }
 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String msg) {
        super(msg);
        this.code = "0";
    }

    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
