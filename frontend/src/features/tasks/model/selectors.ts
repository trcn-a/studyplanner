import type { Task } from './types';
import { today } from '../../../shared/lib/date';
export function selectTaskGroups(tasks: Task[], search: string, statusFilter: string, priorityFilter: string) {
    const roots = tasks.filter(task => !task.parentTaskId);
    const children = (id: string) => tasks.filter(task => task.parentTaskId === id);
    const matches = (task: Task) => matchesTask(task, search, statusFilter, priorityFilter);
    return {
        roots, children,
        filteredRoots: roots.filter(task => matches(task) || children(task.id).some(matches)),
        done: roots.filter(task => task.status === 'DONE').length,
        overdue: roots.filter(task => task.status !== 'DONE' && task.dueDate && task.dueDate < today()).length,
    };
}

export function matchesTask(task: Task, search: string, statusFilter: string, priorityFilter: string) {
  return (!statusFilter || task.status === statusFilter)
    && (!priorityFilter || task.priority === priorityFilter)
    && (!search || `${task.title} ${task.description}`.toLocaleLowerCase('uk').includes(search.toLocaleLowerCase('uk')));
}
