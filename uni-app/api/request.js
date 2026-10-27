/**
 * uni-app 统一请求封装（H5 / App / 微信小程序 通用）
 *
 * 【地址策略：谁走得通就用谁】候选列表从上往下试，第一个连不上自动换下一个：
 *   1) H5 打包后（后端/隧道托管）→ 同源优先（用什么域名打开页面，接口就走什么域名）
 *   2) H5 本地开发(5173) / 微信小程序 / App → 局域网 IP 优先，连不上自动切隧道
 *
 * ★ 所以 .env 平时留空即可！隧道地址变了只需要改下面 TUNNEL_BASE 这一行。
 */
const ENV_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');
/** 局域网候选：以当前实际在用的 WLAN 地址（192.168.100.200）打头，失败再自动降级。
 *  手机 App 基座没有"同源"概念，第一位的地址必须填电脑当前网卡 IP，否则手机端
 *  每个请求都要先等 6 秒超时才切到下一个（2026-10-23 实测：.199 已失效） */
const LAN_BASES = [
  'http://192.168.100.200:8080',   // WLAN 当前在用（放第一位）
  'http://192.168.103.21:8080',    // 有线网卡（Ethernet）
  'http://192.168.100.199:8080'    // WLAN 旧网段，已失效，仅兜底
];
const LAN_BASE = LAN_BASES[0];
// const TUNNEL_BASE = 'https://6d21a756.r8.cpolar.top';
const TUNNEL_BASE = 'https://6d21a756.r8.cpolar.top';
// 隧道地址见上方 TUNNEL_BASE，改地址只动那一行即可
// 旧隧道备用：https://6d21a756.r8.cpolar.top


function uniq(arr) {
  const out = [];
  for (let i = 0; i < arr.length; i++) {
    if (out.indexOf(arr[i]) < 0) out.push(arr[i]);
  }
  return out;
}

let first;
// #ifdef H5
// H5：打包后同源；本地开发(5173)回退局域网
first = (import.meta.env.DEV === true) ? LAN_BASE : '';
// #endif
// #ifndef H5
// 小程序 / App：没有同源概念，局域网优先（.env 里显式配了就用 .env 的）
first = ENV_BASE || LAN_BASE;
// #endif

let BASES = uniq([first, ENV_BASE].concat(LAN_BASES).concat([TUNNEL_BASE]));
if (first !== '') {
  // 非同源场景里不该出现空串
  BASES = BASES.filter(function (b) { return b !== ''; });
}
export const BASES_LIST = BASES;
export const BASE_URL = BASES[0];

const TOKEN_KEY = 'j10143_token';
const USER_KEY = 'j10143_user';

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || '';
}

export function getUser() {
  return uni.getStorageSync(USER_KEY) || null;
}

export function setLogin(user) {
  uni.setStorageSync(TOKEN_KEY, user.token);
  uni.setStorageSync(USER_KEY, user);
}

export function logout() {
  uni.removeStorageSync(TOKEN_KEY);
  uni.removeStorageSync(USER_KEY);
}

/** 单次请求（指定地址），连通性失败会 reject，业务失败也会 reject 但已弹 toast。
 *  超时按地址分档：cpolar 免费隧道慢（实测 10 秒上下）给 20 秒，局域网 6 秒足够 */
function timeoutFor(base) {
  return (base.indexOf('cpolar') >= 0 || base.indexOf('https://') === 0) ? 20000 : 6000;
}

/** 最近一次请求成功的后端地址（webview 打开活动页时用这个，保证是"能连通的"） */
let LAST_OK_BASE = '';
export function getOkBase() {
  return LAST_OK_BASE || '';
}

function requestOnce(path, options, base) {
  const { method = 'GET', data, auth = true } = options;
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' };
    if (auth && getToken()) header['Authorization'] = 'Bearer ' + getToken();

    uni.request({
      url: base + path,
      method,
      data,
      header,
      timeout: timeoutFor(base),
      success: (res) => {
        const body = res.data;
        if (!body) return reject(new Error('后端没有返回数据'));
        // 服务端有应答（哪怕业务报错）说明地址是通的，不再换下一个
        LAST_OK_BASE = base;
        if (body.code !== '1') {
          uni.showToast({ title: body.msg || '请求失败', icon: 'none' });
          return reject(new Error(body.msg || '请求失败'));
        }
        resolve(body.result);
      },
      fail: (err) => reject(err)
    });
  });
}

/**
 * 统一请求：按 BASES 顺序尝试，连不上自动换下一个地址
 */
export function request(path, options = {}) {
  const { loading = false } = options;
  return new Promise((resolve, reject) => {
    let settled = false;
    const run = (idx) => {
      const base = BASES[idx];
      if (idx === 0 && loading) uni.showLoading({ title: '加载中', mask: true });
      console.info('[Java10143] ' + (options.method || 'GET') + ' ' + base + path);
      requestOnce(path, options, base).then((result) => {
        if (loading) uni.hideLoading();
        if (!settled) { settled = true; resolve(result); }
      }).catch((err) => {
        console.warn('[Java10143] ' + base + ' 连不上' +
          (idx + 1 < BASES.length ? ('，自动改用 ' + BASES[idx + 1]) : '，所有地址都不通'));
        if (idx + 1 < BASES.length) {
          run(idx + 1);
          return;
        }
        if (loading) uni.hideLoading();
        if (!settled) {
          settled = true;
          uni.showToast({ title: '连不上后端，请确认电脑上的服务已启动', icon: 'none', duration: 3500 });
          reject(err);
        }
      });
    };
    run(0);
  });
}

/* ---------------- 接口清单 ---------------- */
export const api = {
  banner: (site = 1) => request(`/home/banner?distributionSite=${site}`, { auth: false }),
  newGoods: (limit = 8) => request(`/home/new?limit=${limit}`, { auth: false }),
  hot: (limit = 8) => request(`/home/hot?limit=${limit}`, { auth: false }),
  // AI 智能推荐：带 token 就是千人千面，不带就是热销兜底（auth 默认 true，会自动附 token）
  recommend: (limit = 6) => request(`/home/recommend?limit=${limit}`),
  categoryHead: () => request('/home/category/head', { auth: false }),
  category: (id) => request(`/category?id=${id}`, { auth: false }),
  goods: (id) => request(`/goods?id=${id}`, { auth: false }),

  login: (account, password) => request('/login', { method: 'POST', data: { account, password }, auth: false }),

  cartList: () => request('/member/cart'),
  cartAdd: (skuId, count) => request('/member/cart', { method: 'POST', data: { skuId: String(skuId), count } }),
  cartUpdate: (skuId, data) => request(`/member/cart/${skuId}`, { method: 'PUT', data }),
  cartDelete: (ids) => request('/member/cart', { method: 'DELETE', data: { ids: ids.map(String) } }),

  addressList: () => request('/member/address'),
  orderPre: (skuIds) => request(`/member/order/pre?skuIds=${skuIds.join(',')}`),
  orderSubmit: (data) => request('/member/order', { method: 'POST', data }),
  orderDetail: (id) => request(`/member/order/${id}`),
  orderList: () => request('/member/order'),
  pay: (orderId) => request(`/pay/aliPay?orderId=${orderId}`),

  profile: () => request('/member/profile'),

  /* ---------------- AI 智能客服 ---------------- */
  aiChat: (message, orderId = null, sessionId = null) =>
    request('/member/ai/chat', { method: 'POST', data: { message, orderId, sessionId } }),
  aiStatus: () => request('/member/ai/status', { auth: false }),

  /* ---------------- 短视频（抖音式信息流） ---------------- */
  videoFeed: (limit = 10) => request(`/home/video?limit=${limit}`, { auth: false }),
  videoLike: (id) => request(`/home/video/${id}/like`, { auth: false }),
  videoLikeCancel: (id) => request(`/home/video/${id}/like/cancel`, { auth: false }),
  videoCollect: (id) => request(`/home/video/${id}/collect`, { auth: false }),
  videoCollectCancel: (id) => request(`/home/video/${id}/collect/cancel`, { auth: false }),
  videoShare: (id) => request(`/home/video/${id}/share`, { auth: false }),

  /* ---------------- 评论（targetType = goods | video） ---------------- */
  commentList: (targetType, targetId, limit = 50) =>
    request(`/comment/list?targetType=${targetType}&targetId=${targetId}&limit=${limit}`, { auth: false }),
  commentAdd: (targetType, targetId, content, score = 5) =>
    request('/member/comment', { method: 'POST', data: { targetType, targetId: String(targetId), content, score } }),
  commentDelete: (id) => request(`/member/comment/${id}`, { method: 'DELETE' }),
  commentMine: () => request('/member/comment/mine'),
  commentLike: (id) => request(`/comment/${id}/like`, { method: 'POST' }),

  /* ---------------- 优惠券 / 秒杀 / 价格走势（B 方案） ---------------- */
  couponCenter: () => request('/coupon/list', { auth: false }),
  couponTake: (id) => request(`/member/coupon/${id}/take`, { method: 'POST' }),
  couponMine: (status) => request('/member/coupon' + (status === undefined || status === null ? '' : `?status=${status}`)),
  seckillList: () => request('/seckill/list', { auth: false }),
  seckillDetail: (id) => request(`/seckill/${id}`, { auth: false }),
  seckillBuy: (data) => request('/member/seckill/buy', { method: 'POST', data }),
  priceHistory: (spuId, days = 90) => request(`/goods/${spuId}/price-history?days=${days}`, { auth: false }),

  /* ---------------- 直播带货 ---------------- */
  liveList: () => request('/live/list', { auth: false }),
  liveDetail: (id) => request(`/live/${id}`, { auth: false }),
  liveMessages: (id, afterId) => request(`/live/${id}/messages?afterId=${afterId || 0}`, { auth: false }),
  liveJoin: (id) => request(`/member/live/${id}/join`, { method: 'POST' }),
  liveLike: (id) => request(`/member/live/${id}/like`, { method: 'POST' }),
  liveSend: (id, content) => request(`/member/live/${id}/message`, { method: 'POST', data: { content } }),

  /* ---------------- 管理后台（仅 admin 账号可用） ---------------- */
  adminStats: () => request('/admin/stats'),

  adminGoods: (keyword = '', page = 1, size = 10) =>
    request(`/admin/goods?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`),
  adminGoodsDetail: (id) => request(`/admin/goods/${id}`),
  adminGoodsAdd: (data) => request('/admin/goods', { method: 'POST', data }),
  adminGoodsUpdate: (id, data) => request(`/admin/goods/${id}`, { method: 'PUT', data }),
  adminGoodsDelete: (id) => request(`/admin/goods/${id}`, { method: 'DELETE' }),

  adminVideoList: () => request('/admin/video'),
  adminVideoAdd: (data) => request('/admin/video', { method: 'POST', data }),
  // AI 生成短视频：后端转发 Python（查商品→AI图封面→ffmpeg 合成），耗时 30~90 秒
  // 兼容旧用法（字符串关键词）与新用法（对象 {keyword, spuId, style, duration}）
  adminVideoGenerate: (kwOrPayload) => request('/admin/video/generate', {
    method: 'POST',
    data: typeof kwOrPayload === 'string' ? { keyword: kwOrPayload } : kwOrPayload,
    readTimeout: 180000
  }),
  adminVideoUpdate: (id, data) => request(`/admin/video/${id}`, { method: 'PUT', data }),
  adminVideoDelete: (id) => request(`/admin/video/${id}`, { method: 'DELETE' }),

  adminCommentList: (targetType = '', keyword = '') =>
    request(`/admin/comment?targetType=${targetType}&keyword=${encodeURIComponent(keyword)}`),
  adminCommentUpdate: (id, data) => request(`/admin/comment/${id}`, { method: 'PUT', data }),
  adminCommentDelete: (id) => request(`/admin/comment/${id}`, { method: 'DELETE' }),

  adminUserList: (keyword = '') => request(`/admin/user?keyword=${encodeURIComponent(keyword)}`),
  adminUserAdd: (data) => request('/admin/user', { method: 'POST', data }),
  adminUserUpdate: (id, data) => request(`/admin/user/${id}`, { method: 'PUT', data }),
  adminUserDelete: (id) => request(`/admin/user/${id}`, { method: 'DELETE' }),

  adminOrderList: () => request('/admin/order')
};
