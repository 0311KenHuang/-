import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  traceId: string;
}

const TOKEN_KEY = 'env_token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY) ?? '',
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const client = axios.create({ baseURL: '/api', timeout: 20000 });

client.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ApiEnvelope<null>>) => {
    const status = error.response?.status;
    if (status === 401) {
      tokenStore.clear();
      if (!location.pathname.startsWith('/login')) {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname)}`;
      }
    }
    const msg = error.response?.data?.message ?? error.message ?? '网络异常';
    ElMessage.error(msg);
    return Promise.reject(error);
  },
);

/** 统一拆包：只把 data 交给业务层 */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await client.request<ApiEnvelope<T>>(config);
  const body = res.data;
  if (body.code !== 0) {
    ElMessage.error(body.message || '请求失败');
    throw new Error(body.message);
  }
  return body.data;
}

export const http = {
  get: <T>(url: string, params?: unknown) => request<T>({ url, method: 'GET', params }),
  post: <T>(url: string, data?: unknown) => request<T>({ url, method: 'POST', data }),
  patch: <T>(url: string, data?: unknown) => request<T>({ url, method: 'PATCH', data }),
  del: <T>(url: string) => request<T>({ url, method: 'DELETE' }),
};
