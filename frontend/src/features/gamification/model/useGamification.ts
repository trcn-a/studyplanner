import { useCallback, useEffect, useRef, useState } from 'react';
import { gamificationApi } from '../api/gamificationApi';
import type { Gamification } from './types';

export function useGamification(revision: string) {
 const [data, setData] = useState<Gamification | null>(null);
 const [error, setError] = useState('');
 const [notice, setNotice] = useState('');
 const previous = useRef<Gamification | null>(null);
 const generation = useRef(0);
 const refresh = useCallback(async () => {
  const id = ++generation.current;
  try {
   const next = await gamificationApi.summary();
   if (id !== generation.current) return;
   const old = previous.current;
   if (old && next.points > old.points) {
    const unlocked = next.achievements.filter(a => a.unlocked && !old.achievements.find(b => b.id === a.id)?.unlocked);
    setNotice(`+${next.points - old.points} балів${unlocked.length ? ' · ' + unlocked.map(a => a.title).join(', ') : ''}`);
   }
   previous.current = next; setData(next); setError('');
  } catch { if (id === generation.current) setError('Не вдалося завантажити досягнення'); }
 }, []);
 useEffect(() => {
  void refresh(); const timer = setInterval(() => void refresh(), 60000);
  return () => { clearInterval(timer); ++generation.current; };
 }, [revision, refresh]);
 useEffect(() => { if (!notice) return; const timer = setTimeout(() => setNotice(''), 7000); return () => clearTimeout(timer); }, [notice]);
 return { data, error, notice, refresh };
}
