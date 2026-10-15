import { useEffect, useState } from 'react';
const pad = (value: number) => String(value).padStart(2, '0');
export function TimerClock({ since }: { since: string }) {
    const [now, setNow] = useState(() => Date.now());
    useEffect(() => { const id = window.setInterval(() => setNow(Date.now()), 1000); return () => window.clearInterval(id); }, []);
    const seconds = Math.max(0, Math.floor((now - new Date(since).getTime()) / 1000));
    return <time>{pad(Math.floor(seconds / 3600))}:{pad(Math.floor(seconds / 60) % 60)}:{pad(seconds % 60)}</time>;
}
