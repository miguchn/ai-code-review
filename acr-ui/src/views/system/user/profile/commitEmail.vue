<template>
  <div class="commit-email-panel">
    <p class="hint">关联你用这些邮箱提交的代码，即可查看自己的审查数据。</p>

    <section class="block">
      <h4>已关联</h4>
      <el-empty v-if="!identities.length" description="尚未关联提交邮箱" :image-size="64" />
      <div v-for="item in identities" :key="item.id" class="identity-row">
        <div class="identity-main">
          <span class="identity-id">{{ item.identifier }}</span>
          <span v-if="item.displayName" class="identity-name">{{ item.displayName }}</span>
          <el-tag size="small" effect="plain" class="origin-tag">{{ originLabel(item.origin) }}</el-tag>
        </div>
        <el-button type="danger" link @click="remove(item)">移除</el-button>
      </div>
    </section>

    <section class="block">
      <h4>建议关联</h4>
      <el-empty v-if="!candidates.length" description="暂无匹配建议" :image-size="64" />
      <div v-for="item in candidates" :key="item.identifier" class="candidate-card">
        <div class="identity-id">{{ item.identifier }}</div>
        <div class="sample">
          {{ item.sampleProjectName || '仓库' }} · {{ item.sampleMessage || '提交消息' }} · {{ item.sampleTime || '--' }}
        </div>
        <el-button type="primary" size="small" @click="confirmCandidate(item)">确认关联</el-button>
      </div>
    </section>

    <section class="block">
      <h4>手动添加</h4>
      <el-form @submit.prevent="addManual">
        <el-input
          v-model="manualInput"
          placeholder="输入提交邮箱或 Git 名称，回车添加"
          clearable
          @keyup.enter="addManual"
        >
          <template #append>
            <el-button type="primary" @click="addManual">添加</el-button>
          </template>
        </el-input>
      </el-form>
      <p v-if="conflictMsg" class="conflict">{{ conflictMsg }}</p>
    </section>

    <section class="block im-reserve">
      <h4>我的 IM 账号</h4>
      <p class="hint muted">即将支持</p>
    </section>
  </div>
</template>

<script setup>
import {
  addMyIdentity,
  listIdentityCandidates,
  listMyIdentities,
  removeMyIdentity
} from '@/api/system/identity'

const identities = ref([])
const candidates = ref([])
const manualInput = ref('')
const conflictMsg = ref('')

const originMap = {
  SELF: '自己添加',
  AUTO: '系统建议',
  ADMIN: '管理员指派'
}

function originLabel(origin) {
  return originMap[origin] || origin || '自己添加'
}

async function reload() {
  conflictMsg.value = ''
  const [mineRes, candRes] = await Promise.all([listMyIdentities(), listIdentityCandidates()])
  identities.value = mineRes.data || []
  candidates.value = candRes.data || []
}

async function confirmCandidate(item) {
  conflictMsg.value = ''
  try {
    await addMyIdentity({
      identifier: item.identifier,
      displayName: item.displayName,
      origin: 'AUTO'
    })
    await reload()
  } catch (e) {
    conflictMsg.value = e?.message || e?.msg || '关联失败'
  }
}

async function addManual() {
  const value = (manualInput.value || '').trim()
  if (!value) return
  conflictMsg.value = ''
  try {
    await addMyIdentity({ identifier: value })
    manualInput.value = ''
    await reload()
  } catch (e) {
    conflictMsg.value = e?.message || e?.msg || '关联失败'
  }
}

async function remove(item) {
  await removeMyIdentity(item.id)
  await reload()
}

onMounted(reload)
</script>

<style scoped lang="scss">
.commit-email-panel {
  .hint {
    margin: 0 0 16px;
    color: var(--text-secondary, #64748b);
    font-size: 13px;
  }
  .muted { color: var(--text-assist, #7e8b84); }
  .block {
    margin-bottom: 22px;
    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: var(--text-primary, #0f172a);
    }
  }
  .identity-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border: 1px solid var(--border-light, #e5eae7);
    border-radius: 8px;
    margin-bottom: 8px;
  }
  .identity-main { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
  .identity-id { font-weight: 600; color: var(--text-primary, #0f172a); }
  .identity-name { color: var(--text-secondary, #64748b); font-size: 13px; }
  .origin-tag { color: #2f7650; border-color: #cfe5d5; background: #f0f7f2; }
  .candidate-card {
    border: 1px solid var(--border-light, #e5eae7);
    border-radius: 8px;
    padding: 12px 14px;
    margin-bottom: 10px;
    .sample {
      margin: 6px 0 10px;
      color: var(--text-secondary, #64748b);
      font-size: 12px;
    }
  }
  .conflict {
    margin-top: 8px;
    color: #c2413a;
    font-size: 13px;
  }
  .im-reserve {
    opacity: 0.72;
    border-top: 1px solid var(--divider, #ebefed);
    padding-top: 16px;
  }
}
</style>
