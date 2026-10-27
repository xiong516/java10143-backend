<template>
  <view class="detail-page">
    <!-- 自定义头部：pages.json 已对本页设 navigationStyle=custom，系统导航不再渲染，
         只保留这一个头部，避免出现两个"商品详情" -->
    <!-- 自定义头部：手机端把「返回箭头 + 商品名 + 看视频」放同一行；电脑网页显示商品名 -->
    <view class="detail-head" :class="{ 'dh-mobile': isMobile }" :style="headStyle">
      <view class="dh-back" @click="goBack">‹</view>
      <view class="dh-title">{{ isMobile ? (goods ? goods.name : '商品详情') : headTitle }}</view>
      <view v-if="isMobile && goods && goods.video && goods.video.url" class="dh-vbtn" @click="showVideo = true">▶ 看视频</view>
    </view>

    <!-- 数据加载中 -->
    <view class="detail-loading" v-if="!goods">
      <view class="dl-spinner"></view>
      <text class="dl-text">加载中…</text>
    </view>

    <view class="container" v-if="goods">
    <swiper class="banner" indicator-dots circular>
      <swiper-item v-for="(pic, i) in goods.mainPictures" :key="i">
        <image :src="pic" mode="aspectFill" style="width:100%;height:600rpx" />
      </swiper-item>
    </swiper>
    <!-- AI 短视频入口：商品配了视频时显示（手机端已放进头部那一行，这里只在电脑网页显示） -->
    <view class="vbtn" v-if="goods.video && goods.video.url && !isMobile" @click="showVideo = true">▶ 看视频</view>
    <!-- 全屏视频浮层：用网页原生 video 播放，App 打包没勾 videoplayer 模块也能正常看 -->
    <view class="vmodal" v-if="showVideo" @click="showVideo = false">
      <view class="vclose">✕ 点击任意处关闭</view>
      <view class="vplayer" @click.stop v-html="videoHtml"></view>
    </view>

    <view class="card">
      <view style="font-size:34rpx;font-weight:bold">{{ goods.name }}</view>
      <view style="color:#999;font-size:24rpx;margin-top:8rpx">{{ goods.desc }}</view>
      <view style="margin-top:16rpx">
        <text class="price" style="font-size:44rpx">￥{{ currentPrice }}</text>
        <text style="color:#999;text-decoration:line-through;margin-left:16rpx">￥{{ goods.oldPrice }}</text>
      </view>
      <view style="color:#999;font-size:24rpx;margin-top:8rpx">
        库存 {{ goods.inventory }} · 已售 {{ goods.salesCount }} · 评价 {{ goods.commentCount }}
      </view>
      <!-- 价格走势入口（H5 直接看图；小程序/APP 用文字版） -->
      <view class="ph-entry" @click="togglePrice">
        <text>📈</text>
        <text class="ph-brief">{{ phBrief }}</text>
        <text class="ph-arrow">{{ phOpen ? '收起 ▴' : '走势 ▾' }}</text>
      </view>
      <view class="ph-panel" v-if="phOpen">
        <view class="ph-stats">
          <view class="phs"><em>当前价</em><b class="hot">￥{{ ph.current }}</b></view>
          <view class="phs"><em>历史最低</em><b>￥{{ ph.min }}</b></view>
          <view class="phs"><em>历史最高</em><b>￥{{ ph.max }}</b></view>
          <view class="phs"><em>平均价</em><b>￥{{ ph.avg }}</b></view>
        </view>
        <!-- #ifdef H5 -->
        <canvas ref="phc" class="ph-canvas" :width="phW" :height="150"></canvas>
        <!-- #endif -->
        <view class="ph-advice">
          <text :class="'ph-tip ' + ph.adviceLevel">{{ ph.advice }}</text>
          <text class="ph-sub">当前价处于近 {{ phDays }} 天第 {{ ph.percentile }} 百分位</text>
        </view>
        <view class="ph-tabs">
          <text v-for="d in [30,60,90]" :key="d" :class="'ph-t ' + (phDays === d ? 'on' : '')" @click="loadPrice(d)">近{{ d }}天</text>
        </view>
      </view>
    </view>

    <view class="card" v-if="goods.specs && goods.specs.length">
      <view class="title">选择规格</view>
      <view v-for="s in goods.specs" :key="s.id" style="margin-bottom:20rpx">
        <view style="color:#666;font-size:26rpx;margin-bottom:12rpx">{{ s.name }}</view>
        <view class="row" style="flex-wrap:wrap">
          <view v-for="v in s.values" :key="v.name"
                :style="'padding:10rpx 26rpx;border:1rpx solid ' + (choose[s.name] === v.name ? '#e1251b' : '#ddd') + ';color:' + (choose[s.name] === v.name ? '#e1251b' : '#333') + ';border-radius:8rpx;margin:0 16rpx 16rpx 0'"
                @click="pickSpec(s.name, v.name)">
            {{ v.name }}
          </view>
        </view>
      </view>
    </view>

    <view class="card row" style="justify-content:space-between">
      <text>购买数量</text>
      <view class="qty">
        <view @click="changeQty(-1)">-</view>
        <text>{{ qty }}</text>
        <view @click="changeQty(1)">+</view>
      </view>
    </view>

    <view class="card">
      <view class="title">商品评论（{{ comments.length }}）</view>
      <view class="cmt" v-for="c in comments" :key="c.id">
        <image class="cavatar" :src="c.avatar" mode="aspectFill" />
        <view class="cbody">
          <text class="cname">{{ c.nickname }}</text>
          <text class="ctext">{{ c.content }}</text>
          <view class="cfoot">
            <text class="ctime">{{ c.createTime }}</text>
            <text class="cdel" v-if="canDelete(c)" @click="delComment(c)">删除</text>
          </view>
        </view>
      </view>
      <view class="empty" v-if="!comments.length">还没有评论，来做第一个吧～</view>

      <view class="cmt-input">
        <input v-model="cmtText" placeholder="说说你的使用感受…（需登录）" @confirm="sendComment" />
        <button size="mini" class="btn-primary" @click="sendComment">发表</button>
      </view>
    </view>

    <view class="row" style="gap:20rpx;padding:0 20rpx 40rpx">
      <button class="btn-plain" style="flex:1" @click="addCart">加入购物车</button>
      <button class="btn-primary" style="flex:1" @click="buyNow">立即购买</button>
    </view>
    </view>
  </view>
</template>

<script>
import { api, getToken, getUser } from '../../api/request.js';
import { statusBarHeight } from '../../utils/sbh.js';

export default {
  data() {
    return { goods: null, goodsId: '', choose: {}, chosenSku: null, qty: 1, comments: [], cmtText: '', me: null, showVideo: false,
             phOpen: false, phLoading: false, phDays: 90, ph: null, phBrief: '近 90 天价格走势', phW: 600,
             isMobile: false, sbh: 0 };
  },
  computed: {
    currentPrice() {
      return this.chosenSku ? this.chosenSku.price : (this.goods ? this.goods.price : '0.00');
    },
    /* 头部标题：加载完显示商品名，加载中显示"商品详情" */
    headTitle() {
      return this.goods ? (this.goods.name || '商品详情') : '商品详情';
    },
    /* App 端把真实状态栏高度补到头部 padding-top 上（CSS 变量在 App 端不生效，见 utils/sbh.js），
       防止红色头部被系统状态栏压住；H5 端 sbh=0，返回 null 走 CSS 默认值 */
    headStyle() {
      return this.sbh ? { paddingTop: (this.sbh + 8) + 'px' } : null;
    },
    /* 网页原生 video（v-html 注入）：不走 App 原生播放器，免 videoplayer 模块 */
    videoHtml() {
      const v = this.goods && this.goods.video;
      if (!v || !v.url) return '';
      const esc = s => String(s || '').replace(/"/g, '&quot;');
      return '<video src="' + esc(v.url) + '" poster="' + esc(v.cover) + '" controls autoplay ' +
        'playsinline webkit-playsinline x5-video-player-type="h5" ' +
        'style="width:100%;height:100%;object-fit:contain;background:#000;border-radius:12rpx"></video>';
    }
  },
  async onLoad(options) {
    // 状态栏高度：App 端取真实值补到头部 padding（H5 端为 0）
    this.sbh = statusBarHeight();
    // 手机端判断（2026-10-24 修复）：必须两端都执行！
    // 之前包在 #ifdef H5 里，App 端编译后 isMobile 永远是 false，
    // 导致手机 App 里全是电脑版样式（看视频按钮位置错、dh-mobile 不生效）。
    // App 端（#ifndef H5）本身就是手机，直接 true；H5 端按 UA 判断。
    // #ifndef H5
    this.isMobile = true;
    // #endif
    // #ifdef H5
    this.isMobile = /Mobile|Android|iPhone|HarmonyOS/i.test(navigator.userAgent);
    // #endif
    this.goodsId = options.id;
    this.me = getUser();
    this.goods = await api.goods(options.id);
    (this.goods.specs || []).forEach(s => {
      if (s.values && s.values.length) this.choose[s.name] = s.values[0].name;
    });
    this.chosenSku = this.matchSku() || (this.goods.skus || [])[0] || null;
    this.loadComments();
    // 带 openPrice=1 进来时自动展开价格走势（方便分享/演示直达）
    if (options.openPrice === '1' || options.openPrice === 'true') {
      setTimeout(() => this.togglePrice(), 600);
    }
  },
  methods: {
    /* 返回三步走，保证 H5 任何入口点返回都有反应：
       1) 页面栈里还有上一页（应用内跳转进来的）→ uni.navigateBack()；
       2) 栈里只有本页但浏览器有历史（刷新 / 直接打开详情链接）→ history.back()，
          并在 600ms 后检查：真的退掉了就不管，还停在详情页就兜底回首页；
       3) 浏览器也没历史（新标签页直接打开）→ 直接回首页。 */
    goBack() {
      // #ifdef H5
      var pages = getCurrentPages();
      if (pages.length > 1) {
        uni.navigateBack({
          fail: () => uni.reLaunch({ url: '/pages/index/index' })
        });
        return;
      }
      if (window.history.length > 1) {
        window.history.back();
        setTimeout(() => {
          var cur = getCurrentPages();
          if (cur.length === 1 && (cur[0].route || '').indexOf('detail') >= 0) {
            uni.reLaunch({ url: '/pages/index/index' });
          }
        }, 600);
      } else {
        uni.reLaunch({ url: '/pages/index/index' });
      }
      // #endif
      // #ifndef H5
      var pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
      } else {
        uni.reLaunch({ url: '/pages/index/index' })
      }
      // #endif
    },
    /* ---------------- 价格走势 ---------------- */
    togglePrice() {
      this.phOpen = !this.phOpen;
      if (this.phOpen && !this.ph) this.loadPrice(this.phDays);
      else if (this.phOpen) this.$nextTick(() => this.drawPrice());
    },
    async loadPrice(days) {
      if (this.phLoading) return;
      this.phLoading = true;
      this.phDays = days || 90;
      try {
        const d = await api.priceHistory(this.goodsId, this.phDays);
        this.ph = d;
        this.phBrief = `近 ${d.days} 天最低 ￥${d.min}`;
        // #ifdef H5
        this.$nextTick(() => this.drawPrice());
        // #endif
      } catch (e) {
        uni.showToast({ title: '走势加载失败', icon: 'none' });
      }
      this.phLoading = false;
    },
    // H5 手绘折线，不引第三方库
    drawPrice() {
      // #ifdef H5
      const d = this.ph;
      let cv = this.$refs.phc;
      if (cv && cv.$el) cv = cv.$el.querySelector('canvas') || cv.$el;
      if (!cv || !cv.getContext) return;
      if (!d || !d.labels || !d.labels.length) return;
      let W = cv.parentElement ? cv.parentElement.clientWidth - 16 : 300;
      if (!W || W < 60) W = 300;
      this.phW = W;
      const H = 150, dpr = window.devicePixelRatio || 1;
      cv.width = W * dpr; cv.height = H * dpr;
      const ctx = cv.getContext('2d');
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, W, H);
      const padL = 46, padR = 14, padT = 14, padB = 24;
      const w = W - padL - padR, h = H - padT - padB;
      const prices = d.prices.map(Number);
      let min = Math.min(...prices), max = Math.max(...prices);
      if (max === min) max = min + 1;
      const pad = (max - min) * 0.15; min -= pad; max += pad;
      const X = i => padL + (prices.length === 1 ? w / 2 : (i / (prices.length - 1)) * w);
      const Y = v => padT + h - ((v - min) / (max - min)) * h;
      ctx.strokeStyle = '#eef0f3'; ctx.fillStyle = '#9aa0a8';
      ctx.font = '10px sans-serif'; ctx.textAlign = 'right'; ctx.lineWidth = 1;
      for (let i = 0; i <= 4; i++) {
        const v = min + (max - min) * i / 4, y = Y(v);
        ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(W - padR, y); ctx.stroke();
        ctx.fillText('￥' + Math.round(v), padL - 6, y + 3);
      }
      const grad = ctx.createLinearGradient(0, padT, 0, padT + h);
      grad.addColorStop(0, 'rgba(225,37,27,.22)'); grad.addColorStop(1, 'rgba(225,37,27,0)');
      ctx.beginPath();
      prices.forEach((v, i) => i === 0 ? ctx.moveTo(X(i), Y(v)) : ctx.lineTo(X(i), Y(v)));
      ctx.lineTo(X(prices.length - 1), padT + h); ctx.lineTo(X(0), padT + h);
      ctx.closePath(); ctx.fillStyle = grad; ctx.fill();
      ctx.beginPath();
      prices.forEach((v, i) => i === 0 ? ctx.moveTo(X(i), Y(v)) : ctx.lineTo(X(i), Y(v)));
      ctx.strokeStyle = '#e1251b'; ctx.lineWidth = 2; ctx.lineJoin = 'round'; ctx.stroke();
      const iMin = prices.indexOf(Math.min(...prices));
      ctx.beginPath(); ctx.arc(X(iMin), Y(prices[iMin]), 4.5, 0, Math.PI * 2);
      ctx.fillStyle = '#fff'; ctx.fill();
      ctx.strokeStyle = '#0a8a4a'; ctx.lineWidth = 2.5; ctx.stroke();
      ctx.fillStyle = '#0a8a4a'; ctx.font = 'bold 10px sans-serif'; ctx.textAlign = 'center';
      ctx.fillText('最低', X(iMin), Y(prices[iMin]) - 9);
      const lastI = prices.length - 1;
      ctx.beginPath(); ctx.arc(X(lastI), Y(prices[lastI]), 4, 0, Math.PI * 2);
      ctx.fillStyle = '#e1251b'; ctx.fill();
      ctx.fillStyle = '#9aa0a8'; ctx.font = '10px sans-serif';
      ctx.textAlign = 'left'; ctx.fillText((d.labels[0] || '').slice(5), padL, H - 7);
      ctx.textAlign = 'right'; ctx.fillText((d.labels[lastI] || '').slice(5), W - padR, H - 7);
      // #endif
    },
    async loadComments() {
      try {
        this.comments = await api.commentList('goods', this.goodsId);
      } catch (e) {
        this.comments = [];
      }
    },
    canDelete(c) {
      if (!this.me) return false;
      return String(c.userId) === String(this.me.id) || this.me.admin === true;
    },
    async sendComment() {
      if (!this.checkLogin()) return;
      const text = (this.cmtText || '').trim();
      if (!text) return uni.showToast({ title: '评论内容不能为空', icon: 'none' });
      try {
        await api.commentAdd('goods', this.goodsId, text, 5);
        this.cmtText = '';
        uni.showToast({ title: '评论成功', icon: 'none' });
        this.loadComments();
      } catch (e) {
        /* 已提示 */
      }
    },
    delComment(c) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除这条评论吗？',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await api.commentDelete(c.id);
            this.loadComments();
          } catch (e) {
            /* 已提示 */
          }
        }
      });
    },
    pickSpec(name, value) {
      this.choose[name] = value;
      this.choose = Object.assign({}, this.choose);
      this.chosenSku = this.matchSku();
    },
    matchSku() {
      const skus = this.goods.skus || [];
      return skus.find(sku => Object.keys(this.choose).every(k =>
        (sku.specs || []).some(s => s.name === k && s.valueName === this.choose[k]))) || null;
    },
    changeQty(d) {
      const n = this.qty + d;
      if (n >= 1 && n <= 99) this.qty = n;
    },
    checkLogin() {
      if (!getToken()) {
        uni.showToast({ title: '请先到「我的」页面登录', icon: 'none' });
        return false;
      }
      return true;
    },
    async addCart() {
      if (!this.checkLogin()) return;
      if (!this.chosenSku) return uni.showToast({ title: '请选择规格', icon: 'none' });
      await api.cartAdd(this.chosenSku.id, this.qty);
      uni.showToast({ title: '已加入购物车' });
    },
    async buyNow() {
      if (!this.checkLogin()) return;
      if (!this.chosenSku) return uni.showToast({ title: '请选择规格', icon: 'none' });
      try {
        await api.cartAdd(this.chosenSku.id, this.qty);
      } catch (e) {
        /* 购物车里可能已有该商品（后端合并数量），不拦着继续结算 */
      }
      // ★ 购物车是 tabBar 页面：navigateTo 跳它会静默失败，switchTab 又不能带参数，
      //   所以用 storage 把要结算的 skuId 中转给购物车页
      uni.setStorageSync('checkoutSkuIds', [String(this.chosenSku.id)]);
      uni.switchTab({
        url: '/pages/cart/cart',
        fail: () => uni.showToast({ title: '跳转购物车失败，请重试', icon: 'none' })
      });
    }
  }
};
</script>

<style>
/* ===== 自定义头部（替代系统导航栏，只保留这一个） ===== */
.detail-head {
  display: flex;
  align-items: center;
  background: #e1251b;
  padding: 16rpx 20rpx;
  /* 状态栏安全区：App 端状态栏压在页面上，用 --status-bar-height 补回；H5 端该变量为 0 */
  padding-top: calc(16rpx + var(--status-bar-height, 0px));
  flex: none;
  overflow: hidden; /* 双保险：标题再长也不会把右侧按钮挤出栏目 */
}
.dh-back {
  color: #fff;
  font-size: 56rpx;
  line-height: 56rpx;
  width: 64rpx;
  text-align: center;
  flex: none;
  font-family: serif;
}
.dh-title {
  flex: 1;
  min-width: 0; /* 关键：允许收缩到内容宽度以下，省略号才生效，否则长标题会把按钮挤出去 */
  color: #fff;
  font-size: 34rpx;
  font-weight: 600;
  text-align: center;
  margin-right: 64rpx; /* 抵消左侧返回键宽度，让标题真正居中 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 手机端：箭头 + 商品名 + 看视频 同一行，标题左对齐，不再让看视频浮到最顶上 */
.detail-head.dh-mobile .dh-title {
  text-align: left;
  margin-right: 16rpx;
  min-width: 0;
  font-size: 30rpx; /* 手机上一行要放箭头+长商品名+按钮，字号收一点留空间 */
}
.dh-vbtn {
  flex: none;
  color: #fff;
  font-size: 24rpx;
  background: rgba(0, 0, 0, .38);
  padding: 8rpx 20rpx;
  border-radius: 30rpx;
  white-space: nowrap;
}
/* ===== 加载中 ===== */
.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 45vh;
}
.dl-spinner {
  width: 44rpx;
  height: 44rpx;
  border: 5rpx solid #e1251b;
  border-top-color: transparent;
  border-radius: 50%;
  animation: dl-spin 0.8s linear infinite;
}
.dl-text { margin-top: 20rpx; font-size: 24rpx; color: #999; }
@keyframes dl-spin { to { transform: rotate(360deg); } }

/* 商品 AI 短视频入口 + 全屏浮层 */
.vbtn { position: absolute; top: 20rpx; right: 20rpx; z-index: 9; background: rgba(0,0,0,.55); color: #fff;
  font-size: 24rpx; padding: 10rpx 24rpx; border-radius: 30rpx; }
.vmodal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,.9);
  z-index: 9999; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.vclose { color: #ddd; font-size: 24rpx; margin-bottom: 24rpx; }
.vplayer { width: 92vw; height: 88vh; }
.vplayer video { width: 100%; height: 100%; }
.cmt { display: flex; padding: 20rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.cavatar { width: 64rpx; height: 64rpx; border-radius: 32rpx; margin-right: 16rpx; }
.cbody { flex: 1; }
.cname { color: #4a6fa5; font-size: 24rpx; }
.ctext { display: block; color: #333; font-size: 27rpx; margin: 6rpx 0; }
.cfoot { display: flex; align-items: center; }
.ctime { color: #aaa; font-size: 21rpx; margin-right: 20rpx; }
.cdel { color: #e1251b; font-size: 21rpx; }
.cmt-input { display: flex; align-items: center; margin-top: 20rpx; }
.cmt-input input { flex: 1; height: 70rpx; background: #f2f3f5; border-radius: 35rpx; padding: 0 24rpx; font-size: 26rpx; }
.cmt-input button { margin-left: 16rpx; }

/* 价格走势 */
.ph-entry { display: flex; align-items: center; gap: 10rpx; margin-top: 16rpx; padding: 12rpx 20rpx;
  background: #fff8f7; border: 1rpx solid #ffe0da; border-radius: 14rpx; width: fit-content; }
.ph-entry:active { background: #fff1ef; }
.ph-brief { font-size: 23rpx; color: #a8422f; }
.ph-arrow { font-size: 21rpx; color: #c98b7d; }
.ph-panel { margin-top: 16rpx; padding: 20rpx; background: #fcfcfd; border-radius: 16rpx; }
.ph-stats { display: flex; gap: 12rpx; margin-bottom: 16rpx; }
.phs { flex: 1; background: #fff; border: 1rpx solid #f0f1f4; border-radius: 12rpx; padding: 12rpx 8rpx; text-align: center; }
.phs em { display: block; font-style: normal; font-size: 19rpx; color: #9aa0a8; }
.phs b { font-size: 25rpx; color: #2b2f3a; }
.phs b.hot { color: #e1251b; }
.ph-canvas { width: 100%; background: #fff; border-radius: 12rpx; }
.ph-advice { margin-top: 14rpx; display: flex; flex-direction: column; gap: 6rpx; }
.ph-tip { font-size: 23rpx; padding: 8rpx 18rpx; border-radius: 10rpx; font-weight: 600; align-self: flex-start; }
.ph-tip.low { color: #0a8a4a; background: #eafaf1; }
.ph-tip.high { color: #e1251b; background: #fff1ef; }
.ph-tip.mid { color: #8a6d1f; background: #fdf7e5; }
.ph-sub { font-size: 20rpx; color: #9aa0a8; }
.ph-tabs { display: flex; gap: 12rpx; margin-top: 16rpx; }
.ph-t { padding: 8rpx 22rpx; border-radius: 22rpx; background: #fff; border: 1rpx solid #eceef2; font-size: 21rpx; color: #7a7f87; }
.ph-t.on { background: #fff1ef; border-color: #ffc8be; color: #e1251b; font-weight: 700; }
</style>
