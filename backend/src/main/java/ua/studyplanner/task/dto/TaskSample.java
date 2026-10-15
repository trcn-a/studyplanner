package ua.studyplanner.task.dto;

import ua.studyplanner.task.domain.*;
import java.util.UUID;
import java.time.Instant;

/** Characteristics known before execution, with actual time as the training target. */
public record TaskSample(
        UUID id, UUID subjectId, boolean subtask, int subtaskCount,
        Priority priority, TaskStatus status, Integer estimatedMinutes, int actualMinutes,
        Instant createdAt, Instant availableAt, TaskType taskType, Difficulty difficulty) {
    public TaskSample(UUID id, UUID subjectId, boolean subtask, int subtaskCount,
            Priority priority, TaskStatus status, Integer estimatedMinutes, int actualMinutes) {
        this(id, subjectId, subtask, subtaskCount, priority, status, estimatedMinutes, actualMinutes,
                null, null, null, null);
    }
    public TaskSample(UUID id, UUID subjectId, boolean subtask, int subtaskCount,
            Priority priority, TaskStatus status, Integer estimatedMinutes, int actualMinutes,
            Instant createdAt, Instant availableAt) {
        this(id, subjectId, subtask, subtaskCount, priority, status, estimatedMinutes, actualMinutes,
                createdAt, availableAt, null, null);
    }
    public static TaskSample of(Task t, int subtaskCount, int actualMinutes) {
        boolean completed = t.getStatus() == TaskStatus.DONE;
        return new TaskSample(t.getId(), t.getSubjectId(), t.getParentTaskId() != null,
                0, // Mutable subtask count is excluded from prediction features.
                completed ? t.getInitialPriority() : t.getPriority(), t.getStatus(),
                completed ? t.getInitialEstimatedMinutes() : t.getEstimatedMinutes(),
                actualMinutes, t.getCreatedAt(), t.getPredictionDataAt(),
                completed ? t.getInitialTaskType() : t.getTaskType(),
                completed ? t.getInitialDifficulty() : t.getDifficulty());
    }
}
