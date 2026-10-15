import { useEffect, useState } from 'react';
import { subjectsApi } from '../api/subjectsApi';
import type { Subject, SubjectInput } from './types';
export function useSubjects(onError: (error: unknown) => void) {
    const [subjects, setSubjects] = useState<Subject[]>([]);
    const [selectedId, setSelectedId] = useState('');
    const [initialLoading, setInitialLoading] = useState(true);
    useEffect(() => {
        let active = true;
        subjectsApi.list().then(data => {
            if (active) {
                setSubjects(data);
                setSelectedId(data[0]?.id || '');
            }
        }).catch(error => { if (active)
            onError(error); })
            .finally(() => { if (active)
            setInitialLoading(false); });
        return () => { active = false; };
    }, [onError]);
    async function saveSubject(input: SubjectInput, id?: string) {
        const saved = await subjectsApi.save(input, id);
        setSubjects(previous => id ? previous.map(subject => subject.id === id ? saved : subject) : [...previous, saved]);
        setSelectedId(saved.id);
        return saved;
    }
    return { subjects, selectedId, setSelectedId, initialLoading, saveSubject, selected: subjects.find(subject => subject.id === selectedId) };
}
