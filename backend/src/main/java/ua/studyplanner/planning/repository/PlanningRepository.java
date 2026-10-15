package ua.studyplanner.planning.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.planning.domain.*;
import java.util.*;
import java.time.*;
public interface PlanningRepository extends JpaRepository<PlanningSettings,UUID> {

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from PlanningSettings p where p.userId=:user")
 Optional<PlanningSettings> findLocked(@Param("user") UUID user);
 @Query("select p.zoneId from PlanningSettings p where p.userId=:user")
 String findZone(@Param("user") UUID user);


 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("update PlanningSettings e set e.userId=:owner where e.userId=:legacy")
 int reassignOwner(@Param("owner") UUID owner,@Param("legacy") UUID legacy);

}
