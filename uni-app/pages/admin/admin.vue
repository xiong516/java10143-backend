<template>
  <view class="wrap">
    <!-- 权限门 -->
    <view class="gate" v-if="!checked">
      <text class="gtext">正在校验权限…</text>
    </view>
    <view class="gate" v-else-if="!isAdmin">
      <text class="gico">🔒</text>
      <text class="gtitle">仅管理员可访问</text>
      <text class="gtext">请用管理员账号（admin）登录后再进。</text>
      <text class="gtext">普通账号即使打开本页，后端 /admin/** 也会返回 403。</text>
    </view>

    <block v-else>
      <!-- 标签栏 -->
      <scroll-view scroll-x class="tabs">
        <view class="tab" :class="{ on: tab === 'stats' }" @click="switchTab('stats')">看板</view>
        <view class="tab" :class="{ on: tab === 'goods' }" @click="switchTab('goods')">商品</view>
        <view class="tab" :class="{ on: tab === 'video' }" @click="switchTab('video')">短视频</view>
        <view class="tab" :class="{ on: tab === 'comment' }" @click="switchTab('comment')">评论</view>
        <view class="tab" :class="{ on: tab === 'user' }" @click="switchTab('user')">用户</view>
        <view class="tab" :class="{ on: tab === 'order' }" @click="switchTab('order')">订单</view>
      </scroll-view>

      <!-- 看板 -->
      <view v-if="tab === 'stats'" class="cards">
        <view class="stat"><text class="k">商品总数</text><text class="v">{{ stats.goodsCount || '-' }}</text></view>
        <view class="stat"><text class="k">SKU 规格</text><text class="v">{{ stats.skuCount || '-' }}</text></view>
        <view class="stat"><text class="k">注册用户</text><text class="v">{{ stats.userCount || '-' }}</text></view>
        <view class="stat"><text class="k">订单总数</text><text class="v">{{ stats.orderCount || '-' }}</text></view>
        <view class="stat"><text class="k">短视频</text><text class="v">{{ stats.videoCount || '-' }}</text></view>
        <view class="stat"><text class="k">评论</text><text class="v">{{ stats.commentCount || '-' }}</text></view>
      </view>

      <!-- 商品 -->
      <block v-if="tab === 'goods'">
        <view class="bar">
          <input v-model="keyword" placeholder="搜索商品名 / 编码" @confirm="loadGoods" />
          <button size="mini" class="grey" @click="loadGoods">搜索</button>
          <button size="mini" class="red" @click="openGoodsForm(null)">+ 新增</button>
        </view>
        <view class="pager">
          <text class="ptext">共 {{ goodsTotal }} 件 · 第 {{ goodsPage }} 页</text>
          <button size="mini" class="grey" @click="goodsPagePrev">上一页</button>
          <button size="mini" class="grey" @click="goodsPageNext">下一页</button>
        </view>
        <view class="item" v-for="g in goodsRows" :key="g.id">
          <image class="thumb" :src="g.picture" mode="aspectFill" />
          <view class="ibody">
            <text class="iname">{{ g.name }}</text>
            <text class="iprice">￥{{ g.price }} · {{ g.spuCode }}</text>
            <text class="isub">销量 {{ g.salesCount }} · 评论 {{ g.commentCount }}</text>
          </view>
          <view class="iacts">
            <button size="mini" class="blue" @click="openGoodsForm(g)">编辑</button>
            <button size="mini" class="red-line" @click="delGoods(g)">删除</button>
          </view>
        </view>
      </block>

      <!-- 短视频 -->
      <block v-if="tab === 'video'">
        <view class="bar">
          <text class="bartitle">短视频管理</text>
          <button size="mini" class="red" @click="aiOpen = !aiOpen">🤖 AI 生成</button>
          <button size="mini" class="red" @click="openVideoForm(null)">+ 新增</button>
        </view>
        <view class="ai-panel" v-if="aiOpen">
          <input class="ai-input" v-model="aiKeyword" placeholder="商品关键词，如：耳机 / 跑鞋 / 口红" />
          <view class="chip-row">
            <text class="chip-label">风格</text>
            <text v-for="s in aiStyles" :key="s.v" class="chip" :class="{ on: aiStyle === s.v }" @click="aiStyle = s.v">{{ s.t }}</text>
          </view>
          <view class="chip-row">
            <text class="chip-label">时长</text>
            <text v-for="d in [8, 10, 15]" :key="d" class="chip" :class="{ on: aiDuration === d }" @click="aiDuration = d">{{ d }} 秒</text>
          </view>
          <view class="caps">
            <text class="cap" v-for="(c, i) in aiCapsPreview" :key="i">{{ c }}</text>
          </view>
          <button size="mini" class="red" :disabled="aiBusy" @click="aiVideoGenerate">{{ aiBusy ? '合成中…' : '开始生成' }}</button>
          <text class="ai-tip">竖屏 720×1280 · 运镜 + 烧录字幕 · 约 15-60 秒 · 自动上架信息流</text>
        </view>
        <view class="item" v-for="v in videos" :key="v.id">
          <image class="thumb" :src="v.cover" mode="aspectFill" />
          <view class="ibody">
            <text class="iname">{{ v.title }}</text>
            <text class="isub">{{ v.url ? '真实 mp4 · ' + (v.duration || '?') + 's' : '图片合成兜底' }} · @{{ v.authorName }}</text>
            <text class="isub">赞 {{ v.likeCount }} · 评论 {{ v.commentCount }}</text>
          </view>
          <view class="iacts">
            <button size="mini" class="blue" @click="openVideoForm(v)">编辑</button>
            <button size="mini" class="red-line" @click="delVideo(v)">删除</button>
          </view>
        </view>
      </block>

      <!-- 评论 -->
      <block v-if="tab === 'comment'">
        <view class="bar">
          <input v-model="keyword" placeholder="搜索评论内容" @confirm="loadComments" />
          <button size="mini" class="grey" @click="loadComments">搜索</button>
        </view>
        <view class="item" v-for="c in comments" :key="c.id">
          <image class="thumb" :src="c.avatar" mode="aspectFill" />
          <view class="ibody">
            <text class="iname">{{ c.nickname }}</text>
            <text class="isub">{{ c.targetType === 'video' ? '视频' : '商品' }} {{ c.targetId }} · 评分 {{ c.score }}</text>
            <text class="itext">{{ c.content }}</text>
          </view>
          <view class="iacts">
            <button size="mini" class="red-line" @click="delComment(c)">删除</button>
          </view>
        </view>
      </block>

      <!-- 用户 -->
      <block v-if="tab === 'user'">
        <view class="bar">
          <input v-model="keyword" placeholder="搜索账号 / 昵称" @confirm="loadUsers" />
          <button size="mini" class="grey" @click="loadUsers">搜索</button>
          <button size="mini" class="red" @click="openUserForm(null)">+ 新增</button>
        </view>
        <view class="item" v-for="u in users" :key="u.id">
          <image class="thumb" :src="u.avatar" mode="aspectFill" />
          <view class="ibody">
            <text class="iname">{{ u.nickname }}</text>
            <text class="isub">{{ u.account }} · {{ u.mobile }}</text>
            <text class="isub">{{ u.role === 'admin' ? '管理员' : '普通用户' }} · {{ u.createTime }}</text>
          </view>
          <view class="iacts">
            <button size="mini" class="blue" @click="openUserForm(u)">编辑</button>
            <button size="mini" class="red-line" @click="delUser(u)">删除</button>
          </view>
        </view>
      </block>

      <!-- 订单（只读） -->
      <block v-if="tab === 'order'">
        <view class="item" v-for="o in orders" :key="o.id">
          <view class="ibody">
            <text class="iname">订单号 {{ o.id }}</text>
            <text class="iprice">实付 ￥{{ o.payMoney }} · {{ o.totalNum }} 件</text>
            <text class="isub">{{ stateText(o.orderState) }} · {{ o.receiver }} {{ o.receiverContact }}</text>
            <text class="isub">{{ o.createTime }}</text>
          </view>
        </view>
      </block>
    </block>

    <!-- 表单弹层（商品 / 视频 / 用户 共用） -->
    <view class="form-mask" v-if="formType" @click="formType = ''">
      <view class="form-panel" @click.stop>
        <text class="form-title">{{ formTitle }}</text>

        <block v-if="formType === 'goods'">
          <input v-model="form.name" placeholder="商品名称 *" />
          <input v-model="form.price" placeholder="现价，如 199.00" />
          <input v-model="form.oldPrice" placeholder="原价" />
          <input v-model="form.inventory" placeholder="库存" />
          <input v-model="form.spuCode" placeholder="商品编码" />
          <input v-model="form.description" placeholder="商品描述" />
          <input v-model="form.mainPictures" placeholder="主图地址（留空自动生成）" />
        </block>

        <block v-if="formType === 'video'">
          <input v-model="form.title" placeholder="标题 *" />
          <input v-model="form.description" placeholder="描述" />
          <input v-model="form.authorName" placeholder="作者昵称" />
          <input v-model="form.tags" placeholder="话题标签，逗号分隔" />
          <input v-model="form.spuId" placeholder="关联商品 ID" />
          <input v-model="form.url" placeholder="视频地址，留空用封面兜底" />
          <input v-model="form.cover" placeholder="封面地址" />
          <input v-model="form.sort" placeholder="排序（越小越前）" />
        </block>

        <block v-if="formType === 'user'">
          <input v-model="form.account" placeholder="账号 *" :disabled="!!editingId" />
          <input v-model="form.nickname" placeholder="昵称" />
          <input v-model="form.mobile" placeholder="手机号" />
          <input v-model="form.password" password placeholder="密码（不填默认 123456 / 编辑时空着不改）" />
          <view class="role-row">
            <text class="role-label">角色：</text>
            <text class="role-opt" :class="{ on: form.role !== 'admin' }" @click="form.role = 'user'">普通用户</text>
            <text class="role-opt" :class="{ on: form.role === 'admin' }" @click="form.role = 'admin'">管理员</text>
          </view>
        </block>

        <view class="form-btns">
          <button class="grey" @click="formType = ''">取消</button>
          <button class="red" @click="saveForm">保存</button>
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
      checked: false,
      isAdmin: false,
      tab: 'stats',
      stats: {},
      keyword: '',
      goodsRows: [],
      goodsTotal: '0',
      goodsPage: 1,
      videos: [],
      comments: [],
      users: [],
      orders: [],
      formType: '',
      formTitle: '',
      editingId: '',
      form: {},
      aiOpen: false,
      aiBusy: false,
      aiKeyword: '',
      aiStyle: 'seeds',
      aiDuration: 10,
      aiStyles: [
        { v: 'seeds', t: '🌿 种草慢推' },
        { v: 'unbox', t: '📦 开箱横移' },
        { v: 'flash', t: '⚡ 快闪节奏' },
        { v: 'fresh', t: '🌤 清新淡出' }
      ],
      aiCapTpl: {
        seeds: ['{kw}｜真的会回购', '细节好评 直接种草', '无脑冲 不踩雷'],
        unbox: ['开箱！{kw} 到手', '第一眼就爱住', '这个质感真的绝'],
        flash: ['{kw} 三连拍', '闭眼入', '手慢无'],
        fresh: ['今日份 {kw}', '生活需要一点小美好', '慢下来 看看它']
      }
    };
  },
  computed: {
    /** 字幕预览：与 ai-service/server.py 的 GEN_CAPTIONS 保持一致 */
    aiCapsPreview() {
      const kw = (this.aiKeyword || '').trim() || '好物';
      return (this.aiCapTpl[this.aiStyle] || []).map(t => t.split('{kw}').join(kw));
    }
  },
  async onLoad() {
    const me = getUser();
    if (me && me.admin === true) {
      this.isAdmin = true;
      this.checked = true;
      this.loadStats();
      return;
    }
    // 本地没有管理员标记，去后端再确认一次（token 有效才行）
    try {
      const p = await api.profile();
      this.isAdmin = p.admin === true;
      if (p && p.admin) {
        const u = getUser() || {};
        u.id = p.id;
        u.nickname = p.nickname;
        u.admin = true;
        uni.setStorageSync('j10143_user', u);
      }
    } catch (e) {
      this.isAdmin = false;
    }
    this.checked = true;
    if (this.isAdmin) this.loadStats();
  },
  methods: {
    stateText(s) {
      const map = { 1: '待付款', 2: '待发货', 3: '待收货', 4: '待评价', 5: '已完成', 6: '已取消' };
      return map[s] || '处理中';
    },
    switchTab(t) {
      this.tab = t;
      this.keyword = '';
      if (t === 'stats') this.loadStats();
      else if (t === 'goods') this.loadGoods();
      else if (t === 'video') this.loadVideos();
      else if (t === 'comment') this.loadComments();
      else if (t === 'user') this.loadUsers();
      else if (t === 'order') this.loadOrders();
    },

    async loadStats() {
      try { this.stats = await api.adminStats(); } catch (e) { /* 已提示 */ }
    },
    async loadGoods() {
      try {
        const res = await api.adminGoods(this.keyword, this.goodsPage, 10);
        this.goodsRows = res.rows || [];
        this.goodsTotal = res.total;
      } catch (e) { /* 已提示 */ }
    },
    goodsPagePrev() {
      if (this.goodsPage > 1) { this.goodsPage--; this.loadGoods(); }
    },
    goodsPageNext() {
      this.goodsPage++;
      this.loadGoods();
    },
    async loadVideos() {
      try { this.videos = await api.adminVideoList(); } catch (e) { /* 已提示 */ }
    },
    async loadComments() {
      try { this.comments = await api.adminCommentList('', this.keyword); } catch (e) { /* 已提示 */ }
    },
    async loadUsers() {
      try { this.users = await api.adminUserList(this.keyword); } catch (e) { /* 已提示 */ }
    },
    async loadOrders() {
      try { this.orders = await api.adminOrderList(); } catch (e) { /* 已提示 */ }
    },

    openGoodsForm(g) {
      this.formType = 'goods';
      this.editingId = g ? g.id : '';
      this.formTitle = g ? ('编辑商品 #' + g.id) : '新增商品';
      this.form = {
        name: g ? g.name : '',
        price: g ? g.price : '',
        oldPrice: g ? g.oldPrice : '',
        inventory: g ? String(g.inventory === undefined ? 100 : g.inventory) : '100',
        spuCode: g ? (g.spuCode || '') : '',
        description: g ? (g.desc || '') : '',
        mainPictures: ''
      };
    },
    /**
     * AI 生成短视频：面板里选关键词/风格/时长 → 后端选商品 → Python 按风格运镜+烧字幕合成。
     * 耗时 15~90 秒，期间 loading + 按钮禁用，完成后刷新列表。
     */
    async aiVideoGenerate() {
      const that = this;
      const kw = (this.aiKeyword || '').trim();
      if (!kw) {
        uni.showToast({ title: '请输入关键词', icon: 'none' });
        return;
      }
      if (this.aiBusy) return;
      this.aiBusy = true;
      uni.showLoading({ title: 'AI 合成中…约 1 分钟', mask: true });
      api.adminVideoGenerate({ keyword: kw, style: this.aiStyle, duration: this.aiDuration }).then(() => {
        uni.hideLoading();
        that.aiBusy = false;
        uni.showToast({ title: '生成完成，已加入列表', icon: 'success' });
        that.loadVideos();
      }).catch(e => {
        uni.hideLoading();
        that.aiBusy = false;
        uni.showToast({ title: '生成失败：' + (e.errMsg || e.message || '请确认 8010 AI 服务已启动'), icon: 'none' });
      });
    },
    openVideoForm(v) {
      this.formType = 'video';
      this.editingId = v ? v.id : '';
      this.formTitle = v ? ('编辑短视频 #' + v.id) : '新增短视频';
      this.form = {
        title: v ? v.title : '',
        description: v ? v.description : '',
        authorName: v ? v.authorName : '',
        tags: v && v.tags ? v.tags.join(',') : '',
        spuId: v ? v.spuId : '',
        url: v ? v.url : '',
        cover: v ? v.cover : '',
        sort: '99'
      };
    },
    openUserForm(u) {
      this.formType = 'user';
      this.editingId = u ? u.id : '';
      this.formTitle = u ? ('编辑用户 #' + u.id) : '新增用户';
      this.form = {
        account: u ? u.account : '',
        nickname: u ? u.nickname : '',
        mobile: u ? u.mobile : '',
        password: '',
        role: u ? (u.role === 'admin' ? 'admin' : 'user') : 'user'
      };
    },

    async saveForm() {
      try {
        if (this.formType === 'goods') {
          if (!this.form.name) return uni.showToast({ title: '商品名称不能为空', icon: 'none' });
          if (this.editingId) await api.adminGoodsUpdate(this.editingId, this.form);
          else await api.adminGoodsAdd(this.form);
          this.loadGoods();
        } else if (this.formType === 'video') {
          if (!this.form.title) return uni.showToast({ title: '标题不能为空', icon: 'none' });
          if (this.editingId) await api.adminVideoUpdate(this.editingId, this.form);
          else await api.adminVideoAdd(this.form);
          this.loadVideos();
        } else if (this.formType === 'user') {
          if (!this.editingId && !this.form.account) return uni.showToast({ title: '账号不能为空', icon: 'none' });
          if (this.editingId) await api.adminUserUpdate(this.editingId, this.form);
          else await api.adminUserAdd(this.form);
          this.loadUsers();
        }
        this.formType = '';
        uni.showToast({ title: '保存成功', icon: 'none' });
      } catch (e) { /* 已提示 */ }
    },

    delGoods(g) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除商品「' + g.name + '」吗？',
        success: async (r) => {
          if (!r.confirm) return;
          try { await api.adminGoodsDelete(g.id); this.loadGoods(); } catch (e) { /* 已提示 */ }
        }
      });
    },
    delVideo(v) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除短视频「' + v.title + '」吗？',
        success: async (r) => {
          if (!r.confirm) return;
          try { await api.adminVideoDelete(v.id); this.loadVideos(); } catch (e) { /* 已提示 */ }
        }
      });
    },
    delComment(c) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除这条评论吗？',
        success: async (r) => {
          if (!r.confirm) return;
          try { await api.adminCommentDelete(c.id); this.loadComments(); } catch (e) { /* 已提示 */ }
        }
      });
    },
    delUser(u) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除用户「' + u.account + '」吗？',
        success: async (r) => {
          if (!r.confirm) return;
          try { await api.adminUserDelete(u.id); this.loadUsers(); } catch (e) { /* 已提示 */ }
        }
      });
    }
  }
};
</script>

<style>
.wrap { padding: 16rpx; background: #f4f5f7; min-height: 100vh; }
.gate { display: flex; flex-direction: column; align-items: center; padding: 160rpx 40rpx; }
.gico { font-size: 80rpx; }
.gtitle { font-size: 34rpx; font-weight: bold; margin: 20rpx 0; }
.gtext { font-size: 25rpx; color: #888; text-align: center; line-height: 1.7; }

.tabs { white-space: nowrap; margin-bottom: 16rpx; }
.tab {
  display: inline-block; padding: 12rpx 28rpx; margin-right: 12rpx; font-size: 26rpx;
  color: #333; background: #fff; border-radius: 30rpx;
}
.tab.on { background: #e1251b; color: #fff; }

.cards { display: flex; flex-wrap: wrap; justify-content: space-between; }
.stat {
  width: 48%; background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx;
  display: flex; flex-direction: column;
}
.stat .k { font-size: 24rpx; color: #888; }
.stat .v { font-size: 44rpx; color: #e1251b; font-weight: bold; margin-top: 8rpx; }

.bar { display: flex; align-items: center; margin-bottom: 14rpx; }
.bar input { flex: 1; height: 68rpx; background: #fff; border-radius: 34rpx; padding: 0 24rpx; font-size: 26rpx; }
.bartitle { flex: 1; font-size: 28rpx; font-weight: bold; }

/* ---- AI 生成面板 ---- */
.ai-panel { background: #fff; border-radius: 16rpx; padding: 20rpx; margin-bottom: 16rpx; }
.ai-input { height: 68rpx; background: #f5f5f5; border-radius: 34rpx; padding: 0 24rpx; font-size: 26rpx; margin-bottom: 14rpx; }
.chip-row { display: flex; align-items: center; flex-wrap: wrap; margin-bottom: 12rpx; }
.chip-label { font-size: 24rpx; color: #999; width: 72rpx; }
.chip { font-size: 23rpx; color: #666; background: #f5f5f5; border-radius: 26rpx; padding: 8rpx 22rpx; margin: 0 10rpx 8rpx 0; border: 1rpx solid #eee; }
.chip.on { color: #fff; background: #e1251b; border-color: #e1251b; }
.caps { display: flex; flex-wrap: wrap; margin-bottom: 14rpx; }
.cap { font-size: 22rpx; color: #c0392b; background: #fff1f0; border: 1rpx solid #ffd5d2; border-radius: 8rpx; padding: 6rpx 14rpx; margin: 0 10rpx 8rpx 0; }
.ai-tip { display: block; font-size: 22rpx; color: #999; margin-top: 12rpx; line-height: 1.6; }
.pager { display: flex; align-items: center; margin-bottom: 14rpx; }
.ptext { flex: 1; font-size: 24rpx; color: #888; }

.item {
  display: flex; align-items: center; background: #fff; border-radius: 16rpx;
  padding: 16rpx; margin-bottom: 14rpx;
}
.thumb { width: 96rpx; height: 96rpx; border-radius: 12rpx; margin-right: 16rpx; }
.ibody { flex: 1; display: flex; flex-direction: column; }
.iname { font-size: 27rpx; font-weight: 500; }
.iprice { font-size: 25rpx; color: #e1251b; margin-top: 4rpx; }
.isub { font-size: 22rpx; color: #999; margin-top: 2rpx; }
.itext { font-size: 25rpx; color: #444; margin-top: 4rpx; }
.iacts { display: flex; flex-direction: column; }

button.grey { background: #f0f0f0; color: #333; margin-left: 10rpx; }
button.red { background: #e1251b; color: #fff; margin-left: 10rpx; }
button.blue { background: #eef3ff; color: #3b6fd4; margin-bottom: 8rpx; }
button.red-line { background: #ffeeed; color: #e1251b; }

.form-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.45); z-index: 99;
  display: flex; flex-direction: column; justify-content: flex-end;
}
.form-panel { background: #fff; border-radius: 24rpx 24rpx 0 0; padding: 28rpx; max-height: 86vh; overflow-y: auto; }
.form-title { display: block; font-size: 30rpx; font-weight: bold; margin-bottom: 18rpx; }
.form-panel input {
  height: 72rpx; background: #f5f6f8; border-radius: 12rpx; padding: 0 20rpx;
  font-size: 26rpx; margin-bottom: 14rpx;
}
.form-btns { display: flex; margin-top: 10rpx; }
.form-btns button { flex: 1; }
.role-row { display: flex; align-items: center; margin: 6rpx 0 18rpx; }
.role-label { font-size: 26rpx; color: #666; }
.role-opt {
  font-size: 25rpx; padding: 8rpx 24rpx; border-radius: 26rpx;
  background: #f0f0f0; color: #333; margin-right: 14rpx;
}
.role-opt.on { background: #e1251b; color: #fff; }
</style>
