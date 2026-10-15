import { createContext, useContext } from 'react';
import type { Prediction } from './types';
// Прогнози за id задачі: рядки списку, канбану й календаря читають їх звідси, не передаючи через кожен компонент.
export const PredictionContext = createContext<Record<string, Prediction>>({});
export const usePrediction = (taskId: string | undefined) => useContext(PredictionContext)[taskId ?? ''];
export const methodLabels = { USER_ESTIMATE: 'початкова оцінка користувача без коригування', REGRESSION: 'модель навчена на вашій історії', ESTIMATE_ADJUSTED: 'ваша оцінка з поправкою на минулі відхилення', HISTORY_MEDIAN: 'типовий час ваших виконаних задач' } as const;
export const confidenceLabels = { LOW: 'попередній прогноз', MEDIUM: 'попередній прогноз', HIGH: 'попередній прогноз' } as const;
