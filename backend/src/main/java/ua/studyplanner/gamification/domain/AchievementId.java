package ua.studyplanner.gamification.domain;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
@Embeddable
public class AchievementId implements Serializable {
    @Column(name="user_id") private UUID userId;
    @Column(name="achievement_code", length=40) private String code;
    public AchievementId() {}
    public AchievementId(UUID userId,String code) { this.userId=userId; this.code=code; }
    public UUID getUserId() { return userId; }
    public String getCode() { return code; }
    @Override public boolean equals(Object other) {
        return other instanceof AchievementId id && Objects.equals(userId,id.userId) && Objects.equals(code,id.code);
    }
    @Override public int hashCode() { return Objects.hash(userId,code); }
}
