import type { Status, Priority, TaskInput } from './types';
export const statuses: Record<Status, string> = { TODO: 'До виконання', IN_PROGRESS: 'У роботі', DONE: 'Виконано' };
export const priorities: Record<Priority, string> = { LOW: 'Низький', MEDIUM: 'Середній', HIGH: 'Високий' };
export const blankTask: TaskInput = { title: '', description: '', status: 'TODO', priority: 'MEDIUM', scheduledStart: null, scheduledEnd: null, dueDate: null, estimatedMinutes: null, planningTimeSource: 'PREDICTION', taskType: null, difficulty: null };

export const taskTypes = { LABORATORY: 'Лабораторна', PRACTICE: 'Практична', NOTES: 'Конспект', EXAM_PREPARATION: 'Підготовка до контролю', OTHER: 'Інше' };
export const difficulties = { LOW: 'Низька', MEDIUM: 'Середня', HIGH: 'Висока' };
