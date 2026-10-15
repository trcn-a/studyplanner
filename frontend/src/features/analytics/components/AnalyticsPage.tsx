import { useState } from 'react';
import { Check, Clock3, CalendarCheck, Target, Flame, Timer } from 'lucide-react';
import type { ReactNode } from 'react';
import { useAnalytics } from '../model/useAnalytics';
import { shiftDate } from '../model/series';
import { ActivityChart, StackedBar, WeekdayChart } from './charts';
import { formatMinutes } from '../../../shared/lib/duration';
import { dateLabel, today } from '../../../shared/lib/date';
import { subjectColor } from '../../subjects/model/colors';

const presets = [{ days: 7, label: '7 днів' }, { days: 30, label: '30 днів' }, { days: 90, label: '90 днів' }];
const percent = (value: number | null) => value === null ? '—' : `${value}%`;

function Card({ icon, title, value, note }: { icon: ReactNode; title: string; value: string; note: string }) {
    return <article className="analytics-card"><span className="analytics-icon" aria-hidden="true">{icon}</span><div><span>{title}</span><strong>{value}</strong><small>{note}</small></div></article>;
}

/** Сторінка аналітики: картки показників, графіки за період і прогрес завдань. */
export function AnalyticsPage({ revision }: { revision: string }) {
    const [days, setDays] = useState<number | null>(30);
    const [from, setFrom] = useState(() => shiftDate(today(), -29));
    const [to, setTo] = useState(today);
    const { data, loading, error } = useAnalytics(from, to, revision);
    const choose = (count: number) => { setDays(count); setTo(today()); setFrom(shiftDate(today(), -(count - 1))); };
    const subjectNames = new Map((data?.subjects ?? []).map(s => [s.id, s]));
    const maxSubject = Math.max(1, ...(data?.subjects ?? []).map(s => s.actualMinutes));
    return <section className="analytics" aria-label="Аналітика навчальної діяльності">
        <div className="analytics-heading">
            <div className="analytics-period">
                <div className="segmented" role="group" aria-label="Період">
                    {presets.map(p => <button key={p.days} className={days === p.days ? 'active' : ''} onClick={() => choose(p.days)}>{p.label}</button>)}
                </div>
                <label>З<input type="date" value={from} max={to} onChange={e => { setDays(null); setFrom(e.target.value); }}/></label>
                <label>По<input type="date" value={to} min={from} onChange={e => { setDays(null); setTo(e.target.value); }}/></label>
            </div>
        </div>
        {error && <div className="error" role="alert">{error}</div>}
        {!data && loading && <div className="empty" role="status">Обчислюємо показники…</div>}
        {data && <div className={loading ? 'analytics-body stale' : 'analytics-body'}>
            <div className="analytics-cards">
                <Card icon={<Check size={20}/>} title="Виконано завдань" value={String(data.summary.completedTasks)} note={`Відкритих зараз: ${data.summary.openTasks}`}/>
                <Card icon={<Clock3 size={20}/>} title="Витрачено часу" value={formatMinutes(data.summary.actualMinutes)} note={`Сесій: ${data.summary.sessions}`}/>
                <Card icon={<CalendarCheck size={20}/>} title="Дедлайни вчасно" value={percent(data.deadlines.onTimePercent)} note={data.deadlines.onTime + data.deadlines.late + data.deadlines.overdueOpen > 0 ? `Вчасно: ${data.deadlines.onTime} із ${data.deadlines.onTime + data.deadlines.late + data.deadlines.overdueOpen}` : 'Ще немає результатів'}/>
                <Card icon={<Target size={20}/>} title="План виконано" value={percent(data.plan.completionPercent)} note={data.plan.plannedMinutes ? `${formatMinutes(data.plan.fulfilledMinutes)} із ${formatMinutes(data.plan.plannedMinutes)}` : 'Завершених планових сесій ще немає'}/>
                <Card icon={<Flame size={20}/>} title="Активні дні" value={`${data.summary.activeDays} / ${data.summary.periodDays}`} note={`Серія: ${data.summary.currentStreak} дн. · найдовша: ${data.summary.longestStreak} дн.`}/>
                <Card icon={<Timer size={20}/>} title="У середньому за день" value={formatMinutes(data.summary.averageMinutesPerActiveDay)} note="Лише за дні, коли ви навчалися"/>
            </div>

            <section className="analytics-panel wide"><h3>Динаміка навчальної активності</h3>
                <ActivityChart days={data.days}/>
                <ul className="legend"><li><i className="bar-actual"/>Фактично витрачено</li><li><i className="bar-plan"/>Заплановано</li></ul>
                {data.summary.actualMinutes === 0 && data.plan.plannedMinutes === 0 && <p className="analytics-empty">За цей період немає сесій. Запустіть таймер або внесіть час вручну.</p>}
            </section>

            <div className="analytics-grid">
                <section className="analytics-panel analytics-plan"><h3>План</h3>
                    <StackedBar empty="Для цього періоду ще немає запланованих сесій." segments={[
                        { label: 'Виконано', value: data.plan.fulfilledMinutes, className: 'seg-done' },
                        { label: 'Пропущено', value: data.plan.missedMinutes, className: 'seg-missed' },
                        { label: 'Попереду', value: data.plan.futureMinutes, className: 'seg-future' }]} formatValue={formatMinutes}/>
                    {data.plan.extraMinutes > 0 && <p className="analytics-note">Додатково до плану: {formatMinutes(data.plan.extraMinutes)}.</p>}
                </section>
                <section className="analytics-panel"><h3>Дедлайн</h3>
                    <StackedBar empty="У цьому періоді немає завдань із дедлайном." segments={[
                        { label: 'Вчасно', value: data.deadlines.onTime, className: 'seg-done' },
                        { label: 'Із запізненням', value: data.deadlines.late, className: 'seg-late' },
                        { label: 'Прострочено', value: data.deadlines.overdueOpen, className: 'seg-missed' },
                        { label: 'Попереду', value: data.deadlines.upcoming, className: 'seg-future' }]}/>
                    {data.deadlines.unknownCompletion > 0 && <p className="analytics-note">Ще {data.deadlines.unknownCompletion} завершених завдань без відомої дати завершення не враховано.</p>}
                </section>
                <section className="analytics-panel"><h3>Навчальний час за предметами</h3>
                    {data.subjects.length === 0 ? <p className="analytics-empty">Предметів ще немає.</p> : <ul className="subject-bars">
                        {data.subjects.map(s => <li key={s.id}>
                            <div><span><i style={{ background: subjectColor(s.color) }}/>{s.name}</span><b>{formatMinutes(s.actualMinutes)}</b></div>
                            <div className="track"><span style={{ width: `${(s.actualMinutes / maxSubject) * 100}%`, background: subjectColor(s.color) }}/></div>
                            <small>Виконано {s.doneTasks} з {s.totalTasks} завдань · {s.totalTasks ? Math.round(s.doneTasks / s.totalTasks * 100) : 0}%</small>
                        </li>)}
                    </ul>}
                </section>
                <section className="analytics-panel"><h3>Витрачений час за днями тижня</h3>
                    <WeekdayChart weekdays={data.weekdays}/>
                    <p className="analytics-note">Сумарний час за вибраний період, згрупований за днями тижня.</p>
                </section>
            </div>

            <section className="analytics-panel wide"><h3>Прогрес незавершених завдань</h3>
                {data.progress.length === 0 ? <p className="analytics-empty">Усі завдання виконано.</p> : <ul className="task-progress">
                    {data.progress.map(t => {
                        const subject = subjectNames.get(t.subjectId);
                        return <li key={t.id}>
                            <div><strong>{t.title}</strong><span>{subject?.name}{t.dueDate ? ` · до ${dateLabel(t.dueDate)}` : ''}</span></div>
                            <div className="track" title={t.expectedMinutes ? `Очікувано ${formatMinutes(t.expectedMinutes)}` : 'Немає оцінки чи прогнозу'}>
                                <span className={t.percent !== null && t.percent > 100 ? 'over' : ''} style={{ width: `${Math.min(100, t.percent ?? 0)}%` }}/>
                            </div>
                            <small>{t.expectedMinutes ? `${formatMinutes(t.actualMinutes)} з ${formatMinutes(t.expectedMinutes)} · ${t.percent}%` : `${formatMinutes(t.actualMinutes)} · без оцінки`}</small>
                        </li>;
                    })}
                </ul>}
            </section>
        </div>}
    </section>;
}
