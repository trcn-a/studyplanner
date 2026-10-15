package ua.studyplanner.account;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class CalendarMergeMigrationTest {
 @Test void preservesBothSourcesAndHandlesSharedIdsAcrossUsers(){
  var source=new SingleConnectionDataSource("jdbc:h2:mem:calendarmerge;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("17").load().migrate();var db=new JdbcTemplate(source);
   var a=UUID.fromString("00000000-0000-0000-0000-000000000001");var b=UUID.randomUUID();var event=UUID.randomUUID();
   db.update("INSERT INTO app_users(id,email,display_name,password_hash) VALUES(?,'merge@example.com','Other','hash')",b);
   db.update("INSERT INTO calendar_events(id,user_id,external_uid,title,starts_at,ends_at) VALUES(?,?,?,'ICS',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1' HOUR)",event,a,event.toString());
   for(var user:new UUID[]{a,b})db.update("INSERT INTO google_calendar_events(id,user_id,title,starts_at,ends_at) VALUES(?,?,'Google',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1' HOUR)",event,user);
   Flyway.configure().dataSource(source).load().migrate();
   assertEquals(3,db.queryForObject("SELECT COUNT(*) FROM calendar_events",Integer.class));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)='google_calendar_events'",Integer.class));
   assertEquals(event,db.queryForObject("SELECT id FROM calendar_events WHERE source='ICALENDAR'",UUID.class));
   assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM calendar_events WHERE source='GOOGLE' AND external_uid=?",Integer.class,event.toString()));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO calendar_events(id,user_id,source,external_uid,title,starts_at,ends_at) VALUES(?,?,'ICALENDAR',?,'Duplicate',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '1' HOUR)",UUID.randomUUID(),a,event.toString()));
  }finally{source.destroy();}
 }
}
