package ua.studyplanner.planning.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="calendar_events")
public class CalendarEvent {
    @Id
    @Column(name="id")
    private UUID id;
    
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="source")
    private String source;
    
    @Column(name="external_uid")
    private String externalUid;
    
    @Column(name="title")
    private String title;
    
    @Column(name="starts_at")
    private OffsetDateTime startsAt;
    
    @Column(name="ends_at")
    private OffsetDateTime endsAt;
    public CalendarEvent() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public String getExternalUid() { return externalUid; }
    public void setExternalUid(String value) { externalUid = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime value) { startsAt = value; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime value) { endsAt = value; }
}
