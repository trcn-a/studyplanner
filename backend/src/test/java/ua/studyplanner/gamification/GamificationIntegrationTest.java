package ua.studyplanner.gamification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import ua.studyplanner.gamification.service.GamificationService;
import ua.studyplanner.task.service.TaskService;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.TaskInput;
import ua.studyplanner.subject.repository.SubjectRepository;
import ua.studyplanner.subject.domain.Subject;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:rewards;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
class GamificationIntegrationTest extends ua.studyplanner.support.LegacyUserTest {
 @Autowired TaskService tasks;
 @Autowired GamificationService rewards;
 @Autowired SubjectRepository subjects;
 @Autowired JdbcTemplate db;
 @Autowired PlatformTransactionManager manager;
 UUID subject;
 @BeforeEach void setup() {
  db.update("DELETE FROM user_achievements");
  db.update("DELETE FROM notifications");
  db.update("DELETE FROM completion_rewards");
  // This test exercises reward idempotency, regardless of the day it runs.
  db.update("UPDATE planning_settings SET study_days=127 WHERE user_id=?",ua.studyplanner.account.security.CurrentUser.id());
  subject=subjects.save(new Subject("Rewards","","#81583d")).getId();
 }
 private TaskInput input(TaskStatus status) {return new TaskInput("Task","",status,Priority.MEDIUM,null,30,null,null);}
 @Test void statusToggleAndEditCannotEarnTwiceAndDeleteKeepsPoints() {
  var task=tasks.createTask(subject,null,input(TaskStatus.TODO));
  tasks.updateStatus(task.id(),TaskStatus.DONE);
  tasks.updateStatus(task.id(),TaskStatus.TODO);
  tasks.updateTask(task.id(),input(TaskStatus.DONE));
  assertEquals(10,rewards.summary().points());assertEquals(1,rewards.summary().currentStreak());
  tasks.deleteTask(task.id());assertEquals(10,rewards.summary().points());
 }
 @Test void creatingCompletedTasksAndSubtasksUsesDifferentRewards() {
  var root=tasks.createTask(subject,null,input(TaskStatus.DONE));
  tasks.createTask(subject,root.id(),input(TaskStatus.DONE));
  assertEquals(15,rewards.summary().points());assertEquals(2,rewards.summary().completedTasks());
 }
 @Test void rolledBackCompletionDoesNotLeaveReward() {
  var task=tasks.createTask(subject,null,input(TaskStatus.TODO));
  var tx=new TransactionTemplate(manager);
  tx.executeWithoutResult(status->{tasks.updateStatus(task.id(),TaskStatus.DONE);status.setRollbackOnly();});
  assertEquals(0,rewards.summary().points());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM user_achievements",Integer.class));
  assertEquals(TaskStatus.TODO,tasks.tasks(subject).getFirst().status());
 }
 @Test void parallelCompletionHasOneReward() throws Exception {
  var task=tasks.createTask(subject,null,input(TaskStatus.TODO));
  try(var executor=Executors.newFixedThreadPool(2)) {
   var a=executor.submit(()->{authenticate();return tasks.updateStatus(task.id(),TaskStatus.DONE);});
   var b=executor.submit(()->{authenticate();return tasks.updateStatus(task.id(),TaskStatus.DONE);});
   a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
  }
  assertEquals(10,rewards.summary().points());assertEquals(1,rewards.summary().completedTasks());
 }

 @Test void earnedStreakSurvivesChangingStudyDaysAndDeletion(){
  var today=java.time.LocalDate.now(java.time.ZoneId.of("Europe/Kyiv"));
  for(int i=1;i<=2;i++)db.update("INSERT INTO completion_rewards(id,task_id,user_id,points,completed_at) VALUES(?,?,?,10,?)",UUID.randomUUID(),UUID.randomUUID(),ua.studyplanner.account.security.CurrentUser.id(),today.minusDays(i).atStartOfDay(java.time.ZoneId.of("Europe/Kyiv")).toOffsetDateTime());
  var task=tasks.createTask(subject,null,input(TaskStatus.DONE));
  assertTrue(rewards.summary().achievements().stream().anyMatch(a->a.id().equals("streak3")&&a.unlocked()));
  db.update("UPDATE planning_settings SET study_days=? WHERE user_id=?",1 << (today.getDayOfWeek().getValue()-1),ua.studyplanner.account.security.CurrentUser.id());
  assertEquals(1,rewards.summary().currentStreak());
  tasks.deleteTask(task.id());
  assertTrue(rewards.summary().achievements().stream().anyMatch(a->a.id().equals("streak3")&&a.unlocked()&&a.progress()==a.target()));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM user_achievements WHERE achievement_code='streak3'",Integer.class));
 }
}
