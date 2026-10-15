import { useEffect, useMemo, useState } from 'react';
import { predictionsApi } from '../api/predictionsApi';
import type { ModelInfo, Prediction } from './types';
export function usePredictions(revision: string) {
    const [list, setList] = useState<Prediction[]>([]);
    const [model, setModel] = useState<ModelInfo | null>(null);
    useEffect(() => {
        let active = true;
        predictionsApi.report().then(report => {
            if (active) { setList(report.predictions); setModel(report.model); }
        }).catch(() => { if (active) { setList([]); setModel(null); } });
        return () => { active = false; };
    }, [revision]);
    const predictions = useMemo(() => Object.fromEntries(list.map(item => [item.taskId,item])) as Record<string,Prediction>, [list]);
    return { predictions, model };
}
