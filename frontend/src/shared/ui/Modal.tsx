import { useEffect, useRef, useId } from 'react';
import type { ReactNode } from 'react';
import { X } from 'lucide-react';
export function Modal({ title, children, close, busy, className }: {
    className?: string;
    title: string;
    children: ReactNode;
    close: () => void;
    busy: boolean;
}) {
    const titleId = useId();
    const ref = useRef<HTMLDialogElement>(null);
    useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close(); }, []);
    return <dialog className={className} ref={ref} onCancel={e => { e.preventDefault(); if (!busy)
        close(); }} aria-labelledby={titleId}>
    <div className="modal-heading">
    <h2 id={titleId}>{title}</h2>
    <button className="icon" aria-label="Закрити" disabled={busy} onClick={close}>
    <X size={20}/>
    </button>
    </div>{children}
  </dialog>;
}
