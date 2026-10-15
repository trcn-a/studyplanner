import { request } from '../../../shared/api/http';
import type { Subject, SubjectInput } from '../model/types';
export const subjectsApi = {
    list: () => request<Subject[]>('/subjects'),
    save: (input: SubjectInput, id?: string) => request<Subject>(id ? `/subjects/${id}` : '/subjects', id ? 'PUT' : 'POST', input),
};
