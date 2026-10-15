package ua.studyplanner.googlecalendar.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.googlecalendar.domain.*;
import ua.studyplanner.googlecalendar.repository.GoogleCalendarRepository;
import ua.studyplanner.planning.domain.CalendarEvent;
import ua.studyplanner.planning.repository.CalendarEventRepository;
import ua.studyplanner.account.security.CurrentUser;
import java.time.*;
import java.util.*;
@Service
@Transactional
public class GoogleCalendarDataService {
 public record Connection(String token,UUID revision,Instant lastSync,String error) {}
 private final GoogleCalendarRepository connections;
 private final CalendarEventRepository events;
 public GoogleCalendarDataService(GoogleCalendarRepository connections,CalendarEventRepository events){this.connections=connections;this.events=events;}
 public List<GoogleBusyEvent> events(){return events.findByUserIdAndSourceOrderByExternalUid(CurrentUser.id(),"GOOGLE").stream().map(e->new GoogleBusyEvent(UUID.fromString(e.getExternalUid()),e.getTitle(),e.getStartsAt().toInstant(),e.getEndsAt().toInstant())).toList();}
 public Optional<Connection> connection(){return connections.findById(CurrentUser.id()).map(c->new Connection(c.getRefreshToken(),c.getRevision(),c.getLastSync(),c.getError()));}
 public void connect(String token){disconnect();connections.flush();var c=new CalendarConnection();c.setUserId(CurrentUser.id());c.setRefreshToken(token);c.setRevision(UUID.randomUUID());connections.save(c);}
 private void removeEvents(){events.deleteByUserIdAndSource(CurrentUser.id(),"GOOGLE");events.flush();}
 public void disconnect(){removeEvents();connections.findById(CurrentUser.id()).ifPresent(connections::delete);}
 public void replace(List<GoogleBusyEvent> inputEvents){
  removeEvents();for(var input:inputEvents){var e=new CalendarEvent();e.setId(UUID.randomUUID());e.setUserId(CurrentUser.id());e.setSource("GOOGLE");e.setExternalUid(input.id().toString());e.setTitle(input.title());e.setStartsAt(input.start().atOffset(ZoneOffset.UTC));e.setEndsAt(input.end().atOffset(ZoneOffset.UTC));events.save(e);}
  connections.findById(CurrentUser.id()).ifPresent(c->{c.setLastSync(Instant.now());c.setError(null);connections.save(c);});
 }
 public void error(UUID revision,String message){connections.findById(CurrentUser.id()).filter(c->c.getRevision().equals(revision)).ifPresent(c->{c.setError(message);connections.save(c);});}
}
