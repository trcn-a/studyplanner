package ua.studyplanner.task.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/** Ручний запис витраченого часу; {@code date} за замовчуванням — сьогодні. */
public record ManualTimeInput(@NotNull @Min(1) @Max(1440) Integer minutes, LocalDate date) {}
