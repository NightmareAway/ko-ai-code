<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="200px">
        <div class="title-bar">
          <img class="logo" src="../assets/logo.png" alt="logo" />
          <div class="title">ko项目</div>
        </div>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
          style="border-bottom: none; line-height: 64px"
        />
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
          <a-button type="primary">登录</a-button>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { h, ref } from 'vue'
import { HomeOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'

const router = useRouter()
const current = ref<string[]>(['/'])

const items = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/about',
    icon: () => h(InfoCircleOutlined),
    label: '关于我们',
    title: '关于我们',
  },
])

const doMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}
</script>

<style scoped>
#globalHeader {
  width: 100%;
  padding: 0 24px;
}

.title-bar {
  display: flex;
  align-items: center;
}

.logo {
  height: 32px;
  margin-right: 16px;
}

.title {
  color: black;
  font-size: 18px;
  font-weight: bold;
}

.user-login-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
</style>
