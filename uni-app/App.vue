<script>
export default {
  onLaunch() {
    console.log('京东10143 启动，接口地址见 api/request.js 里的 BASE_URL')
  }
}
</script>

<style>
page {
  background: #f5f5f5;
  font-size: 28rpx;
  color: #333;
}
.container {
  padding: 20rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.title {
  font-size: 34rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 8rpx solid #e1251b;
}
.price {
  color: #e1251b;
  font-weight: bold;
}
.btn-primary {
  background: #e1251b;
  color: #fff;
  border-radius: 40rpx;
  font-size: 30rpx;
}
.btn-plain {
  background: #f2f2f2;
  color: #333;
  border-radius: 40rpx;
  font-size: 30rpx;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}
.product {
  width: 48%;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  padding-bottom: 8rpx;
  box-sizing: border-box;
}
/* 方形图框：宽度撑满卡片，padding-top:100% 让高度等于宽度。
   aspectFit 保证图片完整显示不裁切，比例不符时留白，绝不压住下面的文字 */
.product .pic {
  position: relative;
  width: 100%;
  height: 0;
  padding-top: 100%;
  background: #f7f7f7;
  overflow: hidden;
}
.product .pic image {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  display: block;
}
.product .name {
  font-size: 26rpx;
  line-height: 36rpx;
  padding: 12rpx 12rpx 4rpx;
  height: 84rpx;
  box-sizing: border-box;
  overflow: hidden;
  word-break: break-all;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.product .p {
  color: #e1251b;
  font-weight: bold;
  padding: 0 12rpx 8rpx;
  font-size: 32rpx;
  line-height: 42rpx;
}
.empty {
  text-align: center;
  color: #999;
  padding: 120rpx 0;
}
.row {
  display: flex;
  align-items: center;
}
.qty {
  display: flex;
  border: 1rpx solid #ddd;
  border-radius: 8rpx;
  overflow: hidden;
}
.qty text {
  width: 60rpx;
  text-align: center;
  line-height: 56rpx;
}
.qty view {
  width: 56rpx;
  text-align: center;
  line-height: 56rpx;
  background: #f7f7f7;
}

/* PC 宽屏适配统一放在项目根 index.html 的内联样式里（H5），此处不再重复 */
</style>
