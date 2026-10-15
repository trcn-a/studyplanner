import { request } from '../../../shared/api/http';
import type { PlanningState, PlanningSettings } from '../model/types';
export const planningApi = {
    moveSession: (id: string, start: string, manual: boolean) => request<PlanningState>(manual ? `/planning/tasks/${id}/session` : `/planning/sessions/${id}`, 'PATCH', {start}),
    refresh: () => request<PlanningState>('/planning/refresh','POST'),
    generate: () => request<PlanningState>('/planning/generate','POST'),
    saveSettings: (settings: PlanningSettings) => request<PlanningState>('/planning/settings','PUT',settings),
    importCalendar: (content: string) => request<PlanningState>('/planning/calendar/import','POST',{content}),
    removeEvent: (id: string) => request<PlanningState>('/planning/events/'+id,'DELETE'),
    exportUrl: '/api/planning/calendar/export',
};
