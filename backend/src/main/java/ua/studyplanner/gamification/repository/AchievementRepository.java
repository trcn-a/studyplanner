package ua.studyplanner.gamification.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.gamification.domain.*;
import java.util.*;
import java.time.*;
public interface AchievementRepository extends JpaRepository<UserAchievement,AchievementId> {

 @Query("select a.id.code from UserAchievement a where a.id.userId=:user")
 Set<String> findCodes(@Param("user") UUID user);


 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("update UserAchievement e set e.id.userId=:owner where e.id.userId=:legacy")
 int reassignOwner(@Param("owner") UUID owner,@Param("legacy") UUID legacy);

}
