export const useGuideStore = defineStore('guide', {
  state: () => ({
    visible: false,
    activeDocId: null
  }),
  actions: {
    /** 打开抽屉；docId 为空时保持当前选中或回退默认篇 */
    open(docId = null) {
      this.activeDocId = docId || this.activeDocId || 'quick-start-first-repo'
      this.visible = true
    },
    close() {
      this.visible = false
    },
    select(docId) {
      this.activeDocId = docId
    }
  }
})

export default useGuideStore
