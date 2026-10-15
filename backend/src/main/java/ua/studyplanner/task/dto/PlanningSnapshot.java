package ua.studyplanner.task.dto;
import java.util.List;
/** Read-only task boundary for scheduling; no managed entities leave this module. */
public record PlanningSnapshot(List<TaskView> tasks, List<TimeEntryView> timeEntries) {}
