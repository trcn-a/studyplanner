package ua.studyplanner.gamification.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.gamification.domain.*;
import ua.studyplanner.gamification.repository.AchievementRepository;
import java.time.Instant;
import java.util.*;
@Service
@Transactional
public class AchievementDataService {
 private final AchievementRepository achievements;
 public AchievementDataService(AchievementRepository achievements){this.achievements=achievements;}
 public Set<String> earned(){return achievements.findCodes(CurrentUser.id());}
 // The calling service holds the user's lock to serialize awards.
 public boolean record(String code,Instant at){var id=new AchievementId(CurrentUser.id(),code);if(achievements.existsById(id))return false;achievements.save(new UserAchievement(id,at));return true;}
}
