import { useEffect, useRef, useState } from 'react';
import { tasksApi } from '../api/tasksApi';
import { timeApi } from '../api/timeApi';
import type { Task, TaskEditor, Status } from './types';
export function useTasks(subjectIds: string[], onError: (error: unknown) => void) {
    const scope = JSON.stringify(subjectIds);
    const [tasks, setTasks] = useState<Task[]>([]);
    const [loadedSubject, setLoadedSubject] = useState('');
    const [loading, setLoading] = useState(false);
    const [revision, setRevision] = useState(0);
    const requestVersion = useRef(0);
    useEffect(() => {
        let active = true;
        const version = ++requestVersion.current;
        if (scope === '[]') {
            setTasks([]);
            setLoadedSubject(scope);
            setLoading(false);
            return;
        }
        setLoading(true);
        Promise.all((JSON.parse(scope) as string[]).map(id => tasksApi.list(id))).then(groups => groups.flat()).then(data => {
            if (active && version === requestVersion.current) {
                setTasks(data);
                setLoadedSubject(scope);
            }
        }).catch(error => { if (active) {
            onError(error);
            setTasks([]);
            setLoadedSubject(scope);
        } })
            .finally(() => { if (active)
            setLoading(false); });
        return () => { active = false; };
    }, [scope, revision, onError]);
    function replaceTask(updated: Task) {
        setTasks(previous => previous.map(item => item.id === updated.id ? updated : item));
    }
    async function saveTask(editor: TaskEditor) {
        await tasksApi.save(editor.subjectId, editor.input, editor.id, editor.parentId);
        setRevision(value => value + 1);
    }
    async function changeStatus(task: Task, status: Status) {
        replaceTask(await tasksApi.updateStatus(task.id, status));
    }
    async function toggleStatus(task: Task) {
        replaceTask(await tasksApi.updateStatus(task.id, task.status === 'DONE' ? 'TODO' : 'DONE'));
    }
    async function deleteTask(id: string) {
        await tasksApi.remove(id);
        setRevision(value => value + 1);
    }
    async function startTimer(task: Task) {
        replaceTask(await timeApi.startTimer(task.id));
    }
    async function stopTimer(task: Task) {
        replaceTask(await timeApi.stopTimer(task.id));
    }
    async function logTime(task: Task, minutes: number, date: string) {
        replaceTask(await timeApi.log(task.id, minutes, date));
    }
    async function removeTimeEntry(entryId: string) {
        replaceTask(await timeApi.removeEntry(entryId));
    }
    return { reload: () => setRevision(value=>value+1), currentTasks: loadedSubject === scope ? tasks : [], loading: loading || (scope !== '[]' && loadedSubject !== scope), saveTask, toggleStatus, changeStatus, deleteTask, startTimer, stopTimer, logTime, removeTimeEntry, fetchTimeEntries: timeApi.entries };
}
