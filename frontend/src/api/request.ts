import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

const instance = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

instance.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || error.message || '网络请求失败';
    return Promise.reject(new Error(message));
  }
);

export async function post<T>(url: string, data?: unknown): Promise<T> {
  const res = await instance.post<{ success: boolean; data: T }>(url, data);
  return res.data as T;
}

export default instance;
