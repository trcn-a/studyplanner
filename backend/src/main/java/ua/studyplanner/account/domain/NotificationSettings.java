package ua.studyplanner.account.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="notification_settings")
public class NotificationSettings {
    @Id
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="reminders_enabled")
    private boolean remindersEnabled = true;
    
    @Column(name="reminder_minutes")
    private int reminderMinutes = 15;
    public NotificationSettings() {}
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public boolean getRemindersEnabled() { return remindersEnabled; }
    public void setRemindersEnabled(boolean value) { remindersEnabled = value; }
    public int getReminderMinutes() { return reminderMinutes; }
    public void setReminderMinutes(int value) { reminderMinutes = value; }
}
