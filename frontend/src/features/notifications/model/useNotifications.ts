import { useEffect, useRef, useState } from 'react';
import { notificationsApi, type Notice } from '../api/notificationsApi';
export function useNotifications(revision: string) {
 const [notices, setNotices] = useState<Notice[]>([]), [error, setError] = useState('');
 const generation = useRef(0);
 async function perform(operation: () => Promise<Notice[]>) { const version = ++generation.current; try { const result = await operation(); if (version === generation.current) { setNotices(result); setError(''); } } catch { if (version === generation.current) setError('Не вдалося оновити сповіщення'); } }
 useEffect(() => { void perform(notificationsApi.refresh); const timer = setInterval(() => void perform(notificationsApi.refresh), 60000); return () => { clearInterval(timer); ++generation.current; }; }, [revision]);
 return { notices, error, refresh: () => perform(notificationsApi.refresh), read: (id: string) => perform(() => notificationsApi.read(id)), readAll: () => perform(notificationsApi.readAll) };
}
