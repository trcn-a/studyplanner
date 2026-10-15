package ua.studyplanner.account;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class RewardIdentifierMigrationTest {
 @Test void preservesRewardsAndAddsIndependentIdentifier(){
  var source=new SingleConnectionDataSource("jdbc:h2:mem:rewardidentifier;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","",true);
  try {
   Flyway.configure().dataSource(source).target("21").load().migrate();
   var db=new JdbcTemplate(source);var user=UUID.fromString("00000000-0000-0000-0000-000000000001");var task=UUID.randomUUID();
   db.update("INSERT INTO completion_rewards(task_id,user_id,points,completed_at) VALUES(?,?,10,CURRENT_TIMESTAMP)",task,user);
   Flyway.configure().dataSource(source).load().migrate();
   var id=db.queryForObject("SELECT id FROM completion_rewards WHERE task_id=?",UUID.class,task);
   assertNotNull(id);assertNotEquals(task,id);
   assertEquals(10,db.queryForObject("SELECT points FROM completion_rewards WHERE id=?",Integer.class,id));
   assertEquals(user,db.queryForObject("SELECT user_id FROM completion_rewards WHERE id=?",UUID.class,id));
   assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO completion_rewards(id,task_id,user_id,points) VALUES(?,?,?,10)",UUID.randomUUID(),task,user));
   // A deleted or absent task does not prevent storing its historical reward.
   db.update("INSERT INTO completion_rewards(id,task_id,user_id,points) VALUES(?,?,?,5)",UUID.randomUUID(),UUID.randomUUID(),user);
  } finally {source.destroy();}
 }
}
