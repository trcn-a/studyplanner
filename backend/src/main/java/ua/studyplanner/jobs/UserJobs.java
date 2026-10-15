package ua.studyplanner.jobs;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import ua.studyplanner.account.service.AccountAccess;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.planning.service.PlanningService;
import ua.studyplanner.googlecalendar.service.GoogleCalendarService;
import ua.studyplanner.notification.service.ReminderService;
@Component
public class UserJobs {
 private final AccountAccess users;private final PlanningService planning;private final GoogleCalendarService google;private final ReminderService reminders;
 public UserJobs(AccountAccess users,PlanningService planning,GoogleCalendarService google,ReminderService reminders){this.users=users;this.planning=planning;this.google=google;this.reminders=reminders;}
 @Scheduled(fixedDelay=60000,initialDelay=15000)
 public void tick(){for(var user:users.activeUsers())CurrentUser.runAs(user,()->{
  try{google.sync();}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Calendar refresh failed for user {}",user);}
  try{planning.refreshAutomatically();reminders.generate();}catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Planning or reminders failed for user {}",user);}
 });}
}
