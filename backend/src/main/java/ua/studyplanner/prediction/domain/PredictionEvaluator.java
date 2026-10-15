package ua.studyplanner.prediction.domain;

import java.util.*;
import ua.studyplanner.task.dto.TaskSample;
import ua.studyplanner.prediction.dto.*;
import static ua.studyplanner.prediction.domain.PredictionFeatures.*;
import static ua.studyplanner.prediction.domain.DurationPredictor.*;

/** Walk-forward evaluation at task creation time, including nested lambda selection. */
public final class PredictionEvaluator {
    private final DurationPredictor predictor = new DurationPredictor();
    private final RegularizationSelector selector = new RegularizationSelector();
    public record Fold(TaskSample target, List<TaskSample> training) {}
    public record Observation(TaskSample target, int predicted, double median, PredictionMethod method) {}
    public record Evaluation(List<Observation> observations) {
        public double sigma() {
            if (observations.size() < 3) return FALLBACK_SIGMA;
            double squares = 0;
            for (var o : observations) {
                double r = Math.log(o.target().actualMinutes()) - Math.log(o.predicted());
                squares += r * r;
            }
            return Math.min(1.0, Math.max(0.15, Math.sqrt(squares / observations.size())));
        }
    }

    public static List<Fold> folds(List<TaskSample> history) {
        var dated = history.stream().filter(s -> s.createdAt() != null && s.availableAt() != null
                && !s.availableAt().isBefore(s.createdAt())).sorted(Comparator.comparing(TaskSample::createdAt)
                .thenComparing(TaskSample::id)).toList();
        var result = new ArrayList<Fold>();
        for (var target : dated) {
            var training = dated.stream().filter(s -> !s.id().equals(target.id())
                    && s.availableAt().isBefore(target.createdAt())).toList();
            // Median and personalized baselines require at least one earlier observation.
            if (!training.isEmpty()) result.add(new Fold(target, training));
        }
        return result;
    }

    public Evaluation evaluate(List<TaskSample> history) {
        var observations = new ArrayList<Observation>();
        for (var fold : folds(history)) {
            var training = fold.training();
            var stats = Stats.of(training);
            // Outer target is never available to parameter selection.
            double lambda = selector.select(training).lambda();
            var model = training.size() >= MIN_SAMPLES_FOR_MODEL ? predictor.fit(training, lambda) : null;
            var prediction = predictor.predict(fold.target(), stats, training.size(), model, FALLBACK_SIGMA);
            observations.add(new Observation(fold.target(), prediction.minutes(),
                    stats.median(fold.target().subjectId()), prediction.method()));
        }
        return new Evaluation(List.copyOf(observations));
    }

    public static ModelInfo withErrors(ModelInfo base, Evaluation evaluation) {
        var observations = evaluation.observations();
        boolean compareEstimates = observations.stream().filter(o -> o.target().estimatedMinutes() != null).count() >= 3;
        double model = 0, baseline = 0, median = 0;
        int evaluated = 0, regression = 0;
        for (var o : observations) {
            var s = o.target();
            if (compareEstimates && s.estimatedMinutes() == null) continue;
            double actual = s.actualMinutes();
            model += Math.abs(o.predicted() - actual) / actual;
            median += Math.abs(o.median() - actual) / actual;
            if (compareEstimates) baseline += Math.abs(s.estimatedMinutes() - actual) / actual;
            if (o.method() == PredictionMethod.REGRESSION) regression++;
            evaluated++;
        }
        // Do not display a quality percentage based on one or two observations.
        boolean enough = evaluated >= 3;
        return new ModelInfo(base.trained(), base.samples(), base.minSamples(),
                enough ? percent(model / evaluated) : null,
                enough && compareEstimates ? percent(baseline / evaluated) : null,
                enough ? percent(median / evaluated) : null, evaluated,
                base.regularization(), base.regularizationTuned(), base.tuningSamples(), regression);
    }

    private static double percent(double fraction) { return Math.round(fraction * 1000) / 10.0; }
}
