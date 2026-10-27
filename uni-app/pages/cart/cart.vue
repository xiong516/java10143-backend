<template>
  <view class="container">
    <jd-nav active="cart" />

    <view v-if="!token" class="card empty">
      还没有登录，请先到「我的」页面登录
    </view>

    <block v-else>
      <view class="card" v-if="!list.length">
        <view class="empty">购物车还是空的</view>
      </view>

      <view class="card" v-for="c in list" :key="c.skuId">
        <view class="row">
          <checkbox :checked="selected.indexOf(String(c.skuId)) > -1"
                    @click="toggle(String(c.skuId))" />
          <image :src="c.picture" mode="aspectFill"
                 style="width:140rpx;height:140rpx;border-radius:12rpx;margin:0 16rpx" />
          <view style="flex:1">
            <view style="font-size:26rpx">{{ c.name }}</view>
            <view style="color:#999;font-size:22rpx;margin:6rpx 0">{{ c.attrsText }}</view>
            <view class="row" style="justify-content:space-between">
              <text class="price">￥{{ c.nowPrice }}</text>
              <view class="qty">
                <view @click="changeCount(c, -1)">-</view>
                <text>{{ c.count }}</text>
                <view @click="changeCount(c, 1)">+</view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="card row" style="justify-content:space-between" v-if="list.length">
        <text>已选 {{ selected.length }} 件</text>
        <view class="row" style="gap:16rpx">
          <button class="btn-plain" size="mini" @click="removeSelected">删除</button>
          <button class="btn-primary" size="mini" @click="checkout">去结算</button>
        </view>
      </view>
    </block>
  </view>
</template>

<script>
import { api, getToken } from '../../api/request.js';

export default {
  data() {
    return { list: [], selected: [], token: '' };
  },
  onShow() {
    this.token = getToken();
    // ★ 切到购物车就回到顶部：不然从详情页/长列表跳过来会停在中间的滚动位置
    this.toTop();
    // 「立即购买」跳过来的：读取中转的 skuIds，自动只勾选这一件
    const ids = uni.getStorageSync('checkoutSkuIds');
    if (ids && ids.length) {
      this.selected = ids.map(String);
      uni.removeStorageSync('checkoutSkuIds');
    }
    // 数据渲染完再压一次，保证长列表也不会停在中间
    if (this.token) this.load().then(() => this.toTop());
  },
  methods: {
    toTop() {
      uni.pageScrollTo({ scrollTop: 0, duration: 0 });
      // H5 / 小程序：内容在 tabBar 页里偶尔会自己恢复滚动位置，渲染后再压一次才稳
      this.$nextTick(() => uni.pageScrollTo({ scrollTop: 0, duration: 0 }));
    },
    async load() {
      this.list = await api.cartList();
      if (!this.selected.length) {
        this.selected = this.list.filter(c => c.selected).map(c => String(c.skuId));
      }
    },
    async toggle(skuId) {
      const i = this.selected.indexOf(skuId);
      const on = i === -1;
      if (on) this.selected.push(skuId); else this.selected.splice(i, 1);
      await api.cartUpdate(skuId, { selected: on });
    },
    async changeCount(c, d) {
      const n = c.count + d;
      if (n < 1) return;
      c.count = n;
      await api.cartUpdate(c.skuId, { count: n });
      this.load();
    },
    async removeSelected() {
      if (!this.selected.length) return uni.showToast({ title: '请先勾选', icon: 'none' });
      await api.cartDelete(this.selected);
      this.selected = [];
      this.load();
    },
    async checkout() {
      if (!this.selected.length) return uni.showToast({ title: '请先勾选商品', icon: 'none' });
      const pre = await api.orderPre(this.selected);
      const addr = (pre.userAddresses || [])[0];
      if (!addr) return uni.showToast({ title: '请先在后端 address 表里加一条收货地址', icon: 'none' });

      const res = await api.orderSubmit({
        addressId: String(addr.id),
        buyerMessage: '',
        deliveryTimeType: 1,
        payChannel: 1,
        payType: 1,
        goods: pre.goods.map(g => ({ skuId: String(g.skuId), count: g.count }))
      });
      // 调支付接口（模拟支付宝），成功后跳订单详情
      await api.pay(res.orderId);
      uni.showToast({ title: '下单并支付成功' });
      this.selected = [];
      uni.navigateTo({ url: '/pages/order/order?id=' + res.orderId });
    }
  }
};
</script>
