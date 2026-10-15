package ua.studyplanner.planning.service;
import org.springframework.stereotype.Service;
import ua.studyplanner.planning.domain.ActivityPreferences;
import ua.studyplanner.planning.dto.HabitSummary;
import ua.studyplanner.task.dto.PlanningSnapshot;
import ua.studyplanner.session.domain.SessionSource;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudyHabitService {
    public ActivityPreferences analyze(PlanningSnapshot snapshot, ZoneId zone, Instant now) {
        var subjects = snapshot.tasks().stream().collect(Collectors.toMap(t -> t.id(),t -> t.subjectId()));
        var history = snapshot.timeEntries().stream()
                // Manual records store only the date, not the actual hour of work.
                .filter(e -> e.source()==SessionSource.TIMER && e.endedAt()!=null && subjects.containsKey(e.taskId()))
                .map(e -> new ActivityPreferences.Observation(e.taskId(),subjects.get(e.taskId()),e.startedAt(),e.endedAt())).toList();
        return ActivityPreferences.from(history,zone,now);
    }
    public HabitSummary summary(ActivityPreferences preferences) {
        return new HabitSummary(preferences.sessions(),preferences.activeDays(),preferences.subjectProfiles(),preferences.ready());
    }
}
