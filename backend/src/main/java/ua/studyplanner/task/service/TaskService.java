package ua.studyplanner.task.service;

import ua.studyplanner.session.dto.SessionRecord;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ua.studyplanner.common.exception.BusinessRuleException;
import ua.studyplanner.common.exception.ResourceNotFoundException;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.task.domain.Task;
import ua.studyplanner.task.domain.TaskStatus;
import ua.studyplanner.task.dto.TaskInput;
import ua.studyplanner.task.dto.TaskSample;
import ua.studyplanner.task.dto.TaskView;
import ua.studyplanner.task.dto.TimeSummary;
import ua.studyplanner.task.repository.TaskRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {
    private final org.springframework.context.ApplicationEventPublisher events;
    private final TaskRepository tasks;
    private final TaskSessionView sessionView;

    private final SubjectService subjects;
    private final ua.studyplanner.session.service.SessionService sessions;
    private final ua.studyplanner.common.time.UserTime userTime;

    public TaskService(TaskRepository tasks, SubjectService subjects, org.springframework.context.ApplicationEventPublisher events, ua.studyplanner.common.time.UserTime userTime, ua.studyplanner.session.service.SessionService sessions,TaskSessionView sessionView) {
        this.sessionView=sessionView;
        this.sessions=sessions;
        this.userTime=userTime;
        this.events=events;
        this.tasks = tasks;

        this.subjects = subjects;
    }

    @Transactional(readOnly = true)
    public List<TaskView> tasks(UUID subjectId) {
        subjects.requireExists(subjectId);
        List<Task> found = tasks.findBySubjectIdOrderByCreatedAtAsc(subjectId);
        Map<UUID, List<SessionRecord>> entries = entriesByTask(found);
        return found.stream()
                .map(task -> TaskView.of(sessionView.hydrate(task), TimeSummary.of(entries.getOrDefault(task.getId(), List.of()))))
                .toList();
    }

    /** Усі задачі з характеристиками та сумарним фактичним часом — вхідні дані для прогнозування. */
    @Transactional(readOnly = true)
    public List<TaskSample> samples() {
        List<Task> all = tasks.findAllOwned(ua.studyplanner.account.security.CurrentUser.id());
        Map<UUID, List<SessionRecord>> entries = entriesByTask(all);
        Map<UUID, Integer> subtaskCounts = new HashMap<>();
        for (Task task : all) {
            if (task.getParentTaskId() != null) {
                subtaskCounts.merge(task.getParentTaskId(), 1, Integer::sum);
            }
        }
        return all.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE || task.hasPredictionSnapshot())
                .map(task -> TaskSample.of(
                        task,
                        subtaskCounts.getOrDefault(task.getId(), 0),
                        TimeSummary.of(entries.getOrDefault(task.getId(), List.of())).minutes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskSample previewSample(UUID subjectId, UUID taskId, UUID parentId, TaskInput input) {
        subjects.requireExists(subjectId);
        if (taskId != null) {
            Task existing = requireTask(taskId);
            if (!existing.getSubjectId().equals(subjectId))
                throw new BusinessRuleException("Дисципліна завдання не збігається");
            parentId = existing.getParentTaskId();
        } else if (parentId != null) {
            validateParent(subjectId, parentId);
        }
        return new TaskSample(taskId == null ? UUID.randomUUID() : taskId, subjectId,
                parentId != null, 0, input.priority(),
                TaskStatus.TODO, input.estimatedMinutes(), 0, null, null, input.taskType(), input.difficulty());
    }

    public TaskView createTask(UUID subjectId, UUID parentId, TaskInput input) {
        subjects.lockForTaskChange(subjectId);
        validateSchedule(input);
        if (parentId != null) {
            validateParent(subjectId, parentId);
        }
        Task task =
                new Task(
                        subjectId,
                        parentId,
                        input.title(),
                        input.description(),
                        input.status(),
                        input.priority(),
                        input.dueDate(),
                        input.estimatedMinutes(), input.taskType(), input.difficulty());
        task.setPlanningTimeSource(input.planningTimeSource());
        task.schedule(input.scheduledStart(), input.scheduledEnd());
        tasks.saveAndFlush(task);
        sessions.saveManualPlan(task.getId(),task.getStatus()==TaskStatus.DONE,input.scheduledStart(),input.scheduledEnd());
        rewardCompletion(task);
        return view(task);
    }

    public TaskView updateTask(UUID id, TaskInput input) {
        Task task = requireTask(id);
        subjects.lockForTaskChange(task.getSubjectId());
        validateSchedule(input);
        task.setCharacteristics(input.taskType(), input.difficulty());
        task.update(
                input.title(),
                input.description(),
                input.status(),
                input.priority(),
                input.dueDate(),
                input.estimatedMinutes());
        task.setPlanningTimeSource(input.planningTimeSource());
        task.schedule(input.scheduledStart(), input.scheduledEnd());
        sessions.saveManualPlan(task.getId(),task.getStatus()==TaskStatus.DONE,input.scheduledStart(),input.scheduledEnd());
        stopTimerIfDone(task);
        rewardCompletion(task);
        return view(task);
    }

    public TaskView updateStatus(UUID id, TaskStatus status) {
        Task task = requireTask(id);
        subjects.lockForTaskChange(task.getSubjectId());
        task.changeStatus(status);
        stopTimerIfDone(task);
        rewardCompletion(task);
        return view(task);
    }

    public void deleteTask(UUID id) {
        Task task = requireTask(id);
        subjects.lockForTaskChange(task.getSubjectId());
        tasks.deleteAll(tasks.findByParentTaskId(id));
        tasks.flush();
        tasks.delete(task);
    }

    private void rewardCompletion(Task task) {
        if(task.getStatus()==TaskStatus.DONE) events.publishEvent(new ua.studyplanner.task.dto.TaskCompleted(task.getId(),task.getParentTaskId()!=null,Instant.now()));
    }

    private Task requireTask(UUID id) {
        return tasks.findOwned(id,ua.studyplanner.account.security.CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Задачу не знайдено"));
    }

    private TaskView view(Task task) {
        return TaskView.of(sessionView.hydrate(task), TimeSummary.of(sessions.actual(task.getId())));
    }

    private Map<UUID, List<SessionRecord>> entriesByTask(List<Task> found) {
        if (found.isEmpty()) {
            return Map.of();
        }
        return sessions.actual(found.stream().map(Task::getId).toList()).stream()
                .collect(Collectors.groupingBy(SessionRecord::taskId));
    }

    // Виконана задача не може мати активного таймера.
    private void stopTimerIfDone(Task task) {
        if (task.getStatus() != TaskStatus.DONE) {
            return;
        }
        sessions.finishTask(task.getId());
    }

    private void validateParent(UUID subjectId, UUID parentId) {
        Task parent = requireTask(parentId);
        if (!parent.getSubjectId().equals(subjectId)) {
            throw new BusinessRuleException(
                    "Підзадача має належати до предмета батьківської задачі");
        }
        if (parent.getParentTaskId() != null) {
            throw new BusinessRuleException("Підтримується один рівень підзадач");
        }
    }

    private void validateSchedule(TaskInput input) {
        if (!input.isPlanningTimeValid()) throw new BusinessRuleException("Для планування за власною оцінкою вкажіть тривалість");
        if (!input.isScheduleValid()) throw new BusinessRuleException("Вкажіть початок і завершення в межах одного дня");
        userTime.store(input.scheduledStart());
        userTime.store(input.scheduledEnd());
    }
}
