package ua.studyplanner.analytics.domain;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import ua.studyplanner.analytics.dto.AnalyticsView;
import ua.studyplanner.analytics.dto.AnalyticsView.*;
import ua.studyplanner.session.domain.SessionStatus;
import ua.studyplanner.task.domain.TaskStatus;

/**
 * Чисті обчислення аналітики за списками фактів. Клас не звертається до бази даних, тому його
 * поведінка перевіряється модульними тестами без Spring.
 */
public final class AnalyticsCalculator {
    private AnalyticsCalculator() {}

    public record SubjectFact(UUID id, String name, String color) {}

    /** expectedMinutes — прогноз тривалості, а за його відсутності оцінка користувача. */
    public record TaskFact(
            UUID id, UUID subjectId, UUID parentId, String title, TaskStatus status, LocalDate dueDate,
            int actualMinutes, Integer expectedMinutes) {}

    public record SessionFact(
            UUID taskId, Instant start, Instant end, Integer minutes, Integer plannedMinutes, SessionStatus status) {}

    public record Input(
            LocalDate from, LocalDate to, LocalDate today, Instant now, ZoneId zone,
            List<SubjectFact> subjects, List<TaskFact> tasks, List<SessionFact> sessions,
            Map<UUID, LocalDate> completions) {}

    private static final class Acc {
        int actual, planned, fulfilled, completed;
        boolean active() { return actual > 0 || completed > 0; }
    }

    public static AnalyticsView compute(Input in) {
        Map<UUID, TaskFact> tasks = in.tasks().stream().collect(Collectors.toMap(TaskFact::id, t -> t, (a, b) -> a));
        Set<UUID> parents = in.tasks().stream().map(TaskFact::parentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<LocalDate, Acc> days = new LinkedHashMap<>();
        for (LocalDate d = in.from(); !d.isAfter(in.to()); d = d.plusDays(1)) days.put(d, new Acc());
        Map<UUID, int[]> bySubject = new HashMap<>(); // [фактичні хвилини, завершені завдання]

        int planned = 0, fulfilled = 0, extra = 0, future = 0, sessionCount = 0, actualTotal = 0;
        for (SessionFact s : in.sessions()) {
            TaskFact task = tasks.get(s.taskId());
            Acc acc = days.get(s.start().atZone(in.zone()).toLocalDate());
            if (task == null || acc == null) continue;
            Integer plan = plannedOf(s);
            int[] subject = bySubject.computeIfAbsent(task.subjectId(), id -> new int[2]);
            switch (s.status()) {
                case COMPLETED -> {
                    int minutes = s.minutes() == null ? 0 : s.minutes();
                    acc.actual += minutes; actualTotal += minutes; sessionCount++; subject[0] += minutes;
                    if (plan == null) {
                        extra += minutes;
                    } else {
                        int done = Math.min(minutes, plan);
                        planned += plan; fulfilled += done; extra += minutes - done;
                        acc.planned += plan; acc.fulfilled += done;
                    }
                }
                // Заняття, скасоване завершенням завдання, не є невиконаним планом.
                case SKIPPED -> { if (plan != null && task.status() != TaskStatus.DONE) { planned += plan; acc.planned += plan; } }
                case PLANNED -> {
                    if (plan != null) {
                        acc.planned += plan;
                        if (s.end() != null && s.end().isBefore(in.now())) planned += plan; else future += plan;
                    }
                }
                default -> { } // RUNNING: час ще не зафіксовано
            }
        }

        for (Map.Entry<UUID, LocalDate> e : in.completions().entrySet()) {
            TaskFact task = tasks.get(e.getKey());
            Acc acc = days.get(e.getValue());
            if (task == null || acc == null) continue;
            acc.completed++;
            bySubject.computeIfAbsent(task.subjectId(), id -> new int[2])[1]++;
        }

        int completedTasks = days.values().stream().mapToInt(a -> a.completed).sum();
        int activeDays = (int) days.values().stream().filter(Acc::active).count();
        int periodDays = days.size();
        List<Boolean> active = days.values().stream().map(Acc::active).toList();

        List<TaskFact> leaves = in.tasks().stream().filter(t -> !parents.contains(t.id())).toList();
        int open = (int) leaves.stream().filter(t -> t.status() != TaskStatus.DONE).count();
        Summary summary = new Summary(completedTasks, open, actualTotal, activeDays, periodDays,
                activeDays == 0 ? 0 : Math.round((float) actualTotal / activeDays),
                sessionCount, sessionCount == 0 ? 0 : Math.round((float) actualTotal / sessionCount),
                currentStreak(in, days, active), longestStreak(active));

        PlanExecution plan = new PlanExecution(planned, fulfilled, planned - fulfilled, extra, future,
                percent(fulfilled, planned));

        List<Day> daySeries = days.entrySet().stream()
                .map(e -> new Day(e.getKey(), e.getValue().actual, e.getValue().planned, e.getValue().fulfilled, e.getValue().completed))
                .toList();

        List<SubjectStat> subjects = in.subjects().stream().map(s -> {
            int[] values = bySubject.getOrDefault(s.id(), new int[2]);
            List<TaskFact> own = in.tasks().stream().filter(t -> s.id().equals(t.subjectId())).toList();
            return new SubjectStat(s.id(), s.name(), s.color(), values[0], values[1],
                    (int) own.stream().filter(t -> t.status() == TaskStatus.DONE).count(), own.size());
        }).sorted(Comparator.comparingInt(SubjectStat::actualMinutes).reversed().thenComparing(SubjectStat::name)).toList();

        int[] weekdayMinutes = new int[8];
        days.forEach((date, acc) -> weekdayMinutes[date.getDayOfWeek().getValue()] += acc.actual);
        List<Weekday> weekdays = new ArrayList<>();
        for (int d = 1; d <= 7; d++) weekdays.add(new Weekday(d, weekdayMinutes[d]));

        return new AnalyticsView(in.from(), in.to(), summary, deadlines(in, tasks.values()), plan, daySeries,
                subjects, weekdays, progress(leaves));
    }

    private static Integer plannedOf(SessionFact s) {
        if (s.plannedMinutes() != null) return s.plannedMinutes();
        boolean plan = s.status() == SessionStatus.PLANNED || s.status() == SessionStatus.SKIPPED;
        return plan && s.end() != null ? (int) Math.max(1, Duration.between(s.start(), s.end()).toMinutes()) : null;
    }

    private static Deadlines deadlines(Input in, Collection<TaskFact> tasks) {
        int due = 0, onTime = 0, late = 0, overdue = 0, upcoming = 0, unknown = 0;
        for (TaskFact t : tasks) {
            LocalDate deadline = t.dueDate();
            if (deadline == null || deadline.isBefore(in.from()) || deadline.isAfter(in.to())) continue;
            due++;
            if (t.status() == TaskStatus.DONE) {
                LocalDate completed = in.completions().get(t.id());
                if (completed == null) unknown++;
                else if (completed.isAfter(deadline)) late++;
                else onTime++;
            } else if (deadline.isBefore(in.today())) overdue++;
            else upcoming++;
        }
        return new Deadlines(due, onTime, late, overdue, upcoming, unknown, percent(onTime, onTime + late + overdue));
    }

    private static List<TaskProgress> progress(List<TaskFact> leaves) {
        return leaves.stream().filter(t -> t.status() != TaskStatus.DONE)
                .sorted(Comparator.comparing((TaskFact t) -> t.dueDate() == null ? LocalDate.MAX : t.dueDate()).thenComparing(TaskFact::title))
                .limit(8)
                .map(t -> new TaskProgress(t.id(), t.title(), t.subjectId(), t.dueDate(), t.status().name(), t.actualMinutes(),
                        t.expectedMinutes(), t.expectedMinutes() == null || t.expectedMinutes() <= 0 ? null
                                : (int) Math.round(100.0 * t.actualMinutes() / t.expectedMinutes())))
                .toList();
    }

    private static int longestStreak(List<Boolean> active) {
        int best = 0, run = 0;
        for (boolean a : active) { run = a ? run + 1 : 0; best = Math.max(best, run); }
        return best;
    }

    /** Серія активних днів, що закінчується останнім днем періоду (але не пізніше сьогодні). Сьогодні ще можна продовжити. */
    private static int currentStreak(Input in, Map<LocalDate, Acc> days, List<Boolean> active) {
        LocalDate end = in.to().isAfter(in.today()) ? in.today() : in.to();
        int index = (int) java.time.temporal.ChronoUnit.DAYS.between(in.from(), end);
        if (index < 0 || index >= active.size()) return 0;
        if (!active.get(index) && end.equals(in.today())) index--;
        int run = 0;
        while (index >= 0 && active.get(index)) { run++; index--; }
        return run;
    }

    private static Integer percent(int part, int whole) {
        return whole <= 0 ? null : (int) Math.round(100.0 * part / whole);
    }
}
