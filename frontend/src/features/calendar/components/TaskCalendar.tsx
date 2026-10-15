import { PlanningTools } from '../../planning/components/PlanningTools';
import { usePlanning } from '../../planning/model/usePlanning';
import { WeekSchedule } from './WeekSchedule';
import type { Subject } from '../../subjects/model/types';
import { subjectColor } from '../../subjects/model/colors';
import { useState } from 'react';
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react';
import type { TaskActions } from '../../tasks/model/types';
import type { Task } from '../../tasks/model/types';
import { statuses } from '../../tasks/model/constants';
import { groupTasksByDate, monthDays, shiftMonth, weekDates, localDateKey } from '../model/calendar';
import { today } from '../../../shared/lib/date';

interface Props {
  subjects: Subject[];
  tasks: Task[];
  loading: boolean;
  busy: boolean;
  actions: TaskActions;
  onSettings: () => void;
  onMoved: () => void;
  onCreate: (date?: string, time?: string) => void;
}

const weekdays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Нд'];
const fullDate = (date: string) => new Intl.DateTimeFormat('uk-UA', {
  day: 'numeric', month: 'long', year: 'numeric',
}).format(new Date(date + 'T12:00:00'));

export function TaskCalendar({ subjects, tasks, loading, busy, actions, onCreate, onSettings, onMoved }: Props) {
  const planning = usePlanning(JSON.stringify(tasks));
  const [month, setMonth] = useState(() => ({ year: new Date().getFullYear(), month: new Date().getMonth() }));
  const [selectedDate, setSelectedDate] = useState(today);
  const [view, setView] = useState<'month' | 'week' | 'day'>('week');
  const week = weekDates(selectedDate);
  const days = monthDays(month.year, month.month);
  const { byDate: calendarDates } = groupTasksByDate(tasks);
  const monthLabel = new Intl.DateTimeFormat('uk-UA', { month: 'long', year: 'numeric' })
    .format(new Date(month.year, month.month, 1, 12));
  const todayKey = today();
  const dueTasks = tasks.filter(task => task.dueDate === selectedDate);
  const deadlineHeading = selectedDate === todayKey ? 'Дедлайн сьогодні' : `Дедлайн: ${fullDate(selectedDate)}`;

  function navigate(offset: number) {
    if (view !== 'month') {
      const nextDate = new Date(selectedDate + 'T12:00:00');
      nextDate.setDate(nextDate.getDate() + offset * (view === 'day' ? 1 : 7));
      setSelectedDate(localDateKey(nextDate));
      setMonth({ year: nextDate.getFullYear(), month: nextDate.getMonth() });
      return;
    }
    const next = shiftMonth(month.year, month.month, offset);
    setMonth(next);
    setSelectedDate(`${next.year}-${String(next.month + 1).padStart(2, '0')}-01`);
  }

  function goToToday() {
    const now = new Date();
    setMonth({ year: now.getFullYear(), month: now.getMonth() });
    setSelectedDate(today());
  }

  return <section className="tasks-panel calendar-panel" aria-label="Календар">
    <PlanningTools {...planning} onSettings={onSettings}>
      <button className="primary" disabled={busy || loading || !subjects.length} onClick={() => onCreate(selectedDate)}><Plus size={17}/>Нова задача</button>
    </PlanningTools>
    <div className="task-view-switch calendar-mode" role="group" aria-label="Період календаря">
      <button aria-pressed={view === 'month'} onClick={() => { const date = new Date(selectedDate + 'T12:00:00'); setMonth({ year: date.getFullYear(), month: date.getMonth() }); setView('month'); }}>Місяць</button>
      <button aria-pressed={view === 'week'} onClick={() => setView('week')}>Тиждень</button>
      <button aria-pressed={view === 'day'} onClick={() => setView('day')}>День</button>
    </div>
    <div className="calendar-toolbar">
      <h3 aria-live="polite">{view === 'month' ? monthLabel : view === 'day' ? fullDate(selectedDate) : `${fullDate(week[0])} — ${fullDate(week[6])}`}</h3>
      <div className="calendar-navigation">
        <button className="secondary" onClick={goToToday}>Сьогодні</button>
        <button className="icon" aria-label={view === 'month' ? 'Попередній місяць' : view === 'day' ? 'Попередній день' : 'Попередній тиждень'} onClick={() => navigate(-1)}><ChevronLeft size={19}/></button>
        <button className="icon" aria-label={view === 'month' ? 'Наступний місяць' : view === 'day' ? 'Наступний день' : 'Наступний тиждень'} onClick={() => navigate(1)}><ChevronRight size={19}/></button>
      </div>
    </div>
    {loading ? <div className="empty" role="status">Завантажуємо календар…</div> : <>
      {view !== 'month' ? <div className={view === 'day' ? 'calendar-day-layout' : undefined}>
        <WeekSchedule onMove={async (block,start)=>{if(await planning.actions.moveSession(block.id,start,block.source==='MANUAL'))onMoved();}} dayView={view === 'day'} planning={planning.state} date={selectedDate} tasks={tasks} subjects={subjects} busy={busy || planning.busy} onSelect={setSelectedDate} onEdit={actions.onEdit} onCreate={onCreate}/>
        {view === 'day' && <aside className="day-deadlines" aria-label={deadlineHeading}>
          <h3>{deadlineHeading} <span>{dueTasks.length}</span></h3>
          {dueTasks.length ? <ul>{dueTasks.map(task => {
            const subject = subjects.find(item => item.id === task.subjectId);
            return <li key={task.id}><button className={`day-deadline-task${task.status === 'DONE' ? ' is-done' : ''}`} disabled={busy} onClick={() => actions.onEdit(task)} aria-label={`Редагувати: ${task.title}`} style={{borderLeftColor:subjectColor(subject?.color ?? '')}}>
              <small>{subject?.name}{task.parentTaskId ? ' · Підзавдання' : ''}</small>
              <strong>{task.title}</strong>
              <span className={`status ${task.status.toLowerCase()}`}>{statuses[task.status]}</span>
            </button></li>;
          })}</ul> : <p className="calendar-empty">На цей день дедлайнів немає.</p>}
        </aside>}
      </div> : <>
      <div className="calendar-weekdays" aria-hidden="true">{weekdays.map(day => <span key={day}>{day}</span>)}</div>
      <div className="calendar-grid" aria-label={monthLabel}>
        {days.map(day => {
          const items = calendarDates[day.date] ?? [];
          return <div key={day.date} className={`calendar-cell${day.inMonth ? '' : ' outside-month'}${day.date === selectedDate ? ' selected-day' : ''}`}>
            <button className={`calendar-date${day.date === todayKey ? ' is-today' : ''}`}
              aria-label={`${fullDate(day.date)}. Завдань: ${items.length}`}
              aria-pressed={day.date === selectedDate} aria-current={day.date === todayKey ? 'date' : undefined}
              onClick={() => { setSelectedDate(day.date); setView('day'); }}>{day.day}</button>
            <div className="calendar-events">{items.slice(0, 2).map(task => <button key={task.id}
              className={`calendar-event ${task.status.toLowerCase()}${task.status !== 'DONE' && day.date < todayKey ? ' late' : ''}`}
              disabled={busy} onClick={() => actions.onEdit(task)}
              style={{ borderLeft: `3px solid ${subjectColor(subjects.find(subject => subject.id === task.subjectId)?.color ?? '')}` }}
              title={`${subjects.find(subject => subject.id === task.subjectId)?.name}: ${task.title} — ${statuses[task.status]}`}
              aria-label={`Редагувати: ${task.title}, ${fullDate(day.date)}`}>
                <small style={{ display: 'block' }}>{subjects.find(subject => subject.id === task.subjectId)?.name}</small>
                {task.parentTaskId && <span aria-hidden="true">↳ </span>}{task.title}
            </button>)}</div>
            {items.length > 2 && <button className="calendar-more" onClick={() => { setSelectedDate(day.date); setView('day'); }}>Ще {items.length - 2}</button>}
            {items.length > 0 && <span className="calendar-mobile-count" aria-hidden="true">{items.length}</span>}
          </div>;
        })}
      </div>
      <div className="calendar-legend"><span>Дедлайни</span><span className="late">Прострочені</span><span className="done">Виконані</span></div>
      </>}
    </>}
  </section>;
}
