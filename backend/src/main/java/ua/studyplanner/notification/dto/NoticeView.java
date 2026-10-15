package ua.studyplanner.notification.dto;
import java.time.Instant;
import java.util.UUID;
public record NoticeView(UUID id,String kind,String title,String body,Instant createdAt,boolean read) {}
