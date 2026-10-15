package ua.studyplanner.planning.service;
import ua.studyplanner.session.domain.StudySession;
import ua.studyplanner.session.repository.StudySessionRepository;
import org.junit.jupiter.api.Test;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.domain.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudyHabitServiceTest {
    @Test void manualDateOnlyRecordsDoNotCreateMidnightPreference() {
        var task=new Task(UUID.randomUUID(),null,"Task","",TaskStatus.TODO,Priority.MEDIUM,null,60);
        var entries=new ArrayList<TimeEntryView>();
        for(int i=1;i<=5;i++) entries.add(TimeEntryView.of(ua.studyplanner.session.dto.SessionRecord.of(StudySession.manual(task.getId(),Instant.now().minus(Duration.ofDays(i)),60))));
        var snapshot=new PlanningSnapshot(List.of(TaskView.of(task,TimeSummary.EMPTY)),entries);
        var habits=new StudyHabitService().analyze(snapshot,ZoneId.of("Europe/Kyiv"),Instant.now());
        assertEquals(0,habits.sessions());assertFalse(habits.ready());
    }
}
