package com.java10143.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Address;
import com.java10143.entity.Brand;
import com.java10143.entity.Category;
import com.java10143.entity.Evaluation;
import com.java10143.entity.Sku;
import com.java10143.entity.Spu;
import com.java10143.entity.Video;
import com.java10143.mapper.AddressMapper;
import com.java10143.mapper.BrandMapper;
import com.java10143.mapper.CategoryMapper;
import com.java10143.mapper.EvaluationMapper;
import com.java10143.mapper.SkuMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.mapper.VideoMapper;
import com.java10143.util.ImgUrl;
import com.java10143.util.Json;
import com.java10143.util.UserContext;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 商品详情：GET /goods?id=xxx */
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final BrandMapper brandMapper;
    private final CategoryMapper categoryMapper;
    private final EvaluationMapper evaluationMapper;
    private final AddressMapper addressMapper;
    private final VideoMapper videoMapper;

    public Map<String, Object> detail(Long id) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) throw new BizException("商品不存在");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", spu.getId());
        m.put("name", spu.getName());
        m.put("spuCode", spu.getSpuCode());
        m.put("desc", spu.getDescription());
        m.put("price", Vo.money(spu.getPrice()));
        m.put("oldPrice", Vo.money(spu.getOldPrice()));
        m.put("discount", spu.getDiscount() == null ? null : spu.getDiscount().doubleValue());
        m.put("inventory", spu.getInventory());
        m.put("salesCount", spu.getSalesCount());
        m.put("commentCount", spu.getCommentCount());
        m.put("collectCount", spu.getCollectCount());
        m.put("isCollect", null);
        m.put("isPreSale", spu.getIsPreSale() != null && spu.getIsPreSale() == 1);
        m.put("videoScale", spu.getVideoScale());
        m.put("mainVideos", Json.strList(spu.getMainVideos()));
        m.put("mainPictures", ImgUrl.abs(Json.strList(spu.getMainPictures())));
        m.put("recommends", null);

        // 商品对应的 AI 短视频（详情页「看视频」入口；没有就返回 null）
        Video gv = videoMapper.selectOne(Wrappers.<Video>lambdaQuery()
                .eq(Video::getSpuId, spu.getId())
                .eq(Video::getStatus, 1)
                .orderByAsc(Video::getSort)
                .last("limit 1"));
        m.put("video", gv == null ? null : Vo.video(gv, null));

        // 品牌
        Brand brand = spu.getBrandId() == null ? null : brandMapper.selectById(spu.getBrandId());
        Map<String, Object> brandMap = new LinkedHashMap<>();
        if (brand != null) {
            brandMap.put("id", brand.getId());
            brandMap.put("name", brand.getName());
            brandMap.put("nameEn", brand.getNameEn());
            brandMap.put("logo", ImgUrl.abs(brand.getLogo()));
            brandMap.put("picture", ImgUrl.abs(brand.getPicture()));
            brandMap.put("desc", brand.getDescription());
        }
        brandMap.putIfAbsent("place", null);
        brandMap.putIfAbsent("type", null);
        m.put("brand", brandMap.isEmpty() ? null : brandMap);

        // 分类链
        m.put("categories", categoryChain(spu.getCategoryId()));

        // 详情图文
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("pictures", ImgUrl.abs(Json.strList(spu.getDetailPictures())));
        details.put("properties", Json.mapList(spu.getDetailProperties()));
        m.put("details", details);

        // 规格 / SKU
        m.put("specs", Json.mapList(spu.getSpecs()));
        List<Sku> skus = skuMapper.selectList(
                Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, spu.getId()));
        List<Map<String, Object>> skuMaps = new ArrayList<>();
        for (Sku s : skus) skuMaps.add(Vo.sku(s));
        m.put("skus", skuMaps);

        // 评价
        Evaluation ev = evaluationMapper.selectOne(
                Wrappers.<Evaluation>lambdaQuery().eq(Evaluation::getSpuId, spu.getId()).last("limit 1"));
        if (ev != null) {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("id", ev.getId());
            em.put("content", ev.getContent());
            em.put("createTime", ev.getCreateTime() == null ? "" : ev.getCreateTime().toString().replace("T", " "));
            em.put("praiseCount", ev.getPraiseCount());
            em.put("praisePercent", ev.getPraisePercent());
            em.put("score", ev.getScore());
            em.put("tags", null);
            em.put("pictures", null);
            em.put("member", null);
            em.put("officialReply", null);
            em.put("orderInfo", null);
            m.put("evaluationInfo", em);
        } else {
            m.put("evaluationInfo", null);
        }

        // 24 小时热销 / 同类推荐（用数据库里现成的商品来模拟）
        m.put("hotByDay", products(spuMapper.selectList(Wrappers.<Spu>lambdaQuery()
                .orderByDesc(Spu::getSalesCount).last("limit 6"))));
        m.put("similarProducts", products(similarOf(spu)));

        // 登录了才返回地址列表（和原接口行为一致）
        Long userId = UserContext.get();
        if (userId != null) {
            List<Address> addresses = addressMapper.selectList(Wrappers.<Address>lambdaQuery()
                    .eq(Address::getUserId, userId).orderByAsc(Address::getIsDefault));
            List<Map<String, Object>> am = new ArrayList<>();
            for (Address a : addresses) am.add(Vo.address(a));
            m.put("userAddresses", am);
        } else {
            m.put("userAddresses", null);
        }
        return m;
    }

    /** 同类推荐：同一个一级分类下的其它商品 */
    private List<Spu> similarOf(Spu spu) {
        Category cur = spu.getCategoryId() == null ? null : categoryMapper.selectById(spu.getCategoryId());
        Long parentId = (cur == null || cur.getParentId() == null) ? spu.getCategoryId() : cur.getParentId();
        List<Category> siblings = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, parentId));
        List<Long> ids = new ArrayList<>();
        if (cur != null) ids.add(cur.getId());
        for (Category c : siblings) ids.add(c.getId());
        if (ids.isEmpty()) return new ArrayList<>();
        return spuMapper.selectList(Wrappers.<Spu>lambdaQuery()
                .in(Spu::getCategoryId, ids)
                .ne(Spu::getId, spu.getId())
                .orderByDesc(Spu::getSalesCount)
                .last("limit 6"));
    }

    private List<Map<String, Object>> products(List<Spu> list) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (Spu s : list) res.add(Vo.product(s));
        return res;
    }

    /** 一级 -> 二级 -> 三级 分类链 */
    private List<Map<String, Object>> categoryChain(Long categoryId) {
        List<Map<String, Object>> res = new ArrayList<>();
        Category cur = categoryId == null ? null : categoryMapper.selectById(categoryId);
        while (cur != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cur.getId());
            m.put("name", cur.getName());
            m.put("layer", cur.getLayer());
            m.put("parent", null);
            res.add(0, m);
            cur = cur.getParentId() == null ? null : categoryMapper.selectById(cur.getParentId());
        }
        // 填充 parent 引用
        for (int i = 0; i < res.size(); i++) {
            if (i > 0) res.get(i).put("parent", res.get(i - 1));
        }
        return res;
    }

    /** 关键词搜索：名称/描述模糊匹配，最多 30 条 */
    public List<Map<String, Object>> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BizException("请输入搜索关键词");
        }
        String kw = keyword.trim();
        List<Spu> list = spuMapper.selectList(Wrappers.<Spu>lambdaQuery()
                .and(w -> w.like(Spu::getName, kw).or().like(Spu::getDescription, kw))
                .orderByDesc(Spu::getSalesCount)
                .last("limit 30"));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Spu s : list) {
            res.add(Vo.product(s));
        }
        return res;
    }
}
