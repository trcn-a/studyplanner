package ua.studyplanner.gamification.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.gamification.domain.CompletionReward;
import ua.studyplanner.gamification.repository.RewardRepository;
import ua.studyplanner.account.security.CurrentUser;
import java.time.*;
import java.util.*;
@Service
@Transactional
public class RewardDataService {
 public record Reward(int points,Instant completedAt) {}
 private final RewardRepository rewards;
 public RewardDataService(RewardRepository rewards){this.rewards=rewards;}
 public void record(UUID taskId,int points,Instant at){
  if(rewards.existsByTaskId(taskId))return;
  var r=new CompletionReward();r.setId(UUID.randomUUID());r.setTaskId(taskId);r.setUserId(CurrentUser.id());r.setPoints(points);r.setCompletedAt(at);rewards.save(r);
 }
 public Map<UUID,LocalDate> completionDates(ZoneId zone){var result=new HashMap<UUID,LocalDate>();for(var r:rewards.findByUserId(CurrentUser.id()))if(r.getCompletedAt()!=null)result.put(r.getTaskId(),r.getCompletedAt().atZone(zone).toLocalDate());return result;}
 public List<Reward> rewards(){return rewards.findByUserId(CurrentUser.id()).stream().map(r->new Reward(r.getPoints(),r.getCompletedAt())).toList();}
}
