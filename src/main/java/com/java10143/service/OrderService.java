package com.java10143.service;

import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.java10143.common.BizException;
import com.java10143.entity.Address;
import com.java10143.entity.OrderItem;
import com.java10143.entity.Orders;
import com.java10143.entity.Sku;
import com.java10143.entity.Spu;
import com.java10143.mapper.AddressMapper;
import com.java10143.mapper.OrderItemMapper;
import com.java10143.mapper.OrdersMapper;
import com.java10143.mapper.SkuMapper;
import com.java10143.mapper.SpuMapper;
import com.java10143.vo.Vo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 订单：结算预订单 / 提交订单 / 订单详情 / 订单列表 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final AddressMapper addressMapper;
    private final CartService cartService;
    private final AddressService addressService;
    private final CouponService couponService;

    /** 支付超时时间：30 分钟 */
    private static final long PAY_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * GET /member/order/pre?skuIds=1,2   或   body: ["1","2"]
     * 返回：商品清单 + 金额汇总 + 我的收货地址
     */
    public Map<String, Object> pre(Long userId, List<String> skuIds) {
        List<String> ids = new ArrayList<>();
        if (skuIds != null && !skuIds.isEmpty()) {
            ids.addAll(skuIds);
        } else {
            // 没传就取购物车里所有已选中的
            for (Map<String, Object> c : cartService.list(userId)) {
                if (Boolean.TRUE.equals(c.get("selected"))) {
                    ids.add(String.valueOf(c.get("skuId")));
                }
            }
        }
        if (ids.isEmpty()) throw new BizException("请先选择要购买的商品");

        List<Map<String, Object>> cartItems = cartService.findForOrder(userId, ids);

        List<Map<String, Object>> goods = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int goodsCount = 0;
        List<Long> spuIds = new ArrayList<>();

        for (Map<String, Object> c : cartItems) {
            BigDecimal price = new BigDecimal(String.valueOf(c.get("price")));
            int count = Integer.parseInt(String.valueOf(c.get("count")));
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(count));

            Map<String, Object> g = new LinkedHashMap<>();
            g.put("id", c.get("id"));
            g.put("skuId", c.get("skuId"));
            g.put("spuId", c.get("spuId"));
            g.put("name", c.get("name"));
            g.put("picture", c.get("picture"));
            g.put("attrsText", c.get("attrsText"));
            g.put("count", count);
            g.put("price", Vo.money(price));
            g.put("payPrice", Vo.money(price));
            g.put("totalPrice", Vo.money(lineTotal));
            g.put("totalPayPrice", Vo.money(lineTotal));
            goods.add(g);

            totalPrice = totalPrice.add(lineTotal);
            goodsCount += count;
            if (c.get("spuId") != null) {
                Long sid = Long.valueOf(String.valueOf(c.get("spuId")));
                if (!spuIds.contains(sid)) spuIds.add(sid);
            }
        }

        // ★ 优惠券试算：默认自动选最优，前端也能换成别的券重新算
        Map<String, Object> couponInfo = couponService.bestFor(userId, spuIds, totalPrice);
        BigDecimal couponDiscount = new BigDecimal(String.valueOf(couponInfo.get("discount")));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("goodsCount", goodsCount);
        summary.put("totalPrice", totalPrice);
        summary.put("postFee", 0);
        summary.put("discountPrice", couponDiscount);
        summary.put("couponDiscount", couponDiscount);
        summary.put("totalPayPrice", totalPrice.subtract(couponDiscount).max(BigDecimal.ZERO));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("goods", goods);
        res.put("summary", summary);
        res.put("coupons", couponInfo);
        res.put("userAddresses", addressService.list(userId));
        return res;
    }

    /**
     * POST /member/order
     * body: {addressId, buyerMessage, deliveryTimeType, goods:[{skuId,count}], payChannel, payType}
     */
    @Transactional
    public Map<String, Object> submit(Long userId, Map<String, Object> body) {
        String addressIdStr = str(body.get("addressId"));
        Object goodsObj = body.get("goods");
        if (addressIdStr == null || StrUtil.isBlank(addressIdStr)) throw new BizException("请选择收货地址");
        // JDK 8：不支持 instanceof 模式匹配
        if (!(goodsObj instanceof List)) throw new BizException("请选择要购买的商品");
        List<?> goodsList = (List<?>) goodsObj;
        if (goodsList.isEmpty()) throw new BizException("请选择要购买的商品");

        Address address = addressMapper.selectById(Long.valueOf(addressIdStr));
        if (address == null) throw new BizException("收货地址不存在");

        Orders order = new Orders();
        order.setUserId(userId);
        order.setOrderState(1);
        order.setPayState(0);
        order.setPayType(toInt(body.get("payType"), 1));
        order.setPayChannel(toInt(body.get("payChannel"), 1));
        order.setBuyerMessage(str(body.get("buyerMessage")));
        order.setDeliveryTimeType(toInt(body.get("deliveryTimeType"), 1));
        order.setReceiver(address.getReceiver());
        order.setReceiverContact(address.getContact());
        order.setProvinceCode(address.getProvinceCode());
        order.setCityCode(address.getCityCode());
        order.setCountyCode(address.getCountyCode());
        order.setReceiverAddress(address.getFullLocation() + " " + address.getAddress());
        order.setPostFee(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now());
        order.setPayLatestTime(String.valueOf(System.currentTimeMillis() + PAY_TIMEOUT_MS));
        order.setTotalMoney(BigDecimal.ZERO);
        order.setPayMoney(BigDecimal.ZERO);
        order.setTotalNum(0);
        ordersMapper.insert(order);

        BigDecimal total = BigDecimal.ZERO;
        int totalNum = 0;
        List<Long> boughtSkuIds = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        List<Long> spuIds = new ArrayList<>();

        // 秒杀单：价格以 seckill.price 为准，不读 sku.price
        Long seckillId = toLongOrNull(body.get("seckillId"));
        BigDecimal seckillPrice = null;
        Long seckillSkuId = null;
        if (seckillId != null) {
            try {
                Map<String, Object> sk = jdbcSeckill(seckillId);
                seckillPrice = new BigDecimal(String.valueOf(sk.get("price")));
                seckillSkuId = Long.valueOf(String.valueOf(sk.get("sku_id")));
            } catch (Exception e) {
                throw new BizException("秒杀活动已失效");
            }
        }

        for (Object o : goodsList) {
            if (!(o instanceof Map)) continue;
            Map<?, ?> g = (Map<?, ?>) o;
            Long skuId = Long.valueOf(String.valueOf(g.get("skuId")));
            int count = toInt(g.get("count"), 1);

            Sku sku = skuMapper.selectById(skuId);
            if (sku == null) continue;
            Spu spu = spuMapper.selectById(sku.getSpuId());
            if (spu == null) continue;

            // 秒杀命中就用秒杀价
            BigDecimal unitPrice = sku.getPrice();
            if (seckillPrice != null && seckillSkuId != null && seckillSkuId.equals(skuId)) {
                unitPrice = seckillPrice;
            }
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(count));

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSpuId(spu.getId());
            item.setSkuId(sku.getId());
            item.setName(spu.getName());
            item.setImage(Vo.firstPicture(spu));
            item.setAttrsText(Vo.attrsText(sku.getSpecs()));
            item.setCurPrice(unitPrice);
            item.setQuantity(count);
            item.setRealPay(lineTotal);
            orderItemMapper.insert(item);

            items.add(item);
            total = total.add(lineTotal);
            totalNum += count;
            boughtSkuIds.add(skuId);
            if (!spuIds.contains(spu.getId())) spuIds.add(spu.getId());
        }

        if (items.isEmpty()) throw new BizException("商品信息有误，下单失败");

        // ★ 优惠券核销：显式传了 couponId 就用它，没传就自动用最优券（pr=0 可关闭）
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Long ucId = toLongOrNull(body.get("couponId"));
        if (ucId == null && !"0".equals(String.valueOf(body.get("autoCoupon")))) {
            Map<String, Object> best = couponService.bestFor(userId, spuIds, total);
            Object b = best.get("best");
            if (b instanceof Map) {
                Object bid = ((Map<?, ?>) b).get("ucId");
                if (bid != null) ucId = Long.valueOf(String.valueOf(bid));
            }
        }
        if (ucId != null) {
            couponDiscount = couponService.redeem(userId, ucId, order.getId(), total);
        }

        BigDecimal payMoney = total.subtract(couponDiscount).max(BigDecimal.ZERO);

        order.setTotalMoney(total);
        order.setPayMoney(payMoney);
        order.setTotalNum(totalNum);
        order.setPostFee(BigDecimal.ZERO);
        ordersMapper.updateById(order);

        // 下单成功后，把购物车里的这些商品清掉（和真实电商一致）
        cartService.removeBySkuIds(userId, boughtSkuIds);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("orderId", order.getId());
        res.put("id", order.getId());
        res.put("totalMoney", Vo.money(total));
        res.put("couponDiscount", Vo.money(couponDiscount));
        res.put("payMoney", Vo.money(payMoney));
        return res;
    }

    /**
     * 秒杀信息读取（只在这里用，为避免 SeckillService 与 OrderService 循环依赖，直接查库）
     */
    private Map<String, Object> jdbcSeckill(Long seckillId) {
        return seckillJdbc.queryForMap("SELECT price, sku_id FROM seckill WHERE id = ?", seckillId);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate seckillJdbc;

    /** GET /member/order/{id} */
    public Map<String, Object> detail(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BizException("无权查看该订单");
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        return Vo.orderDetail(order, items);
    }

    /**
     * 不校验归属的订单查询，只给模拟支付结果页用。
     * 真实项目里这个页面要走支付平台的异步通知 + 验签，不能这样开放。
     */
    public Map<String, Object> detailAny(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        return Vo.orderDetail(order, items);
    }

    /** GET /member/order —— 我的订单列表（state 不传就查全部） */
    public List<Map<String, Object>> list(Long userId, Integer state) {
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getUserId, userId)
                .eq(state != null, Orders::getOrderState, state)
                .orderByDesc(Orders::getId));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Orders o : orders) {
            List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, o.getId()));
            res.add(Vo.orderDetail(o, items));
        }
        return res;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        try {
            return (int) Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static Long toLongOrNull(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        if (s.isEmpty() || "null".equals(s)) return null;
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}
