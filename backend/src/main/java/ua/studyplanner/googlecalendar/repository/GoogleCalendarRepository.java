package ua.studyplanner.googlecalendar.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.googlecalendar.domain.*;
import java.util.*;
import java.time.*;
public interface GoogleCalendarRepository extends JpaRepository<CalendarConnection,UUID> {


 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("update CalendarConnection e set e.userId=:owner where e.userId=:legacy")
 int reassignOwner(@Param("owner") UUID owner,@Param("legacy") UUID legacy);

}
