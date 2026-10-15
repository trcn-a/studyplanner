import { request } from '../../../shared/api/http';
export interface GoogleStatus { configured: boolean; connected: boolean; lastSync: string | null; error: string | null }
const call = <T,>(path = '', method = 'GET') => request<T>('/google-calendar' + path, method);
export const googleCalendarApi = {
 status: () => call<GoogleStatus>(),
 connect: () => call<{ url: string }>('/connect', 'POST'),
 sync: () => call<GoogleStatus>('/sync', 'POST'),
 disconnect: () => call<GoogleStatus>('', 'DELETE'),
};
