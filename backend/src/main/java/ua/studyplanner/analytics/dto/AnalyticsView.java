package ua.studyplanner.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Агреговані показники навчальної діяльності за вибраний період. */
public record AnalyticsView(
        LocalDate from,
        LocalDate to,
        Summary summary,
        Deadlines deadlines,
        PlanExecution plan,
        List<Day> days,
        List<SubjectStat> subjects,
        List<Weekday> weekdays,
        List<TaskProgress> progress) {

    /** Загальні показники: виконані завдання, витрачений час, регулярність. */
    public record Summary(
            int completedTasks,
            int openTasks,
            int actualMinutes,
            int activeDays,
            int periodDays,
            int averageMinutesPerActiveDay,
            int sessions,
            int averageSessionMinutes,
            int currentStreak,
            int longestStreak) {}

    /** Дотримання дедлайнів завдань, термін яких припадає на період. */
    public record Deadlines(
            int due, int onTime, int late, int overdueOpen, int upcoming, int unknownCompletion, Integer onTimePercent) {}

    /** План і факт: запланований обсяг, виконана його частина, пропущене та робота понад план. */
    public record PlanExecution(
            int plannedMinutes,
            int fulfilledMinutes,
            int missedMinutes,
            int extraMinutes,
            int futureMinutes,
            Integer completionPercent) {}

    public record Day(LocalDate date, int actualMinutes, int plannedMinutes, int fulfilledMinutes, int completedTasks) {}

    public record SubjectStat(
            UUID id, String name, String color, int actualMinutes, int completedTasks, int doneTasks, int totalTasks) {}

    /** dayOfWeek: 1 — понеділок, 7 — неділя. */
    public record Weekday(int dayOfWeek, int actualMinutes) {}

    public record TaskProgress(
            UUID id,
            String title,
            UUID subjectId,
            LocalDate dueDate,
            String status,
            int actualMinutes,
            Integer expectedMinutes,
            Integer percent) {}
}
