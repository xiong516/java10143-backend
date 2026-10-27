package com.java10143.vo;

import cn.hutool.core.util.StrUtil;

import com.java10143.entity.Address;
import com.java10143.entity.Banner;
import com.java10143.entity.Category;
import com.java10143.entity.OrderItem;
import com.java10143.entity.Orders;
import com.java10143.entity.Sku;
import com.java10143.entity.Spu;
import com.java10143.util.ImgUrl;
import com.java10143.util.Json;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把数据库实体转成「和鸿蒙端 model 完全一致」的 JSON 结构。
 * 用 LinkedHashMap 是为了保证字段顺序，方便肉眼核对接口。
 */
public class Vo {

    public static String money(BigDecimal b) {
        return b == null ? "0.00" : b.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 商品主图 */
    public static String firstPicture(Spu spu) {
        List<String> pics = Json.strList(spu.getMainPictures());
        return pics.isEmpty() ? "" : ImgUrl.abs(pics.get(0));
    }

    /** 商品卡片：/home/new、分类页商品列表都用它 */
    public static Map<String, Object> product(Spu spu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", spu.getId());
        m.put("name", spu.getName());
        m.put("desc", spu.getDescription());
        m.put("discount", spu.getDiscount());
        m.put("orderNum", spu.getSalesCount() == null ? 0 : spu.getSalesCount());
        m.put("picture", firstPicture(spu));
        m.put("price", money(spu.getPrice()));
        m.put("oldPrice", money(spu.getOldPrice()));
        m.put("spuCode", spu.getSpuCode());
        m.put("salesCount", spu.getSalesCount() == null ? 0 : spu.getSalesCount());
        m.put("commentCount", spu.getCommentCount() == null ? 0 : spu.getCommentCount());
        return m;
    }

    /** 轮播图 */
    public static Map<String, Object> banner(Banner b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("imgUrl", ImgUrl.abs(b.getImgUrl()));
        m.put("hrefUrl", b.getHrefUrl());
        m.put("type", b.getType());
        return m;
    }

    /** 分类（简版） */
    public static Map<String, Object> category(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("picture", ImgUrl.abs(c.getPicture()));
        m.put("layer", c.getLayer());
        m.put("parentId", c.getParentId());
        m.put("children", null);
        m.put("goods", null);
        return m;
    }

    /** SKU -> 鸿蒙端 Skus 结构 */
    public static Map<String, Object> sku(Sku sku) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sku.getId());
        m.put("spuId", sku.getSpuId());
        m.put("skuCode", sku.getSkuCode());
        m.put("price", money(sku.getPrice()));
        m.put("oldPrice", money(sku.getOldPrice()));
        m.put("inventory", sku.getInventory() == null ? 0 : sku.getInventory());
        m.put("specs", Json.mapList(sku.getSpecs()));
        return m;
    }

    /** 规格名拼接成“颜色：白 尺码：40” */
    public static String attrsText(String specsJson) {
        List<Map<String, Object>> specs = Json.mapList(specsJson);
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> s : specs) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(s.get("name")).append("：").append(s.get("valueName"));
        }
        return sb.toString();
    }

    /** 购物车条目（App 端 ShopCarResult 结构） */
    public static Map<String, Object> cartItem(Sku sku, Spu spu, int count, boolean selected) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sku.getId());
        m.put("skuId", sku.getId());
        m.put("spuId", spu.getId());
        m.put("name", spu.getName());
        m.put("picture", firstPicture(spu));
        m.put("attrsText", attrsText(sku.getSpecs()));
        m.put("specs", specNames(sku.getSpecs()));
        m.put("price", money(sku.getPrice()));
        m.put("nowPrice", money(sku.getPrice()));
        m.put("nowOriginalPrice", money(sku.getOldPrice()));
        m.put("count", count);
        m.put("selected", selected);
        m.put("stock", sku.getInventory() == null ? 0 : sku.getInventory());
        m.put("isEffective", true);
        m.put("isCollect", false);
        m.put("discount", null);
        m.put("postFee", 0);
        return m;
    }

    private static List<String> specNames(String specsJson) {
        List<String> res = new ArrayList<>();
        for (Map<String, Object> s : Json.mapList(specsJson)) {
            res.add(String.valueOf(s.get("valueName")));
        }
        return res;
    }

    /** 收货地址 */
    public static Map<String, Object> address(Address a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("receiver", a.getReceiver());
        m.put("contact", a.getContact());
        m.put("provinceCode", a.getProvinceCode());
        m.put("cityCode", a.getCityCode());
        m.put("countyCode", a.getCountyCode());
        m.put("address", a.getAddress());
        m.put("fullLocation", a.getFullLocation());
        m.put("postalCode", a.getPostalCode());
        m.put("addressTags", a.getAddressTags());
        m.put("isDefault", a.getIsDefault() == null ? 1 : a.getIsDefault());
        return m;
    }

    /** 订单商品行（结算页 goods / 订单详情 skus 通用） */
    public static Map<String, Object> orderItem(OrderItem it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", it.getSpuId());
        m.put("spuId", it.getSpuId());
        m.put("skuId", it.getSkuId());
        m.put("name", it.getName());
        m.put("picture", ImgUrl.normalize(it.getImage()));
        m.put("image", ImgUrl.normalize(it.getImage()));
        m.put("attrsText", it.getAttrsText());
        m.put("count", it.getQuantity());
        m.put("quantity", it.getQuantity());
        m.put("price", money(it.getCurPrice()));
        m.put("curPrice", it.getCurPrice());
        m.put("payPrice", money(it.getRealPay()));
        m.put("realPay", it.getRealPay());
        m.put("totalPrice", money(it.getCurPrice().multiply(BigDecimal.valueOf(it.getQuantity()))));
        m.put("totalPayPrice", money(it.getRealPay()));
        m.put("properties", attrsProperties(it.getAttrsText()));
        m.put("totalMoney", null);
        return m;
    }

    /** “颜色：白 尺码：40” -> [{propertyMainName:颜色,propertyValueName:白}] */
    private static List<Map<String, Object>> attrsProperties(String attrsText) {
        List<Map<String, Object>> res = new ArrayList<>();
        if (attrsText == null || StrUtil.isBlank(attrsText)) return res;
        for (String part : attrsText.split("\\s+")) {
            String[] kv = part.split("：");
            if (kv.length == 2) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("propertyMainName", kv[0]);
                m.put("propertyValueName", kv[1]);
                res.add(m);
            }
        }
        return res;
    }

    /** 订单详情（鸿蒙端 HUO.ets / PageMyJd.ets 用的结构） */
    public static Map<String, Object> orderDetail(Orders o, List<OrderItem> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderState", o.getOrderState());
        m.put("payState", o.getPayState());
        m.put("payType", o.getPayType());
        m.put("payChannel", o.getPayChannel());
        m.put("totalMoney", o.getTotalMoney());
        m.put("postFee", o.getPostFee());
        m.put("payMoney", o.getPayMoney());
        m.put("totalNum", o.getTotalNum());
        m.put("buyerMessage", o.getBuyerMessage());
        m.put("deliveryTimeType", o.getDeliveryTimeType());
        m.put("receiverContact", o.getReceiver());
        m.put("receiverMobile", o.getReceiverContact());
        m.put("provinceCode", o.getProvinceCode());
        m.put("cityCode", o.getCityCode());
        m.put("countyCode", o.getCountyCode());
        m.put("receiverAddress", o.getReceiverAddress());
        m.put("createTime", str(o.getCreateTime()));
        m.put("payLatestTime", o.getPayLatestTime());
        m.put("payTime", str(o.getPayTime()));
        m.put("consignTime", str(o.getConsignTime()));
        m.put("closeTime", str(o.getCloseTime()));
        m.put("endTime", str(o.getEndTime()));
        m.put("evaluationTime", null);
        m.put("arrivalEstimatedTime", null);
        m.put("countdown", countdown(o));
        List<Map<String, Object>> skus = new ArrayList<>();
        for (OrderItem it : items) skus.add(orderItem(it));
        m.put("skus", skus);
        return m;
    }

    /** 短视频（抖音式信息流）：带作者、互动数据，以及关联商品卡片 */
    public static Map<String, Object> video(com.java10143.entity.Video v, Spu goods) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("spuId", v.getSpuId());
        m.put("title", v.getTitle());
        m.put("description", v.getDescription());
        m.put("cover", ImgUrl.abs(v.getCover()));
        m.put("url", ImgUrl.media(v.getUrl()));
        m.put("authorName", v.getAuthorName());
        m.put("authorAvatar", ImgUrl.abs(v.getAuthorAvatar()));
        m.put("tags", tags(v.getTags()));
        m.put("likeCount", n(v.getLikeCount()));
        m.put("commentCount", n(v.getCommentCount()));
        m.put("shareCount", n(v.getShareCount()));
        m.put("collectCount", n(v.getCollectCount()));
        m.put("duration", n(v.getDuration()));
        m.put("createTime", v.getCreateTime() == null ? null : v.getCreateTime().toString().replace("T", " "));
        if (goods != null) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("id", goods.getId());
            g.put("name", goods.getName());
            g.put("price", money(goods.getPrice()));
            g.put("picture", firstPicture(goods));
            m.put("goods", g);
        } else {
            m.put("goods", null);
        }
        return m;
    }

    /** 评论 */
    public static Map<String, Object> comment(com.java10143.entity.Comment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("targetType", c.getTargetType());
        m.put("targetId", c.getTargetId());
        m.put("userId", c.getUserId());
        m.put("nickname", c.getNickname());
        m.put("avatar", ImgUrl.abs(c.getAvatar()));
        m.put("score", c.getScore());
        m.put("content", c.getContent());
        m.put("pictures", ImgUrl.abs(Json.strList(c.getPictures())));
        m.put("likeCount", n(c.getLikeCount()));
        m.put("replyCount", n(c.getReplyCount()));
        m.put("createTime", c.getCreateTime() == null ? null : c.getCreateTime().toString().replace("T", " "));
        return m;
    }

    private static List<String> tags(String s) {
        List<String> res = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) return res;
        for (String t : s.split("[,，]")) {
            String x = t.trim();
            if (!x.isEmpty()) res.add(x);
        }
        return res;
    }

    private static int n(Integer i) {
        return i == null ? 0 : i;
    }

    /** 距离支付超时还剩多少秒，-1 表示已超时 */
    public static int countdown(Orders o) {
        if (o.getPayState() != null && o.getPayState() == 1) return -1;
        if (o.getPayLatestTime() == null) return -1;
        try {
            long deadline = Long.parseLong(o.getPayLatestTime());
            long left = (deadline - System.currentTimeMillis()) / 1000;
            return left > 0 ? (int) left : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String str(java.time.LocalDateTime t) {
        return t == null ? null : t.toString().replace("T", " ");
    }
}
