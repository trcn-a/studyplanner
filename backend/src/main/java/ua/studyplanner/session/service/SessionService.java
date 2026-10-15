package ua.studyplanner.session.service;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.account.service.AccountAccess;
import ua.studyplanner.common.time.UserTime;
import ua.studyplanner.common.exception.*;
import ua.studyplanner.session.domain.*;
import ua.studyplanner.session.dto.SessionRecord;
import ua.studyplanner.session.repository.StudySessionRepository;

@Service @Transactional
public class SessionService {
 private final StudySessionRepository sessions;private final UserTime time;private final AccountAccess accounts;
 public SessionService(StudySessionRepository sessions,UserTime time,AccountAccess accounts){this.sessions=sessions;this.time=time;this.accounts=accounts;}
 @Transactional(readOnly=true) public List<SessionRecord> actual(UUID taskId){return actual(List.of(taskId));}
 @Transactional(readOnly=true) public List<SessionRecord> actual(Collection<UUID> taskIds){
  if(taskIds.isEmpty())return List.of();
  return sessions.findByTaskIdIn(taskIds).stream().filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())).map(SessionRecord::of).sorted(Comparator.comparing(SessionRecord::startedAt).reversed()).toList();
 }
 @Transactional(readOnly=true) public Optional<SessionRecord> manualPlan(UUID taskId){return sessions.planned(taskId,CurrentUser.id()).stream().filter(s->s.getSource()==SessionSource.MANUAL).map(SessionRecord::of).findFirst();}
 public void start(UUID taskId,Instant now){
  requireOwnedTask(taskId);
  accounts.lockCurrent();
  if(sessions.existsByUserIdAndStatus(CurrentUser.id(),SessionStatus.RUNNING))throw new BusinessRuleException("Таймер уже запущено. Спочатку зупиніть його");
  var next=sessions.planned(taskId,CurrentUser.id()).stream().filter(s->s.getEndedAt().isAfter(now)).findFirst();
  if(next.isPresent())next.get().begin(now);else sessions.save(StudySession.startTimer(taskId,now));
 }
 public void stop(UUID taskId,Instant now){
  accounts.lockCurrent();
  var running=ownedActual(taskId).stream().filter(StudySession::isRunning).findFirst().orElseThrow(()->new BusinessRuleException("Для цієї задачі таймер не запущено"));
  running.stop(now);
 }
 public void manual(UUID taskId,LocalDate date,int minutes){
  requireOwnedTask(taskId);
  accounts.lockCurrent();if(minutes<=0)throw new BusinessRuleException("Тривалість має бути додатною");
  var zone=time.zone();var day=date==null?LocalDate.now(zone):date;
  var planned=sessions.planned(taskId,CurrentUser.id()).stream().filter(s->s.getStartedAt().atZone(zone).toLocalDate().equals(day)).findFirst();
  if(planned.isPresent())planned.get().completeManually(planned.get().getStartedAt(),minutes);
  else sessions.save(StudySession.manual(taskId,day.atStartOfDay(zone).toInstant(),minutes));
 }
 public UUID deleteActual(UUID id){
  accounts.lockCurrent();
  var record=sessions.findById(id).filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())&&(s.isRunning()||s.isCompleted())).orElseThrow(()->new ResourceNotFoundException("Запис часу не знайдено"));
  var taskId=record.getTaskId();sessions.delete(record);sessions.flush();return taskId;
 }
 public void saveManualPlan(UUID taskId,boolean completed,LocalDateTime start,LocalDateTime end){
  requireOwnedTask(taskId);
  accounts.lockCurrent();
  var planned=sessions.planned(taskId,CurrentUser.id()).stream().filter(s->s.getSource()==SessionSource.MANUAL).toList();
  if(start==null||completed){sessions.deleteAll(planned);return;}
  var from=time.store(start).toInstant();var to=time.store(end).toInstant();
  if(planned.isEmpty())sessions.save(StudySession.planned(taskId,from,to,SessionSource.MANUAL));else planned.getFirst().reschedule(from,to);
 }
 public void finishTask(UUID taskId){
  requireOwnedTask(taskId);
  accounts.lockCurrent();sessions.planned(taskId,CurrentUser.id()).forEach(StudySession::skip);
  ownedActual(taskId).stream().filter(StudySession::isRunning).forEach(s->s.stop(Instant.now()));
 }
 private void requireOwnedTask(UUID taskId){if(!sessions.ownsTask(taskId,CurrentUser.id()))throw new ResourceNotFoundException("Завдання не знайдено");}
 private List<StudySession> ownedActual(UUID taskId){return sessions.findByTaskIdOrderByStartedAtDesc(taskId).stream().filter(s->sessions.ownsTask(s.getTaskId(),CurrentUser.id())).toList();}
}
