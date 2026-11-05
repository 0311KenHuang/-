import { defineStore } from 'pinia';
import { authApi, type LoginPayload, type LoginResult } from '@/api';
import { tokenStore } from '@/api/http';

interface UserState {
  id: number | null;
  username: string;
  realName: string;
  roles: string[];
  dataScope: string;
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: {
      id: null,
      username: '',
      realName: '',
      roles: [] as string[],
      dataScope: '',
    } as UserState,
  }),

  getters: {
    isLogin: () => !!tokenStore.get(),
    hasRole: (state) => (roles: string[]) =>
      state.user.roles.includes('SUP') || roles.some((r) => state.user.roles.includes(r)),
  },

  actions: {
    async login(payload: LoginPayload) {
      const res: LoginResult = await authApi.login(payload);
      tokenStore.set(res.token);
      this.user = { ...res.user };
    },

    logout() {
      tokenStore.clear();
      this.user = { id: null, username: '', realName: '', roles: [], dataScope: '' };
    },
  },
});
