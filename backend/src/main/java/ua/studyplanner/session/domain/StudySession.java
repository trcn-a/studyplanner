package ua.studyplanner.session.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
import ua.studyplanner.session.domain.SessionSource;

/** One lifecycle keeps planned duration separately from recorded work. */
@Entity
@Table(name="study_sessions")
public class StudySession {
    @Id private UUID id;
    @Column(nullable=false) private UUID taskId;
    @Column(name="starts_at",nullable=false) private Instant startedAt;
    @Column(name="ends_at") private Instant endedAt;
    private Integer minutes;
    /** Обсяг, запланований до початку роботи; зберігається після переходу сесії в таймер чи ручний запис. */
    @Column(name="planned_minutes") private Integer plannedMinutes;
    @Column(nullable=false) private boolean pinned;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=12) private SessionStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private SessionSource source;
    @Column(nullable=false) private Instant createdAt;
    protected StudySession() {}
    private StudySession(UUID taskId,Instant start,Instant end,SessionSource source,SessionStatus status,Integer minutes){
        id=UUID.randomUUID();this.taskId=taskId;startedAt=start;endedAt=end;this.source=source;this.status=status;this.minutes=minutes;createdAt=Instant.now();
    }
    public static StudySession planned(UUID taskId,Instant start,Instant end,SessionSource source){var session=new StudySession(taskId,start,end,source,SessionStatus.PLANNED,null);session.plannedMinutes=span(start,end);return session;}
    private static int span(Instant start,Instant end){return (int)Math.max(1,Duration.between(start,end).toMinutes());}
    private void keepPlan(){if(plannedMinutes==null&&status==SessionStatus.PLANNED&&endedAt!=null)plannedMinutes=span(startedAt,endedAt);}
    public static StudySession planned(UUID id,UUID taskId,Instant start,Instant end,SessionSource source){var session=planned(taskId,start,end,source);session.id=id;return session;}
    public static StudySession startTimer(UUID taskId,Instant now){return new StudySession(taskId,now,null,SessionSource.TIMER,SessionStatus.RUNNING,null);}
    public static StudySession manual(UUID taskId,Instant start,int minutes){return new StudySession(taskId,start,start.plusSeconds(minutes*60L),SessionSource.MANUAL,SessionStatus.COMPLETED,minutes);}
    public void begin(Instant now){if(status!=SessionStatus.PLANNED)throw new IllegalStateException("Session is not planned");keepPlan();startedAt=now;endedAt=null;minutes=null;source=SessionSource.TIMER;status=SessionStatus.RUNNING;}
    public void completeManually(Instant start,int minutes){if(status!=SessionStatus.PLANNED)throw new IllegalStateException("Session is not planned");keepPlan();startedAt=start;endedAt=start.plusSeconds(minutes*60L);this.minutes=minutes;source=SessionSource.MANUAL;status=SessionStatus.COMPLETED;}
    public void stop(Instant now){if(!isRunning())throw new IllegalStateException("Session is not running");endedAt=now.isBefore(startedAt)?startedAt:now;minutes=(int)Math.max(1,Math.round(Duration.between(startedAt,endedAt).getSeconds()/60.0));status=SessionStatus.COMPLETED;}
    public void reschedule(Instant start,Instant end){if(status!=SessionStatus.PLANNED)throw new IllegalStateException("Only planned sessions can change");startedAt=start;endedAt=end;plannedMinutes=span(start,end);}
    public void skip(){if(status==SessionStatus.PLANNED)status=SessionStatus.SKIPPED;}
    public void pin(){pinned=true;}
    public boolean isPinned(){return pinned;}
    public boolean isRunning(){return status==SessionStatus.RUNNING;}
    public boolean isCompleted(){return status==SessionStatus.COMPLETED;}
    public UUID getId(){return id;} public UUID getTaskId(){return taskId;}
    public Instant getStartedAt(){return startedAt;} public Instant getEndedAt(){return endedAt;} public Integer getMinutes(){return minutes;} public Integer getPlannedMinutes(){return plannedMinutes;}
    public SessionSource getSource(){return source;} public SessionStatus getStatus(){return status;}
}
