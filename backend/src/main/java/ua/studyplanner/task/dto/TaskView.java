package ua.studyplanner.task.dto;

import jakarta.validation.constraints.*;

import ua.studyplanner.task.domain.*;

import java.time.*;
import java.util.UUID;

public record TaskView(
        UUID id,
        UUID subjectId,
        UUID parentTaskId,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate,
        Integer estimatedMinutes,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        int actualMinutes,
        Instant timerStartedAt,
        Instant createdAt,
        Instant updatedAt,
        PlanningTimeSource planningTimeSource, TaskType taskType, Difficulty difficulty) {
    public Integer planningMinutes(Integer prediction) {
        return planningTimeSource == PlanningTimeSource.USER_ESTIMATE ? estimatedMinutes
                : prediction != null ? prediction : estimatedMinutes;
    }
    public static TaskView of(Task t, TimeSummary time) {
        return new TaskView(
                t.getId(),
                t.getSubjectId(),
                t.getParentTaskId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getDueDate(),
                t.getEstimatedMinutes(),
                t.getScheduledStart(),
                t.getScheduledEnd(),
                time.minutes(),
                time.runningSince(),
                t.getCreatedAt(),
                t.getUpdatedAt(), t.getPlanningTimeSource(), t.getTaskType(), t.getDifficulty());
    }
}
