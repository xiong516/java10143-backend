<template>
  <view class="feed">
    <!-- 竖屏滑动：一次一屏 -->
    <swiper class="swiper" :vertical="true" :current="index" :circular="false" @change="onChange">
      <swiper-item v-for="(item, i) in list" :key="item.id">
        <view class="slide">
          <!-- 有 mp4 就播视频，没有就用封面兜底 -->
          <video
            v-if="item.url && !showCmt"
            :id="'v' + item.id"
            class="video"
            :src="item.url"
            :poster="item.cover"
            :autoplay="index === i"
            :loop="true"
            :muted="true"
            :controls="false"
            :show-center-play-btn="false"
            :enable-progress-gesture="false"
            object-fit="cover"
          />
          <view v-else class="fallback">
            <image :src="item.cover" mode="aspectFit" class="fallback-img" />
            <text class="fallback-tip">该商品暂无实拍短视频，用封面展示</text>
          </view>

          <!-- 右侧互动栏（必须用 cover-view 才能盖在原生 video 之上，App 真机才点得动） -->
          <cover-view class="rail">
            <cover-image class="avatar" :src="item.authorAvatar" />
            <cover-view class="rail-btn" @click="doLike(item)">
              <cover-view class="ico" :class="{ liked: liked[item.id] }">❤</cover-view>
              <cover-view class="n">{{ fmt(item.likeCount) }}</cover-view>
            </cover-view>
            <cover-view class="rail-btn" @click="openComments(item)">
              <cover-view class="ico">💬</cover-view>
              <cover-view class="n">{{ fmt(item.commentCount) }}</cover-view>
            </cover-view>
            <cover-view class="rail-btn" @click="share(item)">
              <cover-view class="ico">↗</cover-view>
              <cover-view class="n">{{ fmt(item.shareCount) }}</cover-view>
            </cover-view>
            <cover-view class="rail-btn" @click="doCollect(item)">
              <cover-view class="ico" :class="{ liked: collected[item.id] }">★</cover-view>
              <cover-view class="n">{{ fmt(item.collectCount) }}</cover-view>
            </cover-view>
          </cover-view>

          <!-- 底部文案（同上，必须 cover-view 才能盖在原生 video 之上） -->
          <cover-view class="meta">
            <cover-view class="author">@{{ item.authorName }}</cover-view>
            <cover-view class="title">{{ item.title }}</cover-view>
            <cover-view v-if="item.description" class="desc">{{ item.description }}</cover-view>
            <cover-view class="tags" v-if="item.tags && item.tags.length">
              <cover-view class="tag" v-for="(t, ti) in item.tags" :key="ti">#{{ t }}</cover-view>
            </cover-view>
            <cover-view class="goods" v-if="item.goods" @click="goGoods(item.goods.id)">
              <cover-image class="gimg" :src="item.goods.picture" />
              <cover-view class="gname">{{ item.goods.name }}</cover-view>
              <cover-view class="gprice">￥{{ item.goods.price }}</cover-view>
              <cover-view class="go">去看看</cover-view>
            </cover-view>
          </cover-view>
        </view>
      </swiper-item>
    </swiper>

    <!-- 评论弹层 -->
    <view class="cmt-mask" v-if="showCmt" @click="showCmt = false">
      <view class="cmt-panel" @click.stop>
        <view class="cmt-head">
          <text class="cmt-title">全部评论（{{ comments.length }}）</text>
          <text class="cmt-close" @click="showCmt = false">×</text>
        </view>
        <scroll-view scroll-y class="cmt-list">
          <view class="cmt" v-for="c in comments" :key="c.id">
            <image class="cavatar" :src="c.avatar" mode="aspectFill" />
            <view class="cbody">
              <view class="crow">
                <text class="cname">{{ c.nickname }}</text>
                <text class="cstar">{{ star(c.score) }}</text>
              </view>
              <text class="ctext">{{ c.content }}</text>
              <view class="cfoot">
                <text class="ctime">{{ c.createTime }}</text>
                <text class="clike" @click="likeComment(c)">赞 {{ c.likeCount }}</text>
                <text v-if="canDelete(c)" class="cdel" @click="delComment(c)">删除</text>
              </view>
            </view>
          </view>
          <view class="empty" v-if="!comments.length">还没有评论，来抢沙发～</view>
        </scroll-view>
        <view class="cmt-input">
          <input v-model="cmtText" placeholder="说点什么…" @confirm="sendComment" />
          <button class="send" size="mini" @click="sendComment">发送</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { api, getUser } from '../../api/request.js';

export default {
  data() {
    return {
      list: [],
      index: 0,
      liked: {},
      collected: {},
      showCmt: false,
      cmtTarget: '',
      comments: [],
      cmtText: '',
      me: null
    };
  },
  async onLoad() {
    this.me = getUser();
    try {
      this.list = await api.videoFeed(60);
    } catch (e) {
      uni.showToast({ title: '视频加载失败，请检查后端是否启动', icon: 'none' });
    }
    // 首屏视频自动播放（H5 浏览器要求静音才能自动播放，模板已默认 muted）
    this.$nextTick(() => this.playCurrent());
  },
  methods: {
    fmt(n) {
      const num = Number(n || 0);
      if (num >= 10000) return (num / 10000).toFixed(1).replace('.0', '') + 'w';
      return String(num);
    },
    star(score) {
      const n = Math.round(Number(score || 0));
      return '★'.repeat(n > 5 ? 5 : n);
    },
    onChange(e) {
      this.index = e.detail.current;
      // 切换时只播当前屏，其余暂停并回到开头，省流量也避免后台出声
      this.playCurrent();
    },
    // 播放当前屏视频、暂停其它视频
    playCurrent() {
      this.list.forEach((item, i) => {
        if (!item.url) return;
        const ctx = uni.createVideoContext('v' + item.id, this);
        if (!ctx) return;
        if (i === this.index) {
          ctx.play();
        } else {
          ctx.pause();
          ctx.seek(0);
        }
      });
    },
    // 点赞 / 取消点赞（开关式）
    async doLike(item) {
      try {
        const liked = !!this.liked[item.id];
        const res = liked ? await api.videoLikeCancel(item.id) : await api.videoLike(item.id);
        item.likeCount = res.likeCount;
        this.$set ? this.$set(this.liked, item.id, !liked) : (this.liked[item.id] = !liked);
        this.$forceUpdate ? this.$forceUpdate() : null;
        uni.showToast({ title: liked ? '已取消点赞' : '已点赞', icon: 'none' });
      } catch (e) {
        /* 提示已在 request 里统一处理 */
      }
    },
    // 收藏 / 取消收藏（开关式）
    async doCollect(item) {
      try {
        const collected = !!this.collected[item.id];
        const res = collected ? await api.videoCollectCancel(item.id) : await api.videoCollect(item.id);
        item.collectCount = res.collectCount;
        this.$set ? this.$set(this.collected, item.id, !collected) : (this.collected[item.id] = !collected);
        this.$forceUpdate ? this.$forceUpdate() : null;
        uni.showToast({ title: collected ? '已取消收藏' : '已收藏', icon: 'none' });
      } catch (e) {
        /* 提示已在 request 里统一处理 */
      }
    },
    async share(item) {
      uni.showToast({ title: '分享：' + item.title, icon: 'none' });
      try {
        const res = await api.videoShare(item.id);
        item.shareCount = res.shareCount;
      } catch (e) {
        /* 分享统计失败不打扰用户 */
      }
    },
    goGoods(id) {
      uni.navigateTo({ url: '/pages/detail/detail?id=' + id });
    },
    async openComments(item) {
      this.cmtTarget = item.id;
      this.showCmt = true;
      try {
        this.comments = await api.commentList('video', item.id);
      } catch (e) {
        this.comments = [];
      }
    },
    canDelete(c) {
      if (!this.me) return false;
      return String(c.userId) === String(this.me.id) || this.me.admin === true;
    },
    async sendComment() {
      if (!this.me) {
        uni.showToast({ title: '请先到「我的」页面登录', icon: 'none' });
        return;
      }
      const text = (this.cmtText || '').trim();
      if (!text) {
        uni.showToast({ title: '说点什么吧～', icon: 'none' });
        return;
      }
      try {
        await api.commentAdd('video', this.cmtTarget, text, 5);
        this.cmtText = '';
        uni.showToast({ title: '评论成功', icon: 'none' });
        const item = this.list.find(v => String(v.id) === String(this.cmtTarget));
        if (item) item.commentCount = Number(item.commentCount || 0) + 1;
        this.comments = await api.commentList('video', this.cmtTarget);
      } catch (e) {
        /* 已提示 */
      }
    },
    async delComment(c) {
      try {
        await api.commentDelete(c.id);
        uni.showToast({ title: '已删除', icon: 'none' });
        this.comments = await api.commentList('video', this.cmtTarget);
        const item = this.list.find(v => String(v.id) === String(this.cmtTarget));
        if (item && item.commentCount > 0) item.commentCount = item.commentCount - 1;
      } catch (e) {
        /* 已提示 */
      }
    },
    async likeComment(c) {
      try {
        const res = await api.commentLike(c.id);
        c.likeCount = res.likeCount;
      } catch (e) {
        /* 已提示 */
      }
    }
  }
};
</script>

<style>
.feed { height: 100%; background: #000; }
page { height: 100%; background: #000; }
.swiper { height: 100%; }
.slide { position: relative; width: 100%; height: 100%; background: #0d1220; }
.video { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.fallback {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 100%; height: 100%;
}
.fallback-img { width: 70%; height: 500rpx; border-radius: 24rpx; animation: kb 7s ease-in-out infinite alternate; }
@keyframes kb { from { transform: scale(1); } to { transform: scale(1.08) translateY(-14rpx); } }
.fallback-tip { color: #bbbbbb; font-size: 22rpx; margin-top: 20rpx; }

.rail {
  position: absolute; right: 20rpx; bottom: 200rpx;
  display: flex; flex-direction: column; align-items: center;
}
.avatar { width: 88rpx; height: 88rpx; border-radius: 44rpx; border: 4rpx solid #fff; margin-bottom: 30rpx; }
.rail-btn { display: flex; flex-direction: column; align-items: center; margin-bottom: 28rpx; }
.ico { font-size: 52rpx; color: #fff; }
.ico.liked { color: #ff2d55; }
.n { font-size: 22rpx; color: #fff; }

.meta { position: absolute; left: 24rpx; bottom: 40rpx; width: 540rpx; }
.author { display: block; color: #fff; font-size: 30rpx; font-weight: bold; margin-bottom: 8rpx; }
.title { display: block; color: #fff; font-size: 28rpx; margin-bottom: 6rpx; }
.desc { display: block; color: #dddddd; font-size: 24rpx; margin-bottom: 8rpx; }
.tags { display: flex; flex-wrap: wrap; }
.tag { color: #ffd35c; font-size: 22rpx; margin-right: 12rpx; }
.goods {
  display: flex; align-items: center; margin-top: 16rpx;
  background: rgba(255, 255, 255, 0.16); border-radius: 16rpx; padding: 12rpx;
}
.gimg { width: 68rpx; height: 68rpx; border-radius: 12rpx; }
.gname { flex: 1; color: #fff; font-size: 24rpx; margin-left: 12rpx;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.gprice { color: #ffd35c; font-size: 26rpx; font-weight: bold; }
.go { color: #fff; font-size: 22rpx; background: #e1251b; border-radius: 20rpx;
  padding: 6rpx 14rpx; margin-left: 12rpx; }

.cmt-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5); z-index: 99;
  display: flex; flex-direction: column; justify-content: flex-end;
}
.cmt-panel { height: 72vh; background: #fff; border-radius: 24rpx 24rpx 0 0; display: flex; flex-direction: column; }
.cmt-head { display: flex; align-items: center; padding: 24rpx; border-bottom: 1rpx solid #eee; }
.cmt-title { flex: 1; font-size: 28rpx; font-weight: bold; color: #333; }
.cmt-close { font-size: 40rpx; color: #999; }
.cmt-list { flex: 1; }
.cmt { display: flex; padding: 20rpx 24rpx; }
.cavatar { width: 64rpx; height: 64rpx; border-radius: 32rpx; margin-right: 16rpx; }
.cbody { flex: 1; }
.crow { display: flex; align-items: center; }
.cname { color: #4a6fa5; font-size: 24rpx; }
.cstar { color: #ffb400; font-size: 22rpx; margin-left: 10rpx; }
.ctext { display: block; color: #333; font-size: 27rpx; margin: 6rpx 0; }
.cfoot { display: flex; align-items: center; }
.ctime { color: #aaa; font-size: 21rpx; margin-right: 20rpx; }
.clike { color: #888; font-size: 21rpx; margin-right: 20rpx; }
.cdel { color: #e1251b; font-size: 21rpx; }
.empty { color: #999; font-size: 26rpx; text-align: center; padding: 60rpx 0; }
.cmt-input { display: flex; align-items: center; padding: 20rpx 24rpx; border-top: 1rpx solid #eee; }
.cmt-input input { flex: 1; height: 70rpx; background: #f2f3f5; border-radius: 35rpx;
  padding: 0 24rpx; font-size: 26rpx; }
.send { background: #e1251b; color: #fff; margin-left: 16rpx; }
</style>
