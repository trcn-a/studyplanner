import { useState } from 'react';
import type { ComponentProps } from 'react';
import { Plus } from 'lucide-react';
import type { Task, Status } from '../model/types';
import type { Subject } from '../../subjects/model/types';
import { subjectColor } from '../../subjects/model/colors';
import { statuses } from '../model/constants';
import { matchesTask } from '../model/selectors';
import { TaskFilters } from './TaskFilters';
import { TaskRow } from './TaskRow';
import type { TaskActions } from '../model/types';
interface Props {
  tasks: Task[]; subjects: Subject[]; loading: boolean; busy: boolean;
  filters: ComponentProps<typeof TaskFilters>; actions: TaskActions;
  onCreate: () => void; onStatus: (task: Task, status: Status) => void;
}
export function TaskKanban({ tasks, subjects, loading, busy, filters, actions, onCreate, onStatus }: Props) {
  const [dragged, setDragged] = useState<string | null>(null);
  const [over, setOver] = useState<Status | null>(null);
  const visible = tasks.filter(task => matchesTask(task, filters.search, filters.statusFilter, filters.priorityFilter));
  return <section className="tasks-panel task-toolbar-panel" aria-label="Канбан">
    <TaskFilters {...filters}><button className="primary" disabled={busy || loading || !subjects.length} onClick={onCreate}><Plus size={17}/>Нова задача</button></TaskFilters>
    {loading ? <div className="empty" role="status">Завантажуємо задачі…</div> : <div className="kanban-board">
      {(Object.keys(statuses) as Status[]).map(status => {
        const items = visible.filter(task => task.status === status);
        return <section key={status} className={`kanban-column ${over === status ? 'drag-over' : ''}`} aria-label={statuses[status]}
          onDragOver={event => { if (dragged && !busy) { event.preventDefault(); event.dataTransfer.dropEffect = 'move'; setOver(status); } }}
          onDragLeave={event => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOver(null); }}
          onDrop={event => { event.preventDefault(); const task = tasks.find(task => task.id === dragged); setDragged(null); setOver(null); if (task && !busy) onStatus(task, status); }}>
          <h3>{statuses[status]} <span>{items.length}</span></h3>
          {items.map(task => <article key={task.id} className={`kanban-card ${dragged === task.id ? 'dragging' : ''}`} draggable={!busy}
            onDragStart={event => { setDragged(task.id); event.dataTransfer.setData('text/plain', task.id); event.dataTransfer.effectAllowed = 'move'; }}
            onDragEnd={() => { setDragged(null); setOver(null); }}
            style={{ borderTopColor: subjectColor(subjects.find(subject => subject.id === task.subjectId)?.color ?? '') }}>
            <div className="kanban-context">{subjects.find(subject => subject.id === task.subjectId)?.name}{task.parentTaskId && <small>Підзадача: {tasks.find(parent => parent.id === task.parentTaskId)?.title}</small>}</div>
            <TaskRow hideStatusControl task={task} child={!!task.parentTaskId} subtasks={tasks.filter(child => child.parentTaskId === task.id)} busy={busy} {...actions}/>
          </article>)}
          {!items.length && <p className="kanban-empty">Немає задач</p>}
        </section>;
      })}
    </div>}
  </section>;
}
