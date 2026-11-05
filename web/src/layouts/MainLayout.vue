<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const collapsed = ref(false);

const menus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer', roles: [] as string[] },
  { path: '/stations', title: '站房管理', icon: 'OfficeBuilding', roles: ['ORG', 'MGR', 'REG'] },
  { path: '/ops', title: '运维证据链', icon: 'Tickets', roles: ['ORG', 'MGR', 'ENG'] },
];

const visibleMenus = computed(() => menus.filter((m) => !m.roles.length || auth.hasRole(m.roles)));

async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' });
  auth.logout();
  router.push({ name: 'login' });
}
</script>

<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '208px'" class="aside">
      <div class="brand">
        <span class="dot" />
        <span v-show="!collapsed" class="brand-text">污染源运维</span>
      </div>
      <el-menu :default-active="route.path" :collapse="collapsed" router class="menu">
        <el-menu-item v-for="m in visibleMenus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-tag v-if="auth.user.roles.length" size="small" type="info">
            {{ auth.user.roles.join(' / ') }}
          </el-tag>
          <el-dropdown>
            <span class="user">{{ auth.user.realName || auth.user.username || '未登录' }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #0f1b2d;
  transition: width 0.2s;
  display: flex;
  flex-direction: column;
}
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #e6eefc;
  font-weight: 600;
  letter-spacing: 0.02em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.dot {
  width: 9px;
  height: 9px;
  border-radius: 2px;
  background: #4d8dff;
  box-shadow: 0 0 0 3px rgba(77, 141, 255, 0.2);
}
.brand-text {
  font-size: 14px;
}
.menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #b6c4da;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
}
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8edf4;
}
.left,
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  cursor: pointer;
  font-size: 18px;
  color: #5b6b82;
}
.user {
  cursor: pointer;
  font-size: 13px;
  color: #16202e;
}
.main {
  background: #f5f8fc;
  padding: 16px;
}
</style>
