package ua.studyplanner.prediction.domain;

import org.junit.jupiter.api.Test;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.TaskSample;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TaskCharacteristicsTest {
    @Test void snapshotsUseInitialCharacteristicsAfterCompletion() {
        var task = new Task(UUID.randomUUID(), null, "Lab", "short", TaskStatus.TODO,
                Priority.LOW, null, 60, TaskType.LABORATORY, Difficulty.HIGH);
        task.setCharacteristics(TaskType.NOTES, Difficulty.LOW);
        var pending = TaskSample.of(task, 0, 0);
        assertEquals(TaskType.NOTES, pending.taskType());
        assertEquals(Difficulty.LOW, pending.difficulty());
        task.update("Lab", "a much longer edited description", TaskStatus.DONE, Priority.HIGH, null, 90);
        var completed = TaskSample.of(task, 0, 120);
        assertEquals(TaskType.LABORATORY, completed.taskType());
        assertEquals(Difficulty.HIGH, completed.difficulty());
        assertEquals(60, completed.estimatedMinutes());
        assertEquals(Priority.LOW, completed.priority());
    }

    @Test void changingDescriptionDoesNotChangePredictionFeatures() {
        var task = new Task(UUID.randomUUID(), null, "Lab", "", TaskStatus.TODO,
                Priority.MEDIUM, null, 60, TaskType.LABORATORY, Difficulty.HIGH);
        var stats = PredictionFeatures.Stats.of(List.of());
        var before = PredictionFeatures.features(TaskSample.of(task, 0, 0), stats, null, null);
        task.update("different title", "description".repeat(300), TaskStatus.TODO, Priority.MEDIUM, null, 60);
        var after = PredictionFeatures.features(TaskSample.of(task, 0, 0), stats, null, null);
        assertArrayEquals(before, after);
    }

    @Test void regressionLearnsTypeAndDifficultyIndependently() {
        UUID subject = UUID.randomUUID();
        var history = new ArrayList<TaskSample>();
        for (int i=0; i<8; i++) {
            for (TaskType type : List.of(TaskType.LABORATORY, TaskType.NOTES)) {
                for (Difficulty difficulty : List.of(Difficulty.LOW, Difficulty.HIGH)) {
                    int minutes = (type == TaskType.LABORATORY ? 45 : 15) * (difficulty == Difficulty.HIGH ? 3 : 1);
                    history.add(sample(subject, type, difficulty, minutes));
                }
            }
        }
        var predictor = new DurationPredictor();
        var model = predictor.fit(history);
        var stats = PredictionFeatures.Stats.of(history);
        int lowLab = predictor.predict(sample(subject, TaskType.LABORATORY, Difficulty.LOW, 0),stats,history.size(),model,.5).minutes();
        int highLab = predictor.predict(sample(subject, TaskType.LABORATORY, Difficulty.HIGH, 0),stats,history.size(),model,.5).minutes();
        int lowNotes = predictor.predict(sample(subject, TaskType.NOTES, Difficulty.LOW, 0),stats,history.size(),model,.5).minutes();
        assertTrue(highLab > lowLab * 2, "Difficulty must affect the learned prediction");
        assertTrue(lowLab > lowNotes * 2, "Task type must affect the learned prediction");
        assertTrue(predictor.predict(sample(subject,null,null,0),stats,history.size(),model,.5).minutes()>0);
    }

    @Test void laterClassificationDoesNotInventHistoricalCharacteristics() {
        var legacy = new Task(UUID.randomUUID(), null, "Old", "", TaskStatus.TODO, Priority.MEDIUM, null, 30);
        legacy.setCharacteristics(TaskType.LABORATORY, Difficulty.HIGH);
        legacy.changeStatus(TaskStatus.DONE);
        var sample = TaskSample.of(legacy, 0, 45);
        assertNull(sample.taskType());
        assertNull(sample.difficulty());
    }

    private TaskSample sample(UUID subject, TaskType type, Difficulty difficulty, int actual) {
        return new TaskSample(UUID.randomUUID(),subject,false,0,Priority.MEDIUM,
                actual > 0 ? TaskStatus.DONE : TaskStatus.TODO,60,actual,null,null,type,difficulty);
    }
}
