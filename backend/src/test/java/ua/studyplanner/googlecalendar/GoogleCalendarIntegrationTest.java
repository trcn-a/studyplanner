package ua.studyplanner.googlecalendar;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.studyplanner.googlecalendar.service.GoogleCalendarStore;
import ua.studyplanner.googlecalendar.service.GoogleCalendarDataService;
import ua.studyplanner.googlecalendar.domain.GoogleBusyEvent;
import ua.studyplanner.planning.service.PlanningService;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.subject.domain.Subject;
import ua.studyplanner.subject.repository.SubjectRepository;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.repository.TaskRepository;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:google;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
class GoogleCalendarIntegrationTest extends ua.studyplanner.support.LegacyUserTest {
 @Autowired GoogleCalendarStore store;
 @Autowired GoogleCalendarDataService repository;
 @Autowired PlanningService planning;
 @Autowired JdbcTemplate db;
 @Autowired SubjectRepository subjects;
 @Autowired TaskRepository tasks;
 @Test void syncReschedulesTasksAndRemovesDeletedBusyEventsWithoutTouchingIcs() {
  var zone=ZoneId.of("Europe/Kyiv");var date=LocalDate.now(zone).plusDays(1);
  var subject=subjects.save(new Subject("Subject","","#81583d"));
  tasks.save(new Task(subject.getId(),null,"Task","",TaskStatus.TODO,Priority.MEDIUM,date,60));
  planning.saveSettings(new Settings("Europe/Kyiv",true,7,60,List.of(new Availability(date.getDayOfWeek().getValue(),LocalTime.of(9,0),LocalTime.of(12,0)))));
  assertEquals(date.atTime(9,0),planning.read().sessions().getFirst().start());
  var event=new GoogleBusyEvent(UUID.randomUUID(),"Google",date.atTime(9,0).atZone(zone).toInstant(),date.atTime(10,0).atZone(zone).toInstant());
  store.connect("encrypted",List.of(event));
  assertEquals(date.atTime(10,0),planning.read().sessions().getFirst().start());
  var revision=repository.connection().orElseThrow().revision();
  db.update("INSERT INTO calendar_events(id,external_uid,title,starts_at,ends_at,user_id) VALUES(?,?,?,?,?,'00000000-0000-0000-0000-000000000001')",UUID.randomUUID(),"ics","ICS",date.atTime(11,0),date.atTime(12,0));
  store.replace(revision,List.of());
  assertEquals(date.atTime(9,0),planning.read().sessions().getFirst().start());
  assertEquals(1,planning.read().externalEvents().size());
  store.disconnect();store.replace(revision,List.of(event));
  assertTrue(repository.connection().isEmpty());
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM calendar_events WHERE source='GOOGLE'",Integer.class));
 }
}
