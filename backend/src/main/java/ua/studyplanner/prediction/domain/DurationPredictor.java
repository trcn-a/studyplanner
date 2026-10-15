package ua.studyplanner.prediction.domain;

import java.util.*;
import ua.studyplanner.task.dto.TaskSample;
import ua.studyplanner.task.domain.Priority;
import ua.studyplanner.prediction.dto.*;
import static ua.studyplanner.prediction.domain.PredictionFeatures.*;

public final class DurationPredictor {
    public static final int MIN_SAMPLES_FOR_MODEL = 8;
    public static final double FALLBACK_SIGMA = 0.5;
    public static final double DEFAULT_LAMBDA = 3.0;
    public PredictionView userEstimate(TaskSample sample) {
        int minutes=sample.estimatedMinutes();
        return new PredictionView(sample.id(),minutes,minutes,minutes,PredictionMethod.USER_ESTIMATE,Confidence.LOW,0);
    }

    public RidgeRegression fit(List<TaskSample> history) {
        return fit(history, DEFAULT_LAMBDA);
    }

    public RidgeRegression fit(List<TaskSample> history, double lambda) {
        Stats stats = Stats.of(history);
        double[][] x = new double[history.size()][];
        double[] y = new double[history.size()];
        for (int i = 0; i < history.size(); i++) {
            TaskSample sample = history.get(i);
            double log = Math.log(sample.actualMinutes());
            Double ratio = sample.estimatedMinutes() == null ? null : log - Math.log(sample.estimatedMinutes());
            x[i] = features(sample, stats, log, ratio);
            y[i] = log - offset(sample);
        }
        return RidgeRegression.fit(x, y, lambda);
    }

    public PredictionView predict(
            TaskSample s, Stats stats, int n, RidgeRegression model, double sigma) {
        PredictionMethod method;
        double mu;
        Integer estimate = s.estimatedMinutes();
        // Регресія надійна лише там, де в навчанні були задачі того ж типу (з оцінкою / без неї).
        boolean supported = (estimate != null ? stats.withEstimate : n - stats.withEstimate) >= MIN_SAMPLES_FOR_MODEL / 2;
        if (model != null && supported) {
            method = PredictionMethod.REGRESSION;
            mu = model.predict(features(s, stats, null, null)) + offset(s);
            if (estimate != null) {
                // Модель не має суперечити власній оцінці більше ніж вчетверо.
                mu = Math.min(Math.log(estimate) + Math.log(4), Math.max(Math.log(estimate) - Math.log(4), mu));
            } else {
                mu = Math.min(stats.maxLog + Math.log(2), Math.max(stats.minLog - Math.log(2), mu));
            }
        } else if (estimate != null && stats.total.ratioCount > 0) {
            method = PredictionMethod.ESTIMATE_ADJUSTED;
            Sums subject = stats.bySubject.getOrDefault(s.subjectId(), Sums.EMPTY);
            double userBias = stats.total.ratioSum / stats.total.ratioCount;
            mu = Math.log(estimate) + (subject.ratioSum + SHRINK * userBias) / (subject.ratioCount + SHRINK);
        } else if (estimate != null) {
            return userEstimate(s);
        } else {
            method = PredictionMethod.HISTORY_MEDIAN;
            mu = Math.log(stats.median(s.subjectId()));
        }

        int minutes = clampMinutes(Math.exp(mu));
        int low = Math.min(minutes, clampMinutes(Math.exp(mu - sigma)));
        int high = Math.max(minutes, clampMinutes(Math.exp(mu + sigma)));
        // No calibrated confidence level is available yet.
        Confidence confidence = Confidence.LOW;
        return new PredictionView(s.id(), minutes, low, high, method, confidence, n);
    }

    private static int clampMinutes(double value) {
        return (int) Math.max(1, Math.min(Math.round(value), 100_000));
    }


    public List<PredictionView> predictAll(List<TaskSample> history, List<TaskSample> pending, double sigma) {
        return predictAll(history, pending, sigma, DEFAULT_LAMBDA);
    }

    public List<PredictionView> predictAll(List<TaskSample> history, List<TaskSample> pending, double sigma, double lambda) {
        Stats stats = Stats.of(history);
        RidgeRegression model = history.size() >= MIN_SAMPLES_FOR_MODEL ? fit(history, lambda) : null;
        return pending.stream().map(sample -> predict(sample, stats, history.size(), model, sigma)).toList();
    }
}
