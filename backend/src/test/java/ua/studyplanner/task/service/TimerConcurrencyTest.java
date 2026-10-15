package ua.studyplanner.task.service;
import ua.studyplanner.session.domain.StudySession;
import ua.studyplanner.session.repository.StudySessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.studyplanner.subject.domain.Subject;
import ua.studyplanner.subject.repository.SubjectRepository;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.task.repository.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties = {
 "spring.datasource.url=jdbc:h2:mem:timercheck;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
 "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver"
})
class TimerConcurrencyTest extends ua.studyplanner.support.LegacyUserTest {
 @Autowired SubjectRepository subjects;
 @Autowired TaskRepository tasks;
 @Autowired StudySessionRepository entries;
 @Autowired TimeTrackingService timers;
 @Test void differentSubjectsCannotRunTimersConcurrently() throws Exception {
   var a = subjects.save(new Subject("A", "", "#81583d"));
   var b = subjects.save(new Subject("B", "", "#81583d"));
   var ta = tasks.save(new Task(a.getId(), null, "A", "", TaskStatus.TODO, Priority.MEDIUM, null, 30));
   var tb = tasks.save(new Task(b.getId(), null, "B", "", TaskStatus.TODO, Priority.MEDIUM, null, 30));
   var gate = new CountDownLatch(1);
   try (var pool = Executors.newFixedThreadPool(2)) {
     var results = new java.util.ArrayList<Future<Boolean>>();
     for (var task : java.util.List.of(ta, tb)) results.add(pool.submit(() -> { authenticate();
       gate.await();
       try { timers.startTimer(task.getId()); return true; }
       catch (ua.studyplanner.common.exception.BusinessRuleException expected) { return false; }
     }));
     gate.countDown();
     int started = 0;
     for (var result : results) if (result.get(15, TimeUnit.SECONDS)) started++;
     assertEquals(1, started);
     assertEquals(1, entries.findByEndedAtIsNull().size());
   }
 }
}
