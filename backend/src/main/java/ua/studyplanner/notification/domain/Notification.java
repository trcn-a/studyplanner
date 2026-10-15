package ua.studyplanner.notification.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="notifications")
public class Notification {
    @Id
    @Column(name="id")
    private UUID id;
    
    @Column(name="user_id")
    private UUID userId;
    
    @Column(name="event_key")
    private String eventKey;
    
    @Column(name="kind")
    private String kind;
    
    @Column(name="title")
    private String title;
    
    @Column(name="body")
    private String body;
    
    @Column(name="created_at")
    private Instant createdAt;
    
    @Column(name="read_at")
    private Instant readAt;
    public Notification() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String value) { eventKey = value; }
    public String getKind() { return kind; }
    public void setKind(String value) { kind = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getBody() { return body; }
    public void setBody(String value) { body = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant value) { readAt = value; }
}
