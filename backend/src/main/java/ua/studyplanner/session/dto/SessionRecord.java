package ua.studyplanner.session.dto;
import ua.studyplanner.session.domain.SessionSource;
import java.time.Instant;
import java.util.UUID;
import ua.studyplanner.session.domain.*;
public record SessionRecord(UUID id,UUID taskId,Instant startedAt,Instant endedAt,Integer minutes,SessionSource source,SessionStatus status,Integer plannedMinutes,boolean pinned) {
 public SessionRecord(UUID id,UUID taskId,Instant startedAt,Instant endedAt,Integer minutes,SessionSource source,SessionStatus status,Integer plannedMinutes){this(id,taskId,startedAt,endedAt,minutes,source,status,plannedMinutes,false);}
 public SessionRecord(UUID id,UUID taskId,Instant startedAt,Instant endedAt,Integer minutes,SessionSource source,SessionStatus status){this(id,taskId,startedAt,endedAt,minutes,source,status,null);}
 public static SessionRecord of(StudySession s){return new SessionRecord(s.getId(),s.getTaskId(),s.getStartedAt(),s.getEndedAt(),s.getMinutes(),s.getSource(),s.getStatus(),s.getPlannedMinutes(),s.isPinned());}
 public boolean isRunning(){return status==SessionStatus.RUNNING;}
 public boolean isCompleted(){return status==SessionStatus.COMPLETED;}
}
