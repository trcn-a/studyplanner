import { Plus, ChevronRight, GraduationCap, CalendarDays, BarChart3, ListTodo, Columns3, UserRound } from 'lucide-react';
import type { Subject } from '../model/types';
import { subjectColor } from '../model/colors';
interface Props {
    allActive: boolean;
    onAll: () => void;
    profileActive: boolean;
    onProfile: () => void;
    kanbanActive: boolean;
    onKanban: () => void;
    calendarActive: boolean;
    onCalendar: () => void;
    analyticsActive: boolean;
    onAnalytics: () => void;
    subjects: Subject[];
    selectedId: string;
    initialLoading: boolean;
    onCreate: () => void;
    onSelect: (id: string) => void;
}
export function SubjectSidebar({ allActive, onAll, profileActive, onProfile, kanbanActive, onKanban, calendarActive, onCalendar, analyticsActive, onAnalytics, subjects, selectedId, initialLoading, onCreate, onSelect }: Props) {
    return <aside className="sidebar">
    <a className="brand" href="/">
    <span className="brand-icon">
    <GraduationCap size={24}/>
    </span>
    <span>StudyPlanner
    </span>
    </a>
      <button className={`subject-nav ${allActive ? 'active' : ''}`} onClick={onAll} aria-current={allActive ? 'page' : undefined}><ListTodo size={18}/>Усі завдання</button>
      <button className={`subject-nav ${calendarActive ? 'active' : ''}`} onClick={onCalendar} aria-current={calendarActive ? 'page' : undefined}><CalendarDays size={18}/>Календар</button>
      <button className={`subject-nav ${kanbanActive ? 'active' : ''}`} onClick={onKanban} aria-current={kanbanActive ? 'page' : undefined}><Columns3 size={18}/>Канбан</button>
      <button className={`subject-nav ${analyticsActive ? 'active' : ''}`} onClick={onAnalytics} aria-current={analyticsActive ? 'page' : undefined}><BarChart3 size={18}/>Аналітика</button>
      <button className={`subject-nav ${profileActive ? 'active' : ''}`} onClick={onProfile} aria-current={profileActive ? 'page' : undefined}><UserRound size={18}/>Профіль</button>
      <div className="sidebar-label">ПРЕДМЕТИ <button className="icon" aria-label="Створити предмет" onClick={onCreate}>
    <Plus size={18}/>
    </button>
    </div>
      <nav aria-label="Предмети">{subjects.map(s => <button key={s.id} className={`subject-nav ${selectedId === s.id ? 'active' : ''}`} onClick={() => onSelect(s.id)}>
        <span className="subject-dot" style={{ background: subjectColor(s.color) }}/>
        <span>{s.name}</span>{selectedId === s.id && <ChevronRight size={15}/>}</button>)}{!initialLoading && subjects.length === 0 && <p className="nav-empty">Додай свій перший предмет</p>}</nav>
      <button className="add-subject" onClick={onCreate}>
    <Plus size={16}/> Новий предмет</button>
    </aside>;
}
