import { Search } from 'lucide-react';
import type { ReactNode } from 'react';
import { statuses, priorities } from '../model/constants';
interface Props {
    children?: ReactNode;
    search: string;
    statusFilter: string;
    priorityFilter: string;
    setSearch: (value: string) => void;
    setStatusFilter: (value: string) => void;
    setPriorityFilter: (value: string) => void;
}
export function TaskFilters({ search, statusFilter, priorityFilter, setSearch, setStatusFilter, setPriorityFilter, children }: Props) {
    return <div className="filters">
    <label className="search">
    <Search size={17}/>
    <input aria-label="Пошук задач" placeholder="Знайти задачу…" value={search} onChange={e => setSearch(e.target.value)}/>
    </label>
    <select aria-label="Фільтр за статусом" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
    <option value="">Усі статуси</option>{Object.entries(statuses).map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
    <select aria-label="Фільтр за пріоритетом" value={priorityFilter} onChange={e => setPriorityFilter(e.target.value)}>
    <option value="">Усі пріоритети</option>{Object.entries(priorities).map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
    {children}
    </div>;
}
