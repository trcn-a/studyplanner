package ua.studyplanner.subject.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id private UUID id;
    @Column(nullable=false) private UUID userId=ua.studyplanner.account.security.CurrentUser.LEGACY;
    public UUID getUserId(){return userId;}
    public void assignUser(UUID userId){this.userId=userId;}


    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 7)
    private String color;


    private Instant createdAt;
    private Instant updatedAt;

    protected Subject() {}

    public Subject(String name, String description, String color) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.name = name.strip();
        this.description = description.strip();
        this.color = color;
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String description, String color) {
        this.name = name.strip();
        this.description = description.strip();
        this.color = color;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
