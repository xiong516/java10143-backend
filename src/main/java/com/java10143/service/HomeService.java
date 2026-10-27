package com.java10143.service;

import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.entity.Banner;
import com.java10143.entity.Category;
import com.java10143.entity.Spu;
import com.java10143.mapper.BannerMapper;
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

/** 首页相关：轮播图 / 新鲜好物 / 分类头部数据 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final BannerMapper bannerMapper;
    private final SpuMapper spuMapper;
    private final CategoryMapper categoryMapper;
    private final com.java10143.mapper.BrandMapper brandMapper;

    /** GET /home/brand?limit=6 —— 热门品牌 */
    public List<Map<String, Object>> hotBrands(String limit) {
        int size = 6;
        try {
            if (limit != null && !StrUtil.isBlank(limit)) size = Integer.parseInt(limit);
        } catch (Exception ignored) {
        }
        List<com.java10143.entity.Brand> list = brandMapper.selectList(
                Wrappers.<com.java10143.entity.Brand>lambdaQuery().last("limit " + size));
        List<Map<String, Object>> res = new ArrayList<>();
        for (com.java10143.entity.Brand b : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("enName", b.getNameEn());
            m.put("picture", ImgUrl.abs(b.getPicture()));
            m.put("logo", ImgUrl.abs(b.getLogo()));
            m.put("desc", b.getDescription());
            res.add(m);
        }
        return res;
    }

    /** GET /home/banner?distributionSite=1 */
    public List<Map<String, Object>> banner(String distributionSite) {
        String site = (distributionSite == null || StrUtil.isBlank(distributionSite)) ? "1" : distributionSite;
        List<Banner> list = bannerMapper.selectList(
                Wrappers.<Banner>lambdaQuery()
                        .eq(Banner::getDistributionSite, site)
                        .orderByAsc(Banner::getSort));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Banner b : list) res.add(Vo.banner(b));
        return res;
    }

    /** GET /home/new?limit=4 —— 新品（按创建时间倒序） */
    public List<Map<String, Object>> newProducts(String limit) {
        int size = 4;
        try {
            if (limit != null && !StrUtil.isBlank(limit)) size = Integer.parseInt(limit);
        } catch (Exception ignored) {
        }
        List<Spu> list = spuMapper.selectList(
                Wrappers.<Spu>lambdaQuery().orderByDesc(Spu::getCreateTime).last("limit " + size));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Spu s : list) res.add(Vo.product(s));
        return res;
    }

    /** GET /home/hot?limit=4 —— 热销榜 */
    public List<Map<String, Object>> hotProducts(String limit) {
        int size = 4;
        try {
            if (limit != null && !StrUtil.isBlank(limit)) size = Integer.parseInt(limit);
        } catch (Exception ignored) {
        }
        List<Spu> list = spuMapper.selectList(
                Wrappers.<Spu>lambdaQuery().orderByDesc(Spu::getSalesCount).last("limit " + size));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Spu s : list) res.add(Vo.product(s));
        return res;
    }

    /** GET /home/category/head —— 分类页左侧一级分类 + 每个一级分类下的二级分类和商品 */
    public List<Map<String, Object>> categoryHead() {
        List<Category> tops = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getLayer, 1)
                        .orderByAsc(Category::getSort));

        List<Map<String, Object>> res = new ArrayList<>();
        for (Category top : tops) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", top.getId());
            item.put("name", top.getName());
            item.put("picture", ImgUrl.abs(top.getPicture()));
            item.put("children", childrenOf(top.getId()));

            List<Spu> goods = spuMapper.selectList(
                    Wrappers.<Spu>lambdaQuery().eq(Spu::getCategoryId, top.getId()).last("limit 4"));
            List<Map<String, Object>> goodsMaps = new ArrayList<>();
            for (Spu s : goods) goodsMaps.add(Vo.product(s));
            item.put("goods", goodsMaps);
            res.add(item);
        }
        return res;
    }

    private List<Map<String, Object>> childrenOf(Long parentId) {
        List<Category> children = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getParentId, parentId)
                        .orderByAsc(Category::getSort));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Category c : children) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("picture", ImgUrl.abs(c.getPicture()));
            m.put("children", null);
            m.put("goods", null);
            res.add(m);
        }
        return res;
    }
}
