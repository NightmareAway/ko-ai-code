<template>
  <div id="basicLayout">
    <a-layout class="layout">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">
        <GlobalFooter />
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
#basicLayout {
  min-height: 100vh;
}

.layout {
  min-height: 100vh;
}

.header {
  padding: 0;
  background: #fff;
  box-shadow: 0 2px 8px #f0f1f2;
  position: sticky;
  top: 0;
  z-index: 10;
}

.content {
  background: linear-gradient(to right, #fefefe, #fff);
  padding: 24px;
  margin-bottom: 60px; /* Space for fixed footer */
}

.footer {
  padding: 0;
  background: #efefef;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  z-index: 10;
}
</style>
