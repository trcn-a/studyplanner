import type { FormEvent } from 'react';
import type { PlanningSettings } from '../model/types';
const days=['Понеділок','Вівторок','Середа','Четвер','П’ятниця','Субота','Неділя'];
interface Props { draft: PlanningSettings; setDraft: (draft: PlanningSettings | null) => void; busy: boolean; onSave: (event: FormEvent) => void }
export function AvailabilityForm({ draft, setDraft, busy, onSave }: Props) {
  return <form onSubmit={onSave} className="availability-editor">
   <h3>Доступний час</h3>
   {draft.availability.map((a,i)=><div className="availability-row" key={i}>
    <select aria-label="День тижня" value={a.dayOfWeek} onChange={e=>setDraft({...draft,availability:draft.availability.map((x,j)=>j===i?{...x,dayOfWeek:Number(e.target.value)}:x)})}>{days.map((name,j)=><option key={name} value={j+1}>{name}</option>)}</select>
    <input aria-label="Початок доступного часу" required type="time" value={a.start.slice(0,5)} onInput={e=>{const value=e.currentTarget.value;setDraft({...draft,availability:draft.availability.map((x,j)=>j===i?{...x,start:value}:x)});}}/>
    <span>—</span><input aria-label="Кінець доступного часу" required type="time" value={a.end.slice(0,5)} onInput={e=>{const value=e.currentTarget.value;setDraft({...draft,availability:draft.availability.map((x,j)=>j===i?{...x,end:value}:x)});}}/>
    <button type="button" className="text-button" aria-label={`Видалити проміжок ${i+1}`} onClick={()=>setDraft({...draft,availability:draft.availability.filter((_,j)=>j!==i)})}>Видалити</button>
   </div>)}
   <button type="button" className="secondary" onClick={()=>setDraft({...draft,availability:[...draft.availability,{dayOfWeek:1,start:'16:00',end:'18:00'}]})}>Додати проміжок</button>
   <div className="planning-options">
    <label>Часовий пояс<input required value={draft.zoneId} onChange={e=>setDraft({...draft,zoneId:e.target.value})}/></label>
    <label>План на днів<input type="number" min="1" max="90" required value={draft.horizonDays} onChange={e=>setDraft({...draft,horizonDays:Number(e.target.value)})}/></label>
    <label>Тривалість сесії, хв<input type="number" min="15" max="240" required value={draft.sessionMinutes} onChange={e=>setDraft({...draft,sessionMinutes:Number(e.target.value)})}/></label>
    <label>Перерва між сесіями, хв<input type="number" min="0" max="120" required value={draft.breakMinutes ?? 0} onChange={e=>setDraft({...draft,breakMinutes:Number(e.target.value)})}/></label>
   </div>
   <label className="planning-checkbox"><input type="checkbox" checked={draft.autoReplan} onChange={e=>setDraft({...draft,autoReplan:e.target.checked})}/>Автоматичне перепланування</label>
   <label className="planning-checkbox"><input type="checkbox" checked={draft.habitAware} onChange={e=>setDraft({...draft,habitAware:e.target.checked})}/>Враховувати звичні години роботи</label>
   <p className="planning-hint">Зміни діятимуть після перепланування.</p>
   <div className="planning-actions"><button className="primary" disabled={busy}>Зберегти</button><button type="button" className="secondary" onClick={()=>setDraft(null)}>Скасувати</button></div>
  </form>;
}
