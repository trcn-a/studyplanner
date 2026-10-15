package ua.studyplanner.planning.service;
import ua.studyplanner.session.domain.StudySession;
import ua.studyplanner.session.repository.StudySessionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.studyplanner.subject.domain.Subject;
import ua.studyplanner.subject.repository.SubjectRepository;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.repository.*;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.common.exception.BusinessRuleException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:planning;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
class PlanningIntegrationTest extends ua.studyplanner.support.LegacyUserTest {
 @Autowired PlanningService planning;
 @Autowired ua.studyplanner.session.service.SessionService sessionService;
 @Autowired CalendarExchangeService calendar;
 @Autowired JdbcTemplate db;
 @Autowired SubjectRepository subjects;
 @Autowired TaskRepository tasks;
 @Autowired StudySessionRepository entries;
 private UUID subject;
 private LocalDate tomorrow;
 @BeforeEach void setup(){
  db.update("DELETE FROM planning_issues");db.update("DELETE FROM study_sessions");db.update("DELETE FROM calendar_events");entries.deleteAll();db.update("DELETE FROM tasks");subjects.deleteAll();
  subject=subjects.save(new Subject("Test","","#81583d")).getId();tomorrow=LocalDate.now(ZoneId.of("Europe/Kyiv")).plusDays(1);
  planning.saveSettings(new Settings("Europe/Kyiv",false,7,60,List.of(new Availability(tomorrow.getDayOfWeek().getValue(),LocalTime.of(9,0),LocalTime.of(12,0)))));
 }
 private Task task(String name,Integer estimate){return tasks.save(new Task(subject,null,name,"",TaskStatus.TODO,Priority.MEDIUM,tomorrow,estimate));}
 @Test void breakSettingPersistsAndChangesGeneratedSchedule(){
  var saved=planning.saveSettings(new Settings("Europe/Kyiv",false,7,45,planning.settings().availability(),false,15));
  assertEquals(15,saved.settings().breakMinutes());assertEquals(15,planning.settings().breakMinutes());
  var target=task("Breaks",90);var result=planning.generate();
  var blocks=result.sessions().stream().filter(b->b.taskId().equals(target.getId()) && b.source().equals("AUTO")).toList();
  assertEquals(2,blocks.size());assertEquals(15,Duration.between(blocks.getFirst().end(),blocks.getLast().start()).toMinutes());
  assertThrows(BusinessRuleException.class,()->planning.saveSettings(new Settings("Europe/Kyiv",false,7,45,planning.settings().availability(),false,-1)));
 }
 @Test void remainingTimeAndManualBlocksArePreserved(){
  var a=task("Automatic",120);entries.save(StudySession.manual(a.getId(),Instant.now().minusSeconds(7200),30));
  var b=task("Manual",60);sessionService.saveManualPlan(b.getId(),false,tomorrow.atTime(10,0),tomorrow.atTime(11,0));
  var state=planning.generate();
  assertEquals(90,state.sessions().stream().filter(s->s.taskId().equals(a.getId()) && s.source().equals("AUTO")).mapToLong(s->Duration.between(s.start(),s.end()).toMinutes()).sum());
  assertTrue(state.sessions().stream().noneMatch(s->s.taskId().equals(b.getId())));
  assertEquals(tomorrow.atTime(10,0),sessionService.manualPlan(b.getId()).orElseThrow().startedAt().atZone(ZoneId.of("Europe/Kyiv")).toLocalDateTime());
  assertEquals(tomorrow.atTime(10,0).atZone(ZoneId.of("Europe/Kyiv")).toInstant(),
    db.queryForObject("SELECT starts_at FROM study_sessions WHERE task_id=? AND source='MANUAL'",OffsetDateTime.class,b.getId()).toInstant());
 }
 @Test void autoRefreshIsStableThenReactsToDeadlineAndCompletion(){
  var a=task("Auto",60);var settings=planning.settings();
  planning.saveSettings(new Settings(settings.zoneId(),true,settings.horizonDays(),settings.sessionMinutes(),settings.availability()));
  var before=planning.read();planning.refreshAutomatically();assertEquals(before.sessions(),planning.read().sessions());
  a.update(a.getTitle(),"",TaskStatus.TODO,Priority.HIGH,tomorrow.minusDays(2),60);tasks.save(a);planning.refreshAutomatically();
  assertTrue(planning.read().sessions().isEmpty());assertEquals(1,planning.read().issues().size());
  a.changeStatus(TaskStatus.DONE);tasks.save(a);planning.refreshAutomatically();assertTrue(planning.read().issues().isEmpty());
 }
 @Test void onlyChildrenAreAllocatedAndMissingEstimateIsReported(){
  var parent=task("Parent",120);var child=tasks.save(new Task(subject,parent.getId(),"Child","",TaskStatus.TODO,Priority.HIGH,tomorrow,30));
  var unknown=task("Unknown",null);var state=planning.generate();
  assertTrue(state.sessions().stream().allMatch(s->s.taskId().equals(child.getId())));assertEquals(unknown.getId(),state.issues().getFirst().taskId());
 }
 @Test void calendarImportIsIdempotentAndBlocksStudyTime(){
  task("Task",180);
  String date=tomorrow.toString().replace("-","");
  String file="BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:meeting\nDTSTART:"+date+"T100000\nDTEND:"+date+"T110000\nSUMMARY:Meeting\nEND:VEVENT\nEND:VCALENDAR";
  calendar.importCalendar(file);calendar.importCalendar(file);
  assertEquals(1,planning.read().externalEvents().size());var state=planning.generate();assertEquals(60,state.issues().getFirst().remainingMinutes());
  assertThrows(BusinessRuleException.class,()->planning.saveSettings(new Settings("Europe/Kyiv",false,7,60,List.of(new Availability(1,LocalTime.of(9,0),LocalTime.of(11,0)),new Availability(1,LocalTime.of(10,0),LocalTime.of(12,0))))));
 }

 @Test void missedSessionsAreReplacedAndRepeatedRefreshDoesNotDuplicate(){
  var task=task("Missed",90);var settings=planning.settings();
  planning.saveSettings(new Settings(settings.zoneId(),true,7,60,settings.availability()));
  db.update("UPDATE study_sessions SET starts_at=?,ends_at=?",tomorrow.minusDays(2).atTime(9,0),tomorrow.minusDays(2).atTime(10,0));
  planning.refreshAutomatically();var state=planning.read();
  assertEquals(90,state.sessions().stream().mapToLong(b->Duration.between(b.start(),b.end()).toMinutes()).sum());
  assertTrue(state.sessions().stream().allMatch(b->b.start().toLocalDate().equals(tomorrow)));
  planning.refreshAutomatically();assertEquals(state.sessions(),planning.read().sessions());
 }
 @Test void failedCalendarImportLeavesExistingEventsUntouched(){
  assertThrows(BusinessRuleException.class,()->calendar.importCalendar("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:ok\nDTSTART:20261005T090000\nDTEND:20261005T100000\nEND:VEVENT\nBEGIN:VEVENT\nUID:bad\nRRULE:FREQ=DAILY\nEND:VEVENT\nEND:VCALENDAR"));
  assertTrue(planning.read().externalEvents().isEmpty());
 }
 @Test void movedAutomaticSessionKeepsDurationAndSurvivesRegeneration(){
  var target=task("Moved",90);var block=planning.generate().sessions().getFirst();
  var moved=planning.moveSession(block.id(),tomorrow.atTime(14,0),false).sessions().stream().filter(b->b.id().equals(block.id())).findFirst().orElseThrow();
  assertEquals("PINNED",moved.source());assertEquals(Duration.between(block.start(),block.end()),Duration.between(moved.start(),moved.end()));
  var regenerated=planning.generate();assertTrue(regenerated.sessions().contains(moved));
  assertEquals(90,regenerated.sessions().stream().filter(b->target.getId().equals(b.taskId())).mapToLong(b->Duration.between(b.start(),b.end()).toMinutes()).sum());
 }
 @Test void manualSessionCanMoveToAnotherDay(){
  var target=task("Manual move",45);sessionService.saveManualPlan(target.getId(),false,tomorrow.minusDays(1).atTime(9,0),tomorrow.minusDays(1).atTime(9,45));
  planning.moveSession(target.getId(),tomorrow.atTime(15,0),true);
  var saved=sessionService.manualPlan(target.getId()).orElseThrow();
  assertEquals(tomorrow.atTime(15,0),saved.startedAt().atZone(ZoneId.of("Europe/Kyiv")).toLocalDateTime());assertEquals(45,Duration.between(saved.startedAt(),saved.endedAt()).toMinutes());
 }
 @Test void movingRejectsOverlapsPastAndDeadlineWithoutChangingSession(){
  task("First",60);task("Second",60);var before=planning.generate();var block=before.sessions().getFirst();var other=before.sessions().getLast();
  assertThrows(BusinessRuleException.class,()->planning.moveSession(block.id(),other.start(),false));
  assertThrows(BusinessRuleException.class,()->planning.moveSession(block.id(),tomorrow.minusDays(2).atTime(14,0),false));
  assertThrows(BusinessRuleException.class,()->planning.moveSession(block.id(),tomorrow.plusDays(1).atTime(14,0),false));
  assertEquals(before.sessions(),planning.read().sessions());
 }
 @Test void completedSessionsCannotBeMoved(){
  var target=task("Completed",60);var recorded=entries.save(StudySession.manual(target.getId(),Instant.now().minusSeconds(7200),60));
  assertThrows(ua.studyplanner.common.exception.ResourceNotFoundException.class,()->planning.moveSession(recorded.getId(),tomorrow.atTime(14,0),false));
 }
}
