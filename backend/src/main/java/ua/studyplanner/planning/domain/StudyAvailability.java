package ua.studyplanner.planning.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="study_availability")
public class StudyAvailability {
    @Id
    @Column(name="id")
    private UUID id;
    
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="day_of_week")
    private int dayOfWeek;
    
    @Column(name="start_time")
    private LocalTime startTime;
    
    @Column(name="end_time")
    private LocalTime endTime;
    public StudyAvailability() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int value) { dayOfWeek = value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { startTime = value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { endTime = value; }
}
