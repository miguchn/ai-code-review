<template>
  <Teleport to="body">
    <div v-if="tourStore.activeTourId && focusRect" class="acr-tour" aria-live="polite">
      <div v-for="mask in masks" :key="mask.key" class="acr-tour__mask" :style="mask.style" />
      <div class="acr-tour__focus" :style="focusStyle" />
      <section ref="panelRef" class="acr-tour__panel" :style="panelStyle" role="dialog" :aria-label="currentStep?.title">
        <div class="acr-tour__meta">{{ activeTour?.title }} · {{ tourStore.stepIndex + 1 }}/{{ activeTour?.steps.length }}</div>
        <h3 class="acr-tour__title">{{ currentStep?.title }}</h3>
        <p class="acr-tour__content">{{ currentStep?.content }}</p>
        <div class="acr-tour__actions">
          <button type="button" class="acr-tour__skip" @click="finishTour">跳过</button>
          <div class="acr-tour__nav">
            <button type="button" :disabled="tourStore.stepIndex === 0" @click="move(-1)">上一步</button>
            <button type="button" class="is-primary" @click="move(1)">
              {{ isLastStep ? '完成' : '下一步' }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import useTourStore from '@/store/modules/tour'
import useUserStore from '@/store/modules/user'
import { completionStorageKey, findTour, pickAutoTourId } from '../manifest'

const router = useRouter()
const route = useRoute()
const tourStore = useTourStore()
const userStore = useUserStore()

const panelRef = ref(null)
const focusRect = ref(null)
const panelPosition = ref({ top: 12, left: 12 })
const activeTour = computed(() => findTour(tourStore.activeTourId))
const currentStep = computed(() => activeTour.value?.steps[tourStore.stepIndex] || null)
const isLastStep = computed(() => tourStore.stepIndex === (activeTour.value?.steps.length || 1) - 1)

let targetElement = null
let resizeObserver = null
let activationToken = 0
let navigationDirection = 1
let autoStartTimer = null
let autoStartCheckedFor = null

const focusStyle = computed(() => ({
  top: `${focusRect.value.top}px`,
  left: `${focusRect.value.left}px`,
  width: `${focusRect.value.width}px`,
  height: `${focusRect.value.height}px`
}))

const panelStyle = computed(() => ({
  top: `${panelPosition.value.top}px`,
  left: `${panelPosition.value.left}px`
}))

const masks = computed(() => {
  const rect = focusRect.value
  if (!rect) return []
  const right = rect.left + rect.width
  const bottom = rect.top + rect.height
  return [
    { key: 'top', style: { top: '0', left: '0', width: '100vw', height: `${rect.top}px` } },
    { key: 'left', style: { top: `${rect.top}px`, left: '0', width: `${rect.left}px`, height: `${rect.height}px` } },
    { key: 'right', style: { top: `${rect.top}px`, left: `${right}px`, width: `${Math.max(0, window.innerWidth - right)}px`, height: `${rect.height}px` } },
    { key: 'bottom', style: { top: `${bottom}px`, left: '0', width: '100vw', height: `${Math.max(0, window.innerHeight - bottom)}px` } }
  ]
})

function delay(ms) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

async function waitForElement(selector, token, attempts = 12) {
  for (let index = 0; index < attempts; index += 1) {
    if (token !== activationToken) return null
    const element = document.querySelector(selector)
    if (element && element.getClientRects().length) return element
    await delay(100)
  }
  return null
}

function clearTarget() {
  resizeObserver?.disconnect()
  resizeObserver = null
  targetElement = null
  focusRect.value = null
}

function updatePosition() {
  if (!targetElement || !targetElement.isConnected) return
  const rect = targetElement.getBoundingClientRect()
  const padding = 6
  const top = Math.max(4, rect.top - padding)
  const left = Math.max(4, rect.left - padding)
  const right = Math.min(window.innerWidth - 4, rect.right + padding)
  const bottom = Math.min(window.innerHeight - 4, rect.bottom + padding)
  focusRect.value = {
    top,
    left,
    width: Math.max(0, right - left),
    height: Math.max(0, bottom - top)
  }
  nextTick(updatePanelPosition)
}

function updatePanelPosition() {
  if (!focusRect.value) return
  const gap = 14
  const margin = 12
  const width = Math.min(360, window.innerWidth - margin * 2)
  const height = panelRef.value?.offsetHeight || 190
  const centeredLeft = focusRect.value.left + (focusRect.value.width - width) / 2
  const left = Math.max(margin, Math.min(centeredLeft, window.innerWidth - width - margin))
  const below = focusRect.value.top + focusRect.value.height + gap
  const top = below + height <= window.innerHeight - margin
    ? below
    : Math.max(margin, focusRect.value.top - height - gap)
  panelPosition.value = { top, left }
}

function completeStorage() {
  if (!userStore.id || !tourStore.activeTourId) return
  const key = completionStorageKey(userStore.id)
  let completed = []
  try {
    completed = JSON.parse(localStorage.getItem(key) || '[]')
    if (!Array.isArray(completed)) completed = []
  } catch {
    completed = []
  }
  if (!completed.includes(tourStore.activeTourId)) completed.push(tourStore.activeTourId)
  localStorage.setItem(key, JSON.stringify(completed))
}

function finishTour() {
  completeStorage()
  activationToken += 1
  clearTarget()
  tourStore.stop()
}

function move(direction) {
  if (!activeTour.value) return
  if (direction > 0 && isLastStep.value) {
    finishTour()
    return
  }
  const nextIndex = tourStore.stepIndex + direction
  if (nextIndex < 0 || nextIndex >= activeTour.value.steps.length) return
  navigationDirection = direction
  tourStore.goTo(nextIndex)
}

function skipMissingStep() {
  const steps = activeTour.value?.steps || []
  const nextIndex = tourStore.stepIndex + navigationDirection
  if (nextIndex >= 0 && nextIndex < steps.length) {
    tourStore.goTo(nextIndex)
    return
  }
  if (navigationDirection < 0) {
    navigationDirection = 1
    const fallbackIndex = Math.min(tourStore.stepIndex + 1, steps.length - 1)
    if (fallbackIndex !== tourStore.stepIndex) {
      tourStore.goTo(fallbackIndex)
      return
    }
  }
  finishTour()
}

async function activateStep() {
  const step = currentStep.value
  if (!step) {
    finishTour()
    return
  }
  const token = ++activationToken
  clearTarget()

  const destination = router.resolve(step.route).fullPath
  if (route.fullPath !== destination) {
    await router.push(step.route)
    await nextTick()
  }
  if (token !== activationToken) return

  let element = document.querySelector(step.selector)
  if ((!element || !element.getClientRects().length) && step.prepare?.click) {
    const trigger = await waitForElement(step.prepare.click, token)
    if (trigger) {
      trigger.click()
      await delay(260)
    }
  }
  element = await waitForElement(step.selector, token)
  if (token !== activationToken) return
  if (!element) {
    skipMissingStep()
    return
  }

  targetElement = element
  targetElement.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
  await delay(180)
  if (token !== activationToken) return
  updatePosition()
  resizeObserver = new ResizeObserver(updatePosition)
  resizeObserver.observe(targetElement)
}

function scheduleAutoStart() {
  const userId = userStore.id
  if (!userId || autoStartCheckedFor === userId || tourStore.activeTourId) return
  autoStartCheckedFor = userId
  const tourId = pickAutoTourId({ roles: userStore.roles, permissions: userStore.permissions })
  let completed = []
  try {
    completed = JSON.parse(localStorage.getItem(completionStorageKey(userId)) || '[]')
  } catch {
    completed = []
  }
  if (Array.isArray(completed) && completed.includes(tourId)) return

  const tryStart = () => {
    if (!document.querySelector('.el-overlay-message-box')) {
      tourStore.start(tourId)
      return
    }
    autoStartTimer = window.setTimeout(tryStart, 500)
  }
  autoStartTimer = window.setTimeout(tryStart, 700)
}

function handleKeydown(event) {
  if (event.key === 'Escape' && tourStore.activeTourId) finishTour()
}

watch([() => tourStore.activeTourId, () => tourStore.stepIndex], activateStep, { flush: 'post' })
watch(() => userStore.id, scheduleAutoStart, { immediate: true })

onMounted(() => {
  window.addEventListener('resize', updatePosition)
  window.addEventListener('scroll', updatePosition, true)
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  activationToken += 1
  clearTarget()
  window.clearTimeout(autoStartTimer)
  window.removeEventListener('resize', updatePosition)
  window.removeEventListener('scroll', updatePosition, true)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style lang="scss">
.acr-tour {
  position: fixed;
  inset: 0;
  z-index: 5000;
  pointer-events: none;
}

.acr-tour__mask {
  position: fixed;
  background: var(--el-overlay-color-lighter);
  pointer-events: none;
}

.acr-tour__focus {
  position: fixed;
  border: 2px solid var(--el-color-primary);
  border-radius: var(--radius-control, 8px);
  box-shadow: var(--el-box-shadow-light);
  pointer-events: none;
  transition: inset 0.16s ease, width 0.16s ease, height 0.16s ease;
}

.acr-tour__panel {
  position: fixed;
  width: min(360px, calc(100vw - 24px));
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: var(--radius-card, 12px);
  background: var(--el-bg-color-overlay);
  box-shadow: var(--el-box-shadow-dark);
  color: var(--el-text-color-primary);
  pointer-events: auto;
}

.acr-tour__meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.acr-tour__title {
  margin: 6px 0 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
  line-height: 24px;
}

.acr-tour__content {
  margin: 8px 0 16px;
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 22px;
}

.acr-tour__actions,
.acr-tour__nav {
  display: flex;
  align-items: center;
}

.acr-tour__actions {
  justify-content: space-between;
  gap: 12px;
}

.acr-tour__nav {
  gap: 8px;
}

.acr-tour__actions button {
  min-height: 32px;
  padding: 0 13px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--radius-control, 6px);
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  cursor: pointer;
}

.acr-tour__actions button:hover:not(:disabled) {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.acr-tour__actions button:disabled {
  color: var(--el-text-color-disabled);
  cursor: not-allowed;
}

.acr-tour__actions .is-primary {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: var(--el-color-white);
}

.acr-tour__actions .is-primary:hover {
  border-color: var(--el-color-primary-light-3);
  background: var(--el-color-primary-light-3);
  color: var(--el-color-white);
}

.acr-tour__skip {
  border-color: transparent !important;
  background: transparent !important;
  color: var(--el-text-color-secondary) !important;
}
</style>
