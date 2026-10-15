import { formatMinutes } from '../../../shared/lib/duration';
import { Plus, Trash2, CalendarDays, Clock3, ListTodo, Check, Play, Square, Timer } from 'lucide-react';
import type { Task, TaskActions } from '../model/types';
import { statuses, priorities, taskTypes, difficulties } from '../model/constants';
import { usePrediction } from '../../prediction/model/predictionContext';
import { dateLabel, today } from '../../../shared/lib/date';
interface Props extends TaskActions {
    task: Task;
    child?: boolean;
    subtasks: Task[];
    busy: boolean;
    subjectName?: string;
    selectStatus?: boolean;
    hideStatusControl?: boolean;
}
export function TaskRow({ task, child = false, subtasks, busy, subjectName, selectStatus = false, hideStatusControl = false, onStatus, onToggle, onEdit, onAddChild, onDelete, onTimer, onLogTime }: Props) {
    const subs = subtasks;
    const completed = subs.filter(t => t.status === 'DONE').length;
    const prediction = usePrediction(task.status === 'DONE' ? undefined : task.id);
    const running = !!task.timerStartedAt;
    const useForecast = task.status !== 'DONE' && task.planningTimeSource !== 'USER_ESTIMATE' && prediction && prediction.method !== 'USER_ESTIMATE';
    const plannedMinutes = useForecast ? prediction.minutes : task.estimatedMinutes;
    const progress = task.status === 'DONE' ? 100 : plannedMinutes ? Math.min(99, Math.round(task.actualMinutes / plannedMinutes * 100)) : null;
    return <div className={`task-row ${child ? 'child-row' : ''} ${task.status === 'DONE' ? 'is-done' : ''} ${busy ? 'is-busy' : ''}`} key={task.id}
      onClick={event => { if (!busy && !(event.target as HTMLElement).closest('button, input, select, textarea, a')) onEdit(task); }}>
      {selectStatus && onStatus ? <select className={`task-status-select status ${task.status.toLowerCase()}`} aria-label={`Статус: ${task.title}`} value={task.status} disabled={busy} onChange={event=>onStatus(task,event.target.value as Task['status'])}>{Object.entries(statuses).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select> : !hideStatusControl && <button className={`check ${task.status === 'DONE' ? 'checked' : ''}`} disabled={busy} aria-label={`${task.status === 'DONE' ? 'Повернути до виконання' : 'Завершити'}: ${task.title}`} onClick={() => onToggle(task)}>{task.status === 'DONE' && <Check size={14}/>}</button>}
      <div className="task-content" role="button" tabIndex={busy ? -1 : 0} aria-disabled={busy} aria-label={`Редагувати: ${task.title}`}
        onKeyDown={event => { if (!busy && (event.key === 'Enter' || event.key === ' ')) { event.preventDefault(); onEdit(task); } }}>
    <div className="task-title-line"><div className="task-title">{task.title}</div>{subjectName && <span className="task-subject-label">{subjectName}</span>}</div>{task.description && <p className="task-description">{task.description}</p>}
        <div className="task-meta">
    {!selectStatus && <span className={`status ${task.status.toLowerCase()}`}>{statuses[task.status]}</span>}
    <span className="task-characteristics">{task.taskType && <span>{taskTypes[task.taskType]}</span>}
    {task.difficulty && <span title="Очікувана складність">{difficulties[task.difficulty]} складність</span>}
    <span className={`priority ${task.priority.toLowerCase()}`} title={`${priorities[task.priority]} пріоритет`} aria-label={`${priorities[task.priority]} пріоритет`}>
    <i aria-hidden="true"/></span>
          {progress !== null && <span className="task-progress-value" title={task.status === 'DONE' ? 'Завдання завершено' : 'Прогрес за витраченим часом; 100% після завершення завдання'}>{progress}%</span>}
</span>
    <span className="task-timing">
          {task.dueDate && <span className={task.status !== 'DONE' && task.dueDate < today() ? 'overdue' : ''}>
        <CalendarDays size={13}/>{dateLabel(task.dueDate)}</span>}
          {plannedMinutes != null && plannedMinutes > 0 && <span title="Тривалість для розкладу">
        <Clock3 size={13} aria-hidden="true"/>{formatMinutes(plannedMinutes)}</span>}
    </span>
          {running && <span className="actual running"><Timer size={13}/>Таймер працює</span>}
          {!child && subs.length > 0 && <span className="task-subtask-count">
        <ListTodo size={13}/>{completed}/{subs.length} підзадач</span>}
        </div>
      </div>
      <div className="row-actions">{task.status !== 'DONE' && <button className={`icon ${running ? 'timer-on' : ''}`} disabled={busy} title={running ? 'Зупинити таймер' : 'Запустити таймер'} aria-label={`${running ? 'Зупинити таймер' : 'Запустити таймер'}: ${task.title}`} onClick={() => onTimer(task)}>
        {running ? <Square size={15}/> : <Play size={15}/>}
        </button>}<button className="icon" disabled={busy} title="Облік часу" aria-label={`Облік часу: ${task.title}`} onClick={() => onLogTime(task)}>
        <Timer size={15}/>
        </button>{!child && <button className="icon" disabled={busy} title="Додати підзадачу" aria-label={`Додати підзадачу: ${task.title}`} onClick={() => onAddChild(task.id)}>
        <Plus size={17}/>
        </button>}
    <button className="icon danger" disabled={busy} title="Видалити" aria-label={`Видалити: ${task.title}`} onClick={() => onDelete(task)}>
    <Trash2 size={15}/>
    </button>
    </div>
    </div>;
}
