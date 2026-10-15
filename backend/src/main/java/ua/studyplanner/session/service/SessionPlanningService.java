package ua.studyplanner.session.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.session.repository.StudySessionRepository;
import ua.studyplanner.session.dto.SessionRecord;
import ua.studyplanner.session.domain.*;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.account.service.AccountAccess;
import java.util.*;

@Service @Transactional
public class SessionPlanningService {
 private final StudySessionRepository sessions;private final AccountAccess accounts;
 public SessionPlanningService(StudySessionRepository sessions,AccountAccess accounts){this.sessions=sessions;this.accounts=accounts;}
 @Transactional(readOnly=true) public List<SessionRecord> planned(){return sessions.findByUserIdAndStatusOrderByStartedAtAsc(CurrentUser.id(),SessionStatus.PLANNED).stream().filter(s->s.getSource()==SessionSource.AUTO).map(SessionRecord::of).toList();}
 @Transactional(readOnly=true) public List<SessionRecord> recorded(){return sessions.findByUserIdAndStatusInOrderByStartedAtAsc(CurrentUser.id(),List.of(SessionStatus.RUNNING,SessionStatus.COMPLETED)).stream().map(SessionRecord::of).toList();}
 /** Усі сесії користувача (плани, пропуски, активні й завершені) для аналітики. */
 @Transactional(readOnly=true) public List<SessionRecord> history(){return sessions.findByUserIdOrderByStartedAtAsc(CurrentUser.id()).stream().map(SessionRecord::of).toList();}
 /**
  * Знімає автоматичний план із розкладу. Майбутній план видаляється, а вже минулий позначається як пропущений,
  * щоб аналітика могла показати, скільки запланованої роботи не було виконано.
  */
 public void retirePlanned(UUID id,java.time.Instant now){accounts.lockCurrent();sessions.findById(id).filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())&&s.getStatus()==SessionStatus.PLANNED&&s.getSource()==SessionSource.AUTO).ifPresent(s->{if(s.getEndedAt().isAfter(now))sessions.delete(s);else s.skip();});sessions.flush();}
 public void removePlanned(UUID id){accounts.lockCurrent();sessions.findById(id).filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())&&s.getStatus()==SessionStatus.PLANNED&&s.getSource()==SessionSource.AUTO).ifPresent(sessions::delete);sessions.flush();}
 public void addPlanned(List<SessionRecord> values){accounts.lockCurrent();for(var v:values){if(!sessions.ownsTask(v.taskId(),CurrentUser.id()))throw new ua.studyplanner.common.exception.ResourceNotFoundException("Завдання не знайдено");sessions.save(StudySession.planned(v.id(),v.taskId(),v.startedAt(),v.endedAt(),SessionSource.AUTO));}sessions.flush();}
 public SessionRecord requirePlanned(UUID id,boolean manualTask){
  var found=manualTask?sessions.planned(id,CurrentUser.id()).stream().filter(s->s.getSource()==SessionSource.MANUAL).findFirst():sessions.findById(id).filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())&&s.getStatus()==SessionStatus.PLANNED);
  return found.map(SessionRecord::of).orElseThrow(()->new ua.studyplanner.common.exception.ResourceNotFoundException("Заплановане заняття не знайдено"));
 }
 public void move(UUID id,java.time.Instant start,java.time.Instant end){
  accounts.lockCurrent();
  var session=sessions.findById(id).filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())&&s.getStatus()==SessionStatus.PLANNED).orElseThrow(()->new ua.studyplanner.common.exception.ResourceNotFoundException("Заплановане заняття не знайдено"));
  session.reschedule(start,end);session.pin();sessions.flush();
 }
}
