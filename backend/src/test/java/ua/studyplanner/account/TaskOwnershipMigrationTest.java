package ua.studyplanner.account;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TaskOwnershipMigrationTest {
 @Test void upgradeRemovesDuplicateOwnerAndPreservesRelationsAndHistory() {
  var source=new SingleConnectionDataSource("jdbc:h2:mem:taskowner;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("14").load().migrate();
   var db=new JdbcTemplate(source);
   var user=UUID.fromString("00000000-0000-0000-0000-000000000001");
   var subject=UUID.randomUUID();var otherSubject=UUID.randomUUID();var parent=UUID.randomUUID();var child=UUID.randomUUID();var session=UUID.randomUUID();
   for(var id:new UUID[]{subject,otherSubject})db.update("INSERT INTO subjects(id,user_id,name,description,color,created_at,updated_at) VALUES(?,?,'Study','','#81583d',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",id,user);
   db.update("INSERT INTO tasks(id,user_id,subject_id,title,description,status,priority,initial_estimated_minutes,initial_description_length,initial_priority,prediction_snapshot,created_at,updated_at) VALUES(?,?,?,'Parent','','TODO','HIGH',60,0,'HIGH',TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",parent,user,subject);
   db.update("INSERT INTO tasks(id,user_id,subject_id,parent_task_id,title,description,status,priority,created_at,updated_at) VALUES(?,?,?,?,'Child','','TODO','LOW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",child,user,subject,parent);
   var start=OffsetDateTime.parse("2026-10-08T10:00:00Z");
   db.update("INSERT INTO study_sessions(id,user_id,task_id,starts_at,ends_at,minutes,planned_minutes,status,source,created_at) VALUES(?,?,?,?,?,40,60,'COMPLETED','TIMER',CURRENT_TIMESTAMP)",session,user,child,start,start.plusMinutes(40));
   db.update("INSERT INTO planning_issues(task_id,user_id,title,reason,remaining_minutes) VALUES(?,?,'Child','Capacity',20)",child,user);
   db.update("INSERT INTO completion_rewards(task_id,user_id,points,activity_date) VALUES(?,?,5,DATE '2026-10-08')",child,user);
   Flyway.configure().dataSource(source).load().migrate();
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='tasks' AND LOWER(column_name)='user_id'",Integer.class));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='completion_rewards' AND LOWER(column_name)='activity_date'",Integer.class));
   assertEquals(java.time.LocalDate.parse("2026-10-08"),db.queryForObject("SELECT completed_at FROM completion_rewards WHERE task_id=?",OffsetDateTime.class,child).atZoneSameInstant(java.time.ZoneId.of(db.queryForObject("SELECT zone_id FROM planning_settings WHERE user_id=?",String.class,user))).toLocalDate());
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name) IN ('study_sessions','planning_issues') AND LOWER(column_name)='user_id'",Integer.class));
   assertEquals(user,db.queryForObject("SELECT s.user_id FROM tasks t JOIN subjects s ON s.id=t.subject_id WHERE t.id=?",UUID.class,child));
   assertEquals(parent,db.queryForObject("SELECT parent_task_id FROM tasks WHERE id=?",UUID.class,child));
   assertEquals(60,db.queryForObject("SELECT initial_estimated_minutes FROM tasks WHERE id=?",Integer.class,parent));
   assertEquals(40,db.queryForObject("SELECT minutes FROM study_sessions WHERE id=?",Integer.class,session));
   assertEquals(60,db.queryForObject("SELECT planned_minutes FROM study_sessions WHERE id=?",Integer.class,session));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO tasks(id,subject_id,parent_task_id,title,description,status,priority,created_at,updated_at) VALUES(?,?,?,'Wrong','','TODO','LOW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",UUID.randomUUID(),otherSubject,parent));
   db.update("DELETE FROM tasks WHERE id=?",parent);
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM tasks",Integer.class));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM study_sessions",Integer.class));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM planning_issues",Integer.class));
   assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM completion_rewards",Integer.class));
  } finally {source.destroy();}
 }
}
