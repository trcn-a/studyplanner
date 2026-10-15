import { bucketDays, niceMax, weekdayNames } from '../model/series';
import type { AnalyticsDay, AnalyticsWeekday } from '../model/types';
import { formatMinutes } from '../../../shared/lib/duration';

/** Стовпчики фактичного часу по днях (або тижнях) на тлі запланованого обсягу. */
export function ActivityChart({ days }: { days: AnalyticsDay[] }) {
    const buckets = bucketDays(days);
    const W = 760, H = 250, right = 8, top = 12, bottom = 30;
    const max = niceMax(Math.max(0, ...buckets.map(b => Math.max(b.actual, b.planned))));
    const ticks = [0, 1, 2, 3, 4].map(i => (max / 4) * i);
    const tickLabel = (value: number) => value === 0 ? '0' : formatMinutes(Math.round(value));
    const left = Math.max(46, ...ticks.map(t => tickLabel(t).length * 7 + 12));
    const plotW = W - left - right, plotH = H - top - bottom;
    const slot = plotW / buckets.length, bar = Math.max(3, slot * 0.62);
    const y = (v: number) => top + plotH - (v / max) * plotH;
    const every = Math.ceil(buckets.length / 10);
    return <svg className="chart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Фактичний і запланований навчальний час за днями">
        {ticks.map(t => <g key={t}>
            <line x1={left} x2={W - right} y1={y(t)} y2={y(t)} className="chart-grid"/>
            <text x={left - 6} y={y(t) + 4} textAnchor="end" className="chart-axis">{tickLabel(t)}</text>
        </g>)}
        {buckets.map((b, i) => {
            const x = left + slot * i + (slot - bar) / 2;
            return <g key={i}>
                <title>{`${b.title}: фактично ${formatMinutes(b.actual)}, заплановано ${formatMinutes(b.planned)}`}</title>
                {b.planned > 0 && <rect x={x} y={y(b.planned)} width={bar} height={Math.max(0, top + plotH - y(b.planned))} className="bar-plan"/>}
                {b.actual > 0 && <rect x={x + bar * 0.18} y={y(b.actual)} width={bar * 0.64} height={Math.max(0, top + plotH - y(b.actual))} className="bar-actual"/>}
                {i % every === 0 && <text x={x + bar / 2} y={H - 10} textAnchor="middle" className="chart-axis">{b.label}</text>}
            </g>;
        })}
    </svg>;
}

/** Сумарний фактичний час за кожним днем тижня у вибраному періоді. */
export function WeekdayChart({ weekdays }: { weekdays: AnalyticsWeekday[] }) {
    const W = 560, H = 210, top = 28, bottom = 24;
    const max = Math.max(30, ...weekdays.map(d => d.actualMinutes));
    const slot = W / 7, bar = slot * 0.6, plotH = H - top - bottom;
    return <svg className="chart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Сумарний витрачений час за днями тижня">
        {weekdays.map((d, i) => {
            const h = (d.actualMinutes / max) * plotH, x = slot * i + (slot - bar) / 2;
            return <g key={d.dayOfWeek}>
                <title>{`${weekdayNames[i]}: ${formatMinutes(d.actualMinutes)}`}</title>
                <rect x={x} y={top + plotH - h} width={bar} height={h} className="bar-actual" rx="3"/>
                <text x={x + bar / 2} y={Math.max(12, top + plotH - h - 6)} textAnchor="middle" className="chart-axis">{formatMinutes(d.actualMinutes)}</text>
                <text x={x + bar / 2} y={H - 7} textAnchor="middle" className="chart-axis">{weekdayNames[i]}</text>
            </g>;
        })}
    </svg>;
}

export interface Segment { label: string; value: number; className: string }

/** Горизонтальна смуга з сегментів та легенда зі значеннями. */
export function StackedBar({ segments, empty, formatValue = String }: { segments: Segment[]; empty: string; formatValue?: (value: number) => string }) {
    const total = segments.reduce((sum, s) => sum + s.value, 0);
    if (total === 0) return <p className="analytics-empty">{empty}</p>;
    return <>
        <div className="stacked" role="img" aria-label={segments.map(s => `${s.label}: ${formatValue(s.value)}`).join(', ')}>
            {segments.filter(s => s.value > 0).map(s => <span key={s.label} className={s.className} style={{ width: `${(s.value / total) * 100}%` }} title={`${s.label}: ${formatValue(s.value)}`}/>)}
        </div>
        <ul className="legend">{segments.map(s => <li key={s.label}><i className={s.className}/><span>{s.label}</span><b>{formatValue(s.value)}</b></li>)}</ul>
    </>;
}
