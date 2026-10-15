import type { Subject } from '../../subjects/model/types';
import { Plus, ListTodo } from 'lucide-react';
import { useState, type ComponentProps } from 'react';
import type { Task } from '../model/types';
import { TaskRow } from './TaskRow';
import type { TaskActions } from '../model/types';
import { TaskFilters } from './TaskFilters';
interface Props {
    title?: string;
    subjects?: Subject[];
    canCreate?: boolean;
    total: number;
    busy: boolean;
    loading: boolean;
    filteredRoots: Task[];
    children: (id: string) => Task[];
    onCreate: () => void;
    actions: TaskActions;
    filters: ComponentProps<typeof TaskFilters>;
}
export function TaskPanel({ title = 'Задачі предмета', subjects, canCreate = true, total, busy, loading, filteredRoots, children, onCreate, actions, filters }: Props) {
    const [sort, setSort] = useState('due-asc');
    function sorted(items: Task[]) {
      if (!subjects) return items;
      const statusOrder = { TODO: 0, IN_PROGRESS: 1, DONE: 2 };
      return [...items].sort((a,b) => {
        if (sort === 'title') return a.title.localeCompare(b.title, 'uk');
        if (sort === 'status') return statusOrder[a.status] - statusOrder[b.status] || a.title.localeCompare(b.title, 'uk');
        if (!a.dueDate || !b.dueDate) return Number(!a.dueDate) - Number(!b.dueDate);
        return (sort === 'due-desc' ? -1 : 1) * a.dueDate.localeCompare(b.dueDate);
      });
    }
    return <section className={`tasks-panel task-toolbar-panel${subjects ? ' all-tasks-panel' : ''}`} aria-label={title}>
    <TaskFilters {...filters}>
    {subjects && <select aria-label="Сортування завдань" value={sort} onChange={event=>setSort(event.target.value)}>
      <option value="due-asc">Сортувати: дедлайн ↑</option><option value="due-desc">Сортувати: дедлайн ↓</option><option value="status">Сортувати: за статусом</option><option value="title">Сортувати: за назвою</option>
    </select>}
    <button className="primary" disabled={busy || !canCreate} onClick={() => onCreate()}>
    <Plus size={17}/>Нова задача</button>
    </TaskFilters>
          {loading ? <div className="empty" role="status">Завантажуємо задачі…</div> : filteredRoots.length ? <div className="task-list">{sorted(filteredRoots).map(task => <article className="task-group" key={task.id}>
            <TaskRow selectStatus task={task} subjectName={subjects?.find(s => s.id === task.subjectId)?.name} subtasks={children(task.id)} busy={busy} {...actions}/>{children(task.id).length > 0 && <div className="subtasks">{sorted(children(task.id)).map(child => <TaskRow selectStatus key={child.id} task={child} child subtasks={[]} busy={busy} {...actions}/>)}</div>}</article>)}</div> : <div className="empty">
        <ListTodo size={32}/>
        <h3>{total ? 'Нічого не знайдено' : 'Задач поки немає'}</h3>
        {total > 0 && <p>Зміни пошук або фільтри.</p>}</div>}
        </section>;
}
