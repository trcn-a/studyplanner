import { useState, type FormEvent, type ReactNode } from 'react';
import { useAccount } from '../model/AccountContext';
import { accountApi } from '../api/accountApi';
export function ProfilePanel({children}: {children?: ReactNode}) {
 const { account, update, logout } = useAccount();
 const [busy, setBusy] = useState(false);
 const [message, setMessage] = useState('');
 async function save(event: FormEvent<HTMLFormElement>) {
  event.preventDefault(); const data = new FormData(event.currentTarget);
  const studyDays = data.getAll('studyDays').reduce<number>((mask, day) => mask | (1 << Number(day)), 0);
  if (!studyDays) { setMessage('Оберіть хоча б один навчальний день'); return; }
  setBusy(true); setMessage('');
  try { update(await accountApi.profile(String(data.get('name')), data.has('reminders'), Number(data.get('minutes')), studyDays)); setMessage('Збережено'); }
  catch (e) { setMessage(e instanceof Error ? e.message : 'Не вдалося зберегти'); } finally { setBusy(false); }
 }
 return <section className="profile-panel" aria-label="Обліковий запис">
  <div className="profile-heading"><div><h2>{account.name}</h2><p>{account.email}</p></div><button className="text-button" disabled={busy} onClick={async () => { setBusy(true); try { await logout(); } catch { setMessage('Не вдалося вийти'); setBusy(false); } }}>Вийти</button></div>
  <form onSubmit={save}><fieldset disabled={busy}>
   <label>Ім’я<input name="name" defaultValue={account.name} required maxLength={100}/></label>
   <h3>Сповіщення</h3>
   <label className="checkbox-label"><input type="checkbox" name="reminders" defaultChecked={account.remindersEnabled}/>Нагадування про заняття й дедлайни</label>
   <label>Нагадувати за, хв<input type="number" name="minutes" defaultValue={account.reminderMinutes} min={5} max={1440} required/></label>
   <fieldset className="study-days"><legend>Навчальні дні</legend>{['Пн','Вт','Ср','Чт','Пт','Сб','Нд'].map((day,index)=><label className="checkbox-label" key={day}><input type="checkbox" name="studyDays" value={index} defaultChecked={(account.studyDays & (1 << index)) !== 0}/>{day}</label>)}</fieldset>
   <button className="primary" disabled={busy}>{busy ? 'Збереження…' : 'Зберегти профіль'}</button>
  </fieldset></form>{message && <p role="status">{message}</p>}{children}
 </section>;
}
