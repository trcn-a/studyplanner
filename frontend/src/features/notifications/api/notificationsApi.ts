import { request } from '../../../shared/api/http';
export interface Notice { id: string; kind: string; title: string; body: string; createdAt: string; read: boolean }
export const notificationsApi = {
 refresh: () => request<Notice[]>('/notifications/refresh', 'POST'),
 read: (id: string) => request<Notice[]>(`/notifications/${id}/read`, 'POST'),
 readAll: () => request<Notice[]>('/notifications/read-all', 'POST'),
};
