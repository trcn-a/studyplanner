import { GoogleCalendarPanel } from '../../google-calendar/components/GoogleCalendarPanel';
import type { ChangeEvent } from 'react';
import { planningApi } from '../api/planningApi';
import type { ScheduleBlock, PlanningActions } from '../model/types';
interface Props { events: ScheduleBlock[]; busy: boolean; actions: PlanningActions; onNotice: (notice: string) => void; onChanged: () => void }
export function CalendarExchangePanel({ events, busy, actions, onNotice, onChanged }: Props) {
 async function importFile(e:ChangeEvent<HTMLInputElement>){
  const file=e.target.files?.[0];e.target.value='';if(!file)return;
  if(file.size>1000000){onNotice('Файл має бути не більшим за 1 МБ');return;}
  try{if(await actions.importCalendar(await file.text()))onNotice('Події імпортовано');}
  catch{onNotice('Не вдалося прочитати файл');}
 }
 return <details className="calendar-exchange"><summary>Зовнішні календарі</summary>
   <GoogleCalendarPanel onChanged={onChanged} />
   <p className="planning-hint">Імпорт .ics підтримує разові події.</p>
   <div className="planning-actions"><label className="secondary">Імпортувати .ics<input aria-label="Імпортувати календар" type="file" accept=".ics,text/calendar" disabled={busy} onChange={importFile}/></label><a className="secondary" href={planningApi.exportUrl} download="studyplanner.ics">Експортувати розклад</a></div>
   {events.filter(e => e.source !== 'GOOGLE').map(e=><div className="external-event-row" key={e.id}><span><strong>{e.title}</strong> · {e.start.replace('T',' ').slice(0,16)} — {e.end.replace('T',' ').slice(0,16)}</span><button className="text-button" disabled={busy} onClick={()=>void actions.removeEvent(e.id)}>Видалити</button></div>)}
  </details>;
}
