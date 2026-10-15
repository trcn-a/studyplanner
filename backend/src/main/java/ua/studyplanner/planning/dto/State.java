package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record State(Settings settings, List<Block> sessions, List<Block> externalEvents, List<Issue> issues, HabitSummary habits) {}
