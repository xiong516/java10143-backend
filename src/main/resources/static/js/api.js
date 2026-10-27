/**
 * Java10143 接口封装
 * WEB 端和 HBuilderX 里的 H5 用的是同一套后端接口
 */
const API = {
  // 同源部署时留空即可；如果要把网页单独放别处，改成 'http://localhost:8080'
  base: '',

  token() {
    return localStorage.getItem('j10143_token') || '';
  },

  user() {
    try { return JSON.parse(localStorage.getItem('j10143_user') || 'null'); } catch (e) { return null; }
  },

  setLogin(user) {
    localStorage.setItem('j10143_token', user.token);
    localStorage.setItem('j10143_user', JSON.stringify(user));
  },

  logout() {
    localStorage.removeItem('j10143_token');
    localStorage.removeItem('j10143_user');
  },

  async request(path, options = {}) {
    const { method = 'GET', body, auth = true } = options;
    const headers = { 'Content-Type': 'application/json' };
    if (auth && API.token()) headers['Authorization'] = 'Bearer ' + API.token();

    const res = await fetch(API.base + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    });
    let json;
    try {
      json = await res.json();
    } catch (e) {
      throw new Error('服务器没有返回数据，请确认 Java10143 后端已启动');
    }
    if (json.code !== '1') throw new Error(json.msg || '请求失败');
    return json.result;
  },

  // ---------- 首页 ----------
  banner: (site = 1) => API.request(`/home/banner?distributionSite=${site}`, { auth: false }),
  newGoods: (limit = 8) => API.request(`/home/new?limit=${limit}`, { auth: false }),
  hot: (limit = 8) => API.request(`/home/hot?limit=${limit}`, { auth: false }),
  categoryHead: () => API.request('/home/category/head', { auth: false }),

  // ---------- 分类 / 商品 ----------
  category: (id) => API.request(`/category?id=${id}`, { auth: false }),
  goods: (id) => API.request(`/goods?id=${id}`, { auth: false }),

  // ---------- 登录 ----------
  login: (account, password) => API.request('/login', { method: 'POST', body: { account, password }, auth: false }),

  // ---------- 购物车 ----------
  cartList: () => API.request('/member/cart'),
  cartAdd: (skuId, count) => API.request('/member/cart', { method: 'POST', body: { skuId: String(skuId), count } }),
  cartUpdate: (skuId, data) => API.request(`/member/cart/${skuId}`, { method: 'PUT', body: data }),
  cartDelete: (ids) => API.request('/member/cart', { method: 'DELETE', body: { ids: ids.map(String) } }),

  // ---------- 地址 ----------
  addressList: () => API.request('/member/address'),
  addressAdd: (addr) => API.request('/member/address', { method: 'POST', body: addr }),

  // ---------- 订单 ----------
  orderPre: (skuIds) => API.request('/member/order/pre?skuIds=' + skuIds.join(',')),
  orderSubmit: (body) => API.request('/member/order', { method: 'POST', body }),
  orderDetail: (id) => API.request(`/member/order/${id}`),
  orderList: (state) => API.request('/member/order' + (state ? `?orderState=${state}` : '')),

  // ---------- 支付 ----------
  pay: (orderId) => API.request(`/pay/aliPay?orderId=${orderId}`),

  // ---------- AI 智能客服 ----------
  // message：用户说的话；orderId：想关联的订单（不传后端自动取最近一单）
  aiChat: (message, orderId, sessionId) => API.request('/member/ai/chat', {
    method: 'POST',
    body: { message, orderId: orderId || null, sessionId: sessionId || null }
  }),
  aiStatus: () => API.request('/member/ai/status'),

  // ---------- 短视频（抖音式信息流） ----------
  videoFeed: (limit = 10) => API.request(`/home/video?limit=${limit}`, { auth: false }),
  videoLike: (id) => API.request(`/home/video/${id}/like`, { auth: false }),
  videoLikeCancel: (id) => API.request(`/home/video/${id}/like/cancel`, { auth: false }),
  videoCollect: (id) => API.request(`/home/video/${id}/collect`, { auth: false }),
  videoCollectCancel: (id) => API.request(`/home/video/${id}/collect/cancel`, { auth: false }),
  videoShare: (id) => API.request(`/home/video/${id}/share`, { auth: false }),

  // ---------- 评论（商品 / 视频通用，targetType = goods | video） ----------
  commentList: (targetType, targetId, limit = 50) =>
    API.request(`/comment/list?targetType=${targetType}&targetId=${targetId}&limit=${limit}`, { auth: false }),
  commentAdd: (targetType, targetId, content, score) =>
    API.request('/member/comment', { method: 'POST', body: { targetType, targetId: String(targetId), content, score: score || 5 } }),
  commentDelete: (id) => API.request(`/member/comment/${id}`, { method: 'DELETE' }),
  commentMine: () => API.request('/member/comment/mine'),
  commentLike: (id) => API.request(`/comment/${id}/like`, { method: 'POST' }),

  // ---------- 管理后台（仅 admin 账号可用） ----------
  /* ---------- 短视频带货（内容电商闭环） ---------- */
  videoGoods: (videoId) => API.request(`/home/video/${videoId}/goods`, { auth: false }),

  /* ---------- 优惠券 ---------- */
  couponCenter: () => API.request('/coupon/list', { auth: false }),
  couponTake: (id) => API.request(`/member/coupon/${id}/take`, { method: 'POST' }),
  couponMine: (status) => API.request('/member/coupon' + (status === undefined || status === null ? '' : `?status=${status}`)),
  couponPreview: (amount, spuIds) =>
    API.request(`/member/coupon/preview?amount=${amount || 0}&spuIds=${(spuIds || []).join(',')}`),

  /* ---------- 秒杀 ---------- */
  seckillList: () => API.request('/seckill/list', { auth: false }),
  seckillDetail: (id) => API.request(`/seckill/${id}`, { auth: false }),
  seckillBuy: (body) => API.request('/member/seckill/buy', { method: 'POST', body }),

  /* ---------- 价格走势 ---------- */
  priceHistory: (spuId, days = 90) =>
    API.request(`/goods/${spuId}/price-history?days=${days}`, { auth: false }),

  /* ---------- AI 推荐（个性化） ---------- */
  recommend: (limit = 8) => API.request(`/home/recommend?limit=${limit}`, { auth: false }),

  /* ---------- 直播带货 ---------- */
  liveList: () => API.request('/live/list', { auth: false }),
  liveDetail: (id) => API.request(`/live/${id}`, { auth: false }),
  liveMessages: (id, afterId) => API.request(`/live/${id}/messages?afterId=${afterId || 0}`, { auth: false }),
  liveJoin: (id) => API.request(`/member/live/${id}/join`, { method: 'POST' }),
  liveLike: (id) => API.request(`/member/live/${id}/like`, { method: 'POST' }),
  liveSend: (id, content) => API.request(`/member/live/${id}/message`, { method: 'POST', body: { content } }),

  admin: {
    stats: () => API.request('/admin/stats'),
    dashboard: (days = 14) => API.request(`/admin/dashboard?days=${days}`),
    priceSnapshot: () => API.request('/admin/price/snapshot', { method: 'POST' }),
    categories: () => API.request('/admin/categories'),

    goodsList: (keyword = '', page = 1, size = 10) =>
      API.request(`/admin/goods?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`),
    goodsDetail: (id) => API.request(`/admin/goods/${id}`),
    goodsCreate: (body) => API.request('/admin/goods', { method: 'POST', body }),
    goodsUpdate: (id, body) => API.request(`/admin/goods/${id}`, { method: 'PUT', body }),
    goodsDelete: (id) => API.request(`/admin/goods/${id}`, { method: 'DELETE' }),

    videoList: () => API.request('/admin/video'),
    videoCreate: (body) => API.request('/admin/video', { method: 'POST', body }),
    videoUpdate: (id, body) => API.request(`/admin/video/${id}`, { method: 'PUT', body }),
    videoDelete: (id) => API.request(`/admin/video/${id}`, { method: 'DELETE' }),
    // 兼容旧用法（字符串关键词）与新用法（对象 {keyword, spuId, style, duration}）
    videoGenerate: (kwOrPayload) => API.request('/admin/video/generate', {
      method: 'POST',
      body: typeof kwOrPayload === 'string' ? { keyword: kwOrPayload } : kwOrPayload
    }),

    commentList: (targetType = '', keyword = '') =>
      API.request(`/admin/comment?targetType=${targetType}&keyword=${encodeURIComponent(keyword)}`),
    commentUpdate: (id, body) => API.request(`/admin/comment/${id}`, { method: 'PUT', body }),
    commentDelete: (id) => API.request(`/admin/comment/${id}`, { method: 'DELETE' }),

    userList: (keyword = '') => API.request(`/admin/user?keyword=${encodeURIComponent(keyword)}`),
    userCreate: (body) => API.request('/admin/user', { method: 'POST', body }),
    userUpdate: (id, body) => API.request(`/admin/user/${id}`, { method: 'PUT', body }),
    userDelete: (id) => API.request(`/admin/user/${id}`, { method: 'DELETE' }),

    orderList: () => API.request('/admin/order')
  }
};
