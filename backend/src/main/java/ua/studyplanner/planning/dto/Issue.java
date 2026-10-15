package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record Issue(UUID taskId, String title, String reason, int remainingMinutes) {}
