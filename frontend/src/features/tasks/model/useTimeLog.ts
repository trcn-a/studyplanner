import { useEffect, useRef, useState } from 'react';
import type { Task, TimeEntry } from './types';
import type { useTasks } from './useTasks';
import type { useMutation } from '../../../shared/hooks/useMutation';

export function useTimeLog(tasks: ReturnType<typeof useTasks>, mutation: ReturnType<typeof useMutation>) {
    const [targetId, setTargetId] = useState<string | null>(null);
    const [timeEntries, setTimeEntries] = useState<TimeEntry[] | null>(null);
    const generation = useRef(0);
    useEffect(() => () => { ++generation.current; }, []);
    const timeTarget = tasks.currentTasks.find(task => task.id === targetId) ?? null;
    async function load(id: string) {
        const version = ++generation.current;
        try { const data = await tasks.fetchTimeEntries(id); if (version === generation.current) setTimeEntries(data); }
        catch (error) { if (version === generation.current) mutation.reportError(error); }
    }
    function openTimeLog(task: Task) { mutation.setError(''); setTimeEntries(null); setTargetId(task.id); void load(task.id); }
    function closeTimeLog() { ++generation.current; mutation.setError(''); setTargetId(null); setTimeEntries(null); }
    async function logTime(minutes: number, date: string) {
        if (!timeTarget) return;
        const id = timeTarget.id, version = generation.current;
        if (await mutation.mutate(() => tasks.logTime(timeTarget, minutes, date)) && version === generation.current) await load(id);
    }
    async function removeTimeEntry(entry: TimeEntry) {
        const version = generation.current;
        if (await mutation.mutate(() => tasks.removeTimeEntry(entry.id)) && version === generation.current) await load(entry.taskId);
    }
    return { timeTarget, timeEntries, openTimeLog, closeTimeLog, logTime, removeTimeEntry };
}
