<template>
  <view class="container">
    <jd-nav active="my" />

    <!-- 未登录：登录表单 -->
    <view v-if="!user" class="card">
      <view class="title">登录京东10143</view>
      <input v-model="account" placeholder="用户名（hm）" style="border:1rpx solid #ddd;padding:20rpx;border-radius:12rpx;margin-bottom:20rpx" />
      <input v-model="password" password placeholder="登录密码" style="border:1rpx solid #ddd;padding:20rpx;border-radius:12rpx;margin-bottom:20rpx" />
      <button class="btn-primary" @click="doLogin">登录</button>
      <view style="color:#999;font-size:24rpx;margin-top:16rpx;text-align:center">演示账号：hm / test / admin</view>
    </view>

    <!-- 已登录 -->
    <block v-else>
      <view class="card row" style="gap:20rpx">
        <image :src="user.avatar" style="width:110rpx;height:110rpx;border-radius:55rpx" />
        <view style="flex:1">
          <view style="font-size:32rpx;font-weight:bold">{{ user.nickname }}</view>
          <view style="color:#999;font-size:24rpx">{{ user.mobile }} · {{ user.profession }}</view>
        </view>
        <button class="btn-plain" size="mini" @click="doLogout">退出</button>
      </view>

      <view class="card">
        <view class="title">更多</view>
        <view class="menu-item" @click="goChat">
          <text>🤖 AI 客服</text>
          <text class="arrow">›</text>
        </view>
        <view class="menu-item" @click="goVideo">
          <text>🎬 商品短视频</text>
          <text class="arrow">›</text>
        </view>
        <view class="menu-item" @click="goAdmin">
          <text>🛠 管理后台（仅管理员）</text>
          <text class="arrow">›</text>
        </view>
      </view>

      <view class="card">
        <view class="title">我的订单（{{ orders.length }}）</view>
        <view v-for="o in orders" :key="o.id" style="border-bottom:1rpx solid #f0f0f0;padding:20rpx 0"
              @click="goOrder(o.id)">
          <view class="row" style="justify-content:space-between">
            <text style="color:#666;font-size:24rpx">订单号 {{ o.id }}</text>
            <text :style="'font-weight:bold;color:' + (o.orderState >= 5 ? '#e1251b' : '#333')">
              {{ stateText(o.orderState) }}
            </text>
          </view>
          <view class="row" style="margin-top:14rpx">
            <image :src="o.skus && o.skus[0] ? o.skus[0].image : ''"
                   style="width:110rpx;height:110rpx;border-radius:12rpx;margin-right:16rpx" />
            <view style="flex:1;font-size:26rpx">{{ o.skus && o.skus[0] ? o.skus[0].name : '' }}</view>
            <text class="price">￥{{ o.payMoney }}</text>
          </view>
        </view>
        <view v-if="!orders.length" class="empty">还没有订单</view>
      </view>
    </block>
  </view>
</template>

<script>
import { api, getUser, setLogin, logout } from '../../api/request.js';

export default {
  data() {
    return { user: null, account: 'hm', password: '123456', orders: [] };
  },
  onShow() {
    this.user = getUser();
    if (this.user) this.loadOrders();
  },
  methods: {
    async doLogin() {
      const user = await api.login(this.account, this.password);
      setLogin(user);
      this.user = user;
      uni.showToast({ title: '登录成功' });
      this.loadOrders();
    },
    doLogout() {
      logout();
      this.user = null;
      this.orders = [];
    },
    async loadOrders() {
      this.orders = await api.orderList();
    },
    stateText(s) {
      return { 1: '待付款', 2: '待发货', 3: '待收货', 4: '待评价', 5: '已完成', 6: '已取消' }[s] || s;
    },
    goOrder(id) {
      uni.navigateTo({ url: '/pages/order/order?id=' + id });
    },
    goChat() {
      uni.navigateTo({ url: '/pages/chat/chat' });
    },
    goVideo() {
      uni.navigateTo({ url: '/pages/video/video' });
    },
    goAdmin() {
      uni.navigateTo({ url: '/pages/admin/admin' });
    }
  }
};
</script>

<style>
.menu-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 26rpx 0; border-bottom: 1rpx solid #f0f0f0; font-size: 28rpx; color: #333;
}
.menu-item .arrow { color: #cccccc; font-size: 34rpx; }
</style>
