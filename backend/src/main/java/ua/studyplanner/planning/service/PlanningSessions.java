package ua.studyplanner.planning.service;
import org.springframework.stereotype.Service;
import ua.studyplanner.session.service.SessionPlanningService;
import ua.studyplanner.session.dto.SessionRecord;
import ua.studyplanner.session.domain.*;
import ua.studyplanner.common.time.UserTime;
import ua.studyplanner.planning.dto.Block;
import java.time.*;
import java.util.*;
@Service
public class PlanningSessions {
 private final SessionPlanningService sessions;private final UserTime time;
 public PlanningSessions(SessionPlanningService sessions,UserTime time){this.sessions=sessions;this.time=time;}
 public List<Block> planned(){return sessions.planned().stream().map(this::block).toList();}
 public List<Block> recorded(){return sessions.recorded().stream().map(this::block).toList();}
 private Block block(SessionRecord s){var zone=time.zone();return new Block(s.id(),s.taskId(),"",s.startedAt().atZone(zone).toLocalDateTime(),(s.endedAt()==null?Instant.now():s.endedAt()).atZone(zone).toLocalDateTime(),s.status()==SessionStatus.PLANNED?(s.pinned()?"PINNED":"AUTO"):s.status().name());}
 public SessionRecord requirePlanned(UUID id,boolean manualTask){return sessions.requirePlanned(id,manualTask);}
 public void move(UUID id,LocalDateTime start,LocalDateTime end){sessions.move(id,time.store(start).toInstant(),time.store(end).toInstant());}
 public void remove(UUID id){sessions.retirePlanned(id,Instant.now());}
 public void add(List<Block> blocks){sessions.addPlanned(blocks.stream().map(b->new SessionRecord(b.id(),b.taskId(),time.store(b.start()).toInstant(),time.store(b.end()).toInstant(),null,SessionSource.AUTO,SessionStatus.PLANNED)).toList());}
}
