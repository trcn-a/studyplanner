package ua.studyplanner.account;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class AchievementMigrationTest {
 @Test void backfillsHistoryAndPreservesPreviouslyNotifiedBadge(){
  var source=new SingleConnectionDataSource("jdbc:h2:mem:achievementmigration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("16").load().migrate();var db=new JdbcTemplate(source);
   var user=UUID.fromString("00000000-0000-0000-0000-000000000001");
   for(int i=0;i<10;i++)db.update("INSERT INTO completion_rewards(task_id,user_id,points) VALUES(?,?,10)",UUID.randomUUID(),user);
   db.update("INSERT INTO notifications(id,user_id,event_key,kind,title,body,created_at) VALUES(?,?,'achievement:streak7','ACHIEVEMENT','Earned','',TIMESTAMP WITH TIME ZONE '2026-10-01 12:00:00+00')",UUID.randomUUID(),user);
   Flyway.configure().dataSource(source).load().migrate();
   assertEquals(3,db.queryForObject("SELECT COUNT(*) FROM user_achievements WHERE user_id=?",Integer.class,user));
   assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM user_achievements WHERE user_id=? AND achievement_code='streak7' AND earned_at IS NOT NULL",Integer.class,user));
   assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM user_achievements WHERE user_id=? AND earned_at IS NULL",Integer.class,user));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO user_achievements(user_id,achievement_code) VALUES(?,'first')",user));
  }finally{source.destroy();}
 }
}
