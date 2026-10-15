package ua.studyplanner.notification.domain;
import ua.studyplanner.notification.dto.NoticeRequested;
import ua.studyplanner.task.dto.TaskView;
import ua.studyplanner.task.domain.TaskStatus;
import ua.studyplanner.planning.dto.Block;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
public final class ReminderRules {
 private ReminderRules(){}
 public static List<NoticeRequested> pending(List<TaskView> tasks,List<Block> sessions,LocalDateTime now,int lead){
  var notices=new LinkedHashMap<String,NoticeRequested>();var pending=new HashMap<UUID,TaskView>();
  for(var task:tasks)if(task.status()!=TaskStatus.DONE){
   pending.put(task.id(),task);
   if(task.dueDate()!=null){
    var deadline=task.dueDate().plusDays(1).atStartOfDay();
    if(!deadline.isAfter(now.plusHours(24))){String key="due:"+task.id()+":"+task.dueDate();
     notices.put(key,new NoticeRequested(key,"DEADLINE",deadline.isAfter(now)?"Наближається дедлайн":"Дедлайн минув",shortTitle(task.title())+" · до "+task.dueDate()));}
   }
  }
  var blocks=new ArrayList<>(sessions);
  for(var task:pending.values())if(task.scheduledStart()!=null&&task.scheduledEnd()!=null)blocks.add(new Block(task.id(),task.id(),task.title(),task.scheduledStart(),task.scheduledEnd(),"MANUAL"));
  for(var block:blocks){var task=pending.get(block.taskId());if(task==null)continue;
   if(!block.start().isAfter(now.plusMinutes(lead)) && block.end().isAfter(now)){
    String key="session:"+task.id()+":"+block.start();
    notices.put(key,new NoticeRequested(key,"SESSION",block.start().isAfter(now)?"Незабаром заняття":"Заплановане заняття триває",shortTitle(task.title())+" · "+block.start().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))));
   }
  }
  return List.copyOf(notices.values());
 }
 private static String shortTitle(String title){return title.substring(0,Math.min(200,title.length()));}
}
