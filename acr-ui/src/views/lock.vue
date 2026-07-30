<template>
  <div class="lock-container">
    <!-- 时钟 -->
    <div class="lock-time">{{ currentTime }}</div>
    <div class="lock-date">{{ currentDate }}</div>

    <!-- 锁屏卡片 -->
    <div class="lock-card">
      <div class="avatar-wrap">
        <img :src="userStore.avatar" class="lock-avatar" @error="onAvatarError" />
        <div class="lock-icon"><el-icon><Lock /></el-icon></div>
      </div>
      <div class="lock-username">{{ userStore.nickName }}</div>
      <div class="lock-hint">系统已锁定，请输入密码解锁</div>

      <div class="input-wrap">
        <input ref="passwordInput" v-model="password" type="password" placeholder="请输入登录密码" class="lock-input" @keydown.enter="handleUnlock" autocomplete="off" />
        <button class="unlock-btn" @click="handleUnlock" :disabled="loading">
          <span v-if="!loading">→</span>
          <span v-else class="loading-dot">···</span>
        </button>
      </div>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

      <div class="lock-footer">
        <a href="javascript:;" @click="goLogin">退出重新登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import { unlockScreen } from '@/api/login'
import defAva from '@/assets/images/profile.jpg'

const router = useRouter()
const userStore = useUserStore()
const lockStore = useLockStore()

const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const currentTime = ref('')
const currentDate = ref('')
const passwordInput = ref(null)

let timer = null

const onAvatarError = (e) => {
  e.target.src = defAva
}

const startClock = () => {
  const update = () => {
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    currentTime.value = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
    const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    currentDate.value = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${days[now.getDay()]}`
  }
  update()
  timer = setInterval(update, 1000)
}

const handleUnlock = async () => {
  if (!password.value) {
    showError('请输入密码')
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await unlockScreen(password.value)
    const lockPath = lockStore.lockPath
    lockStore.unlockScreen()
    router.replace(lockPath)
  } catch (err) {
    const msg = err.message || err.toString()
    showError(msg)
    password.value = ''
    nextTick(() => passwordInput.value?.focus())
  } finally {
    loading.value = false
  }
}

const showError = (msg) => {
  errorMsg.value = msg
}

const goLogin = () => {
  lockStore.unlockScreen()
  userStore.logOut().then(() => {
    router.push('/login')
  })
}

onMounted(() => {
  startClock()
  nextTick(() => passwordInput.value?.focus())
})

onBeforeUnmount(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.lock-container {
  position: fixed;
  inset: 0;
  background: var(--neutral-page);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 32px 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

.lock-time {
  position: relative;
  z-index: 1;
  margin-bottom: 4px;
  color: var(--text-primary);
  font-size: 52px;
  font-weight: 500;
  line-height: 1.15;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.lock-date {
  position: relative;
  z-index: 1;
  margin-bottom: 32px;
  color: var(--text-secondary);
  font-size: 14px;
}

.lock-card {
  position: relative;
  z-index: 1;
  width: min(400px, calc(100vw - 32px));
  padding: 32px;
  background: var(--neutral-card);
  border: 1px solid var(--border-default);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: var(--shadow-float);
}

.avatar-wrap {
  position: relative;
  margin-bottom: 16px;
}

.lock-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid var(--brand-border);
  object-fit: cover;
  display: block;
}

.lock-icon {
  position: absolute;
  bottom: -4px;
  right: -4px;
  background: var(--brand-bg-soft);
  color: var(--brand-600);
  border-radius: 50%;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  border: 1px solid var(--brand-border);
}

.lock-username {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

.lock-hint {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 24px;
}

.input-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  min-height: 40px;
  background: var(--neutral-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  padding: 2px 2px 2px 12px;
  transition: border-color 0.15s ease-out, box-shadow 0.15s ease-out;
}

.input-wrap:focus-within {
  border-color: var(--brand-500);
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.14);
}

.lock-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  padding: 8px 0;
}

.lock-input::placeholder {
  color: var(--text-placeholder);
}

.unlock-btn {
  width: 42px;
  height: 36px;
  border-radius: var(--radius-control);
  background: var(--brand-600);
  border: none;
  color: var(--brand-on-solid);
  font-size: 18px;
  cursor: pointer;
  transition: background-color 0.15s ease-out, box-shadow 0.15s ease-out;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.unlock-btn:hover:not(:disabled) {
  background: var(--brand-700);
}

.unlock-btn:focus-visible {
  outline: 1px solid var(--brand-500);
  outline-offset: 2px;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.16);
}

.unlock-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-dot {
  font-size: 13px;
  letter-spacing: 1px;
}

.error-msg {
  margin-top: 14px;
  color: var(--status-danger-text);
  font-size: 13px;
  text-align: center;
}

.lock-footer {
  margin-top: 24px;
}

.lock-footer a {
  color: var(--brand-600);
  font-size: 13px;
  text-decoration: underline;
  text-underline-offset: 3px;
  transition: color 0.15s ease-out;
}

.lock-footer a:hover {
  color: var(--brand-700);
}

@media (max-height: 680px) {
  .lock-date {
    margin-bottom: 20px;
  }

  .lock-card {
    padding-top: 24px;
    padding-bottom: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .input-wrap,
  .unlock-btn,
  .lock-footer a {
    transition: none;
  }
}
</style>
