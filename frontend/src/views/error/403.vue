<template>
  <div class="forbidden-container">
    <div class="forbidden-card">
      <el-icon class="lock-icon"><Lock /></el-icon>
      <h1>403</h1>
      <h2>无权限访问</h2>
      <p>您没有权限查看此页面，请联系管理员获取相应权限。</p>
      <el-button type="primary" @click="$router.push('/dashboard')" round>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { Lock } from '@element-plus/icons-vue'
</script>

<style scoped>
.forbidden-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.forbidden-card {
  text-align: center;
  background: rgba(255, 255, 255, 0.95);
  padding: 40px 60px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  max-width: 500px;
  width: 90%;
}

.lock-icon {
  font-size: 80px;
  color: #f56c6c;
  margin-bottom: 20px;
}

h1 {
  font-size: 72px;
  margin: 0;
  color: #333;
  line-height: 1;
  font-weight: 600;
}

h2 {
  font-size: 28px;
  margin: 10px 0 20px;
  color: #555;
  font-weight: 500;
}

p {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
  line-height: 1.6;
}

.el-button {
  padding: 12px 30px;
  font-size: 16px;
}
</style>