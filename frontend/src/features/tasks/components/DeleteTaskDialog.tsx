import { Modal } from '../../../shared/ui/Modal';
import type { Task } from '../model/types';
interface Props {
    task: Task;
    childCount: number;
    busy: boolean;
    error: string;
    onClose: () => void;
    onConfirm: () => void;
}
export function DeleteTaskDialog({ task, childCount, busy, error, onClose, onConfirm }: Props) {
    return <Modal title="Видалити задачу?" busy={busy} close={() => onClose()}>
    <p className="delete-message">«{task.title}» буде видалено{childCount > 0 ? ' разом з усіма підзадачами' : ''}. Цю дію неможливо скасувати.</p>{error && <p className="error" role="alert">{error}</p>}<div className="modal-footer">
    <button className="secondary" disabled={busy} onClick={() => onClose()}>Скасувати</button>
    <button className="primary destructive" disabled={busy} onClick={onConfirm}>{busy ? 'Видалення…' : 'Видалити'}</button>
    </div>
    </Modal>;
}
