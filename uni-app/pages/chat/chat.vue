<template>
  <view class="chat-page">
    <!-- 顶部状态 -->
    <view class="chat-head">
      <view class="avatar">🤖</view>
      <view class="info">
        <view class="name">京东 AI 客服</view>
        <view class="status" :style="'color:' + (online ? '#52c41a' : '#999')">
          {{ online ? (me ? '● 在线，可查你的订单' : '● 在线（登录后才能使用）') : '○ 连接中…' }}
        </view>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view class="chat-body" scroll-y :scroll-into-view="anchor" scroll-with-animation>
      <view class="bubble ai">
        你好{{ me && me.nickname ? '，' + me.nickname : '' }}！我是京东 AI 客服 🤖
        可以问我：订单到哪了、商品推荐、退货换货流程。也可以点下面的快捷提问～
        <template v-if="!me">
          你还没登录，登录之后才能和我聊天，去「我的」页登录一下就行。
        </template>
      </view>
      <view v-for="(m, i) in msgs" :key="i" :id="'msg' + i"
            :class="m.role === 'me' ? 'row-me' : 'row-ai'">
        <view class="bubble" :class="m.role">{{ m.text }}</view>
      </view>
      <view v-if="typing" class="row-ai">
        <view class="bubble ai typing">对方正在输入…</view>
      </view>
      <view style="height:20rpx"></view>
    </scroll-view>

    <!-- 快捷提问 -->
    <scroll-view class="quick" scroll-x>
      <view class="quick-inner">
        <text v-for="q in quicks" :key="q" class="chip" :class="{ off: typing }" @click="send(q)">{{ q }}</text>
      </view>
    </scroll-view>

    <!-- 输入区 -->
    <view class="chat-foot">
      <input class="ipt" v-model="draft" confirm-type="send" placeholder="输入你想问的…"
             @confirm="send()" />
      <!-- #ifdef H5 -->
      <button class="mic" size="mini" :class="{ rec: recording }" @click="voice()">
        {{ recording ? '● 听中' : '🎤 语音' }}
      </button>
      <!-- #endif -->
      <button class="send" size="mini" :disabled="typing" @click="send()">{{ typing ? '回复中…' : '发送' }}</button>
    </view>
  </view>
</template>

<script>
import { api, getUser } from '../../api/request.js';

export default {
  data() {
    return {
      me: null,
      online: false,
      draft: '',
      typing: false,
      sessionId: null,
      anchor: '',
      quicks: ['我的订单到哪了', '推荐一款手机', '退货怎么操作', '有什么新品'],
      msgs: [],
      recording: false,
      _rec: null
    };
  },
  onShow() {
    this.me = getUser();
    this.checkOnline();
  },
  methods: {
    async checkOnline() {
      try {
        const s = await api.aiStatus();
        this.online = !!(s && (s.aiOnline || s.online));
      } catch (e) {
        this.online = false;
      }
    },
    /**
     * 语音输入（🎤，仅 H5）：浏览器自带 Web Speech API 本地识别，
     * 说完自动填入并直接发送。小程序/App 里该按钮不渲染（条件编译）。
     */
    voice() {
      const W = typeof window !== 'undefined' ? window : {};
      const SR = W.SpeechRecognition || W.webkitSpeechRecognition;
      if (!SR) {
        uni.showToast({ title: '此浏览器不支持语音，请用 Chrome/Edge', icon: 'none' });
        return;
      }
      if (this._rec) { this._rec.stop(); return; }  // 正在录：再点=结束
      const rec = new SR();
      this._rec = rec;
      rec.lang = 'zh-CN';
      rec.interimResults = false;
      rec.maxAlternatives = 1;
      this.recording = true;
      uni.showToast({ title: '🎤 在听呢，请讲…', icon: 'none' });
      rec.onresult = (e) => {
        const text = (e.results[0][0].transcript || '').trim();
        if (text) this.send(text);
      };
      rec.onerror = (e) => {
        const tip = e.error === 'not-allowed' ? '麦克风权限被拒，请在浏览器里允许'
          : e.error === 'no-speech' ? '没听到你说话，再试一次' : '';
        if (tip) uni.showToast({ title: tip, icon: 'none' });
      };
      rec.onend = () => { this.recording = false; this._rec = null; };
      try { rec.start(); } catch (e) { this.recording = false; this._rec = null; }
    },
    async send(preset) {
      const text = (preset || this.draft || '').trim();
      if (!text) return;
      if (this.typing) { uni.showToast({ title: '客服正在回复，稍等一下~', icon: 'none' }); return; }
      // 必须登录才能聊（2026-10-17）：未登录直接拦下并引导去「我的」页登录
      if (!this.me) {
        uni.showToast({ title: '请先到「我的」页面登录', icon: 'none' });
        return;
      }
      this.draft = '';
      this.msgs.push({ role: 'me', text });
      this.typing = true;
      this.toBottom();
      try {
        const r = await api.aiChat(text, null, this.sessionId);
        if (r && r.sessionId) this.sessionId = r.sessionId;
        this.msgs.push({ role: 'ai', text: (r && r.reply) || '（客服没有返回内容）' });
      } catch (e) {
        this.msgs.push({ role: 'ai', text: '网络开小差了，请稍后再试 🙏' });
      } finally {
        this.typing = false;
        this.toBottom();
      }
    },
    toBottom() {
      this.$nextTick(() => {
        this.anchor = '';
        setTimeout(() => { this.anchor = 'msg' + (this.msgs.length - 1); }, 50);
      });
    }
  }
};
</script>

<style>
page { height: 100%; background: #f5f6f8; }
.chat-page { display: flex; flex-direction: column; height: 100vh; background: #f5f6f8; }
.chat-head {
  display: flex; align-items: center; background: #e1251b;
  padding: 24rpx 30rpx; flex: none;
}
.chat-head .avatar {
  width: 80rpx; height: 80rpx; border-radius: 40rpx; background: #ffffff33;
  display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 20rpx;
}
.chat-head .name { color: #fff; font-size: 32rpx; font-weight: bold; }
.chat-head .status { color: #eee; font-size: 22rpx; margin-top: 6rpx; }

.chat-body { flex: 1; padding: 24rpx; box-sizing: border-box; }
.row-me { display: flex; justify-content: flex-end; margin-bottom: 20rpx; }
.row-ai { display: flex; justify-content: flex-start; margin-bottom: 20rpx; }
.bubble {
  max-width: 70%; padding: 20rpx 26rpx; border-radius: 18rpx;
  font-size: 28rpx; line-height: 1.6; word-break: break-word; white-space: pre-wrap;
}
.bubble.me { background: #e1251b; color: #fff; border-top-right-radius: 4rpx; }
.bubble.ai { background: #ffffff; color: #333; border-top-left-radius: 4rpx; }
.bubble.typing { color: #999; }

.quick { flex: none; background: #f5f6f8; padding: 10rpx 0; white-space: nowrap; }
.quick-inner { display: flex; padding: 0 20rpx; }
.chip {
  flex: none; background: #fff; color: #e1251b; border: 1rpx solid #f3c1be;
  border-radius: 30rpx; padding: 10rpx 26rpx; font-size: 24rpx; margin-right: 16rpx;
}

.chat-foot {
  flex: none; display: flex; align-items: center;
  background: #fff; padding: 16rpx 20rpx;
  padding-bottom: calc(16rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.chat-foot .ipt {
  flex: 1; height: 72rpx; background: #f2f3f5; border-radius: 36rpx;
  padding: 0 28rpx; font-size: 28rpx;
}
.chat-foot .send {
  margin-left: 16rpx; background: #e1251b; color: #fff;
  border-radius: 36rpx; font-size: 26rpx; padding: 0 30rpx;
}
.chat-foot .send[disabled] { opacity: .55; }
.chip.off { opacity: .5; }
/* 🎤 语音按钮（H5） */
.chat-foot .mic {
  margin-left: 16rpx; background: #fff; color: #333;
  border: 1rpx solid #ddd; border-radius: 36rpx;
  font-size: 24rpx; padding: 0 24rpx; height: 72rpx;
  display: flex; align-items: center; justify-content: center;
  line-height: 1;
}
.chat-foot .mic.rec {
  background: #e1251b; color: #fff; border-color: #e1251b;
  animation: micPulse 1.1s ease-in-out infinite;
}
@keyframes micPulse { 0%,100% { transform: scale(1); } 50% { transform: scale(1.12); } }
</style>
