package ua.studyplanner.task.service;
import org.springframework.stereotype.Service;
import ua.studyplanner.common.time.UserTime;
import ua.studyplanner.session.service.SessionService;
import ua.studyplanner.task.domain.Task;
@Service
public class TaskSessionView {
 private final SessionService sessions;private final UserTime time;
 public TaskSessionView(SessionService sessions,UserTime time){this.sessions=sessions;this.time=time;}
 public Task hydrate(Task task){var manual=sessions.manualPlan(task.getId());var zone=time.zone();task.schedule(manual.map(s->s.startedAt().atZone(zone).toLocalDateTime()).orElse(null),manual.map(s->s.endedAt().atZone(zone).toLocalDateTime()).orElse(null));return task;}
}
