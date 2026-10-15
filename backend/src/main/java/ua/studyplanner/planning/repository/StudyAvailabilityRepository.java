package ua.studyplanner.planning.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.planning.domain.*;
import java.util.*;
import java.time.*;
public interface StudyAvailabilityRepository extends JpaRepository<StudyAvailability,UUID> {

 List<StudyAvailability> findByUserIdOrderByDayOfWeekAscStartTimeAsc(UUID userId);
 void deleteByUserId(UUID userId);


 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("update StudyAvailability e set e.userId=:owner where e.userId=:legacy")
 int reassignOwner(@Param("owner") UUID owner,@Param("legacy") UUID legacy);

}
