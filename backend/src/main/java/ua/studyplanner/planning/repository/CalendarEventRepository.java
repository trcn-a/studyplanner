package ua.studyplanner.planning.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.planning.domain.*;
import java.util.*;
import java.time.*;
public interface CalendarEventRepository extends JpaRepository<CalendarEvent,UUID> {

 List<CalendarEvent> findByUserIdAndSourceOrderById(UUID userId,String source);
 List<CalendarEvent> findByUserIdAndSourceOrderByExternalUid(UUID userId,String source);
 Optional<CalendarEvent> findByUserIdAndSourceAndExternalUid(UUID userId,String source,String uid);
 Optional<CalendarEvent> findByIdAndUserIdAndSource(UUID id,UUID userId,String source);
 void deleteByUserIdAndSource(UUID userId,String source);


 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("update CalendarEvent e set e.userId=:owner where e.userId=:legacy")
 int reassignOwner(@Param("owner") UUID owner,@Param("legacy") UUID legacy);

}
