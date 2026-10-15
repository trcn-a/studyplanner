package ua.studyplanner.googlecalendar.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import ua.studyplanner.googlecalendar.config.GoogleCalendarConfig;
import ua.studyplanner.googlecalendar.integration.*;
import ua.studyplanner.googlecalendar.service.GoogleCalendarDataService;
import ua.studyplanner.common.exception.BusinessRuleException;
import java.time.*;

@Service
public class GoogleCalendarService {
 public record Status(boolean configured,boolean connected,Instant lastSync,String error) {}
 private final GoogleCalendarConfig config;
 private final GoogleCalendarClient client;
 private final TokenCipher cipher;
 private final GoogleCalendarDataService repository;
 private final GoogleCalendarStore store;
 public GoogleCalendarService(GoogleCalendarConfig config,GoogleCalendarClient client,TokenCipher cipher,GoogleCalendarDataService repository,GoogleCalendarStore store) {
  this.config=config;this.client=client;this.cipher=cipher;this.repository=repository;this.store=store;
 }
 public Status status() {var c=repository.connection();return new Status(config.configured(),c.isPresent(),c.map(x->x.lastSync()).orElse(null),c.map(x->x.error()).orElse(null));}
 public void connect(String code,String verifier) {
  var tokens=client.exchange(code,verifier);String refresh=(String)tokens.get("refresh_token");
  if(refresh==null || refresh.isBlank()) throw new BusinessRuleException("Google не надав фоновий доступ. Підключіть календар повторно.");
  String access=(String)tokens.get("access_token");
  var events=client.events(access,Instant.now().minus(Duration.ofDays(1)),Instant.now().plus(Duration.ofDays(92)));
  store.connect(cipher.encrypt(refresh),events);
 }
 public void disconnect() {store.disconnect();}

 public synchronized void sync() {
  if(!config.configured()) return;
  var connection=repository.connection();if(connection.isEmpty())return;
  var c=connection.get();
  try {
   String access=client.accessToken(cipher.decrypt(c.token()));
   Instant from=Instant.now().minus(Duration.ofDays(1)),to=Instant.now().plus(Duration.ofDays(92));
   var events=client.events(access,from,to);store.replace(c.revision(),events);
  } catch(Exception e) {
   // Never persist provider responses: they can include sensitive information.
   repository.error(c.revision(),"Не вдалося оновити Google Calendar. Попередні події збережено. Перевірте з’єднання або підключіть календар повторно.");
  }
 }
}
