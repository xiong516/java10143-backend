package com.java10143.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.CartItem;
import com.java10143.entity.Sku;
import com.java10143.entity.Spu;
import com.java10143.mapper.CartItemMapper;
import com.java10143.mapper.SkuMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 购物车 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;

    /** GET /member/cart */
    public List<Map<String, Object>> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getCreateTime));
        List<Map<String, Object>> res = new ArrayList<>();
        for (CartItem it : items) {
            Map<String, Object> vo = toVo(it);
            if (vo != null) res.add(vo);
        }
        return res;
    }

    /** POST /member/cart  body: {skuId, count} —— 已有就累加数量 */
    public Map<String, Object> add(Long userId, Long skuId, Integer count) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) throw new BizException("商品规格不存在，skuId=" + skuId);
        int num = (count == null || count < 1) ? 1 : count;

        CartItem exist = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId).eq(CartItem::getSkuId, skuId).last("limit 1"));
        if (exist == null) {
            exist = new CartItem();
            exist.setUserId(userId);
            exist.setSkuId(skuId);
            exist.setCount(num);
            exist.setSelected(1);
            cartItemMapper.insert(exist);
        } else {
            exist.setCount(exist.getCount() + num);
            cartItemMapper.updateById(exist);
        }
        return toVo(exist);
    }

    /** PUT /member/cart/{skuId}  body: {count, selected} */
    public Map<String, Object> update(Long userId, Long skuId, Integer count, Boolean selected) {
        CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId).eq(CartItem::getSkuId, skuId).last("limit 1"));
        if (item == null) throw new BizException("购物车里没有这个商品");
        if (count != null && count > 0) item.setCount(count);
        if (selected != null) item.setSelected(selected ? 1 : 0);
        cartItemMapper.updateById(item);
        return toVo(item);
    }

    /** DELETE /member/cart  body: {ids:[skuId...]} */
    public boolean delete(Long userId, List<String> ids) {
        if (ids == null || ids.isEmpty()) return false;
        for (String id : ids) {
            cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId).eq(CartItem::getSkuId, Long.valueOf(id)));
        }
        return true;
    }

    /** 结算时用：根据 skuId 集合取出购物车里的商品（取不到就按数量 1 处理） */
    public List<Map<String, Object>> findForOrder(Long userId, List<String> skuIds) {
        List<Map<String, Object>> res = new ArrayList<>();
        for (String idStr : skuIds) {
            Long skuId;
            try {
                skuId = Long.valueOf(idStr);
            } catch (Exception e) {
                continue;
            }
            Sku sku = skuMapper.selectById(skuId);
            if (sku == null) continue;
            Spu spu = spuMapper.selectById(sku.getSpuId());
            if (spu == null) continue;

            CartItem item = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId).eq(CartItem::getSkuId, skuId).last("limit 1"));
            Map<String, Object> vo = Vo.cartItem(sku, spu,
                    item == null ? 1 : item.getCount(),
                    item == null || item.getSelected() == 1);
            res.add(vo);
        }
        return res;
    }

    /** 结算完成后清掉购物车里这些商品 */
    public void removeBySkuIds(Long userId, List<Long> skuIds) {
        for (Long skuId : skuIds) {
            cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId).eq(CartItem::getSkuId, skuId));
        }
    }

    private Map<String, Object> toVo(CartItem item) {
        Sku sku = skuMapper.selectById(item.getSkuId());
        if (sku == null) return null;
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null) return null;
        Map<String, Object> vo = new LinkedHashMap<>(Vo.cartItem(sku, spu, item.getCount(), item.getSelected() == 1));
        return vo;
    }
}
