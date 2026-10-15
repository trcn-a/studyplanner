package ua.studyplanner.prediction.service;

import org.springframework.stereotype.Service;
import java.util.List;
import ua.studyplanner.prediction.domain.DurationPredictor;
import ua.studyplanner.prediction.domain.PredictionEvaluator;
import ua.studyplanner.prediction.domain.RegularizationSelector;
import ua.studyplanner.prediction.dto.*;
import ua.studyplanner.task.domain.TaskStatus;
import ua.studyplanner.task.dto.TaskSample;
import ua.studyplanner.task.service.TaskService;
import static ua.studyplanner.prediction.domain.DurationPredictor.*;

@Service
public class DurationPredictionService {
    private final TaskService tasks;
    private final DurationPredictor predictor = new DurationPredictor();
    private final PredictionEvaluator evaluator = new PredictionEvaluator();

    public DurationPredictionService(TaskService tasks) { this.tasks = tasks; }

    public PredictionReport report() { return report(true); }
    public List<PredictionView> predictions() { return report(false).predictions(); }

    public PredictionView preview(PredictionPreviewInput input) {
        TaskSample target = tasks.previewSample(input.subjectId(), input.taskId(), input.parentId(), input.input());
        var history = tasks.samples().stream()
                .filter(s -> !s.id().equals(target.id()) && s.status() == TaskStatus.DONE && s.actualMinutes() > 0).toList();
        if (history.isEmpty()) return target.estimatedMinutes() == null ? null : predictor.userEstimate(target);
        var selection = new RegularizationSelector().select(history);
        return predictor.predictAll(history, List.of(target), FALLBACK_SIGMA, selection.lambda()).getFirst();
    }

    private PredictionReport report(boolean evaluate) {
        List<TaskSample> all = tasks.samples();
        List<TaskSample> history = all.stream()
                .filter(s -> s.status() == TaskStatus.DONE && s.actualMinutes() > 0).toList();
        List<TaskSample> pending = all.stream().filter(s -> s.status() != TaskStatus.DONE).toList();
        var selection = new RegularizationSelector().select(history);
        ModelInfo info = new ModelInfo(history.size() >= MIN_SAMPLES_FOR_MODEL, history.size(),
                MIN_SAMPLES_FOR_MODEL, null, null, null, 0,
                selection.lambda(), selection.tuned(), selection.samples(), 0);
        if (history.isEmpty()) {
            return new PredictionReport(info, pending.stream().filter(s -> s.estimatedMinutes() != null)
                    .map(predictor::userEstimate).toList());
        }
        double sigma = FALLBACK_SIGMA;
        if (evaluate) {
            var validation = evaluator.evaluate(history);
            sigma = validation.sigma();
            info = PredictionEvaluator.withErrors(info, validation);
        }
        return new PredictionReport(info, predictor.predictAll(history, pending, sigma, selection.lambda()));
    }
}
