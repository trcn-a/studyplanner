package ua.studyplanner.googlecalendar.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.googlecalendar.service.GoogleCalendarDataService;
import ua.studyplanner.googlecalendar.domain.GoogleBusyEvent;
import java.util.List;

@Service
public class GoogleCalendarQueryService {
 private final GoogleCalendarDataService repository;
 public GoogleCalendarQueryService(GoogleCalendarDataService repository) {this.repository=repository;}
 @Transactional(readOnly=true)
 public List<GoogleBusyEvent> events() {return repository.events();}
}
