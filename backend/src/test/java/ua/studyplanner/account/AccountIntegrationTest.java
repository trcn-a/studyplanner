package ua.studyplanner.account;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.studyplanner.account.service.AccountService;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.subject.dto.SubjectInput;
import ua.studyplanner.task.service.*;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.gamification.service.GamificationService;
import ua.studyplanner.googlecalendar.service.*;
import ua.studyplanner.googlecalendar.domain.GoogleBusyEvent;
import ua.studyplanner.planning.service.*;
import ua.studyplanner.notification.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:accounts;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver","app.legacy-claim-token=local-test-claim-token-at-least-24-chars"})
class AccountIntegrationTest {
 @Autowired WebApplicationContext context;
 @Autowired AccountService accounts;
 @Autowired SubjectService subjects;
 @Autowired TaskService tasks;
 @Autowired TimeTrackingService times;
 @Autowired GamificationService rewards;
 @Autowired GoogleCalendarStore googleStore;
 @Autowired GoogleCalendarService google;
 @Autowired PlanningService planning;
 @Autowired CalendarExchangeService calendars;
 @Autowired NotificationService notices;
 @Autowired ReminderService reminders;
 @Autowired JdbcTemplate db;
 MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 private UUID register(){return accounts.register(UUID.randomUUID()+"@example.com","Student","long-password-123","").id();}
 private TaskInput task(TaskStatus status,LocalDate due,LocalDateTime start){return new TaskInput("Private task","",status,Priority.MEDIUM,due,30,start,start==null?null:start.plusMinutes(30));}
 @Test void anonymousAndMissingCsrfRequestsAreRejected() throws Exception {
  mvc.perform(get("/api/subjects")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"email\":\"x@example.com\",\"name\":\"X\",\"password\":\"long-password\"}")).andExpect(status().isForbidden());
 }
 @Test void browserCsrfTokenWorksAcrossLoginAndLogout() throws Exception {
  var csrfResult=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)csrfResult.getRequest().getSession();
  String token=com.jayway.jsonpath.JsonPath.read(csrfResult.getResponse().getContentAsString(),"$.token");
  mvc.perform(post("/api/auth/register").session(session).header("X-CSRF-TOKEN",token).contentType("application/json").content("{\"email\":\""+UUID.randomUUID()+"@example.com\",\"name\":\"Browser\",\"password\":\"long-password-123\"}")).andExpect(status().isOk());
  var refreshed=mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
  String next=com.jayway.jsonpath.JsonPath.read(refreshed.getResponse().getContentAsString(),"$.token");
  mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN",next)).andExpect(status().isNoContent());
 }
 @Test void sessionLoginLogoutAndProfileWork() throws Exception {
  String email=UUID.randomUUID()+"@example.com";
  var session=new MockHttpSession();String before=session.getId();
  var response=mvc.perform(post("/api/auth/register").session(session).with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"name\":\"Student\",\"password\":\"long-password-123\"}")).andExpect(status().isOk()).andReturn();
  var signed=(MockHttpSession)response.getRequest().getSession();assertNotEquals(before,signed.getId());
  mvc.perform(get("/api/auth/me").session(signed)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
  mvc.perform(put("/api/auth/profile").session(signed).with(csrf()).contentType("application/json").content("{\"name\":\"Updated\",\"remindersEnabled\":false,\"reminderMinutes\":30}")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
  mvc.perform(post("/api/auth/logout").session(signed).with(csrf())).andExpect(status().isNoContent());
  assertTrue(signed.isInvalid());mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email.toUpperCase(Locale.ROOT)+"\",\"password\":\"long-password-123\"}")).andExpect(status().isOk());
  assertTrue(db.queryForObject("SELECT password_hash FROM app_users WHERE email=?",String.class,email).startsWith("$2"));
 }
 @Test void usersCannotReadOrModifyEachOthersData() throws Exception {
  UUID a=register(),b=register();var subject=new AtomicReference<UUID>();var task=new AtomicReference<UUID>();
  CurrentUser.runAs(a,()->{
   subject.set(subjects.createSubject(new SubjectInput("Private","","#81583d")).id());
   task.set(tasks.createTask(subject.get(),null,task(TaskStatus.DONE,null,null)).id());
   googleStore.connect("encrypted",List.of(new GoogleBusyEvent(UUID.randomUUID(),"Private Google event",Instant.now(),Instant.now().plusSeconds(3600))));
   calendars.importCalendar("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:shared-uid\nDTSTART:20261008T100000\nDTEND:20261008T110000\nSUMMARY:Private\nEND:VEVENT\nEND:VCALENDAR");
   assertEquals(10,rewards.summary().points());assertFalse(notices.list().isEmpty());
  });
  CurrentUser.runAs(b,()->{
   assertTrue(subjects.subjects().isEmpty());assertTrue(tasks.samples().isEmpty());
   assertTrue(planning.read().externalEvents().isEmpty());assertEquals(0,rewards.summary().points());
   assertFalse(google.status().connected());assertTrue(notices.list().isEmpty());
   assertThrows(ua.studyplanner.common.exception.ResourceNotFoundException.class,()->tasks.deleteTask(task.get()));
   assertThrows(ua.studyplanner.common.exception.ResourceNotFoundException.class,()->times.entries(task.get()));
   calendars.importCalendar("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:shared-uid\nDTSTART:20261008T120000\nDTEND:20261008T130000\nSUMMARY:Other\nEND:VEVENT\nEND:VCALENDAR");
   assertEquals(1,planning.read().externalEvents().size());
  });
  var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(b,null,List.of());
  mvc.perform(get("/api/subjects/"+subject.get()+"/tasks").with(authentication(auth))).andExpect(status().isNotFound());
  mvc.perform(patch("/api/tasks/"+task.get()+"/status").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"status\":\"TODO\"}")).andExpect(status().isNotFound());
  CurrentUser.runAs(a,()->assertEquals(2,planning.read().externalEvents().size()));
 }
 @Test void independentUsersCanRunTimersAndRemindersAreDeduplicated() {
  UUID a=register(),b=register();var id=new AtomicReference<UUID>();
  for(var user:List.of(a,b))CurrentUser.runAs(user,()->{
   var subject=subjects.createSubject(new SubjectInput("Study","","#81583d"));
   var now=LocalDateTime.now(ZoneId.of("Europe/Kyiv"));
   var task=tasks.createTask(subject.id(),null,task(TaskStatus.TODO,now.toLocalDate(),now.plusMinutes(5)));id.set(task.id());
   times.startTimer(task.id());reminders.generate();reminders.generate();
   assertEquals(1,notices.list().size()); // The planned block became a running session; only the deadline remains.
  });
  CurrentUser.runAs(b,()->{
   tasks.updateStatus(id.get(),TaskStatus.DONE);reminders.generate();
   assertTrue(notices.list().stream().filter(n->!n.kind().equals("ACHIEVEMENT")).allMatch(n->n.read()));
  });
 }
 @Test void legacyClaimRequiresSecretAndIsSingleUse(){
  assertThrows(ua.studyplanner.common.exception.BusinessRuleException.class,()->accounts.register(UUID.randomUUID()+"@example.com","Bad","long-password-123","wrong"));
  var user=accounts.register(UUID.randomUUID()+"@example.com","Owner","long-password-123","local-test-claim-token-at-least-24-chars");
  CurrentUser.runAs(user.id(),()->assertNotNull(planning.settings()));
  assertThrows(ua.studyplanner.common.exception.BusinessRuleException.class,()->accounts.register(UUID.randomUUID()+"@example.com","Again","long-password-123","local-test-claim-token-at-least-24-chars"));
 }
 @Test void nestedAndForeignSubjectSubtasksAreRejected(){
  CurrentUser.runAs(register(),()->{
   var a=subjects.createSubject(new SubjectInput("A","","#81583d"));
   var b=subjects.createSubject(new SubjectInput("B","","#81583d"));
   var parent=tasks.createTask(a.id(),null,task(TaskStatus.TODO,null,null));
   var child=tasks.createTask(a.id(),parent.id(),task(TaskStatus.TODO,null,null));
   assertThrows(ua.studyplanner.common.exception.BusinessRuleException.class,()->tasks.createTask(a.id(),child.id(),task(TaskStatus.TODO,null,null)));
   assertThrows(ua.studyplanner.common.exception.BusinessRuleException.class,()->tasks.createTask(b.id(),parent.id(),task(TaskStatus.TODO,null,null)));
  });
 }

 @Test void studyDaysArePersistedValidatedAndIsolated() throws Exception {
  UUID a=register(),b=register();
  var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(a,null,List.of());
  mvc.perform(get("/api/auth/me").with(authentication(auth))).andExpect(status().isOk()).andExpect(jsonPath("$.studyDays").value(31));
  mvc.perform(put("/api/auth/profile").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"name\":\"Student\",\"remindersEnabled\":true,\"reminderMinutes\":15,\"studyDays\":21}")).andExpect(status().isOk()).andExpect(jsonPath("$.studyDays").value(21));
  mvc.perform(get("/api/auth/me").with(authentication(auth))).andExpect(status().isOk()).andExpect(jsonPath("$.studyDays").value(21));
  mvc.perform(put("/api/auth/profile").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"name\":\"Student\",\"remindersEnabled\":true,\"reminderMinutes\":15}")).andExpect(status().isOk()).andExpect(jsonPath("$.studyDays").value(21));
  for(int mask:List.of(0,128))mvc.perform(put("/api/auth/profile").with(authentication(auth)).with(csrf()).contentType("application/json").content("{\"name\":\"Student\",\"remindersEnabled\":true,\"reminderMinutes\":15,\"studyDays\":"+mask+"}")).andExpect(status().isBadRequest());
  CurrentUser.runAs(b,()->assertEquals(31,accounts.me().studyDays()));
 }

 @Test void changingStudyDaysRecalculatesRewardHistoryWithoutChangingPoints() {
  UUID user=register();
  CurrentUser.runAs(user,()->{
   var today=LocalDate.now(ZoneId.of(planning.settings().zoneId()));
   accounts.update("Student",true,15,127);
   for(int i=0;i<3;i++){var day=today.minusDays(i);db.update("INSERT INTO completion_rewards(id,task_id,user_id,points,completed_at) VALUES(?,?,?,10,?)",UUID.randomUUID(),UUID.randomUUID(),user,day.atStartOfDay(ZoneId.of(planning.settings().zoneId())).toOffsetDateTime());}
   assertEquals(3,rewards.summary().currentStreak());
   accounts.update("Student",true,15,1 << (today.getDayOfWeek().getValue()-1));
   assertEquals(1,rewards.summary().currentStreak());assertEquals(30,rewards.summary().points());
  });
 }
}
