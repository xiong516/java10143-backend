import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// uni-app H5 构建配置（HBuilderX 项目补 CLI 支持）
export default defineConfig({
  plugins: [uni()]
})
