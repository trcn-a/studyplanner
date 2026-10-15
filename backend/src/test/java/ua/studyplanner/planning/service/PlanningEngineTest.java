package ua.studyplanner.planning.service;
import ua.studyplanner.planning.domain.PlanningEngine;
import org.junit.jupiter.api.Test;
import ua.studyplanner.planning.dto.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PlanningEngineTest {
 private final LocalDateTime now=LocalDateTime.of(2026,10,5,8,0);
 private Settings settings(){return new Settings("Europe/Kyiv",true,2,60,List.of(new Availability(1,LocalTime.of(9,0),LocalTime.of(12,0)),new Availability(2,LocalTime.of(9,0),LocalTime.of(12,0))));}
 @Test void splitsAcrossDaysAndAvoidsBusyTime(){
  var task=new PlanningEngine.Work(UUID.randomUUID(),"Study",now.toLocalDate().plusDays(1),1,240);
  var busy=new Block(UUID.randomUUID(),null,"Meeting",now.withHour(10),now.withHour(11),"EXTERNAL");
  var result=new PlanningEngine().plan(now,settings(),List.of(task),List.of(busy));
  assertEquals(4,result.sessions().size());assertTrue(result.issues().isEmpty());
  assertEquals(240,result.sessions().stream().mapToLong(s->Duration.between(s.start(),s.end()).toMinutes()).sum());
  for(var s:result.sessions()){assertFalse(s.start().isBefore(busy.end()) && s.end().isAfter(busy.start()));assertTrue(Duration.between(s.start(),s.end()).toMinutes()<=60);}
  assertEquals(now.toLocalDate().plusDays(1),result.sessions().getLast().start().toLocalDate());
 }
 @Test void respectsDeadlineAndReportsUnallocatedMinutes(){
  var task=new PlanningEngine.Work(UUID.randomUUID(),"Study",now.toLocalDate(),1,240);
  var result=new PlanningEngine().plan(now,settings(),List.of(task),List.of());
  assertEquals(60,result.issues().getFirst().remainingMinutes());
  assertTrue(result.sessions().stream().allMatch(s->s.start().toLocalDate().equals(now.toLocalDate())));
 }
 @Test void ordersByDeadlineThenPriorityAndNeverPlansInPast(){
  var low=new PlanningEngine.Work(UUID.randomUUID(),"Low",now.toLocalDate(),0,60);
  var high=new PlanningEngine.Work(UUID.randomUUID(),"High",now.toLocalDate(),2,60);
  var later=new PlanningEngine.Work(UUID.randomUUID(),"Later",now.toLocalDate().plusDays(1),2,60);
  var result=new PlanningEngine().plan(now.withHour(10).withMinute(30),settings(),List.of(later,low,high),List.of());
  assertEquals(high.id(),result.sessions().getFirst().taskId());
  assertEquals(now.withHour(10).withMinute(30),result.sessions().getFirst().start());
  assertEquals(low.id(),result.issues().getFirst().taskId());
 }
 @Test void emptyAvailabilityAndExpiredDeadlineAreExplicit(){
  var task=new PlanningEngine.Work(UUID.randomUUID(),"Expired",now.toLocalDate().minusDays(1),1,60);
  var result=new PlanningEngine().plan(now,settings(),List.of(task),List.of());assertTrue(result.sessions().isEmpty());assertTrue(result.issues().getFirst().reason().contains("Дедлайн"));
  var empty=new Settings("Europe/Kyiv",true,7,60,List.of());
  result=new PlanningEngine().plan(now,empty,List.of(new PlanningEngine.Work(task.id(),"New",null,1,60)),List.of());
  assertEquals(60,result.issues().getFirst().remainingMinutes());
 }
}
