import { request } from '../../../shared/api/http';
import type { Analytics } from '../model/types';
export const analyticsApi = { report: (from: string, to: string) => request<Analytics>(`/analytics?from=${from}&to=${to}`) };
