package ua.studyplanner.prediction.service;
import org.junit.jupiter.api.Test;
import ua.studyplanner.task.dto.TaskSample;
import ua.studyplanner.task.domain.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DurationPredictionServiceTest {
    @Test void initialFeaturesSurviveEdits() {
        var task = new Task(UUID.randomUUID(), null, "task", "abc", TaskStatus.TODO, Priority.LOW, null, 30);
        task.update("task", "changed description", TaskStatus.DONE, Priority.HIGH, null, 900);
        var sample = TaskSample.of(task, 5, 60);
        assertEquals(30, sample.estimatedMinutes());
        assertEquals(Priority.LOW, sample.priority());
        assertEquals(0, sample.subtaskCount());
    }

    @Test void noHistoryReturnsOwnEstimateAndOmitsUnknownDuration() {
        var tasks=org.mockito.Mockito.mock(ua.studyplanner.task.service.TaskService.class);
        UUID subject=UUID.randomUUID(),known=UUID.randomUUID();
        org.mockito.Mockito.when(tasks.samples()).thenReturn(List.of(
            new TaskSample(known,subject,false,0,Priority.MEDIUM,TaskStatus.TODO,45,0),
            new TaskSample(UUID.randomUUID(),subject,false,0,Priority.MEDIUM,TaskStatus.TODO,null,0)));
        var service=new DurationPredictionService(tasks);
        var result=service.report();
        assertEquals(1,result.predictions().size());
        assertEquals(45,result.predictions().getFirst().minutes());
        assertEquals(ua.studyplanner.prediction.domain.PredictionMethod.USER_ESTIMATE,result.predictions().getFirst().method());
        assertEquals(result.predictions(),service.predictions());
        assertNull(result.model().medianBaselineErrorPercent());
        assertEquals(0,result.model().evaluationSamples());
    }
    @Test void reportIncludesMedianComparisonAtTrainingThreshold() {
        var tasks=org.mockito.Mockito.mock(ua.studyplanner.task.service.TaskService.class);
        UUID subject=UUID.randomUUID();
        var history=new ArrayList<TaskSample>();
        for(int i=0;i<8;i++) {
            var created=java.time.Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i*86400L);
            history.add(new TaskSample(UUID.randomUUID(),subject,false,0,
                Priority.MEDIUM,TaskStatus.DONE,30,60,created,created.plusSeconds(3600)));
        }
        org.mockito.Mockito.when(tasks.samples()).thenReturn(history);
        var result=new DurationPredictionService(tasks).report().model();
        assertTrue(result.trained());
        assertEquals(7,result.evaluationSamples());
        assertEquals(0.0,result.medianBaselineErrorPercent());
        assertEquals(50.0,result.baselineErrorPercent());
        assertEquals(0.0,result.errorPercent());
        assertFalse(result.regularizationTuned());
        assertEquals(0,result.regressionEvaluationSamples());
    }
    @Test void historyWithoutEstimateRatiosDoesNotReplaceOwnEstimateWithMedian() {
        var tasks=org.mockito.Mockito.mock(ua.studyplanner.task.service.TaskService.class);
        UUID subject=UUID.randomUUID();
        org.mockito.Mockito.when(tasks.samples()).thenReturn(List.of(
            new TaskSample(UUID.randomUUID(),subject,false,0,Priority.MEDIUM,TaskStatus.DONE,null,240),
            new TaskSample(UUID.randomUUID(),subject,false,0,Priority.MEDIUM,TaskStatus.TODO,45,0)));
        assertEquals(45,new DurationPredictionService(tasks).predictions().getFirst().minutes());
    }
}
