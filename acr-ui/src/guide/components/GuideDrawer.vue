<template>
  <el-drawer
    v-model="drawerVisible"
    :size="drawerSize"
    :with-header="false"
    append-to-body
    class="guide-drawer"
    @closed="onClosed"
  >
    <div class="guide-drawer__inner">
      <header class="guide-drawer__header">
        <span class="guide-drawer__title">功能助手</span>
        <el-icon class="guide-drawer__close" @click="guideStore.close()"><Close /></el-icon>
      </header>

      <el-input
        v-model="keyword"
        class="guide-drawer__search"
        placeholder="搜索指引：如 webhook、token、注释"
        clearable
        :prefix-icon="Search"
      />

      <div v-if="keyword" class="guide-drawer__results">
        <div v-if="!searchResults.length" class="guide-drawer__empty">没有匹配的指引，换个关键词试试</div>
        <div
          v-for="doc in searchResults"
          :key="doc.id"
          class="guide-nav__item"
          :class="{ 'is-active': doc.id === guideStore.activeDocId }"
          @click="selectDoc(doc.id)"
        >{{ doc.title }}<span class="guide-nav__group">{{ groupTitle(doc.group) }}</span></div>
      </div>

      <template v-else>
        <div class="guide-drawer__tocbar" @click="tocOpen = !tocOpen">
          <span>指引目录</span>
          <el-icon class="guide-drawer__tocarrow" :class="{ 'is-open': tocOpen }"><ArrowDown /></el-icon>
        </div>
        <el-collapse v-show="tocOpen" v-model="openGroups" class="guide-drawer__nav">
          <el-collapse-item v-for="group in GUIDE_GROUPS" :key="group.key" :name="group.key" :title="group.title">
            <div
              v-for="id in group.docIds"
              :key="id"
              class="guide-nav__item"
              :class="{ 'is-active': id === guideStore.activeDocId }"
              @click="selectDoc(id)"
            >{{ findDoc(id)?.title }}</div>
          </el-collapse-item>
        </el-collapse>
      </template>

      <div class="guide-drawer__docbar">
        <span class="guide-drawer__docgroup">{{ groupTitle(activeDoc.group) }}</span>
        <span class="guide-drawer__doctitle">{{ activeDoc.title }}</span>
      </div>

      <div ref="contentScrollRef" class="guide-drawer__content">
        <GuideContent v-if="activeDoc" :key="activeDoc.id" :doc="activeDoc" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { Search, ArrowDown } from '@element-plus/icons-vue'
import { GUIDE_GROUPS, GUIDE_DOCS, DEFAULT_DOC_ID, findDoc } from '../manifest'
import useGuideStore from '@/store/modules/guide'
import GuideContent from './GuideContent.vue'
import useAppStore from '@/store/modules/app'
import { useRoute } from 'vue-router'

const guideStore = useGuideStore()
const appStore = useAppStore()
const route = useRoute()

/* 抽屉为全局浮层：切 tab（路由变化）时关闭，避免跨页残留 */
watch(() => route.fullPath, () => guideStore.close())

const keyword = ref('')
const openGroups = ref([])
const tocOpen = ref(true)
const contentScrollRef = ref(null)

const drawerSize = computed(() => (appStore.device === 'mobile' ? '100%' : '620px'))

const drawerVisible = computed({
  get: () => guideStore.visible,
  set: (v) => (v ? guideStore.open() : guideStore.close())
})

const activeDoc = computed(() => findDoc(guideStore.activeDocId) || findDoc(DEFAULT_DOC_ID))

/* 目录只展开当前篇目所在分组，减少视觉噪音、把空间留给正文 */
watch(activeDoc, (doc) => {
  if (doc?.group) {
    openGroups.value = [doc.group]
  }
}, { immediate: true })

const searchResults = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return []
  return GUIDE_DOCS.filter(d =>
    d.title.toLowerCase().includes(kw) || d.keywords.some(k => k.toLowerCase().includes(kw))
  )
})

function groupTitle(groupKey) {
  return GUIDE_GROUPS.find(g => g.key === groupKey)?.title || ''
}

function selectDoc(id) {
  guideStore.select(id)
  keyword.value = ''
  nextTick(() => { contentScrollRef.value && (contentScrollRef.value.scrollTop = 0) })
}

function onClosed() {
  keyword.value = ''
}
</script>

<style lang="scss" scoped>
.guide-drawer__inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.guide-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  padding: 16px 20px 12px;
}

.guide-drawer__title {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.guide-drawer__close {
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;

  &:hover {
    color: var(--text-primary);
  }
}

.guide-drawer__search {
  flex-shrink: 0;
  padding: 0 20px;
  margin-bottom: 12px;
}

/* 目录/搜索结果区：不挤压正文，最高占 40% 后自滚 */
.guide-drawer__results,
.guide-drawer__nav {
  flex-shrink: 0;
  max-height: 40%;
  overflow: auto;
  padding: 0 20px;
}

.guide-drawer__nav {
  --el-collapse-border-color: transparent;
}

/* 分组标题收成小标签样式，与篇目项拉开层级 */
.guide-drawer__nav :deep(.el-collapse-item__header) {
  height: 34px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.guide-drawer__nav :deep(.el-collapse-item__wrap) {
  margin-bottom: 4px;
}

/* 目录折叠开关：阅读时可整体收起目录，把空间让给正文 */
.guide-drawer__tocbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  margin: 0 20px;
  padding: 4px 12px;
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.01em;
  cursor: pointer;
  user-select: none;

  &:hover {
    background: var(--bg-hover);
    color: var(--text-regular);
  }
}

.guide-drawer__tocarrow {
  font-size: 14px;
  transition: transform 0.18s ease-out;

  &.is-open {
    transform: rotate(180deg);
  }
}

.guide-drawer__empty {
  padding: 24px 0;
  color: var(--text-secondary);
  font-size: 13px;
  text-align: center;
}

.guide-nav__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  line-height: 32px;
  padding: 0 12px;
  border-radius: 6px;
  color: var(--text-regular);
  font-size: 13px;
  cursor: pointer;

  &:hover {
    background: var(--bg-hover);
  }

  &.is-active {
    background: var(--brand-bg-soft);
    color: var(--brand-text);
  }
}

.guide-nav__group {
  color: var(--text-assist);
  font-size: 12px;
}

.guide-drawer__content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px 32px;
}

/* 常驻篇目栏：正文滚动时始终可见当前篇目 */
.guide-drawer__docbar {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 24px;
  border-top: 1px solid var(--divider);
  border-bottom: 1px solid var(--divider);
  background: var(--neutral-content);
}

.guide-drawer__docgroup {
  flex-shrink: 0;
  color: var(--text-assist);
  font-size: 12px;

  &::after {
    content: '/';
    margin-left: 8px;
    color: var(--border-default);
  }
}

.guide-drawer__doctitle {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
