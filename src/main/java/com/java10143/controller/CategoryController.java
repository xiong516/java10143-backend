package com.java10143.controller;

import cn.hutool.core.util.StrUtil;

import com.java10143.common.BizException;
import com.java10143.common.R;
import com.java10143.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 分类接口
 *   GET /category?id=1
 *   GET /category/list
 *
 * 鸿蒙端是把 id 放在 body 里发 GET 请求的，JsonBodyParamFilter 已经帮我们转成了参数
 */
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/category")
    public R<Map<String, Object>> detail(@RequestParam(required = false) String id) {
        if (id == null || StrUtil.isBlank(id)) throw new BizException("缺少分类 id");
        return R.ok(categoryService.detail(Long.valueOf(id)));
    }

    @GetMapping("/category/list")
    public R<List<com.java10143.entity.Category>> list() {
        return R.ok(categoryService.listTop());
    }
}
