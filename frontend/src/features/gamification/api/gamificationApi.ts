import { request } from '../../../shared/api/http';
import type { Gamification } from '../model/types';
export const gamificationApi = { summary: () => request<Gamification>('/gamification') };
