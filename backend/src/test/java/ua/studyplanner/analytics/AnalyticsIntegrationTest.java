package ua.studyplanner.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.account.service.AccountService;
import ua.studyplanner.analytics.service.AnalyticsService;
import ua.studyplanner.common.exception.BusinessRuleException;
import ua.studyplanner.planning.service.PlanningService;
import ua.studyplanner.session.domain.*;
import ua.studyplanner.session.dto.SessionRecord;
import ua.studyplanner.session.service.SessionPlanningService;
import ua.studyplanner.subject.dto.SubjectInput;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.service.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:analyticslife;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver"})
class AnalyticsIntegrationTest {
    @Autowired AccountService accounts;
    @Autowired SubjectService subjects;
    @Autowired TaskService tasks;
    @Autowired TimeTrackingService timing;
    @Autowired SessionPlanningService plans;
    @Autowired PlanningService planning;
    @Autowired AnalyticsService analytics;
    @Autowired JdbcTemplate db;

    private void user(Runnable test) {
        var user = accounts.register(UUID.randomUUID() + "@example.com", "Student", "long-password-123", "");
        CurrentUser.runAs(user.id(), test);
    }

    private TaskView task() {
        var subject = subjects.createSubject(new SubjectInput("Study", "", "#81583d"));
        return tasks.createTask(subject.id(), null, new TaskInput("Task", "", TaskStatus.TODO, Priority.MEDIUM, null, 60, null, null));
    }

    private AnalyticsView3 report() {
        var today = LocalDate.now(ZoneId.of("Europe/Kyiv"));
        var view = analytics.report(today.minusDays(1), today.plusDays(1));
        return new AnalyticsView3(view.plan().plannedMinutes(), view.plan().fulfilledMinutes(), view.plan().missedMinutes(), view.summary().actualMinutes());
    }

    record AnalyticsView3(int planned, int fulfilled, int missed, int actual) {}

    @Test void plannedVolumeSurvivesTimerAndIsComparedWithActualTime() {
        user(() -> {
            var task = task();
            var id = UUID.randomUUID();
            var start = Instant.now().plusSeconds(300);
            plans.addPlanned(List.of(new SessionRecord(id, task.id(), start, start.plusSeconds(3600), null, SessionSource.AUTO, SessionStatus.PLANNED)));
            assertEquals(60, plans.history().getFirst().plannedMinutes());
            timing.startTimer(task.id());
            db.update("UPDATE study_sessions SET starts_at=? WHERE id=?", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(40), id);
            timing.stopTimer(task.id());
            var done = plans.history().getFirst();
            assertEquals(SessionStatus.COMPLETED, done.status());
            assertEquals(40, done.minutes());
            assertEquals(60, done.plannedMinutes(), "план зберігається після переходу в таймер");
            var r = report();
            assertEquals(60, r.planned());
            assertEquals(40, r.fulfilled());
            assertEquals(20, r.missed());
            assertEquals(40, r.actual());
        });
    }

    @Test void pastAutomaticPlanBecomesSkippedInsteadOfDisappearing() {
        user(() -> {
            var task = task();
            var start = Instant.now().minusSeconds(7200);
            plans.addPlanned(List.of(new SessionRecord(UUID.randomUUID(), task.id(), start, start.plusSeconds(3600), null, SessionSource.AUTO, SessionStatus.PLANNED)));
            planning.generate();
            var history = plans.history();
            assertEquals(1, history.size());
            assertEquals(SessionStatus.SKIPPED, history.getFirst().status());
            var r = report();
            assertEquals(60, r.planned());
            assertEquals(0, r.fulfilled());
            assertEquals(60, r.missed());
        });
    }

    @Test void rejectsInvalidPeriods() {
        user(() -> {
            var today = LocalDate.now(ZoneId.of("Europe/Kyiv"));
            assertThrows(BusinessRuleException.class, () -> analytics.report(today, today.minusDays(1)));
            assertThrows(BusinessRuleException.class, () -> analytics.report(today.minusDays(400), today));
            assertEquals(30, analytics.report(null, null).days().size());
        });
    }
}
