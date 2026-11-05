import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { tokenStore } from '@/api/http';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer' },
      },
      {
        path: 'stations',
        name: 'stations',
        component: () => import('@/views/StationList.vue'),
        meta: { title: '站房管理', icon: 'OfficeBuilding', roles: ['ORG', 'MGR', 'REG'] },
      },
      {
        path: 'ops',
        name: 'ops',
        component: () => import('@/views/OpsTasks.vue'),
        meta: { title: '运维证据链', icon: 'Tickets', roles: ['ORG', 'MGR', 'ENG'] },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  document.title = `${(to.meta.title as string) ?? '工作台'} · 污染源自动监控运维管理系统`;
  if (to.meta.public) return true;
  if (!tokenStore.get()) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;
