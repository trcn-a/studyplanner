import { useState, type ReactNode } from 'react';
import { PlanningIssues } from './PlanningIssues';
import type { PlanningState, PlanningActions } from '../model/types';
interface Props { state: PlanningState | null; busy: boolean; error: string; actions: PlanningActions; onSettings: () => void; children?: ReactNode }
export function PlanningTools({state,busy,error,actions,onSettings,children}:Props){
 const [notice,setNotice]=useState('');
 return <section className="planning-tools" aria-label="Автоматичне планування">
  <div className="planning-actions">
   <button className="secondary" onClick={onSettings}>Налаштування планування</button>
   <button className="primary" disabled={busy || !state} onClick={async()=>{if(await actions.generate())setNotice('Розклад сформовано');}}>{busy?'Оновлюємо…':state?.sessions.length?'Перерозподілити':'Сформувати розклад'}</button>
   {children && <div className="planning-create">{children}</div>}
  </div>
  {error && <p className="error" role="alert">{error}</p>}
  {notice && <p role="status" className="planning-hint">{notice}</p>}
  {state && !state.settings.availability.length && <p className="planning-hint">Задайте доступний час у профілі.</p>}
  <PlanningIssues issues={state?.issues ?? []}/>
 </section>;
}
