package ua.studyplanner.subject.dto;

import jakarta.validation.constraints.*;

import ua.studyplanner.subject.domain.*;

import java.time.*;

public record SubjectInput(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Size(max = 2000) String description,
        @NotNull @Pattern(regexp = "#[0-9a-fA-F]{6}") String color) {}
