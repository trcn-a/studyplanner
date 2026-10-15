package ua.studyplanner.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import ua.studyplanner.analytics.domain.AnalyticsCalculator;
import ua.studyplanner.analytics.domain.AnalyticsCalculator.*;
import ua.studyplanner.analytics.dto.AnalyticsView;
import ua.studyplanner.session.domain.SessionStatus;
import ua.studyplanner.task.domain.TaskStatus;

class AnalyticsCalculatorTest {
    static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    final UUID subjectA = UUID.randomUUID(), subjectB = UUID.randomUUID();
    final UUID t1 = UUID.randomUUID(), t2 = UUID.randomUUID(), t3 = UUID.randomUUID(), t4 = UUID.randomUUID(), t5 = UUID.randomUUID();

    static Instant at(int day, int hour) { return LocalDateTime.of(2026, 10, day, hour, 0).atZone(KYIV).toInstant(); }

    SessionFact completed(UUID task, int day, int minutes, Integer planned) {
        return new SessionFact(task, at(day, 10), at(day, 10).plusSeconds(minutes * 60L), minutes, planned, SessionStatus.COMPLETED);
    }

    SessionFact plan(UUID task, int day, int hour, int minutes, SessionStatus status) {
        return new SessionFact(task, at(day, hour), at(day, hour).plusSeconds(minutes * 60L), null, null, status);
    }

    AnalyticsView report() {
        var tasks = List.of(
                new TaskFact(t1, subjectA, null, "Лаба 1", TaskStatus.DONE, LocalDate.of(2026, 10, 3), 60, 90),
                new TaskFact(t2, subjectA, null, "Лаба 2", TaskStatus.DONE, LocalDate.of(2026, 10, 4), 120, 60),
                new TaskFact(t3, subjectB, null, "Реферат", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 10, 5), 30, 60),
                new TaskFact(t4, subjectB, null, "Проєкт", TaskStatus.TODO, LocalDate.of(2026, 10, 9), 0, null),
                new TaskFact(t5, subjectB, null, "Давнє", TaskStatus.DONE, LocalDate.of(2026, 10, 2), 0, null));
        var sessions = List.of(
                completed(t1, 3, 60, 90),                         // 60 з 90 виконано
                completed(t2, 5, 120, 60),                        // план виконано, 60 хв понад план
                completed(t3, 6, 30, null),                       // робота поза планом
                plan(t3, 4, 12, 45, SessionStatus.SKIPPED),       // пропущений план відкритого завдання
                plan(t1, 4, 14, 30, SessionStatus.SKIPPED),       // план скасовано завершенням завдання
                plan(t3, 7, 16, 30, SessionStatus.PLANNED));      // майбутній план
        var completions = Map.of(t1, LocalDate.of(2026, 10, 3), t2, LocalDate.of(2026, 10, 5));
        return AnalyticsCalculator.compute(new Input(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7),
                LocalDate.of(2026, 10, 7), at(7, 9), KYIV,
                List.of(new SubjectFact(subjectA, "Бази даних", "#81583d"), new SubjectFact(subjectB, "Алгоритми", "#73754f")),
                tasks, sessions, completions));
    }

    @Test void summaryCountsCompletedTasksTimeAndRegularity() {
        var s = report().summary();
        assertEquals(2, s.completedTasks());
        assertEquals(210, s.actualMinutes());
        assertEquals(3, s.sessions());
        assertEquals(70, s.averageSessionMinutes());
        assertEquals(3, s.activeDays());
        assertEquals(7, s.periodDays());
        assertEquals(2, s.longestStreak());
        assertEquals(2, s.currentStreak(), "сьогодні ще можна продовжити серію");
        assertEquals(2, s.openTasks());
    }

    @Test void planExecutionComparesPlannedAndActualWork() {
        var p = report().plan();
        assertEquals(195, p.plannedMinutes());
        assertEquals(120, p.fulfilledMinutes());
        assertEquals(75, p.missedMinutes());
        assertEquals(90, p.extraMinutes());
        assertEquals(30, p.futureMinutes());
        assertEquals(62, p.completionPercent());
    }

    @Test void deadlinesSeparateOnTimeLateAndOverdue() {
        var d = report().deadlines();
        assertEquals(4, d.due());
        assertEquals(1, d.onTime());
        assertEquals(1, d.late());
        assertEquals(1, d.overdueOpen());
        assertEquals(1, d.unknownCompletion());
        assertEquals(33, d.onTimePercent());
    }

    @Test void groupsByDaysSubjectsAndWeekdays() {
        var r = report();
        assertEquals(7, r.days().size());
        assertEquals(60, r.days().get(2).actualMinutes());
        assertEquals(1, r.days().get(2).completedTasks());
        assertEquals("Бази даних", r.subjects().getFirst().name());
        assertEquals(180, r.subjects().getFirst().actualMinutes());
        assertEquals(2, r.subjects().getFirst().completedTasks());
        assertEquals(120, r.weekdays().get(0).actualMinutes(), "понеділок 5 жовтня");
        assertEquals(60, r.weekdays().get(5).actualMinutes(), "субота 3 жовтня");
    }

    @Test void taskProgressUsesExpectedDuration() {
        var progress = report().progress();
        assertEquals("Реферат", progress.getFirst().title());
        assertEquals(50, progress.getFirst().percent());
        assertNull(progress.get(1).percent());
    }

    @Test void emptyPeriodHasNoPercentagesAndZeroStreak() {
        var empty = AnalyticsCalculator.compute(new Input(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7),
                LocalDate.of(2026, 10, 7), at(7, 9), KYIV, List.of(), List.of(), List.of(), Map.of()));
        assertNull(empty.plan().completionPercent());
        assertNull(empty.deadlines().onTimePercent());
        assertEquals(0, empty.summary().currentStreak());
        assertEquals(0, empty.summary().actualMinutes());
    }
}
