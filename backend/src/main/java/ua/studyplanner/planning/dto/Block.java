package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record Block(UUID id, UUID taskId, String title, LocalDateTime start, LocalDateTime end, String source) {}
