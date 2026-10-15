package ua.studyplanner.account.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ua.studyplanner.account.domain.*;
import java.util.*;
import java.time.*;
public interface AccountRepository extends JpaRepository<Account,UUID> {

 boolean existsByEmailIgnoreCase(String email);
 Optional<Account> findByEmailIgnoreCaseAndEnabledTrue(String email);
 List<Account> findByEnabledTrue();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from Account a where a.id=:id")
 Optional<Account> findLocked(@Param("id") UUID id);


}
