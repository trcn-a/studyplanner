package ua.studyplanner.planning.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.planning.dto.*;
import ua.studyplanner.planning.domain.*;
import ua.studyplanner.planning.repository.*;
import ua.studyplanner.prediction.service.DurationPredictionService;
import ua.studyplanner.task.service.TaskPlanningQueryService;
import ua.studyplanner.task.dto.TaskView;
import ua.studyplanner.task.domain.TaskStatus;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlanningService {
    private final PlanningDataService repository;
    private final PlanningSessions sessions;
    private final ua.studyplanner.account.service.AccountAccess accounts;
    private final CalendarEventDataService calendars;
    private final TaskPlanningQueryService tasks;
    private final DurationPredictionService predictions;
    private final StudyHabitService habits;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final ua.studyplanner.googlecalendar.service.GoogleCalendarQueryService google;

    public PlanningService(PlanningDataService repository,CalendarEventDataService calendars,
            TaskPlanningQueryService tasks,DurationPredictionService predictions,StudyHabitService habits,
            ua.studyplanner.googlecalendar.service.GoogleCalendarQueryService google,org.springframework.context.ApplicationEventPublisher events,PlanningSessions sessions,ua.studyplanner.account.service.AccountAccess accounts) {
        this.sessions=sessions;this.accounts=accounts;
        this.events=events;
        this.google=google;
        this.repository=repository;this.calendars=calendars;this.tasks=tasks;this.predictions=predictions;this.habits=habits;
    }

    @Transactional(readOnly=true)
    public Settings settings() { return repository.settings(); }

    public State saveSettings(Settings input) {
        PlanningSettingsValidator.validate(input);
        lockForChange();
        repository.saveSettings(input);
        refresh(false);
        return read();
    }

    public void lockForChange(){accounts.lockCurrent();repository.lock();}

    public State generate() { lockForChange();refresh(true);return read(); }
    public void refreshAutomatically() { lockForChange();refresh(false); }

    @Transactional(readOnly=true)
    public State read() {
        var settings=repository.settings();
        var snapshot=tasks.snapshot();
        var titles=snapshot.tasks().stream().collect(Collectors.toMap(TaskView::id,TaskView::title));
        var displayed=new ArrayList<>(sessions.planned());displayed.addAll(sessions.recorded());
        var sessions=displayed.stream().map(b -> new Block(b.id(),b.taskId(),
                titles.getOrDefault(b.taskId(),""),b.start(),b.end(),b.source())).toList();
        var preferences=habits.analyze(snapshot,ZoneId.of(settings.zoneId()),Instant.now());
        return new State(settings,sessions,externalEvents(settings),repository.issues(),habits.summary(preferences));
    }

    public State removeEvent(UUID id) {
        lockForChange();calendars.remove(id);refresh(false);return read();
    }

    public State moveSession(UUID id,LocalDateTime start,boolean manualTask) {
        lockForChange();
        var original=sessions.requirePlanned(id,manualTask);
        var zone=ZoneId.of(settings().zoneId());
        var oldStart=original.startedAt().atZone(zone).toLocalDateTime();
        var end=start.plus(Duration.between(oldStart,original.endedAt().atZone(zone).toLocalDateTime()));
        if(start.isBefore(LocalDateTime.now(zone)) || !start.toLocalDate().equals(end.minusNanos(1).toLocalDate()))
            throw new ua.studyplanner.common.exception.BusinessRuleException("Оберіть майбутній час у межах одного дня");
        var state=read();
        var snapshot=tasks.snapshot();
        var task=snapshot.tasks().stream().filter(t->t.id().equals(original.taskId())).findFirst().orElseThrow(()->new ua.studyplanner.common.exception.ResourceNotFoundException("Завдання не знайдено"));
        if(!pending(task))throw new ua.studyplanner.common.exception.BusinessRuleException("Завершене завдання не можна перепланувати");
        if(task.dueDate()!=null && end.isAfter(task.dueDate().plusDays(1).atStartOfDay()))throw new ua.studyplanner.common.exception.BusinessRuleException("Заняття має завершитися до дедлайну");
        var occupied=new ArrayList<>(state.sessions());occupied.addAll(state.externalEvents());
        for(var t:snapshot.tasks())if(t.scheduledStart()!=null&&t.scheduledEnd()!=null)occupied.add(new Block(t.id(),t.id(),t.title(),t.scheduledStart(),t.scheduledEnd(),"MANUAL"));
        if(occupied.stream().filter(b->!b.id().equals(original.id()) && !(manualTask && b.id().equals(original.taskId()))).anyMatch(b->start.isBefore(b.end())&&end.isAfter(b.start())))
            throw new ua.studyplanner.common.exception.BusinessRuleException("Цей час уже зайнятий іншим заняттям або подією");
        sessions.move(original.id(),start,end);
        return read();
    }

    private void refresh(boolean force) {
        var settings=repository.settings();
        if(!force && !settings.autoReplan()) return;
        var snapshot=tasks.snapshot();
        var all=snapshot.tasks();
        var byId=all.stream().collect(Collectors.toMap(TaskView::id,t -> t));
        Instant instant=Instant.now();
        LocalDateTime now=LocalDateTime.ofInstant(instant,ZoneId.of(settings.zoneId())).withSecond(0).withNano(0).plusMinutes(1);
        var external=externalEvents(settings);
        String signature=PlanningSignature.of(settings,snapshot,external,now.toLocalDate());
        var previous=sessions.planned();
        boolean missed=previous.stream().anyMatch(b -> !b.end().isAfter(now) && pending(byId.get(b.taskId())));
        if(!force && signature.equals(repository.fingerprint()) && !missed) return;

        var occupied=new ArrayList<>(external);
        occupied.addAll(sessions.recorded());
        var fixed=new ArrayList<Block>();
        for(var task:all) if(task.scheduledStart()!=null && task.scheduledEnd()!=null) {
            var block=new Block(task.id(),task.id(),task.title(),task.scheduledStart(),task.scheduledEnd(),"MANUAL");
            occupied.add(block);
            if(block.end().isAfter(now) && pending(task)) fixed.add(block);
        }
        for(var block:previous) {
            if(pending(byId.get(block.taskId())) && block.end().isAfter(now) && (block.source().equals("PINNED") || !block.start().isAfter(now))) {
                fixed.add(block);occupied.add(block);
            }
        }

        var estimates=predictions.predictions().stream().collect(Collectors.toMap(p -> p.taskId(),p -> p.minutes()));
        var parents=all.stream().map(TaskView::parentTaskId).filter(Objects::nonNull).collect(Collectors.toSet());
        var work=new ArrayList<PlanningEngine.Work>();
        var issues=new ArrayList<Issue>();
        for(var task:all) {
            if(!pending(task) || parents.contains(task.id()) || task.timerStartedAt()!=null) continue;
            Integer total=task.planningMinutes(estimates.get(task.id()));
            if(total==null) {
                issues.add(new Issue(task.id(),task.title(),"Задайте тривалість: для прогнозу ще недостатньо даних",0));continue;
            }
            int spent=task.actualMinutes();
            if(task.timerStartedAt()!=null) spent+=(int)Math.max(0,Duration.between(task.timerStartedAt(),instant).toMinutes());
            int remaining=total-spent;
            if(remaining<=0) {
                issues.add(new Issue(task.id(),task.title(),"Оцінений час вичерпано. Уточніть тривалість або завершіть завдання",0));continue;
            }
            for(var b:fixed) if(task.id().equals(b.taskId())) {
                remaining-=(int)Duration.between(b.start().isBefore(now)?now:b.start(),b.end()).toMinutes();
            }
            if(remaining>0) work.add(new PlanningEngine.Work(task.id(),task.subjectId(),task.title(),task.dueDate(),task.priority().ordinal(),remaining));
        }
        var preferences=habits.analyze(snapshot,ZoneId.of(settings.zoneId()),instant);
        var result=new PlanningEngine().plan(now,settings,work,occupied,preferences);
        issues.addAll(result.issues());
        var retained=fixed.stream().map(Block::id).collect(Collectors.toSet());
        for(var block:previous) {
            if((pending(byId.get(block.taskId())) && !retained.contains(block.id()))
                    || (!pending(byId.get(block.taskId())) && block.start().isAfter(now))) sessions.remove(block.id());
        }
        sessions.add(result.sessions());
        repository.replaceIssues(issues);
        repository.saveFingerprint(signature);
        var after=sessions.planned();
        if(!previous.isEmpty() && !scheduleKeys(previous).equals(scheduleKeys(after)))
            events.publishEvent(new ua.studyplanner.notification.dto.NoticeRequested("plan:"+UUID.randomUUID(),"PLAN","Навчальний план оновлено","Перевір розташування занять у календарі."));
    }

    private List<Block> externalEvents(Settings settings) {
        var blocks=new ArrayList<>(calendars.events());
        var zone=ZoneId.of(settings.zoneId());
        for(var event:google.events()) blocks.add(new Block(event.id(),null,event.title(),
                event.start().atZone(zone).toLocalDateTime(),event.end().atZone(zone).toLocalDateTime(),"GOOGLE"));
        return blocks;
    }

    private Set<String> scheduleKeys(List<Block> blocks) {
        return blocks.stream().map(b->b.taskId()+":"+b.start()+":"+b.end()).collect(Collectors.toSet());
    }

    private boolean pending(TaskView task) { return task!=null && task.status()!=TaskStatus.DONE; }
}
