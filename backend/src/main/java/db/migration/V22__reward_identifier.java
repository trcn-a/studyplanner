package db.migration;
import org.flywaydb.core.api.migration.*;
import java.util.UUID;
public class V22__reward_identifier extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 22001;}
 @Override public void migrate(Context context)throws Exception {
  var c=context.getConnection();
  try(var st=c.createStatement()){
   st.execute("CREATE TABLE rewards_with_id(id UUID PRIMARY KEY,task_id UUID NOT NULL UNIQUE,user_id UUID NOT NULL REFERENCES app_users(id),points INTEGER NOT NULL CHECK(points IN (5,10)),completed_at TIMESTAMP WITH TIME ZONE)");
  }
  try(var st=c.createStatement();var r=st.executeQuery("SELECT task_id,user_id,points,completed_at FROM completion_rewards");var insert=c.prepareStatement("INSERT INTO rewards_with_id(id,task_id,user_id,points,completed_at) VALUES(?,?,?,?,?)")){
   while(r.next()){insert.setObject(1,UUID.randomUUID());insert.setObject(2,r.getObject(1));insert.setObject(3,r.getObject(2));insert.setInt(4,r.getInt(3));insert.setObject(5,r.getObject(4));insert.addBatch();}insert.executeBatch();
  }
  try(var st=c.createStatement();var r=st.executeQuery("SELECT (SELECT COUNT(*) FROM completion_rewards),(SELECT COUNT(*) FROM rewards_with_id)")){r.next();if(r.getLong(1)!=r.getLong(2))throw new java.sql.SQLException("Reward migration count mismatch");}
  try(var st=c.createStatement()){st.execute("DROP TABLE completion_rewards");st.execute("ALTER TABLE rewards_with_id RENAME TO completion_rewards");st.execute("CREATE INDEX idx_completion_rewards_user ON completion_rewards(user_id)");}
 }
}
