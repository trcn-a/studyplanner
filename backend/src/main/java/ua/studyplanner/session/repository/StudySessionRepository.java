package ua.studyplanner.session.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ua.studyplanner.session.domain.StudySession;
import java.util.*;

public interface StudySessionRepository extends JpaRepository<StudySession,UUID> {
    @Query("select s from StudySession s where s.taskId in (select t.id from Task t where t.subjectId in (select d.id from Subject d where d.userId=:userId)) and s.status=:status order by s.startedAt asc")
    List<StudySession> findByUserIdAndStatusOrderByStartedAtAsc(UUID userId,ua.studyplanner.session.domain.SessionStatus status);
    @Query("select s from StudySession s where s.taskId in (select t.id from Task t where t.subjectId in (select d.id from Subject d where d.userId=:userId)) and s.status in :status order by s.startedAt asc")
    List<StudySession> findByUserIdAndStatusInOrderByStartedAtAsc(UUID userId,Collection<ua.studyplanner.session.domain.SessionStatus> status);
    @Query("select s from StudySession s where s.taskId=:taskId and s.status in (ua.studyplanner.session.domain.SessionStatus.RUNNING,ua.studyplanner.session.domain.SessionStatus.COMPLETED) order by s.startedAt desc")
    List<StudySession> findByTaskIdOrderByStartedAtDesc(@Param("taskId") UUID taskId);
    @Query("select s from StudySession s where s.taskId in :taskIds and s.status in (ua.studyplanner.session.domain.SessionStatus.RUNNING,ua.studyplanner.session.domain.SessionStatus.COMPLETED)")
    List<StudySession> findByTaskIdIn(@Param("taskIds") Collection<UUID> taskIds);
    @Query("select s from StudySession s where s.status=ua.studyplanner.session.domain.SessionStatus.RUNNING")
    List<StudySession> findByEndedAtIsNull();
    @Query("select s from StudySession s where s.taskId=:taskId and s.taskId in (select t.id from Task t where t.subjectId in (select d.id from Subject d where d.userId=:userId)) and s.status=ua.studyplanner.session.domain.SessionStatus.PLANNED order by s.startedAt,s.id")
    List<StudySession> planned(@Param("taskId") UUID taskId,@Param("userId") UUID userId);
    @Query("select s from StudySession s where s.taskId in (select t.id from Task t where t.subjectId in (select d.id from Subject d where d.userId=:userId)) order by s.startedAt asc")
    List<StudySession> findByUserIdOrderByStartedAtAsc(UUID userId);
    @Query("select (count(s)>0) from StudySession s where s.taskId in (select t.id from Task t where t.subjectId in (select d.id from Subject d where d.userId=:userId)) and s.status=:status")
    boolean existsByUserIdAndStatus(UUID userId,ua.studyplanner.session.domain.SessionStatus status);
    @Query("select (count(t)>0) from Task t where t.id=:taskId and t.subjectId in (select d.id from Subject d where d.userId=:userId)")
    boolean ownsTask(UUID taskId,UUID userId);
}
