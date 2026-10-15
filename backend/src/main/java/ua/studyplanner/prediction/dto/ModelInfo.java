package ua.studyplanner.prediction.dto;

/**
 * Стан моделі та MAPE стратегії прогнозування (включно з резервними способами).
 * Усі похибки обчислені на тих самих evaluationSamples завданнях.
 * Перевірка використовує лише результати, доступні до створення перевірочного завдання.
 */
public record ModelInfo(
        boolean trained,
        int samples,
        int minSamples,
        Double errorPercent,
        Double baselineErrorPercent,
        Double medianBaselineErrorPercent,
        int evaluationSamples,
        double regularization,
        boolean regularizationTuned,
        int tuningSamples,
        int regressionEvaluationSamples) {}
