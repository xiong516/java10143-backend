<template>
  <view class="jd-nav" :class="{ flush: flush }" :style="navStyle">
    <view class="jd-nav-list">
      <view v-for="t in tabs"
            :key="t.key"
            class="jd-nav-item"
            :class="{ on: active === t.key }"
            @click="go(t.key)">{{ t.label }}</view>
    </view>
  </view>
</template>

<script>
import { statusBarHeight } from '../../utils/sbh.js';

export default {
  name: 'jd-nav',
  props: {
    active: { type: String, default: 'index' },
    /** flush=true 时贴边显示（页面自己贴顶部，不需要负边距） */
    flush: { type: Boolean, default: false }
  },
  data() {
    return {
      sbh: 0,
      tabs: [
        { key: 'index', label: '首页' },
        { key: 'category', label: '分类' },
        { key: 'cart', label: '购物车' },
        { key: 'my', label: '我的' }
      ]
    };
  },
  computed: {
    /* App 端补真实状态栏高度（CSS 变量在 App 端不注入，见 utils/sbh.js）；
       H5 端 sbh=0 返回 null，走 CSS 默认值，电脑网页不受影响 */
    navStyle() {
      return this.sbh ? { paddingTop: (this.sbh + 9) + 'px' } : null;
    }
  },
  created() {
    this.sbh = statusBarHeight();
  },
  methods: {
    go(key) {
      if (key === this.active) return;
      uni.switchTab({ url: '/pages/' + key + '/' + key });
    }
  }
};
</script>

<style>
.jd-nav {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #e1251b, #ff5c4d);
  padding: 18rpx 20rpx;
  /* 手机 App 里状态栏压在页面上，补回状态栏高度；H5 端该变量为 0，不受影响 */
  padding-top: calc(18rpx + var(--status-bar-height, 0px));
  margin: -20rpx -20rpx 20rpx;
  flex: none;
}
.jd-nav.flush {
  margin: 0;
}
.jd-nav-list {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.jd-nav-item {
  flex: 1;
  text-align: center;
  color: #ffe3e1;
  font-size: 26rpx;
  line-height: 52rpx;
  border-radius: 26rpx;
  transition: background-color 0.15s;
}
.jd-nav-item.on {
  color: #e1251b;
  background: #fff;
  font-weight: 700;
}
</style>
