package ua.studyplanner.planning.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="planning_issues")
public class PlanningIssue {
    @Id
    @Column(name="task_id")
    private UUID taskId;
    
    @Column(name="title")
    private String title;
    
    @Column(name="reason")
    private String reason;
    
    @Column(name="remaining_minutes")
    private int remainingMinutes;
    public PlanningIssue() {}
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID value) { taskId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public int getRemainingMinutes() { return remainingMinutes; }
    public void setRemainingMinutes(int value) { remainingMinutes = value; }
}
