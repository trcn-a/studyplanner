package ua.studyplanner.notification.controllers;
import org.springframework.web.bind.annotation.*;
import ua.studyplanner.notification.service.*;
import ua.studyplanner.notification.dto.NoticeView;
import java.util.*;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
 private final NotificationService notifications;private final ReminderService reminders;
 public NotificationController(NotificationService notifications,ReminderService reminders){this.notifications=notifications;this.reminders=reminders;}
 @GetMapping public List<NoticeView> list(){return notifications.list();}
 @PostMapping("/refresh") public List<NoticeView> refresh(){reminders.generate();return notifications.list();}
 @PostMapping("/{id}/read") public List<NoticeView> read(@PathVariable UUID id){notifications.read(id);return notifications.list();}
 @PostMapping("/read-all") public List<NoticeView> readAll(){notifications.readAll();return notifications.list();}
}
