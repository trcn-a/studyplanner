package ua.studyplanner.account;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StudyDaysMigrationTest {
 @Test void movesExistingChoicesToPlanningSettings() {
  var source=new SingleConnectionDataSource("jdbc:h2:mem:studydaysmigration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("15").load().migrate();
   var db=new JdbcTemplate(source);
   var user=UUID.fromString("00000000-0000-0000-0000-000000000001");
   db.update("UPDATE app_users SET study_days=21 WHERE id=?",user);
   Flyway.configure().dataSource(source).load().migrate();
   assertEquals(21,db.queryForObject("SELECT study_days FROM planning_settings WHERE user_id=?",Integer.class,user));
   assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='app_users' AND LOWER(column_name)='study_days'",Integer.class));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("UPDATE planning_settings SET study_days=0 WHERE user_id=?",user));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("UPDATE planning_settings SET study_days=128 WHERE user_id=?",user));
  } finally {source.destroy();}
 }
}
