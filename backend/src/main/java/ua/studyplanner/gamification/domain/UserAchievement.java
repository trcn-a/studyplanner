package ua.studyplanner.gamification.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="user_achievements")
public class UserAchievement {
    @EmbeddedId private AchievementId id;
    @Column(name="earned_at") private Instant earnedAt;
    public UserAchievement() {}
    public UserAchievement(AchievementId id,Instant at) { this.id=id; this.earnedAt=at; }
    public AchievementId getId() { return id; }
    public Instant getEarnedAt() { return earnedAt; }
}
