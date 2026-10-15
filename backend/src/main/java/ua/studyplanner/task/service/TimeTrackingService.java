package ua.studyplanner.task.service;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.common.exception.*;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.repository.TaskRepository;
import ua.studyplanner.session.service.SessionService;
import ua.studyplanner.account.security.CurrentUser;

/** Coordinates task state and session commands in one transaction. */
@Service @Transactional
public class TimeTrackingService {
 private final TaskRepository tasks;private final SubjectService subjects;private final SessionService sessions;private final TaskSessionView sessionView;
 public TimeTrackingService(TaskRepository tasks,SubjectService subjects,SessionService sessions,TaskSessionView sessionView){this.tasks=tasks;this.subjects=subjects;this.sessions=sessions;this.sessionView=sessionView;}
 @Transactional(readOnly=true) public List<TimeEntryView> entries(UUID taskId){requireTask(taskId);return sessions.actual(taskId).stream().map(TimeEntryView::of).toList();}
 public TaskView startTimer(UUID taskId){var task=requireTask(taskId);subjects.lockForTaskChange(task.getSubjectId());if(task.getStatus()==TaskStatus.DONE)throw new BusinessRuleException("Таймер не можна запустити для виконаної задачі");sessions.start(taskId,Instant.now());if(task.getStatus()==TaskStatus.TODO)task.changeStatus(TaskStatus.IN_PROGRESS);return view(task);}
 public TaskView stopTimer(UUID taskId){var task=requireTask(taskId);subjects.lockForTaskChange(task.getSubjectId());sessions.stop(taskId,Instant.now());task.predictionDataChanged();return view(task);}
 public TaskView logManual(UUID taskId,ManualTimeInput input){var task=requireTask(taskId);subjects.lockForTaskChange(task.getSubjectId());sessions.manual(taskId,input.date(),input.minutes());task.predictionDataChanged();return view(task);}
 public TaskView deleteEntry(UUID entryId){var taskId=sessions.deleteActual(entryId);var task=requireTask(taskId);task.predictionDataChanged();return view(task);}
 private Task requireTask(UUID id){return tasks.findOwned(id,CurrentUser.id()).orElseThrow(()->new ResourceNotFoundException("Задачу не знайдено"));}
 private TaskView view(Task task){return TaskView.of(sessionView.hydrate(task),TimeSummary.of(sessions.actual(task.getId())));}
}
