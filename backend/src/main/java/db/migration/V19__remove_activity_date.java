package db.migration;
import org.flywaydb.core.api.migration.*;
import java.time.*;
public class V19__remove_activity_date extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 19001;}
 @Override public void migrate(Context context)throws Exception {
  var c=context.getConnection();
  try(var q=c.createStatement();var r=q.executeQuery("SELECT r.task_id,r.activity_date,p.zone_id FROM completion_rewards r LEFT JOIN planning_settings p ON p.user_id=r.user_id WHERE r.completed_at IS NULL AND r.activity_date IS NOT NULL")){
   while(r.next()){
    var zone=r.getString(3);var at=r.getObject(2,LocalDate.class).atStartOfDay(ZoneId.of(zone==null?"UTC":zone)).toOffsetDateTime();
    try(var update=c.prepareStatement("UPDATE completion_rewards SET completed_at=? WHERE task_id=?")){update.setObject(1,at);update.setObject(2,r.getObject(1));update.executeUpdate();}
   }
  }
  try(var q=c.createStatement()){q.execute("ALTER TABLE completion_rewards DROP COLUMN activity_date");}
 }
}
