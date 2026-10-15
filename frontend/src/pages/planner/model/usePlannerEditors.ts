import { useState } from 'react';
import type { useSubjects } from '../../../features/subjects/model/useSubjects';
import type { SubjectEditor } from '../../../features/subjects/model/types';
import type { Task, TaskEditor } from '../../../features/tasks/model/types';
import { colors, subjectColor } from '../../../features/subjects/model/colors';
import { blankTask } from '../../../features/tasks/model/constants';

export function usePlannerEditors(subjects: ReturnType<typeof useSubjects>, tasks: Task[], view: string, clearError: (value: string) => void) {
    const [subjectEditor, setSubjectEditor] = useState<SubjectEditor | null>(null);
    const [taskEditor, setTaskEditor] = useState<TaskEditor | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<Task | null>(null);
    function newSubject() { clearError(''); setSubjectEditor({ input: { name: '', description: '', color: colors[subjects.subjects.length % colors.length] } }); }
    function editSubject() { const subject = subjects.selected; if (!subject) return; clearError(''); setSubjectEditor({ id: subject.id, input: { name: subject.name, description: subject.description, color: subjectColor(subject.color) } }); }
    function openTask(task?: Task, parentId?: string, dueDate?: string, startTime?: string) {
        clearError('');
        setTaskEditor({ subjectId: task?.subjectId ?? tasks.find(item => item.id === parentId)?.subjectId ?? (view === 'subject' ? subjects.selectedId : ''), id: task?.id, parentId: task?.parentTaskId || parentId,
            input: task ? { scheduledStart: task.scheduledStart, scheduledEnd: task.scheduledEnd, title: task.title, description: task.description, status: task.status, priority: task.priority, dueDate: task.dueDate, estimatedMinutes: task.estimatedMinutes, planningTimeSource: task.planningTimeSource ?? 'PREDICTION', taskType: task.taskType ?? null, difficulty: task.difficulty ?? null }
            : { ...blankTask, dueDate: startTime ? null : dueDate ?? null, scheduledStart: startTime ? `${dueDate}T${startTime}:00` : null, scheduledEnd: startTime ? `${dueDate}T${String(Number(startTime.slice(0, 2)) + 1).padStart(2, '0')}:00:00` : null } });
    }
    return { subjectEditor, setSubjectEditor, taskEditor, setTaskEditor, deleteTarget, setDeleteTarget, newSubject, editSubject, openTask };
}
