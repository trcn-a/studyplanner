package ua.studyplanner.subject.dto;

import jakarta.validation.constraints.*;

import ua.studyplanner.subject.domain.*;

import java.time.*;
import java.util.UUID;

public record SubjectView(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        Instant updatedAt) {
    public static SubjectView of(Subject s) {
        return new SubjectView(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.getColor(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
