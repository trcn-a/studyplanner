package ua.studyplanner.notification.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.notification.domain.*;
import java.util.*;
import java.time.*;
public interface NotificationRepository extends JpaRepository<Notification,UUID> {

 boolean existsByUserIdAndEventKey(UUID userId,String eventKey);
 List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
 Optional<Notification> findByIdAndUserId(UUID id,UUID userId);
 List<Notification> findByUserIdAndReadAtIsNull(UUID userId);
 List<Notification> findByUserIdAndKindInAndReadAtIsNull(UUID userId,Collection<String> kinds);


}
