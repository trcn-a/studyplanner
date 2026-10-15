import type { Task } from '../../tasks/model/types';

export interface CalendarDay {
  date: string;
  day: number;
  inMonth: boolean;
}

export function localDateKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

// Calendar dates are local dates, not UTC instants. Weeks start on Monday.
export function monthDays(year: number, month: number): CalendarDay[] {
  const first = new Date(year, month, 1, 12);
  const offset = (first.getDay() + 6) % 7;
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(year, month, 1 - offset + index, 12);
    return { date: localDateKey(date), day: date.getDate(), inMonth: date.getMonth() === month && date.getFullYear() === year };
  });
}

export function shiftMonth(year: number, month: number, offset: number) {
  const date = new Date(year, month + offset, 1, 12);
  return { year: date.getFullYear(), month: date.getMonth() };
}

export function groupTasksByDate(tasks: Task[]) {
  const byDate: Record<string, Task[]> = {};
  const withoutDate: Task[] = [];
  for (const task of tasks) {
    if (task.dueDate) (byDate[task.dueDate] ??= []).push(task);
    else withoutDate.push(task);
  }
  return { byDate, withoutDate };
}

export function weekDates(anchor: string): string[] {
  const date = new Date(anchor + 'T12:00:00');
  date.setDate(date.getDate() - (date.getDay() + 6) % 7);
  return Array.from({ length: 7 }, (_, index) => {
    const day = new Date(date); day.setDate(day.getDate() + index);
    return localDateKey(day);
  });
}

export function workloadTasks(tasks: Task[], allTasks: Task[]): Task[] {
  const parents = new Set(allTasks.map(task => task.parentTaskId).filter(Boolean));
  return tasks.filter(task => task.status !== 'DONE' && !parents.has(task.id));
}
