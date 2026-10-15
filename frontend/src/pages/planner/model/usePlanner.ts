import { usePredictions } from '../../../features/prediction/model/usePredictions';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation } from '../../../shared/hooks/useMutation';
import { useSubjects } from '../../../features/subjects/model/useSubjects';
import { useTasks } from '../../../features/tasks/model/useTasks';
import { useTimeLog } from '../../../features/tasks/model/useTimeLog';
import { usePlannerEditors } from './usePlannerEditors';
import { selectTaskGroups } from '../../../features/tasks/model/selectors';
import type { Task, Status } from '../../../features/tasks/model/types';
export function usePlanner() {
    const mutation = useMutation();
    const subjectState = useSubjects(mutation.reportError);
    const [view, setView] = useState<'subject' | 'calendar' | 'kanban' | 'analytics' | 'all' | 'profile'>('calendar');
    const taskState = useTasks(view !== 'subject' ? subjectState.subjects.map(s => s.id) : subjectState.selectedId ? [subjectState.selectedId] : [], mutation.reportError);
    const predictionState = usePredictions(JSON.stringify(taskState.currentTasks));
    const [search, setSearch] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [priorityFilter, setPriorityFilter] = useState('');
    const { subjectEditor, setSubjectEditor, taskEditor, setTaskEditor, deleteTarget, setDeleteTarget, newSubject, editSubject, openTask } = usePlannerEditors(subjectState, taskState.currentTasks, view, mutation.setError);
    const { timeTarget, timeEntries, openTimeLog, closeTimeLog, logTime, removeTimeEntry } = useTimeLog(taskState, mutation);
    function chooseSubject(id: string) {
        setView('subject');
        subjectState.setSelectedId(id);
        setSearch('');
        setStatusFilter('');
        setPriorityFilter('');
        mutation.setError('');
    }
    async function saveSubject(event: FormEvent) {
        event.preventDefault();
        if (!subjectEditor)
            return;
        await mutation.mutate(async () => {
            const subject = await subjectState.saveSubject(subjectEditor.input, subjectEditor.id);
            chooseSubject(subject.id);
            setSubjectEditor(null);
        });
    }
    async function saveTask(event: FormEvent) {
        event.preventDefault();
        if (!taskEditor || !taskEditor.subjectId)
            return;
        await mutation.mutate(async () => { await taskState.saveTask(taskEditor); setTaskEditor(null); });
    }
    async function confirmDelete() {
        if (!deleteTarget)
            return;
        await mutation.mutate(async () => { await taskState.deleteTask(deleteTarget.id); setDeleteTarget(null); });
    }
    const actions = {
        onStatus: (task: Task, status: Status) => { if (!mutation.busy && task.status !== status) void mutation.mutate(() => taskState.changeStatus(task, status)); },
        onToggle: (task: Task) => { void mutation.mutate(() => taskState.toggleStatus(task)); },
        onEdit: (task: Task) => openTask(task),
        onAddChild: (id: string) => openTask(undefined, id),
        onDelete: (task: Task) => { mutation.setError(''); setDeleteTarget(task); },
        onTimer: (task: Task) => { void mutation.mutate(() => task.timerStartedAt ? taskState.stopTimer(task) : taskState.startTimer(task)); },
        onLogTime: openTimeLog,
    };
    return {
        showAll: () => { setView('all'); setSearch(''); setStatusFilter(''); setPriorityFilter(''); mutation.setError(''); },
        showProfile: () => { setView('profile'); mutation.setError(''); },
        showKanban: () => { setView('kanban'); setSearch(''); setStatusFilter(''); setPriorityFilter(''); mutation.setError(''); },
        changeStatus: (task: Task, status: Status) => { if (!mutation.busy && task.status !== status) void mutation.mutate(() => taskState.changeStatus(task, status)); },
        showAnalytics: () => { setView('analytics'); setSearch(''); setStatusFilter(''); setPriorityFilter(''); mutation.setError(''); },
        view, showCalendar: () => { setView('calendar'); setSearch(''); setStatusFilter(''); setPriorityFilter(''); mutation.setError(''); },
        subjects: subjectState.subjects, selectedId: subjectState.selectedId, selected: subjectState.selected,
        initialLoading: subjectState.initialLoading, currentTasks: taskState.currentTasks, loading: taskState.loading,
        busy: mutation.busy, error: mutation.error, actions, reloadTasks: taskState.reload,
        ...selectTaskGroups(taskState.currentTasks, search, statusFilter, priorityFilter),
        filters: { search, setSearch, statusFilter, setStatusFilter, priorityFilter, setPriorityFilter },
        predictions: predictionState.predictions, model: predictionState.model,
        timeTarget, timeEntries, closeTimeLog, logTime, removeTimeEntry,
        subjectEditor, setSubjectEditor, taskEditor, setTaskEditor, deleteTarget, setDeleteTarget,
        chooseSubject, newSubject, editSubject, openTask, saveSubject, saveTask, confirmDelete,
    };
}
