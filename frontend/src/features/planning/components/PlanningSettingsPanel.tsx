import { useState, type FormEvent } from 'react';
import { usePlanning } from '../model/usePlanning';
import type { PlanningSettings } from '../model/types';
import { AvailabilityForm } from './AvailabilityForm';
import { CalendarExchangePanel } from './CalendarExchangePanel';
export function PlanningSettingsPanel({revision}:{revision:string}) {
 const planning=usePlanning(revision);
 const [draft,setDraft]=useState<PlanningSettings | null>(null);
 const [notice,setNotice]=useState('');
 const [expanded,setExpanded]=useState(false);
 async function save(event:FormEvent) {
  event.preventDefault();
  const settings=draft ?? planning.state?.settings;
  if(settings && await planning.actions.saveSettings(settings)){setDraft(null);setNotice('Налаштування збережено');}
 }
 return <section className="profile-settings" aria-label="Налаштування планування">
  <button className="secondary" aria-expanded={expanded} aria-controls="profile-planning-settings" onClick={()=>setExpanded(!expanded)}>Налаштування планування</button>
  <div id="profile-planning-settings" hidden={!expanded}>
  {planning.error && <p className="error" role="alert">{planning.error}</p>}
  {notice && <p role="status">{notice}</p>}
  {!planning.state ? <p role="status">Завантаження…</p> : <>
   <fieldset disabled={planning.busy}><AvailabilityForm draft={draft ?? planning.state.settings} setDraft={setDraft} busy={planning.busy} onSave={save}/></fieldset>
   <CalendarExchangePanel events={planning.state.externalEvents} busy={planning.busy} actions={planning.actions} onNotice={setNotice} onChanged={()=>void planning.actions.refresh()}/>
  </>}
  </div>
 </section>;
}
