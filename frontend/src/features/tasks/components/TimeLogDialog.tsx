import { useState } from 'react';
import type { FormEvent } from 'react';
import { Trash2, Clock3, Plus } from 'lucide-react';
import { Modal } from '../../../shared/ui/Modal';
import { today } from '../../../shared/lib/date';
import { formatMinutes } from '../../../shared/lib/duration';
import type { Task, TimeEntry } from '../model/types';
interface Props {
    task: Task;
    entries: TimeEntry[] | null;
    busy: boolean;
    error: string;
    onLog: (minutes: number, date: string) => void;
    onRemove: (entry: TimeEntry) => void;
    onClose: () => void;
}
const when = (value: string, timer: boolean) => new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'short', ...(timer ? { hour: '2-digit', minute: '2-digit' } as const : {}) }).format(new Date(value));
export function TimeLogDialog({ task, entries, busy, error, onLog, onRemove, onClose }: Props) {
    const [minutes, setMinutes] = useState('');
    const [date, setDate] = useState(today());
    const recordedMinutes = entries?.reduce((total,entry)=>total+(entry.minutes ?? 0),0) ?? task.actualMinutes;
    function submit(event: FormEvent) {
        event.preventDefault();
        const value = Number(minutes);
        if (!Number.isInteger(value) || value < 1 || value > 1440)
            return;
        onLog(value, date);
        setMinutes('');
    }
    return <Modal className="time-log-dialog" title="Облік часу" busy={busy} close={onClose}>
    <p className="time-task-name">{task.title}</p>
    <div className="time-summary"><Clock3 size={20} aria-hidden="true"/><div><small>Записано часу</small><strong>{formatMinutes(recordedMinutes)}</strong></div>{entries && <span>Записів: {entries.length}</span>}</div>
    <form className="time-log-form" onSubmit={submit}>
      <fieldset disabled={busy}>
        <div className="time-add-row">
          <label>Витрачено, хв<input type="number" required min={1} max={1440} step={1} placeholder="Наприклад, 45" value={minutes} onChange={e => setMinutes(e.target.value)}/></label>
          <label>Дата<input type="date" required max={today()} value={date} onChange={e => setDate(e.target.value)}/></label>
          <button className="primary" disabled={busy || !minutes || !date}><Plus size={16}/>Додати</button>
        </div>
      </fieldset>
    </form>
    {error && <p className="error" role="alert">{error}</p>}
    <h3 className="time-history-heading">Записи часу</h3>
    {entries === null ? <p role="status">Завантажуємо записи…</p> : entries.length === 0 ? <p className="kanban-empty">Записів часу ще немає.</p> : <ul className="time-entries">
      {entries.map(entry => <li key={entry.id}>
        <span>{when(entry.startedAt, entry.source === 'TIMER')}<small>{entry.source === 'TIMER' ? 'Таймер' : 'Вручну'}</small></span>
        <strong>{entry.minutes === null ? 'триває…' : formatMinutes(entry.minutes)}</strong>
        {entry.minutes !== null && <button className="icon danger" type="button" disabled={busy} title="Видалити запис" aria-label="Видалити запис часу" onClick={() => onRemove(entry)}><Trash2 size={15}/></button>}
      </li>)}
    </ul>}
  </Modal>;
}
