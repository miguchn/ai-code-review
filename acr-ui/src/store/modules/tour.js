import { findTour } from '@/tour/manifest'

export const useTourStore = defineStore('tour', {
  state: () => ({
    activeTourId: null,
    stepIndex: 0
  }),
  actions: {
    start(tourId) {
      if (!findTour(tourId)) return
      this.activeTourId = tourId
      this.stepIndex = 0
    },
    goTo(stepIndex) {
      this.stepIndex = stepIndex
    },
    stop() {
      this.activeTourId = null
      this.stepIndex = 0
    }
  }
})

export default useTourStore
