import { useEffect, useState } from 'react';
import { request } from '../../../shared/api/http';
import { formatMinutes } from '../../../shared/lib/duration';
import type { PlanningState } from '../../planning/model/types';
import { timeApi } from '../api/timeApi';
import type { Task } from '../model/types';

interface SessionItem { id: string; start: string; end: string | null; minutes: number | null; label: string; kind: string }
export function TaskSessions({task}: {task: Task}) {
 const [items,setItems]=useState<SessionItem[]>([]);
 const [loading,setLoading]=useState(true);
 const [error,setError]=useState('');
 const [zone,setZone]=useState('Europe/Kyiv');
 useEffect(()=>{
  let active=true;
  setLoading(true);setError('');
  Promise.all([request<PlanningState>('/planning'),timeApi.entries(task.id)]).then(([plan,entries])=>{
   if(!active)return;
   const planned: SessionItem[]=plan.sessions.filter(s=>s.taskId===task.id && (s.source==='AUTO'||s.source==='PINNED')).map(s=>({id:s.id,start:s.start,end:s.end,minutes:null,label:s.source==='PINNED'?'Перенесено вручну':'Заплановано автоматично',kind:s.source==='PINNED'?'manual':'auto'}));
   if(task.scheduledStart && task.scheduledEnd)planned.push({id:'manual-'+task.id,start:task.scheduledStart,end:task.scheduledEnd,minutes:null,label:'Заплановано вручну',kind:'manual'});
   const actual: SessionItem[]=entries.map(s=>({id:s.id,start:s.startedAt,end:s.endedAt,minutes:s.minutes,label:s.endedAt?'Завершено':'Триває',kind:s.endedAt?'completed':'running'}));
   const localTimestamp=(value:string)=>value.slice(0,16);
   const dateKey=(value:string)=>/[Zz]|[+-]\d\d:\d\d$/.test(value)?new Intl.DateTimeFormat('sv-SE',{timeZone:plan.settings.zoneId,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date(value)).replace(' ','T'):localTimestamp(value);
   setItems([...planned,...actual].sort((a,b)=>dateKey(b.start).localeCompare(dateKey(a.start))));setZone(plan.settings.zoneId);
  }).catch(e=>{if(active)setError(e instanceof Error?e.message:'Не вдалося завантажити заняття');}).finally(()=>{if(active)setLoading(false);});
  return()=>{active=false;};
 },[task.id,task.scheduledStart,task.scheduledEnd]);
 function label(value:string) {
  const instant=/[Zz]|[+-]\d\d:\d\d$/.test(value);
  return new Intl.DateTimeFormat('uk-UA',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit',...(instant?{timeZone:zone}:{})}).format(new Date(value));
 }
 return <section className="task-session-list" aria-label="Навчальні сесії">
  <h3>Навчальні сесії</h3>
  {loading?<p role="status">Завантаження…</p>:error?<p role="alert" className="error">{error}</p>:items.length?<ul>{items.map(item=><li key={item.id} className={`session-${item.kind}`}>
   <strong>{item.label}</strong><span>{label(item.start)}{item.end?` — ${label(item.end)}`:''}</span>{item.minutes != null && <small>{formatMinutes(item.minutes)}</small>}
  </li>)}</ul>:<p>Занять ще немає.</p>}
 </section>;
}
