import { useGoogleCalendar } from '../model/useGoogleCalendar';
export function GoogleCalendarPanel({ onChanged }: { onChanged: () => void }) {
 const { status, busy, error, perform } = useGoogleCalendar(onChanged);
 return <section aria-label="Google Calendar">
  <h3>Google Calendar</h3>
  {error && <p role="alert" className="error">{error}</p>}
  {status?.error && <p role="alert" className="error">{status.error}</p>}
  {status && !status.configured && <p>Google Calendar наразі недоступний.</p>}
  {status?.connected ? <>
   <p className="planning-hint">Підключено</p>
   {status.lastSync && <p className="planning-hint">Оновлено: {new Date(status.lastSync).toLocaleString('uk-UA')}</p>}
   <div className="planning-actions"><button className="secondary" disabled={busy || !status.configured} onClick={() => void perform('sync')}>Оновити зараз</button><button className="text-button" disabled={busy} onClick={() => void perform('disconnect')}>Відключити</button><button className="text-button" disabled={busy || !status.configured} onClick={() => void perform('connect')}>Підключити повторно</button></div>
  </> : status?.configured && <button className="secondary" disabled={busy} onClick={() => void perform('connect')}>Підключити Google Calendar</button>}
 </section>;
}
