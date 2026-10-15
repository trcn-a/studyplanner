package ua.studyplanner.analytics.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.analytics.domain.AnalyticsCalculator;
import ua.studyplanner.analytics.domain.AnalyticsCalculator.*;
import ua.studyplanner.analytics.dto.AnalyticsView;
import ua.studyplanner.common.exception.BusinessRuleException;
import ua.studyplanner.common.time.UserTime;
import ua.studyplanner.gamification.service.GamificationService;
import ua.studyplanner.prediction.dto.PredictionView;
import ua.studyplanner.prediction.service.DurationPredictionService;
import ua.studyplanner.session.service.SessionPlanningService;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.task.dto.TaskView;
import ua.studyplanner.task.service.TaskPlanningQueryService;

/** Збирає дані суміжних модулів і передає їх чистому калькулятору показників. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    static final int MAX_DAYS = 366;
    static final int DEFAULT_DAYS = 30;

    private final SubjectService subjects;
    private final TaskPlanningQueryService tasks;
    private final SessionPlanningService sessions;
    private final GamificationService gamification;
    private final DurationPredictionService predictions;
    private final UserTime time;

    public AnalyticsService(SubjectService subjects, TaskPlanningQueryService tasks, SessionPlanningService sessions,
            GamificationService gamification, DurationPredictionService predictions, UserTime time) {
        this.subjects = subjects;
        this.tasks = tasks;
        this.sessions = sessions;
        this.gamification = gamification;
        this.predictions = predictions;
        this.time = time;
    }

    /** Показники за період [from, to]; без параметрів — останні 30 днів до сьогодні. */
    public AnalyticsView report(LocalDate from, LocalDate to) {
        ZoneId zone = time.zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS - 1) : from;
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS)
            throw new BusinessRuleException("Оберіть період від одного до " + MAX_DAYS + " днів");

        Map<java.util.UUID, Integer> predicted = predictions.predictions().stream()
                .collect(Collectors.toMap(PredictionView::taskId, PredictionView::minutes, (a, b) -> a));
        List<TaskView> views = this.tasks.snapshot().tasks();
        List<TaskFact> taskFacts = views.stream()
                .map(t -> new TaskFact(t.id(), t.subjectId(), t.parentTaskId(), t.title(), t.status(), t.dueDate(),
                        t.actualMinutes(), t.planningMinutes(predicted.get(t.id()))))
                .toList();
        List<SessionFact> sessionFacts = sessions.history().stream()
                .map(s -> new SessionFact(s.taskId(), s.startedAt(), s.endedAt(), s.minutes(), s.plannedMinutes(), s.status()))
                .toList();
        List<SubjectFact> subjectFacts = subjects.subjects().stream()
                .map(s -> new SubjectFact(s.id(), s.name(), s.color()))
                .toList();
        return AnalyticsCalculator.compute(new Input(start, end, today, Instant.now(), zone, subjectFacts, taskFacts,
                sessionFacts, gamification.completionDates()));
    }
}
