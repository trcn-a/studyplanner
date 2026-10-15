package ua.studyplanner.task.dto;

import jakarta.validation.constraints.*;

import ua.studyplanner.task.domain.*;

import java.time.*;

public record TaskInput(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Size(max = 5000) String description,
        @NotNull TaskStatus status,
        @NotNull Priority priority,
        LocalDate dueDate,
        @Positive Integer estimatedMinutes,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        PlanningTimeSource planningTimeSource,
        TaskType taskType,
        Difficulty difficulty) {
    public TaskInput(String title, String description, TaskStatus status, Priority priority,
            LocalDate dueDate, Integer estimatedMinutes, LocalDateTime scheduledStart, LocalDateTime scheduledEnd,
            PlanningTimeSource planningTimeSource) {
        this(title, description, status, priority, dueDate, estimatedMinutes, scheduledStart, scheduledEnd,
                planningTimeSource, null, null);
    }
    public TaskInput(String title, String description, TaskStatus status, Priority priority,
            LocalDate dueDate, Integer estimatedMinutes, LocalDateTime scheduledStart, LocalDateTime scheduledEnd) {
        this(title, description, status, priority, dueDate, estimatedMinutes, scheduledStart, scheduledEnd, PlanningTimeSource.PREDICTION);
    }
    @AssertTrue(message = "Для планування за власною оцінкою вкажіть тривалість")
    public boolean isPlanningTimeValid() {
        return planningTimeSource != PlanningTimeSource.USER_ESTIMATE || estimatedMinutes != null && estimatedMinutes > 0;
    }
    @AssertTrue(message = "Вкажіть початок і завершення в межах одного дня; завершення має бути пізніше початку")
    public boolean isScheduleValid() {
        return scheduledStart == null && scheduledEnd == null ||
            scheduledStart != null && scheduledEnd != null && scheduledEnd.isAfter(scheduledStart)
            && scheduledStart.toLocalDate().equals(scheduledEnd.toLocalDate());
    }
}
