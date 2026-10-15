import { request } from '../../../shared/api/http';
import type { Task, TimeEntry } from '../model/types';
export const timeApi = {
    startTimer: (taskId: string) => request<Task>(`/tasks/${taskId}/timer/start`, 'POST'),
    stopTimer: (taskId: string) => request<Task>(`/tasks/${taskId}/timer/stop`, 'POST'),
    entries: (taskId: string) => request<TimeEntry[]>(`/tasks/${taskId}/time-entries`),
    log: (taskId: string, minutes: number, date: string) => request<Task>(`/tasks/${taskId}/time-entries`, 'POST', { minutes, date }),
    removeEntry: (entryId: string) => request<Task>(`/time-entries/${entryId}`, 'DELETE'),
};
