package ua.studyplanner.account;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SessionMigrationTest {
 @Test void transfersPlanManualScheduleAndRecordedTimeBeforeDroppingOldTable(){
  var source=new SingleConnectionDataSource("jdbc:h2:mem:sessionmigration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("11").load().migrate();var db=new JdbcTemplate(source);
   var user=CurrentUserId();var subject=UUID.randomUUID();var task=UUID.randomUUID();var plan=UUID.randomUUID();var actual=UUID.randomUUID();
   var start=OffsetDateTime.parse("2035-01-10T09:00:00+02:00");
   db.update("INSERT INTO subjects(id,user_id,name,description,color,created_at,updated_at) VALUES(?,?,'Study','','#81583d',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",subject,user);
   db.update("INSERT INTO tasks(id,user_id,subject_id,title,description,status,priority,created_at,updated_at,scheduled_start,scheduled_end) VALUES(?,?,?,'Task','','TODO','LOW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?)",task,user,subject,start,start.plusHours(1));
   db.update("INSERT INTO study_sessions(id,user_id,task_id,starts_at,ends_at) VALUES(?,?,?,?,?)",plan,user,task,start.plusDays(1),start.plusDays(1).plusHours(1));
   db.update("INSERT INTO time_entries(id,user_id,task_id,started_at,ended_at,minutes,source,created_at) VALUES(?,?,?,?,?,25,'MANUAL',CURRENT_TIMESTAMP)",actual,user,task,start.minusDays(1),start.minusDays(1).plusMinutes(25));
   Flyway.configure().dataSource(source).load().migrate();
   assertEquals(3,db.queryForObject("SELECT COUNT(*) FROM study_sessions",Integer.class));
   assertEquals("PLANNED",db.queryForObject("SELECT status FROM study_sessions WHERE id=?",String.class,plan));
   assertEquals(25,db.queryForObject("SELECT minutes FROM study_sessions WHERE id=?",Integer.class,actual));
   assertEquals(start.minusDays(1).toInstant(),db.queryForObject("SELECT starts_at FROM study_sessions WHERE id=?",OffsetDateTime.class,actual).toInstant());
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)='time_entries'",Integer.class));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='tasks' AND LOWER(column_name)='scheduled_start'",Integer.class));
  }finally{source.destroy();}
 }
 private UUID CurrentUserId(){return UUID.fromString("00000000-0000-0000-0000-000000000001");}
}
