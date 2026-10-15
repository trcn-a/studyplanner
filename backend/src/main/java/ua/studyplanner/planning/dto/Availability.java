package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record Availability(@Min(1) @Max(7) int dayOfWeek, @NotNull LocalTime start, @NotNull LocalTime end) {}
