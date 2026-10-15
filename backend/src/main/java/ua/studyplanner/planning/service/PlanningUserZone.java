package ua.studyplanner.planning.service;
import java.time.ZoneId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.common.time.UserZoneProvider;
import ua.studyplanner.planning.repository.PlanningRepository;
import ua.studyplanner.account.security.CurrentUser;
@Component
public class PlanningUserZone implements UserZoneProvider {
 private final PlanningRepository repository;
 public PlanningUserZone(PlanningRepository repository){this.repository=repository;}
 @Transactional(readOnly=true)
 public ZoneId zone(){return ZoneId.of(repository.findZone(CurrentUser.id()));}
}
