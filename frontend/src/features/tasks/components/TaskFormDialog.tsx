import { formatMinutes } from '../../../shared/lib/duration';
import type { Subject } from '../../subjects/model/types';
import type { FormEvent } from 'react';
import { Modal } from '../../../shared/ui/Modal';
import { statuses, priorities, taskTypes, difficulties } from '../model/constants';
import { usePredictionPreview } from '../../prediction/model/usePredictionPreview';
import type { TaskEditor, Status, Priority, TaskType, Difficulty } from '../model/types';
import type { Task } from '../model/types';
import { TaskSessions } from './TaskSessions';
interface Props {
    task?: Task;
    subjects: Subject[];
    taskEditor: TaskEditor;
    setTaskEditor: (editor: TaskEditor) => void;
    busy: boolean;
    error: string;
    onSubmit: (event: FormEvent) => void;
    onClose: () => void;
    parentTitle?: string;
}
export function TaskFormDialog({ subjects, taskEditor, setTaskEditor, busy, error, onSubmit, onClose, parentTitle, task }: Props) {
    const preview = usePredictionPreview(taskEditor);
    const prediction = preview.prediction;
    const independent = prediction && prediction.method !== 'USER_ESTIMATE';
    const source = taskEditor.input.planningTimeSource ?? 'PREDICTION';
    return <Modal className="task-dialog" title={taskEditor.id ? 'Редагувати задачу' : taskEditor.parentId ? 'Нова підзадача' : 'Нова задача'} busy={busy} close={() => onClose()}>
    <form className="task-form" onSubmit={onSubmit}>
    <fieldset disabled={busy}>
<label>Назва<input autoFocus required maxLength={200} value={taskEditor.input.title} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, title: e.target.value } })} placeholder="Що потрібно зробити?"/>
    </label>
    {taskEditor.parentId && <p className="parent-label">Підзадача до: {parentTitle}</p>}
    <div className="form-grid"><label>Предмет<select required disabled={!!taskEditor.id || !!taskEditor.parentId} value={taskEditor.subjectId} onChange={e => setTaskEditor({ ...taskEditor, subjectId: e.target.value })}><option value="" disabled>Вибери предмет</option>{subjects.map(subject => <option key={subject.id} value={subject.id}>{subject.name}</option>)}</select></label><label>Тип завдання<select aria-label="Тип завдання" value={taskEditor.input.taskType ?? ''} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, taskType: e.target.value ? e.target.value as TaskType : null } })}><option value="">Не зазначено</option>{Object.entries(taskTypes).map(([v,l]) => <option key={v} value={v}>{l}</option>)}</select></label></div>
<label>Опис<textarea rows={2} maxLength={5000} value={taskEditor.input.description} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, description: e.target.value } })} placeholder="Деталі, вимоги та нотатки"/>
    </label>
    <div className="form-grid"><label>Очікувана складність<select aria-label="Очікувана складність" value={taskEditor.input.difficulty ?? ''} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, difficulty: e.target.value ? e.target.value as Difficulty : null } })}><option value="">Не зазначено</option>{Object.entries(difficulties).map(([v,l]) => <option key={v} value={v}>{l}</option>)}</select></label><label>Пріоритет<select value={taskEditor.input.priority} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, priority: e.target.value as Priority } })}>{Object.entries(priorities).map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
    </label><label>Дедлайн<input type="date" value={taskEditor.input.dueDate || ''} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, dueDate: e.target.value || null } })}/>
    </label><label>Оцінка часу, хв<input type="number" min={1} max={2147483647} step={1} placeholder="Наприклад, 60" value={taskEditor.input.estimatedMinutes ?? ''} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, estimatedMinutes: e.target.value ? Number(e.target.value) : null, planningTimeSource: e.target.value ? source : 'PREDICTION' } })}/>
    </label></div>
    {taskEditor.id && <label>Статус<select value={taskEditor.input.status} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, status: e.target.value as Status } })}>{Object.entries(statuses).map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
    </label>}
    <details className="manual-session-fields" open={taskEditor.input.scheduledStart || taskEditor.input.scheduledEnd ? true : undefined}><summary>Запланувати заняття вручну</summary><div className="form-grid">
    <label>Початок заняття<input type="datetime-local" required={!!taskEditor.input.scheduledEnd} value={taskEditor.input.scheduledStart?.slice(0, 16) ?? ''} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, scheduledStart: e.target.value || null } })}/></label>
    <label>Завершення заняття<input type="datetime-local" required={!!taskEditor.input.scheduledStart} min={taskEditor.input.scheduledStart?.slice(0, 16)} max={taskEditor.input.scheduledStart ? taskEditor.input.scheduledStart.slice(0, 10) + 'T23:59' : undefined} value={taskEditor.input.scheduledEnd?.slice(0, 16) ?? ''} onChange={e => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, scheduledEnd: e.target.value || null } })}/></label>
    {(taskEditor.input.scheduledStart || taskEditor.input.scheduledEnd) && <button type="button" className="text-button schedule-clear" onClick={() => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, scheduledStart: null, scheduledEnd: null } })}>Прибрати час заняття</button>}
    </div></details>
    <div className="planning-time-choice" aria-live="polite">
        <p><strong>Тривалість для планування</strong></p>
        {preview.loading ? <p>Обчислення прогнозу…</p> : preview.error ? <p role="alert">Не вдалося отримати прогноз. {preview.error}</p>
            : independent ? null
            : <p>{preview.valid ? (taskEditor.input.estimatedMinutes ? 'Немає історії для окремого прогнозу.' : 'Недостатньо історії. Вкажіть оцінку часу.') : 'Для прогнозу вкажіть предмет і назву.'}</p>}
        <div role="group" aria-label="Джерело тривалості для планування">
            <label><input type="radio" name="planningTimeSource" value="USER_ESTIMATE" checked={source === 'USER_ESTIMATE'} disabled={!taskEditor.input.estimatedMinutes}
                onChange={() => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, planningTimeSource: 'USER_ESTIMATE' } })}/>
                Власна оцінка{taskEditor.input.estimatedMinutes ? ` — ${formatMinutes(taskEditor.input.estimatedMinutes)}` : ' — не задана'}</label>
            <label><input type="radio" name="planningTimeSource" value="PREDICTION" checked={source === 'PREDICTION'}
                onChange={() => setTaskEditor({ ...taskEditor, input: { ...taskEditor.input, planningTimeSource: 'PREDICTION' } })}/>
                Прогноз системи{independent ? ` — ~${formatMinutes(prediction.minutes)}` : ' — за відсутності використовується оцінка'}</label>
        </div>
        {taskEditor.id && <p className="planning-time-hint">Зміни діятимуть після перепланування.</p>}
    </div>
    </fieldset>{task && <TaskSessions task={task}/>} {error && <p className="error" role="alert">{error}</p>}<div className="modal-footer">
    <button type="button" className="secondary" disabled={busy} onClick={() => onClose()}>Скасувати</button>
    <button className="primary" disabled={busy || !taskEditor.subjectId || !taskEditor.input.title.trim()}>{busy ? 'Збереження…' : 'Зберегти задачу'}</button>
    </div>
    </form>
    </Modal>;
}
