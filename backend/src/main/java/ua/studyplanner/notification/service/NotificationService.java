package ua.studyplanner.notification.service;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.notification.service.NotificationDataService;
import ua.studyplanner.notification.dto.*;
import java.util.*;
@Service
public class NotificationService {
 private final NotificationDataService repository;
 private final ua.studyplanner.account.service.AccountAccess accounts;
 public NotificationService(NotificationDataService repository,ua.studyplanner.account.service.AccountAccess accounts){this.accounts=accounts;this.repository=repository;}
 @EventListener @Transactional public void receive(NoticeRequested notice){accounts.lockCurrent();repository.add(notice);}
 @Transactional(readOnly=true) public List<NoticeView> list(){return repository.list();}
 @Transactional public void read(UUID id){repository.read(id);}
 @Transactional public void readAll(){repository.readAll();}
}
