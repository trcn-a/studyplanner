import { useEffect, useState } from 'react';
import { googleCalendarApi, type GoogleStatus } from '../api/googleCalendarApi';
export function useGoogleCalendar(onChanged: () => void) {
 const [status, setStatus] = useState<GoogleStatus | null>(null);
 const [busy, setBusy] = useState(false);
 const [error, setError] = useState('');
 useEffect(() => {
  let active = true;
  async function refresh() {
   try { const value = await googleCalendarApi.status(); if (active) setStatus(value); }
   catch { if (active) setError('Не вдалося перевірити підключення Google Calendar'); }
  }
  void refresh(); const timer = setInterval(() => void refresh(), 60000);
  const params = new URLSearchParams(window.location.search);
  if (params.get('google') === 'error') setError('Підключення не завершене. Спробуйте ще раз.');
  if (params.has('google')) { params.delete('google'); window.history.replaceState(null, '', window.location.pathname + (params.size ? '?' + params : '') + window.location.hash); }
  return () => { active = false; clearInterval(timer); };
 }, []);
 async function perform(action: 'connect' | 'sync' | 'disconnect') {
  setBusy(true); setError('');
  try {
   if (action === 'connect') window.location.assign((await googleCalendarApi.connect()).url);
   else { setStatus(await googleCalendarApi[action]()); onChanged(); }
  } catch (e) { setError(e instanceof Error ? e.message : 'Не вдалося оновити календар'); }
  finally { setBusy(false); }
 }
 return { status, busy, error, perform };
}
