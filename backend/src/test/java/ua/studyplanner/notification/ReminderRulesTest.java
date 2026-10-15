package ua.studyplanner.notification;
import org.junit.jupiter.api.Test;
import ua.studyplanner.notification.domain.ReminderRules;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.domain.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ReminderRulesTest {
 private final LocalDateTime now=LocalDateTime.of(2026,10,7,12,0);
 private TaskView task(TaskStatus status,LocalDate due,LocalDateTime start){
  var t=new Task(UUID.randomUUID(),null,"Study","",status,Priority.MEDIUM,due,30);
  if(start!=null)t.schedule(start,start.plusMinutes(30));return TaskView.of(t,TimeSummary.EMPTY);
 }
 @Test void futureOutsideWindowAndCompletedTasksAreIgnored(){
  assertTrue(ReminderRules.pending(List.of(task(TaskStatus.TODO,now.toLocalDate().plusDays(2),now.plusHours(1)),task(TaskStatus.DONE,now.toLocalDate(),now)),List.of(),now,15).isEmpty());
 }
 @Test void deadlineAndSessionGetDifferentStableKeys(){
  var task=task(TaskStatus.TODO,now.toLocalDate(),now.plusMinutes(15));
  var first=ReminderRules.pending(List.of(task),List.of(),now,15);
  assertEquals(2,first.size());assertNotEquals(first.get(0).key(),first.get(1).key());
  assertEquals(first,ReminderRules.pending(List.of(task),List.of(),now,15));
 }
 @Test void expiredSessionIsNotPresentedAsUpcoming(){
  var result=ReminderRules.pending(List.of(task(TaskStatus.TODO,now.toLocalDate().minusDays(1),now.minusHours(1))),List.of(),now,15);
  assertEquals(1,result.size());assertEquals("Дедлайн минув",result.getFirst().title());
 }
}
