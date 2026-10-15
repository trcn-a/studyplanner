package ua.studyplanner.account;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.studyplanner.account.service.AccountService;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.subject.dto.SubjectInput;
import ua.studyplanner.task.service.*;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.session.service.*;
import ua.studyplanner.session.dto.SessionRecord;
import ua.studyplanner.session.domain.*;
import ua.studyplanner.planning.service.PlanningService;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:sessionlife;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
class SessionLifecycleTest {
 @Autowired AccountService accounts;@Autowired SubjectService subjects;@Autowired TaskService tasks;
 @Autowired TimeTrackingService timing;@Autowired SessionService sessions;@Autowired SessionPlanningService plans;
 @Autowired PlanningService planning;@Autowired JdbcTemplate db;
 private void user(Runnable test){var user=accounts.register(UUID.randomUUID()+"@example.com","Student","long-password-123","");CurrentUser.runAs(user.id(),test);}
 private TaskView task(){var subject=subjects.createSubject(new SubjectInput("Study","","#81583d"));return tasks.createTask(subject.id(),null,new TaskInput("Task","",TaskStatus.TODO,Priority.MEDIUM,null,60,null,null));}
 @Test void predictionAvailabilityTracksCompletionAndTimeCorrections(){user(()->{
  var task=task();assertNotNull(tasks.samples().getFirst().availableAt());
  tasks.updateStatus(task.id(),TaskStatus.DONE);
  var old=OffsetDateTime.parse("2000-01-01T00:00:00Z");
  db.update("UPDATE tasks SET prediction_data_at=? WHERE id=?",old,task.id());
  timing.logManual(task.id(),new ManualTimeInput(25,LocalDate.now().minusDays(7)));
  var sample=tasks.samples().getFirst();
  assertTrue(sample.availableAt().isAfter(old.toInstant()));assertEquals(25,sample.actualMinutes());
  var entry=sessions.actual(task.id()).getFirst();
  db.update("UPDATE tasks SET prediction_data_at=? WHERE id=?",old,task.id());
  timing.deleteEntry(entry.id());
  sample=tasks.samples().getFirst();
  assertTrue(sample.availableAt().isAfter(old.toInstant()));assertEquals(0,sample.actualMinutes());
  db.update("UPDATE tasks SET prediction_data_at=? WHERE id=?",old,task.id());
  tasks.updateStatus(task.id(),TaskStatus.DONE);
  assertTrue(tasks.samples().getFirst().availableAt().isAfter(old.toInstant()));
 });}
 @Test void plannedSessionBecomesActualWithoutDuplicateOrEstimatedMinutes(){user(()->{
  var task=task();var id=UUID.randomUUID();var start=Instant.now().plusSeconds(300);
  plans.addPlanned(List.of(new SessionRecord(id,task.id(),start,start.plusSeconds(3600),null,SessionSource.AUTO,SessionStatus.PLANNED)));
  assertEquals(0,tasks.samples().getFirst().actualMinutes());
  timing.startTimer(task.id());assertEquals(id,sessions.actual(task.id()).getFirst().id());
  db.update("UPDATE study_sessions SET starts_at=? WHERE id=?",OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(40),id);
  timing.stopTimer(task.id());var actual=sessions.actual(task.id()).getFirst();
  assertEquals(id,actual.id());assertEquals(SessionStatus.COMPLETED,actual.status());assertEquals(40,actual.minutes());
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM study_sessions WHERE task_id=?",Integer.class,task.id()));
  assertEquals(40,tasks.samples().getFirst().actualMinutes());
  planning.generate();assertEquals(actual,sessions.actual(task.id()).getFirst());
 });}
 @Test void manualCompletionReusesPlanAndDoneNeverInventsActualTime(){user(()->{
  var task=task();var start=LocalDate.now(ZoneId.of("Europe/Kyiv")).atTime(18,0);
  sessions.saveManualPlan(task.id(),false,start,start.plusHours(1));var id=sessions.manualPlan(task.id()).orElseThrow().id();
  timing.logManual(task.id(),new ManualTimeInput(25,start.toLocalDate()));
  assertEquals(id,sessions.actual(task.id()).getFirst().id());assertEquals(25,sessions.actual(task.id()).getFirst().minutes());
  assertTrue(sessions.manualPlan(task.id()).isEmpty());
  var second=task();sessions.saveManualPlan(second.id(),false,start,start.plusHours(1));
  tasks.updateStatus(second.id(),TaskStatus.DONE);assertTrue(sessions.actual(second.id()).isEmpty());
  assertEquals("SKIPPED",db.queryForObject("SELECT status FROM study_sessions WHERE task_id=?",String.class,second.id()));
 });}
}
