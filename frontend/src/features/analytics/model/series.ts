import type { AnalyticsDay } from './types';

export interface Bucket { label: string; title: string; actual: number; planned: number; fulfilled: number }
const short = (date: string) => new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'short' }).format(new Date(date + 'T12:00:00'));

/** Для довгих періодів дні об’єднуються за тижнями, щоб стовпчики лишалися читабельними. */
export function bucketDays(days: AnalyticsDay[], maxBars = 31): Bucket[] {
    const size = days.length <= maxBars ? 1 : 7;
    const buckets: Bucket[] = [];
    for (let i = 0; i < days.length; i += size) {
        const chunk = days.slice(i, i + size);
        const first = chunk[0].date, last = chunk[chunk.length - 1].date;
        buckets.push({
            label: short(first),
            title: size === 1 ? short(first) : `${short(first)} – ${short(last)}`,
            actual: chunk.reduce((sum, d) => sum + d.actualMinutes, 0),
            planned: chunk.reduce((sum, d) => sum + d.plannedMinutes, 0),
            fulfilled: chunk.reduce((sum, d) => sum + d.fulfilledMinutes, 0),
        });
    }
    return buckets;
}

/** Верхня межа осі: найближче «кругле» значення не менше за максимум. */
export function niceMax(value: number): number {
    if (value <= 60) return 60;
    const step = value <= 240 ? 60 : value <= 720 ? 120 : 240;
    return Math.ceil(value / step) * step;
}

export const weekdayNames = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Нд'];

export function shiftDate(date: string, days: number): string {
    const d = new Date(date + 'T12:00:00');
    d.setDate(d.getDate() + days);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
