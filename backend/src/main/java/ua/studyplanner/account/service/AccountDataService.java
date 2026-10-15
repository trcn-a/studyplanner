package ua.studyplanner.account.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.account.domain.*;
import ua.studyplanner.account.dto.AccountView;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.account.repository.*;
import ua.studyplanner.planning.domain.PlanningSettings;
import ua.studyplanner.planning.repository.*;
import ua.studyplanner.gamification.repository.*;
import ua.studyplanner.googlecalendar.repository.GoogleCalendarRepository;
import ua.studyplanner.subject.repository.SubjectRepository;
import ua.studyplanner.common.exception.*;
import java.util.*;

@Service
@Transactional
public class AccountDataService {
 public record Credentials(UUID id,String hash) {}
 private final AccountRepository accounts;
 private final NotificationSettingsRepository notifications;
 private final PlanningRepository planning;
 private final SubjectRepository subjects;
 private final StudyAvailabilityRepository availability;
 private final CalendarEventRepository calendars;
 private final GoogleCalendarRepository google;
 private final RewardRepository rewards;
 private final AchievementRepository achievements;
 public AccountDataService(AccountRepository accounts,NotificationSettingsRepository notifications,PlanningRepository planning,
   SubjectRepository subjects,StudyAvailabilityRepository availability,CalendarEventRepository calendars,
   GoogleCalendarRepository google,RewardRepository rewards,AchievementRepository achievements){
  this.accounts=accounts;this.notifications=notifications;this.planning=planning;this.subjects=subjects;this.availability=availability;
  this.calendars=calendars;this.google=google;this.rewards=rewards;this.achievements=achievements;
 }
 private Account account(UUID id){return accounts.findById(id).orElseThrow(()->new ResourceNotFoundException("Користувача не знайдено"));}
 public void lockUser(UUID id){accounts.findLocked(id).orElseThrow(()->new ResourceNotFoundException("Користувача не знайдено"));}
 public void lockSetup(){lockUser(CurrentUser.LEGACY);}
 public boolean emailExists(String email){return accounts.existsByEmailIgnoreCase(email);}
 public UUID create(String email,String name,String hash){
  var a=new Account();a.setId(UUID.randomUUID());a.setEmail(email);a.setDisplayName(name);a.setPasswordHash(hash);accounts.saveAndFlush(a);
  var p=new PlanningSettings();p.setUserId(a.getId());planning.save(p);
  var n=new NotificationSettings();n.setUserId(a.getId());notifications.save(n);return a.getId();
 }
 public Optional<Credentials> credentials(String email){return accounts.findByEmailIgnoreCaseAndEnabledTrue(email).map(a->new Credentials(a.getId(),a.getPasswordHash()));}
 public AccountView profile(UUID id){
  var a=account(id);if(!a.getEnabled())throw new ResourceNotFoundException("Користувача не знайдено");
  var p=planning.findById(id).orElseThrow();var n=notifications.findById(id).orElseThrow();
  return new AccountView(id,a.getEmail(),a.getDisplayName(),n.getRemindersEnabled(),n.getReminderMinutes(),p.getStudyDays());
 }
 public void update(UUID id,String name,boolean reminders,int minutes,int days){
  var a=account(id);a.setDisplayName(name);accounts.save(a);
  var n=notifications.findById(id).orElseThrow();n.setRemindersEnabled(reminders);n.setReminderMinutes(minutes);notifications.save(n);
  var p=planning.findById(id).orElseThrow();p.setStudyDays(days);planning.save(p);
 }
 public int studyDays(UUID id){return planning.findById(id).orElseThrow().getStudyDays();}
 public List<UUID> users(){return accounts.findByEnabledTrue().stream().map(Account::getId).toList();}
 public void claimLegacy(UUID owner){
  UUID legacy=CurrentUser.LEGACY;
  if(!planning.existsById(legacy))throw new BusinessRuleException("Попередні дані вже перенесено");
  // Repository bulk updates flush and clear managed state after transferring identifiers.
  planning.deleteById(owner);planning.flush();
  subjects.reassignOwner(owner,legacy);planning.reassignOwner(owner,legacy);availability.reassignOwner(owner,legacy);
  calendars.reassignOwner(owner,legacy);google.reassignOwner(owner,legacy);rewards.reassignOwner(owner,legacy);achievements.reassignOwner(owner,legacy);
 }
}
