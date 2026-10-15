package ua.studyplanner.notification.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.account.service.AccountService;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.planning.service.PlanningService;
import ua.studyplanner.task.service.TaskPlanningQueryService;
import ua.studyplanner.notification.service.NotificationDataService;
import ua.studyplanner.notification.domain.ReminderRules;
import java.time.*;
import java.util.stream.Collectors;
import java.util.Set;
@Service
public class ReminderService {
 private final ua.studyplanner.account.service.AccountAccess access;
 private final AccountService accounts;private final PlanningService planning;private final TaskPlanningQueryService tasks;private final NotificationDataService notifications;
 public ReminderService(AccountService accounts,PlanningService planning,TaskPlanningQueryService tasks,NotificationDataService notifications,ua.studyplanner.account.service.AccountAccess access){this.access=access;this.accounts=accounts;this.planning=planning;this.tasks=tasks;this.notifications=notifications;}
 @Transactional public void generate(){
  var account=accounts.me();
  var state=planning.read();
  var values=account.remindersEnabled()?ReminderRules.pending(tasks.snapshot().tasks(),state.sessions().stream().filter(s->!s.source().equals("RUNNING")&&!s.source().equals("COMPLETED")).toList(),LocalDateTime.now(ZoneId.of(state.settings().zoneId())),account.reminderMinutes()):java.util.List.<ua.studyplanner.notification.dto.NoticeRequested>of();
  access.lockCurrent();notifications.retireReminders(values.stream().map(v->v.key()).collect(Collectors.toSet()));
  for(var notice:values)notifications.add(notice);
 }
}
