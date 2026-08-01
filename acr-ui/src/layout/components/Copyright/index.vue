<template>
  <footer v-if="visible" class="copyright">
    <span>{{ content }}</span>
  </footer>
</template>

<script setup>
import useSettingsStore from '@/store/modules/settings'

const settingsStore = useSettingsStore()

const visible = computed(() => settingsStore.footerVisible)
const content = computed(() => settingsStore.footerContent)
</script>

<style scoped>
.copyright {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 24px;
  text-align: center;
  background-color: var(--neutral-content);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
  letter-spacing: 0.02em;
  border-top: 1px solid var(--divider);
  z-index: 999;
}
</style>
