package ua.studyplanner.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ua.studyplanner.task.domain.Task;

import java.util.*;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @org.springframework.data.jpa.repository.Query("select t from Task t where t.subjectId in (select s.id from Subject s where s.userId=:userId)")
    List<Task> findAllOwned(UUID userId);
    @org.springframework.data.jpa.repository.Query("select t from Task t where t.id=:id and t.subjectId in (select s.id from Subject s where s.userId=:userId)")
    Optional<Task> findOwned(UUID id,UUID userId);

    List<Task> findBySubjectIdOrderByCreatedAtAsc(UUID subjectId);

    List<Task> findByParentTaskId(UUID parentTaskId);
}
