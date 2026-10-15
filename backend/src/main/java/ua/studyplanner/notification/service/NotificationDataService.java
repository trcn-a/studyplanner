package ua.studyplanner.notification.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.studyplanner.account.security.CurrentUser;
import ua.studyplanner.notification.domain.Notification;
import ua.studyplanner.notification.repository.NotificationRepository;
import ua.studyplanner.notification.dto.*;
import java.time.Instant;
import java.util.*;
@Service
@Transactional
public class NotificationDataService {
 private final NotificationRepository notifications;
 public NotificationDataService(NotificationRepository notifications){this.notifications=notifications;}
 public void add(NoticeRequested notice){
  if(notifications.existsByUserIdAndEventKey(CurrentUser.id(),notice.key()))return;
  var n=new Notification();n.setId(UUID.randomUUID());n.setUserId(CurrentUser.id());n.setEventKey(notice.key());n.setKind(notice.kind());n.setTitle(notice.title());n.setBody(notice.body());n.setCreatedAt(Instant.now());notifications.save(n);
 }
 public List<NoticeView> list(){return notifications.findTop100ByUserIdOrderByCreatedAtDesc(CurrentUser.id()).stream().map(n->new NoticeView(n.getId(),n.getKind(),n.getTitle(),n.getBody(),n.getCreatedAt(),n.getReadAt()!=null)).toList();}
 private void markRead(Notification n){if(n.getReadAt()==null){n.setReadAt(Instant.now());notifications.save(n);}}
 public void read(UUID id){notifications.findByIdAndUserId(id,CurrentUser.id()).ifPresent(this::markRead);}
 public void readAll(){notifications.findByUserIdAndReadAtIsNull(CurrentUser.id()).forEach(this::markRead);}
 public void retireReminders(Set<String> relevant){notifications.findByUserIdAndKindInAndReadAtIsNull(CurrentUser.id(),List.of("SESSION","DEADLINE")).stream().filter(n->!relevant.contains(n.getEventKey())).forEach(this::markRead);}
}
