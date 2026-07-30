<template>
  <div class="wscn-http404-container">
    <div class="wscn-http404">
      <div class="pic-404">
        <img class="pic-404__parent" src="@/assets/404_images/404.png" alt="404">
        <img class="pic-404__child left" src="@/assets/404_images/404_cloud.png" alt="404">
        <img class="pic-404__child mid" src="@/assets/404_images/404_cloud.png" alt="404">
        <img class="pic-404__child right" src="@/assets/404_images/404_cloud.png" alt="404">
      </div>
      <div class="bullshit">
        <div class="bullshit__oops">
          404错误!
        </div>
        <div class="bullshit__headline">
          {{ message }}
        </div>
        <div class="bullshit__info">
          对不起，您正在寻找的页面不存在。尝试检查URL的错误，然后按浏览器上的刷新按钮或尝试在我们的应用程序中找到其他内容。
        </div>
        <router-link to="/index" class="bullshit__return-home">
          返回首页
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
let message = computed(() => {
  return '找不到网页！'
})
</script>

<style lang="scss" scoped>
.wscn-http404-container {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--neutral-page);
}

.wscn-http404 {
  width: min(960px, 100%);
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(280px, 1fr);
  align-items: center;
  gap: 48px;
  padding: 40px;
  overflow: hidden;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background: var(--neutral-card);

  .pic-404 {
    position: relative;
    width: 100%;
    overflow: hidden;

    &__parent {
      display: block;
      width: 100%;
    }

    &__child {
      display: none;
    }
  }

  .bullshit {
    min-width: 0;

    &__oops {
      margin-bottom: 12px;
      color: var(--brand-600);
      font-size: 32px;
      line-height: 40px;
      font-weight: 600;
    }

    &__headline {
      margin-bottom: 8px;
      color: var(--text-primary);
      font-size: 20px;
      line-height: 28px;
      font-weight: 600;
    }

    &__info {
      margin-bottom: 24px;
      color: var(--text-secondary);
      font-size: 14px;
      line-height: 22px;
    }

    &__return-home {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 110px;
      height: 36px;
      padding: 0 16px;
      border-radius: var(--radius-control);
      color: var(--brand-on-solid);
      background: var(--brand-600);
      font-size: 14px;
      font-weight: 500;
      transition: background-color 0.15s ease-out, box-shadow 0.15s ease-out;

      &:hover {
        background: var(--brand-700);
      }

      &:focus-visible {
        outline: 1px solid var(--brand-500);
        outline-offset: 2px;
        box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.16);
      }
    }
  }
}

@media (max-width: 767px) {
  .wscn-http404-container {
    padding: 16px;
  }

  .wscn-http404 {
    grid-template-columns: 1fr;
    gap: 24px;
    padding: 24px 20px;

    .pic-404 {
      max-width: 280px;
      margin: 0 auto;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .wscn-http404 .bullshit__return-home {
    transition: none;
  }
}
</style>
