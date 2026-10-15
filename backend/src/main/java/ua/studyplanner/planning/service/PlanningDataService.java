package ua.studyplanner.planning.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.planning.domain.*;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.planning.repository.*;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.common.exception.ResourceNotFoundException;
import java.util.*;
@Service
@Transactional
public class PlanningDataService {
 private final PlanningRepository planning;
 private final StudyAvailabilityRepository availability;
 private final PlanningIssueRepository issues;
 public PlanningDataService(PlanningRepository planning,StudyAvailabilityRepository availability,PlanningIssueRepository issues){this.planning=planning;this.availability=availability;this.issues=issues;}
 private UUID user(){return CurrentUser.id();}
 private PlanningSettings current(){return planning.findById(user()).orElseThrow(()->new ResourceNotFoundException("Налаштування не знайдено"));}
 public void lock(){planning.findLocked(user()).orElseThrow(()->new ResourceNotFoundException("Налаштування не знайдено"));}
 public Settings settings(){
  var available=availability.findByUserIdOrderByDayOfWeekAscStartTimeAsc(user()).stream().map(a->new Availability(a.getDayOfWeek(),a.getStartTime(),a.getEndTime())).toList();
  var p=current();return new Settings(p.getZoneId(),p.getAutoReplan(),p.getHorizonDays(),p.getSessionMinutes(),available,p.getHabitAware(),p.getBreakMinutes());
 }
 public void saveSettings(Settings input){
  var p=current();p.setZoneId(input.zoneId());p.setAutoReplan(input.autoReplan());p.setHorizonDays(input.horizonDays());p.setSessionMinutes(input.sessionMinutes());p.setHabitAware(input.habitAware());p.setBreakMinutes(input.breakMinutes());p.setFingerprint(null);planning.save(p);
  availability.deleteByUserId(user());availability.flush();
  for(var inputAvailability:input.availability()){
   var a=new StudyAvailability();a.setId(UUID.randomUUID());a.setUserId(user());a.setDayOfWeek(inputAvailability.dayOfWeek());a.setStartTime(inputAvailability.start());a.setEndTime(inputAvailability.end());availability.save(a);
  }
 }
 public String fingerprint(){return current().getFingerprint();}
 public void saveFingerprint(String signature){var p=current();p.setFingerprint(signature);planning.save(p);}
 public List<Issue> issues(){return issues.findOwned(user()).stream().map(i->new Issue(i.getTaskId(),i.getTitle(),i.getReason(),i.getRemainingMinutes())).toList();}
 public void replaceIssues(List<Issue> inputIssues){
  issues.deleteAll(issues.findOwned(user()));issues.flush();
  for(var input:inputIssues){var i=new PlanningIssue();i.setTaskId(input.taskId());i.setTitle(input.title());i.setReason(input.reason());i.setRemainingMinutes(input.remainingMinutes());issues.save(i);}
 }
}
