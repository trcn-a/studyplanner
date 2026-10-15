package ua.studyplanner.prediction.domain;

import org.junit.jupiter.api.Test;
import ua.studyplanner.prediction.dto.ModelInfo;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.TaskSample;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PredictionEvaluatorTest {
    private final UUID subject = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private TaskSample sample(int day, Integer estimate, int actual) {
        Instant created = start.plusSeconds(day * 86400L);
        return new TaskSample(UUID.randomUUID(), subject, false, 0,
                Priority.MEDIUM, TaskStatus.DONE, estimate, actual, created, created.plusSeconds(3600));
    }
    private ModelInfo metrics(List<TaskSample> history) {
        var base = new ModelInfo(history.size() >= 8, history.size(), 8, null, null, null, 0, 3, false, 0, 0);
        return PredictionEvaluator.withErrors(base, new PredictionEvaluator().evaluate(history));
    }
    @Test void excludesFutureOverlappingAndUnknownObservations() {
        var first = sample(0, 30, 60);
        var second = sample(1, 30, 60);
        var overlapping = new TaskSample(UUID.randomUUID(), subject, false, 0, Priority.MEDIUM,
                TaskStatus.DONE, 30, 9000, start.minusSeconds(86400), start.plusSeconds(2 * 86400));
        var unknown = new TaskSample(UUID.randomUUID(), subject, false, 0, Priority.MEDIUM, TaskStatus.DONE, 30, 9000);
        var folds = PredictionEvaluator.folds(List.of(second, overlapping, sample(3,30,9000), unknown, first));
        var fold = folds.stream().filter(f -> f.target().id().equals(second.id())).findFirst().orElseThrow();
        assertEquals(List.of(first), fold.training());
    }
    @Test void laterAnswersCannotChangeEarlierPredictionIncludingParameterSelection() {
        var history = new ArrayList<TaskSample>();
        for (int i = 0; i < 15; i++) history.add(sample(i, 30, 30 + 10 * i));
        var evaluator = new PredictionEvaluator();
        var before = evaluator.evaluate(history).observations();
        var target = history.get(12);
        var changed = new TaskSample(target.id(), target.subjectId(), false, 0,
                target.priority(), target.status(), target.estimatedMinutes(), 9000, target.createdAt(), target.availableAt());
        history.set(12, changed);
        history.add(sample(20, 30, 100000));
        var after = evaluator.evaluate(history).observations();
        for (int i = 0; i < 12; i++) {
            assertEquals(before.get(i).predicted(), after.get(i).predicted());
            assertEquals(before.get(i).median(), after.get(i).median());
        }
    }
    @Test void comparesAllMethodsOnTheSameEstimatedTasks() {
        var result = metrics(List.of(sample(0, 30, 60), sample(1, 30, 60), sample(2, null, 60),
                sample(3, 30, 60), sample(4, 30, 60)));
        assertEquals(3, result.evaluationSamples());
        assertEquals(0.0, result.errorPercent());
        assertEquals(50.0, result.baselineErrorPercent());
        assertEquals(0.0, result.medianBaselineErrorPercent());
        assertEquals(0, result.regressionEvaluationSamples());
    }
    @Test void suppressesQualityWithInsufficientDatedHistory() {
        var result = metrics(List.of(sample(0, 30, 60), sample(1, 30, 60)));
        assertEquals(1, result.evaluationSamples());
        assertNull(result.errorPercent());
        assertNull(result.medianBaselineErrorPercent());
    }
    @Test void usesGlobalMedianForNewSubjectAndOmitsUnavailableUserBaseline() {
        var target = sample(3, null, 60);
        target = new TaskSample(target.id(), UUID.randomUUID(), false, 0, Priority.MEDIUM,
                TaskStatus.DONE, null, 60, target.createdAt(), target.availableAt());
        var result = metrics(List.of(sample(0, null, 60), sample(1, null, 60), sample(2, null, 60), target));
        assertEquals(3, result.evaluationSamples());
        assertEquals(0.0, result.errorPercent());
        assertEquals(0.0, result.medianBaselineErrorPercent());
        assertNull(result.baselineErrorPercent());
    }
    @Test void equalAvailabilityAndCreationDoesNotAssumeOrdering() {
        var first = sample(0, 30, 60);
        var second = sample(1, 30, 60);
        first = new TaskSample(first.id(), subject, false, 0, Priority.MEDIUM,
                TaskStatus.DONE, 30, 60, first.createdAt(), second.createdAt());
        assertTrue(PredictionEvaluator.folds(List.of(first, second)).isEmpty());
    }
}
