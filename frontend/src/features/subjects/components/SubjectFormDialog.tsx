import type { FormEvent } from 'react';
import { Modal } from '../../../shared/ui/Modal';
import { Check } from 'lucide-react';
import { colors } from '../model/colors';
import type { SubjectEditor } from '../model/types';
interface Props {
    subjectEditor: SubjectEditor;
    setSubjectEditor: (editor: SubjectEditor) => void;
    busy: boolean;
    error: string;
    onSubmit: (event: FormEvent) => void;
    onClose: () => void;
}
export function SubjectFormDialog({ subjectEditor, setSubjectEditor, busy, error, onSubmit, onClose }: Props) {
    return <Modal title={subjectEditor.id ? 'Редагувати предмет' : 'Новий предмет'} busy={busy} close={() => onClose()}>
    <form onSubmit={onSubmit}>
    <fieldset disabled={busy}>
    <label>Назва предмета<input autoFocus required maxLength={120} value={subjectEditor.input.name} onChange={e => setSubjectEditor({ ...subjectEditor, input: { ...subjectEditor.input, name: e.target.value } })} placeholder="Наприклад, бази даних"/>
    </label>
    <label>Опис<textarea maxLength={2000} rows={3} value={subjectEditor.input.description} onChange={e => setSubjectEditor({ ...subjectEditor, input: { ...subjectEditor.input, description: e.target.value } })} placeholder="Що вивчаємо цього семестру?"/>
    </label>
    <label>Колір предмета</label>
    <div className="color-options">{colors.map(color => <button key={color} type="button" className="color-option" style={{ background: color }} aria-label={`Колір ${color}`} aria-pressed={subjectEditor.input.color === color} onClick={() => setSubjectEditor({ ...subjectEditor, input: { ...subjectEditor.input, color } })}>{subjectEditor.input.color === color && <Check size={19}/>}</button>)}</div>
    </fieldset>{error && <p className="error" role="alert">{error}</p>}<div className="modal-footer">
    <button type="button" className="secondary" disabled={busy} onClick={() => onClose()}>Скасувати</button>
    <button className="primary" disabled={busy || !subjectEditor.input.name.trim()}>{busy ? 'Збереження…' : 'Зберегти предмет'}</button>
    </div>
    </form>
    </Modal>;
}
