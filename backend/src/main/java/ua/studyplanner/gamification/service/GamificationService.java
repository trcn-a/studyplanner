package ua.studyplanner.gamification.service;

import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.gamification.domain.GamificationRules;
import ua.studyplanner.gamification.dto.GamificationView;
import ua.studyplanner.gamification.service.RewardDataService;
import ua.studyplanner.planning.service.PlanningService;
import ua.studyplanner.task.dto.TaskCompleted;
import java.time.*;

@Service
public class GamificationService {
 private final ua.studyplanner.account.service.AccountDataService profiles;
 private final RewardDataService rewards;
 private final ua.studyplanner.gamification.service.AchievementDataService achievements;
 private final ua.studyplanner.account.service.AccountAccess accounts;
 private final org.springframework.context.ApplicationEventPublisher events;
 private final PlanningService planning;
 public GamificationService(ua.studyplanner.gamification.service.AchievementDataService achievements,ua.studyplanner.account.service.AccountDataService profiles,RewardDataService rewards,PlanningService planning,org.springframework.context.ApplicationEventPublisher events,ua.studyplanner.account.service.AccountAccess accounts) {this.achievements=achievements;this.profiles=profiles;this.accounts=accounts;this.rewards=rewards;this.planning=planning;this.events=events;}
 @EventListener
 @Transactional
 public void completed(TaskCompleted event) {
  accounts.lockCurrent();

  rewards.record(event.taskId(),GamificationRules.points(event.subtask()),event.completedAt());
  captureAchievements(event.completedAt());
 }
 /** Дати завершення завдань користувача (за часовим поясом планування); використовуються аналітикою. */
 @Transactional(readOnly=true)
 public java.util.Map<java.util.UUID,LocalDate> completionDates() {return rewards.completionDates(ZoneId.of(planning.settings().zoneId()));}
 @Transactional(readOnly=true)
 public GamificationView summary() {
  var raw=calculate();var earned=achievements.earned();
  var badges=raw.achievements().stream().map(a->earned.contains(a.id())?new GamificationView.Achievement(a.id(),a.title(),a.description(),a.target(),a.target(),true):a).toList();
  return new GamificationView(raw.points(),raw.level(),raw.levelProgress(),raw.nextLevelPoints(),raw.completedTasks(),raw.currentStreak(),raw.longestStreak(),raw.activeToday(),raw.week(),badges);
 }
 @Transactional public void captureAchievements(Instant at){
  accounts.lockCurrent();
  for(var a:calculate().achievements())if(a.unlocked()&&achievements.record(a.id(),at))events.publishEvent(new ua.studyplanner.notification.dto.NoticeRequested("achievement:"+a.id(),"ACHIEVEMENT","Нове досягнення",a.title()+" · "+a.description()));
 }
 private GamificationView calculate() {
  var all=rewards.rewards();
  var today=LocalDate.now(ZoneId.of(planning.settings().zoneId()));
  return GamificationRules.summarize(all.stream().mapToLong(RewardDataService.Reward::points).sum(),all.size(),
    all.stream().map(r->r.completedAt()==null?null:r.completedAt().atZone(ZoneId.of(planning.settings().zoneId())).toLocalDate()).toList(),today,profiles.studyDays(ua.studyplanner.account.security.CurrentUser.id()));
 }
}
