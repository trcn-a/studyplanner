import { Bell } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useNotifications } from '../model/useNotifications';
export function NotificationPanel({ revision }: { revision: string }) {
 const { notices, error, refresh, read, readAll } = useNotifications(revision);
 const unread = notices.filter(n => !n.read).length;
 const panel = useRef<HTMLDetailsElement>(null);
 useEffect(() => {
  function closeOutside(event: PointerEvent) {
   if (panel.current && !panel.current.contains(event.target as Node)) panel.current.open = false;
  }
  function closeOnEscape(event: KeyboardEvent) {
   if (event.key === 'Escape' && panel.current?.open) {
    panel.current.open = false;
    panel.current.querySelector('summary')?.focus();
   }
  }
  document.addEventListener('pointerdown', closeOutside);
  document.addEventListener('keydown', closeOnEscape);
  return () => {
   document.removeEventListener('pointerdown', closeOutside);
   document.removeEventListener('keydown', closeOnEscape);
  };
 }, []);
 return <details ref={panel} className="notification-panel"><summary aria-label={unread ? `Сповіщення: ${unread} непрочитаних` : 'Сповіщення'} title="Сповіщення"><Bell size={20} aria-hidden="true"/>{unread > 0 && <strong className="unread-count" aria-hidden="true">{unread}</strong>}</summary>
 <div className="notification-dropdown"><h2>Сповіщення</h2>
 <div className="notification-actions"><button className="text-button" onClick={() => void refresh()}>Оновити</button><button className="text-button" disabled={!unread} onClick={() => void readAll()}>Прочитати всі</button></div>
 {error && <p role="alert" className="error">{error}</p>}
 {!notices.length && <p>Нових сповіщень поки немає.</p>}
 <div className="notification-list">{notices.map(n => <article key={n.id} className={n.read ? 'notice read' : 'notice'}><strong>{n.title}</strong><p>{n.body}</p><small>{new Date(n.createdAt).toLocaleString('uk-UA')}</small>{!n.read && <button className="text-button" aria-label={`Прочитати: ${n.title}`} onClick={() => void read(n.id)}>Прочитано</button>}</article>)}</div>
 </div></details>;
}
