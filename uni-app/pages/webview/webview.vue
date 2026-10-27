<template>
  <view class="wrap">
    <web-view v-if="url" :src="url" @error="onErr"></web-view>
    <view v-else class="tip">
      <text class="t1">活动页地址无效</text>
      <text class="t2">请确认后端已启动（8080）</text>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return { url: '' };
  },
  onLoad(q) {
    const raw = q && q.url ? decodeURIComponent(q.url) : '';
    this.url = raw;
    if (q && q.title) {
      uni.setNavigationBarTitle({ title: decodeURIComponent(q.title) });
    }
  },
  methods: {
    onErr() {
      uni.showToast({ title: '页面加载失败，检查网络', icon: 'none' });
    }
  }
};
</script>

<style scoped>
.wrap {
  width: 100%;
  height: 100vh;
  box-sizing: border-box;
  /* 状态栏补偿由被加载的 html 页面自己做（plus 取真实高度写进 --sbh），
     这里不再补，避免 App 里双重 padding；H5/电脑网页不受影响 */
  background: #f5f5f5;
}
.tip { padding-top: 40vh; display: flex; flex-direction: column; align-items: center; }
.t1 { font-size: 16px; color: #333; }
.t2 { margin-top: 8px; font-size: 13px; color: #999; }
</style>
