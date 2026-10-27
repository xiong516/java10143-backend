package com.java10143.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Category;
import com.java10143.entity.Spu;
import com.java10143.mapper.CategoryMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.util.ImgUrl;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 分类相关 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final SpuMapper spuMapper;

    /** 一级分类列表 */
    public List<Category> listTop() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getLayer, 1).orderByAsc(Category::getSort));
    }

    /**
     * GET /category?id=1
     * 返回：一级分类 + 它的二级分类（二级分类下面挂商品）
     */
    public Map<String, Object> detail(Long id) {
        Category top = categoryMapper.selectById(id);
        if (top == null) throw new BizException("分类不存在");

        List<Category> children = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, id).orderByAsc(Category::getSort));

        List<Map<String, Object>> childList = new ArrayList<>();
        for (Category c : children) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("picture", ImgUrl.abs(c.getPicture()));
            m.put("parentId", c.getParentId());
            m.put("parentName", top.getName());
            m.put("brands", null);
            m.put("categories", null);
            m.put("saleProperties", null);

            List<Spu> goods = spuMapper.selectList(Wrappers.<Spu>lambdaQuery()
                    .eq(Spu::getCategoryId, c.getId()).orderByDesc(Spu::getSalesCount).last("limit 6"));
            List<Map<String, Object>> goodsMaps = new ArrayList<>();
            for (Spu s : goods) goodsMaps.add(Vo.product(s));
            m.put("goods", goodsMaps);
            childList.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", top.getId());
        res.put("name", top.getName());
        res.put("picture", ImgUrl.abs(top.getPicture()));
        res.put("children", childList);
        return res;
    }
}
