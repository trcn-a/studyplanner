package ua.studyplanner.subject.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import ua.studyplanner.subject.domain.Subject;

import java.util.*;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findAllByOrderByCreatedAtAsc();
    List<Subject> findByUserIdOrderByCreatedAtAsc(UUID userId);
    boolean existsByIdAndUserId(UUID id,UUID userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Subject s where s.id = :id and s.userId = :userId")
    Optional<Subject> findLockedByIdAndUserId(UUID id, UUID userId);
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically=true,clearAutomatically=true)
    @org.springframework.data.jpa.repository.Query("update Subject s set s.userId=:owner where s.userId=:legacy")
    int reassignOwner(@org.springframework.data.repository.query.Param("owner") java.util.UUID owner,
                      @org.springframework.data.repository.query.Param("legacy") java.util.UUID legacy);
}
