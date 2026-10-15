package ua.studyplanner.prediction.domain;

import org.junit.jupiter.api.Test;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.TaskSample;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RegularizationSelectorTest {
    private List<TaskSample> history(int count) {
        var result = new ArrayList<TaskSample>();
        UUID subject = UUID.randomUUID();
        for (int i = 0; i < count; i++) {
            Instant created = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i * 86400L);
            result.add(new TaskSample(UUID.randomUUID(), subject, false, 0, Priority.MEDIUM,
                    TaskStatus.DONE, 30, (i + 3) * 10, created, created.plusSeconds(3600)));
        }
        return result;
    }
    @Test void defaultsUntilThreeRegressionValidationExamplesExist() {
        var selected = new RegularizationSelector().select(history(10));
        assertFalse(selected.tuned());
        assertEquals(3.0, selected.lambda());
        assertEquals(2, selected.samples());
    }
    @Test void selectsWeakerRegularizationForCleanLearnableRelation() {
        var examples = history(16);
        var learnable = new ArrayList<TaskSample>();
        for (int i=0; i<examples.size(); i++) {
            var s=examples.get(i);
            int estimate=(i+3)*10;
            int actual=(int)Math.round(30*Math.pow(estimate/30.0,1.5));
            learnable.add(new TaskSample(s.id(),s.subjectId(),false,0,s.priority(),s.status(),estimate,actual,s.createdAt(),s.availableAt()));
        }
        var selected = new RegularizationSelector().select(learnable);
        assertTrue(selected.tuned());
        assertEquals(8, selected.samples());
        assertEquals(0.1, selected.lambda());
    }
    @Test void unknownDatesNeverInventEvidenceForTuning() {
        var undated = history(20).stream().map(s -> new TaskSample(s.id(), s.subjectId(), s.subtask(),
                0, s.priority(), s.status(), s.estimatedMinutes(), s.actualMinutes())).toList();
        var selected = new RegularizationSelector().select(undated);
        assertFalse(selected.tuned());
        assertEquals(0, selected.samples());
    }
    @Test void limitsTuningToTwentyMostRecentValidationExamples() {
        assertEquals(20, new RegularizationSelector().select(history(40)).samples());
    }
}
