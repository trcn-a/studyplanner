package ua.studyplanner.gamification.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.gamification.domain.*;
import java.util.*;
import java.time.*;
public interface RewardRepository extends JpaRepository<CompletionReward,UUID> {

 boolean existsByTaskId(UUID taskId);
 List<CompletionReward> findByUserId(UUID userId);


 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("update CompletionReward e set e.userId=:owner where e.userId=:legacy")
 int reassignOwner(@Param("owner") UUID owner,@Param("legacy") UUID legacy);

}
