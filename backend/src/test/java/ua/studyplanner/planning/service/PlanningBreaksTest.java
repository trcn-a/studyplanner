package ua.studyplanner.planning.service;

import org.junit.jupiter.api.Test;
import ua.studyplanner.planning.domain.PlanningEngine;
import ua.studyplanner.planning.dto.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlanningBreaksTest {
    private final LocalDateTime now=LocalDateTime.of(2026,10,5,8,0);
    private PlanningEngine.Work work(int minutes) { return new PlanningEngine.Work(UUID.randomUUID(),"Task",now.toLocalDate(),1,minutes); }
    private Settings settings(int gap,int session,List<Availability> available) { return new Settings("Europe/Kyiv",false,1,session,available,false,gap); }
    private List<Availability> available() { return List.of(new Availability(1,LocalTime.of(9,0),LocalTime.of(12,0))); }
    private long minutes(List<Block> blocks) { return blocks.stream().mapToLong(b->Duration.between(b.start(),b.end()).toMinutes()).sum(); }
    private void gaps(List<Block> blocks,int minimum) {
        var sorted=blocks.stream().sorted(Comparator.comparing(Block::start)).toList();
        for(int i=1;i<sorted.size();i++) assertTrue(Duration.between(sorted.get(i-1).end(),sorted.get(i).start()).toMinutes()>=minimum);
    }
    @Test void reservesBreaksWithoutCountingThemAsStudyTime() {
        var result=new PlanningEngine().plan(now,settings(15,45,available()),List.of(work(90)),List.of());
        assertTrue(result.issues().isEmpty());assertEquals(90,minutes(result.sessions()));
        assertEquals(now.withHour(10),result.sessions().get(1).start());gaps(result.sessions(),15);
    }
    @Test void reservesBreaksBetweenDifferentTasksAndAdjacentAvailabilityWindows() {
        var windows=List.of(new Availability(1,LocalTime.of(9,0),LocalTime.of(10,0)),new Availability(1,LocalTime.of(10,0),LocalTime.of(12,0)));
        var result=new PlanningEngine().plan(now,settings(15,60,windows),List.of(work(60),work(60)),List.of());
        assertTrue(result.issues().isEmpty());assertEquals(120,minutes(result.sessions()));gaps(result.sessions(),15);
        assertEquals(now.withHour(10).withMinute(15),result.sessions().get(1).start());
    }
    @Test void allowsZeroBreakToPreservePreviousBehavior() {
        var result=new PlanningEngine().plan(now,settings(0,45,available()),List.of(work(90)),List.of());
        assertEquals(result.sessions().getFirst().end(),result.sessions().get(1).start());
        assertEquals(90,minutes(result.sessions()));
    }
    @Test void reportsInsufficientTimeAfterReservingBreaks() {
        var windows=List.of(new Availability(1,LocalTime.of(9,0),LocalTime.of(10,30)));
        var result=new PlanningEngine().plan(now,settings(15,45,windows),List.of(work(90)),List.of());
        assertEquals(75,minutes(result.sessions()));assertEquals(15,result.issues().getFirst().remainingMinutes());gaps(result.sessions(),15);
    }
    @Test void reservesBreaksAroundFixedStudySessions() {
        var fixed=new Block(UUID.randomUUID(),UUID.randomUUID(),"Manual",now.withHour(10),now.withHour(10).withMinute(45),"MANUAL");
        var result=new PlanningEngine().plan(now,settings(15,45,available()),List.of(work(90)),List.of(fixed));
        var all=new ArrayList<>(result.sessions());all.add(fixed);gaps(all,15);
        assertEquals(90,minutes(result.sessions()));assertEquals(now.withHour(11),result.sessions().getLast().start());
    }
    @Test void externalEventsDoNotRequireAnExtraStudyBreak() {
        var fixed=new Block(UUID.randomUUID(),null,"Meeting",now.withHour(10),now.withHour(11),"EXTERNAL");
        var result=new PlanningEngine().plan(now,settings(15,60,available()),List.of(work(120)),List.of(fixed));
        assertTrue(result.issues().isEmpty());assertEquals(120,minutes(result.sessions()));
        assertEquals(fixed.start(),result.sessions().getFirst().end());assertEquals(fixed.end(),result.sessions().getLast().start());
    }
}
