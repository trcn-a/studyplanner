package ua.studyplanner.planning.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.planning.domain.CalendarEvent;
import ua.studyplanner.planning.repository.CalendarEventRepository;
import ua.studyplanner.planning.dto.Block;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.common.time.UserTime;
import java.time.LocalDateTime;
import java.util.*;
@Service
@Transactional
public class CalendarEventDataService {
 private final CalendarEventRepository events;
 private final UserTime time;
 public CalendarEventDataService(CalendarEventRepository events,UserTime time){this.events=events;this.time=time;}
 public List<Block> events(){return events.findByUserIdAndSourceOrderById(CurrentUser.id(),"ICALENDAR").stream().map(e->new Block(e.getId(),null,e.getTitle(),time.display(e.getStartsAt()),time.display(e.getEndsAt()),"EXTERNAL")).toList();}
 public void remove(UUID id){events.findByIdAndUserIdAndSource(id,CurrentUser.id(),"ICALENDAR").ifPresent(events::delete);}
 private Optional<CalendarEvent> byUid(String uid){return events.findByUserIdAndSourceAndExternalUid(CurrentUser.id(),"ICALENDAR",uid);}
 public void removeByUid(String uid){byUid(uid).ifPresent(events::delete);}
 public void upsert(String uid,String title,LocalDateTime start,LocalDateTime end){
  var found=byUid(uid);var e=found.orElseGet(CalendarEvent::new);
  if(found.isEmpty()){e.setId(UUID.randomUUID());e.setUserId(CurrentUser.id());e.setSource("ICALENDAR");e.setExternalUid(uid);}
  e.setTitle(title);e.setStartsAt(time.store(start));e.setEndsAt(time.store(end));events.save(e);
 }
}
