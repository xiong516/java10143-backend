<template>
  <view class="container" v-if="order">
    <view class="card">
      <view class="title">订单详情</view>
      <view style="line-height:1.9;color:#666;font-size:26rpx">
        <view>订单号：{{ order.id }}</view>
        <view>订单状态：<text style="color:#e1251b;font-weight:bold">{{ stateText(order.orderState) }}</text></view>
        <view>下单时间：{{ order.createTime }}</view>
        <view>收货人：{{ order.receiverContact }} {{ order.receiverMobile }}</view>
        <view>收货地址：{{ order.receiverAddress }}</view>
      </view>
    </view>

    <view class="card">
      <view class="title">商品清单</view>
      <view class="row" v-for="s in order.skus" :key="s.skuId"
            style="border-bottom:1rpx solid #f0f0f0;padding:20rpx 0">
        <image :src="s.image" style="width:140rpx;height:140rpx;border-radius:12rpx;margin-right:16rpx" />
        <view style="flex:1">
          <view style="font-size:26rpx">{{ s.name }}</view>
          <view style="color:#999;font-size:22rpx">{{ s.attrsText }}</view>
          <view style="margin-top:8rpx">￥{{ s.curPrice }} × {{ s.quantity }}</view>
        </view>
        <text class="price">￥{{ s.realPay }}</text>
      </view>
      <view class="row" style="justify-content:flex-end;margin-top:20rpx">
        合计：<text class="price" style="font-size:36rpx">￥{{ order.payMoney }}</text>
      </view>
    </view>

    <view class="card">
      <view class="title">模拟支付宝支付</view>
      <view style="color:#999;font-size:24rpx;word-break:break-all">{{ payUrl || '已支付完成' }}</view>
    </view>
  </view>
</template>

<script>
import { api } from '../../api/request.js';

export default {
  data() {
    return { order: null, payUrl: '' };
  },
  async onLoad(options) {
    this.order = await api.orderDetail(options.id);
    const pay = await api.pay(options.id);
    this.payUrl = pay.redirect;
  },
  methods: {
    stateText(s) {
      return { 1: '待付款', 2: '待发货', 3: '待收货', 4: '待评价', 5: '已完成', 6: '已取消' }[s] || s;
    }
  }
};
</script>
