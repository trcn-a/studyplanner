export type PredictionMethod = 'USER_ESTIMATE' | 'REGRESSION' | 'ESTIMATE_ADJUSTED' | 'HISTORY_MEDIAN';
export type Confidence = 'LOW' | 'MEDIUM' | 'HIGH';
export interface Prediction {
    taskId: string;
    minutes: number;
    lowMinutes: number;
    highMinutes: number;
    method: PredictionMethod;
    confidence: Confidence;
    samples: number;
}
export interface ModelInfo {
    trained: boolean;
    samples: number;
    minSamples: number;
    errorPercent: number | null;
    baselineErrorPercent: number | null;
    medianBaselineErrorPercent: number | null;
    evaluationSamples: number;
    regularization: number;
    regularizationTuned: boolean;
    tuningSamples: number;
    regressionEvaluationSamples: number;
}
export interface PredictionReport {
    model: ModelInfo;
    predictions: Prediction[];
}
