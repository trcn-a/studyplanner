package ua.studyplanner.account.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.account.domain.*;
import java.util.*;
import java.time.*;
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings,UUID> {


}
