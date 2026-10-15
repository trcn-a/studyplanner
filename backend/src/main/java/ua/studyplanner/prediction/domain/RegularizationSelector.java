package ua.studyplanner.prediction.domain;

import java.util.*;
import ua.studyplanner.task.dto.TaskSample;
import static ua.studyplanner.prediction.domain.DurationPredictor.*;
import static ua.studyplanner.prediction.domain.PredictionFeatures.*;

/** Selects lambda using only chronological folds within the supplied training history. */
public final class RegularizationSelector {
    private static final double[] CANDIDATES = {DEFAULT_LAMBDA, 0.1, 1.0, 10.0, 30.0};
    private static final int MIN_VALIDATION_SAMPLES = 3;
    private static final int MAX_VALIDATION_SAMPLES = 20;
    public record Selection(double lambda, boolean tuned, int samples) {}

    public Selection select(List<TaskSample> history) {
        var folds = PredictionEvaluator.folds(history).stream().filter(f -> {
            int withEstimate = (int) f.training().stream().filter(s -> s.estimatedMinutes() != null).count();
            int sameType = f.target().estimatedMinutes() != null ? withEstimate : f.training().size() - withEstimate;
            return f.training().size() >= MIN_SAMPLES_FOR_MODEL && sameType >= MIN_SAMPLES_FOR_MODEL / 2;
        }).toList();
        if (folds.size() < MIN_VALIDATION_SAMPLES)
            return new Selection(DEFAULT_LAMBDA, false, folds.size());
        folds = folds.subList(Math.max(0, folds.size() - MAX_VALIDATION_SAMPLES), folds.size());
        var predictor = new DurationPredictor();
        double bestLambda = DEFAULT_LAMBDA, bestError = Double.POSITIVE_INFINITY;
        for (double lambda : CANDIDATES) {
            double error = 0;
            for (var fold : folds) {
                var training = fold.training();
                var prediction = predictor.predict(fold.target(), Stats.of(training), training.size(),
                        predictor.fit(training, lambda), FALLBACK_SIGMA);
                error += Math.abs(prediction.minutes() - (double) fold.target().actualMinutes())
                        / fold.target().actualMinutes();
            }
            if (error < bestError - 1e-12) { bestError = error; bestLambda = lambda; }
        }
        return new Selection(bestLambda, true, folds.size());
    }
}
