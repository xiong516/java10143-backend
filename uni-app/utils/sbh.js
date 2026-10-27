/* 状态栏高度（单位 px）
 *
 * 背景（2026-10-24 踩坑）：App 端（5+ Runtime）不会注入 --status-bar-height
 * 这个 CSS 变量（那是微信小程序的能力），页面里写
 *   padding-top: calc(16rpx + var(--status-bar-height, 0px))
 * 在 App 上永远退化成 16rpx，自定义头部会被系统状态栏压住
 * （详情页头部、jd-nav、直播/秒杀页顶栏都中过招）。
 *
 * 正确做法：JS 里取真实高度，页面头部用 :style 绑定补回去。
 *   App 端：uni.getSystemInfoSync().statusBarHeight（兜底 plus.navigator）
 *   H5 端 ：浏览器没有"状态栏"概念，返回 0，电脑网页不受影响
 */
export function statusBarHeight() {
  try {
    const info = uni.getSystemInfoSync();
    if (info && info.uniPlatform !== 'h5' && info.statusBarHeight) {
      return info.statusBarHeight;
    }
  } catch (e) {}
  try {
    if (window.plus && plus.navigator && plus.navigator.getStatusbarHeight) {
      return plus.navigator.getStatusbarHeight() || 0;
    }
  } catch (e) {}
  return 0;
}
