package ua.studyplanner.planning.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="planning_settings")
public class PlanningSettings {
    @Id
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="zone_id")
    private String zoneId = "Europe/Kyiv";
    
    @Column(name="auto_replan")
    private boolean autoReplan;
    
    @Column(name="horizon_days")
    private int horizonDays = 14;
    
    @Column(name="session_minutes")
    private int sessionMinutes = 60;
    
    @Column(name="break_minutes")
    private int breakMinutes = 0;
    
    @Column(name="habit_aware")
    private boolean habitAware = true;
    
    @Column(name="study_days")
    private int studyDays = 31;
    
    @Column(name="fingerprint")
    private String fingerprint;
    public PlanningSettings() {}
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String value) { zoneId = value; }
    public boolean getAutoReplan() { return autoReplan; }
    public void setAutoReplan(boolean value) { autoReplan = value; }
    public int getHorizonDays() { return horizonDays; }
    public void setHorizonDays(int value) { horizonDays = value; }
    public int getSessionMinutes() { return sessionMinutes; }
    public void setSessionMinutes(int value) { sessionMinutes = value; }
    public int getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(int value) { breakMinutes = value; }
    public boolean getHabitAware() { return habitAware; }
    public void setHabitAware(boolean value) { habitAware = value; }
    public int getStudyDays() { return studyDays; }
    public void setStudyDays(int value) { studyDays = value; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String value) { fingerprint = value; }
}
