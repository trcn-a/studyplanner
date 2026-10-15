import { Check, Clock3, ListTodo } from 'lucide-react';
export function TaskStats({ total, done, overdue }: {
    total: number;
    done: number;
    overdue: number;
}) {
    return <section className="stats" aria-label="Підсумок основних задач">
    <div>
    <span className="stat-icon stat-tasks">
    <ListTodo size={20}/>
    </span>
    <div>
    <span>Усього задач</span>
    <strong>{total}</strong>
    </div>
    </div>
    <div>
    <span className="stat-icon stat-done">
    <Check size={20}/>
    </span>
    <div>
    <span>Виконано</span>
    <strong>{done} / {total} · {total ? Math.round(done / total * 100) : 0}%
    </strong>
    </div>
    </div>
    <div>
    <span className="stat-icon stat-overdue">
    <Clock3 size={20}/>
    </span>
    <div>
    <span>Прострочено</span>
    <strong>{overdue}</strong>
    </div>
    </div>
    </section>;
}
