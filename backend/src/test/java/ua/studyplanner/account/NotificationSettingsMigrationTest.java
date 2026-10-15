package ua.studyplanner.account;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class NotificationSettingsMigrationTest {
 @Test void preservesPreferencesAndRemovesAccountColumns(){
  var source=new SingleConnectionDataSource("jdbc:h2:mem:notificationmigration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("19").load().migrate();
   var db=new JdbcTemplate(source);var user=UUID.fromString("00000000-0000-0000-0000-000000000001");
   db.update("UPDATE app_users SET reminders_enabled=FALSE,reminder_minutes=45 WHERE id=?",user);
   Flyway.configure().dataSource(source).load().migrate();
   assertFalse(db.queryForObject("SELECT reminders_enabled FROM notification_settings WHERE user_id=?",Boolean.class,user));
   assertEquals(45,db.queryForObject("SELECT reminder_minutes FROM notification_settings WHERE user_id=?",Integer.class,user));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='app_users' AND LOWER(column_name) IN ('reminders_enabled','reminder_minutes')",Integer.class));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO notification_settings(user_id) VALUES(?)",user));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("UPDATE notification_settings SET reminder_minutes=4 WHERE user_id=?",user));
  } finally {source.destroy();}
 }
}
