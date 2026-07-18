<template>
  <main class="login-container">
    <div class="bg-image" aria-hidden="true"></div>
    <div class="bg-overlay" aria-hidden="true"></div>

    <section class="login-box" aria-labelledby="login-title">
      <aside class="login-left" aria-label="鲜果集品牌介绍">
        <div class="brand-content">
          <span class="brand-mark" aria-hidden="true">
            <el-icon><Apple /></el-icon>
          </span>
          <p class="brand-eyebrow">FRUIT MALL</p>
          <h1 class="brand-title">鲜果集</h1>
          <p class="brand-desc">水果商城与智慧管理一体化平台</p>
          <p class="brand-slogan">顾客登录进入商城 · 员工登录进入后台</p>
        </div>
      </aside>

      <div class="login-right">
        <div class="mobile-brand" aria-hidden="true">
          <el-icon><Apple /></el-icon>
          <span>鲜果集</span>
        </div>

        <header class="form-header">
          <p class="form-eyebrow">欢迎回来</p>
          <h2 id="login-title">登录鲜果集</h2>
          <p class="sub-title">顾客、员工和管理员使用同一账号体系登录</p>
        </header>

        <div v-if="loginError" class="login-alert" role="alert" aria-live="polite">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ loginError }}</span>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名 / 手机号 / 邮箱"
              :prefix-icon="User"
              clearable
              maxlength="50"
              autocomplete="username"
              aria-label="登录账号"
              @input="clearLoginError"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              show-password
              maxlength="72"
              autocomplete="current-password"
              aria-label="登录密码"
              @input="clearLoginError"
              @keyup.enter="handleLogin"
              @keyup="handlePasswordKeyup"
            />
            <div v-if="capsLockOn" class="caps-tip">大写锁定已开启</div>
          </el-form-item>

          <el-form-item prop="captcha">
            <div class="captcha-field">
              <div class="captcha-box">
                <el-input
                  v-model="loginForm.captcha"
                  placeholder="请输入验证码"
                  :prefix-icon="Key"
                  maxlength="4"
                  autocomplete="off"
                  aria-label="验证码"
                  @input="clearLoginError"
                  @keyup.enter="handleLogin"
                />
                <button
                  type="button"
                  class="captcha-img"
                  :class="{ 'is-error': captchaError }"
                  :disabled="captchaLoading"
                  :title="captchaError || '点击换一张验证码'"
                  aria-label="刷新验证码"
                  @click="refreshCaptcha"
                >
                  <img v-if="captchaUrl" :src="captchaUrl" alt="图形验证码" />
                  <span v-else>{{ captchaLoading ? '加载中...' : '点击重试' }}</span>
                </button>
              </div>
              <div class="captcha-help" :class="{ 'is-error': captchaError }">
                <span>{{ captchaError || '验证码不清晰？' }}</span>
                <button v-if="!captchaError" type="button" @click="refreshCaptcha">换一张</button>
              </div>
            </div>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button
              native-type="submit"
              type="primary"
              class="login-btn"
              :loading="loading"
            >
              {{ loading ? '登录中...' : '立即登录' }}
            </el-button>
          </el-form-item>

          <nav class="login-links" aria-label="登录辅助操作">
            <router-link to="/mall">返回商城</router-link>
            <router-link to="/register">注册顾客账号</router-link>
          </nav>
        </el-form>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Apple, Key, Lock, User, WarningFilled } from '@element-plus/icons-vue'
import { getCaptcha, login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loginFormRef = ref(null)
const loading = ref(false)
const captchaUrl = ref('')
const captchaLoading = ref(false)
const captchaError = ref('')
const capsLockOn = ref(false)
const loginError = ref('')

let captchaObjectUrl = ''

const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const clearLoginError = () => {
  loginError.value = ''
}

const validateAccount = (rule, value, callback) => {
  const val = (value || '').trim()

  if (!val) {
    return callback(new Error('请输入账号'))
  }

  if (val.length < 3 || val.length > 50) {
    return callback(new Error('账号长度应为 3–50 位'))
  }

  const usernameReg = /^[a-zA-Z0-9_-]{3,50}$/
  const phoneReg = /^1[3-9]\d{9}$/
  const emailReg = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

  if (usernameReg.test(val) || phoneReg.test(val) || emailReg.test(val)) {
    callback()
  } else {
    callback(new Error('请输入正确的用户名、手机号或邮箱'))
  }
}

const validatePassword = (rule, value, callback) => {
  const val = value || ''

  if (!val) {
    return callback(new Error('请输入密码'))
  }

  if (val.length < 6 || val.length > 72) {
    return callback(new Error('密码长度应为 6–72 位'))
  }

  if (/\s/.test(val)) {
    return callback(new Error('密码不能包含空格'))
  }

  callback()
}

const validateCaptcha = (rule, value, callback) => {
  const val = (value || '').trim()

  if (!val) {
    return callback(new Error('请输入验证码'))
  }

  if (val.length !== 4) {
    return callback(new Error('验证码为 4 位字符'))
  }

  callback()
}

const loginRules = {
  username: [{ validator: validateAccount, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  captcha: [{ validator: validateCaptcha, trigger: 'blur' }]
}

const delay = ms => new Promise(resolve => window.setTimeout(resolve, ms))

const releaseCaptchaUrl = () => {
  if (captchaObjectUrl) {
    URL.revokeObjectURL(captchaObjectUrl)
    captchaObjectUrl = ''
  }
}

const loadCaptcha = async ({ attempts = 1, notify = true } = {}) => {
  if (captchaLoading.value) return false

  captchaLoading.value = true
  captchaError.value = ''

  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const blob = await getCaptcha()
      if (!(blob instanceof Blob) || !blob.type.startsWith('image/') || blob.size === 0) {
        throw new Error('验证码接口未返回有效图片')
      }

      releaseCaptchaUrl()
      captchaObjectUrl = URL.createObjectURL(blob)
      captchaUrl.value = captchaObjectUrl
      loginForm.captcha = ''
      loginFormRef.value?.clearValidate('captcha')
      captchaLoading.value = false
      return true
    } catch (error) {
      if (attempt < attempts) {
        await delay(1200)
      }
    }
  }

  captchaUrl.value = ''
  captchaError.value = '验证码未加载，请检查后端及本地数据库配置后重试'
  captchaLoading.value = false
  if (notify) {
    ElMessage.error(captchaError.value)
  }
  return false
}

const refreshCaptcha = () => {
  clearLoginError()
  return loadCaptcha({ attempts: 1, notify: true })
}

const handlePasswordKeyup = event => {
  if (event && typeof event.getModifierState === 'function') {
    capsLockOn.value = event.getModifierState('CapsLock')
  }
}

onMounted(() => {
  loadCaptcha({ attempts: 8, notify: false })
})

onBeforeUnmount(() => {
  releaseCaptchaUrl()
})

const getLoginErrorMessage = error => {
  return error?.msg || error?.response?.data?.msg || error?.message || '登录失败，请稍后重试'
}

const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) return

  clearLoginError()
  loginForm.username = (loginForm.username || '').trim()
  loginForm.captcha = (loginForm.captcha || '').trim()

  if (!captchaUrl.value) {
    loginError.value = captchaLoading.value ? '验证码正在加载，请稍候' : '请先获取验证码'
    if (!captchaLoading.value) {
      loadCaptcha({ attempts: 1, notify: false })
    }
    return
  }

  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true

  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha
    })

    if (String(res.code) !== '200') {
      throw res
    }

    userStore.login(res.data.user || {}, res.data.token, res.data.permissionCodes || [])
    useCartStore().switchOwner(res.data.user?.id)
    ElMessage.success('登录成功，欢迎回来！')

    const requested = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const safeRequested = requested.startsWith('/') && !requested.startsWith('//') ? requested : ''
    const destination = safeRequested || ((res.data.permissionCodes || []).length ? '/dashboard' : '/mall')
    router.replace(destination)
  } catch (error) {
    if (!error?.__handled) {
      console.error('登录报错:', error)
    }
    loginError.value = getLoginErrorMessage(error)
    loginForm.captcha = ''
    loadCaptcha({ attempts: 1, notify: false })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  --brand: #079447;
  --brand-dark: #05743a;
  --brand-soft: #e8f7ee;
  --text: #19221d;
  --muted: #6f7a74;
  --border: #dfe8e2;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 100vh;
  min-height: 100dvh;
  box-sizing: border-box;
  padding: 32px;
  overflow-x: hidden;
  overflow-y: auto;
  color: var(--text);
}

.bg-image,
.bg-overlay {
  position: fixed;
  inset: 0;
}

.bg-image {
  z-index: 0;
  background: #edf8f1 url('/mall/hero-fruit-basket.png') no-repeat center center;
  background-size: cover;
  animation: bgZoom 24s ease-in-out infinite alternate;
}

.bg-overlay {
  z-index: 1;
  background:
    radial-gradient(circle at 22% 18%, rgba(255, 255, 255, 0.76), transparent 31%),
    linear-gradient(110deg, rgba(226, 246, 234, 0.58) 0%, rgba(240, 249, 244, 0.28) 56%, rgba(220, 241, 230, 0.22) 100%);
  backdrop-filter: blur(2px);
}

@keyframes bgZoom {
  from { transform: scale(1); }
  to { transform: scale(1.035); }
}

.login-box {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 46% 54%;
  width: min(1020px, 100%);
  min-height: 560px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(255, 255, 255, 0.76);
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(27, 61, 42, 0.2), 0 8px 24px rgba(27, 61, 42, 0.1);
  animation: fadeInUp 0.62s ease-out;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(22px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-left {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 56px;
  color: #fff;
  background: linear-gradient(145deg, #0caf5b 0%, #068747 55%, #04713b 100%);
}

.login-left::before,
.login-left::after {
  content: '';
  position: absolute;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.07);
}

.login-left::before {
  top: -95px;
  left: -90px;
  width: 240px;
  height: 240px;
}

.login-left::after {
  right: -95px;
  bottom: -135px;
  width: 330px;
  height: 330px;
}

.brand-content {
  position: relative;
  z-index: 1;
  width: 100%;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 76px;
  height: 76px;
  margin-bottom: 42px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25);
  font-size: 42px;
}

.brand-eyebrow,
.form-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.brand-eyebrow {
  color: rgba(255, 255, 255, 0.72);
}

.brand-title {
  margin: 0 0 14px;
  font-size: 40px;
  line-height: 1.16;
  letter-spacing: 0.08em;
}

.brand-desc {
  margin: 0;
  font-size: 18px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.9);
}

.brand-slogan {
  display: inline-flex;
  align-items: center;
  margin: 52px 0 0;
  padding: 9px 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.92);
}

.login-right {
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-sizing: border-box;
  padding: 58px 68px;
  background: rgba(255, 255, 255, 0.97);
}

.mobile-brand {
  display: none;
}

.form-header {
  margin-bottom: 28px;
}

.form-eyebrow {
  color: var(--brand);
}

.form-header h2 {
  margin: 0 0 10px;
  color: var(--text);
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.sub-title {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.login-alert {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin: -6px 0 18px;
  padding: 11px 13px;
  border: 1px solid #f3c8c4;
  border-radius: 10px;
  background: #fff4f2;
  color: #b42318;
  font-size: 13px;
  line-height: 1.45;
}

.login-alert .el-icon {
  flex: 0 0 auto;
  margin-top: 2px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 52px;
  box-sizing: border-box;
  padding: 0 16px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 11px;
  box-shadow: 0 4px 12px rgba(29, 53, 39, 0.035);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: #b8cabf;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--brand);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(7, 148, 71, 0.12);
}

.login-form :deep(.el-input__inner) {
  color: var(--text);
  font-size: 15px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #a0aaa4;
}

.login-form :deep(.el-form-item__error) {
  padding-top: 5px;
}

.captcha-field {
  width: 100%;
}

.captcha-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 160px;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.captcha-img {
  width: 160px;
  height: 48px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  color: var(--muted);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.captcha-img:hover:not(:disabled),
.captcha-img:focus-visible {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(7, 148, 71, 0.12);
  outline: none;
}

.captcha-img.is-error {
  border-color: #e6a23c;
  color: #b26a00;
}

.captcha-img:disabled {
  cursor: wait;
}

.captcha-img img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
}

.captcha-help {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  min-height: 18px;
  margin-top: 6px;
  color: #8a958f;
  font-size: 12px;
  line-height: 18px;
}

.captcha-help button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--brand);
  font: inherit;
  cursor: pointer;
}

.captcha-help button:hover {
  text-decoration: underline;
}

.captcha-help.is-error {
  justify-content: flex-start;
  color: #b26a00;
}

.caps-tip {
  width: 100%;
  margin: 6px 0 -3px 2px;
  color: #b26a00;
  font-size: 12px;
  line-height: 18px;
}

.login-form :deep(.submit-item) {
  margin-top: 4px;
  margin-bottom: 0;
}

.login-btn {
  width: 100%;
  height: 52px;
  border: 0;
  border-radius: 11px;
  background: linear-gradient(100deg, #09a852 0%, #078f47 100%);
  box-shadow: 0 10px 22px rgba(7, 148, 71, 0.2);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.08em;
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 13px 26px rgba(7, 148, 71, 0.27);
  filter: brightness(1.03);
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
  font-size: 14px;
}

.login-links a {
  color: var(--brand-dark);
  text-decoration: none;
  text-underline-offset: 4px;
}

.login-links a:hover,
.login-links a:focus-visible {
  text-decoration: underline;
}

@media (max-width: 900px) {
  .login-container {
    align-items: flex-start;
    padding: 28px 18px;
  }

  .bg-image {
    background-position: 68% center;
  }

  .bg-overlay {
    background: rgba(230, 247, 237, 0.58);
    backdrop-filter: blur(4px);
  }

  .login-box {
    display: block;
    width: min(100%, 480px);
    min-height: 0;
    margin: auto 0;
    border-radius: 20px;
  }

  .login-left {
    display: none;
  }

  .login-right {
    padding: 38px 34px 34px;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 30px;
    color: var(--brand-dark);
    font-size: 17px;
    font-weight: 800;
  }

  .mobile-brand .el-icon {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 14px;
  }

  .login-right {
    padding: 30px 22px 26px;
  }

  .mobile-brand {
    margin-bottom: 24px;
  }

  .form-header {
    margin-bottom: 24px;
  }

  .form-header h2 {
    font-size: 26px;
  }

  .captcha-box {
    grid-template-columns: minmax(0, 1fr) 120px;
    gap: 8px;
  }

  .captcha-img {
    width: 120px;
    height: 40px;
  }

  .login-links {
    margin-top: 20px;
  }
}

@media (max-height: 680px) and (min-width: 901px) {
  .login-container {
    align-items: flex-start;
    padding-top: 24px;
    padding-bottom: 24px;
  }

  .login-box {
    min-height: 540px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .bg-image,
  .login-box {
    animation: none;
  }

  .login-btn {
    transition: none;
  }
}
</style>
