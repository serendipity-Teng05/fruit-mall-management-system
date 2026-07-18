<template>
  <div class="register-page">
    <div class="register-visual">
      <router-link to="/mall" class="register-brand"><span><el-icon><Apple /></el-icon></span><strong>鲜果集</strong></router-link>
      <div><h1>新鲜生活，从一份好水果开始</h1><p>注册后可保存收货地址、跟踪订单并完成安全演示支付。</p></div>
      <img src="/mall/hero-fruit-basket.png" alt="新鲜水果" />
    </div>
    <div class="register-form-wrap">
      <div class="register-form-box">
        <h2>创建顾客账号</h2><p>已有账号？<router-link to="/login">立即登录</router-link></p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
          <div class="form-grid"><el-form-item label="用户名" prop="username"><el-input v-model="form.username" placeholder="3-50 位字母、数字或下划线" /></el-form-item><el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" placeholder="用于收货信息" /></el-form-item></div>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" maxlength="11" placeholder="请输入 11 位手机号" /></el-form-item>
          <el-form-item label="登录密码" prop="password"><el-input v-model="form.password" type="password" show-password maxlength="72" placeholder="8-72 位" /></el-form-item>
          <el-form-item label="验证码" prop="captcha"><div class="captcha-row"><el-input v-model="form.captcha" maxlength="4" @keyup.enter="submit" /><button type="button" :disabled="captchaLoading" @click="loadCaptcha"><img v-if="captchaUrl" :src="captchaUrl" alt="验证码" /><span v-else>点击刷新</span></button></div></el-form-item>
          <el-button class="mall-primary-button submit-button" type="primary" :loading="loading" @click="submit">注册并去登录</el-button>
        </el-form>
        <router-link to="/mall" class="back-link">先逛逛商城</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Apple } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCaptcha, register } from '@/api/auth'
const router = useRouter(); const formRef = ref(); const loading = ref(false); const captchaLoading = ref(false); const captchaUrl = ref(''); let objectUrl = ''
const form = reactive({ username: '', realName: '', phone: '', password: '', captcha: '' })
const rules = { username: [{ required: true, pattern: /^[A-Za-z0-9_@.\-]{3,50}$/, message: '用户名格式不正确', trigger: 'blur' }], realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }], phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确手机号', trigger: 'blur' }], password: [{ required: true, min: 8, max: 72, message: '密码长度应为 8-72 位', trigger: 'blur' }], captcha: [{ required: true, len: 4, message: '请输入 4 位验证码', trigger: 'blur' }] }
const releaseUrl = () => { if (objectUrl) URL.revokeObjectURL(objectUrl); objectUrl = '' }
const loadCaptcha = async () => { captchaLoading.value = true; try { const blob = await getCaptcha(); releaseUrl(); objectUrl = URL.createObjectURL(blob); captchaUrl.value = objectUrl; form.captcha = '' } finally { captchaLoading.value = false } }
const submit = async () => { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; loading.value = true; try { await register(form); ElMessage.success('注册成功，请登录'); router.replace({ path: '/login', query: { redirect: '/mall/checkout', account: form.username } }) } catch { loadCaptcha() } finally { loading.value = false } }
onMounted(loadCaptcha); onBeforeUnmount(releaseUrl)
</script>

<style scoped>
.register-page { min-height: 100vh; display: grid; grid-template-columns: minmax(420px, .95fr) minmax(520px, 1.05fr); background: #fff; }.register-visual { position: relative; overflow: hidden; padding: 42px 54px; background: var(--mall-mint); display: flex; flex-direction: column; justify-content: space-between; }.register-brand { position: relative; z-index: 2; display: flex; align-items: center; gap: 10px; color: var(--mall-green); text-decoration: none; font-size: 25px; }.register-brand > span { width: 38px; height: 38px; border: 3px solid var(--mall-green); border-radius: 50%; display: grid; place-items: center; }.register-visual > div { position: relative; z-index: 2; max-width: 520px; margin-bottom: 44%; }.register-visual h1 { font-size: clamp(38px, 4vw, 60px); line-height: 1.12; margin: 0; }.register-visual p { color: var(--mall-muted); line-height: 1.8; font-size: 17px; }.register-visual img { position: absolute; left: -40%; bottom: 0; width: 150%; max-width: none; }.register-form-wrap { display: grid; place-items: center; padding: 42px; }.register-form-box { width: min(560px, 100%); }.register-form-box h2 { font-size: 34px; margin: 0 0 8px; }.register-form-box > p { color: var(--mall-muted); margin: 0 0 30px; }.register-form-box a { color: var(--mall-green); }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }.captcha-row { width: 100%; display: grid; grid-template-columns: 1fr 120px; gap: 12px; }.captcha-row button { height: 42px; padding: 0; border: 1px solid var(--mall-border); border-radius: 8px; overflow: hidden; background: #fff; cursor: pointer; }.captcha-row img { width: 100%; height: 100%; object-fit: cover; }.submit-button { width: 100%; min-height: 48px; }.back-link { display: block; margin-top: 20px; text-align: center; text-decoration: none; }
@media (max-width: 900px) { .register-page { grid-template-columns: 1fr; }.register-visual { min-height: 260px; padding: 28px; }.register-visual > div { margin: 40px 0 60px; max-width: 60%; }.register-visual h1 { font-size: 34px; }.register-visual img { left: 10%; width: 120%; }.register-form-wrap { padding: 34px 24px; } }
@media (max-width: 560px) { .register-visual { display: none; }.form-grid { grid-template-columns: 1fr; gap: 0; }.register-form-wrap { padding: 30px 18px; }.register-form-box h2 { font-size: 30px; } }
</style>
