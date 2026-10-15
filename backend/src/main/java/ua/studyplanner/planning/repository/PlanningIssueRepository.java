package ua.studyplanner.planning.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.planning.domain.*;
import java.util.*;
import java.time.*;
public interface PlanningIssueRepository extends JpaRepository<PlanningIssue,UUID> {

 @Query("select i from PlanningIssue i where i.taskId in (select t.id from Task t, Subject s where t.subjectId=s.id and s.userId=:user) order by i.title")
 List<PlanningIssue> findOwned(@Param("user") UUID user);


}
