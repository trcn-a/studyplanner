package db.migration;
import org.flywaydb.core.api.migration.*;
import ua.studyplanner.gamification.domain.GamificationRules;
import java.sql.*;
import java.time.*;
import java.util.*;
public class V17__persistent_achievements extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 17001;}
 @Override public void migrate(Context context)throws Exception {
  var c=context.getConnection();
  try(var st=c.createStatement()){st.execute("CREATE TABLE user_achievements(user_id UUID NOT NULL REFERENCES app_users(id), achievement_code VARCHAR(40) NOT NULL, earned_at TIMESTAMP WITH TIME ZONE, PRIMARY KEY(user_id,achievement_code), CHECK(achievement_code IN ('first','ten','fifty','streak3','streak7','streak30')))");}
  // Notifications preserve previously earned badges even if the current schedule changed.
  try(var st=c.createStatement();var rs=st.executeQuery("SELECT user_id,event_key,MIN(created_at) FROM notifications WHERE kind='ACHIEVEMENT' GROUP BY user_id,event_key")){
   while(rs.next()){String key=rs.getString(2);if(key.startsWith("achievement:")){String code=key.substring(12);if(Set.of("first","ten","fifty","streak3","streak7","streak30").contains(code))insert(c,rs.getObject(1,UUID.class),code,rs.getObject(3,OffsetDateTime.class));}}
  }
  try(var st=c.createStatement();var users=st.executeQuery("SELECT user_id,zone_id,study_days FROM planning_settings")){
   while(users.next()){var id=users.getObject(1,UUID.class);var days=new ArrayList<LocalDate>();long points=0;int count=0;
    try(var q=c.prepareStatement("SELECT points,activity_date FROM completion_rewards WHERE user_id=?")){q.setObject(1,id);try(var r=q.executeQuery()){while(r.next()){points+=r.getInt(1);count++;days.add(r.getObject(2,LocalDate.class));}}}
    var view=GamificationRules.summarize(points,count,days,LocalDate.now(ZoneId.of(users.getString(2))),users.getInt(3));
    // Historical acquisition time cannot be reconstructed reliably.
    for(var badge:view.achievements())if(badge.unlocked())insert(c,id,badge.id(),null);
   }
  }
 }
 private void insert(Connection c,UUID user,String code,OffsetDateTime at)throws SQLException{
  try(var q=c.prepareStatement("SELECT COUNT(*) FROM user_achievements WHERE user_id=? AND achievement_code=?")){q.setObject(1,user);q.setString(2,code);try(var r=q.executeQuery()){r.next();if(r.getInt(1)>0)return;}}
  try(var q=c.prepareStatement("INSERT INTO user_achievements(user_id,achievement_code,earned_at) VALUES(?,?,?)")){q.setObject(1,user);q.setString(2,code);q.setObject(3,at);q.executeUpdate();}
 }
}
