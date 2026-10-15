package ua.studyplanner.gamification.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="completion_rewards")
public class CompletionReward {
    @Id
    @Column(name="id")
    private UUID id;
    
    @Column(name="task_id")
    private UUID taskId;
    
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="points")
    private int points;
    
    @Column(name="completed_at")
    private Instant completedAt;
    public CompletionReward() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID value) { taskId = value; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public int getPoints() { return points; }
    public void setPoints(int value) { points = value; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
}
