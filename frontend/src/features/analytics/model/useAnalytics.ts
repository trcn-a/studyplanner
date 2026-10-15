import { useEffect, useRef, useState } from 'react';
import { analyticsApi } from '../api/analyticsApi';
import type { Analytics } from './types';

/** Завантажує показники за період; відповіді застарілих запитів відкидаються. */
export function useAnalytics(from: string, to: string, revision: string) {
 const [data, setData] = useState<Analytics | null>(null);
 const [loading, setLoading] = useState(true);
 const [error, setError] = useState('');
 const generation = useRef(0);
 useEffect(() => {
  const id = ++generation.current;
  if (!from || !to || from > to) { setLoading(false); setError('Початок періоду має бути не пізніше за його кінець'); return; }
  setLoading(true);
  analyticsApi.report(from, to).then(report => {
   if (id !== generation.current) return;
   setData(report); setError('');
  }).catch(e => {
   if (id === generation.current) setError(e instanceof Error ? e.message : 'Не вдалося завантажити аналітику');
  }).finally(() => { if (id === generation.current) setLoading(false); });
  return () => { ++generation.current; };
 }, [from, to, revision]);
 return { data, loading, error };
}
