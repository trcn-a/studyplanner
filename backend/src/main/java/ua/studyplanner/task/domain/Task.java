package ua.studyplanner.task.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {
    @Id private UUID id;

    @Column(nullable = false)
    private UUID subjectId;

    private UUID parentTaskId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority;

    @Transient
    private java.time.LocalDateTime scheduledStart;
    @Transient
    private java.time.LocalDateTime scheduledEnd;
    public java.time.LocalDateTime getScheduledStart() { return scheduledStart; }
    public java.time.LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void schedule(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        scheduledStart = start; scheduledEnd = end;
    }
    private LocalDate dueDate;
    private Integer estimatedMinutes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanningTimeSource planningTimeSource = PlanningTimeSource.PREDICTION;
    public PlanningTimeSource getPlanningTimeSource() { return planningTimeSource; }
    public void setPlanningTimeSource(PlanningTimeSource source) {
        planningTimeSource = source == null ? PlanningTimeSource.PREDICTION : source;
    }
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private TaskType taskType;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private TaskType initialTaskType;
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty initialDifficulty;
    public TaskType getTaskType() { return taskType; }
    public Difficulty getDifficulty() { return difficulty; }
    public TaskType getInitialTaskType() { return initialTaskType; }
    public Difficulty getInitialDifficulty() { return initialDifficulty; }
    public void setCharacteristics(TaskType taskType, Difficulty difficulty) {
        this.taskType = taskType;
        this.difficulty = difficulty;
    }
    private boolean predictionSnapshot;
    private Integer initialEstimatedMinutes;
    @Enumerated(EnumType.STRING)
    private Priority initialPriority;
    public boolean hasPredictionSnapshot() { return predictionSnapshot; }
    public Integer getInitialEstimatedMinutes() { return initialEstimatedMinutes; }
    public Priority getInitialPriority() { return initialPriority; }

    private Instant createdAt;
    private Instant updatedAt;
    // Null for legacy records: their historical data availability cannot be reconstructed.
    private Instant predictionDataAt;
    public Instant getPredictionDataAt() { return predictionDataAt; }
    public void predictionDataChanged() { predictionDataAt = Instant.now(); }

    protected Task() {}

    public Task(
            UUID subjectId,
            UUID parentTaskId,
            String title,
            String description,
            TaskStatus status,
            Priority priority,
            LocalDate dueDate,
            Integer estimatedMinutes) {
        this(subjectId, parentTaskId, title, description, status, priority, dueDate, estimatedMinutes, null, null);
    }

    public Task(UUID subjectId, UUID parentTaskId, String title, String description,
            TaskStatus status, Priority priority, LocalDate dueDate, Integer estimatedMinutes,
            TaskType taskType, Difficulty difficulty) {
        this.taskType = taskType;
        this.difficulty = difficulty;
        this.initialTaskType = taskType;
        this.initialDifficulty = difficulty;
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.predictionSnapshot = true;
        this.initialEstimatedMinutes = estimatedMinutes;
        this.initialPriority = priority;
        this.subjectId = subjectId;
        this.parentTaskId = parentTaskId;
        this.title = title.strip();
        this.description = description.strip();
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.estimatedMinutes = estimatedMinutes;
        this.updatedAt = this.createdAt;
        this.predictionDataAt = this.createdAt;
    }

    public void update(
            String title,
            String description,
            TaskStatus status,
            Priority priority,
            LocalDate dueDate,
            Integer estimatedMinutes) {
        this.title = title.strip();
        this.description = description.strip();
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.estimatedMinutes = estimatedMinutes;
        this.updatedAt = Instant.now();
        predictionDataChanged();
    }

    public void changeStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        predictionDataChanged();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public UUID getParentTaskId() {
        return parentTaskId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

