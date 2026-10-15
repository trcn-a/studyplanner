package ua.studyplanner.googlecalendar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Base64;

@Component
public record GoogleCalendarConfig(
 @Value("${google.calendar.client-id:}") String clientId,
 @Value("${google.calendar.client-secret:}") String clientSecret,
 @Value("${google.calendar.encryption-key:}") String encryptionKey,
 @Value("${google.calendar.redirect-uri:http://127.0.0.1:5173/api/google-calendar/callback}") String redirectUri,
 @Value("${google.calendar.frontend-url:http://127.0.0.1:5173/}") String frontendUrl) {
 public boolean configured() {
  try { return !clientId.isBlank() && !clientSecret.isBlank() && Base64.getDecoder().decode(encryptionKey).length==32; }
  catch (IllegalArgumentException e) { return false; }
 }
}
