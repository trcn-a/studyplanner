package ua.studyplanner.task.service;

import ua.studyplanner.session.dto.SessionRecord;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.repository.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TaskPlanningQueryService {
    private final TaskRepository tasks;
    private final TaskSessionView sessionView;
    private final ua.studyplanner.session.service.SessionService sessions;


    public TaskPlanningQueryService(TaskRepository tasks, ua.studyplanner.session.service.SessionService sessions,TaskSessionView sessionView) {
        this.sessionView=sessionView;
        this.sessions=sessions;
        this.tasks = tasks;

    }

    public PlanningSnapshot snapshot() {
        var all = tasks.findAllOwned(ua.studyplanner.account.security.CurrentUser.id());
        var times = all.isEmpty() ? List.<SessionRecord>of() : sessions.actual(all.stream().map(t->t.getId()).toList());
        var grouped = times.stream().collect(Collectors.groupingBy(SessionRecord::taskId));
        var views = all.stream().map(t -> TaskView.of(sessionView.hydrate(t),
                TimeSummary.of(grouped.getOrDefault(t.getId(), List.of())))).toList();
        return new PlanningSnapshot(views, times.stream().map(TimeEntryView::of).toList());
    }
}
