package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record Settings(@NotBlank String zoneId, boolean autoReplan, @Min(1) @Max(90) int horizonDays,
        @Min(15) @Max(240) int sessionMinutes, @NotNull @Size(max=100) List<@NotNull @Valid Availability> availability,
        boolean habitAware, @Min(0) @Max(120) int breakMinutes) {
    public Settings(String zoneId, boolean autoReplan, int horizonDays, int sessionMinutes, List<Availability> availability, boolean habitAware) {
        this(zoneId, autoReplan, horizonDays, sessionMinutes, availability, habitAware, 0);
    }
    public Settings(String zoneId, boolean autoReplan, int horizonDays, int sessionMinutes, List<Availability> availability) {
        this(zoneId, autoReplan, horizonDays, sessionMinutes, availability, true);
    }
}
