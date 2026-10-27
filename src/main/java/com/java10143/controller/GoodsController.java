package com.java10143.controller;

import cn.hutool.core.util.StrUtil;

import com.java10143.common.BizException;
import com.java10143.common.R;
import com.java10143.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 商品：GET /goods?id=xxx  搜索：GET /goods/search?keyword=xxx */
@RestController
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @GetMapping("/goods")
    public R<Map<String, Object>> detail(@RequestParam(required = false) String id) {
        if (id == null || StrUtil.isBlank(id)) throw new BizException("缺少商品 id");
        return R.ok(goodsService.detail(Long.valueOf(id)));
    }

    /** 关键词搜索（名称/描述模糊匹配，最多 30 条） */
    @GetMapping("/goods/search")
    public R<java.util.List<Map<String, Object>>> search(@RequestParam(required = false) String keyword) {
        return R.ok(goodsService.search(keyword));
    }
}
