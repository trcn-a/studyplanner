package ua.studyplanner.planning.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.planning.repository.*;
import ua.studyplanner.planning.integration.ICalendarCodec;
import ua.studyplanner.task.service.TaskPlanningQueryService;
import ua.studyplanner.planning.dto.*;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class CalendarExchangeService {
    private final PlanningDataService repository;
    private final CalendarEventDataService events;
    private final PlanningService planning;
    private final TaskPlanningQueryService tasks;
    public CalendarExchangeService(PlanningDataService repository,CalendarEventDataService events,PlanningService planning,TaskPlanningQueryService tasks) {
        this.repository=repository;this.events=events;this.planning=planning;this.tasks=tasks;
    }
    public State importCalendar(String content) {
        planning.lockForChange();
        var parsed=new ICalendarCodec().parse(content,ZoneId.of(planning.settings().zoneId()));
        for(var e:parsed) {
            if(e.uid().endsWith("@studyplanner")) continue;
            if(e.remove()) events.removeByUid(e.uid());
            else events.upsert(e.uid(),e.title(),e.start(),e.end());
        }
        planning.refreshAutomatically();
        return planning.read();
    }
    @Transactional(readOnly=true)
    public String exportCalendar() {
        var state=planning.read();
        var blocks=new ArrayList<>(state.sessions().stream().filter(b->b.source().equals("AUTO")).toList());
        for(var t:tasks.snapshot().tasks()) if(t.scheduledStart()!=null && t.scheduledEnd()!=null) {
            blocks.add(new Block(t.id(),t.id(),t.title(),t.scheduledStart(),t.scheduledEnd(),"MANUAL"));
        }
        return new ICalendarCodec().export(blocks,ZoneId.of(state.settings().zoneId()));
    }
}
