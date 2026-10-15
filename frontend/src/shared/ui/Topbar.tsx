import type { ReactNode } from 'react';

export function Topbar({ title, children }: { title: string; children?: ReactNode }) {
    return <header className="topbar">
    <span>{title}</span>
    <div className="topbar-actions"><span className="today">{new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date())}</span>{children}</div>
    </header>;
}
