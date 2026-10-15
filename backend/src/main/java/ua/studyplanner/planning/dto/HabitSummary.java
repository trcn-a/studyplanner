package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record HabitSummary(int timerSessions, int activeDays, int subjectProfiles, boolean ready) {}
