import { request } from '../../../shared/api/http';
export interface Account { id: string; email: string; name: string; remindersEnabled: boolean; reminderMinutes: number; studyDays: number }
export const accountApi = {
 me: () => request<Account>('/auth/me'),
 login: (email: string, password: string) => request<Account>('/auth/login', 'POST', { email, password }),
 register: (email: string, name: string, password: string, claimCode: string) => request<Account>('/auth/register', 'POST', { email, name, password, claimCode }),
 logout: () => request<void>('/auth/logout', 'POST'),
 profile: (name: string, remindersEnabled: boolean, reminderMinutes: number, studyDays: number) => request<Account>('/auth/profile', 'PUT', { name, remindersEnabled, reminderMinutes, studyDays }),
};
