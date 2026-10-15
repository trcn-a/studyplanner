package ua.studyplanner.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.account.service.AccountService;
import ua.studyplanner.subject.service.SubjectService;
import ua.studyplanner.subject.dto.SubjectInput;
import ua.studyplanner.task.service.TaskService;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.dto.*;
import ua.studyplanner.task.service.TimeTrackingService;
import ua.studyplanner.task.dto.ManualTimeInput;
import ua.studyplanner.prediction.service.DurationPredictionService;
import ua.studyplanner.prediction.dto.PredictionPreviewInput;
import ua.studyplanner.planning.service.PlanningService;
import ua.studyplanner.planning.dto.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:planningchoice;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
class PlanningTimeChoiceTest {
    @Autowired AccountService accounts;
    @Autowired SubjectService subjects;
    @Autowired TaskService tasks;
    @Autowired TimeTrackingService timing;
    @Autowired DurationPredictionService predictions;
    @Autowired PlanningService planning;
    @Autowired JdbcTemplate db;
    private void user(Runnable test) {
        var user=accounts.register(UUID.randomUUID()+"@example.com","Student","long-password-123","");
        CurrentUser.runAs(user.id(),test);
    }
    private TaskInput input(int estimate, PlanningTimeSource source) {
        return new TaskInput("Task","description",TaskStatus.TODO,Priority.MEDIUM,null,estimate,null,null,source);
    }
    @Test void characteristicsPersistAndPreviewUsesDraftWithoutChangingSnapshot() { user(()->{
        var subject=subjects.createSubject(new SubjectInput("Study","","#81583d"));
        var input=new TaskInput("Lab","",TaskStatus.TODO,Priority.MEDIUM,null,60,null,null,
                PlanningTimeSource.PREDICTION,TaskType.LABORATORY,Difficulty.HIGH);
        var task=tasks.createTask(subject.id(),null,input);
        assertEquals(TaskType.LABORATORY,task.taskType());
        assertEquals("LABORATORY",db.queryForObject("SELECT initial_task_type FROM tasks WHERE id=?",String.class,task.id()));
        var edited=new TaskInput("Lab","long description",TaskStatus.TODO,Priority.MEDIUM,null,90,null,null,
                PlanningTimeSource.PREDICTION,TaskType.NOTES,Difficulty.LOW);
        var preview=tasks.previewSample(subject.id(),task.id(),null,edited);
        assertEquals(TaskType.NOTES,preview.taskType());
        assertEquals(Difficulty.LOW,preview.difficulty());
        assertEquals(TaskType.LABORATORY,tasks.tasks(subject.id()).getFirst().taskType());
        tasks.updateTask(task.id(),edited);
        var loaded=tasks.tasks(subject.id()).getFirst();
        assertEquals(TaskType.NOTES,loaded.taskType());
        assertEquals(Difficulty.LOW,loaded.difficulty());
        tasks.updateStatus(task.id(),TaskStatus.DONE);
        var sample=tasks.samples().getFirst();
        assertEquals(TaskType.LABORATORY,sample.taskType());
        assertEquals(Difficulty.HIGH,sample.difficulty());
        assertEquals(60,sample.estimatedMinutes());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='TASKS' AND COLUMN_NAME='INITIAL_DESCRIPTION_LENGTH'",Integer.class));
    }); }
    @Test void selectedSourceControlsScheduleAndPreservesTrainingEstimate() { user(()->{
        var subject=subjects.createSubject(new SubjectInput("Study","","#81583d"));
        var history=tasks.createTask(subject.id(),null,input(60,PlanningTimeSource.USER_ESTIMATE));
        timing.logManual(history.id(),new ManualTimeInput(120,LocalDate.now()));
        tasks.updateStatus(history.id(),TaskStatus.DONE);
        int count=db.queryForObject("SELECT COUNT(*) FROM tasks",Integer.class);
        var preview=predictions.preview(new PredictionPreviewInput(subject.id(),null,null,input(30,PlanningTimeSource.PREDICTION)));
        assertEquals(60,preview.minutes());
        assertEquals(count,db.queryForObject("SELECT COUNT(*) FROM tasks",Integer.class));
        var target=tasks.createTask(subject.id(),null,input(30,PlanningTimeSource.USER_ESTIMATE));
        var slots=java.util.stream.IntStream.rangeClosed(1,7).mapToObj(day->new Availability(day,LocalTime.of(8,0),LocalTime.of(22,0))).toList();
        planning.saveSettings(new Settings("Europe/Kyiv",false,7,45,slots,false));
        var own=planning.generate();
        assertEquals(30,own.sessions().stream().filter(s->s.taskId().equals(target.id()) && s.source().equals("AUTO"))
                .mapToLong(s->Duration.between(s.start(),s.end()).toMinutes()).sum());
        var changed=tasks.updateTask(target.id(),input(30,PlanningTimeSource.PREDICTION));
        assertEquals(PlanningTimeSource.PREDICTION,changed.planningTimeSource());
        assertEquals(30,changed.estimatedMinutes());
        var automatic=planning.generate();
        assertEquals(60,automatic.sessions().stream().filter(s->s.taskId().equals(target.id()) && s.source().equals("AUTO"))
                .mapToLong(s->Duration.between(s.start(),s.end()).toMinutes()).sum());
        tasks.updateStatus(target.id(),TaskStatus.DONE);
        assertEquals(30,tasks.samples().stream().filter(s->s.id().equals(target.id())).findFirst().orElseThrow().estimatedMinutes());
    }); }
    @Test void previewUsesDraftFieldsAndHandlesNoHistory() { user(()->{
        var subject=subjects.createSubject(new SubjectInput("Study","","#81583d"));
        assertEquals(45,predictions.preview(new PredictionPreviewInput(subject.id(),null,null,input(45,PlanningTimeSource.PREDICTION))).minutes());
        var noEstimate=new TaskInput("Task","",TaskStatus.TODO,Priority.LOW,null,null,null,null);
        assertNull(predictions.preview(new PredictionPreviewInput(subject.id(),null,null,noEstimate)));
        assertFalse(new TaskInput("Task","",TaskStatus.TODO,Priority.LOW,null,null,null,null,PlanningTimeSource.USER_ESTIMATE).isPlanningTimeValid());
    }); }
    @Test void previewCannotReadAnotherUsersSubjectOrTask() {
        var owner=accounts.register(UUID.randomUUID()+"@example.com","Owner","long-password-123","");
        var stranger=accounts.register(UUID.randomUUID()+"@example.com","Other","long-password-123","");
        var ids=new UUID[2];
        CurrentUser.runAs(owner.id(),()->{
            var subject=subjects.createSubject(new SubjectInput("Private","","#81583d")); ids[0]=subject.id();
            ids[1]=tasks.createTask(subject.id(),null,input(30,PlanningTimeSource.PREDICTION)).id();
        });
        CurrentUser.runAs(stranger.id(),()->{
            assertThrows(ua.studyplanner.common.exception.ResourceNotFoundException.class,()->predictions.preview(new PredictionPreviewInput(ids[0],null,null,input(30,PlanningTimeSource.PREDICTION))));
            var own=subjects.createSubject(new SubjectInput("Own","","#81583d"));
            assertThrows(ua.studyplanner.common.exception.ResourceNotFoundException.class,()->predictions.preview(new PredictionPreviewInput(own.id(),ids[1],null,input(30,PlanningTimeSource.PREDICTION))));
        });
    }
}

