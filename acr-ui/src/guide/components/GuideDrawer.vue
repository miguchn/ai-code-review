<template>
  <el-drawer
    v-model="drawerVisible"
    :size="drawerSize"
    :with-header="false"
    append-to-body
    custom-class="guide-drawer"
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

      <el-collapse v-else v-model="openGroups" class="guide-drawer__nav">
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

      <div ref="contentScrollRef" class="guide-drawer__content">
        <GuideContent v-if="activeDoc" :key="activeDoc.id" :doc="activeDoc" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { GUIDE_GROUPS, GUIDE_DOCS, DEFAULT_DOC_ID, findDoc } from '../manifest'
import useGuideStore from '@/store/modules/guide'
import GuideContent from './GuideContent.vue'
import useAppStore from '@/store/modules/app'

const guideStore = useGuideStore()
const appStore = useAppStore()

const keyword = ref('')
const openGroups = ref(GUIDE_GROUPS.map(g => g.key))
const contentScrollRef = ref(null)

const drawerSize = computed(() => (appStore.device === 'mobile' ? '100%' : '560px'))

const drawerVisible = computed({
  get: () => guideStore.visible,
  set: (v) => (v ? guideStore.open() : guideStore.close())
})

const activeDoc = computed(() => findDoc(guideStore.activeDocId) || findDoc(DEFAULT_DOC_ID))

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
  padding: 14px 20px 10px;
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
  margin-bottom: 8px;
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
  padding: 4px 20px 24px;
}
</style>
