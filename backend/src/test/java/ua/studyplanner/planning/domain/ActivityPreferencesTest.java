package ua.studyplanner.planning.domain;
import org.junit.jupiter.api.Test;
import ua.studyplanner.planning.dto.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ActivityPreferencesTest {
    private final ZoneId zone=ZoneId.of("Europe/Kyiv");
    private final LocalDateTime now=LocalDateTime.of(2026,10,5,8,0);
    private final UUID subject=UUID.randomUUID(),task=UUID.randomUUID();
    private List<ActivityPreferences.Observation> history(UUID taskId,UUID subjectId,int hour,int days) {
        var history=new ArrayList<ActivityPreferences.Observation>();
        for(int i=1;i<=days;i++) {
            Instant start=now.minusDays(i).withHour(hour).atZone(zone).toInstant();
            history.add(new ActivityPreferences.Observation(taskId,subjectId,start,start.plusSeconds(3600)));
        }
        return history;
    }
    private Settings settings(boolean enabled) {
        return new Settings(zone.getId(),true,1,60,List.of(new Availability(1,LocalTime.of(9,0),LocalTime.of(20,0))),enabled);
    }
    @Test void subjectHistorySelectsEveningOverEarlierEmptyHours() {
        var observations=history(task,subject,18,3);
        observations.addAll(history(UUID.randomUUID(),UUID.randomUUID(),9,3));
        var habits=ActivityPreferences.from(observations,zone,now.atZone(zone).toInstant());
        var work=new PlanningEngine.Work(UUID.randomUUID(),subject,"New task",now.toLocalDate(),1,60);
        var result=new PlanningEngine().plan(now,settings(true),List.of(work),List.of(),habits);
        assertEquals(18,result.sessions().getFirst().start().getHour());
        assertEquals(2,habits.subjectProfiles());
        assertTrue(result.issues().isEmpty());
    }
    @Test void habitsNeverOverrideBusyTimeOrDeadline() {
        var habits=ActivityPreferences.from(history(task,subject,18,3),zone,now.atZone(zone).toInstant());
        var busy=new Block(UUID.randomUUID(),null,"Busy",now.withHour(17),now.withHour(20),"EXTERNAL");
        var work=new PlanningEngine.Work(task,subject,"Task",now.toLocalDate(),2,120);
        var result=new PlanningEngine().plan(now,settings(true),List.of(work),List.of(busy),habits);
        assertEquals(120,result.sessions().stream().mapToLong(b->Duration.between(b.start(),b.end()).toMinutes()).sum());
        assertTrue(result.sessions().stream().allMatch(b->!b.end().isAfter(busy.start()) && b.start().toLocalDate().equals(now.toLocalDate())));
    }
    @Test void switchAndInsufficientDaysFallBackToEarliestTime() {
        var observations=history(task,subject,18,2);
        observations.addAll(observations);observations.addAll(observations);
        var habits=ActivityPreferences.from(observations,zone,now.atZone(zone).toInstant());
        assertFalse(habits.ready());
        var work=new PlanningEngine.Work(task,subject,"Task",now.toLocalDate(),1,60);
        assertEquals(9,new PlanningEngine().plan(now,settings(true),List.of(work),List.of(),habits).sessions().getFirst().start().getHour());
        habits=ActivityPreferences.from(history(task,subject,18,3),zone,now.atZone(zone).toInstant());
        assertEquals(9,new PlanningEngine().plan(now,settings(false),List.of(work),List.of(),habits).sessions().getFirst().start().getHour());
    }
    @Test void futureAndUnfinishedSessionsAreIgnoredAndLocalHoursAreUsed() {
        var observations=history(task,subject,18,3);
        observations.add(new ActivityPreferences.Observation(task,subject,now.plusDays(1).atZone(zone).toInstant(),now.plusDays(1).plusHours(1).atZone(zone).toInstant()));
        observations.add(new ActivityPreferences.Observation(task,subject,now.atZone(zone).toInstant(),null));
        var habits=ActivityPreferences.from(observations,zone,now.atZone(zone).toInstant());
        assertEquals(3,habits.sessions());
        assertTrue(habits.score(task,subject,now.withHour(18),60)>habits.score(task,subject,now.withHour(15),60));
    }
}
