<template>
  <div class="login">
    <div class="login-shell">
      <!-- 左侧：标语 + 元素图，纵向排列，互不重叠 -->
      <aside class="login-hero">
        <div class="login-hero__copy">
          <h2 class="login-hero__headline">
            <span class="login-hero__ai">AI</span>
            <span>Code Review</span>
          </h2>
          <p class="login-hero__lede">你的每行代码，都要经得起 AI 的审视。</p>
        </div>
        <img class="login-hero__art" src="@/assets/images/login-light.png" alt="AI 代码审查" />
      </aside>

      <!-- 右侧：登录区（留白与纵向居中，避免「顶满屏」） -->
      <main class="login-panel">
        <div class="login-panel__inner">
          <div class="login-card">
            <env-tag v-if="currentEnv !== 'production'" :env="currentEnv" class="login-env-tag" />
            <div class="login-brand">
              <div class="login-brand__mark">
                <img v-if="brandLogo" :src="brandLogo" alt="" class="login-brand__logo" />
              </div>
              <h1 class="login-title">{{ platformTitle }}</h1>
            </div>

            <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  size="large"
                  auto-complete="off"
                  placeholder="账号"
                >
                  <template #prefix><svg-icon icon-class="user" class="input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  size="large"
                  auto-complete="off"
                  placeholder="密码"
                  @keyup.enter="handleLogin"
                >
                  <template #prefix><svg-icon icon-class="password" class="input-icon" /></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="code" v-if="captchaEnabled">
                <div class="captcha-wrap">
                  <el-input
                    v-model="loginForm.code"
                    size="large"
                    auto-complete="off"
                    placeholder="验证码"
                    class="captcha-input"
                    @keyup.enter="handleLogin"
                  >
                    <template #prefix><svg-icon icon-class="validCode" class="input-icon" /></template>
                  </el-input>
                  <div
                    class="captcha-img"
                    title="点击刷新验证码"
                    role="button"
                    tabindex="0"
                    @click="getCode"
                    @keydown.enter.prevent="getCode"
                  >
                    <img :src="codeUrl" class="login-code-img" alt="验证码" />
                  </div>
                </div>
              </el-form-item>
              <div class="login-options">
                <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
                <router-link v-if="register" class="link-type" :to="'/register'">立即注册</router-link>
              </div>
              <el-form-item class="login-btn-wrap">
                <el-button
                  :loading="loading"
                  size="large"
                  type="primary"
                  class="login-btn"
                  @click.prevent="handleLogin"
                >
                  <span v-if="!loading">登 录</span>
                  <span v-else>登 录 中...</span>
                </el-button>
              </el-form-item>
            </el-form>

            <div class="login-footer">
              <span>{{ footerContent }}</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import brandLogoUrl from '@/assets/logo/logo.png'
import EnvTag from '@/components/EnvTag'

const brandLogo = brandLogoUrl
const platformTitle = import.meta.env.VITE_APP_TITLE
const currentEnv = import.meta.env.VITE_APP_ENV
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
.login {
  --login-ink: #0f172a;
  --login-muted: #64748b;
  --login-soft: #94a3b8;
  --login-line: rgba(148, 163, 184, 0.35);
  --login-card-bg: rgba(255, 255, 255, 0.82);
  --login-card-border: rgba(255, 255, 255, 0.85);
  --login-card-shadow: 0 4px 6px -1px rgba(15, 23, 42, 0.05), 0 24px 48px -16px rgba(21, 128, 61, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  --login-accent: #16a34a;
  --login-accent-strong: #15803d;
  --login-accent-soft: rgba(22, 163, 74, 0.12);
  --login-input-bg: rgba(255, 255, 255, 0.92);
  --login-input-border: rgba(148, 163, 184, 0.35);
  --el-color-primary: #16a34a;
  --el-color-primary-light-3: #53b976;
  --el-color-primary-light-5: #8bd39f;
  --el-color-primary-light-7: #bce7c8;
  --el-color-primary-light-9: #eaf7ee;
  --el-color-primary-dark-2: #15803d;
  --el-fill-color-blank: #fff;
  --el-text-color-regular: #334155;
  --el-border-color: #cbd5e1;
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  color-scheme: only light;
  color: var(--login-ink);
  background:
    radial-gradient(circle at 28% 48%, rgba(134, 239, 172, 0.16), transparent 32%),
    linear-gradient(150deg, #f8faf9 0%, #eef8f1 48%, #f5faf7 100%);
}

.login-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 1360px;
  margin: 0 auto;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 400px);
  gap: clamp(16px, 3vw, 36px);
  align-items: center;
  padding: clamp(28px, 6vh, 72px) clamp(20px, 4vw, 40px) clamp(32px, 7vh, 80px);
  box-sizing: border-box;
}

.login-hero {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: clamp(20px, 3.5vh, 32px);
  max-width: 780px;
  justify-self: end;
  margin-bottom: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  color: inherit;
  background: transparent;
  font: inherit;
  line-height: normal;
}

@media (min-width: 921px) {
  .login-hero {
    transform: translateY(clamp(30px, 5vh, 48px));
  }
}

.login-hero__copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: clamp(0px, 1vw, 10px);
}

.login-hero__art {
  display: block;
  width: 100%;
  max-width: min(100%, 760px);
  height: auto;
  margin: -8px auto 0;
  pointer-events: none;
  user-select: none;
  opacity: 0.96;
  -webkit-mask-image: radial-gradient(
    ellipse 74% 70% at 50% 48%,
    #000 50%,
    rgba(0, 0, 0, 0.96) 67%,
    rgba(0, 0, 0, 0.48) 83%,
    transparent 100%
  );
  mask-image: radial-gradient(
    ellipse 74% 70% at 50% 48%,
    #000 50%,
    rgba(0, 0, 0, 0.96) 67%,
    rgba(0, 0, 0, 0.48) 83%,
    transparent 100%
  );
  filter: saturate(0.94) drop-shadow(0 16px 34px rgba(20, 83, 45, 0.13));
}

.login-hero__headline {
  display: flex;
  align-items: baseline;
  gap: 0.2em;
  margin: 0;
  font-size: clamp(2.9rem, 5vw, 4.6rem);
  font-weight: 760;
  line-height: 1.18;
  letter-spacing: -0.03em;
  color: #0f172a;
  animation: text-settle 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.12s both;
}

.login-hero__ai {
  color: var(--login-accent);
  font-weight: 850;
}

.login-hero__lede {
  margin: 0;
  font-size: clamp(1.08rem, 1.55vw, 1.38rem);
  font-weight: 450;
  line-height: 1.65;
  letter-spacing: 0.015em;
  color: #526175;
  animation: text-settle 0.9s cubic-bezier(0.22, 1, 0.36, 1) 0.2s both;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: min(100%, 560px);
}

.login-panel__inner {
  width: 100%;
  max-width: 400px;
  animation: panel-in 0.85s cubic-bezier(0.22, 1, 0.36, 1) 0.08s both;
}

.login-card {
  width: 100%;
  padding: 28px 28px 22px;
  border-radius: 20px;
  background: var(--login-card-bg);
  border: 1px solid var(--login-card-border);
  box-shadow: var(--login-card-shadow);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  position: relative;
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.55) 0%, transparent 42%, transparent 100%);
  opacity: 0.9;
}

.login-brand {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.login-brand__mark {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  animation: logo-breathe 5s ease-in-out infinite;
}

.login-brand__logo {
  width: 48px;
  height: 48px;
  object-fit: contain;
  filter: drop-shadow(0 6px 18px rgba(21, 128, 61, 0.18));
}

.login-title {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0.035em;
  color: var(--login-ink);
}

.login :deep(.login-env-tag.env-tag.env-tag-badge) {
  position: absolute;
  z-index: 2;
  top: 16px;
  right: 16px;
  height: 18px !important;
  padding: 0 6px !important;
  margin: 0 !important;
  font-size: 8px !important;
  letter-spacing: 0.08em !important;
  box-shadow: none !important;
}

.login :deep(.login-env-tag .env-tag-badge__inner) {
  gap: 4px;
}

.login :deep(.login-env-tag .env-tag-badge__inner::before) {
  width: 3px;
  height: 3px;
  box-shadow: none;
}

.login-form {
  position: relative;
  z-index: 1;

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__error) {
    padding-top: 4px;
    font-size: 12px;
    font-weight: 500;
    color: #dc2626;
    letter-spacing: 0.01em;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    background: var(--login-input-bg);
    box-shadow: 0 0 0 1px var(--login-input-border) inset;
    transition: box-shadow 0.28s ease, background 0.28s ease, transform 0.2s ease;
  }

  :deep(.el-input__wrapper:hover) {
    box-shadow: 0 0 0 1px rgba(22, 163, 74, 0.42) inset;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow:
      0 0 0 1px rgba(22, 163, 74, 0.58) inset,
      0 0 0 4px var(--login-accent-soft);
    background: #fff;
  }

  :deep(.el-input__inner) {
    color: #0f172a;
    caret-color: var(--login-accent);
  }

  :deep(.el-input__inner::placeholder) {
    color: #94a3b8;
  }

  .input-icon {
    height: 20px;
    width: 18px;
    margin-left: 2px;
    color: var(--login-soft);
    transition: color 0.22s ease;
  }

  :deep(.el-input__wrapper.is-focus) .input-icon {
    color: var(--login-accent);
  }
}

.captcha-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 104px;
  height: 40px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--login-input-border);
  flex-shrink: 0;
  background: var(--login-input-bg);
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.2s ease;

  &:hover {
    border-color: rgba(22, 163, 74, 0.5);
    box-shadow: 0 4px 14px rgba(22, 163, 74, 0.12);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 13px;

  :deep(.el-checkbox__label) {
    color: var(--login-muted);
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: var(--login-ink);
  }

  :deep(.el-checkbox__inner) {
    border-radius: 6px;
  }

  :deep(.el-checkbox:hover .el-checkbox__inner),
  :deep(.el-checkbox__input.is-focus .el-checkbox__inner) {
    border-color: var(--login-accent);
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: var(--login-accent);
    border-color: var(--login-accent);
  }
}

.link-type {
  color: var(--login-accent);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s ease, opacity 0.2s ease;

  &:hover {
    color: var(--login-accent-strong);
    opacity: 0.92;
  }
}

.login-btn-wrap {
  margin-bottom: 0;
}

.login-btn.el-button--primary {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.1em;
  border: none !important;
  border-radius: 12px;
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: transparent;
  --el-button-hover-border-color: transparent;
  --el-button-active-bg-color: transparent;
  --el-button-active-border-color: transparent;
  --el-button-disabled-bg-color: #8fba9b;
  --el-button-disabled-border-color: #8fba9b;
  color: #fff !important;
  background: linear-gradient(100deg, #1baa50 0%, #15803d 100%) !important;
  background-size: 160% 100% !important;
  box-shadow: 0 6px 20px rgba(21, 128, 61, 0.22), 0 0 0 1px rgba(255, 255, 255, 0.2) inset;
  transition: background 0.24s ease, transform 0.22s ease, box-shadow 0.25s ease;

  &:hover:not(.is-disabled):not(.is-loading) {
    background: linear-gradient(100deg, #179447 0%, #166534 100%) !important;
    transform: translateY(-1px);
    box-shadow: 0 10px 26px rgba(21, 128, 61, 0.27), 0 0 0 1px rgba(255, 255, 255, 0.26) inset;
  }

  &:active:not(.is-disabled):not(.is-loading) {
    background: #14532d !important;
    transform: translateY(0);
    box-shadow: 0 4px 12px rgba(20, 83, 45, 0.24);
  }

  &.is-disabled:not(.is-loading) {
    color: rgba(255, 255, 255, 0.92) !important;
    background: #8fba9b !important;
    box-shadow: none;
  }

  &.is-loading {
    background: linear-gradient(100deg, #22a955 0%, #197c3f 100%) !important;
    box-shadow: 0 6px 22px rgba(21, 128, 61, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.15) inset;
    animation: btn-loading-pulse 1.4s ease-in-out infinite;
  }
}

.login-footer {
  position: relative;
  z-index: 1;
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--login-soft);
}

.login-code-img {
  height: 40px;
}

@keyframes panel-in {
  from {
    opacity: 0;
    transform: translateY(16px) translateX(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0) translateX(0);
  }
}

@keyframes text-settle {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes logo-breathe {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

@keyframes btn-loading-pulse {
  0%,
  100% {
    box-shadow: 0 6px 22px rgba(21, 128, 61, 0.18), 0 0 0 1px rgba(255, 255, 255, 0.15) inset;
  }
  50% {
    box-shadow: 0 8px 28px rgba(21, 128, 61, 0.26), 0 0 0 1px rgba(255, 255, 255, 0.22) inset;
  }
}

@media (max-width: 920px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 440px;
    padding-top: clamp(20px, 4vh, 40px);
  }

  .login-hero {
    min-height: auto;
    order: 0;
    justify-self: center;
    align-items: center;
    text-align: center;
  }

  .login-hero__art {
    max-width: min(100%, 380px);
  }

  .login-hero__headline {
    font-size: clamp(2rem, 7vw, 2.45rem);
  }

  .login-hero__lede {
    font-size: 1rem;
  }

  .login-hero__copy {
    padding-left: 0;
  }

  .login-panel {
    order: 1;
    min-height: 0;
    align-items: flex-start;
    padding-bottom: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-panel__inner,
  .login-hero__headline,
  .login-hero__lede,
  .login-brand__mark,
  .login-btn.is-loading {
    animation: none !important;
  }

  .login-panel__inner,
  .login-hero__headline,
  .login-hero__lede {
    opacity: 1;
    transform: none;
  }

}

html.dark .login :deep(.login-env-tag.env-tag-badge.el-tag--warning) {
  color: #b45309 !important;
  border-color: rgba(251, 191, 36, 0.42) !important;
  background: linear-gradient(155deg, rgba(255, 255, 255, 0.82), rgba(254, 243, 199, 0.65)) !important;
}

html.dark .login :deep(.login-env-tag.env-tag-badge.el-tag--info) {
  color: #1d4ed8 !important;
  border-color: rgba(147, 197, 253, 0.55) !important;
  background: linear-gradient(155deg, rgba(255, 255, 255, 0.85), rgba(219, 234, 254, 0.62)) !important;
}
</style>
