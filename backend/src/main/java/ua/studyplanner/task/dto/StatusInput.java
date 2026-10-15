package ua.studyplanner.task.dto;

import jakarta.validation.constraints.*;

import ua.studyplanner.task.domain.*;

import java.time.*;

public record StatusInput(@NotNull TaskStatus status) {}
