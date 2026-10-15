import { request } from '../../../shared/api/http';
import type { Task, TaskInput, Status } from '../model/types';
export const tasksApi = {
    list: (subjectId: string) => request<Task[]>(`/subjects/${subjectId}/tasks`),
    save: (subjectId: string, input: TaskInput, id?: string, parentId?: string) => request<Task>(id ? `/tasks/${id}` : `/subjects/${subjectId}/tasks${parentId ? '?parentId=' + parentId : ''}`, id ? 'PUT' : 'POST', input),
    updateStatus: (id: string, status: Status) => request<Task>(`/tasks/${id}/status`, 'PATCH', { status }),
    remove: (id: string) => request<void>(`/tasks/${id}`, 'DELETE'),
};
