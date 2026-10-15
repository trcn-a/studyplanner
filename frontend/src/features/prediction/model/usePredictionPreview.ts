import { useEffect, useState } from 'react';
import type { TaskEditor } from '../../tasks/model/types';
import { predictionsApi } from '../api/predictionsApi';
import type { Prediction } from './types';

export function usePredictionPreview(editor: TaskEditor) {
    const { subjectId, id, parentId, input } = editor;
    const key = JSON.stringify([subjectId, id, parentId, input.title, input.priority, input.estimatedMinutes, input.taskType, input.difficulty]);
    const valid = !!subjectId && !!input.title.trim() && input.title.length <= 200 && input.description.length <= 5000
        && (input.estimatedMinutes == null || Number.isInteger(input.estimatedMinutes) && input.estimatedMinutes > 0 && input.estimatedMinutes <= 2147483647);
    const [result, setResult] = useState<{ key: string; prediction?: Prediction; error?: string }>();
    useEffect(() => {
        if (!valid) return;
        let active = true;
        const timer = window.setTimeout(() => {
            // Scheduling fields and the selected source do not affect the prediction.
            predictionsApi.preview({ ...editor, input: { ...input, scheduledStart: null, scheduledEnd: null, planningTimeSource: 'PREDICTION' } })
                .then(prediction => { if (active) setResult({ key, prediction }); })
                .catch(error => { if (active) setResult({ key, error: error.message }); });
        }, 400);
        return () => { active = false; window.clearTimeout(timer); };
        // The key contains all fields used to compute the preview.
    }, [key, valid]);
    return { prediction: result?.key === key ? result.prediction : undefined,
        error: result?.key === key ? result.error : undefined,
        loading: valid && result?.key !== key, valid };
}
