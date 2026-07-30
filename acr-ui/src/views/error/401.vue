<template>
  <div class="errPage-container">
    <el-button icon="arrow-left" class="pan-back-btn" @click="back">
      返回
    </el-button>
    <el-row>
      <el-col :xs="24" :md="12">
        <h1 class="text-jumbo text-ginormous">
          401错误!
        </h1>
        <h2>您没有访问权限！</h2>
        <h6>对不起，您没有访问权限，请不要进行非法操作！您可以返回主页面</h6>
        <ul class="list-unstyled">
          <li class="link-type">
            <router-link to="/">
              回首页
            </router-link>
          </li>
        </ul>
      </el-col>
      <el-col :xs="24" :md="12" class="error-illustration">
        <img :src="errGif" alt="无访问权限">
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import errImage from "@/assets/401_images/401.gif"

let { proxy } = getCurrentInstance()

const errGif = ref(errImage + "?" + +new Date())

function back() {
  if (proxy.$route.query.noGoBack) {
    proxy.$router.push({ path: "/" })
  } else {
    proxy.$router.go(-1)
  }
}
</script>

<style lang="scss" scoped>
.errPage-container {
  width: 960px;
  max-width: calc(100% - 32px);
  margin: 64px auto;
  padding: 32px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background: var(--neutral-card);
  .pan-back-btn {
    margin-bottom: 24px;
    background: var(--brand-600);
    color: var(--brand-on-solid);
    border: none !important;
    &:hover {
      background: var(--brand-700);
    }
  }
  .pan-gif {
    margin: 0 auto;
    display: block;
  }
  .pan-img {
    display: block;
    margin: 0 auto;
    width: 100%;
  }
  .text-jumbo {
    margin: 24px 0 16px;
    font-size: 40px;
    line-height: 48px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .list-unstyled {
    font-size: 14px;
    li {
      padding-bottom: 5px;
    }
    a {
      color: var(--brand-600);
      text-decoration: none;
      &:hover {
        text-decoration: underline;
      }
    }
  }

  h2 {
    color: var(--text-primary);
    font-weight: 600;
  }

  h6 {
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 400;
    line-height: 22px;
  }

  .error-illustration {
    text-align: center;

    img {
      width: min(280px, 80%);
      height: auto;
    }
  }
}

@media (max-width: 767px) {
  .errPage-container {
    margin: 24px auto;
    padding: 24px 20px;

    .text-jumbo {
      font-size: 32px;
      line-height: 40px;
    }

    .error-illustration {
      margin-top: 24px;
    }
  }
}
</style>
