import { BookOpen, Pencil } from 'lucide-react';
import type { Subject } from '../model/types';
import { subjectColor } from '../model/colors';
export function SubjectHeading({ selected, busy, onEdit }: {
    selected: Subject;
    busy: boolean;
    onEdit: () => void;
}) {
    return <div className="page-heading">
    <div>

    <h1>
    <span className="subject-emblem" style={{ color: subjectColor(selected.color), background: subjectColor(selected.color) + '16' }}>
    <BookOpen size={27}/>
    </span>{selected.name}</h1>
    <>{selected.description && <p>{selected.description}</p>}</>
    </div>
    <div className="heading-actions">
    <button className="secondary" disabled={busy} onClick={onEdit}>
    <Pencil size={15}/>Редагувати</button>
    </div>
    </div>;
}
