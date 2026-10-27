package com.java10143.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一返回结构，和鸿蒙端 model 里定义的 { code, msg, result } 完全对应。
 * code = "1" 表示成功，其它表示失败（鸿蒙端就是这么判断的）
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class R<T> {

    /** 业务状态码：1 成功，0 失败 */
    private String code;

    /** 提示信息 */
    private String msg;

    /** 业务数据 */
    private T result;

    /** 兼容部分页面读 data 字段的写法（和 result 内容一致） */
    private T data;

    public R() {
    }

    public R(String code, String msg, T result) {
        this.code = code;
        this.msg = msg;
        this.result = result;
    }

    public static <T> R<T> ok() {
        return new R<>("1", "操作成功", null);
    }

    public static <T> R<T> ok(T result) {
        return new R<>("1", "操作成功", result);
    }

    /**
     * result 和 data 同时写入，兼容两套前端解析方式（支付、订单详情页会用到）
     */
    public static <T> R<T> okBoth(T result) {
        R<T> r = new R<>("1", "操作成功", result);
        r.setData(result);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return new R<>("0", msg, null);
    }

    public static <T> R<T> fail(String code, String msg) {
        return new R<>(code, msg, null);
    }
}
