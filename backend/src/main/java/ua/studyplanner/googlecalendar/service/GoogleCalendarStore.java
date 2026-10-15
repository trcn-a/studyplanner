package ua.studyplanner.googlecalendar.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.googlecalendar.service.GoogleCalendarDataService;
import ua.studyplanner.googlecalendar.domain.GoogleBusyEvent;

import ua.studyplanner.planning.service.PlanningService;
import java.util.*;

@Service
@Transactional
public class GoogleCalendarStore {
 private final GoogleCalendarDataService repository;

 private final PlanningService planning;
 public GoogleCalendarStore(GoogleCalendarDataService repository,PlanningService planning) {
  this.repository=repository;this.planning=planning;
 }
 public void connect(String token,List<GoogleBusyEvent> events) {planning.lockForChange();repository.connect(token);repository.replace(events);planning.refreshAutomatically();}
 public void disconnect() {planning.lockForChange();repository.disconnect();planning.refreshAutomatically();}
 public void replace(UUID revision,List<GoogleBusyEvent> events) {
  planning.lockForChange();
  if(repository.connection().filter(c->c.revision().equals(revision)).isEmpty()) return;
  repository.replace(events);planning.refreshAutomatically();
 }
}
