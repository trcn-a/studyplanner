import { request } from '../../../shared/api/http';
import type { TaskEditor } from '../../tasks/model/types';
import type { Prediction, PredictionReport } from '../model/types';
export const predictionsApi = {
    report: () => request<PredictionReport>('/predictions'),
    preview: (editor: TaskEditor) => request<Prediction | undefined>('/predictions/preview', 'POST', { subjectId: editor.subjectId, taskId: editor.id, parentId: editor.parentId, input: editor.input })
};
