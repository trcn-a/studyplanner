package ua.studyplanner.planning.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record MoveSessionInput(@NotNull LocalDateTime start) {}
