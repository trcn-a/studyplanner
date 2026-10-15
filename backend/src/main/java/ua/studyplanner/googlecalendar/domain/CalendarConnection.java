package ua.studyplanner.googlecalendar.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="google_calendar_connection")
public class CalendarConnection {
    @Id
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="refresh_token")
    private String refreshToken;
    
    @Column(name="revision")
    private UUID revision;
    
    @Column(name="last_sync")
    private Instant lastSync;
    
    @Column(name="error")
    private String error;
    public CalendarConnection() {}
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String value) { refreshToken = value; }
    public UUID getRevision() { return revision; }
    public void setRevision(UUID value) { revision = value; }
    public Instant getLastSync() { return lastSync; }
    public void setLastSync(Instant value) { lastSync = value; }
    public String getError() { return error; }
    public void setError(String value) { error = value; }
}
