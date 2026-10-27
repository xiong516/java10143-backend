<template>
  <view class="container">
    <!-- 顶部导航条：四个 tab 页共用，高亮当前页 -->
    <jd-nav active="index" />

    <!-- 搜索 -->
    <view class="card row" style="padding:16rpx">
      <input v-model="keyword" placeholder="搜索商品" style="flex:1" @confirm="doSearch" />
      <button class="btn-primary" size="mini" @click="doSearch">搜索</button>
    </view>

    <!-- 轮播 -->
    <swiper v-if="banners.length" class="banner" circular autoplay :interval="3500" indicator-dots>
      <swiper-item v-for="b in banners" :key="b.id" @click="goBanner(b)">
        <image :src="b.imgUrl" mode="aspectFill" style="width:100%;height:320rpx" />
      </swiper-item>
    </swiper>

    <!-- 秒杀 + 领券 双入口（跳独立 WEB 页，H5 同源直接开；小程序/APP 内会整页打开） -->
    <view class="play-bar">
      <view class="play-card sk" @click="goWeb('/seckill.html', '限时秒杀')">
        <text class="pc-ico">⚡</text>
        <view class="pc-mid">
          <view class="pc-t1">限时秒杀</view>
          <view class="pc-t2">{{ skBrief }}</view>
        </view>
        <text class="pc-go">去抢 ›</text>
      </view>
      <view class="play-card cp" @click="goWeb('/coupon.html', '领券中心')">
        <text class="pc-ico">🎫</text>
        <view class="pc-mid">
          <view class="pc-t1">领券中心</view>
          <view class="pc-t2">{{ cpBrief }}</view>
        </view>
        <text class="pc-go">去领 ›</text>
      </view>
    </view>

    <!-- 直播带货条 -->
    <view class="live-bar" @click="goWeb('/live.html', '直播')">
      <view class="lb-left">
        <text class="lb-dot"></text>
        <text class="lb-live">LIVE</text>
        <text class="lb-t">京东直播 · 边看边买</text>
      </view>
      <text class="lb-brief">{{ liveBrief }}</text>
      <text class="lb-go">进入 ›</text>
    </view>

    <view class="card">
      <view class="title">新人专享 · 新鲜好物</view>
      <view class="row" style="overflow-x:auto;padding:4rpx 0 8rpx">
        <view v-for="p in news.slice(0,6)" :key="p.id"
              style="width:200rpx;margin-right:24rpx;flex-shrink:0"
              @click="goDetail(p.id)">
          <image :src="p.picture" mode="aspectFill" style="width:200rpx;height:200rpx;border-radius:12rpx;display:block" />
          <view class="price" style="font-size:28rpx;margin-top:8rpx">￥{{ p.price }}</view>
        </view>
      </view>
    </view>

    <!-- AI 智能推荐：登录=按你的订单类目千人千面，访客=热销兜底 -->
    <view class="card">
      <view class="title">🤖 猜你喜欢</view>
      <view class="grid">
        <view class="product" v-for="p in recommend" :key="p.id" @click="goDetail(p.id)">
          <view class="pic">
            <image :src="p.picture" mode="aspectFit" />
          </view>
          <view class="name">{{ p.name }}</view>
          <view class="p">￥{{ p.price }}</view>
          <view class="reason">{{ p.reason }}</view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="title">热销榜单</view>
      <view class="grid">
        <view class="product" v-for="p in hot" :key="p.id" @click="goDetail(p.id)">
          <view class="pic">
            <image :src="p.picture" mode="aspectFit" />
          </view>
          <view class="name">{{ p.name }}</view>
          <view class="p">￥{{ p.price }}</view>
        </view>
      </view>
    </view>

    <!-- AI 客服悬浮球（未登录也能聊，登录后自动带订单） -->
    <view class="cs-fab" @click="goChat()">
      <text class="cs-txt">客服</text>
    </view>

    <!-- 管理后台悬浮球（左侧，和客服对称；仅管理员能真正进入） -->
    <view class="ad-fab" @click="goAdmin()">
      <text class="cs-txt">管理</text>
    </view>
  </view>
</template>

<script>
import { api, BASE_URL, getOkBase } from '../../api/request.js';

export default {
  data() {
    return { banners: [], news: [], hot: [], recommend: [], keyword: '', skBrief: '整点开抢 · 售完即止', cpBrief: '先领券 · 最高立减 800', liveBrief: '主播讲解 · 直播间专享价' };
  },
  async onLoad() {
    this.banners = await api.banner(1);
    this.news = await api.newGoods(10);
    this.hot = await api.hot(20);
    // AI 推荐失败不挡首页（后端没升级完也能正常逛）
    try { this.recommend = await api.recommend(8); } catch (e) { this.recommend = []; }
    // 秒杀 / 领券 简报（失败静默，不影响主流程）
    try {
      const sk = await api.seckillList();
      const run = (sk.running || [])[0];
      if (run) this.skBrief = run.name.slice(0, 8) + '… 仅剩 ' + run.left + ' 件';
    } catch (e) {}
    try {
      const cp = await api.couponCenter();
      if (cp && cp.availableCount) this.cpBrief = cp.availableCount + ' 张券可领 · 先领再下单';
    } catch (e) {}
    try {
      const lv = await api.liveList();
      const run = lv.living || [];
      if (run.length) {
        // 汇总所有直播中的在线人数（不能只取第一个直播间）
        const online = run.reduce((a, x) => a + Number(x.onlineCount || 0), 0);
        this.liveBrief = lv.livingCount + ' 场直播中 · ' + this.fmtN(online) + ' 人在线';
      }
    } catch (e) {}
  },
  methods: {
    fmtN(v) {
      const n = Number(v || 0);
      return n >= 10000 ? (n / 10000).toFixed(1) + '万' : String(n);
    },
    // H5 内同源跳独立页；App / 小程序 用内置 webview 打开同一个页面（不再提示"请在WEB体验"）
    goWeb(path, title) {
      // 活动页（live/seckill/coupon.html）是后端静态资源，不在前端工程里；
      // H5 也必须带后端地址：写同源 '/live.html' 会打到 Vite dev server 上，
      // 被 SPA 兜底返回 index.html，表现为"点了没反应/又回到首页"
      const base = getOkBase() || ((BASE_URL && BASE_URL.indexOf('http') === 0) ? BASE_URL : '');
      // #ifdef H5
      window.location.href = base + path;
      return;
      // #endif
      // #ifndef H5
      // 优先用"刚才请求成功过的那个地址"（可能是局域网也可能是隧道），保证 webview 能打开
      uni.navigateTo({
        url: '/pages/webview/webview?url=' + encodeURIComponent(base + path) +
             '&title=' + encodeURIComponent(title || '京东10143')
      });
      // #endif
    },
    goTab(name) {
      uni.switchTab({
        url: '/pages/' + name + '/' + name,
        fail: () => uni.reLaunch({ url: '/pages/' + name + '/' + name })
      });
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + id });
    },
    // 轮播图点击 → 商品详情。
    // 后端 hrefUrl 形如 http://localhost:8080/#/goods/1001，只取里面的商品数字 id，
    // 域名部分一律不管（WEB / App / 小程序 用的是各自的本地路由，域名对 App 没意义）。
    goBanner(b) {
      if (!b) return;
      // ① 优先按 id 精确匹配，兼容 hash 路由、查询串、纯数字等各种写法
      const href = String(b.hrefUrl || '');
      const m = href.match(/goods\/(\d+)/) || href.match(/goods\?id=(\d+)/) || href.match(/^(\d+)$/);
      if (m) {
        this.goDetail(m[1]);
        return;
      }
      // ② hrefUrl 不是商品链接（比如运营配的活动页）→ 不跳，给个提示，别让用户以为卡死
      uni.showToast({ title: '该活动暂未开放', icon: 'none' });
    },
    goChat() {
      uni.navigateTo({ url: '/pages/chat/chat' });
    },
    goAdmin() {
      // #ifdef H5
      // H5 端：直接打开 PC 管理后台 admin.html（数据看板/商品/短视频/评论/用户）
      // dev(5173) 时 admin.html 由后端 8080 提供，生产(8080 同源)直接用相对路径
      var adminBase = (window.location.port === '5173') ? 'http://localhost:8080' : '';
      window.location.href = adminBase + '/admin.html';
      return;
      // #endif
      // #ifndef H5
      // 小程序 / App 端：没有浏览器可开，进内置的 H5 风格管理页
      uni.navigateTo({ url: '/pages/admin/admin' });
      // #endif
    },
    async doSearch() {
      if (!this.keyword) return;
      const all = await api.newGoods(100);
      this.hot = all.filter(p => (p.name + p.desc).indexOf(this.keyword) > -1);
      uni.showToast({ title: '找到 ' + this.hot.length + ' 件商品', icon: 'none' });
    }
  }
};
</script>

<style>
/* 顶部导航条样式统一在 components/jd-nav/jd-nav.vue 里 */

.banner { border-radius: 16rpx; overflow: hidden; margin-bottom: 20rpx; }

/* 秒杀 + 领券 双入口 */
.play-bar { display: flex; gap: 16rpx; margin-bottom: 20rpx; }
.play-card {
  flex: 1; display: flex; align-items: center; gap: 14rpx;
  padding: 24rpx; border-radius: 20rpx; color: #fff;
}
.play-card.sk { background: linear-gradient(120deg, #ff6a3d, #e1251b); }
.play-card.cp { background: linear-gradient(120deg, #f5b93c, #dd8b0c); }
.live-bar {
  display: flex; align-items: center; gap: 14rpx;
  background: linear-gradient(100deg, #2a1020, #1b1420 60%, #241018);
  border-radius: 16rpx; padding: 18rpx 22rpx; margin-bottom: 20rpx;
  border: 1rpx solid rgba(255,90,77,.28);
}
.live-bar:active { opacity: .88; }
.lb-left { display: flex; align-items: center; gap: 10rpx; }
.lb-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #ff4d3d; }
.lb-live {
  font-size: 20rpx; font-weight: 800; color: #fff; letter-spacing: 1rpx;
  background: linear-gradient(135deg, #ff5a4d, #e1251b); padding: 3rpx 12rpx; border-radius: 20rpx;
}
.lb-t { font-size: 26rpx; font-weight: 700; color: #fff; }
.lb-brief { font-size: 22rpx; color: #ffd35c; margin-left: auto; }
.lb-go { font-size: 22rpx; color: #aeb6c6; }
.play-card:active { transform: scale(.97); }
.pc-ico { font-size: 44rpx; line-height: 1; }
.pc-mid { flex: 1; min-width: 0; }
.pc-t1 { font-size: 28rpx; font-weight: 800; }
.pc-t2 { font-size: 20rpx; opacity: .92; margin-top: 4rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pc-go { font-size: 20rpx; background: rgba(255,255,255,.24); border-radius: 18rpx; padding: 6rpx 14rpx; }

/* AI 客服悬浮球：tabBar 之上，随手可点 */
.cs-fab {
  position: fixed; right: 28rpx; bottom: 180rpx; z-index: 999;
  width: 104rpx; height: 104rpx; border-radius: 50%;
  background: linear-gradient(135deg, #e1251b, #ff5c4d);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(225, 37, 27, .45);
}
.cs-fab:active { transform: scale(.94); }
.cs-txt { color: #fff; font-size: 26rpx; font-weight: 700; }

/* 管理后台悬浮球：左侧，与客服球对称，深色区分身份 */
.ad-fab {
  position: fixed; left: 28rpx; bottom: 180rpx; z-index: 999;
  width: 104rpx; height: 104rpx; border-radius: 50%;
  background: linear-gradient(135deg, #374151, #111827);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, .45);
}
.ad-fab:active { transform: scale(.94); }

/* AI 推荐理由小标签 */
.reason {
  font-size: 20rpx; color: #e1251b; background: #fff4f3;
  border-radius: 8rpx; padding: 4rpx 10rpx; margin-top: 8rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
</style>
