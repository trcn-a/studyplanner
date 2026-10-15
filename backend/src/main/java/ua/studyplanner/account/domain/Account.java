package ua.studyplanner.account.domain;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity
@Table(name="app_users")
public class Account {
    @Id
    @Column(name="id")
    private UUID id;
    
    @Column(name="email")
    private String email;
    
    @Column(name="display_name")
    private String displayName;
    
    @Column(name="password_hash")
    private String passwordHash;
    
    @Column(name="enabled")
    private boolean enabled = true;
    
    @Column(name="created_at")
    private Instant createdAt = Instant.now();
    public Account() {}
    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
