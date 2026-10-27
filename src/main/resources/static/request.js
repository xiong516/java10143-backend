/**
 * uni-app 统一请求封装（H5 / 微信小程序 通用）
 *
 * 后端地址会自动选，正常情况下你**不用改这里**：
 *   H5（电脑上开的 localhost:5173）  → CPOLAR_URL（强制走隧道）
 *   微信小程序 / 真机                 → 下面的 LAN_IP:8080
 *
 * 为什么这么写：H5 页面是从哪台机器打开的，后端就在哪台机器上，
 * 跟着页面的 hostname 走，电脑和手机都能连对，不会再出现「手机连不上」。
 *
 * 注意：端口一定是 8080（Java10143 后端）。
 *       8082 是 coffee（蓝杯咖啡）项目的端口，8083 已彻底废弃，都不要用。
 */
/** 局域网候选：两个 WiFi 网段 + 有线网卡，谁通用谁（不用手改） */
const LAN_BASES = [
  'http://192.168.100.200:8080',   // WLAN 当前在用
  'http://192.168.100.199:8080',   // WLAN 旧网段，仅兜底
  'http://192.168.103.21:8080'     // 有线网卡（Ethernet）
];
const LAN_IP = '192.168.100.200';   // 兼容旧引用
const CPOLAR_URL = 'https://7dcb6692.r8.cpolar.top';
 // https://7dcb6692.r8.cpolar.top
 // 换成你CPOLAR后台重启后拿到的那个新地址，千万注意不要漏掉 https
function pickBaseUrl() {
  // #ifdef H5
  try {
    const h = (typeof location !== 'undefined' && location.hostname) || '';
    if (h) return location.origin;  // 同源优先：localhost / 局域网 / 隧道，页面从哪打开就走哪
  } catch (e) { /* 拿不到就退回局域网 IP */ }
  // #endif
  return LAN_BASES[0];
}

export const BASE_URL = pickBaseUrl();
/** 全部候选线路：首选 → 两个局域网 → 隧道，逐个试，前一个连不上自动换下一个 */
export const BASE_LIST = (function () {
  const raw = [BASE_URL, LAN_BASES[0], LAN_BASES[1], LAN_BASES[2], CPOLAR_URL];
  const out = [];
  for (let i = 0; i < raw.length; i++) {
    if (raw[i] && out.indexOf(raw[i]) < 0) out.push(raw[i]);
  }
  return out;
})();

/** 请求超时（毫秒）。手机上如果连不上后端，8 秒就报错，不要傻等 60 秒 */
const TIMEOUT = 8000;

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

/**
 * 统一请求：按 BASE_LIST 顺序尝试，连不上自动换下一个地址
 * （H5 用隧道打开时，隧道挂了会自动退 WiFi/有线局域网）
 */
export function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    let settled = false;
    const run = (idx) => {
      requestOnce(path, options, BASE_LIST[idx]).then((r) => {
        if (!settled) { settled = true; resolve(r); }
      }).catch((err) => {
        if (idx + 1 < BASE_LIST.length) {
          console.warn('[Java10143] ' + BASE_LIST[idx] + ' 连不上，自动改用 ' + BASE_LIST[idx + 1]);
          run(idx + 1);
          return;
        }
        if (!settled) { settled = true; reject(err); }
      });
    };
    run(0);
  });
}

function requestOnce(path, options, BASE_URL) {
  const { method = 'GET', data, auth = true, loading = false } = options;
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' };
    if (auth && getToken()) header['Authorization'] = 'Bearer ' + getToken();
    if (loading) uni.showLoading({ title: '加载中', mask: true });

    uni.request({
      url: BASE_URL + path,
      method,
      data,
      header,
      timeout: TIMEOUT,
      success: (res) => {
        if (loading) uni.hideLoading();
        const body = res.data;
        if (!body) return reject(new Error('后端没有返回数据，检查 Java10143 是否启动'));
        if (body.code !== '1') {
          uni.showToast({ title: body.msg || '请求失败', icon: 'none' });
          return reject(new Error(body.msg || '请求失败'));
        }
        resolve(body.result);
      },
      fail: (err) => {
        if (loading) uni.hideLoading();
        // 把「到底连的哪个地址」显示出来，这样手机上一眼就知道该改哪儿
        const host = BASE_URL.replace('http://', '');
        const raw = (err && err.errMsg) || '未知错误';
        let tip = '连不上后端 ' + host;
        if (raw.indexOf('timeout') >= 0) tip = '后端响应超时（' + host + '）';
        else if (raw.indexOf('fail') >= 0) tip = '连不上后端 ' + host + '（手机要和电脑同一个 WiFi）';
        uni.showToast({ title: tip, icon: 'none', duration: 3500 });
        console.error('[Java10143] 请求失败 ' + method + ' ' + BASE_URL + path + ' → ' + raw);
        reject(err);
      }
    });
  });
}

/* ---------------- 接口清单 ---------------- */
export const api = {
  banner: (site = 1) => request(`/home/banner?distributionSite=${site}`, { auth: false }),
  newGoods: (limit = 8) => request(`/home/new?limit=${limit}`, { auth: false }),
  hot: (limit = 8) => request(`/home/hot?limit=${limit}`, { auth: false }),
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

  /* ---------------- 评论（targetType = goods | video） ---------------- */
  commentList: (targetType, targetId, limit = 50) =>
    request(`/comment/list?targetType=${targetType}&targetId=${targetId}&limit=${limit}`, { auth: false }),
  commentAdd: (targetType, targetId, content, score = 5) =>
    request('/member/comment', { method: 'POST', data: { targetType, targetId: String(targetId), content, score } }),
  commentDelete: (id) => request(`/member/comment/${id}`, { method: 'DELETE' }),
  commentMine: () => request('/member/comment/mine'),
  commentLike: (id) => request(`/comment/${id}/like`, { method: 'POST' }),

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
