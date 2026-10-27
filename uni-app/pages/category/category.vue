<template>
  <view class="cat-wrap">
    <jd-nav active="category" flush />

    <view class="cat-page">
      <!-- 左侧一级分类 -->
      <view class="cat-side">
        <view v-for="c in heads" :key="c.id"
              class="side-item"
              :class="{ on: String(c.id) === String(activeId) }"
              @click="pick(c.id)">
          {{ c.name }}
        </view>
      </view>

      <!-- 右侧商品 -->
      <view class="cat-main">
        <view v-for="child in children" :key="child.id" class="card">
          <view style="font-weight:bold;margin-bottom:16rpx">{{ child.name }}</view>
          <view class="grid">
            <view class="product" v-for="g in child.goods" :key="g.id" @click="goDetail(g.id)">
              <view class="pic">
                <image :src="g.picture" mode="aspectFit" />
              </view>
              <view class="name">{{ g.name }}</view>
              <view class="p">￥{{ g.price }}</view>
            </view>
          </view>
        </view>
        <view v-if="!children.length" class="empty">该分类下暂无商品</view>
      </view>

      <!-- AI 客服悬浮球 -->
      <view class="cs-fab" @click="goChat()"><text class="cs-txt">客服</text></view>
    </view>
  </view>
</template>

<script>
import { api } from '../../api/request.js';

export default {
  data() {
    return { heads: [], children: [], activeId: '' };
  },
  async onLoad() {
    this.heads = await api.categoryHead();
    if (this.heads.length) await this.pick(this.heads[0].id);
  },
  methods: {
    async pick(id) {
      this.activeId = id;
      const detail = await api.category(id);
      this.children = detail.children || [];
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + id });
    },
    goChat() {
      uni.navigateTo({ url: '/pages/chat/chat' });
    }
  }
};
</script>

<style>
/* 分类页整体布局：顶部导航 + 左右两栏。不用 scroll-view，避免 uni-scroll-view
   自带 width/height 默认值与 flex 冲突 */
.cat-wrap {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}
.cat-page {
  flex: 1;
  min-height: 0;
  display: flex;
  background: #f5f5f5;
  overflow: hidden;
}
.cat-side {
  width: 96px;
  flex-shrink: 0;
  min-height: 0;
  overflow-y: auto;
  background: #fff;
}
.side-item {
  padding: 14px 8px;
  font-size: 14px;
  text-align: center;
  color: #333;
  border-left: 3px solid transparent;
  box-sizing: border-box;
}
.side-item.on {
  color: #e1251b;
  font-weight: bold;
  background: #fff4f3;
  border-left-color: #e1251b;
}
.cat-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden auto;
  padding: 10px;
  box-sizing: border-box;
}
/* 两列商品，留 10px 间隙 */
.cat-main .grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}
.cat-main .product {
  width: calc(50% - 5px);
  min-width: 0;
  max-width: 100%;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 10px;
  padding-bottom: 4px;
  box-sizing: border-box;
}
/* 图片框做成正方形（宽度自适应 + padding-top 撑高），配合 aspectFit：
   图片完整显示不裁切，比例不匹配时四周留白，不会压到下面的文字 */
.cat-main .product .pic {
  position: relative;
  width: 100%;
  height: 0;
  padding-top: 100%;
  background: #f7f7f7;
  overflow: hidden;
}
.cat-main .product .pic image {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  display: block;
}
.cat-main .product .name {
  font-size: 13px;
  line-height: 18px;
  padding: 6px 6px 2px;
  /* 固定两行高度，超出省略；不再用固定 height 硬裁 */
  height: 42px;
  box-sizing: border-box;
  overflow: hidden;
  color: #333;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.cat-main .product .p {
  color: #e1251b;
  font-weight: bold;
  padding: 0 6px 6px;
  font-size: 15px;
  line-height: 20px;
}

/* AI 客服悬浮球（分类页） */
.cs-fab {
  position: fixed; right: 14px; bottom: 90px; z-index: 999;
  width: 52px; height: 52px; border-radius: 50%;
  background: linear-gradient(135deg, #e1251b, #ff5c4d);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(225, 37, 27, .45);
}
.cs-fab:active { transform: scale(.94); }
.cs-txt { color: #fff; font-size: 13px; font-weight: 700; }
</style>
