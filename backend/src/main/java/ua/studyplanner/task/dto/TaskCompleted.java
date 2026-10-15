package ua.studyplanner.task.dto;
import java.time.Instant;
import java.util.UUID;

/** Published synchronously inside the task transaction. */
public record TaskCompleted(UUID taskId, boolean subtask, Instant completedAt) {}
